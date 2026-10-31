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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The profiles screen (profile-admin) sends the identifier of the profiles from its grid, as a list, to the maintains it
 * reuses (ProNew, ProUpd, ProDel). ProUpd keeps working with the identifier as a single value (legacy PrfUpd screen); ProDel
 * is a multiple maintain, so it needs the list that every grid sends (the legacy Prf screen too).
 */
@Tag("integration")
@DisplayName("Profile admin screen tests")
class ProfileAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "prfadm793";
  private static final String PROFILE = PREFIX + " one";
  private static final String OTHER_PROFILE = PREFIX + " other";
  private static final String NEW_PROFILE = PREFIX + " new";
  private static final String NEW_ACRONYM = "Q8N";
  private static final String SIGNED_IN_USER = "prfadm793usr";

  @Autowired
  private DataSource dataSource;

  private int profileId;
  private int otherId;

  @BeforeEach
  void createProfiles() throws Exception {
    deleteProfiles();
    profileId = nextProfileId();
    otherId = profileId + 1;
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + profileId + ", 'Q8A', '" + PROFILE
      + "', 1, NULL, NULL, 1)");
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + otherId + ", 'Q8B', '" + OTHER_PROFILE
      + "', 1, NULL, NULL, 1)");
  }

  @AfterEach
  void deleteProfiles() throws Exception {
    execute("DELETE FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'");
    new SecurityAuditTestSupport(dataSource).deleteUser(SIGNED_IN_USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProUpd updates only the profile when the grid sends its identifier as a one element list")
  void updateWithTheIdentifierAsAList() throws Exception {
    assertOk(updateProfile(objectMapper.createArrayNode().add(profileId), "Q8C", PROFILE + " changed", 0));

    assertThat(profile(profileId)).containsEntry("ACR", "Q8C").containsEntry("NAM", PROFILE + " changed")
      .containsEntry("IDETHM", "2").containsEntry("SCRINI", "user-admin").containsEntry("ACT", "0");
    assertThat(profile(otherId)).containsEntry("ACR", "Q8B").containsEntry("NAM", OTHER_PROFILE).containsEntry("ACT", "1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProUpd keeps updating the profile when the identifier is a single value (legacy screen)")
  void updateWithTheIdentifierAsAValue() throws Exception {
    assertOk(updateProfile(objectMapper.getNodeFactory().numberNode(profileId), "Q8A", PROFILE + " form", 1));

    assertThat(profile(profileId)).containsEntry("NAM", PROFILE + " form").containsEntry("ACT", "1");
    assertThat(profile(otherId)).containsEntry("NAM", OTHER_PROFILE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The Active switch sends 0 to deactivate the profile in ProUpd and 1 to activate it again")
  void updateActiveFromTheSwitch() throws Exception {
    assertOk(updateProfile(objectMapper.createArrayNode().add(profileId), "Q8A", PROFILE, 0));
    assertThat(profile(profileId)).containsEntry("ACT", "0");

    assertOk(updateProfile(objectMapper.createArrayNode().add(profileId), "Q8A", PROFILE, 1));
    assertThat(profile(profileId)).containsEntry("ACT", "1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProDel deletes the selected profiles sent as a list")
  void deleteWithTheIdentifiersAsAList() throws Exception {
    assertOk(maintain("ProDel", "{\"IdePro\":[" + profileId + "," + otherId + "],\"max\":30}"));

    assertThat(exists(profileId)).isFalse();
    assertThat(exists(otherId)).isFalse();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProDel deletes only the profile of a one element list")
  void deleteOneProfile() throws Exception {
    assertOk(maintain("ProDel", "{\"IdePro\":[" + profileId + "],\"max\":30}"));

    assertThat(exists(profileId)).isFalse();
    assertThat(exists(otherId)).isTrue();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProNew creates a new profile even when the grid sends the identifier of a selected profile")
  void createWithASelectedRow() throws Exception {
    Map<String, Object> other = profile(otherId);

    assertOk(maintain("ProNew", "{\"IdePro\":[" + otherId + "],\"Acr\":\"" + NEW_ACRONYM + "\",\"Nam\":\"" + NEW_PROFILE
      + "\",\"IdeThm\":2,\"ScrIni\":\"user-admin\",\"Res\":null,\"Act\":0,\"max\":30}"));

    Map<String, Object> created = profileByName(NEW_PROFILE);
    assertThat(created).containsEntry("ACR", NEW_ACRONYM).containsEntry("IDETHM", "2").containsEntry("ACT", "0");
    assertThat(created).doesNotContainEntry("IDEPRO", String.valueOf(otherId));
    assertThat(profile(otherId)).isEqualTo(other);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The acronym check of the creation finds an acronym in use, ignoring case")
  void acronymCheckFindsTheAcronymInUse() throws Exception {
    String used = mockMvc.perform(post("/action/data/check-profile-acronym")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"Acr\":\"q8a\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertThat(JsonPath.<List<Object>>read(used, "$[?(@.type == 'fill')].parameters.datalist.rows[*].value"))
      .map(String::valueOf).containsExactly(String.valueOf(profileId));
  }

  @Test
  @DisplayName("The profiles screen is generated with its grid and its side panel")
  void screenIsGenerated() throws Exception {
    // The option is in the private menu: sign in first
    new SecurityAuditTestSupport(dataSource).copyTestUser(SIGNED_IN_USER);
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + SIGNED_IN_USER + "\",\"pwd_usr\":\"test\"}")
      .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

    String response = mockMvc.perform(post("/screen-data/profiles")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(response, "$.screen.name")).isEqualTo("profile-admin");
    List<String> components = JsonPath.read(response, "$.components[*].id");
    assertThat(components).contains("GrdProAdm", "CrtProTxt", "CrtProSta", "PnlMod", "Acr", "Nam", "IdeThm", "ScrIni", "Res",
      "Act", "ButPnlUpd", "ButPnlNew", "ButPnlCnl", "CtxButUpd", "CtxButDel", "ButDel", "ButNew")
      .doesNotContain("IdePro", "CrtPro", "CrtThm", "CrtScrIni", "CrtAct", "ButSch", "ButRst", "ButViw", "ButUpd");
    // The search and the quick filter of the grid toolbar replace the criteria window
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'GrdProAdm')].controller.search"))
      .containsExactly("CrtProTxt");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtProTxt')].controller.component"))
      .containsExactly("grid-search");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtProSta')].controller.targetAction"))
      .containsExactly("ProStaCnt");
    // Only the identifier of the profiles is sent to the maintains
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[?(@.sendable == true)].name")).containsExactly("IdePro");
    // Delete is the danger action of the selection bar
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.buttonModel[?(@.selection == true)].id")).containsExactly("ButDel");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButDel')].controller.style"))
      .containsExactly("btn-danger");
    assertGridColumns(response);
    // Help is an icon-only ghost button, New profile the primary one
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButHlp')].controller.style"))
      .containsExactly("btn-ghost no-label");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButNew')].controller.style"))
      .containsExactly("btn-primary");
    // Active is a switch, on (1) for a new profile
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Act')].controller.component"))
      .containsExactly("checkbox");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Act')].controller.style"))
      .singleElement().asString().contains("switch");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'Act')].model.selected[*]"))
      .map(String::valueOf).containsExactly("1");
    // Identity header of the side panel: the selected profile of the grid with its state tag
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlPro')].controller.headerGrid"))
      .containsExactly("GrdProAdm");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlPro')].controller.headerTitleField"))
      .containsExactly("Nam");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlPro')].controller.headerSubtitleField"))
      .containsExactly("ProSec");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlPro')].controller.headerAvatarField"))
      .containsExactly("Ini");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlPro')].controller.headerTags[*].field"))
      .containsExactly("StaTxt");
    assertThat(JsonPath.<String>read(response, "$.template")).contains("PnlPro").contains("awe-form-section-title");
  }

  private ResultActions updateProfile(JsonNode profileId, String acronym, String name, int active) throws Exception {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.set("IdePro", profileId);
    parameters.put("Acr", acronym);
    parameters.put("Nam", name);
    parameters.put("IdeThm", active == 0 ? 2 : 1);
    parameters.put("ScrIni", "user-admin");
    parameters.putNull("Res");
    parameters.put("Act", active);
    parameters.put("max", 30);
    return maintain("ProUpd", objectMapper.writeValueAsString(parameters));
  }

  private ResultActions maintain(String target, String body) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
      .with(csrf())
      .contentType(MediaType.APPLICATION_JSON)
      .content(body)
      .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  private static void assertOk(ResultActions result) throws Exception {
    String response = result.andReturn().getResponse().getContentAsString();
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  private Map<String, Object> profile(int id) throws Exception {
    return select("SELECT * FROM AwePro WHERE IdePro = " + id);
  }

  private Map<String, Object> profileByName(String name) throws Exception {
    return select("SELECT * FROM AwePro WHERE Nam = '" + name + "'");
  }

  private boolean exists(int id) throws Exception {
    return !profile(id).isEmpty();
  }

  /**
   * First row of a select, with the column names in upper case and the values as trimmed text
   */
  private Map<String, Object> select(String sql) throws Exception {
    Map<String, Object> row = new HashMap<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      if (result.next()) {
        ResultSetMetaData metaData = result.getMetaData();
        for (int column = 1; column <= metaData.getColumnCount(); column++) {
          Object value = result.getObject(column);
          row.put(metaData.getColumnName(column).toUpperCase(), value == null ? null : value.toString().trim());
        }
      }
    }
    return row;
  }

  private int nextProfileId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdePro) FROM AwePro");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
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

  /**
   * Main column with the avatar and the secondary line, the state tag and the row buttons at the end
   *
   * @param response Screen data
   */
  private static void assertGridColumns(String response) {
    List<String> columns = JsonPath.read(response, "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[*].name");
    assertThat(columns).containsSubsequence("Nam", "StaTxt", "EdtBtn", "MorBtn");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[?(@.name == 'Nam')].secondaryField")).containsExactly("ProSec");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[?(@.name == 'Nam')].avatarField")).containsExactly("Ini");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[?(@.name == 'StaTxt')].statusField")).containsExactly("StaTon");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdProAdm')].controller.columnModel[?(@.name == 'MorBtn')].actions[*].type"))
      .containsExactly("show-context-menu");
  }
}
