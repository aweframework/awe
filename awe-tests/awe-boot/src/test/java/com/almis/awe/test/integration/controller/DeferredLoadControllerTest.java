package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The initial load of the components of a deferred dialog or tab page runs when it is opened, not with the screen (#824).
 * The test screen counts how many times the query of each one of its components has run.
 */
@Tag("integration")
@DisplayName("Deferred load of dialogs and tabs")
@WithMockUser(username = "test", password = "test")
class DeferredLoadControllerTest extends AbstractSpringFixedEnvironmentIT {

  private static final String SESSION_ID = "5e4f1b86-7a63-4b0b-8a0c-2d9f0e1b3c77";
  private static final String OPTION = "matrix-test-deferred";
  private static final String SCREEN_PARAMETERS = "{\"view\":\"base\",\"option\":\"" + OPTION + "\"}";

  private MockHttpSession session;

  @BeforeEach
  void loadScreen() throws Exception {
    // The private menu depends on the restriction stored in the session
    session = new MockHttpSession(null, SESSION_ID);
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session));

    // Loading the screen forgets the calls (on-load maintain), then runs the initial loads of what is not deferred
    screenData();
  }

  private JsonNode screenData() throws Exception {
    MvcResult result = mockMvc.perform(post("/screen-data/" + OPTION)
        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SCREEN_PARAMETERS).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode action(String action, String target, String parameters) throws Exception {
    MvcResult result = mockMvc.perform(post("/action/" + action + "/" + target)
        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(parameters).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode component(JsonNode screenData, String id) {
    for (JsonNode component : screenData.get("components")) {
      if (id.equals(component.get("id").asText())) {
        return component;
      }
    }
    throw new AssertionError("Component not found: " + id);
  }

  private int rows(JsonNode screenData, String id) {
    return component(screenData, id).get("model").get("values").size();
  }

  /**
   * Number of times the query of each component has run, as the test screen reports it
   */
  private Map<String, Integer> calls() throws Exception {
    return calls(session);
  }

  private Map<String, Integer> calls(MockHttpSession callSession) throws Exception {
    MvcResult result = mockMvc.perform(post("/action/data/DeferredLoadCalls")
        .session(callSession).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SCREEN_PARAMETERS).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    JsonNode answer = objectMapper.readTree(result.getResponse().getContentAsString());
    Map<String, Integer> calls = new HashMap<>();
    for (JsonNode action : answer) {
      if ("fill".equals(action.get("type").asText())) {
        for (JsonNode row : action.get("parameters").get("datalist").get("rows")) {
          calls.put(row.get("id").asText(), row.get("calls").asInt());
        }
      }
    }
    return calls;
  }

  private JsonNode deferredData(String container, String extraParameters) throws Exception {
    return action("deferred-data", container, "{\"view\":\"base\",\"option\":\"" + OPTION + "\"" + extraParameters + "}");
  }

  private JsonNode initialModel(JsonNode answer, String component) {
    for (JsonNode action : answer) {
      if ("initial-model".equals(action.get("type").asText()) && component.equals(action.get("target").asText())) {
        return action;
      }
    }
    return null;
  }

  @Test
  @DisplayName("The screen load does not run the queries of a deferred dialog or tab page")
  void screenLoadSkipsDeferredQueries() throws Exception {
    JsonNode screenData = screenData();

    // Deferred: registered with their controller, but empty
    for (String id : new String[]{"DefSelect", "GrdDefRows", "DefText", "TabSecondSelect", "GrdTabRows"}) {
      assertEquals(0, rows(screenData, id), id + " must not be loaded with the screen");
    }

    // Not deferred: loaded as always
    assertEquals(3, rows(screenData, "EagSelect"));
    assertEquals(3, rows(screenData, "TabFirstSelect"));

    // The client knows which containers wait for their opening
    assertTrue(component(screenData, "DlgDeferred").get("controller").get("deferred").asBoolean());
    assertTrue(component(screenData, "DefSecond").get("controller").get("deferred").asBoolean());
    assertFalse(component(screenData, "DlgEager").get("controller").has("deferred"));
    assertFalse(component(screenData, "DefFirst").get("controller").has("deferred"));
  }

  @Test
  @DisplayName("The queries of the deferred components have not run after the screen load")
  void deferredQueriesHaveNotRunAfterTheScreenLoad() throws Exception {
    Map<String, Integer> calls = calls();

    assertEquals(0, calls.get("DefSelect"));
    assertEquals(0, calls.get("DefRows"));
    assertEquals(0, calls.get("DefText"));
    assertEquals(0, calls.get("TabSecondSelect"));
    assertEquals(0, calls.get("TabSecondRows"));
    assertEquals(1, calls.get("EagSelect"));
    assertEquals(1, calls.get("TabFirstSelect"));
  }

  @Test
  @DisplayName("Opening a deferred dialog runs the queries of its components once and returns their initial model")
  void firstOpeningLoadsTheDialog() throws Exception {
    JsonNode answer = deferredData("DlgDeferred", "");

    JsonNode select = initialModel(answer, "DefSelect");
    assertNotNull(select);
    assertEquals(3, select.get("parameters").get("datalist").get("rows").size());
    assertTrue(select.get("parameters").has("selected"));
    assertEquals(3, initialModel(answer, "GrdDefRows").get("parameters").get("datalist").get("rows").size());
    assertEquals("Loaded", initialModel(answer, "DefText").get("parameters").get("selected").get(0).asText());

    // The deferred tab was not opened: it keeps waiting
    Map<String, Integer> calls = calls();
    assertEquals(1, calls.get("DefSelect"));
    assertEquals(1, calls.get("DefRows"));
    assertEquals(1, calls.get("DefText"));
    assertEquals(0, calls.get("TabSecondSelect"));
    assertEquals(0, calls.get("TabSecondRows"));
  }

  @Test
  @DisplayName("The calls are counted by session: another session does not see them")
  void callsAreCountedBySession() throws Exception {
    deferredData("DlgDeferred", "");
    MockHttpSession other = new MockHttpSession(null, "c3a9e2d4-5b71-4f0a-8d36-7e1b9a0c2f55");
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(other));

    assertEquals(1, calls().get("DefSelect"));
    assertEquals(0, calls(other).get("DefSelect"));
    assertEquals(0, calls(other).get("EagSelect"));
  }

  @Test
  @DisplayName("Opening a deferred tab page runs the queries of its components")
  void firstSelectionLoadsTheTabPage() throws Exception {
    JsonNode answer = deferredData("DefSecond", "");

    assertEquals(3, initialModel(answer, "TabSecondSelect").get("parameters").get("datalist").get("rows").size());
    assertEquals(3, initialModel(answer, "GrdTabRows").get("parameters").get("datalist").get("rows").size());
    Map<String, Integer> calls = calls();
    assertEquals(1, calls.get("TabSecondSelect"));
    assertEquals(1, calls.get("TabSecondRows"));
    assertEquals(0, calls.get("DefSelect"));
  }

  @Test
  @DisplayName("Components which already have data or a value are not loaded again, or lose their value")
  void componentsWithDataAreLeftAsTheyAre() throws Exception {
    JsonNode answer = deferredData("DlgDeferred", ",\"deferredLoaded\":[\"GrdDefRows\"],\"deferredValued\":[\"DefText\"]");

    // Already loaded: the query does not even run
    assertNull(initialModel(answer, "GrdDefRows"));
    assertEquals(0, calls().get("DefRows"));

    // Already valued: the options load, the value stays
    JsonNode text = initialModel(answer, "DefText");
    assertNotNull(text);
    assertTrue(text.get("parameters").has("datalist"));
    assertFalse(text.get("parameters").has("selected"));
  }

  @Test
  @DisplayName("Only deferred containers of the screen can be loaded on demand")
  void unknownOrNotDeferredContainersAreRejected() throws Exception {
    for (String container : new String[]{"DlgEager", "DoesNotExist"}) {
      JsonNode answer = deferredData(container, "");

      assertEquals(0, countActions(answer, "initial-model"), container + " has no deferred data to load");
      assertTrue(countActions(answer, "message") > 0, container + " must report the error");
    }
  }

  private int countActions(JsonNode answer, String type) {
    int count = 0;
    for (JsonNode action : answer) {
      if (type.equals(action.get("type").asText())) {
        count++;
      }
    }
    return count;
  }
}
