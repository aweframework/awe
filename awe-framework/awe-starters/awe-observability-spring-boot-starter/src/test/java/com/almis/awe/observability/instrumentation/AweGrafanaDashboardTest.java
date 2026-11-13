package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import com.almis.awe.service.observation.FileTransferObserver.Direction;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The reference Grafana dashboard that ships with the starter renders the metrics the starter emits.
 */
class AweGrafanaDashboardTest {

  static final String DASHBOARD = "awe-observability/grafana/awe-observability-dashboard.json";
  // The usage panels were merged into the observability dashboard
  static final String REMOVED_USAGE_DASHBOARD = "awe-observability/grafana/awe-usage-dashboard.json";
  private static final Pattern AWE_METRIC = Pattern.compile("\\bawe_[a-z_]+");
  // Labels the queries group by (by / without clauses) and label_values(metric, label) of the variables
  private static final Pattern GROUPING = Pattern.compile("\\b(?:by|without)\\s*\\(([^)]*)\\)");
  private static final Pattern LABEL_VALUES = Pattern.compile("label_values\\([^,]+,\\s*(\\w+)\\s*\\)");
  // Equality matchers with a literal value, the variables ($...) aside
  private static final Pattern LITERAL_MATCHER = Pattern.compile("\\b(\\w+)=\"([^\"$]+)\"");
  // Panels that count events: in-range totals (instant) and per-bucket totals (bar charts)
  private static final Set<String> RANGE_COUNTS = Set.of("Screens opened", "Logins", "Failed logins", "Exports",
    "Most opened options", "Screens with errors", "Most exported screens", "Exports by format",
    "Most executed query ids", "Queries with errors", "Most executed maintain ids", "Maintains with errors",
    "Most executed action ids", "Actions with errors");
  private static final Set<String> ZERO_WHEN_EMPTY = Set.of("Screens opened", "Logins", "Failed logins", "Exports");
  private static final Set<String> BUCKET_COUNTS = Set.of("Logins by channel", "Logouts by cause",
    "Files uploaded and downloaded", "Bytes uploaded and downloaded");
  private static final int GRID_WIDTH = 24;

  private JsonNode dashboard(String resource) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertThat(stream).as("Dashboard resource " + resource).isNotNull();
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

  private static Set<String> matches(Pattern pattern, String text) {
    Set<String> found = new LinkedHashSet<>();
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      found.add(matcher.group(matcher.groupCount() == 0 ? 0 : 1));
    }
    return found;
  }

  /**
   * Prometheus scrape of every metric the starter emits, one sample of each family and of each label it carries
   */
  private static String scrape() throws AWException {
    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    meterRegistry.config().commonTags("application", "awe-test");
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));

    MicrometerExecutionObserver observer = new MicrometerExecutionObserver(() -> registry);
    for (ExecutionType type : ExecutionType.values()) {
      Map<String, String> tags = switch (type) {
        case QUERY -> Map.of("connector", "sql");
        case SCREEN -> Map.of("screen", "Scr1");
        case EXPORT -> Map.of("format", "pdf");
        default -> Map.of();
      };
      observer.observe(type, "Id1", tags, () -> "ok");
      assertThatThrownBy(() -> observer.observe(type, "Id1", tags, () -> {
        throw new IllegalStateException("failure");
      })).isInstanceOf(IllegalStateException.class);
    }

    MicrometerFileTransferObserver files = new MicrometerFileTransferObserver(() -> meterRegistry);
    files.transferred(Direction.UPLOAD, 100);
    files.transferred(Direction.DOWNLOAD, 200);

    MicrometerSecurityAuditSink audit = new MicrometerSecurityAuditSink(() -> meterRegistry);
    audit.record(event(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.SUCCESS, SecurityAuditChannel.WEB, null));
    audit.record(event(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.FAILURE, SecurityAuditChannel.WEB,
      SecurityAuditCause.BAD_CREDENTIALS));
    audit.record(event(SecurityAuditEventType.LOGOUT, SecurityAuditOutcome.SUCCESS, SecurityAuditChannel.SYSTEM,
      SecurityAuditCause.EXPIRED));

    new AweConnectionMetrics(new DefaultSimpUserRegistry()).bindTo(meterRegistry);

    return meterRegistry.scrape();
  }

  private static SecurityAuditEvent event(SecurityAuditEventType type, SecurityAuditOutcome outcome,
                                          SecurityAuditChannel channel, SecurityAuditCause cause) {
    return SecurityAuditEvent.builder().type(type).outcome(outcome).channel(channel).cause(cause).username("jdoe").build();
  }

  @Test
  @DisplayName("The dashboard is a Grafana dashboard of the operations and the usage of the application")
  void isADashboard() throws IOException {
    JsonNode dashboard = dashboard(DASHBOARD);

    assertThat(dashboard.path("uid").asText()).isEqualTo("awe-observability");
    assertThat(dashboard.path("title").asText()).isEqualTo("AWE Observability");
    assertThat(dashboard.path("description").asText()).contains("usage");
    assertThat(dashboard.path("panels")).isNotEmpty();
  }

  @Test
  @DisplayName("The starter ships a single dashboard: the usage dashboard was merged into it")
  void usageDashboardIsMerged() {
    assertThat(getClass().getClassLoader().getResource(REMOVED_USAGE_DASHBOARD)).isNull();
  }

  @Test
  @DisplayName("The dashboard filters by data source, application, id and option and shows operations and usage rows")
  void sections() throws IOException {
    JsonNode dashboard = dashboard(DASHBOARD);

    List<String> variables = new ArrayList<>();
    dashboard.path("templating").path("list").forEach(variable -> variables.add(variable.path("name").asText()));
    assertThat(variables).containsExactly("datasource", "application", "id", "option");
    List<String> rows = new ArrayList<>();
    dashboard.path("panels").forEach(panel -> {
      if ("row".equals(panel.path("type").asText())) {
        rows.add(panel.path("title").asText());
      }
    });
    assertThat(rows).containsExactly("Overview", "Usage", "Screens", "Users and sessions", "Exports and files", "Queries",
      "Maintains", "Actions");
  }

  @Test
  @DisplayName("The most executed query ids count every query execution, whatever launched it")
  void mostExecutedQueriesCountEveryExecution() throws IOException {
    String description = panels(Set.of("Most executed query ids")).get(0).path("description").asText();

    assertThat(description).contains("every query execution", "target action of the data actions", "initial loads",
      "maintains", "services", "cache");
  }

  @Test
  @DisplayName("The panels fill the grid without overlapping and each row starts below the panels of the previous one")
  void panelsDoNotOverlap() throws IOException {
    List<JsonNode> panels = new ArrayList<>();
    dashboard(DASHBOARD).path("panels").forEach(panels::add);
    int bottom = 0;

    for (JsonNode panel : panels) {
      JsonNode grid = panel.path("gridPos");
      String title = panel.path("title").asText();
      int x = grid.path("x").asInt(-1);
      int y = grid.path("y").asInt(-1);
      int w = grid.path("w").asInt(-1);
      int h = grid.path("h").asInt(-1);
      assertThat(x).as("x of " + title).isNotNegative();
      assertThat(y).as("y of " + title).isNotNegative();
      assertThat(w).as("w of " + title).isPositive();
      assertThat(h).as("h of " + title).isPositive();
      assertThat(x + w).as("Right edge of " + title).isLessThanOrEqualTo(GRID_WIDTH);
      if ("row".equals(panel.path("type").asText())) {
        assertThat(List.of(x, y, w, h)).as("Grid of row " + title).containsExactly(0, bottom, GRID_WIDTH, 1);
        assertThat(panel.path("panels")).as("Panels of row " + title).isEmpty();
      } else {
        assertThat(y).as("y of " + title + " below its row").isGreaterThan(rowStart(panels, panel));
      }
      bottom = Math.max(bottom, y + h);
    }
    for (int i = 0; i < panels.size(); i++) {
      for (int j = i + 1; j < panels.size(); j++) {
        assertThat(overlap(panels.get(i).path("gridPos"), panels.get(j).path("gridPos")))
          .as(panels.get(i).path("title").asText() + " overlaps " + panels.get(j).path("title").asText()).isFalse();
      }
    }
  }

  private static int rowStart(List<JsonNode> panels, JsonNode panel) {
    int start = -1;
    for (JsonNode candidate : panels) {
      if (candidate == panel) {
        break;
      }
      if ("row".equals(candidate.path("type").asText())) {
        start = candidate.path("gridPos").path("y").asInt();
      }
    }
    return start;
  }

  private static boolean overlap(JsonNode a, JsonNode b) {
    return a.path("x").asInt() < b.path("x").asInt() + b.path("w").asInt()
      && b.path("x").asInt() < a.path("x").asInt() + a.path("w").asInt()
      && a.path("y").asInt() < b.path("y").asInt() + b.path("h").asInt()
      && b.path("y").asInt() < a.path("y").asInt() + a.path("h").asInt();
  }

  /**
   * Per series increase of a counter over a window: the delta since the start of the window, the increase when the
   * counter was reset (application restart) and the whole counter for the series born inside the window
   */
  private static Pattern seriesIncrease(String window) {
    String selector = "(awe_[a-z_]+\\{[^}]*\\})";
    String quotedWindow = Pattern.quote(window);
    return Pattern.compile("\\(clamp_min\\(" + selector + " - \\1 offset " + quotedWindow + ", 0\\) unless resets\\(\\1\\["
      + quotedWindow + "\\]\\) > 0 or \\(increase\\(\\1\\[" + quotedWindow + "\\]\\) and \\1 offset " + quotedWindow
      + "\\) or \\1\\)");
  }

  private List<JsonNode> panels(Set<String> titles) throws IOException {
    List<JsonNode> panels = panels(dashboard(DASHBOARD)).stream()
      .filter(panel -> titles.contains(panel.path("title").asText()) && !"row".equals(panel.path("type").asText()))
      .toList();
    assertThat(panels).as("Panels " + titles).hasSize(titles.size());
    return panels;
  }

  @Test
  @DisplayName("In-range totals count the series born in the range and show whole numbers")
  void rangeCountsAreWholeAndCountNewSeries() throws IOException {
    for (JsonNode panel : panels(RANGE_COUNTS)) {
      String title = panel.path("title").asText();
      assertThat(panel.path("fieldConfig").path("defaults").path("decimals").asInt(-1)).as("Decimals of " + title).isZero();
      String expression = panel.path("targets").path(0).path("expr").asText();
      assertThat(seriesIncrease("$__range").matcher(expression).find()).as("Per series increase in " + expression).isTrue();
      assertThat(seriesIncrease("$__range").matcher(expression).replaceAll("")).as("Rest of " + expression)
        .doesNotContain("increase(", "rate(");
    }
  }

  @Test
  @DisplayName("The overview totals show 0 rather than no data when nothing happened")
  void overviewTotalsDefaultToZero() throws IOException {
    for (JsonNode panel : panels(ZERO_WHEN_EMPTY)) {
      assertThat(panel.path("targets").path(0).path("expr").asText()).as("Query of " + panel.path("title"))
        .endsWith(" or vector(0)");
    }
  }

  @Test
  @DisplayName("Bar charts count each event in one bar only, so their legend total is the real count")
  void barChartsDoNotOverlap() throws IOException {
    for (JsonNode panel : panels(BUCKET_COUNTS)) {
      String title = panel.path("title").asText();
      assertThat(panel.path("fieldConfig").path("defaults").path("custom").path("drawStyle").asText()).isEqualTo("bars");
      assertThat(panel.path("interval").asText()).as("Min interval of " + title).isEqualTo("1m");
      String expression = panel.path("targets").path(0).path("expr").asText();
      assertThat(expression).as("Query of " + title).doesNotContain("$__rate_interval");
      assertThat(seriesIncrease("$__interval").matcher(expression).find()).as("Per bucket increase in " + expression).isTrue();
      assertThat(seriesIncrease("$__interval").matcher(expression).replaceAll("")).as("Rest of " + expression)
        .doesNotContain("increase(", "rate(");
    }
  }

  @Test
  @DisplayName("Every panel has an id of its own and queries the data source variable")
  void panelsAreWellFormed() throws IOException {
    List<JsonNode> panels = panels(dashboard(DASHBOARD));
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
    String scrape = scrape();
    List<String> expressions = expressions(dashboard(DASHBOARD));
    Set<String> queriedMetrics = new LinkedHashSet<>();

    for (String expression : expressions) {
      Set<String> metrics = matches(AWE_METRIC, expression);
      queriedMetrics.addAll(metrics);
      List<String> samples = Arrays.stream(scrape.split("\n"))
        .filter(line -> metrics.stream().anyMatch(metric -> line.matches(metric + "[{ ].*")))
        .toList();
      for (String metric : metrics) {
        assertThat(samples).as("Metric " + metric + " in the Prometheus scrape").anyMatch(line -> line.matches(metric + "[{ ].*"));
      }
      if (metrics.isEmpty()) {
        continue;
      }
      Set<String> labels = new LinkedHashSet<>(matches(LABEL_VALUES, expression));
      matches(GROUPING, expression).forEach(group -> Arrays.stream(group.split(","))
        .map(String::trim).filter(label -> !label.isEmpty()).forEach(labels::add));
      for (String label : labels) {
        assertThat(samples).as("Label " + label + " of " + metrics + " in: " + expression)
          .anyMatch(line -> line.contains(label + "=\""));
      }
      Matcher literal = LITERAL_MATCHER.matcher(expression);
      while (literal.find()) {
        String matcher = literal.group(1) + "=\"" + literal.group(2) + "\"";
        assertThat(samples).as("Matcher " + matcher + " of " + metrics + " in: " + expression)
          .anyMatch(line -> line.contains(matcher));
      }
    }

    assertThat(queriedMetrics).isNotEmpty();
  }
}
