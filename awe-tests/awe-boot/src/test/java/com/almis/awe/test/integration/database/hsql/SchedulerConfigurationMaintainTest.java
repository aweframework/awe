package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.dao.ServerDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Writes of the servers of the scheduler configuration screen (#993) on a real database, through the action controller
 * as the screen launches them: a server used by a task is never deleted and its refusal names the tasks; a free server
 * is deleted; «Quitar» removes the saved private key with its passphrase and keeps the password; the side panel
 * replaces the secrets typed in it whatever they hold (lines, quotes, backslashes) and keeps a blank one; «Probar todos»
 * tests the active servers with a connection; the last test of a server is read back in the session that ran it only. Servers and tasks of their own, far from the ids of the sequences and
 * outside Quartz, are stored and removed after each test.
 */
@Tag("integration")
class SchedulerConfigurationMaintainTest extends AbstractSchedulerMaintainTest {

  private static final int USED = 9411;
  private static final int FREE = 9412;
  private static final int INACTIVE = 9413;
  private static final int FOLDER = 9414;
  private static final int TASK = 9411;
  private static final String SEARCH = "config maintain probe";

  @Autowired
  private DataSource dataSource;
  @Autowired
  private ServerDAO serverDAO;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeServers() {
    jdbc = new JdbcTemplate(dataSource);
    deleteServers();
    for (int server : new int[]{USED, FREE}) {
      jdbc.update("INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act) VALUES (?, ?, 'ssh', 'probe.local', 22, 1)",
        server, "config maintain probe " + server);
    }
    jdbc.update("INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act) VALUES (?, ?, 'ssh', 'probe.local', 22, 0)",
      INACTIVE, SEARCH + " " + INACTIVE);
    jdbc.update("INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act) VALUES (?, ?, 'folder', 'localhost', 0, 1)",
      FOLDER, SEARCH + " " + FOLDER);
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, IdeSrvExe, CmdExe, TypLch, Act) "
      + "VALUES (?, 'Config Maintain Probe', 'probe', 0, ?, 'probe', 0, 1)", TASK, USED);
  }

  @AfterEach
  void deleteServers() {
    jdbc.update("DELETE FROM AweSchTsk WHERE Ide = ?", TASK);
    jdbc.update("DELETE FROM AweSchSrv WHERE Ide IN (?, ?, ?, ?)", USED, FREE, INACTIVE, FOLDER);
  }

  @Test
  void aServerUsedByATaskIsNeverDeletedAndTheRefusalNamesTheTask() throws Exception {
    String refusal = "Cannot be deleted: 1 task uses it (Config Maintain Probe). Change the server of that task first.";

    List<String> configuration = errorMessages(perform("maintain", "DeleteConfigurationServer",
      selectedRow("IdeSrv", USED)));

    assertThat(configuration).containsExactly(refusal);
    assertThat(storedServers()).containsExactly(USED, FREE);
  }

  @Test
  void aFreeServerIsDeletedFromTheConfigurationScreen() throws Exception {
    maintain("DeleteConfigurationServer", selectedRow("IdeSrv", FREE));

    // Only the selected row: the other free server of the grid stays
    assertThat(storedServers()).containsExactly(USED);
    assertThat(jdbc.queryForList("SELECT Ide FROM AweSchSrv WHERE Ide = ?", Integer.class, INACTIVE))
      .containsExactly(INACTIVE);
  }

  @Test
  void activateFromTheConfigurationScreenActivatesTheSelectedServerOnly() throws Exception {
    jdbc.update("UPDATE AweSchSrv SET Act = 0 WHERE Ide = ?", FOLDER);

    maintain("ActSchSrv", selectedRow("Ide", INACTIVE));

    assertThat(jdbc.queryForList("SELECT Ide FROM AweSchSrv WHERE Ide IN (?, ?) AND Act = 1 ORDER BY Ide",
      Integer.class, INACTIVE, FOLDER)).containsExactly(INACTIVE);
  }

  @Test
  void removingThePrivateKeyRemovesItsPassphraseAndKeepsThePassword() throws Exception {
    String name = unique("Key removal");
    maintain("NewSchedulerServer", params().put("Nom", name).put("Pro", "ssh").put("Hst", "10.0.0.3").put("Prt", 22)
      .put("Act", 1).put("SshUsr", "admin").put("SshPwd", "password0").put("SshKey", "key0")
      .put("SshKeyPass", "passphrase0"));
    int serverId = storedServer(name);
    try {
      maintain("UpdSchSrv", params().put("IdeSrv", serverId).put("Nom", name).put("Pro", "ssh").put("Hst", "10.0.0.3")
        .put("Prt", 22).put("Act", 1).put("SshUsr", "admin").put("SshPwd", "").put("SshKey", "").put("SshKeyPass", "")
        .put("FlgSshPwdUpd", 0).put("FlgSshKeyUpd", 0).put("FlgSshKeyPassUpd", 0).put("FlgSshKeyDel", 1));

      Server server = serverDAO.findServer(serverId);
      assertThat(server.getKey()).isNull();
      assertThat(server.getKeyPassphrase()).isNull();
      assertThat(server.getPassword()).isEqualTo("password0");
    } finally {
      jdbc.update("DELETE FROM AweSchSrv WHERE Ide = ?", serverId);
    }
  }

  @Test
  void theSidePanelReplacesTheSecretsTypedInItAndKeepsTheBlankOne() throws Exception {
    String name = unique("Panel secrets");
    maintain("NewSchedulerServer", params().put("Nom", name).put("Pro", "ssh").put("Hst", "10.0.0.5").put("Prt", 22)
      .put("Act", 1).put("SshUsr", "admin").put("SshPwd", "password0").put("SshKey", "key0")
      .put("SshKeyPass", "passphrase0"));
    int serverId = storedServer(name);
    String password = "it's a \"quoted\" \\ password";
    // A fake key, only its format with quotes and backslashes; the header is split so it does not look like a real key
    String key = "-----BEGIN OPENSSH " + "PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAA'quoted'\n\"AAAA\\BBBB\"\n"
      + "-----END OPENSSH " + "PRIVATE KEY-----";
    try {
      // What the panel sends: the server of the selected row of the grid, the typed password and key with their update
      // flags at 1, the blank passphrase with its flag at 0
      maintain("UpdSchSrv", selectedRow("IdeSrv", serverId).put("Nom", name).put("Pro", "ssh").put("Hst", "10.0.0.5")
        .put("Prt", 22).put("Act", 1).put("SshUsr", "admin").put("SshPwd", password).put("SshKey", key)
        .put("SshKeyPass", "").put("FlgSshPwdUpd", 1).put("FlgSshKeyUpd", 1).put("FlgSshKeyPassUpd", 0)
        .put("FlgSshKeyDel", 0));

      Server server = serverDAO.findServer(serverId);
      assertThat(server.getPassword()).isEqualTo(password);
      assertThat(server.getKey()).isEqualTo(key);
      assertThat(server.getKeyPassphrase()).isEqualTo("passphrase0");
    } finally {
      jdbc.update("DELETE FROM AweSchSrv WHERE Ide = ?", serverId);
    }
  }

  @Test
  void theServersToTestAllAreTheActiveOnesWithAConnection() throws Exception {
    assertThat(serverDAO.findActiveServerIds()).contains(USED, FREE).doesNotContain(INACTIVE, FOLDER);
  }

  @Test
  void theLastTestOfAServerIsReadBackInItsSessionOnly() throws Exception {
    // A local port that nobody listens to: the test fails at once, without leaving the machine
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    jdbc.update("UPDATE AweSchSrv SET Hst = '127.0.0.1', Prt = ? WHERE Ide = ?", closedPort, FREE);
    MockHttpSession tested = new MockHttpSession();

    perform(tested, "maintain", "TstSchSrvCnxSel", params().put("IdeSrv.selected", FREE));

    JsonNode inSession = find(serverRows(tested), "IdeSrv", String.valueOf(FREE)).orElseThrow();
    JsonNode otherSession = find(serverRows(new MockHttpSession()), "IdeSrv", String.valueOf(FREE)).orElseThrow();
    assertThat(inSession.get("SrvTstSta").asString()).isEqualTo("error");
    assertThat(inSession.get("SrvTstSub").asString()).endsWith(" · cannot connect");
    assertThat(otherSession.get("SrvTstSta").asString()).isEqualTo("none");
  }

  /**
   * Rows of the servers of the configuration screen seen from a session
   */
  private List<JsonNode> serverRows(MockHttpSession session) throws Exception {
    JsonNode actions = perform(session, "data", "getConfigurationServers", params().put("CrtSrvSch", SEARCH));
    for (JsonNode action : actions) {
      if ("fill".equals(action.get("type").asString())) {
        List<JsonNode> rows = new ArrayList<>();
        action.get("parameters").get("datalist").get("rows").forEach(rows::add);
        return rows;
      }
    }
    throw new AssertionError("No servers: " + actions);
  }

  private JsonNode perform(MockHttpSession session, String type, String name, ObjectNode parameters)
    throws Exception {
    String response = mockMvc.perform(post("/action/" + type + "/" + name)
        .session(session)
        .with(csrf())
        .content(objectMapper.writeValueAsString(parameters))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response);
  }

  /**
   * A column of the servers grid as the grid sends it with one selected row (no send-all): the column carries the
   * values of the selected rows only, and .selected the value of the selected row
   */
  private ObjectNode selectedRow(String column, int server) {
    return params().<ObjectNode>set(column, ints(server)).put(column + ".selected", server);
  }

  private int storedServer(String name) {
    return jdbc.queryForObject("SELECT Ide FROM AweSchSrv WHERE Nom = ?", Integer.class, name);
  }

  private List<Integer> storedServers() {
    return jdbc.queryForList("SELECT Ide FROM AweSchSrv WHERE Ide IN (?, ?) ORDER BY Ide", Integer.class, USED, FREE);
  }

  private static List<String> errorMessages(JsonNode actions) {
    List<String> messages = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("message".equals(action.get("type").asString())
        && "error".equals(action.get("parameters").get("type").asString())) {
        messages.add(action.get("parameters").get("message").asString());
      }
    }
    return messages;
  }
}
