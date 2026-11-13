package com.almis.awe.test;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chart actions of the dynamic series test screen (ChrTstDynamicSeries)
 */
@Tag("integration")
class ChartSeriesServiceTest {

  private final Chart chart = new Chart();

  @Test
  void addsOneSeriesPerUserWithAPointPerMonth() {
    ServiceData serviceData = chart.addSeriesChart(List.of("test", "donald"));

    assertSeries(serviceData);
  }

  @Test
  void replacesTheSeriesWithOneSeriesPerUserWithAPointPerMonth() {
    ServiceData serviceData = chart.replaceSeriesChart(List.of("test", "donald"));

    assertSeries(serviceData);
  }

  @SuppressWarnings("unchecked")
  private static void assertSeries(ServiceData serviceData) {
    assertThat(serviceData.getClientActionList()).hasSize(1);
    List<ChartSerie> series = (List<ChartSerie>) serviceData.getClientActionList().get(0).getParameters().get("series");
    assertThat(series).extracting(ChartSerie::getId).containsExactly("test", "donald");
    assertThat(series).allSatisfy(serie -> assertThat(serie.getData()).hasSize(12));
  }
}
