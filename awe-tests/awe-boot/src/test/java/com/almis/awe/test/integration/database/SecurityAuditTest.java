package com.almis.awe.test.integration.database;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.audit.DatabaseSecurityAuditSink;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security audit on every supported database: events written by real logins and logouts, the queries of the
 * security audit screen (filters, daily aggregation and failures by cause) and the retention purge
 */
@DisplayName("Security audit tests")
public abstract class SecurityAuditTest extends AbstractSpringAppIntegrationTest {

  private static final DateTimeFormatter SCREEN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Autowired
  private DataSource dataSource;

  @Autowired
  private DatabaseSecurityAuditSink databaseSink;

  // Events written by the test are recognized by their user name and deleted afterwards
  private final String auditUser = "audit-it-" + UUID.randomUUID().toString().substring(0, 8);
  private final List<String> loginUsers = new ArrayList<>();

  @AfterEach
  void deleteTestEvents() throws Exception {
    List<String> users = new ArrayList<>(loginUsers);
    users.add(auditUser);
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweSecAudEvt WHERE UsrNam = ? AND EvtDat >= ?")) {
      for (String user : users) {
        statement.setString(1, user);
        statement.setTimestamp(2, java.sql.Timestamp.from(Instant.now().minus(800, ChronoUnit.DAYS)));
        statement.executeUpdate();
      }
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }

  @Test
  @DisplayName("Failed and successful logins are stored with their cause and the request data")
  void loginsAreStored() throws Exception {
    loginUsers.add("test");
    Instant start = Instant.now().minusSeconds(1);

    login(new MockHttpSession(), "test", "wrong-password");
    login(new MockHttpSession(), "test", "test");

    List<Map<String, String>> events = storedEvents("test", start);
    assertThat(events).extracting(row -> row.get("EVTTYP") + "/" + row.get("EVTRES") + "/" + row.get("EVTCAU"))
      .containsSubsequence("LOGIN/FAILURE/BAD_CREDENTIALS", "LOGIN/SUCCESS/null");
    Map<String, String> failure = events.getFirst();
    assertThat(failure.get("CHN")).isEqualTo("WEB");
    assertThat(failure.get("IPADR")).isEqualTo("127.0.0.1");
    assertThat(failure.get("SESHSH")).hasSize(64);
    assertThat(failure.get("CORIDE")).isNotBlank();
    assertThat(failure.get("IDESECAUD")).hasSize(36);
  }

  @Test
  @DisplayName("An explicit logout is stored")
  void logoutIsStored() throws Exception {
    loginUsers.add("test");
    Instant start = Instant.now().minusSeconds(1);
    MockHttpSession session = new MockHttpSession();
    login(session, "test", "test");

    mockMvc.perform(post("/action/logout").with(csrf()).session(session)).andReturn();

    assertThat(storedEvents("test", start))
      .extracting(row -> row.get("EVTTYP") + "/" + row.get("EVTCAU"))
      .contains("LOGOUT/EXPLICIT");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The screen queries filter, aggregate per day and group the failures by cause")
  void screenQueries() throws Exception {
    Instant today = Instant.now();
    Instant yesterday = today.minus(1, ChronoUnit.DAYS);
    store(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.SUCCESS, null, yesterday);
    store(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS, today);
    store(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS, today);
    store(SecurityAuditEventType.ACCESS_DENIED, SecurityAuditOutcome.FAILURE, SecurityAuditCause.RESTRICTED, today);
    String from = LocalDate.now(ZoneId.systemDefault()).minusDays(3).format(SCREEN_DATE);
    String to = LocalDate.now(ZoneId.systemDefault()).format(SCREEN_DATE);
    assertThat(storedEvents(auditUser, yesterday.minus(1, ChronoUnit.HOURS))).as("events stored").hasSize(4);
    String criteria = "\"CrtUsr\":\"" + auditUser + "\",\"CrtFro\":\"" + from + "\",\"CrtTo\":\"" + to + "\"";

    List<JsonNode> events = rows(data("SecAudEvtLst", criteria));
    assertThat(events).hasSize(4);
    assertThat(events.getFirst().path("EvtDat").asString()).isNotBlank();
    assertThat(events).extracting(row -> row.path("TypTxt").asString()).contains("Login", "Access denied");
    assertThat(events).extracting(row -> row.path("ResIco").path("icon").asString()).contains("fa-check-circle", "fa-times-circle");

    List<JsonNode> onlyDenied = rows(data("SecAudEvtLst", criteria + ",\"CrtTyp\":[\"ACCESS_DENIED\"]"));
    assertThat(onlyDenied).singleElement().satisfies(row -> assertThat(row.path("CauTxt").asString()).isEqualTo("Restricted option"));

    List<JsonNode> activity = rows(data("SecAudActChr", criteria));
    assertThat(activity).hasSize(2);
    assertThat(activity).extracting(row -> row.path("success").asInt() + "/" + row.path("failure").asInt())
      .containsExactly("1/0", "0/3");

    Map<String, Integer> causes = new HashMap<>();
    rows(data("SecAudCauChr", criteria)).forEach(row -> causes.put(row.path("names").asString(), row.path("serie1").asInt()));
    assertThat(causes).containsEntry("Wrong user or password", 2).containsEntry("Restricted option", 1).hasSize(2);
  }

  @Test
  @DisplayName("The purge deletes only the events older than the limit")
  void purge() throws Exception {
    store(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.SUCCESS, null, Instant.now().minus(400, ChronoUnit.DAYS));
    store(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.SUCCESS, null, Instant.now());

    databaseSink.purge(Instant.now().minus(365, ChronoUnit.DAYS));

    assertThat(storedEvents(auditUser, Instant.now().minus(800, ChronoUnit.DAYS))).hasSize(1);
  }

  private void store(SecurityAuditEventType type, SecurityAuditOutcome outcome, SecurityAuditCause cause, Instant when) {
    databaseSink.record(SecurityAuditEvent.builder().id(UUID.randomUUID().toString()).timestamp(when).type(type)
      .outcome(outcome).cause(cause).username(auditUser).channel(SecurityAuditChannel.WEB).ipAddress("10.0.0.1").build());
  }

  private void login(MockHttpSession session, String user, String password) throws Exception {
    mockMvc.perform(post("/action/login")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cod_usr\":\"" + user + "\",\"pwd_usr\":\"" + password + "\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private String data(String query, String criteria) throws Exception {
    return mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .content("{" + criteria + ",\"max\":50,\"page\":1}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private List<JsonNode> rows(String response) {
    JsonNode actions = objectMapper.readTree(response);
    List<JsonNode> rows = new ArrayList<>();
    for (JsonNode action : actions) {
      JsonNode found = action.findPath("rows");
      if (found.isArray()) {
        found.forEach(rows::add);
        return rows;
      }
    }
    throw new AssertionError("No data in " + response);
  }

  /**
   * Stored events of a user since a moment, oldest first, with upper-case column names
   */
  private List<Map<String, String>> storedEvents(String user, Instant since) throws Exception {
    List<Map<String, String>> events = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT IdeSecAud, EvtTyp, EvtRes, EvtCau, Chn, IpAdr, SesHsh, CorIde FROM AweSecAudEvt WHERE UsrNam = ? AND EvtDat >= ? ORDER BY EvtDat")) {
      statement.setString(1, user);
      statement.setTimestamp(2, java.sql.Timestamp.from(since));
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          Map<String, String> row = new HashMap<>();
          for (int column = 1; column <= resultSet.getMetaData().getColumnCount(); column++) {
            row.put(resultSet.getMetaData().getColumnLabel(column).toUpperCase(), resultSet.getString(column));
          }
          events.add(row);
        }
      }
    }
    return events;
  }
}
