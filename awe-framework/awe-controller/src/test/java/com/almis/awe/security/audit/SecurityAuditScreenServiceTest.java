package com.almis.awe.security.audit;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules of the services of the security audit screen: the bounds of each preset of the time range picker, the default
 * range and the label of a custom range
 */
class SecurityAuditScreenServiceTest {

  // 9 November 2026, 13:45:30 UTC
  private static final Instant NOW = Instant.parse("2026-11-09T13:45:30Z");
  private final SecurityAuditScreenService service = new SecurityAuditScreenService(Clock.fixed(NOW, ZoneOffset.UTC));

  private static Object value(DataList list, String column) {
    return DataListUtil.getColumn(list, column).getFirst().getObjectValue();
  }

  @ParameterizedTest(name = "{0} goes back {1} seconds")
  @CsvSource({"15m, 900", "1h, 3600", "6h, 21600", "24h, 86400", "7d, 604800", "30d, 2592000",
    "' 1H ', 3600", "unknown, 604800", "'', 604800"})
  @DisplayName("A preset ends now and starts the preset time before, and an unknown or empty preset is the last 7 days")
  void presetBounds(String preset, long seconds) {
    DataList range = service.getRange(preset).getDataList();

    assertThat(value(range, "CrtTo")).isEqualTo(Date.from(NOW));
    assertThat(value(range, "CrtFro")).isEqualTo(Date.from(NOW.minusSeconds(seconds)));
  }

  @Test
  @DisplayName("A preset carries its exact bounds with the time and its label, so a range of hours filters by the hour")
  void presetExactBoundsAndLabel() {
    DataList range = service.getRange("1h").getDataList();

    assertThat(value(range, "CrtFroTs")).isEqualTo("09/11/2026 12:45:30");
    assertThat(value(range, "CrtToTs")).isEqualTo("09/11/2026 13:45:30");
    assertThat(value(range, "CrtRngTxt")).isEqualTo("09/11/2026 12:45 - 09/11/2026 13:45");
  }

  @Test
  @DisplayName("A null preset is the default range")
  void nullPreset() {
    DataList range = service.getRange(null).getDataList();

    assertThat(value(range, "CrtFro")).isEqualTo(Date.from(NOW.minusSeconds(7L * 24 * 60 * 60)));
  }

  @Test
  @DisplayName("The label of a custom range shows both dates with their time, and an empty bound as nothing")
  void customRangeLabel() {
    Date from = Date.from(Instant.parse("2026-11-01T08:05:00Z"));
    Date to = Date.from(Instant.parse("2026-11-03T17:30:00Z"));

    assertThat(value(service.formatRange(from, to).getDataList(), "CrtRngTxt"))
      .isEqualTo("01/11/2026 08:05 - 03/11/2026 17:30");
    assertThat(value(service.formatRange(null, to).getDataList(), "CrtRngTxt")).isEqualTo(" - 03/11/2026 17:30");
  }

  @Test
  @DisplayName("The default start date is the start of the day the default days before today")
  void defaultDateFrom() {
    Object from = value(service.getDefaultDateFrom().getDataList(), "value");

    LocalDate expected = LocalDate.of(2026, 11, 2);
    assertThat(from).isEqualTo(Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }
}
