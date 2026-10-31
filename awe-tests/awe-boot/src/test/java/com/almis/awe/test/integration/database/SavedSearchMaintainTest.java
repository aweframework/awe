package com.almis.awe.test.integration.database;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.screen.component.Window;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saved searches of the advanced block of a window (#790): an administrator (access to the profiles option) saves own
 * searches and publishes them for a profile; a general user (the tools menu restricted) saves own searches, sees the
 * ones published for the profile, and cannot change them nor publish.
 */
@Tag("integration")
@DisplayName("Saved search tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class SavedSearchMaintainTest extends AbstractSpringAppIntegrationTest {

  private static final String SCREEN = "AdvSchTst";
  private static final String WINDOW = "WinAdvInl";
  private static final String WINDOW_WITHOUT_BLOCK = "WinAdvInlGrd";
  private static final String ADMIN = "test";
  private static final String GENERAL = "donald";
  private static final String VALUES = "{\"AdvInlNam\":[{\"value\":\"Ana\",\"label\":\"Ana\"}],\"AdvInlCtr\":[{\"value\":\"FR\",\"label\":\"France\"}]}";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private AweElements aweElements;

  private MockHttpSession admin;
  private MockHttpSession general;
  private Boolean savedSearches;

  @BeforeEach
  void setUp() throws Exception {
    // The window opts in for the test (and gets its own value back after it)
    Window window = window();
    savedSearches = window.getSavedSearches();
    window.setSavedSearches(true);

    admin = session(ADMIN, "ADM", "administrator");
    general = session(GENERAL, "GNR", "general");
    deleteSearches();
  }

  @AfterEach
  void cleanUp() throws Exception {
    window().setSavedSearches(savedSearches);
    deleteSearches();
  }

  @Test
  @DisplayName("An own search is stored for the user, saving the same name replaces its values, and only its owner sees it")
  void ownSearch() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "Mine", VALUES, null));
    assertEquals("ok", save(admin, ADMIN, "Mine", "{\"AdvInlSta\":[{\"value\":\"1\",\"label\":\"Active\"}]}", ""));

    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'test' AND Pro IS NULL AND Nam = 'Mine'"));
    List<Map<String, Object>> searches = searches(admin, ADMIN);
    assertEquals(1, searches.size());
    assertEquals("Mine", searches.get(0).get("name"));
    assertEquals("{\"AdvInlSta\":[{\"value\":\"1\",\"label\":\"Active\"}]}", searches.get(0).get("values"));
    assertEquals("", searches.get(0).get("profile"));
    assertEquals(1, ((Number) searches.get(0).get("own")).intValue());
    assertTrue(searches(general, GENERAL).isEmpty(), "The search of another user is not listed");
  }

  @Test
  @DisplayName("The user comes from the session, a user parameter in the request is ignored")
  void userParameterIsIgnored() throws Exception {
    String type = maintain(general, GENERAL, "saveWindowSearch", "{\"savedSearchScreen\":\"" + SCREEN + "\",\"savedSearchWindow\":\"" + WINDOW
      + "\",\"savedSearchName\":\"Mine\",\"savedSearchValues\":" + quote(VALUES) + ",\"user\":\"mallory\",\"Ope\":\"mallory\",\"max\":30}");

    assertEquals("ok", type);
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'donald'"));
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'mallory'"));
  }

  @Test
  @DisplayName("An administrator publishes a search for a profile: its users see it, read-only, and the administrator manages it")
  void publishedSearch() throws Exception {
    assertTrue(profiles(admin, ADMIN).stream().map(row -> row.get("value")).toList().containsAll(List.of("ADM", "GNR")));

    assertEquals("ok", save(admin, ADMIN, "Shared", VALUES, "GNR"));
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope IS NULL AND Pro = 'GNR' AND Nam = 'Shared'"));
    // The administrator may publish, so it lists the searches published for every profile, to manage them
    List<Map<String, Object>> published = searches(admin, ADMIN);
    assertEquals(List.of("Shared"), published.stream().map(row -> row.get("name")).toList(),
      "A search published for another profile is listed for the administrator");
    assertEquals("GNR", published.get(0).get("profile"));
    assertEquals(0, ((Number) published.get(0).get("own")).intValue());

    assertEquals("ok", save(general, GENERAL, "Mine", VALUES, null));
    List<Map<String, Object>> searches = searches(general, GENERAL);
    assertEquals(List.of("Mine", "Shared"), searches.stream().map(row -> row.get("name")).toList(), "Own first, then shared");
    assertEquals("GNR", searches.get(1).get("profile"));
    assertEquals(0, ((Number) searches.get(1).get("own")).intValue());
    Object sharedId = searches.get(1).get("id");

    // The users of the profile cannot change it
    assertEquals("error", maintain(general, GENERAL, "renameWindowSearch", "{\"savedSearchId\":" + sharedId + ",\"savedSearchName\":\"Hacked\"}"));
    assertEquals("error", maintain(general, GENERAL, "deleteWindowSearch", "{\"savedSearchId\":\"" + sharedId + "\"}"));
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Pro = 'GNR' AND Nam = 'Shared'"));

    assertEquals(sharedId, published.get(0).get("id"), "The administrator manages the same search its users see");

    // The administrator renames it (id as a number) and deletes it (id as a string)
    assertEquals("ok", maintain(admin, ADMIN, "renameWindowSearch", "{\"savedSearchId\":" + sharedId + ",\"savedSearchName\":\"Renamed\"}"));
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Pro = 'GNR' AND Nam = 'Renamed'"));
    assertEquals("ok", maintain(admin, ADMIN, "deleteWindowSearch", "{\"savedSearchId\":\"" + sharedId + "\"}"));
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch WHERE Pro = 'GNR'"));
  }

  @Test
  @DisplayName("A user without access to the profiles option cannot publish and gets no profiles")
  void generalUserCannotPublish() throws Exception {
    assertTrue(profiles(general, GENERAL).isEmpty());

    assertEquals("error", save(general, GENERAL, "Shared", VALUES, "GNR"));
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch"));
  }

  @Test
  @DisplayName("The targets of the published searches change nothing when they are launched directly")
  void publishedTargetsAreNotOpenToDirectCalls() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "Shared", VALUES, "GNR"));
    int id = count("SELECT IdeSavSch FROM AweSavSch WHERE Pro = 'GNR'");

    maintain(general, GENERAL, "insertProfileSavedSearch", "{\"screen\":\"" + SCREEN + "\",\"windowId\":\"" + WINDOW
      + "\",\"name\":\"Forged\",\"values\":\"{}\",\"profile\":\"GNR\",\"savedSearchPublishProfile\":\"GNR\"}");
    maintain(general, GENERAL, "renameProfileSavedSearch", "{\"id\":" + id + ",\"name\":\"Hacked\",\"profile\":\"GNR\"}");
    maintain(general, GENERAL, "updateProfileSavedSearchValues", "{\"id\":" + id + ",\"values\":\"{}\",\"profile\":\"GNR\"}");
    maintain(general, GENERAL, "deleteProfileSavedSearch", "{\"id\":" + id + ",\"profile\":\"GNR\"}");

    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch"), "Nothing is inserted without the profile of the session");
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Pro = 'GNR' AND Nam = 'Shared' AND Val = " + sqlQuote(VALUES)));
    assertEquals(List.of("Shared"), searches(general, GENERAL).stream().map(row -> row.get("name")).toList());
  }

  @Test
  @DisplayName("The targets of the own searches launched directly change nothing: only the service writes them")
  void ownTargetsAreNotOpenToDirectCalls() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "Mine", VALUES, null));
    assertEquals("ok", save(admin, ADMIN, "Other", VALUES, null));
    int id = count("SELECT IdeSavSch FROM AweSavSch WHERE Nam = 'Mine'");

    // Criteria of another window, a name already in use and values over the limit skip the checks of the service
    maintain(admin, ADMIN, "insertUserSavedSearch", "{\"screen\":\"" + SCREEN + "\",\"windowId\":\"" + WINDOW
      + "\",\"name\":\"Forged\",\"values\":\"{\\\"Unknown\\\":[]}\"}");
    maintain(admin, ADMIN, "updateUserSavedSearchValues", "{\"id\":" + id + ",\"values\":\"{\\\"Unknown\\\":[]}\"}");
    maintain(admin, ADMIN, "renameUserSavedSearch", "{\"id\":" + id + ",\"name\":\"Other\"}");

    assertEquals(2, count("SELECT COUNT(*) FROM AweSavSch"), "Nothing is inserted outside the service");
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Nam = 'Mine' AND Val = " + sqlQuote(VALUES)),
      "The search keeps its name and values");
  }

  @Test
  @DisplayName("Another user's own search cannot be renamed nor deleted, and its owner does both")
  void ownSearchOfAnotherUser() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "Mine", VALUES, null));
    Object id = searches(admin, ADMIN).get(0).get("id");

    assertEquals("error", maintain(general, GENERAL, "renameWindowSearch", "{\"savedSearchId\":" + id + ",\"savedSearchName\":\"Hacked\"}"));
    assertEquals("error", maintain(general, GENERAL, "deleteWindowSearch", "{\"savedSearchId\":" + id + "}"));
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'test' AND Nam = 'Mine'"));

    assertEquals("ok", maintain(admin, ADMIN, "renameWindowSearch", "{\"savedSearchId\":\"" + id + "\",\"savedSearchName\":\"Renamed\"}"));
    assertEquals(1, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'test' AND Nam = 'Renamed'"));
    assertEquals("ok", maintain(admin, ADMIN, "deleteWindowSearch", "{\"savedSearchId\":" + id + "}"));
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch"));
  }

  @Test
  @DisplayName("A window without saved searches, values of other criteria or invalid values are not stored as sent")
  void invalidSearchesAreRejected() throws Exception {
    assertEquals("error", maintain(admin, ADMIN, "saveWindowSearch", "{\"savedSearchScreen\":\"" + SCREEN + "\",\"savedSearchWindow\":\""
      + WINDOW_WITHOUT_BLOCK + "\",\"savedSearchName\":\"Mine\",\"savedSearchValues\":" + quote(VALUES) + "}"));
    assertEquals("error", save(admin, ADMIN, "Mine", "not json", null));
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch"));

    assertEquals("ok", save(admin, ADMIN, "Mine", "{\"AdvInlNam\":[{\"value\":\"Ana\",\"label\":\"Ana\"}],\"GrdAdvInl\":[{\"value\":1}]}", null));
    assertEquals("{\"AdvInlNam\":[{\"value\":\"Ana\",\"label\":\"Ana\"}]}", searches(admin, ADMIN).get(0).get("values"));
  }

  @Test
  @DisplayName("A user who may not publish does not read the searches published for another profile")
  void publishedSearchesOfOtherProfilesAreNotReadable() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "AdminOnly", VALUES, "ADM"));
    List<Map<String, Object>> direct = rows(general, GENERAL, "getPublishedSavedSearches",
      "{\"screen\":\"" + SCREEN + "\",\"windowId\":\"" + WINDOW + "\",\"max\":0}");
    assertTrue(direct.isEmpty(), "Published searches of ADM read by a GNR user: " + direct);
  }

  @Test
  @DisplayName("A user who may not publish does not read a search published for another profile by its identifier")
  void publishedSearchOfAnotherProfileIsNotReadableById() throws Exception {
    assertEquals("ok", save(admin, ADMIN, "AdminOnly", VALUES, "ADM"));
    Object id = count("SELECT IdeSavSch FROM AweSavSch WHERE Pro = 'ADM' AND Nam = 'AdminOnly'");
    List<Map<String, Object>> direct = rows(general, GENERAL, "getSavedSearch", "{\"id\":" + id + ",\"max\":0}");
    assertTrue(direct.isEmpty(), "Published search of ADM read by its identifier by a GNR user: " + direct);
  }

  private Window window() throws Exception {
    return aweElements.getScreen(SCREEN).getElementsById(WINDOW).stream()
      .filter(Window.class::isInstance).map(Window.class::cast).findFirst().orElseThrow();
  }

  private MockHttpSession session(String userName, String profile, String restriction) throws Exception {
    MockHttpSession session = new MockHttpSession();
    setParameter(session, userName, "user", userName);
    setParameter(session, userName, "profile", profile);
    setParameter(session, userName, "restriction", restriction);
    return session;
  }

  private static RequestPostProcessor as(String userName) {
    return user(userName).password("test").roles("USER");
  }

  private String save(MockHttpSession session, String userName, String name, String values, String profile) throws Exception {
    return maintain(session, userName, "saveWindowSearch", "{\"savedSearchScreen\":\"" + SCREEN + "\",\"savedSearchWindow\":\"" + WINDOW
      + "\",\"savedSearchName\":\"" + name + "\",\"savedSearchValues\":" + quote(values)
      + (profile == null ? "" : ",\"savedSearchProfile\":\"" + profile + "\"") + ",\"max\":30}");
  }

  /**
   * Launch a maintain target and return the type of its answer: ok, error...
   */
  private String maintain(MockHttpSession session, String userName, String target, String content) throws Exception {
    String result = mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .with(as(userName))
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return type(result);
  }

  private static String type(String result) {
    List<String> types = JsonPath.read(result, "$[?(@.type == 'message')].parameters.type");
    return types.isEmpty() ? "none" : types.get(0);
  }

  private List<Map<String, Object>> searches(MockHttpSession session, String userName) throws Exception {
    // The clients ask for the queries with max 0, as the suggests do: every row comes back
    return rows(session, userName, "getWindowSavedSearches",
      "{\"savedSearchScreen\":\"" + SCREEN + "\",\"savedSearchWindow\":\"" + WINDOW + "\",\"max\":0}");
  }

  private List<Map<String, Object>> profiles(MockHttpSession session, String userName) throws Exception {
    return rows(session, userName, "getSavedSearchProfiles", "{\"max\":0}");
  }

  private List<Map<String, Object>> rows(MockHttpSession session, String userName, String query, String content) throws Exception {
    String result = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .with(as(userName))
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows");
    assertEquals(1, rows.size(), "One fill answer for " + query + ": " + result);
    return rows.get(0);
  }

  private void setParameter(MockHttpSession session, String userName, String name, String value) throws Exception {
    mockMvc.perform(post("/session/set/" + name)
        .with(csrf())
        .with(as(userName))
        .param("value", value)
        .session(session))
      .andReturn();
  }

  private static String quote(String text) {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  private static String sqlQuote(String text) {
    return "'" + text.replace("'", "''") + "'";
  }

  private int count(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      assertTrue(resultSet.next(), "No result for " + sql);
      return resultSet.getInt(1);
    }
  }

  private void deleteSearches() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweSavSch")) {
      statement.executeUpdate();
    }
  }
}
