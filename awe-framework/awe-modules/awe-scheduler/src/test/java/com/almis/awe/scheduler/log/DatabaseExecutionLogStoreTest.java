package com.almis.awe.scheduler.log;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link DatabaseExecutionLogStore}: locator/viewer-selection contracts, window
 * reconstruction from {@code AweSchExeLog} rows, and purge/orphan-purge call construction. The
 * no-op {@link ExecutionLogWriter} stub proves no maintain call is issued for append/complete.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseExecutionLogStoreTest {

  private DatabaseExecutionLogStore store;

  @Mock
  private QueryService queryService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private ExecutionLogWriter writer;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void initStore() {
    store = new DatabaseExecutionLogStore(queryService, maintainService, queryUtil, writer);
    store.setApplicationContext(context);
  }

  @Test
  void appendDelegatesToTheWriter() {
    ExecutionLogLine line = new ExecutionLogLine(new ExecutionKey(1, 1), ExecutionLogOrigin.SCHEDULER, 123L, "line");

    store.append(line);

    verify(writer).append(line);
    verifyNoInteractions(maintainService);
  }

  @Test
  void completeDelegatesToTheWriter() {
    ExecutionKey key = new ExecutionKey(1, 1);

    store.complete(key, ExecutionLogOrigin.SCHEDULER);

    verify(writer).complete(key, ExecutionLogOrigin.SCHEDULER);
    verifyNoInteractions(maintainService);
  }

  @Test
  void locatorNodeEmitsTheOpaqueTokenAndNoPath() {
    ObjectNode node = store.locatorNode(new ExecutionKey(12, 34));

    assertEquals("12-34", node.get("value").asText());
    assertEquals("no-btn", node.get("style").asText());
    assertEquals("SCHEDULER_SHOW_EXECUTION_LOG", node.get("title").asText());
    assertEquals("file-text-o text-info", node.get("icon").asText());
  }

  @Test
  void applyViewerSelectionTargetsTheSharedViewerAndHiddenIdCriteria() {
    ServiceData serviceData = new ServiceData();

    store.applyViewerSelection("5-9", new ExecutionKey(5, 9), serviceData);

    List<ClientAction> actions = serviceData.getClientActionList();
    assertEquals(5, actions.size());
    assertEquals("reset", actions.get(0).getType());
    assertEquals("executionLogViewer", actions.get(0).getTarget());
    assertEquals("update-controller", actions.get(1).getType());
    assertEquals("executionLogViewer", actions.get(1).getTarget());
    assertEquals("select", actions.get(2).getType());
    assertEquals("execution-log-task-id", actions.get(2).getTarget());
    assertEquals("select", actions.get(3).getType());
    assertEquals("execution-log-execution-id", actions.get(3).getTarget());
    assertEquals("filter", actions.get(4).getType());
    assertEquals("executionLogViewer", actions.get(4).getTarget());
    assertTrue(actions.stream().noneMatch(action -> "path".equals(action.getTarget())));
  }

  /**
   * offset 0 means an empty client: a full window is always append mode, even when the stored
   * window is truncated. The full window (marker included) is delivered as plain content.
   */
  @Test
  void readAtOffsetZeroDeliversTheFullTruncatedWindowAsAnAppend() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(), any())).willReturn("TRUNCATED");
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    // logDate values are deliberately NOT monotonic with lineNumber: the single-origin path
    // must order by lineNumber alone, never by logDate (byte-for-byte base ADR-6 preservation).
    dataList.addRow(logRow("S", "T", 12, "tail2", 1_000L));
    dataList.addRow(logRow("S", "H", 0, "head0", 5_000L));
    dataList.addRow(logRow("S", "T", 10, "tail0", 2_000L));
    dataList.addRow(logRow("S", "H", 1, "head1", 4_000L));
    dataList.addRow(logRow("S", "T", 11, "tail1", 3_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 0, null);

    assertFalse(page.replace());
    assertEquals(13L, page.totalLines());
    assertEquals(8L, page.omittedLines());
    assertEquals(List.of("head0", "head1", "TRUNCATED", "tail0", "tail1", "tail2"), page.lines());
  }

  /**
   * A non-zero offset against a truncated window is never safe to append: the truncation marker
   * position itself shifts as new lines get omitted. The full current window (marker included) is
   * delivered in replace mode, in the same response, instead of the old empty-reset round trip.
   */
  @Test
  void readAtNonZeroOffsetReplacesWithTheFullWindowWhenTheWindowIsTruncated() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(), any())).willReturn("TRUNCATED");
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "T", 12, "tail2", 1_000L));
    dataList.addRow(logRow("S", "H", 0, "head0", 5_000L));
    dataList.addRow(logRow("S", "T", 10, "tail0", 2_000L));
    dataList.addRow(logRow("S", "H", 1, "head1", 4_000L));
    dataList.addRow(logRow("S", "T", 11, "tail1", 3_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 6, null);

    assertTrue(page.replace());
    assertEquals(13L, page.totalLines());
    assertEquals(8L, page.omittedLines());
    assertEquals(List.of("head0", "head1", "TRUNCATED", "tail0", "tail1", "tail2"), page.lines());
  }

  @Test
  void readAppendsFromOffsetWhenTheWindowIsIntact() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "H", 0, "line0", 1_000L));
    dataList.addRow(logRow("S", "H", 1, "line1", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 1, null);

    assertFalse(page.replace());
    assertEquals(2L, page.totalLines());
    assertEquals(0L, page.omittedLines());
    assertEquals(List.of("line1"), page.lines());
  }

  @Test
  void readReplacesWithTheFullWindowWhenTheOffsetExceedsTheAvailableWindow() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "H", 0, "line0", 1_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 5, null);

    assertTrue(page.replace());
    assertEquals(List.of("line0"), page.lines());
  }

  @Test
  void readReturnsEmptyPageWhenNoRowsAreStored() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 0, null);

    assertEquals(ExecutionLogPage.empty(), page);
  }

  @Test
  void readNeverConsultsTheWriterSInMemoryState() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "H", 0, "line0", 1_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    store.read(new ExecutionKey(1, 1), 0, null);

    verifyNoInteractions(writer);
  }

  @Test
  void twoOriginReadMergesRowsByEventTimestampWithStableOriginTieBreak() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    dataList.addRow(logRow("A", "H", 0, "app0", 1_000L)); // same millisecond: origin code breaks the tie
    dataList.addRow(logRow("S", "H", 1, "sched1", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 0, null);

    assertFalse(page.replace());
    assertEquals(List.of("app0", "sched0", "sched1"), page.lines());
  }

  @Test
  void mergedOrderNeverSwapsTwoLinesFromTheSameOrigin() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(logRow("S", "H", 0, "sched0", 5_000L));
    dataList.addRow(logRow("S", "H", 1, "sched1", 5_000L)); // identical logDate: lineNumber breaks the tie
    dataList.addRow(logRow("A", "H", 0, "app0", 4_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 0, null);

    assertEquals(List.of("app0", "sched0", "sched1"), page.lines());
  }

  /**
   * A merged (multi-origin) window can be appended incrementally as long as the caller's own
   * echoed version still matches a fingerprint of the freshly recomputed merge's held prefix:
   * nothing sorted in ahead of what the caller already has. The first read returns the window's
   * version; the second read echoes it back at the offset the first read returned, and gets only
   * the new line appended instead of a replace.
   */
  @Test
  void multiOriginReadAppendsWhenTheMergedOrderIsStillAStablePrefix() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList firstDataList = new DataList();
    firstDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    firstDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(firstDataList));
    ExecutionLogPage firstPage = store.read(new ExecutionKey(1, 1), 0, null);
    assertFalse(firstPage.replace());
    assertEquals(List.of("sched0", "app0"), firstPage.lines());

    DataList secondDataList = new DataList();
    secondDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    secondDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    secondDataList.addRow(logRow("S", "H", 1, "sched1", 3_000L)); // strictly newer than everything served
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(secondDataList));

    ExecutionLogPage secondPage = store.read(new ExecutionKey(1, 1), firstPage.lines().size(), firstPage.version());

    assertFalse(secondPage.replace());
    assertEquals(List.of("sched1"), secondPage.lines());
  }

  /**
   * A late-arriving line from a slower origin can sort earlier than lines already delivered
   * (independent flush schedules across origins). Appending naively would silently drop it from
   * ever being displayed, so the store detects the reordering (the echoed version no longer
   * matches the freshly recomputed prefix) and replaces with the full window instead, in the same
   * response.
   */
  @Test
  void multiOriginReadReplacesWhenALateArrivingLineWouldSortBeforeAlreadyDeliveredContent() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList firstDataList = new DataList();
    firstDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    firstDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(firstDataList));
    ExecutionLogPage firstPage = store.read(new ExecutionKey(1, 1), 0, null);
    assertEquals(List.of("sched0", "app0"), firstPage.lines());

    DataList secondDataList = new DataList();
    secondDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    secondDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    // Late flush from the application origin: timestamp sorts before the already-delivered "app0".
    secondDataList.addRow(logRow("A", "H", 1, "app1-late", 1_500L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(secondDataList));

    ExecutionLogPage secondPage = store.read(new ExecutionKey(1, 1), firstPage.lines().size(), firstPage.version());

    assertTrue(secondPage.replace());
    assertEquals(List.of("sched0", "app1-late", "app0"), secondPage.lines());
  }

  /**
   * The append fast-path is per-caller, not per-execution: two independent viewers of the same
   * execution each echo the version they themselves last received, so one viewer's poll can never
   * leak into another's decision. A late-arriving mid-stream line changes the merged order; both
   * viewers, having each held the pre-insert order, must independently get a full replace on their
   * next poll — never a duplicate line (from wrongly trusting the other viewer's freshly-updated
   * order) and never a silently dropped line.
   */
  @Test
  void concurrentViewersEachEchoingTheirOwnVersionNeverGetADuplicateOrMissingLine() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList initialDataList = new DataList();
    initialDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    initialDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(initialDataList));

    // Both viewers independently load the same initial window.
    ExecutionLogPage viewerAFirstPage = store.read(new ExecutionKey(1, 1), 0, null);
    ExecutionLogPage viewerBFirstPage = store.read(new ExecutionKey(1, 1), 0, null);
    assertEquals(List.of("sched0", "app0"), viewerAFirstPage.lines());
    assertEquals(List.of("sched0", "app0"), viewerBFirstPage.lines());

    // A late-arriving line from the application origin sorts in ahead of "app0" (mid-stream
    // insertion), reordering the merge between the two viewers' next polls.
    DataList reorderedDataList = new DataList();
    reorderedDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    reorderedDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    reorderedDataList.addRow(logRow("A", "H", 1, "app-late", 1_500L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(reorderedDataList));

    // Viewer B polls first, echoing its own held version.
    ExecutionLogPage viewerBSecondPage = store.read(new ExecutionKey(1, 1), 2, viewerBFirstPage.version());
    assertTrue(viewerBSecondPage.replace());
    assertEquals(List.of("sched0", "app-late", "app0"), viewerBSecondPage.lines());

    // Viewer A polls next, echoing ITS OWN (unrelated) held version: a global "last delivered
    // order" cache updated by viewer B's read would wrongly consider this safe to append,
    // producing a duplicate "app0" and never surfacing "app-late" to viewer A at all.
    ExecutionLogPage viewerASecondPage = store.read(new ExecutionKey(1, 1), 2, viewerAFirstPage.version());
    assertTrue(viewerASecondPage.replace());
    assertEquals(List.of("sched0", "app-late", "app0"), viewerASecondPage.lines());
  }

  @Test
  void multiOriginTruncationAtOffsetZeroProducesOneAggregateMarkerAtTheFrontOfThePlainWindow() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(), any())).willReturn("TRUNCATED");
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    // scheduler origin: lines 0 and 5 stored, lines 1-4 omitted -> total=6, omitted=4
    dataList.addRow(logRow("S", "H", 0, "sched-head0", 1_000L));
    dataList.addRow(logRow("S", "T", 5, "sched-tail0", 6_000L));
    // application origin: complete window, no omission -> total=2, omitted=0
    dataList.addRow(logRow("A", "H", 0, "app-head0", 2_000L));
    dataList.addRow(logRow("A", "H", 1, "app-head1", 3_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    ExecutionLogPage page = store.read(new ExecutionKey(1, 1), 0, null);

    assertFalse(page.replace());
    assertEquals(8L, page.totalLines());
    assertEquals(4L, page.omittedLines());
    assertEquals(
      List.of("TRUNCATED", "sched-head0", "app-head0", "app-head1", "sched-tail0"),
      page.lines());
  }

  /**
   * A window that becomes truncated between two reads is never safe to append: the marker
   * position and the kept lines both shift. The store replaces with the full current window
   * (marker included) in the same response, instead of the old empty-reset round trip.
   */
  @Test
  void multiOriginReplacesWithTheFullWindowWhenTruncationStartsBetweenTwoReads() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(), any())).willReturn("TRUNCATED");
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList firstDataList = new DataList();
    firstDataList.addRow(logRow("S", "H", 0, "sched0", 1_000L));
    firstDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(firstDataList));
    ExecutionLogPage firstPage = store.read(new ExecutionKey(1, 1), 0, null);
    assertEquals(List.of("sched0", "app0"), firstPage.lines());

    DataList truncatedDataList = new DataList();
    // scheduler origin drops its own head, becoming truncated between the two reads
    truncatedDataList.addRow(logRow("S", "T", 5, "sched-tail", 6_000L));
    truncatedDataList.addRow(logRow("A", "H", 0, "app0", 2_000L));
    given(queryService.launchPrivateQuery(eq("getExecutionLogLines"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(truncatedDataList));

    ExecutionLogPage replacedPage = store.read(new ExecutionKey(1, 1), firstPage.lines().size(), firstPage.version());

    assertTrue(replacedPage.replace());
    assertEquals(List.of("TRUNCATED", "app0", "sched-tail"), replacedPage.lines());
  }

  @Test
  void purgeLaunchesPurgeExecutionLogLinesWithParallelTaskAndExecutionArrays() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);

    store.purge(7, List.of(1, 2));

    verify(maintainService).launchPrivateMaintain(eq("purgeExecutionLogLines"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertEquals("[7,7]", parameters.get("taskId").toString());
    assertEquals("[1,2]", parameters.get("executionId").toString());
  }

  @Test
  void purgeWithNoExecutionsIssuesNoMaintainCall() throws Exception {
    store.purge(7, List.of());

    verifyNoInteractions(maintainService);
  }

  /**
   * The startup orphan purge must never let the default query page size silently truncate the
   * live-execution list: an unpaginated {@code getExecutionLogKeys} read can leave more than a
   * page's worth of rows unseen, causing live rows to be mistaken for orphans and purged. Forcing
   * max=0/page=1 here mirrors the same forcing already applied to {@code getExecutionLogLines}.
   */
  @Test
  void purgeOrphansDeletesOnlyTheKeysMissingFromTheValidSet() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(keyRow(1, 1));
    dataList.addRow(keyRow(1, 2));
    given(queryService.launchPrivateQuery(eq("getExecutionLogKeys"), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);

    store.purgeOrphans(Set.of(new ExecutionKey(1, 1)));

    verify(queryService).launchPrivateQuery(eq("getExecutionLogKeys"), any(ObjectNode.class));
    verify(maintainService).launchPrivateMaintain(eq("purgeExecutionLogLines"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertEquals("[1]", parameters.get("taskId").toString());
    assertEquals("[2]", parameters.get("executionId").toString());
  }

  @Test
  void purgeOrphansWithNoOrphansIssuesNoMaintainCall() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    dataList.addRow(keyRow(1, 1));
    given(queryService.launchPrivateQuery(eq("getExecutionLogKeys"), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));

    store.purgeOrphans(Set.of(new ExecutionKey(1, 1)));

    verifyNoInteractions(maintainService);
  }

  private Map<String, CellData> logRow(String origin, String section, int lineNumber, String lineText, long logDateMillis) {
    Map<String, CellData> row = new HashMap<>();
    row.put("origin", new CellData(origin));
    row.put("section", new CellData(section));
    row.put("slot", new CellData(lineNumber));
    row.put("lineNumber", new CellData(lineNumber));
    row.put("lineText", new CellData(lineText));
    row.put("logDate", new CellData(new Date(logDateMillis)));
    return row;
  }

  private Map<String, CellData> keyRow(int taskId, int executionId) {
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(taskId));
    row.put("executionId", new CellData(executionId));
    return row;
  }
}
