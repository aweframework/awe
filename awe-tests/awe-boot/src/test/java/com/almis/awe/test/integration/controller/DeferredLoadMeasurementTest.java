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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the deferred loading saves in the screen load (#824). The heavy screen has two heavy dialogs and two heavy tab
 * pages (3000 rows each), one of each kind deferred. The screen load carries the data of the heavy components which are
 * not deferred; what the deferred ones would have added to it is what the first opening of each one sends.
 */
@Tag("integration")
@DisplayName("Deferred load measurement")
@WithMockUser(username = "test", password = "test")
class DeferredLoadMeasurementTest extends AbstractSpringFixedEnvironmentIT {

  private static final String OPTION = "matrix-test-deferred-heavy";
  private static final String PARAMETERS = "{\"view\":\"base\",\"option\":\"" + OPTION + "\"}";

  private MockHttpSession session;

  @BeforeEach
  void prepareSession() throws Exception {
    session = new MockHttpSession(null, "8b1d2f5c-3a74-4c0e-9b6f-5d7a1c2e4f90");
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session));
  }

  private record Measure(int bytes, JsonNode body) {
  }

  private Measure call(String url) throws Exception {
    MvcResult result = mockMvc.perform(post(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(PARAMETERS).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    return new Measure(content.getBytes(StandardCharsets.UTF_8).length, objectMapper.readTree(content));
  }

  @Test
  @DisplayName("The screen load leaves out the data of the deferred dialogs and tabs until they open")
  void deferredLoadingMakesTheScreenLoadLighter() throws Exception {
    Measure screen = call("/screen-data/" + OPTION);
    int deferredBytes = call("/action/deferred-data/DlgHeaDeferred").bytes() + call("/action/deferred-data/HeaSecond").bytes();

    // What the deferred containers carry is not in the screen load: the deferred ones are half of the heavy data
    assertTrue(screen.bytes() < (screen.bytes() + deferredBytes) * 0.6,
      "The screen load must be at least 40% lighter than loading everything");

    // The heavy data of the deferred containers reaches the client only when they open
    assertEquals(0, rows(screen.body(), "GrdHeaDeferred"));
    assertEquals(0, rows(screen.body(), "GrdHeaSecond"));
    assertEquals(3000, rows(screen.body(), "GrdHeaEager"));
    assertEquals(3000, rows(screen.body(), "GrdHeaFirst"));
  }

  private int rows(JsonNode screenData, String id) {
    for (JsonNode component : screenData.get("components")) {
      if (id.equals(component.get("id").asText())) {
        return component.get("model").get("values").size();
      }
    }
    throw new AssertionError("Component not found: " + id);
  }
}
