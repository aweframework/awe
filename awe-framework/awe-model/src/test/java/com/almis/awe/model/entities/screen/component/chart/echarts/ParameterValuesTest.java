package com.almis.awe.model.entities.screen.component.chart.echarts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterValuesTest {

  @Test
  void integralNumbersKeepTheirMagnitude() {
    assertThat(ParameterValues.number("12")).isEqualTo(12);
    assertThat(ParameterValues.number("12.5")).isEqualTo(12.5);
    assertThat(ParameterValues.number("1704067200000")).isEqualTo(1704067200000L);
    assertThat(ParameterValues.number("-3000000000")).isEqualTo(-3000000000L);
    assertThat(ParameterValues.number("1e20")).isEqualTo(1e20);
  }

  @Test
  void nonFiniteAndTextValuesAreNotNumbers() {
    assertThat(ParameterValues.number("Infinity")).isNull();
    assertThat(ParameterValues.number("abc")).isNull();
  }
}
