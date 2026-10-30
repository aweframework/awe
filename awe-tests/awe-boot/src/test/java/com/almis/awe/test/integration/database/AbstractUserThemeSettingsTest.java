package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Density and mode of the user (#885, S9): the selectComfortableDensity and selectCompactDensity targets store the
 * density in AweUsrSet next to the mode and the settings of the session carry them
 */
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
public abstract class AbstractUserThemeSettingsTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "test";
  // Another user: a client must not change the settings of other users through the user settings targets
  private static final String OTHER_USER = "other-user-885";
  private static final String COMFORTABLE = "comfortable";
  private static final String COMPACT = "compact";
  private static final String LIGHT = "light";
  private static final String SELECT_COMPACT_DENSITY = "selectCompactDensity";
  private static final String THEME_MODE_COLUMN = "THEMEMODE";
  private static final String DENSITY_COLUMN = "DENSITY";
  private static final String DENSITY_FIELD = "density";
  private static final String USER_BODY_PREFIX = "{\"user\":\"";
  private static final String SELECT_SETTINGS_SQL = "SELECT themeMode, density FROM AweUsrSet WHERE userName = ?";

  @Autowired
  private DataSource dataSource;

  private MockHttpSession session;

  @BeforeEach
  void setUpSession() throws Exception {
    deleteUserSettings();
    session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", USER).session(session))
      .andExpect(status().isOk());
  }

  @AfterEach
  void deleteUserSettings() {
    new JdbcTemplate(dataSource).update("DELETE FROM AweUsrSet WHERE userName IN (?, ?)", USER, OTHER_USER);
  }

  @Test
  void theUserDensityIsStoredAndReachesTheSettings() throws Exception {
    assertThat(settings().get(DENSITY_FIELD).asText()).isEqualTo(COMFORTABLE);

    maintain(SELECT_COMPACT_DENSITY);
    assertThat(userSettings()).containsEntry(THEME_MODE_COLUMN, null).containsEntry(DENSITY_COLUMN, COMPACT);
    assertThat(settings().get(DENSITY_FIELD).asText()).isEqualTo(COMPACT);

    maintain("selectComfortableDensity");
    assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM AweUsrSet WHERE userName = ?",
      Integer.class, USER)).isOne();
    assertThat(userSettings()).containsEntry(DENSITY_COLUMN, COMFORTABLE);
    assertThat(settings().get(DENSITY_FIELD).asText()).isEqualTo(COMFORTABLE);
  }

  @Test
  void theUserModeKeepsWorkingNextToTheDensity() throws Exception {
    maintain("selectDarkMode");
    assertThat(userSettings()).containsEntry(THEME_MODE_COLUMN, "dark").containsEntry(DENSITY_COLUMN, null);
    assertThat(settings().get("themeMode").asText()).isEqualTo("dark");

    maintain(SELECT_COMPACT_DENSITY);
    maintain("selectLightMode");
    assertThat(userSettings()).containsEntry(THEME_MODE_COLUMN, LIGHT).containsEntry(DENSITY_COLUMN, COMPACT);
    JsonNode settings = settings();
    assertThat(settings.get("themeMode").asText()).isEqualTo(LIGHT);
    assertThat(settings.get(DENSITY_FIELD).asText()).isEqualTo(COMPACT);
  }

  @Test
  void theUserSettingsTargetsOnlyChangeTheUserOfTheSession() throws Exception {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update("INSERT INTO AweUsrSet (userName, themeMode, density) VALUES (?, ?, ?)", OTHER_USER, LIGHT, COMFORTABLE);

    // The request names another user: the user of the session is the one changed
    maintain(SELECT_COMPACT_DENSITY, USER_BODY_PREFIX + OTHER_USER + "\",\"max\":30}");
    maintain("selectDarkMode", USER_BODY_PREFIX + OTHER_USER + "\",\"max\":30}");

    assertThat(jdbc.queryForMap(SELECT_SETTINGS_SQL, OTHER_USER))
      .containsEntry(THEME_MODE_COLUMN, LIGHT).containsEntry(DENSITY_COLUMN, COMFORTABLE);
    assertThat(userSettings()).containsEntry(THEME_MODE_COLUMN, "dark").containsEntry(DENSITY_COLUMN, COMPACT);
  }

  @Test
  void theServerOnlyUserSettingsTargetsAreRefusedForAClient() throws Exception {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update("INSERT INTO AweUsrSet (userName, themeMode, density) VALUES (?, ?, ?)", OTHER_USER, LIGHT, COMFORTABLE);

    // Even for the user of the session: these targets are only launched by the server
    for (String user : new String[]{OTHER_USER, USER}) {
      maintain("updateUserDensity", USER_BODY_PREFIX + user + "\",\"density\":\"compact\",\"max\":30}");
      maintain("updateUserMode", USER_BODY_PREFIX + user + "\",\"mode\":\"dark\",\"max\":30}");
      maintain("addUserSettings", USER_BODY_PREFIX + user + "\",\"density\":\"compact\",\"max\":30}");
    }

    assertThat(jdbc.queryForMap(SELECT_SETTINGS_SQL, OTHER_USER))
      .containsEntry(THEME_MODE_COLUMN, LIGHT).containsEntry(DENSITY_COLUMN, COMFORTABLE);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AweUsrSet WHERE userName = ?", Integer.class, USER)).isZero();
  }

  private Map<String, Object> userSettings() {
    return new JdbcTemplate(dataSource).queryForMap(SELECT_SETTINGS_SQL, USER);
  }

  private void maintain(String target) throws Exception {
    maintain(target, "{\"max\":30}");
  }

  private void maintain(String target, String body) throws Exception {
    mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private JsonNode settings() throws Exception {
    String response = mockMvc.perform(post("/settings")
        .with(csrf())
        .session(session)
        .content("{\"view\":\"base\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response);
  }
}
