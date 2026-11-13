package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The screen configuration of a user applies only to that user, also on the first opening of a screen
 */
@DisplayName("Screen configuration isolation")
@Tag("integration")
@WithMockUser(username = "test", password = "test")
// Own application context: the screen must not have been opened (initialized) by another test before
@TestPropertySource(properties = "awe.test.context=screen-configuration-isolation")
class ScreenConfigurationIsolationTest extends AbstractSpringAppIntegrationTest {

  private static final int CONFIGURATION_ID = 990101;
  private static final int DONALD_ID = 2;
  private static final String DONALD_LABEL = "Donald only";
  private static final String SCREEN_PARAMETERS = "{\"module\":\"Test\",\"option\":\"criteria-test-left\",\"view\":\"report\"}";

  @Autowired
  private DataSource dataSource;

  @AfterEach
  void removeConfiguration() {
    new JdbcTemplate(dataSource).update("delete from AweScrCnf where IdeAweScrCnf = ?", CONFIGURATION_ID);
  }

  @Test
  void firstOpeningDoesNotShareTheUserConfiguration() throws Exception {
    new JdbcTemplate(dataSource).update(
      "insert into AweScrCnf (IdeAweScrCnf, IdeOpe, IdePro, Scr, Nam, Atr, Val, Act) values (?, ?, null, 'CrtTstLeft', 'Txt', 'label', ?, 1)",
      CONFIGURATION_ID, DONALD_ID, DONALD_LABEL);

    assertThat(textLabel(sessionOf("donald"))).isEqualTo(DONALD_LABEL);
    assertThat(textLabel(sessionOf("test"))).isNotEqualTo(DONALD_LABEL);
  }

  private MockHttpSession sessionOf(String user) throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionParameter(session, "user", user);
    setSessionParameter(session, "restriction", "administrator");
    return session;
  }

  private void setSessionParameter(MockHttpSession session, String name, String value) throws Exception {
    mockMvc.perform(post(String.format("/session/set/%s", name))
        .with(csrf())
        .param("value", value)
        .session(session))
      .andExpect(status().isOk());
  }

  private String textLabel(MockHttpSession session) throws Exception {
    String response = mockMvc.perform(post("/screen-data/criteria-test-left")
        .session(session)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(SCREEN_PARAMETERS)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    for (JsonNode component : objectMapper.readTree(response).get("components")) {
      if ("Txt".equals(component.get("id").asString())) {
        JsonNode label = component.get("controller").get("label");
        return label == null ? null : label.asString();
      }
    }
    throw new AssertionError("Component Txt not found in criteria-test-left");
  }
}
