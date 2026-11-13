package com.almis.awe.test.integration.controller;

import com.almis.awe.service.EncodeService;
import com.almis.awe.service.LogService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.FileSystemUtils;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actions of the tools and settings screens, through the same server actions their grids and widgets launch. They
 * replace these browser flows:
 * <ul>
 *   <li>{@code ApplicationEmailTestsIT.t081_viewLog} (tools/log): the log list finds {@code SCHEDULER.log} by its
 *   name and the log viewer of the selected row returns its content</li>
 *   <li>{@code IntegrationTestsIT.t070_sqlExtractorEngine} (tools/sqlExtractor): {@code select * from awekey} returns
 *   the {@code OpeKey} sequence</li>
 *   <li>the quick-filter counters of {@code ApplicationSettingsTestsIT.t011_addSearchEditAndDeleteSequences} and
 *   {@code t041_addSearchEditAndDeleteParameters}: All, Active and Inactive count the records of the search, and each
 *   count is the number of rows of its option</li>
 * </ul>
 */
@Tag("integration")
@DisplayName("Tools and settings actions tests")
class ToolsAndSettingsActionsTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String LOG_FILE = "SCHEDULER.log";
  private static final String OTHER_FILE = "OTHER.log";
  private static final List<String> LOG_LINES = List.of(
    "2026-11-12 10:00:00.000 INFO [SCHEDULER] Scheduler started",
    "2026-11-12 10:00:01.000 INFO [SCHEDULER] Task tlqk931 launched");
  // Sequence names are varchar(20): the prefix keeps them short and unique to this class
  private static final String PREFIX = "tlqk931";
  private static final String ACTIVE_SEQUENCE = PREFIX + "act";
  private static final String INACTIVE_SEQUENCE = PREFIX + "ina";
  private static final String ACTIVE_PARAMETER = PREFIX + "ParAct";
  private static final String INACTIVE_PARAMETER = PREFIX + "ParIna";
  private static final String INACTIVE_PARAMETER_DESCRIPTION = "Described by Wqz931 words";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private LogService logService;

  private Path logFolder;
  private boolean createdLogPath;

  @AfterEach
  void cleanUp() throws Exception {
    if (logFolder != null) {
      FileSystemUtils.deleteRecursively(createdLogPath ? Path.of(logService.getLogPath()) : logFolder);
    }
    execute("DELETE FROM AweKey WHERE KeyNam LIKE ?", PREFIX + "%");
    execute("DELETE FROM AweAppPar WHERE ParNam LIKE ?", PREFIX + "%");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void logListFindsTheFileByItsNameAndTheViewerReturnsItsContent() throws Exception {
    File log = createLogFiles();
    String encodedPath = EncodeService.encodeSymmetric(log.getAbsolutePath());

    // The name criterion ignores case and leaves out the files whose name does not contain it
    List<Map<String, Object>> rows = rows("getLogList", "{\"CrtFil\":\"scheduler.LOG\"}");
    assertThat(rows).extracting(row -> String.valueOf(row.get("Nam"))).contains(LOG_FILE).doesNotContain(OTHER_FILE);
    Map<String, Object> row = rows.stream()
      .filter(candidate -> encodedPath.equals(candidate.get("Fil")))
      .findFirst().orElseThrow(() -> new AssertionError("No row of " + log + " in " + rows));
    assertThat(row).containsEntry("SrvAct", "getLogText");

    // The viewer (LogViw) sends the path of the selected row and shows the lines from the offset on
    assertThat(logLines(encodedPath, 0)).containsExactlyElementsOf(LOG_LINES);
    assertThat(logLines(encodedPath, 1)).containsExactly(LOG_LINES.get(1));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void sqlExtractorReturnsTheRowsAndTheColumnsOfTheSelect() throws Exception {
    String result = launch("/action/data/selectExtract",
      "{\"selectCriteria\":\"select * from awekey\",\"sqlType\":\"R\"}");

    List<Map<String, Object>> rows = fillRows("selectExtract", result);
    assertThat(rows).anySatisfy(row -> assertThat(row).containsValue("OpeKey"));
    List<String> columns = JsonPath.read(result,
      "$[?(@.type == 'replace-columns' && @.target == 'selectGrid')].parameters.columns[*].name");
    assertThat(columns).extracting(String::toLowerCase).contains("keynam", "keyval", "act");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void sequenceQuickFiltersCountTheSequencesOfTheSearch() throws Exception {
    execute("INSERT INTO AweKey (KeyNam, KeyVal, Act) VALUES (?, 5, 1)", ACTIVE_SEQUENCE);
    execute("INSERT INTO AweKey (KeyNam, KeyVal, Act) VALUES (?, 7, 0)", INACTIVE_SEQUENCE);

    assertCounts("KeyStaCnt", "CrtKeyTxt", PREFIX, 2, 1, 1);
    // The search ignores case and the counters follow it
    assertCounts("KeyStaCnt", "CrtKeyTxt", INACTIVE_SEQUENCE.toUpperCase(), 1, 0, 1);
    assertCountsAreRowsOfTheirOption("KeyStaCnt", "AweKeyLst", "CrtKeyTxt", "CrtKeySta", "KeyNam");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void parameterQuickFiltersCountTheParametersOfTheSearch() throws Exception {
    int id = nextParameterId();
    execute("INSERT INTO AweAppPar (IdeAweAppPar, ParNam, ParVal, Cat, Des, Act) VALUES (?, ?, 'one', 3, 'Active', 1)",
      id, ACTIVE_PARAMETER);
    execute("INSERT INTO AweAppPar (IdeAweAppPar, ParNam, ParVal, Cat, Des, Act) VALUES (?, ?, 'two', 2, ?, 0)",
      id + 1, INACTIVE_PARAMETER, INACTIVE_PARAMETER_DESCRIPTION);

    assertCounts("AppParStaCnt", "CrtAppParTxt", PREFIX, 2, 1, 1);
    // The search reads the description too, ignoring case, and the counters follow it
    assertCounts("AppParStaCnt", "CrtAppParTxt", "wqZ931", 1, 0, 1);
    assertCountsAreRowsOfTheirOption("AppParStaCnt", "AweAppParLst", "CrtAppParTxt", "CrtAppParSta", "ParNam");
  }

  /**
   * The counters query returns All, Active and Inactive in this order with the counts of the search
   */
  private void assertCounts(String countQuery, String search, String text, int all, int active, int inactive)
    throws Exception {
    assertThat(rows(countQuery, "{\"" + search + "\":\"" + text + "\"}"))
      .extracting(row -> String.valueOf(row.get("value")), ToolsAndSettingsActionsTest::count)
      .containsExactly(tuple("", all), tuple("active", active), tuple("inactive", inactive));
  }

  /**
   * Each count is the number of rows the grid shows for its option with the same search
   */
  private void assertCountsAreRowsOfTheirOption(String countQuery, String listQuery, String search, String option,
                                                String nameField) throws Exception {
    String criteria = "\"" + search + "\":\"" + PREFIX + "\"";
    for (Map<String, Object> counter : rows(countQuery, "{" + criteria + "}")) {
      String value = String.valueOf(counter.get("value"));
      List<Map<String, Object>> gridRows = rows(listQuery, "{" + criteria + ",\"" + option + "\":\"" + value + "\"}");
      assertThat(gridRows).as("rows of option '%s'", value).hasSize(count(counter))
        .allSatisfy(row -> assertThat(String.valueOf(row.get(nameField))).startsWith(PREFIX));
    }
  }

  /**
   * Create the log file in a folder of its own under the log path, next to a file whose name does not match
   */
  private File createLogFiles() throws Exception {
    Path logPath = Path.of(logService.getLogPath());
    createdLogPath = Files.notExists(logPath);
    logFolder = Files.createDirectories(logPath.resolve(PREFIX));
    Files.writeString(logFolder.resolve(OTHER_FILE), "other", StandardCharsets.UTF_8);
    return Files.write(logFolder.resolve(LOG_FILE), LOG_LINES, StandardCharsets.UTF_8).toFile();
  }

  private List<String> logLines(String encodedPath, int offset) throws Exception {
    String result = launch("/action/get-log-file", "{\"path\":\"" + encodedPath + "\",\"offset\":" + offset + "}");
    List<List<String>> logs = JsonPath.read(result, "$[?(@.type == 'log-delta')].parameters.log");
    assertThat(logs).as("log-delta of %s", result).hasSize(1);
    return logs.getFirst();
  }

  private List<Map<String, Object>> rows(String query, String parameters) throws Exception {
    return fillRows(query, launch("/action/data/" + query, parameters));
  }

  private static List<Map<String, Object>> fillRows(String query, String result) {
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows");
    assertThat(rows).as("fill of %s: %s", query, result).hasSize(1);
    return rows.getFirst();
  }

  private String launch(String url, String parameters) throws Exception {
    return mockMvc.perform(post(url)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(parameters)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private static int count(Map<String, Object> row) {
    return Integer.parseInt(String.valueOf(row.get("count")));
  }

  private MockHttpSession signedInSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user")
        .with(csrf())
        .param("value", SIGNED_IN_USER)
        .session(session))
      .andReturn();
    return session;
  }

  private int nextParameterId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeAweAppPar) FROM AweAppPar");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void execute(String sql, Object... values) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < values.length; index++) {
        statement.setObject(index + 1, values[index]);
      }
      statement.executeUpdate();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }
}
