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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Per-user grid layouts: save and reset through the maintain targets, and application when the screen is generated.
 */
@Tag("integration")
@DisplayName("Grid layout tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class GridLayoutMaintainTest extends AbstractSpringAppIntegrationTest {

  private static final String SCREEN = "MatTst";
  private static final String GRID = "GrdSta";

  @Autowired
  private DataSource dataSource;

  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    session = new MockHttpSession();
    setParameter("user", "test");
    setParameter("restriction", "administrator");
    deleteLayouts();
  }

  @AfterEach
  void cleanUp() throws Exception {
    deleteLayouts();
  }

  @Test
  @DisplayName("Saving stores one row per user, screen and grid, and saving again replaces it")
  void saveStoresOneRowAndReplacesIt() throws Exception {
    save("{\"order\":[\"Als\",\"Des\"],\"hidden\":[\"Des\"]}");
    save("{\"order\":[\"Des\",\"Als\"],\"hidden\":[]}");

    assertEquals(1, countRows("test"));
    assertEquals("{\"order\":[\"Des\",\"Als\"],\"hidden\":[]}", storedLayout("test"));
  }

  @Test
  @DisplayName("The user comes from the session, a user parameter in the request is ignored")
  void userParameterIsIgnored() throws Exception {
    mockMvc.perform(post("/action/maintain/saveGridLayout")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gridLayoutScreen\":\"" + SCREEN + "\",\"gridLayoutGrid\":\"" + GRID + "\",\"user\":\"mallory\",\"Ope\":\"mallory\","
          + "\"gridLayoutJson\":\"{\\\"order\\\":[\\\"Als\\\"],\\\"hidden\\\":[]}\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    assertEquals(1, countRows("test"));
    assertEquals(0, countRows("mallory"));
  }

  @Test
  @DisplayName("Resetting removes only the layout of the session user")
  void resetRemovesOnlyMyLayout() throws Exception {
    save("{\"order\":[\"Als\"],\"hidden\":[]}");
    insertRow("another", SCREEN, GRID, "{\"order\":[\"Des\"],\"hidden\":[]}");

    mockMvc.perform(post("/action/maintain/resetGridLayout")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gridLayoutScreen\":\"" + SCREEN + "\",\"gridLayoutGrid\":\"" + GRID + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    assertEquals(0, countRows("test"));
    assertEquals(1, countRows("another"));
  }

  @Test
  @DisplayName("The screen is generated with the original column model plus the merged layout of the user")
  void screenCarriesTheLayout() throws Exception {
    // Des and Prg are in the same group header; Als, DwnFil... are ungrouped. The stored layout tries to
    // move Als into the group and mentions a column that does not exist anymore
    save("{\"order\":[\"Prg\",\"Als\",\"Des\",\"Gone\"],\"hidden\":[\"Gone\",\"Als\"]}");

    String screen = screenData();

    assertTrue(screen.contains("\"columnLayout\""), screen);
    tools.jackson.databind.JsonNode grid = gridOf(screen);
    tools.jackson.databind.JsonNode layout = grid.get("controller").get("columnLayout");
    // The original column model is untouched: XML order
    assertEquals("Als", grid.get("controller").get("columnModel").get(0).get("id").asText());
    // Group members swap among their own slots, ungrouped Als stays first, and the unknown column is dropped
    assertEquals("Als", layout.get("order").get(0).asText());
    assertEquals("Prg", layout.get("order").get(1).asText());
    assertEquals("Des", layout.get("order").get(2).asText());
    assertEquals(1, layout.get("hidden").size());
    assertEquals("Als", layout.get("hidden").get(0).asText());
  }

  @Test
  @DisplayName("Without a stored layout the grid carries no column layout, but it knows its screen")
  void screenWithoutLayout() throws Exception {
    tools.jackson.databind.JsonNode controller = gridOf(screenData()).get("controller");

    assertNull(controller.get("columnLayout"));
    assertEquals(SCREEN, controller.get("screenId").asText());
    assertTrue(controller.get("showToolbar").asBoolean());
  }

  @Test
  @DisplayName("A grid that is not in the screen cannot get a layout")
  void layoutOfAnUnknownGridIsNotStored() throws Exception {
    mockMvc.perform(post("/action/maintain/saveGridLayout")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gridLayoutScreen\":\"" + SCREEN + "\",\"gridLayoutGrid\":\"NoSuchGrid\",\"gridLayoutJson\":\"{\\\"order\\\":[],\\\"hidden\\\":[]}\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn();

    assertEquals(0, countRows("test"));
  }

  @Test
  @DisplayName("The layout of another user is not applied")
  void anotherUsersLayoutIsNotApplied() throws Exception {
    insertRow("another", SCREEN, GRID, "{\"order\":[\"Des\"],\"hidden\":[\"Als\"]}");

    assertNull(gridOf(screenData()).get("controller").get("columnLayout"));
  }

  @Test
  @DisplayName("An invalid layout is not stored")
  void invalidLayoutIsNotStored() throws Exception {
    mockMvc.perform(post("/action/maintain/saveGridLayout")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gridLayoutScreen\":\"" + SCREEN + "\",\"gridLayoutGrid\":\"" + GRID + "\",\"gridLayoutJson\":\"nope\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn();

    assertEquals(0, countRows("test"));
  }

  private tools.jackson.databind.JsonNode gridOf(String screen) throws Exception {
    for (tools.jackson.databind.JsonNode component : objectMapper.readTree(screen).get("components")) {
      if (GRID.equals(component.get("id").asText())) {
        return component;
      }
    }
    throw new IllegalStateException("Grid not found in the screen");
  }

  private String screenData() throws Exception {
    MvcResult result = mockMvc.perform(post("/screen-data/matrix-test")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"module\":\"Test\",\"option\":\"matrix-test\",\"view\":\"report\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    return result.getResponse().getContentAsString();
  }

  private void save(String layout) throws Exception {
    String escaped = layout.replace("\\", "\\\\").replace("\"", "\\\"");
    mockMvc.perform(post("/action/maintain/saveGridLayout")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gridLayoutScreen\":\"" + SCREEN + "\",\"gridLayoutGrid\":\"" + GRID + "\",\"gridLayoutJson\":\"" + escaped + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
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
         PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM AweUsrGrdCnf WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private String storedLayout(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT Lay FROM AweUsrGrdCnf WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    }
  }

  private void insertRow(String user, String screen, String grid, String layout) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweUsrGrdCnf (IdeUsrGrdCnf, Ope, Scr, GrdId, Lay) VALUES ((SELECT COALESCE(MAX(IdeUsrGrdCnf), 0) + 1 FROM AweUsrGrdCnf), ?, ?, ?, ?)")) {
      statement.setString(1, user);
      statement.setString(2, screen);
      statement.setString(3, grid);
      statement.setString(4, layout);
      statement.executeUpdate();
    }
  }

  private void deleteLayouts() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweUsrGrdCnf")) {
      statement.executeUpdate();
    }
  }
}
