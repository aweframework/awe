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
 * The themes screen (theme-admin) sends the identifier of the themes from its grid, as a list, to the maintains it reuses
 * (ThmNew, ThmUpd, ThmDel), with the criteria of its side panel: the palette selects and the colour pickers, composed by
 * the maintains into the stored colours. ThmUpd keeps working with the identifier as a single value (legacy ThmUpd
 * screen); ThmDel is a multiple maintain, so it needs the list that every grid sends (the legacy Thm screen too).
 */
@Tag("integration")
@DisplayName("Theme admin screen tests")
class ThemeAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "thmadm793";
  private static final String THEME = PREFIX + " one";
  private static final String OTHER_THEME = PREFIX + " other";
  private static final String NEW_THEME = PREFIX + " new";
  private static final String SIGNED_IN_USER = "thmadm793usr";

  @Autowired
  private DataSource dataSource;

  private int themeId;
  private int otherId;

  @BeforeEach
  void createThemes() throws Exception {
    deleteThemes();
    themeId = nextThemeId();
    otherId = themeId + 1;
    execute("INSERT INTO AweThm (IdeThm, Nam, Act, Pri, Srf, Mde) VALUES (" + themeId + ", '" + THEME
      + "', 1, 'emerald', 'auto', 'system')");
    execute("INSERT INTO AweThm (IdeThm, Nam, Act, Pri, Srf, Mde) VALUES (" + otherId + ", '" + OTHER_THEME
      + "', 1, 'rose', 'slate', 'dark')");
  }

  @AfterEach
  void deleteThemes() throws Exception {
    execute("DELETE FROM AweThm WHERE Nam LIKE '" + PREFIX + "%'");
    execute("DELETE FROM HISAweThm WHERE Nam LIKE '" + PREFIX + "%'");
    new SecurityAuditTestSupport(dataSource).deleteUser(SIGNED_IN_USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmUpd updates only the theme when the grid sends its identifier as a one element list")
  void updateWithTheIdentifierAsAList() throws Exception {
    assertOk(updateTheme(objectMapper.createArrayNode().add(themeId), THEME + " changed", 0, "custom", "#1a73e8", "zinc", "", "dark"));

    assertThat(theme(themeId)).containsEntry("NAM", THEME + " changed").containsEntry("ACT", "0")
      .containsEntry("PRI", "#1A73E8").containsEntry("SRF", "zinc").containsEntry("MDE", "dark");
    assertThat(theme(otherId)).containsEntry("NAM", OTHER_THEME).containsEntry("ACT", "1").containsEntry("PRI", "rose")
      .containsEntry("SRF", "slate").containsEntry("MDE", "dark");
    // Audited with its identifier
    assertThat(select("SELECT * FROM HISAweThm WHERE Nam = '" + THEME + " changed' AND HISact = 'U'"))
      .containsEntry("IDETHM", String.valueOf(themeId)).containsEntry("PRI", "#1A73E8");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmUpd keeps updating the theme when the identifier is a single value (legacy screen)")
  void updateWithTheIdentifierAsAValue() throws Exception {
    assertOk(updateTheme(objectMapper.getNodeFactory().numberNode(themeId), THEME + " form", 1, "sky", "", "custom", "#abc", "light"));

    assertThat(theme(themeId)).containsEntry("NAM", THEME + " form").containsEntry("ACT", "1")
      .containsEntry("PRI", "sky").containsEntry("SRF", "#AABBCC").containsEntry("MDE", "light");
    assertThat(theme(otherId)).containsEntry("NAM", OTHER_THEME);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The Active switch sends 0 to deactivate the theme in ThmUpd and 1 to activate it again")
  void updateActiveFromTheSwitch() throws Exception {
    assertOk(updateTheme(objectMapper.createArrayNode().add(themeId), THEME, 0, "emerald", "", "auto", "", "system"));
    assertThat(theme(themeId)).containsEntry("ACT", "0");

    assertOk(updateTheme(objectMapper.createArrayNode().add(themeId), THEME, 1, "emerald", "", "auto", "", "system"));
    assertThat(theme(themeId)).containsEntry("ACT", "1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmUpd rejects an invalid custom colour without changing the theme")
  void updateWithAnInvalidColourChangesNothing() throws Exception {
    Map<String, Object> before = theme(themeId);

    String response = updateTheme(objectMapper.createArrayNode().add(themeId), THEME + " bad", 1, "custom", "#12", "auto", "",
      "system").andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<List<String>>read(response, "$[?(@.type == 'message')].parameters.type")).as(response)
      .containsOnly("error");
    assertThat(theme(themeId)).isEqualTo(before);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmDel deletes the selected themes sent as a list")
  void deleteWithTheIdentifiersAsAList() throws Exception {
    assertOk(maintain("ThmDel", "{\"IdeThm\":[" + themeId + "," + otherId + "],\"max\":30}"));

    assertThat(exists(themeId)).isFalse();
    assertThat(exists(otherId)).isFalse();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmDel deletes only the theme of a one element list")
  void deleteOneTheme() throws Exception {
    assertOk(maintain("ThmDel", "{\"IdeThm\":[" + themeId + "],\"Nam\":\"" + THEME + "\",\"Act\":1,\"Mde\":\"system\",\"max\":30}"));

    assertThat(exists(themeId)).isFalse();
    assertThat(exists(otherId)).isTrue();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ThmNew creates a new active theme even when the grid sends the identifier of a selected theme")
  void createWithASelectedRow() throws Exception {
    Map<String, Object> other = theme(otherId);

    assertOk(maintain("ThmNew", "{\"IdeThm\":[" + otherId + "],\"Nam\":\"" + NEW_THEME + "\",\"Act\":1,\"PriPal\":\"custom\","
      + "\"PriHex\":\"#3c2e60\",\"SrfPal\":\"stone\",\"SrfHex\":\"\",\"Mde\":\"dark\",\"PnlMod\":\"new\",\"max\":30}"));

    Map<String, Object> created = themeByName(NEW_THEME);
    assertThat(created).containsEntry("PRI", "#3C2E60").containsEntry("SRF", "stone").containsEntry("MDE", "dark")
      .containsEntry("ACT", "1");
    assertThat(created).doesNotContainEntry("IDETHM", String.valueOf(otherId));
    assertThat(theme(otherId)).isEqualTo(other);
  }

  @Test
  @DisplayName("The themes screen is generated with its grid and its side panel")
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

    String response = mockMvc.perform(post("/screen-data/themes")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(response, "$.screen.name")).isEqualTo("theme-admin");
    List<String> components = JsonPath.read(response, "$.components[*].id");
    assertThat(components).contains("GrdThmAdm", "CrtThmTxt", "CrtThmSta", "PnlMod", "Nam", "Act", "PriPal", "PriHex", "SrfPal",
        "SrfHex", "Mde", "ButPnlUpd", "ButPnlNew", "ButPnlCnl", "CtxButUpd", "CtxButDel", "ButDel", "ButNew")
      .doesNotContain("IdeThm", "CrtNam", "CrtAct", "ButSch", "ButRst", "ButViw", "ButUpd");
    // The search and the quick filter of the grid toolbar replace the criteria window
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'GrdThmAdm')].controller.search"))
      .containsExactly("CrtThmTxt");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtThmSta')].controller.targetAction"))
      .containsExactly("ThmStaCnt");
    // Only the identifier of the themes is sent to the maintains
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdThmAdm')].controller.columnModel[?(@.sendable == true)].name")).containsExactly("IdeThm");
    // Delete is the danger action of the selection bar
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdThmAdm')].controller.buttonModel[?(@.selection == true)].id")).containsExactly("ButDel");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButDel')].controller.style"))
      .containsExactly("btn-danger");
    // Main column with the avatar and the colours below the name, the mode, the state tag and the row buttons at the end
    List<String> columns = JsonPath.read(response, "$.components[?(@.id == 'GrdThmAdm')].controller.columnModel[*].name");
    assertThat(columns).containsSubsequence("NamVal", "MdeTxt", "StaTxt", "EdtBtn", "MorBtn");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdThmAdm')].controller.columnModel[?(@.name == 'NamVal')].secondaryField")).containsExactly("ThmSec");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdThmAdm')].controller.columnModel[?(@.name == 'StaTxt')].statusField")).containsExactly("StaTon");
    // New theme is the primary button
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButNew')].controller.style"))
      .containsExactly("btn-primary");
    // The defaults of a new theme, as the ThmNew screen: emerald, automatic surface and the system mode
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'PriPal')].model.selected[*]"))
      .map(String::valueOf).containsExactly("emerald");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'SrfPal')].model.selected[*]"))
      .map(String::valueOf).containsExactly("auto");
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'Mde')].model.selected[*]"))
      .map(String::valueOf).containsExactly("system");
    // The custom colours are colour pickers; Active is a switch
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PriHex' || @.id == 'SrfHex')].controller.component"))
      .containsExactly("color", "color");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Act')].controller.style"))
      .singleElement().asString().contains("switch");
    // Identity header of the side panel: the selected theme of the grid with its state tag
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlThm')].controller.headerGrid"))
      .containsExactly("GrdThmAdm");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlThm')].controller.headerTitleField"))
      .containsExactly("NamVal");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlThm')].controller.headerSubtitleField"))
      .containsExactly("ThmSec");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlThm')].controller.headerTags[*].field"))
      .containsExactly("StaTxt");
    assertThat(JsonPath.<String>read(response, "$.template")).contains("PnlThm").contains("awe-form-section-title");
  }

  /**
   * Launch ThmUpd with the criteria of the side panel
   */
  private ResultActions updateTheme(JsonNode themeId, String name, int active, String primaryPalette, String primaryHex,
                                    String surfacePalette, String surfaceHex, String mode) throws Exception {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.set("IdeThm", themeId);
    parameters.put("Nam", name);
    parameters.put("Act", active);
    parameters.put("PriPal", primaryPalette);
    parameters.put("PriHex", primaryHex);
    parameters.put("SrfPal", surfacePalette);
    parameters.put("SrfHex", surfaceHex);
    parameters.put("Mde", mode);
    parameters.put("PnlMod", "edit");
    parameters.put("max", 30);
    return maintain("ThmUpd", objectMapper.writeValueAsString(parameters));
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

  private Map<String, Object> theme(int id) throws Exception {
    return select("SELECT IdeThm, Nam, Act, Pri, Srf, Mde FROM AweThm WHERE IdeThm = " + id);
  }

  private Map<String, Object> themeByName(String name) throws Exception {
    return select("SELECT IdeThm, Nam, Act, Pri, Srf, Mde FROM AweThm WHERE Nam = '" + name + "'");
  }

  private boolean exists(int id) throws Exception {
    return !theme(id).isEmpty();
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

  private int nextThemeId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeThm) FROM AweThm");
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
}
