package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Per-user state of the advanced block of the windows (#790): save through the maintain target, and application when
 * the screen is generated.
 */
@Tag("integration")
@DisplayName("Window advanced state tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class WindowStateMaintainTest extends AbstractSpringAppIntegrationTest {

  private static final String SCREEN = "AdvSchTst";
  private static final String OPTION = "matrix-test-advanced-search";
  private static final String WINDOW = "WinAdvInl";
  private static final String WINDOW_WITHOUT_BLOCK = "WinAdvInlGrd";

  @Autowired
  private DataSource dataSource;

  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    session = new MockHttpSession();
    setParameter("user", "test");
    setParameter("restriction", "administrator");
    deleteStates();
  }

  @AfterEach
  void cleanUp() throws Exception {
    deleteStates();
  }

  @Test
  @DisplayName("Saving stores one row per user, screen and window, and saving again replaces it")
  void saveStoresOneRowAndReplacesIt() throws Exception {
    save(WINDOW, "1");
    assertEquals(1, countRows("test"));
    assertEquals(1, storedState("test"));

    save(WINDOW, "0");
    assertEquals(1, countRows("test"));
    assertEquals(0, storedState("test"));
  }

  @Test
  @DisplayName("The user comes from the session, a user parameter in the request is ignored")
  void userParameterIsIgnored() throws Exception {
    mockMvc.perform(post("/action/maintain/saveWindowAdvancedState")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"advancedStateScreen\":\"" + SCREEN + "\",\"advancedStateWindow\":\"" + WINDOW + "\",\"advancedStateOpen\":\"1\","
          + "\"user\":\"mallory\",\"Ope\":\"mallory\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    assertEquals(1, countRows("test"));
    assertEquals(0, countRows("mallory"));
  }

  @Test
  @DisplayName("A window without advanced block, an unknown window or an invalid value is not stored")
  void invalidStatesAreNotStored() throws Exception {
    send(WINDOW_WITHOUT_BLOCK, "1");
    send("NoSuchWindow", "1");
    send(WINDOW, "true");

    assertEquals(0, countRows("test"));
  }

  @Test
  @DisplayName("The screen is generated with the stored state of the user in the window with the advanced block")
  void screenCarriesTheState() throws Exception {
    save(WINDOW, "1");
    JsonNode screen = objectMapper.readTree(screenData());
    assertTrue(controllerOf(screen, WINDOW).get("advancedOpen").asBoolean());
    assertFalse(controllerOf(screen, WINDOW_WITHOUT_BLOCK).has("advancedOpen"));

    save(WINDOW, "0");
    JsonNode closed = controllerOf(objectMapper.readTree(screenData()), WINDOW).get("advancedOpen");
    assertTrue(closed.isBoolean());
    assertFalse(closed.asBoolean());
  }

  @Test
  @DisplayName("Without a stored state the windows carry no state")
  void screenWithoutState() throws Exception {
    JsonNode screen = objectMapper.readTree(screenData());

    assertEquals(SCREEN, screen.at("/screen/name").asText(), "The clients send this screen id when they save");
    assertNull(controllerOf(screen, WINDOW).get("advancedOpen"));
    assertNull(controllerOf(screen, WINDOW_WITHOUT_BLOCK).get("advancedOpen"));
  }

  @Test
  @DisplayName("The state of another user is not applied")
  void anotherUsersStateIsNotApplied() throws Exception {
    insertRow("another", SCREEN, WINDOW, 1);

    assertNull(controllerOf(objectMapper.readTree(screenData()), WINDOW).get("advancedOpen"));
  }

  private JsonNode controllerOf(JsonNode screen, String id) {
    for (JsonNode component : screen.get("components")) {
      if (id.equals(component.get("id").asText())) {
        return component.get("controller");
      }
    }
    throw new IllegalStateException("Component " + id + " not found in the screen");
  }

  private String screenData() throws Exception {
    MvcResult result = mockMvc.perform(post("/screen-data/" + OPTION)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"module\":\"Test\",\"option\":\"" + OPTION + "\",\"view\":\"report\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    return result.getResponse().getContentAsString();
  }

  private void save(String window, String open) throws Exception {
    mockMvc.perform(post("/action/maintain/saveWindowAdvancedState")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"advancedStateScreen\":\"" + SCREEN + "\",\"advancedStateWindow\":\"" + window + "\",\"advancedStateOpen\":\"" + open + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private void send(String window, String open) throws Exception {
    mockMvc.perform(post("/action/maintain/saveWindowAdvancedState")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"advancedStateScreen\":\"" + SCREEN + "\",\"advancedStateWindow\":\"" + window + "\",\"advancedStateOpen\":\"" + open + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn();
  }

  private void setParameter(String name, String value) throws Exception {
    mockMvc.perform(post("/session/set/" + name)
        .with(csrf())
        .param("value", value)
        .session(session))
      .andReturn();
  }

  private int countRows(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM AweUsrWinCnf WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int storedState(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT AdvOpn FROM AweUsrWinCnf WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private void insertRow(String user, String screen, String window, int open) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweUsrWinCnf (IdeUsrWinCnf, Ope, Scr, WinId, AdvOpn) VALUES ((SELECT COALESCE(MAX(IdeUsrWinCnf), 0) + 1 FROM AweUsrWinCnf), ?, ?, ?, ?)")) {
      statement.setString(1, user);
      statement.setString(2, screen);
      statement.setString(3, window);
      statement.setInt(4, open);
      statement.executeUpdate();
    }
  }

  private void deleteStates() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweUsrWinCnf")) {
      statement.executeUpdate();
    }
  }
}
