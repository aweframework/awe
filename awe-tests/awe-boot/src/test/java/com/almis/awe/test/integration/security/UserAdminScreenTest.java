package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The users screen (user-admin) sends the identifier of the users from its grid, as a list, to the maintains it reuses
 * (UsrNew, UsrUpd, UsrUnl). UsrUpd keeps working with the identifier as a single value (legacy UsrUpd screen).
 */
@Tag("integration")
@DisplayName("User admin screen tests")
class UserAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String LOCKED_USER = "usradm793lck";
  private static final String OTHER_USER = "usradm793oth";
  private static final String NEW_USER = "usradm793new";

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;
  private int lockedId;
  private int otherId;

  @BeforeEach
  void createUsers() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    deleteUsers();
    lockedId = support.copyTestUser(LOCKED_USER);
    otherId = support.copyTestUser(OTHER_USER);
    execute("UPDATE ope SET PwdLck = 1, NumLog = 3 WHERE IdeOpe = " + lockedId);
  }

  @AfterEach
  void deleteUsers() throws Exception {
    support.deleteUser(LOCKED_USER);
    support.deleteUser(OTHER_USER);
    support.deleteUser(NEW_USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("Unlocking the selected users clears the lock and the failed logins, and audits only the locked ones")
  void unlockSelectedUsers() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    maintain("UsrUnl", "{\"IdeOpe\":[" + lockedId + "," + otherId + "],\"max\":30}").andExpect(status().isOk());

    assertThat(support.lockStatus(LOCKED_USER)).containsExactly(0, 0);
    assertThat(support.lockStatus(OTHER_USER)).containsExactly(0, 0);
    assertThat(events(LOCKED_USER, start)).containsExactly("ACCOUNT_UNLOCKED/SUCCESS/ADMINISTRATOR");
    assertThat(events(OTHER_USER, start)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("Unlocking only touches the selected users")
  void unlockOnlyTheSelectedUsers() throws Exception {
    execute("UPDATE ope SET PwdLck = 1, NumLog = 2 WHERE IdeOpe = " + otherId);
    Instant start = Instant.now().minusSeconds(1);

    maintain("UsrUnl", "{\"IdeOpe\":[" + lockedId + "],\"max\":30}").andExpect(status().isOk());

    assertThat(support.lockStatus(LOCKED_USER)).containsExactly(0, 0);
    assertThat(support.lockStatus(OTHER_USER)).containsExactly(1, 2);
    assertThat(events(OTHER_USER, start)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrUpd updates and audits the user when the grid sends its identifier as a one element list")
  void updateWithTheIdentifierAsAList() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    String response = updateUser(objectMapper.createArrayNode().add(otherId), "Changed from the grid").andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertOk(response);
    assertThat(support.user(OTHER_USER).get("OPENAM")).isEqualTo("Changed from the grid");
    assertThat(support.lockStatus(OTHER_USER)[0]).isEqualTo(1);
    assertThat(events(OTHER_USER, start)).containsExactly("ACCOUNT_LOCKED/SUCCESS/ADMINISTRATOR");
    assertThat(support.user(LOCKED_USER).get("OPENAM")).isNotEqualTo("Changed from the grid");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrUpd keeps updating and auditing the user when the identifier is a single value (legacy screen)")
  void updateWithTheIdentifierAsAValue() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    String response = updateUser(objectMapper.getNodeFactory().numberNode(otherId), "Changed from the form").andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertOk(response);
    assertThat(support.user(OTHER_USER).get("OPENAM")).isEqualTo("Changed from the form");
    assertThat(events(OTHER_USER, start)).containsExactly("ACCOUNT_LOCKED/SUCCESS/ADMINISTRATOR");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrNew creates a new user even when the grid sends the identifier of a selected user")
  void createWithASelectedRow() throws Exception {
    Map<String, Object> other = support.user(OTHER_USER);

    String response = maintain("UsrNew", "{\"IdeOpe\":[" + otherId + "],\"Usr\":\"" + NEW_USER + "\",\"Pas\":\"newpass1\","
      + "\"Sta\":1,\"Eml\":\"new@almis.example\",\"Lan\":\"en-GB\",\"Nam\":\"New User\",\"Pro\":1,\"PwdLck\":0,\"max\":30}")
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertOk(response);
    Map<String, Object> created = support.user(NEW_USER);
    assertThat(created.get("IDEOPE")).isNotEqualTo(otherId);
    assertThat(created.get("OPENAM")).isEqualTo("New User");
    assertThat(support.user(OTHER_USER)).isEqualTo(other);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The Active switch sends 0 to deactivate the user in UsrUpd and 1 to activate it again")
  void updateActiveFromTheSwitch() throws Exception {
    assertOk(updateUser(objectMapper.createArrayNode().add(otherId), "Switched off", 0).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
    assertThat(Integer.parseInt(string(support.user(OTHER_USER).get("L1_ACT")))).isZero();

    assertOk(updateUser(objectMapper.createArrayNode().add(otherId), "Switched on", 1).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
    assertThat(Integer.parseInt(string(support.user(OTHER_USER).get("L1_ACT")))).isEqualTo(1);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrNew creates an inactive user when the Active switch is off")
  void createInactiveFromTheSwitch() throws Exception {
    String response = maintain("UsrNew", "{\"Usr\":\"" + NEW_USER + "\",\"Pas\":\"newpass1\","
      + "\"Sta\":0,\"Eml\":\"new@almis.example\",\"Lan\":\"en-GB\",\"Nam\":\"New User\",\"Pro\":1,\"PwdLck\":0,\"max\":30}")
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    assertOk(response);
    assertThat(Integer.parseInt(string(support.user(NEW_USER).get("L1_ACT")))).isZero();
  }

  @Test
  @DisplayName("The users screen is generated with its grid and its side panel")
  void screenIsGenerated() throws Exception {
    // The option is in the private menu: sign in first
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + OTHER_USER + "\",\"pwd_usr\":\"test\"}")
      .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

    String response = mockMvc.perform(post("/screen-data/user-admin")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(response, "$.screen.name")).isEqualTo("user-admin");
    List<String> components = JsonPath.read(response, "$.components[*].id");
    assertThat(components).contains("GrdUsrAdm", "CrtUsrTxt", "CrtUsrSta", "PnlMod", "Nam", "Eml", "Usr", "Lan", "Pro", "ScrIni",
      "Res", "Sta", "PwdLck", "FlgPwdUpd", "Pas", "RetPas", "Thm", "EmlSrv", "PcPrn", "WebPrn", "ButPnlUpd", "ButPnlNew",
      "ButPnlUnl", "CtxButUnl", "ButRst2fa", "ButDel").doesNotContain("IdeOpe", "CrtUsr", "CrtPro", "ButSch", "ButRst");
    // The search of the grid toolbar replaces the criteria window: a text criterion with no query of its own
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'GrdUsrAdm')].controller.search"))
      .containsExactly("CrtUsrTxt");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtUsrTxt')].controller.component"))
      .containsExactly("grid-search");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtUsrTxt')].controller.placeholder"))
      .containsExactly("SCREEN_TEXT_USER_SEARCH");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'CrtUsrTxt')].controller.targetAction")).isEmpty();
    List<String> sendable = JsonPath.read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.sendable == true)].name");
    assertThat(sendable).containsExactly("IdeOpe");
    // Reset 2FA and Delete are the actions of the selection bar of the grid, Delete the danger one
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.buttonModel[?(@.selection == true)].id"))
      .containsExactly("ButRst2fa", "ButDel");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButDel')].controller.style"))
      .containsExactly("btn-danger");
    assertThat(JsonPath.<String>read(response, "$.template")).contains("PnlUsr");
    // Row actions: the pencil (edit in the side panel) and the ellipsis (row menu), icon-only buttons of the last columns,
    // not sortable nor printed
    List<String> columns = JsonPath.read(response, "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[*].name");
    assertThat(columns).containsSubsequence("FulNam", "LanTxt", "EdtBtn", "MorBtn");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == 'EdtBtn')].title")).containsExactly("BUTTON_EDIT");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == 'MorBtn')].actions[*].type"))
      .containsExactly("show-context-menu");
    assertThat(JsonPath.<List<Object>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name in ['EdtBtn', 'MorBtn'] && @.sortable == false)]"))
      .hasSize(2);
    // Two-factor is a plain tag (#793): the translated text, with the icon of the row in place of the dot; sorted by 2fa
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == '2faTxt')].iconField")).containsExactly("2faIcn");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == '2faTxt')].statusField")).containsExactly("2faTon");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == '2faTxt')].sortField")).containsExactly("2fa");
    assertThat(JsonPath.<List<Object>>read(response,
      "$.components[?(@.id == 'GrdUsrAdm')].controller.columnModel[?(@.name == '2faIco')]")).isEmpty();
    // Help is an icon-only ghost button, New user the primary one
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButHlp')].controller.style"))
      .containsExactly("btn-ghost no-label");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButNew')].controller.style"))
      .containsExactly("btn-primary");
    // Active is a switch (#793 S19): a checkbox with style switch, checked (1) for a new user
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Sta')].controller.component"))
      .containsExactly("checkbox");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Sta')].controller.style"))
      .singleElement().asString().contains("switch");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'Sta')].model.selected[*]"))
      .map(String::valueOf).containsExactly("1");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'Sta')].model.values[*].value"))
      .map(String::valueOf).containsExactly("1");
    // Identity header of the side panel (#793 S18): the selected user of the grid, with the state and two-factor tags
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerGrid"))
      .containsExactly("GrdUsrAdm");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerTitleField"))
      .containsExactly("FulNam");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerSubtitleField"))
      .containsExactly("UsrPro");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerAvatarField"))
      .containsExactly("Ini");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerAvatarToneField"))
      .containsExactly("IniTon");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerTags[*].field"))
      .containsExactly("StaTxt", "2faHdr");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlUsr')].controller.headerTags[*].iconField"))
      .containsExactly("2faIcn");
    assertThat(columns).contains("UsrPro", "2faHdr");
    // The four sections of the side panel have their icon (written by the tag template) and the awe-form-section-title style
    String template = JsonPath.read(response, "$.template");
    assertThat(template).contains("awe-form-section-title").contains("tag-icon").contains("'lucide:key-round' | aweIcon")
      .contains("'lucide:sliders-horizontal' | aweIcon").contains("'user' | aweIcon").contains("'shield' | aweIcon");
  }

  private ResultActions updateUser(tools.jackson.databind.JsonNode userId, String name) throws Exception {
    return updateUser(userId, name, 1);
  }

  private ResultActions updateUser(tools.jackson.databind.JsonNode userId, String name, int active) throws Exception {
    Map<String, Object> user = support.user(OTHER_USER);
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.set("IdeOpe", userId);
    parameters.put("Sta", active);
    parameters.put("Eml", string(user.get("EMLADR")));
    parameters.put("Lan", string(user.get("L1_LAN")));
    parameters.put("Nam", name);
    parameters.put("Pro", Integer.valueOf(user.get("IDEPRO").toString().trim()));
    parameters.put("PwdLck", 1);
    parameters.put("FlgPwdUpd", 0);
    parameters.put("Pas", "");
    parameters.put("max", 30);
    return maintain("UsrUpd", objectMapper.writeValueAsString(parameters));
  }

  private ResultActions maintain(String target, String body) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
      .with(csrf())
      .contentType(MediaType.APPLICATION_JSON)
      .content(body)
      .accept(MediaType.APPLICATION_JSON));
  }

  private static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).containsOnly("ok");
  }

  private List<String> events(String user, Instant start) throws Exception {
    return support.events(user, start).stream().map(event -> event.substring(0, event.lastIndexOf('/'))).toList();
  }

  private void execute(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }

  private static String string(Object value) {
    return value == null ? null : value.toString().trim();
  }
}
