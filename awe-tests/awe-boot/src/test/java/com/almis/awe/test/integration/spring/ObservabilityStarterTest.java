package com.almis.awe.test.integration.spring;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The observability starter on the test application: it only needs the dependency to expose the health of the
 * AWE connectors and to measure and trace the query and maintain executions.
 */
@Tag("integration")
@DisplayName("Observability starter Tests")
@WithMockUser
@Transactional
@AutoConfigureMetrics
@AutoConfigureTracing
@TestPropertySource(properties = {"management.tracing.sampling.probability=1.0", "management.endpoint.health.show-details=always"})
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
}
