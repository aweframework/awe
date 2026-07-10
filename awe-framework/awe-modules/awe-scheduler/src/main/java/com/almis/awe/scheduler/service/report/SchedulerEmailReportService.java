package com.almis.awe.scheduler.service.report;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.enums.ReportType;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.almis.awe.scheduler.constant.ReportConstants.*;

@Slf4j
public class SchedulerEmailReportService extends ServiceConfig implements ISchedulerReportService {

  // Constants
  private static final String LIST_START = "<li><b>";
  private static final String LIST_END = "</li>";
  private static final String BOLD_END = ":</b> ";
  private static final String COLON_SPACE = ": ";
  private static final String NEW_LINE = "\n";
  private static final String EMPTY = "";

  // Reserved report variable keys (metadata always wins over same-named task parameters)
  private static final String VAR_TASK_NAME = "taskName";
  private static final String VAR_TASK_ID = "taskId";
  private static final String VAR_TASK_DESCRIPTION = "taskDescription";
  private static final String VAR_STATUS = "status";
  private static final String VAR_STATUS_DETAIL = "statusDetail";
  private static final String VAR_EXECUTION_ID = "executionId";
  private static final String VAR_COMMAND = "command";
  private static final String VARIABLE_PREFIX = "${";
  private static final String VARIABLE_SUFFIX = "}";
  private static final String STATUS_LABEL_RESOURCE = "StaTyp";

  // Autowired services
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;
  private final QueryService queryService;
  private final ObjectMapper mapper;

  /**
   * Autowired constructor
   *
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   * @param queryService    Query service
   */
  public SchedulerEmailReportService(QueryUtil queryUtil, MaintainService maintainService, QueryService queryService, ObjectMapper mapper) {
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
    this.queryService = queryService;
    this.mapper = mapper;
  }

  @Override
  public ReportType getType() {
    return ReportType.EMAIL;
  }

  public void execute(Task task, TaskExecution taskExecution) {
    // Resolve report title and message placeholders (${variable}) before building any output
    String reportTitle = task.getReport().getReportTitle();
    String reportMessage = task.getReport().getReportMessage();

    // Short-circuit substitution: only build the variable catalog and resolve placeholders when
    // at least one template actually contains a ${...} marker. This preserves byte-identical
    // legacy behavior for reports without placeholders and avoids building the catalog (and its
    // failure surface) when it would never be used.
    String resolvedTitle;
    String resolvedMessage;
    String resolvedHtmlTitle;
    String resolvedHtmlMessage;
    if (containsPlaceholder(reportTitle) || containsPlaceholder(reportMessage)) {
      Map<String, String> variableCatalog = buildVariableCatalog(task, taskExecution);
      Map<String, String> htmlVariableCatalog = buildHtmlVariableCatalog(variableCatalog);

      resolvedTitle = applyVariables(reportTitle, variableCatalog);
      resolvedMessage = applyVariables(reportMessage, variableCatalog);
      resolvedHtmlTitle = applyVariables(reportTitle, htmlVariableCatalog);
      resolvedHtmlMessage = applyVariables(reportMessage, htmlVariableCatalog);
    } else {
      resolvedTitle = reportTitle;
      resolvedMessage = reportMessage;
      resolvedHtmlTitle = reportTitle;
      resolvedHtmlMessage = reportMessage;
    }

    // Store task and execution in parameters
    ObjectNode parameters = queryUtil.getParameters();
    parameters.set(REPORT_DESTINATION_EMAILS, mapper.valueToTree(task.getReport().getReportEmailDestination()));
    parameters.put(REPORT_TITLE, resolvedTitle);
    parameters.put(REPORT_MESSAGE_HTML, constructHTMLMessage(resolvedHtmlTitle, resolvedHtmlMessage, task, taskExecution));
    parameters.put(REPORT_MESSAGE_TEXT, constructTextMessage(resolvedTitle, resolvedMessage, task, taskExecution));

    try {
      maintainService.launchPrivateMaintain(REPORT_MAINTAIN_TARGET, parameters);
    } catch (AWException exc) {
      // Log error
      log.error("Report generation error for task {}", task.getTaskId(), exc);
    }
  }

  /**
   * Build the ordered raw variable catalog used to resolve ${variable} placeholders in the
   * report title and message. Metadata keys are inserted first (reserved) and always take
   * precedence over a task parameter with the same name; on collision the metadata value is
   * kept and a warning is logged naming the shadowed parameter. Null values are coalesced to
   * an empty string.
   *
   * @param task      Task being reported
   * @param execution Task execution
   * @return Ordered raw catalog of variable name to value
   */
  private Map<String, String> buildVariableCatalog(Task task, TaskExecution execution) {
    Map<String, String> catalog = new LinkedHashMap<>();
    catalog.put(VAR_TASK_NAME, coalesce(task.getName()));
    catalog.put(VAR_TASK_ID, coalesce(task.getTaskId() != null ? task.getTaskId().toString() : null));
    catalog.put(VAR_TASK_DESCRIPTION, coalesce(task.getDescription()));
    catalog.put(VAR_STATUS, coalesce(resolveStatusLabel(execution)));
    catalog.put(VAR_STATUS_DETAIL, coalesce(execution.getDescription()));
    catalog.put(VAR_EXECUTION_ID, coalesce(execution.getExecutionId() != null ? execution.getExecutionId().toString() : null));
    catalog.put(VAR_COMMAND, coalesce(task.getAction()));

    appendTaskParameters(catalog, task);
    return catalog;
  }

  /**
   * Append task parameter values to the catalog. Parameters with a null name are skipped, and
   * names that collide with an already-present key (reserved metadata or an earlier parameter)
   * are not overwritten; the collision is logged.
   *
   * @param catalog Catalog being built, with reserved metadata already inserted
   * @param task    Task providing the parameter list
   */
  private void appendTaskParameters(Map<String, String> catalog, Task task) {
    if (task.getParameterList() == null) {
      return;
    }
    for (TaskParameter parameter : task.getParameterList()) {
      String parameterName = parameter.getName();
      if (parameterName == null) {
        log.warn("Ignoring task parameter with a null name for placeholder resolution");
      } else if (catalog.containsKey(parameterName)) {
        logSkippedParameter(parameterName);
      } else {
        catalog.put(parameterName, coalesce(parameter.getValue()));
      }
    }
  }

  /**
   * Log the reason a colliding task parameter name is skipped: shadowed by reserved metadata
   * or a duplicate parameter name.
   *
   * @param parameterName Colliding task parameter name
   */
  private void logSkippedParameter(String parameterName) {
    if (isReservedMetadataKey(parameterName)) {
      log.warn("Task parameter '{}' is shadowed by a reserved report metadata variable and will not be used for placeholder resolution", parameterName);
    } else {
      log.warn("Duplicate task parameter name '{}' for placeholder resolution; the first value is kept and later values are ignored", parameterName);
    }
  }

  /**
   * Whether the given key is one of the reserved report metadata variable names.
   *
   * @param key Candidate variable name
   * @return {@code true} if the key is a reserved metadata variable name
   */
  private boolean isReservedMetadataKey(String key) {
    return VAR_TASK_NAME.equals(key)
      || VAR_TASK_ID.equals(key)
      || VAR_TASK_DESCRIPTION.equals(key)
      || VAR_STATUS.equals(key)
      || VAR_STATUS_DETAIL.equals(key)
      || VAR_EXECUTION_ID.equals(key)
      || VAR_COMMAND.equals(key);
  }

  /**
   * Whether the given template contains at least one ${...} placeholder marker.
   *
   * @param template Template to inspect (may be null)
   * @return {@code true} if the template contains the placeholder prefix
   */
  private boolean containsPlaceholder(String template) {
    return template != null && template.contains(VARIABLE_PREFIX);
  }

  /**
   * Resolve the localized status label for the given execution, matching the same source
   * used in the fixed task-details block.
   *
   * @param execution Task execution
   * @return Localized status label, or {@code null} if the label cannot be resolved
   */
  private String resolveStatusLabel(TaskExecution execution) {
    if (execution.getStatus() == null) {
      return EMPTY;
    }
    try {
      return getLocale(queryService.findLabel(STATUS_LABEL_RESOURCE, execution.getStatus().toString()));
    } catch (Exception exc) {
      log.error("Error resolving status label for task execution #{}", execution.getExecutionId(), exc);
      return null;
    }
  }

  /**
   * Derive an HTML-safe copy of the raw variable catalog by escaping every value.
   * The template itself is never escaped, only the substituted values.
   *
   * @param catalog Raw variable catalog
   * @return HTML-escaped variable catalog
   */
  private Map<String, String> buildHtmlVariableCatalog(Map<String, String> catalog) {
    Map<String, String> htmlCatalog = new LinkedHashMap<>();
    catalog.forEach((key, value) -> htmlCatalog.put(key, StringEscapeUtils.escapeHtml4(value)));
    return htmlCatalog;
  }

  /**
   * Coalesce a possibly null value to an empty string.
   *
   * @param value Value
   * @return Value, or an empty string if null
   */
  private String coalesce(String value) {
    return value == null ? EMPTY : value;
  }

  /**
   * Resolve every ${key} placeholder in a single left-to-right pass over the original template.
   * Each placeholder is looked up in the catalog: when the key is known its value is appended
   * verbatim and is never re-scanned, so a substituted value that itself contains a ${...} marker
   * is emitted as-is. Unknown placeholders and any unterminated ${ (with no closing brace) are
   * copied literally. Text outside placeholders is copied verbatim.
   *
   * @param template Template containing zero or more ${key} placeholders
   * @param catalog  Variable catalog
   * @return Resolved template, or {@code null} if template is null
   */
  private String applyVariables(String template, Map<String, String> catalog) {
    if (template == null) {
      return null;
    }
    StringBuilder result = new StringBuilder(template.length());
    int cursor = 0;
    while (cursor < template.length()) {
      int start = template.indexOf(VARIABLE_PREFIX, cursor);
      int end = start < 0 ? -1 : template.indexOf(VARIABLE_SUFFIX, start + VARIABLE_PREFIX.length());
      if (start < 0 || end < 0) {
        // No further complete placeholder: copy the rest verbatim (including any unterminated ${).
        result.append(template, cursor, template.length());
        cursor = template.length();
      } else {
        // Copy the literal text preceding the placeholder.
        result.append(template, cursor, start);
        String key = template.substring(start + VARIABLE_PREFIX.length(), end);
        if (catalog.containsKey(key)) {
          // Append the resolved value verbatim; it is never re-scanned.
          result.append(catalog.get(key));
        } else {
          // Unknown placeholder: keep the literal ${key}.
          result.append(VARIABLE_PREFIX).append(key).append(VARIABLE_SUFFIX);
        }
        cursor = end + VARIABLE_SUFFIX.length();
      }
    }
    return result.toString();
  }

  /**
   * Get HTML string with report message with title
   *
   * @param resolvedTitle   Resolved (HTML-escaped) report title
   * @param resolvedMessage Resolved (HTML-escaped) report message
   * @param task            Task
   * @param execution       Task execution
   * @return
   * @throws AWException
   */
  private String constructHTMLMessage(String resolvedTitle, String resolvedMessage, Task task, TaskExecution execution) {
    String msg = "";
    msg += "<html>";
    msg += constructHTMLHeader(resolvedTitle != null ? resolvedTitle : getLocale(PARAMETER_TASK_DETAILS));
    msg += constructHTMLBody(resolvedMessage, task, execution);
    msg += "</html>";
    return msg;
  }

  /**
   * Get HTML message header
   *
   * @return String
   */
  private String constructHTMLHeader(String title) {
    StringBuilder builder = new StringBuilder();
    builder.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
    builder.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
    builder.append("<head>");
    builder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
    builder.append("<title>").append(title).append("</title>");
    builder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>");
    builder.append("</head>");
    return builder.toString();
  }

  /**
   * Get HTML message body
   *
   * @param resolvedMessage Resolved (HTML-escaped) report message
   * @param task            Task
   * @param execution       Task execution
   * @return String
   */
  private String constructHTMLBody(String resolvedMessage, Task task, TaskExecution execution) {
    String msg = "";
    // Construct HTML body
    msg += "<p>" + resolvedMessage + "</p>";
    msg += "<div style=\"border:1px solid black;\">";
    try {
      msg += "<br><b><u>" + getLocale(PARAMETER_TASK_DETAILS) + "</u></b>";
      msg += getTaskDetailsMessage(task, execution);
    } catch (Exception exc) {
      // No details
      log.error("Error generating HTML task details for task #{} execution #{}", execution.getTaskId(), execution.getExecutionId(), exc);
    }
    msg += "</div>";
    return msg;
  }

  /**
   * Get HTML string with task details
   *
   * @return
   * @throws AWException
   */
  private String getTaskDetailsMessage(Task task, TaskExecution execution) throws AWException {
    StringBuilder builder = new StringBuilder();

    String launchType = queryService.findLabel("LchTxtTyp", execution.getGroupId());
    String statusColor = queryService.findLabel("StaColor", execution.getStatus().toString());
    String statusText = queryService.findLabel(STATUS_LABEL_RESOURCE, execution.getStatus().toString());

    // Construct HTML task details message
    builder.append("<div><ul>");
    builder.append(LIST_START).append(getLocale(PARAMETER_NAME)).append(BOLD_END).append(task.getName()).append(LIST_END);
    builder.append(LIST_START).append(getLocale(PARAMETER_IDE)).append(BOLD_END).append(task.getTaskId()).append(LIST_END);
    builder.append(LIST_START).append(getLocale(PARAMETER_LAUNCH_TYPE)).append(BOLD_END).append(getLocale(launchType)).append(LIST_END);
    builder.append(LIST_START).append(getLocale(PARAMETER_DESCRIPTION)).append(BOLD_END).append(task.getDescription()).append(LIST_END);
    builder.append("<li style=\"margin-top:12px;\"><b>")
      .append(getLocale(PARAMETER_STATUS)).append(BOLD_END)
      .append("<span style = \"padding:10px;color:white;-moz-border-radius: 20px; -webkit-border-radius: 20px; border-radius: 20px;background-color:")
      .append(statusColor).append("\">").append(getLocale(statusText)).append("</span></li>");

    switch (TaskStatus.valueOf(execution.getStatus())) {
      case JOB_ERROR:
        builder.append(LIST_START).append(getLocale(ERROR_LOG)).append(BOLD_END).append(execution.getDescription()).append(LIST_END);
        break;
      case JOB_WARNING, JOB_INFO:
        builder.append(LIST_START).append(queryService.findLabel("StaTit", execution.getStatus().toString())).append(BOLD_END).append(execution.getDescription()).append(LIST_END);
        break;
      default:
    }

    builder.append(LIST_START).append(getLocale(PARAMETER_EXECUTED_COMMAND)).append(BOLD_END).append(task.getAction()).append(LIST_END);
    builder.append(LIST_START).append(getLocale(PARAMETER_PARAMETERS)).append(BOLD_END).append(LIST_END);
    builder.append("<ul>");
    for (TaskParameter parameter : task.getParameterList()) {
      builder.append(LIST_START).append(parameter.getName()).append(BOLD_END).append(parameter.getValue()).append(LIST_END);
    }
    builder.append("</ul></li>");
    builder.append("</ul>");
    builder.append("</div>");

    return builder.toString();
  }

  /**
   * Get text string with report message with title
   *
   * @param resolvedTitle   Resolved (raw) report title
   * @param resolvedMessage Resolved (raw) report message
   * @param task            Task
   * @param execution       Task execution
   * @return
   * @throws AWException
   */
  private String constructTextMessage(String resolvedTitle, String resolvedMessage, Task task, TaskExecution execution) {
    String msg = "";
    msg += resolvedTitle != null ? resolvedTitle : getLocale(PARAMETER_TASK_DETAILS) + NEW_LINE;
    msg += constructTextBody(resolvedMessage, task, execution);
    return msg;
  }

  /**
   * Get Text message body
   *
   * @param resolvedMessage Resolved (raw) report message
   * @param task            Task
   * @param execution       Task execution
   * @return String
   */
  private String constructTextBody(String resolvedMessage, Task task, TaskExecution execution) {
    String msg = "";
    // Construct text body
    msg += resolvedMessage + "\n\n";
    try {
      msg += getLocale(PARAMETER_TASK_DETAILS) + NEW_LINE;
      msg += getTextTaskDetailsMessage(task, execution);
    } catch (Exception exc) {
      // No details
      log.error("Error generating text task details for task #{} execution #{}", execution.getTaskId(), execution.getExecutionId(), exc);
    }
    return msg;
  }

  /**
   * Get HTML string with task details
   *
   * @return
   * @throws AWException
   */
  private String getTextTaskDetailsMessage(Task task, TaskExecution execution) throws AWException {
    StringBuilder builder = new StringBuilder();

    String launchType = queryService.findLabel("LchTxtTyp", execution.getGroupId());
    String statusText = queryService.findLabel(STATUS_LABEL_RESOURCE, execution.getStatus().toString());

    // Construct HTML task details message
    builder.append(getLocale(PARAMETER_NAME)).append(COLON_SPACE).append(task.getName()).append(NEW_LINE);
    builder.append(getLocale(PARAMETER_IDE)).append(COLON_SPACE).append(task.getTaskId()).append(NEW_LINE);
    builder.append(getLocale(PARAMETER_LAUNCH_TYPE)).append(COLON_SPACE).append(getLocale(launchType)).append(NEW_LINE);
    builder.append(getLocale(PARAMETER_DESCRIPTION)).append(COLON_SPACE).append(task.getDescription()).append(NEW_LINE);
    builder.append(getLocale(PARAMETER_STATUS)).append(COLON_SPACE).append(getLocale(statusText)).append(NEW_LINE);

    switch (TaskStatus.valueOf(execution.getStatus())) {
      case JOB_ERROR:
        builder.append(getLocale(ERROR_LOG)).append(COLON_SPACE).append(execution.getDescription()).append(NEW_LINE);
        break;
      case JOB_WARNING, JOB_INFO:
        builder.append(queryService.findLabel("StaTit", execution.getStatus().toString())).append(COLON_SPACE).append(execution.getDescription()).append(NEW_LINE);
        break;
      default:
        break;
    }

    builder.append(getLocale(PARAMETER_EXECUTED_COMMAND)).append(COLON_SPACE).append(task.getAction()).append(NEW_LINE);
    builder.append(getLocale(PARAMETER_PARAMETERS)).append(NEW_LINE);
    for (TaskParameter parameter : task.getParameterList()) {
      builder.append("  - ").append(parameter.getName()).append(COLON_SPACE).append(parameter.getValue()).append(NEW_LINE);
    }

    return builder.toString();
  }
}
