package com.almis.awe.scheduler.log;

import com.almis.awe.builder.client.FilterActionBuilder;
import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.client.UpdateControllerActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;

/**
 * Database-backed {@link ExecutionLogStore} adapter. Reads and purges
 * {@code AweSchExeLog} through the AWE query/maintain layer; capture is delegated to
 * {@link ExecutionLogWriter}. Store-conditional addressing (ADR-5): the locator is an opaque
 * {@code "{taskId}-{executionId}"} token, never a filesystem path.
 */
public class DatabaseExecutionLogStore extends ServiceConfig implements ExecutionLogStore {

  private static final String EXECUTION_LOG_LINES_QUERY = "getExecutionLogLines";
  private static final String EXECUTION_LOG_KEYS_QUERY = "getExecutionLogKeys";
  private static final String PURGE_EXECUTION_LOG_LINES_TARGET = "purgeExecutionLogLines";
  private static final String EXECUTION_LOG_VIEWER_ID = "executionLogViewer";
  private static final String GET_EXECUTION_LOG_ACTION = "get-execution-log";
  private static final String HEAD_SECTION = "H";
  private static final String TASK_ID_PARAMETER = "taskId";
  private static final String EXECUTION_ID_PARAMETER = "executionId";
  private static final String LINE_NUMBER_COLUMN = "lineNumber";

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final ExecutionLogWriter writer;

  /**
   * Constructor
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param queryUtil       Query utilities
   * @param writer          Execution log writer
   */
  public DatabaseExecutionLogStore(QueryService queryService, MaintainService maintainService, QueryUtil queryUtil, ExecutionLogWriter writer) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.writer = writer;
  }

  @Override
  public void append(ExecutionLogLine line) {
    writer.append(line);
  }

  @Override
  public void complete(ExecutionKey key, ExecutionLogOrigin origin) {
    writer.complete(key, origin);
  }

  @Override
  public ExecutionLogPage read(ExecutionKey key, int offset, String clientVersion) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID_PARAMETER, key.taskId());
    parameters.put(EXECUTION_ID_PARAMETER, key.executionId());
    DataList dataList = queryService.launchPrivateQuery(EXECUTION_LOG_LINES_QUERY, parameters).getDataList();
    List<Map<String, CellData>> rows = dataList.getRows();

    if (rows.isEmpty()) {
      return ExecutionLogPage.empty();
    }

    Map<String, List<Map<String, CellData>>> rowsByOrigin = rows.stream()
      .collect(Collectors.groupingBy(row -> row.get("origin").getStringValue()));

    return rowsByOrigin.size() > 1 ? readMerged(rowsByOrigin, offset, clientVersion) : readSingleOrigin(rows, offset);
  }

  /**
   * Base ADR-6 behaviour: single writer, so the window is ordered by {@code LinNum} and a
   * truncated window carries one in-place marker between head and tail. Append fast-path: while
   * the window is intact and the offset is in range, {@code lines[offset:]} is served as a plain
   * append. Otherwise (truncation started, or the offset now exceeds the window) the full current
   * window is served in replace mode, in the same response: never pairing an empty page with a
   * separate reset round trip. offset 0 is always append mode, since an empty caller has nothing
   * to replace.
   *
   * @param rows   Rows for the execution's single origin
   * @param offset Client-held offset (line count already displayed)
   * @return Execution log page
   */
  private ExecutionLogPage readSingleOrigin(List<Map<String, CellData>> rows, int offset) {
    List<Map<String, CellData>> orderedRows = new ArrayList<>(rows);
    orderedRows.sort(Comparator.comparingInt(row -> row.get(LINE_NUMBER_COLUMN).getIntegerValue()));

    List<String> displayLines = new ArrayList<>();
    long headCount = 0;
    long tailCount = 0;
    int maxLineNumber = -1;
    for (Map<String, CellData> row : orderedRows) {
      displayLines.add(row.get("lineText").getStringValue());
      maxLineNumber = Math.max(maxLineNumber, row.get(LINE_NUMBER_COLUMN).getIntegerValue());
      if (HEAD_SECTION.equals(row.get("section").getStringValue())) {
        headCount++;
      } else {
        tailCount++;
      }
    }

    long totalLines = maxLineNumber + 1L;
    long omittedLines = Math.max(0, totalLines - headCount - tailCount);
    boolean unsafeToAppend = omittedLines > 0 || offset > displayLines.size();

    if (offset > 0 && !unsafeToAppend) {
      return new ExecutionLogPage(displayLines.subList(offset, displayLines.size()), false, totalLines, omittedLines);
    }

    List<String> window = withTruncationMarker(displayLines, headCount, omittedLines, totalLines);
    return new ExecutionLogPage(window, offset > 0, totalLines, omittedLines);
  }

  /**
   * Cross-origin merge (D5): every origin's rows are folded into a single time-ordered stream by
   * event timestamp ({@code LogDat}), with origin code then line number as a stable tie-break, so
   * lines from the same origin never swap. Append fast-path: since two writers flush on
   * independent schedules, a merged window can only be appended when the caller's own echoed
   * {@code clientVersion} still matches a fingerprint of the freshly recomputed merge's first
   * {@code offset} entries. This is stateless and per-caller by construction (no server-side
   * cache keyed by execution): concurrent viewers of the same execution each echo the version
   * they themselves last received, so one viewer's poll can never desynchronize another's. A
   * late-arriving line from a slower origin sorting in ahead of content already delivered changes
   * that prefix fingerprint, which is detected here and forces a replace instead of a plain
   * append that would otherwise silently drop it. When appending is unsafe (truncation, a shrunk
   * window, a missing/stale version, or a changed prefix), the full current window is served in
   * replace mode, in the same response. A truncated origin never carries its own in-place marker;
   * instead the omitted counts and totals are summed and surfaced as one aggregate marker at the
   * front of the merged window.
   *
   * @param rowsByOrigin  Rows grouped by origin code
   * @param offset        Client-held offset (line count already displayed)
   * @param clientVersion Fingerprint of the merged order the caller currently holds, as
   *                      previously returned in {@link ExecutionLogPage#version()}
   * @return Execution log page
   */
  private ExecutionLogPage readMerged(Map<String, List<Map<String, CellData>>> rowsByOrigin, int offset, String clientVersion) {
    List<MergedLine> mergedLines = new ArrayList<>();
    long totalLines = 0;
    long omittedLines = 0;

    for (List<Map<String, CellData>> originRows : rowsByOrigin.values()) {
      long headCount = 0;
      long tailCount = 0;
      int maxLineNumber = -1;
      for (Map<String, CellData> row : originRows) {
        int lineNumber = row.get(LINE_NUMBER_COLUMN).getIntegerValue();
        maxLineNumber = Math.max(maxLineNumber, lineNumber);
        long logDateMillis = row.get("logDate").getDateValue().getTime();
        mergedLines.add(new MergedLine(logDateMillis, row.get("origin").getStringValue(), lineNumber, row.get("lineText").getStringValue()));
        if (HEAD_SECTION.equals(row.get("section").getStringValue())) {
          headCount++;
        } else {
          tailCount++;
        }
      }

      long originTotal = maxLineNumber + 1L;
      totalLines += originTotal;
      omittedLines += Math.max(0, originTotal - headCount - tailCount);
    }

    mergedLines.sort(Comparator.comparingLong(MergedLine::logDateMillis)
      .thenComparing(MergedLine::origin)
      .thenComparingInt(MergedLine::lineNumber));

    List<MergedLineId> currentOrder = mergedLines.stream().map(MergedLine::id).toList();
    String currentVersion = fingerprint(currentOrder);

    boolean safeToAppend = offset > 0 && omittedLines == 0
      && clientVersion != null && currentOrder.size() >= offset
      && clientVersion.equals(fingerprint(currentOrder.subList(0, offset)));

    if (safeToAppend) {
      List<String> appended = mergedLines.subList(offset, mergedLines.size()).stream().map(MergedLine::text).toList();
      return new ExecutionLogPage(appended, false, totalLines, omittedLines, currentVersion);
    }

    List<String> displayLines = new ArrayList<>();
    if (omittedLines > 0) {
      displayLines.add(getLocale("SCHEDULER_EXECUTION_LOG_TRUNCATED", String.valueOf(omittedLines), String.valueOf(totalLines)));
    }
    mergedLines.forEach(mergedLine -> displayLines.add(mergedLine.text()));

    return new ExecutionLogPage(displayLines, offset > 0, totalLines, omittedLines, currentVersion);
  }

  /**
   * Cheap, stable fingerprint of an ordered sequence of merged-line identities, used as the
   * client-echoed window version. Order-sensitive by construction ({@link List#hashCode()}
   * combines every element's hash with its position), so any reordering, insertion, or removal
   * within the fingerprinted prefix changes the result.
   *
   * @param order Ordered merged-line identities
   * @return Fingerprint string
   */
  private String fingerprint(List<MergedLineId> order) {
    return String.valueOf(order.hashCode());
  }

  /**
   * Rebuilds the head/marker/tail window for a single-origin execution.
   *
   * @param displayLines Ordered lines for the origin, by line number
   * @param headCount    Number of head rows at the front of {@code displayLines}
   * @param omittedLines Number of lines dropped between head and tail (0 means intact)
   * @param totalLines   Total number of lines the execution actually produced
   * @return Window with the truncation marker inserted, or {@code displayLines} unchanged when intact
   */
  private List<String> withTruncationMarker(List<String> displayLines, long headCount, long omittedLines, long totalLines) {
    if (omittedLines <= 0) {
      return displayLines;
    }

    List<String> window = new ArrayList<>(displayLines.subList(0, (int) headCount));
    window.add(getLocale("SCHEDULER_EXECUTION_LOG_TRUNCATED", String.valueOf(omittedLines), String.valueOf(totalLines)));
    window.addAll(displayLines.subList((int) headCount, displayLines.size()));
    return window;
  }

  private record MergedLine(long logDateMillis, String origin, int lineNumber, String text) {
    MergedLineId id() {
      return new MergedLineId(origin, lineNumber);
    }
  }

  private record MergedLineId(String origin, int lineNumber) {
  }

  @Override
  public ObjectNode locatorNode(ExecutionKey key) {
    ObjectNode logNode = JsonNodeFactory.instance.objectNode();
    logNode.put(JSON_VALUE_PARAMETER, key.mdcKey());
    logNode.put(JSON_STYLE_PARAMETER, "no-btn");
    logNode.put(JSON_TITLE_PARAMETER, "SCHEDULER_SHOW_EXECUTION_LOG");
    logNode.put(JSON_ICON_PARAMETER, "file-text-o text-info");
    logNode.put(JSON_LABEL_PARAMETER, "");
    return logNode;
  }

  @Override
  public void applyViewerSelection(String locatorValue, ExecutionKey key, ServiceData serviceData) {
    serviceData.addClientAction(new ClientAction("reset").setTarget(EXECUTION_LOG_VIEWER_ID).setSilent(true));
    serviceData.addClientAction(new UpdateControllerActionBuilder(EXECUTION_LOG_VIEWER_ID, "serverAction", GET_EXECUTION_LOG_ACTION).setSilent(true).build());
    serviceData.addClientAction(new SelectActionBuilder("execution-log-task-id", key.taskId()).setSilent(true).build());
    serviceData.addClientAction(new SelectActionBuilder("execution-log-execution-id", key.executionId()).setSilent(true).build());
    serviceData.addClientAction(new FilterActionBuilder(EXECUTION_LOG_VIEWER_ID).setSilent(true).build());
  }

  @Override
  public void purge(Integer taskId, Collection<Integer> executionIds) throws AWException {
    if (executionIds.isEmpty()) {
      return;
    }

    ArrayNode taskIds = JsonNodeFactory.instance.arrayNode();
    ArrayNode executionIdList = JsonNodeFactory.instance.arrayNode();
    executionIds.forEach(executionId -> {
      taskIds.add(taskId);
      executionIdList.add(executionId);
    });

    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.set(TASK_ID_PARAMETER, taskIds);
    parameters.set(EXECUTION_ID_PARAMETER, executionIdList);
    maintainService.launchPrivateMaintain(PURGE_EXECUTION_LOG_LINES_TARGET, parameters);
  }

  @Override
  public void purgeOrphans(Set<ExecutionKey> validExecutions) throws AWException {
    ObjectNode keysParameters = queryUtil.getParameters(null, "1", "0");
    DataList dataList = queryService.launchPrivateQuery(EXECUTION_LOG_KEYS_QUERY, keysParameters).getDataList();

    ArrayNode taskIds = JsonNodeFactory.instance.arrayNode();
    ArrayNode executionIdList = JsonNodeFactory.instance.arrayNode();
    for (Map<String, CellData> row : dataList.getRows()) {
      Integer taskId = row.get(TASK_ID_PARAMETER).getIntegerValue();
      Integer executionId = row.get(EXECUTION_ID_PARAMETER).getIntegerValue();
      if (!validExecutions.contains(new ExecutionKey(taskId, executionId))) {
        taskIds.add(taskId);
        executionIdList.add(executionId);
      }
    }

    if (taskIds.isEmpty()) {
      return;
    }

    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.set(TASK_ID_PARAMETER, taskIds);
    parameters.set(EXECUTION_ID_PARAMETER, executionIdList);
    maintainService.launchPrivateMaintain(PURGE_EXECUTION_LOG_LINES_TARGET, parameters);
  }
}
