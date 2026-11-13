package com.almis.awe.model.entities.screen.component.chart.echarts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartParameter;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedOptionReporterTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(UnsupportedOptionReporter.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  @Test
  void rememberedOptionsAreBounded() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);

    for (int index = 0; index < UnsupportedOptionReporter.MAX_REMEMBERED + 20; index++) {
      reporter.parameter(ParameterScope.CHART, "custom.option" + index);
    }

    // One message per option plus the notice that the limit was reached
    assertThat(messages).hasSize(UnsupportedOptionReporter.MAX_REMEMBERED + 1);
    assertThat(messages.get(messages.size() - 1)).contains("further ones are not reported");
  }

  @Test
  void theServerLogsEachUnsupportedParameterOnceAtWarnLevel() {
    // The shared reporter remembers options for the whole JVM, so each run uses an option name of its own
    String option = "reporterTestOption" + UUID.randomUUID();
    ChartParameter unknown = ChartParameter.builder().type("integer").name(option).value("1").build();
    Chart chart = Chart.builder().id("chart").type("line").parameterList(List.of(unknown))
      .serieList(List.of(ChartSerie.builder().id("serie").build())).build();

    chart.getEchartsModel();
    chart.getEchartsModel();

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("Highcharts chart-parameter '" + option + "' "
      + "has no ECharts translation yet (scope: chart); it is ignored by the ECharts model");
  }
}
