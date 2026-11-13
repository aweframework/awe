package com.almis.awe.test.integration.database;

import com.almis.awe.model.util.theme.PaletteGenerator;
import com.almis.awe.model.util.theme.ThemeAppearance;
import com.almis.awe.service.ThemeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Theme CRUD (#885): the new, update, list and delete actions of the themes screens read and write the primary colour, the
 * surface and the mode of a theme, and audit them in HISAweThm. The seeded classic themes carry their preset. The screens
 * choose each colour with a palette select plus a colour picker for a custom hex (S12): the list splits the stored value
 * into both and the maintains compose it back, rejecting an invalid palette or hex without changing the data.
 * Each subclass runs it on one way of building the schema (Flyway migrations or the test application's SQL scripts).
 */
@WithMockUser
public abstract class AbstractThemeMaintainTest extends AbstractSpringAppIntegrationTest {

  private static final String THEME = "AWEBOOT-TEST-THM-CRUD";
  private static final String MAINTAIN_OK = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\"}}]";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ThemeService themeService;

  @AfterEach
  void cleanData() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM AweThm WHERE Nam = ?", THEME);
    jdbcTemplate.update("DELETE FROM HISAweThm WHERE Nam = ?", THEME);
  }

  @Test
  void seededClassicThemeCarriesItsPreset() throws Exception {
    Integer id = new JdbcTemplate(dataSource).queryForObject("SELECT IdeThm FROM AweThm WHERE Nam = 'sunset'", Integer.class);
    JsonNode row = launch("data", "ThmLst", "\"CrtNam\":" + id + ",")
      .get(0).get("parameters").get("datalist").get("rows").get(0);
    assertThat(row.get("Pri").asText()).isEqualTo("red");
    assertThat(row.get("Srf").asText()).isEqualTo("auto");
    assertThat(row.get("Mde").asText()).isEqualTo("system");
  }

  @Test
  void themeCrudReadsAndWritesPrimarySurfaceAndMode() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("custom", "#1A73E8", "slate", "") + "\"Mde\":\"dark\",");
    Map<String, Object> created = jdbcTemplate.queryForMap("SELECT IdeThm, Pri, Srf, Mde FROM AweThm WHERE Nam = ?", THEME);
    assertThat(created).containsEntry("PRI", "#1A73E8").containsEntry("SRF", "slate").containsEntry("MDE", "dark");
    int id = ((Number) created.get("IDETHM")).intValue();
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf FROM HISAweThm WHERE Nam = ? AND HISact = 'I'", THEME))
      .containsEntry("PRI", "#1A73E8").containsEntry("SRF", "slate");

    // The list returns the fields the update and view screens load from the selected row
    JsonNode row = listRow(id);
    assertThat(row.get("Pri").asText()).isEqualTo("#1A73E8");
    assertThat(row.get("Srf").asText()).isEqualTo("slate");
    assertThat(row.get("Mde").asText()).isEqualTo("dark");

    launch("maintain", "ThmUpd", "\"IdeThm\":" + id + ",\"Nam\":\"" + THEME + "\",\"Act\":1,"
      + colors("violet", "#1a73e8", "auto", "") + "\"Mde\":\"light\",");
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf, Mde FROM AweThm WHERE IdeThm = ?", id))
      .containsEntry("PRI", "violet").containsEntry("SRF", "auto").containsEntry("MDE", "light");
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf, Mde FROM HISAweThm WHERE Nam = ? AND HISact = 'U'", THEME))
      .containsEntry("PRI", "violet").containsEntry("SRF", "auto").containsEntry("MDE", "light");

    launch("maintain", "ThmDel", "\"IdeThm\":[" + id + "],\"Nam\":[\"" + THEME + "\"],\"Act\":[1],"
      + "\"Pri\":[\"violet\"],\"Srf\":[\"auto\"],\"Mde\":[\"light\"],");
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AweThm WHERE IdeThm = ?", Integer.class, id)).isZero();
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf, Mde FROM HISAweThm WHERE Nam = ? AND HISact = 'D'", THEME))
      .containsEntry("PRI", "violet").containsEntry("SRF", "auto").containsEntry("MDE", "light");
  }

  @Test
  void aNewThemeWithPalettesStoresTheirNamesAndListsThemSplit() throws Exception {
    // A hex left in a hidden picker is ignored when a palette is selected
    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("Violet", "#123456", "zinc", "#654321")
      + "\"Mde\":\"system\",");

    Map<String, Object> created = new JdbcTemplate(dataSource)
      .queryForMap("SELECT IdeThm, Pri, Srf FROM AweThm WHERE Nam = ?", THEME);
    assertThat(created).containsEntry("PRI", "violet").containsEntry("SRF", "zinc");
    assertSplit(listRow(((Number) created.get("IDETHM")).intValue()), "violet", "", "zinc", "");
  }

  @Test
  void aNewThemeWithCustomColoursStoresTheHexesAndListsThemSplit() throws Exception {
    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("custom", " #1A73E8 ", "custom", "#334155")
      + "\"Mde\":\"system\",");

    Map<String, Object> created = new JdbcTemplate(dataSource)
      .queryForMap("SELECT IdeThm, Pri, Srf FROM AweThm WHERE Nam = ?", THEME);
    assertThat(created).containsEntry("PRI", "#1A73E8").containsEntry("SRF", "#334155");
    assertSplit(listRow(((Number) created.get("IDETHM")).intValue()), "custom", "#1A73E8", "custom", "#334155");
  }

  @Test
  void aNewThemeStoresLowerCaseAndShortHexesAsUpperCaseRrggbb() throws Exception {
    // #885, S13: every stored theme hex is #RRGGBB in upper case
    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("custom", "#3c2e60", "custom", "#abc")
      + "\"Mde\":\"system\",");

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    Map<String, Object> created = jdbcTemplate.queryForMap("SELECT IdeThm, Pri, Srf FROM AweThm WHERE Nam = ?", THEME);
    assertThat(created).containsEntry("PRI", "#3C2E60").containsEntry("SRF", "#AABBCC");
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf FROM HISAweThm WHERE Nam = ? AND HISact = 'I'", THEME))
      .containsEntry("PRI", "#3C2E60").containsEntry("SRF", "#AABBCC");
    assertSplit(listRow(((Number) created.get("IDETHM")).intValue()), "custom", "#3C2E60", "custom", "#AABBCC");
  }

  @Test
  void anUpdateSwitchesBetweenAPaletteAndACustomColour() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("emerald", "", "auto", "") + "\"Mde\":\"system\",");
    int id = jdbcTemplate.queryForObject("SELECT IdeThm FROM AweThm WHERE Nam = ?", Integer.class, THEME);
    assertSplit(listRow(id), "emerald", "", "auto", "");

    // Palette -> custom
    launch("maintain", "ThmUpd", "\"IdeThm\":" + id + ",\"Nam\":\"" + THEME + "\",\"Act\":1,"
      + colors("custom", "#f43f5e", "custom", "#78716c") + "\"Mde\":\"system\",");
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf FROM AweThm WHERE IdeThm = ?", id))
      .containsEntry("PRI", "#F43F5E").containsEntry("SRF", "#78716C");
    assertSplit(listRow(id), "custom", "#F43F5E", "custom", "#78716C");

    // Custom -> palette, with the previous hexes still in the pickers
    launch("maintain", "ThmUpd", "\"IdeThm\":" + id + ",\"Nam\":\"" + THEME + "\",\"Act\":1,"
      + colors("sky", "#f43f5e", "stone", "#78716c") + "\"Mde\":\"system\",");
    assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf FROM AweThm WHERE IdeThm = ?", id))
      .containsEntry("PRI", "sky").containsEntry("SRF", "stone");
    assertSplit(listRow(id), "sky", "", "stone", "");
  }

  @Test
  void aSeededClassicThemeListsItsPaletteSplit() throws Exception {
    Integer id = new JdbcTemplate(dataSource).queryForObject("SELECT IdeThm FROM AweThm WHERE Nam = 'sunset'", Integer.class);
    assertSplit(listRow(id), "red", "", "auto", "");
  }

  @Test
  void anInvalidColourIsRejectedWithoutChangingTheThemes() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    String[][] invalid = {
      {"custom", "#12", "auto", ""},
      {"custom", "", "auto", ""},
      {"chartreuse", "", "auto", ""},
      {"emerald", "", "custom", "blue"},
      {"emerald", "", "emerald", ""},
      {"", "", "auto", ""}};
    for (String[] colors : invalid) {
      assertRejected(launchRejected("ThmNew", "\"Nam\":\"" + THEME + "\","
        + colors(colors[0], colors[1], colors[2], colors[3]) + "\"Mde\":\"system\","));
      assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AweThm WHERE Nam = ?", Integer.class, THEME)).isZero();
    }

    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("violet", "", "zinc", "") + "\"Mde\":\"dark\",");
    int id = jdbcTemplate.queryForObject("SELECT IdeThm FROM AweThm WHERE Nam = ?", Integer.class, THEME);
    int audits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HISAweThm WHERE Nam = ?", Integer.class, THEME);
    for (String[] colors : invalid) {
      assertRejected(launchRejected("ThmUpd", "\"IdeThm\":" + id + ",\"Nam\":\"" + THEME + "\",\"Act\":1,"
        + colors(colors[0], colors[1], colors[2], colors[3]) + "\"Mde\":\"light\","));
      assertThat(jdbcTemplate.queryForMap("SELECT Pri, Srf, Mde FROM AweThm WHERE IdeThm = ?", id))
        .containsEntry("PRI", "violet").containsEntry("SRF", "zinc").containsEntry("MDE", "dark");
    }
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HISAweThm WHERE Nam = ?", Integer.class, THEME))
      .isEqualTo(audits);
  }

  @Test
  void theAppearanceOfAThemeComesFromItsRow() throws Exception {
    launch("maintain", "ThmNew", "\"Nam\":\"" + THEME + "\"," + colors("custom", "#1A73E8", "zinc", "") + "\"Mde\":\"dark\",");

    assertThat(themeService.resolve(THEME, null, null))
      .isEqualTo(new ThemeAppearance("#1A73E8", "zinc", "dark", "comfortable", PaletteGenerator.primary("#1a73e8")));
    // A name without row takes its preset
    assertThat(themeService.resolve("amazonia", null, null).primary()).isEqualTo("#948787");
  }

  /**
   * Criteria of the primary and surface selects and colour pickers of the new and update screens
   */
  private static String colors(String primaryPalette, String primaryHex, String surfacePalette, String surfaceHex) {
    return "\"PriPal\":\"" + primaryPalette + "\",\"PriHex\":\"" + primaryHex + "\",\"SrfPal\":\"" + surfacePalette
      + "\",\"SrfHex\":\"" + surfaceHex + "\",";
  }

  private JsonNode listRow(int id) throws Exception {
    return launch("data", "ThmLst", "\"CrtNam\":" + id + ",")
      .get(0).get("parameters").get("datalist").get("rows").get(0);
  }

  private static void assertSplit(JsonNode row, String primaryPalette, String primaryHex, String surfacePalette,
                                  String surfaceHex) {
    assertThat(text(row, "PriPal")).isEqualTo(primaryPalette);
    assertThat(text(row, "PriHex")).isEqualTo(primaryHex);
    assertThat(text(row, "SrfPal")).isEqualTo(surfacePalette);
    assertThat(text(row, "SrfHex")).isEqualTo(surfaceHex);
  }

  private static String text(JsonNode row, String column) {
    JsonNode value = row.path(column);
    return value.isMissingNode() || value.isNull() ? "" : value.asText();
  }

  private static void assertRejected(JsonNode response) {
    JsonNode message = null;
    for (JsonNode action : response) {
      if ("message".equals(action.path("type").asText())) {
        message = action.get("parameters");
      }
    }
    assertThat(message).isNotNull();
    assertThat(message.get("type").asText()).isEqualTo("error");
    assertThat(message.get("title").asText()).isEqualTo("Invalid theme colour");
  }

  private JsonNode launchRejected(String name, String variables) throws Exception {
    return objectMapper.readTree(mockMvc.perform(post("/action/maintain/" + name)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{" + variables + "\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
  }

  private JsonNode launch(String type, String name, String variables) throws Exception {
    var request = mockMvc.perform(post("/action/" + type + "/" + name)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{" + variables + "\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
    if ("maintain".equals(type)) {
      request.andExpect(content().json(MAINTAIN_OK));
    }
    return objectMapper.readTree(request.andReturn().getResponse().getContentAsString());
  }
}
