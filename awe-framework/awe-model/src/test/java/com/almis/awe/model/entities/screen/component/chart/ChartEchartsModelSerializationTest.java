package com.almis.awe.model.entities.screen.component.chart;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The chart controller is serialized with both models, and a failure of the ECharts model never affects the other
 */
class ChartEchartsModelSerializationTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(Chart.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  /**
   * Chart whose ECharts translation fails, while the Highcharts one works
   */
  private static class EChartsFailingChart extends Chart {
    private static final long serialVersionUID = 1L;

    @Override
    public List<ChartAxis> getYAxisList() {
      boolean fromECharts = Arrays.stream(new Throwable().getStackTrace())
        .anyMatch(frame -> frame.getClassName().contains(".chart.echarts."));
      if (fromECharts) {
        throw new IllegalStateException("simulated ECharts failure");
      }
      return super.getYAxisList();
    }
  }

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  private static Chart.ChartBuilder<?, ?> chartBuilder() {
    return Chart.builder().id("chartId").type("line")
      .yAxisList(List.of(ChartAxis.builder().label("Y").build()))
      .serieList(List.of(ChartSerie.builder().id("serie").xValue("x").yValue("y").build()));
  }

  @Test
  void bothModelsAreTopLevelKeysOfTheSerializedChart() {
    JsonNode json = DataListUtil.getMapper().valueToTree(chartBuilder().build());

    assertThat(json.has("chartModel")).isTrue();
    assertThat(json.has("echartsModel")).isTrue();
    assertThat(json.at("/chartModel/series/0/id").asText()).isEqualTo("serie");
    assertThat(json.at("/echartsModel/series/0/id").asText()).isEqualTo("serie");
  }

  @Test
  void aFailingEChartsModelDoesNotBreakTheChartSerialization() {
    EChartsFailingChart chart = new EChartsFailingChart();
    chart.setId("chartId");
    chart.setType("line");
    chart.setYAxisList(List.of(ChartAxis.builder().label("Y").build()));
    chart.setSerieList(List.of(ChartSerie.builder().id("serie").xValue("x").yValue("y").build()));

    JsonNode json = DataListUtil.getMapper().valueToTree(chart);

    assertThat(json.has("chartModel")).isTrue();
    assertThat(json.at("/chartModel/series/0/id").asText()).isEqualTo("serie");
    assertThat(json.has("echartsModel")).isFalse();
    assertThat(json.at("/id").asText()).isEqualTo("chartId");
  }

  @Test
  void aFailingEChartsModelIsLoggedAtWarnLevelWithTheChartId() {
    EChartsFailingChart chart = new EChartsFailingChart();
    chart.setId("failingChart");
    chart.setType("line");

    assertThat(chart.getEchartsModel()).isEmpty();

    assertThat(appender.list).hasSize(1);
    ILoggingEvent event = appender.list.get(0);
    assertThat(event.getLevel()).isEqualTo(Level.WARN);
    assertThat(event.getFormattedMessage()).contains("failingChart").contains("simulated ECharts failure");
  }
}
