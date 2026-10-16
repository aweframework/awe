package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import lombok.extern.slf4j.Slf4j;
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
 * Measures what the deferred loading saves in the screen load (#824). The heavy screen has two heavy dialogs and two heavy tab
 * pages (3000 rows each), one of each kind deferred. The screen load carries the data of the heavy components which are
 * not deferred; what the deferred ones would have added to it is what the first opening of each one sends.
 */
@Slf4j
@Tag("integration")
@DisplayName("Deferred load measurement")
@WithMockUser(username = "test", password = "test")
class DeferredLoadMeasurementTest extends AbstractSpringFixedEnvironmentIT {

  private static final String OPTION = "matrix-test-deferred-heavy";
  private static final String PARAMETERS = "{\"view\":\"base\",\"option\":\"" + OPTION + "\"}";
  private static final int RUNS = 5;

  private MockHttpSession session;

  @BeforeEach
  void prepareSession() throws Exception {
    session = new MockHttpSession(null, "8b1d2f5c-3a74-4c0e-9b6f-5d7a1c2e4f90");
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session));
  }

  private record Measure(int bytes, double millis, JsonNode body) {
  }

  private Measure call(String url) throws Exception {
    long start = System.nanoTime();
    MvcResult result = mockMvc.perform(post(url).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(PARAMETERS).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    double millis = (System.nanoTime() - start) / 1_000_000.0;
    return new Measure(content.getBytes(StandardCharsets.UTF_8).length, millis, objectMapper.readTree(content));
  }

  private static double median(double[] values) {
    double[] sorted = values.clone();
    java.util.Arrays.sort(sorted);
    return sorted[sorted.length / 2];
  }

  @Test
  @DisplayName("The screen load is smaller and faster when the heavy dialogs and tabs are deferred")
  void deferredLoadingMakesTheScreenLoadLighter() throws Exception {
    // Warm up (compiled queries, JSON mapper, screen cache)
    call("/screen-data/" + OPTION);

    double[] screenTimes = new double[RUNS];
    double[] deferredTimes = new double[RUNS];
    int screenBytes = 0;
    int deferredBytes = 0;
    for (int run = 0; run < RUNS; run++) {
      Measure screen = call("/screen-data/" + OPTION);
      Measure dialog = call("/action/deferred-data/DlgHeaDeferred");
      Measure tab = call("/action/deferred-data/HeaSecond");
      screenTimes[run] = screen.millis();
      deferredTimes[run] = dialog.millis() + tab.millis();
      screenBytes = screen.bytes();
      deferredBytes = dialog.bytes() + tab.bytes();
    }

    int eagerEquivalentBytes = screenBytes + deferredBytes;
    double screenMillis = median(screenTimes);
    double eagerEquivalentMillis = screenMillis + median(deferredTimes);
    logger.info("[#824 measurement] screen load WITH deferred containers: {} bytes, {} ms (median of {} runs)", screenBytes, String.format("%.0f", screenMillis), RUNS);
    logger.info("[#824 measurement] screen load WITHOUT the attribute (screen + first openings): {} bytes, {} ms", eagerEquivalentBytes, String.format("%.0f", eagerEquivalentMillis));
    logger.info("[#824 measurement] gain: {}% of the response, {}% of the server time", Math.round(100.0 * deferredBytes / eagerEquivalentBytes),
      Math.round(100.0 * (eagerEquivalentMillis - screenMillis) / eagerEquivalentMillis));

    // What the deferred containers carry is not in the screen load: the deferred ones are half of the heavy data.
    // The times are only reported: they depend on the machine and are not compared
    assertTrue(screenBytes < eagerEquivalentBytes * 0.6, "The screen load must be at least 40% lighter than loading everything");

    // The heavy data of the deferred containers reaches the client only when they open
    assertEquals(0, rows(call("/screen-data/" + OPTION).body(), "GrdHeaDeferred"));
    assertEquals(0, rows(call("/screen-data/" + OPTION).body(), "GrdHeaSecond"));
    assertEquals(3000, rows(call("/screen-data/" + OPTION).body(), "GrdHeaEager"));
    assertEquals(3000, rows(call("/screen-data/" + OPTION).body(), "GrdHeaFirst"));
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
