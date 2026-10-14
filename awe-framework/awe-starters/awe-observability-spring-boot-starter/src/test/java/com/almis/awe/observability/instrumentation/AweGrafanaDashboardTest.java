package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reference Grafana dashboard that ships with the starter renders the metrics the starter emits.
 */
class AweGrafanaDashboardTest {

  static final String DASHBOARD = "awe-observability/grafana/awe-observability-dashboard.json";
  private static final Pattern AWE_METRIC = Pattern.compile("\\bawe_[a-z_]+");

  private JsonNode dashboard() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(DASHBOARD)) {
      assertThat(stream).as("Dashboard resource " + DASHBOARD).isNotNull();
      return new ObjectMapper().readTree(stream);
    }
  }

  private List<JsonNode> panels(JsonNode dashboard) {
    List<JsonNode> panels = new ArrayList<>();
    dashboard.path("panels").forEach(panel -> {
      panels.add(panel);
      panel.path("panels").forEach(panels::add);
    });
    return panels;
  }

  private List<String> expressions(JsonNode dashboard) {
    List<String> expressions = new ArrayList<>();
    panels(dashboard).forEach(panel -> panel.path("targets").forEach(target -> expressions.add(target.path("expr").asText())));
    dashboard.path("templating").path("list").forEach(variable -> expressions.add(variable.path("query").path("query").asText()));
    return expressions;
  }

  @Test
  @DisplayName("The dashboard is a Grafana dashboard with an application filter and a data source variable")
  void isADashboard() throws IOException {
    JsonNode dashboard = dashboard();

    assertThat(dashboard.path("uid").asText()).isEqualTo("awe-observability");
    assertThat(dashboard.path("title").asText()).isEqualTo("AWE Observability");
    assertThat(dashboard.path("panels")).isNotEmpty();
    Set<String> variables = new LinkedHashSet<>();
    dashboard.path("templating").path("list").forEach(variable -> variables.add(variable.path("name").asText()));
    assertThat(variables).contains("datasource", "application");
  }

  @Test
  @DisplayName("Every panel has an id of its own and queries the data source variable")
  void panelsAreWellFormed() throws IOException {
    List<JsonNode> panels = panels(dashboard());
    Set<Integer> ids = new LinkedHashSet<>();

    for (JsonNode panel : panels) {
      assertThat(ids.add(panel.path("id").asInt(-1))).as("Unique id of panel " + panel.path("title")).isTrue();
      if (!"row".equals(panel.path("type").asText())) {
        assertThat(panel.path("targets")).as("Targets of " + panel.path("title")).isNotEmpty();
        panel.path("targets").forEach(target ->
          assertThat(target.path("datasource").path("uid").asText()).isEqualTo("${datasource}"));
      }
    }
  }

  @Test
  @DisplayName("Every AWE metric the dashboard queries is one the starter emits, with the labels it filters by")
  void queriesAreAnsweredByTheMetricsOfTheStarter() throws IOException, AWException {
    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
    MicrometerExecutionObserver observer = new MicrometerExecutionObserver(() -> registry);
    for (ExecutionType type : ExecutionType.values()) {
      observer.observe(type, "Id1", type == ExecutionType.QUERY ? Map.of("connector", "sql") : Map.of(), () -> "ok");
    }
    String scrape = meterRegistry.scrape();

    Set<String> queriedMetrics = new LinkedHashSet<>();
    for (String expression : expressions(dashboard())) {
      Matcher matcher = AWE_METRIC.matcher(expression);
      while (matcher.find()) {
        queriedMetrics.add(matcher.group());
      }
    }

    assertThat(queriedMetrics).isNotEmpty();
    for (String metric : queriedMetrics) {
      assertThat(scrape).as("Metric " + metric + " in the Prometheus scrape").containsPattern("(?m)^" + metric + "(\\{| )");
    }
    // The labels the dashboard groups and filters by
    assertThat(scrape).contains("id=\"Id1\"").contains("outcome=\"success\"").contains("connector=\"sql\"");
  }
}
