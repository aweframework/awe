package com.almis.awe.scheduler.job.report;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.service.report.SchedulerEmailReportService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.almis.awe.scheduler.constant.ReportConstants.REPORT_MAINTAIN_TARGET;
import static com.almis.awe.scheduler.constant.ReportConstants.REPORT_MESSAGE_HTML;
import static com.almis.awe.scheduler.constant.ReportConstants.REPORT_MESSAGE_TEXT;
import static com.almis.awe.scheduler.constant.ReportConstants.REPORT_TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing queries through ActionController
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class SchedulerEmailReportServiceTest {

  @InjectMocks
  private SchedulerEmailReportService schedulerEmailReportService;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private MaintainService maintainService;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  private ObjectMapper mapper;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  void initBeans() throws Exception {
    schedulerEmailReportService.setApplicationContext(context);
  }

  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(schedulerEmailReportService);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @param status Task status
   * @throws NamingException Test error
   */
  @ParameterizedTest
  @EnumSource(TaskStatus.class)
  void executeTask(TaskStatus status) throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");
    executeEmailJob(status);
  }

  /**
   * Execute email report job
   *
   * @throws Exception see {@link Exception}
   */
  private void executeEmailJob(TaskStatus status) throws Exception {
    Task task = new Task()
      .setParameterList(new ArrayList<>())
      .setReport(new Report().setReportUserDestination(new ArrayList<>()))
      .setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    TaskExecution execution = new TaskExecution()
      .setExecutionId(1)
      .setGroupId("TEST_GROUP")
      .setStatus(status.getValue());

    schedulerEmailReportService.execute(task, execution);
    verify(maintainService, times(1)).launchPrivateMaintain(eq(REPORT_MAINTAIN_TARGET), any(ObjectNode.class));
  }

  /**
   * Build a task with the given name, description, action and parameter list
   *
   * @param name        Task name
   * @param description Task description
   * @param action      Task action/command
   * @param parameters  Task parameters
   * @return Task
   */
  private Task buildTask(String name, String description, String action, List<TaskParameter> parameters, String reportTitle, String reportMessage) {
    return new Task()
      .setName(name)
      .setDescription(description)
      .setAction(action)
      .setParameterList(parameters)
      .setReport(new Report()
        .setReportUserDestination(new ArrayList<>())
        .setReportTitle(reportTitle)
        .setReportMessage(reportMessage))
      .setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
  }

  /**
   * Build a task execution with the given execution id, status and description
   *
   * @param executionId Execution id
   * @param status      Task status
   * @param description Execution description (status detail)
   * @return TaskExecution
   */
  private TaskExecution buildExecution(Integer executionId, TaskStatus status, String description) {
    return new TaskExecution()
      .setExecutionId(executionId)
      .setGroupId("TEST_GROUP")
      .setStatus(status.getValue())
      .setDescription(description);
  }

  /**
   * Stub the collaborators used by every report execution, overriding the status label
   * resolution for the given status so tests can assert against a known localized value.
   *
   * @param parameters    Real ObjectNode instance to be populated by the service under test
   * @param statusLabel   Label key returned for the status resource ("StaTyp")
   * @param localizedStatus Localized text the label resolves to
   */
  private void stubReportDependencies(ObjectNode parameters, String statusLabel, String localizedStatus) throws com.almis.awe.exception.AWException {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(queryUtil.getParameters()).willReturn(parameters);
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryService.findLabel(eq("StaTyp"), anyString())).willReturn(statusLabel);
    given(aweElements.getLocaleWithLanguage(eq(statusLabel), anyString())).willReturn(localizedStatus);
  }

  @Test
  void resolvesMetadataVariablesInTitleAndBody() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_ERROR", "ERROR");

    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      "Task ${taskName} finished: ${status}", "Status: ${status}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_ERROR, "boom");

    schedulerEmailReportService.execute(task, execution);

    assertEquals("Task Nightly Sync finished: ERROR", parameters.get(REPORT_TITLE).asText());
    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Status: ERROR"));
    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Status: ERROR"));
  }

  @Test
  void statusDetailBlockHasNoStrayParenthesis() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      null, "All good.");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    String html = parameters.get(REPORT_MESSAGE_HTML).asText();
    assertFalse(html.contains("</span></li>)"),
      "The status list item must not emit a stray closing parenthesis");
  }

  @Test
  void nullMetadataCoalescesToEmptyString() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    Task task = buildTask("Nightly Sync", null, "run.sh", new ArrayList<>(),
      null, "Desc:[${taskDescription}] Detail:[${statusDetail}]");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Desc:[] Detail:[]"));
    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Desc:[] Detail:[]"));
  }

  @Test
  void resolvesTaskParameterByName() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    List<TaskParameter> parameterList = Collections.singletonList(
      new TaskParameter().setName("env").setValue("production"));
    Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
      null, "Environment: ${env}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Environment: production"));
    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Environment: production"));
  }

  @Test
  void reservedMetadataWinsOverSameNamedParameter() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SchedulerEmailReportService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    try {
      ObjectNode parameters = JsonNodeFactory.instance.objectNode();
      stubReportDependencies(parameters, "LBL_OK", "OK");

      List<TaskParameter> parameterList = Collections.singletonList(
        new TaskParameter().setName("status").setValue("custom-value"));
      Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
        null, "Status: ${status}");
      TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

      schedulerEmailReportService.execute(task, execution);

      assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Status: OK"));
      assertFalse(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Status: custom-value"));
      assertTrue(logAppender.list.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("status")));
    } finally {
      logger.detachAppender(logAppender);
    }
  }

  @Test
  void unknownVariableStaysLiteral() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      null, "Value: ${foo}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Value: ${foo}"));
    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Value: ${foo}"));
  }

  @Test
  void htmlEscapesSubstitutedValueOnly() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    List<TaskParameter> parameterList = Collections.singletonList(
      new TaskParameter().setName("note").setValue("<script>alert(1)</script>"));
    Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
      null, "Note: ${note}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    String html = parameters.get(REPORT_MESSAGE_HTML).asText();
    String text = parameters.get(REPORT_MESSAGE_TEXT).asText();

    // The substituted message paragraph must contain the escaped value, not the raw markup
    // (the pre-existing, unrelated fixed task-details block that echoes raw parameter values
    // is explicitly out of scope for this change).
    assertFalse(html.contains("<p>Note: <script>alert(1)</script></p>"));
    assertTrue(html.contains("<p>Note: &lt;script&gt;alert(1)&lt;/script&gt;</p>"));
    assertTrue(text.contains("Note: <script>alert(1)</script>"));
  }

  @Test
  void nullExecutionStatusResolvesToEmptyWithoutFailing() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters()).willReturn(parameters);
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");

    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      null, "Status:[${status}]");
    TaskExecution execution = new TaskExecution()
      .setExecutionId(7)
      .setGroupId("TEST_GROUP")
      .setStatus(null);

    schedulerEmailReportService.execute(task, execution);

    // Report is still produced (no NPE) and the status placeholder resolves to an empty string
    verify(maintainService, times(1)).launchPrivateMaintain(eq(REPORT_MAINTAIN_TARGET), any(ObjectNode.class));
    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Status:[]"));
    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Status:[]"));
  }

  @Test
  void duplicateTaskParameterNameLogsAccurateWarning() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(SchedulerEmailReportService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    try {
      ObjectNode parameters = JsonNodeFactory.instance.objectNode();
      stubReportDependencies(parameters, "LBL_OK", "OK");

      List<TaskParameter> parameterList = new ArrayList<>();
      parameterList.add(new TaskParameter().setName("env").setValue("first"));
      parameterList.add(new TaskParameter().setName("env").setValue("second"));
      Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
        null, "Environment: ${env}");
      TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

      schedulerEmailReportService.execute(task, execution);

      // First value wins
      assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Environment: first"));
      assertFalse(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Environment: second"));

      // Accurate WARN message that does NOT claim the collision is with a reserved metadata variable
      ILoggingEvent warning = logAppender.list.stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .filter(event -> event.getFormattedMessage().contains("env"))
        .findFirst()
        .orElse(null);
      assertNotNull(warning);
      assertFalse(warning.getFormattedMessage().contains("reserved"));
    } finally {
      logger.detachAppender(logAppender);
    }
  }

  @Test
  void noPlaceholderPassthroughDoesNotFailOnNullStatusOrParameters() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters()).willReturn(parameters);
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");

    String reportTitle = "Nightly Report";
    String reportMessage = "All good.";
    Task task = new Task()
      .setName("Nightly Sync")
      .setDescription("desc")
      .setAction("run.sh")
      .setParameterList(null)
      .setReport(new Report()
        .setReportUserDestination(new ArrayList<>())
        .setReportTitle(reportTitle)
        .setReportMessage(reportMessage))
      .setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    TaskExecution execution = new TaskExecution()
      .setExecutionId(7)
      .setGroupId("TEST_GROUP")
      .setStatus(null);

    schedulerEmailReportService.execute(task, execution);

    // Passthrough: raw title/message used verbatim, and the no-placeholder path never NPEs
    // even when status and parameterList are null.
    verify(maintainService, times(1)).launchPrivateMaintain(eq(REPORT_MAINTAIN_TARGET), any(ObjectNode.class));
    assertEquals(reportTitle, parameters.get(REPORT_TITLE).asText());
    String html = parameters.get(REPORT_MESSAGE_HTML).asText();
    assertTrue(html.contains("<title>" + reportTitle + "</title>"));
    assertTrue(html.contains("<p>" + reportMessage + "</p>"));
    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().startsWith(reportTitle));
  }

  @Test
  void substitutedValueContainingPlaceholderIsNotReprocessed() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    List<TaskParameter> parameterList = new ArrayList<>();
    parameterList.add(new TaskParameter().setName("paramA").setValue("${paramB}!!"));
    parameterList.add(new TaskParameter().setName("paramB").setValue("SECRET"));
    Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
      null, "A=${paramA} B=${paramB}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    String text = parameters.get(REPORT_MESSAGE_TEXT).asText();
    String html = parameters.get(REPORT_MESSAGE_HTML).asText();

    // A substituted value that itself looks like a placeholder must be emitted verbatim and
    // never re-scanned by a later substitution.
    assertTrue(text.contains("A=${paramB}!! B=SECRET"));
    assertFalse(text.contains("A=SECRET"));
    assertTrue(html.contains("A=${paramB}!! B=SECRET"));
    assertFalse(html.contains("A=SECRET"));
  }

  @Test
  void statusLabelResolutionFailureDoesNotBreakExecution() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters()).willReturn(parameters);
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");
    // An unchecked failure while resolving the status label must degrade gracefully instead of
    // propagating out of the report execution.
    given(queryService.findLabel(eq("StaTyp"), anyString())).willThrow(new RuntimeException("boom"));

    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      null, "Status:[${status}]");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_ERROR, "boom");

    schedulerEmailReportService.execute(task, execution);

    // Report is still produced and the status placeholder resolves to an empty string.
    verify(maintainService, times(1)).launchPrivateMaintain(eq(REPORT_MAINTAIN_TARGET), any(ObjectNode.class));
    assertTrue(parameters.get(REPORT_MESSAGE_TEXT).asText().contains("Status:[]"));
    assertTrue(parameters.get(REPORT_MESSAGE_HTML).asText().contains("Status:[]"));
  }

  @Test
  void nullTaskParameterNameIsIgnored() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    List<TaskParameter> parameterList = new ArrayList<>();
    parameterList.add(new TaskParameter().setName(null).setValue("x"));
    parameterList.add(new TaskParameter().setName("env").setValue("production"));
    Task task = buildTask("Nightly Sync", "desc", "run.sh", parameterList,
      null, "Environment: ${env}");
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    String text = parameters.get(REPORT_MESSAGE_TEXT).asText();
    String html = parameters.get(REPORT_MESSAGE_HTML).asText();

    // A null parameter name must not leak into the catalog as a "${null}" token.
    assertTrue(text.contains("Environment: production"));
    assertFalse(text.contains("${null}"));
    assertFalse(html.contains("${null}"));
  }

  @Test
  void noPlaceholdersProducesByteIdenticalOutput() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    stubReportDependencies(parameters, "LBL_OK", "OK");

    String reportTitle = "Nightly Report";
    String reportMessage = "All good.";
    Task task = buildTask("Nightly Sync", "desc", "run.sh", new ArrayList<>(),
      reportTitle, reportMessage);
    TaskExecution execution = buildExecution(7, TaskStatus.JOB_OK, null);

    schedulerEmailReportService.execute(task, execution);

    assertEquals(reportTitle, parameters.get(REPORT_TITLE).asText());

    String html = parameters.get(REPORT_MESSAGE_HTML).asText();
    assertTrue(html.contains("<title>" + reportTitle + "</title>"));
    assertTrue(html.contains("<p>" + reportMessage + "</p>"));

    String text = parameters.get(REPORT_MESSAGE_TEXT).asText();
    assertTrue(text.startsWith(reportTitle));
    assertTrue(text.contains(reportMessage + "\n\n"));
  }
}
