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
    assertThat(settings().get("density").asText()).isEqualTo("comfortable");

    maintain("selectCompactDensity");
    assertThat(userSettings()).containsEntry("THEMEMODE", null).containsEntry("DENSITY", "compact");
    assertThat(settings().get("density").asText()).isEqualTo("compact");

    maintain("selectComfortableDensity");
    assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM AweUsrSet WHERE userName = ?",
      Integer.class, USER)).isOne();
    assertThat(userSettings()).containsEntry("DENSITY", "comfortable");
    assertThat(settings().get("density").asText()).isEqualTo("comfortable");
  }

  @Test
  void theUserModeKeepsWorkingNextToTheDensity() throws Exception {
    maintain("selectDarkMode");
    assertThat(userSettings()).containsEntry("THEMEMODE", "dark").containsEntry("DENSITY", null);
    assertThat(settings().get("themeMode").asText()).isEqualTo("dark");

    maintain("selectCompactDensity");
    maintain("selectLightMode");
    assertThat(userSettings()).containsEntry("THEMEMODE", "light").containsEntry("DENSITY", "compact");
    JsonNode settings = settings();
    assertThat(settings.get("themeMode").asText()).isEqualTo("light");
    assertThat(settings.get("density").asText()).isEqualTo("compact");
  }

  @Test
  void theUserSettingsTargetsOnlyChangeTheUserOfTheSession() throws Exception {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update("INSERT INTO AweUsrSet (userName, themeMode, density) VALUES (?, ?, ?)", OTHER_USER, "light", "comfortable");

    maintain("updateUserDensity", "{\"user\":\"" + OTHER_USER + "\",\"density\":\"compact\",\"max\":30}");
    maintain("updateUserMode", "{\"user\":\"" + OTHER_USER + "\",\"mode\":\"dark\",\"max\":30}");

    assertThat(jdbc.queryForMap("SELECT themeMode, density FROM AweUsrSet WHERE userName = ?", OTHER_USER))
      .containsEntry("THEMEMODE", "light").containsEntry("DENSITY", "comfortable");
  }

  @Test
  void addUserSettingsOnlyCreatesTheSettingsOfTheUserOfTheSession() throws Exception {
    maintain("addUserSettings", "{\"user\":\"" + OTHER_USER + "\",\"density\":\"compact\",\"max\":30}");

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AweUsrSet WHERE userName = ?", Integer.class, OTHER_USER)).isZero();
    assertThat(userSettings()).containsEntry("DENSITY", "compact");
  }

  private Map<String, Object> userSettings() {
    return new JdbcTemplate(dataSource).queryForMap("SELECT themeMode, density FROM AweUsrSet WHERE userName = ?", USER);
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
