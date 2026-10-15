package com.almis.awe.model.util.data.date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateFormatDetectorTest {

  @Test
  void detectsEachFormat() {
    assertThat(DateFormatDetector.detect("1978-10-23@20:10:12.345+0100")).contains(DetectedDateFormat.JSON_DATE);
    assertThat(DateFormatDetector.detect("1978-10-23 20:10:12.345")).contains(DetectedDateFormat.SQL_TIMESTAMP);
    assertThat(DateFormatDetector.detect("23/10/1978")).contains(DetectedDateFormat.WEB_DATE);
    assertThat(DateFormatDetector.detect("1978-10-23")).contains(DetectedDateFormat.WBS_DATE);
    assertThat(DateFormatDetector.detect("23/10/1978 20:10:12")).contains(DetectedDateFormat.WEB_TIMESTAMP);
    assertThat(DateFormatDetector.detect("23/10/1978 20:10:12.345")).contains(DetectedDateFormat.WEB_TIMESTAMP);
  }

  @Test
  void detectsNothingForUnknownOrNullText() {
    assertThat(DateFormatDetector.detect("23.10.1978")).isEmpty();
    assertThat(DateFormatDetector.detect(null)).isEmpty();
  }

  @Test
  void validatorsAreStrict() {
    assertThat(DateFormatDetector.isWebDate("23/10/1978 20:10:12")).isFalse();
    assertThat(DateFormatDetector.isSqlTimestamp("1978-10-23 20:10:12")).isFalse();
    assertThat(DateFormatDetector.isWebTimestampWithMs("23/10/1978 20:10:12")).isFalse();
    assertThat(DateFormatDetector.isJsonDate("1978-10-23")).isFalse();
    assertThat(DateFormatDetector.isWbsDate("1978-10-23")).isTrue();
  }
}
