package com.almis.awe.test.integration.spring;

import com.almis.awe.exception.AWException;
import com.almis.awe.observability.autoconfigure.AweObservabilityProperties;
import com.almis.awe.observability.instrumentation.AweTagValueLimitFilter;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The observability starter on the test application: it only needs the dependency to expose the health of the
 * AWE connectors, to measure and trace the query and maintain executions and to count the usage of the application
 * (screens, exports, connected users) with bounded tags.
 */
@Tag("integration")
@DisplayName("Observability starter Tests")
@WithMockUser
@Transactional
@AutoConfigureMetrics
@AutoConfigureTracing
// The test application turns the Prometheus export off: it is turned on again to read the scrape as a monitoring stack does
@TestPropertySource(properties = {"management.tracing.sampling.probability=1.0", "management.endpoint.health.show-details=always",
  "management.prometheus.metrics.export.enabled=true"})
class ObservabilityStarterTest extends AbstractSpringAppIntegrationTest {

  @TestConfiguration
  static class SpanCollector {
    static final List<SpanData> SPANS = new CopyOnWriteArrayList<>();

    @Bean
    SpanProcessor collectingSpanProcessor() {
      return new SpanProcessor() {
        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
          // Only finished spans are collected
        }

        @Override
        public boolean isStartRequired() {
          return false;
        }

        @Override
        public void onEnd(ReadableSpan span) {
          SPANS.add(span.toSpanData());
        }

        @Override
        public boolean isEndRequired() {
          return true;
        }

        @Override
        public CompletableResultCode shutdown() {
          return CompletableResultCode.ofSuccess();
        }
      };
    }
  }

  @Autowired
  private QueryService queryService;

  @Autowired
  private MaintainService maintainService;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private AweObservabilityProperties observabilityProperties;

  private static final String OPTION = "criteria-test-left";
  private static final String SCREEN = "CrtTstLeft";

  @Test
  @DisplayName("The health endpoint reports the database (the test application has no JMS broker, so no queue)")
  void healthChecksConnectors() throws Exception {
    // The overall status also depends on connectors of the environment (LDAP...), so only the database is checked
    mockMvc.perform(get("/actuator/health"))
      .andExpect(jsonPath("$.components.db.status").value("UP"));
  }

  @Test
  @DisplayName("A query execution is a timer of the metrics endpoint, tagged with the query id")
  void queryMetrics() throws Exception {
    queryService.launchQuery("testServiceByQualifier");

    assertThat(meterRegistry.get("awe.query").tags("id", "testServiceByQualifier", "connector", "service", "outcome", "success")
      .timer().count()).isGreaterThanOrEqualTo(1);
    mockMvc.perform(get("/actuator/metrics/awe.query").param("tag", "id:testServiceByQualifier"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("awe.query"))
      .andExpect(jsonPath("$.measurements[?(@.statistic=='COUNT')].value").isNotEmpty());
  }

  @Test
  @DisplayName("A maintain execution is a timer tagged with the maintain id")
  void maintainMetrics() throws AWException {
    maintainService.launchMaintain("SimpleSingleInsertFromVariableValue");

    assertThat(meterRegistry.get("awe.maintain").tags("id", "SimpleSingleInsertFromVariableValue")
      .timer().count()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("An unknown id does not create a new tag value")
  void unknownIdIsBounded() {
    try {
      queryService.launchQuery("notDefinedAnywhere");
    } catch (AWException exc) {
      // The query does not exist, which is the point
    }

    assertThat(meterRegistry.find("awe.query").tag("id", "notDefinedAnywhere").timer()).isNull();
  }

  @Test
  @DisplayName("A query execution is a span with the query id")
  void queryTraces() throws AWException {
    queryService.launchQuery("testServiceByQualifier");

    assertThat(SpanCollector.SPANS).anySatisfy(span -> {
      assertThat(span.getName()).isEqualTo("awe.query");
      assertThat(span.getAttributes().asMap().entrySet())
        .anyMatch(attribute -> "id".equals(attribute.getKey().getKey()) && "testServiceByQualifier".equals(attribute.getValue()));
      assertThat(span.getTraceId()).isNotBlank();
    });
  }

  @Test
  @DisplayName("Opening a screen of the menu is a timer tagged with the option and the screen, exposed to Prometheus")
  void screenMetrics() throws Exception {
    openScreen(adminSession(), OPTION, "{\"view\":\"base\",\"option\":\"" + OPTION + "\"}");

    assertThat(meterRegistry.get("awe.screen").tags("id", OPTION, "screen", SCREEN, "outcome", "success")
      .timer().count()).isGreaterThanOrEqualTo(1);
    mockMvc.perform(get("/actuator/prometheus"))
      .andExpect(status().isOk())
      .andExpect(content().string(matchesPattern(
        "(?s).*awe_screen_seconds_count\\{[^}]*id=\"" + OPTION + "\"[^}]*screen=\"" + SCREEN + "\"[^}]*}.*")));
  }

  @Test
  @DisplayName("Opening screens 1000 times with forged options and random parameters adds no series")
  void screenCardinalityIsBounded() throws Exception {
    MockHttpSession session = adminSession();
    // Error screens are options of the menu too: a forged option shows the not found screen
    openScreen(session, OPTION, "{\"view\":\"base\",\"option\":\"" + OPTION + "\"}");
    openScreen(session, "forged" + UUID.randomUUID().toString().replace("-", ""), "{}");
    Set<String> before = screenIds();

    for (int i = 0; i < 1000; i++) {
      String random = UUID.randomUUID().toString().replace("-", "");
      String option = i % 2 == 0 ? "forged" + random : OPTION;
      openScreen(session, option, "{\"view\":\"base\",\"option\":\"" + option + "\",\"p" + random + "\":\"" + random + "\"}");
    }

    Set<String> after = screenIds();
    assertThat(after).isEqualTo(before).noneMatch(id -> id.startsWith("forged"))
      .hasSizeLessThanOrEqualTo(observabilityProperties.getMaxTagValues());
    assertThat(meterRegistry.get("awe.screen").tags("id", OPTION, "screen", SCREEN).timer().count()).isGreaterThanOrEqualTo(501);
  }

  @Test
  @DisplayName("The cap of the tag values and the connected users gauges apply to the registry of the application")
  void usageBeansApplyToTheRegistry() {
    int cap = observabilityProperties.getMaxTagValues();
    String probe = "awe.cardinality.probe";
    try {
      IntStream.rangeClosed(0, cap).forEach(i -> meterRegistry.timer(probe, "id", "probe" + i).record(Duration.ofMillis(1)));

      Set<String> ids = meterRegistry.find(probe).timers().stream()
        .map(timer -> timer.getId().getTag("id")).collect(Collectors.toSet());
      // The first values keep their own series, the rest share the 'other' one
      assertThat(ids).hasSize(cap + 1).contains("probe0", "probe" + (cap - 1), AweTagValueLimitFilter.OTHER)
        .doesNotContain("probe" + cap);
    } finally {
      meterRegistry.find(probe).meters().forEach(meterRegistry::remove);
    }

    assertThat(meterRegistry.get("awe.users.connected").gauge().value()).isGreaterThanOrEqualTo(0);
    assertThat(meterRegistry.get("awe.connections.active").gauge().value()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("Printing a screen is a timer tagged with the screen and the format")
  void exportMetrics() throws Exception {
    mockMvc.perform(post("/action/maintain/ExpFil").session(adminSession()).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .content("{\"PrnScr\":\"" + SCREEN + "\",\"ActPrn\":\"CREATE\",\"FmtPrn\":\"CSV\"}"))
      .andExpect(status().isOk());

    assertThat(meterRegistry.get("awe.export").tags("id", SCREEN, "format", "csv", "outcome", "success")
      .timer().count()).isGreaterThanOrEqualTo(1);
  }

  /**
   * Session with the menu of an administrator
   */
  private MockHttpSession adminSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", "test").session(session)).andExpect(status().isOk());
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session))
      .andExpect(status().isOk());
    return session;
  }

  private void openScreen(MockHttpSession session, String option, String parameters) throws Exception {
    mockMvc.perform(post("/screen-data/" + option).session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(parameters).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private Set<String> screenIds() {
    return meterRegistry.find("awe.screen").timers().stream()
      .map(Timer::getId).map(id -> id.getTag("id")).collect(Collectors.toSet());
  }
}
