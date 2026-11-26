package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.enums.ExecutionOrigin;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the queries and services of the activity screen (#993): read only, under the scheduler read
 * permission, bound to the remote capable scheduler service.
 */
class SchedulerActivityDescriptorTest {

  private static final String BASE = "application/awe-scheduler/global/";
  private static final Pattern OPTION = Pattern.compile("<option label=\"([^\"]*)\" value=\"([^\"]*)\"/>");

  @ParameterizedTest
  @ValueSource(strings = {"getActivityExecutionList", "getActivityExecutionRows", "getActivityCounters",
    "getActivityStateCounts", "getActivityRunList", "getActivityRunGroups", "getActivityRunWithoutRun",
    "getActivityRunSkippedRows"})
  void everyActivityQueryRequiresTheSchedulerReadPermission(String id) throws IOException {
    assertTrue(element("Queries.xml", "query", id).contains("permission=\"scheduler.read\""), id);
  }

  @ParameterizedTest
  @ValueSource(strings = {"getActivityExecutionList", "getActivityCounters", "getActivityStateCounts",
    "getActivityRunList"})
  void everyActivityServiceRequiresTheSchedulerReadPermissionAndRunsOnTheRemoteCapableService(String id)
    throws IOException {
    String service = element("Services.xml", "service", id);

    assertTrue(service.contains("permission=\"scheduler.read\""), id);
    assertTrue(service.contains("classname=\"com.almis.awe.scheduler.service.RemoteSchedulerService\""), id);
  }

  @Test
  void theExecutionListIsAPageOfTheServiceWithTheActivityColumns() throws IOException {
    String query = element("Queries.xml", "query", "getActivityExecutionList");

    assertTrue(query.contains("service=\"getActivityExecutionList\""));
    assertTrue(query.contains("managed-pagination=\"true\""));
    for (String field : new String[]{"IdeTsk", "TskNam", "ExeTsk", "ExeRun", "ExeRunLbl", "ExeOri", "ExeStaIco", "ExePrg",
      "ExeDfr", "ExeDue", "ExeDueTim", "ExecutionLogPath"}) {
      assertTrue(query.contains("<field id=\"" + field + "\""), field);
    }
  }

  @Test
  void theListReadsTheStateQuickFilterOfTheGrid() throws IOException {
    assertTrue(element("Queries.xml", "query", "getActivityExecutionList")
      .contains("<variable id=\"state\" type=\"STRINGN\" name=\"CrtExeQck\"/>"));
  }

  @Test
  void theStateCountsAreTheOptionsOfTheQuickFilterCountedWithTheFiltersOfTheListButNotItsState() throws IOException {
    String query = element("Queries.xml", "query", "getActivityStateCounts");

    assertTrue(query.contains("service=\"getActivityStateCounts\""));
    for (String field : new String[]{"value", "label", "count", "tone"}) {
      assertTrue(query.contains("<field id=\"" + field + "\"/>"), field);
    }
    for (String criterion : new String[]{"CrtExeTsk", "CrtExeOri", "CrtExeWin.from", "CrtExeWin.to", "CrtExeWin"}) {
      assertTrue(query.contains("name=\"" + criterion + "\""), criterion);
    }
    for (String criterion : new String[]{"CrtExeQck", "CrtExeSta", "CrtExeDfr", "\"page\"", "\"max\""}) {
      assertFalse(query.contains("name=" + (criterion.startsWith("\"") ? criterion : "\"" + criterion + "\"")),
        criterion);
    }
    assertTrue(element("Services.xml", "service", "getActivityStateCounts")
      .contains("method=\"getExecutionStateCounts\""));
  }

  @Test
  void theExecutionRowsJoinTheTaskAndTheDeferredStateNewestFirst() throws IOException {
    String query = element("Queries.xml", "query", "getActivityExecutionRows");

    assertTrue(query.contains("<table id=\"AweSchTsk\" alias=\"Tsk\"/>"));
    assertTrue(query.contains("<table id=\"AweSchExeExt\" alias=\"Ext\"/>"));
    assertTrue(query.contains("<order-by field=\"IniDat\" table=\"Exe\" type=\"DESC\"/>"));
    // Internal: launched by the service only, never by a client
    assertTrue(query.contains("server-only=\"true\""));
  }

  @Test
  void theActivityStatusHasItsOwnStaticIconsWithAWaitingStatusAndTheOldStatusIconsAreUnchanged() throws IOException {
    String compound = element("Queries.xml", "query", "getActivityExecutionRows");
    compound = compound.substring(compound.indexOf("<compound alias=\"ExeStaIco\">"));
    compound = compound.substring(0, compound.indexOf("</compound>"));

    for (String group : new String[]{"ActStaIco", "ActStaSty", "ActStaTyp"}) {
      assertTrue(compound.contains("translate=\"" + group + "\""), group);
      assertTrue(element("Enumerated.xml", "group", group).contains("value=\"deferred\""), group);
    }
    String icons = element("Enumerated.xml", "group", "ActStaIco");
    assertFalse(icons.contains("spin"), "no status of the activity spins");
    assertTrue(element("Enumerated.xml", "group", "StaIco")
      .contains("<option label=\"fa-refresh fa-spin\" value=\"4\"/>"), "the old screens keep their icons");
  }

  @ParameterizedTest
  @CsvSource({"StaTyp, ActStaTyp", "StaSty, ActStaSty"})
  void theActivityStatusGroupKeepsEveryStatusOfItsSourceGroup(String source, String activity) throws IOException {
    // The old groups lack the info status (7), which only the activity groups add: the shared statuses keep their labels
    Map<String, String> activityOptions = options(activity);
    options(source).forEach((status, label) ->
      assertEquals(label, activityOptions.get(status), activity + " must keep the label of " + source + " " + status));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ActStaIco", "ActStaSty", "ActStaTyp", "ActStaTon"})
  void theActivityStatusGroupCoversEveryTaskStatusPlusTheDeferredOne(String group) throws IOException {
    // The icons differ on purpose (static running and queued icons), the statuses must not
    Set<String> expected = new LinkedHashSet<>();
    for (TaskStatus status : TaskStatus.values()) {
      expected.add(String.valueOf(status.getValue()));
    }
    expected.add("deferred");
    expected.add("skipped");
    // A task of a flow run that has not run yet (the flow run page)
    expected.add("pending");

    Map<String, String> options = options(group);
    assertEquals(expected, options.keySet(), group);
    options.forEach((status, label) -> assertFalse(label.isBlank(), group + " " + status));
  }

  @Test
  void theStatusAndTheOriginOfTheRowsAreTheTextIconAndToneOfTheirStatusCells() throws IOException {
    // The text, icon and tone fields of the status and origin columns (status-field and icon-field of the grid)
    String rows = element("Queries.xml", "query", "getActivityExecutionRows");
    assertTrue(rows.contains("<computed format=\"[Sta]\" alias=\"ExeStaTxt\" translate=\"ActStaTyp\"/>"));
    assertTrue(rows.contains("<computed format=\"[Sta]\" alias=\"ExeStaIcn\" translate=\"ActStaIco\"/>"));
    assertTrue(rows.contains("<computed format=\"[Sta]\" alias=\"ExeStaTon\" translate=\"ActStaTon\"/>"));
    assertTrue(rows.contains("<computed format=\"[TypLch]\" alias=\"ExeOriTxt\" translate=\"LchTxtTyp\"/>"));
    assertTrue(rows.contains("<computed format=\"[TypLch]\" alias=\"ExeOriIcn\" translate=\"LchTxtIco\"/>"));
    String list = element("Queries.xml", "query", "getActivityExecutionList");
    for (String field : new String[]{"ExeStaTxt", "ExeStaIcn", "ExeStaTon", "ExeDueLbl", "ExeOriTxt", "ExeOriIcn",
      "ExeOriTon"}) {
      assertTrue(list.contains("<field id=\"" + field + "\"/>"), field);
    }
  }

  @Test
  void theToneOfEachActivityStatusIsAStatusToneAndTheWaitingOneIsPending() throws IOException {
    Map<String, String> tones = options("ActStaTon");
    Set<String> known = Set.of("success", "warning", "danger", "info", "neutral", "pending");
    tones.forEach((status, tone) -> assertTrue(known.contains(tone), status + " has the unknown tone " + tone));
    assertEquals("pending", tones.get("deferred"));
    assertEquals("success", tones.get("0"));
    assertEquals("danger", tones.get("1"));
    assertEquals("warning", tones.get("2"));
    assertEquals("info", tones.get("4"));
  }

  @Test
  void theRunListIsAPageOfTopLevelRowsOfTheServiceWithTheTreeFieldsAndTheFiltersOfTheList() throws IOException {
    String query = element("Queries.xml", "query", "getActivityRunList");

    assertTrue(query.contains("service=\"getActivityRunList\""));
    assertTrue(query.contains("managed-pagination=\"true\""));
    for (String field : new String[]{"id", "RunPar", "RunLeaf", "RunExeTxt", "IdeTsk", "ExeTsk", "ExeRun",
      "ExecutionLogPath"}) {
      assertTrue(query.contains("<field id=\"" + field + "\""), field);
    }
    // The filters of the toolbar of the runs view
    for (String criterion : new String[]{"CrtRunTsk", "CrtRunQck", "CrtRunOri", "CrtRunWin.from", "CrtRunWin.to",
      "CrtRunWin", "sort"}) {
      assertTrue(query.contains("name=\"" + criterion + "\""), criterion);
    }
    assertTrue(element("Services.xml", "service", "getActivityRunList").contains("method=\"getRunList\""));
    // The page of top-level rows: the runs with a matching execution and the matching executions without run
    String groups = element("Queries.xml", "query", "getActivityRunGroups");
    assertTrue(groups.contains("server-only=\"true\""));
    assertTrue(groups.contains("distinct=\"true\""), "one row per run");
    assertTrue(groups.contains("<table id=\"AweSchExe\" alias=\"Exe\"/>"), "joined to its matching executions");
    assertTrue(groups.contains("<union type=\"ALL\" query=\"getActivityRunWithoutRun\"/>"));
    assertTrue(element("Queries.xml", "query", "getActivityRunSkippedRows").contains("server-only=\"true\""));
  }

  @Test
  void theMetadataQueryCarriesTheEngineStatusCode() throws IOException {
    assertTrue(element("Queries.xml", "query", "getSchedulerMetadata")
      .contains("<field id=\"schedulerStatusCode\" alias=\"schedulerStatusCode\"/>"));
  }

  @Test
  void theOriginEnumeratedListsEveryOriginCode() throws IOException {
    assertEquals(Arrays.stream(ExecutionOrigin.values()).map(ExecutionOrigin::getCode).toList(),
      List.copyOf(options("ExeOri").keySet()));
  }

  /**
   * Options of an enumerated group as value to label, in their order
   */
  private Map<String, String> options(String group) throws IOException {
    Map<String, String> options = new LinkedHashMap<>();
    Matcher option = OPTION.matcher(element("Enumerated.xml", "group", group));
    while (option.find()) {
      options.put(option.group(2), option.group(1));
    }
    return options;
  }

  private String element(String file, String tag, String id) throws IOException {
    String xml = read(file);
    int start = xml.indexOf("<" + tag + " id=\"" + id + "\"");
    assertTrue(start >= 0, id + " must exist in " + file);
    return xml.substring(start, xml.indexOf("</" + tag + ">", start));
  }

  private String read(String file) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BASE + file)) {
      assertNotNull(stream, BASE + file + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
