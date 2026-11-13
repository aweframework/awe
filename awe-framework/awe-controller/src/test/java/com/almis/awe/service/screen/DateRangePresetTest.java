package com.almis.awe.service.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Presets of the date range criterion as a default value (#966): same codes and dates as the clients
 */
@DisplayName("Date range presets")
class DateRangePresetTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 31);

  @ParameterizedTest(name = "{0} -> {1} - {2}")
  @CsvSource(value = {
    "YTD, 01/01/2026, 31/03/2026",
    "today, 31/03/2026, 31/03/2026",
    "7D, 24/03/2026, 31/03/2026",
    "2W, 17/03/2026, 31/03/2026",
    "1M, 28/02/2026, 31/03/2026",
    "1Y, 31/03/2025, 31/03/2026"
  })
  @DisplayName("A preset code resolves to its start and end")
  void presetResolvesToItsRange(String code, String from, String to) {
    assertThat(DateRangePreset.resolve(code, TODAY)).contains(List.of(from, to));
  }

  @Test
  @DisplayName("ALL resolves to an empty range")
  void allIsAnEmptyRange() {
    assertThat(DateRangePreset.resolve("ALL", TODAY)).contains(List.of());
  }

  @ParameterizedTest
  @CsvSource({"01/01/2026", "1X", "YTD2"})
  @DisplayName("A value that is not a preset code is not resolved")
  void otherValuesAreNotPresets(String value) {
    assertThat(DateRangePreset.resolve(value, TODAY)).isEmpty();
  }
}
