package com.almis.awe.service;

import com.almis.awe.config.NumericConfigProperties;
import com.almis.awe.model.type.NumericFormatType;
import com.almis.awe.model.type.RoundingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumericServiceTest {

  @InjectMocks
  private NumericService numericService;

  @Mock
  private NumericConfigProperties numericConfigProperties;

  @Test
  void applyPatternEUR() {
    when(numericConfigProperties.getFormat()).thenReturn(NumericFormatType.EUR);
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.CEILING);
    assertEquals("123.123.112,13", numericService.applyPattern("#,###.00", 123123112.123D));
  }

  @Test
  void applyPatternAME() {
    when(numericConfigProperties.getFormat()).thenReturn(NumericFormatType.AME);
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.FLOOR);
    assertEquals("123,123,112.12", numericService.applyPattern("#,###.00", 123123112.123D));
  }

  @Test
  void applyRawPattern() {
    when(numericConfigProperties.getFormat()).thenReturn(NumericFormatType.AME);
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.FLOOR);
    assertEquals("123123112.12", numericService.applyRawPattern("###.00", 123123112.123D));
  }

  @Test
  void applyPatternWithLocale() {
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.FLOOR);
    assertEquals("123.123.112,12", numericService.applyPatternWithLocale("#,###.00", 123123112.123D, "EUR"));
  }

  @Test
  void testApplyPatternWithLocale() {
    assertEquals("123,123,112.12", numericService.applyPatternWithLocale("#,###.00", 123123112.123D, NumberFormat.getInstance(Locale.US)));
  }

  @Test
  void formatNumber() {
    assertEquals("1,212", numericService.formatNumber(NumberFormat.getInstance(Locale.US), 1212D));
  }

  @Test
  void parseNumericString() throws Exception {
    when(numericConfigProperties.getFormat()).thenReturn(NumericFormatType.AME);
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.CEILING);
    assertEquals(1212L, numericService.parseNumericString("1,212"));
  }

  @Test
  void parseRawNumericString() throws Exception {
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.CEILING);
    assertEquals(11212212L, numericService.parseRawNumericString("11212212"));
  }

  @Test
  void getDecimalsNumberInNumericString() {
    when(numericConfigProperties.getFormat()).thenReturn(NumericFormatType.EUR);
    assertEquals(6, numericService.getDecimalsNumberInNumericString("11.212.212,123112"));
    assertEquals(0, numericService.getDecimalsNumberInNumericString("11.212.212"));
  }

  @Test
  void getDecimalsNumberInRawNumericString() {
    assertEquals(5, numericService.getDecimalsNumberInRawNumericString("11212212.12112"));
    assertEquals(0, numericService.getDecimalsNumberInRawNumericString("1121221212112"));
  }

  /**
   * Issue #950: a pattern applied by a previous call must not change how later values are parsed
   */
  @Test
  void applyingAPatternDoesNotChangeLaterParsing() throws Exception {
    NumericConfigProperties properties = new NumericConfigProperties();
    properties.setFormat(NumericFormatType.EUR);
    NumericService service = new NumericService(properties);
    service.applyPattern("###0.000", 1D);
    assertEquals(5016.75D, service.parseNumericString("5.016,75").doubleValue());
  }

  /**
   * Issue #950: parsing and formatting from several request threads at once must not share mutable formats
   */
  @Test
  void parseAndFormatConcurrently() throws Exception {
    NumericConfigProperties properties = new NumericConfigProperties();
    properties.setFormat(NumericFormatType.EUR);
    properties.setRoundType(RoundingType.HALF_UP_SYMMETRIC);
    NumericService service = new NumericService(properties);
    int threads = 8;
    int calls = 10000;
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int thread = 0; thread < threads; thread++) {
      boolean parser = thread % 2 == 0;
      tasks.add(() -> {
        int failures = 0;
        for (int call = 0; call < calls; call++) {
          try {
            boolean valid = parser
              ? service.parseNumericString("5.016,75").doubleValue() == 5016.75
              : "5016.750".equals(service.applyPatternWithLocale("###0.000", 5016.75D, "AME"))
              && "5.016,75".equals(service.applyPattern("###,##0.00", 5016.75D));
            failures += valid ? 0 : 1;
          } catch (Exception exc) {
            failures++;
          }
        }
        return failures;
      });
    }
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      int failures = 0;
      for (Future<Integer> result : executor.invokeAll(tasks)) {
        failures += result.get();
      }
      assertEquals(0, failures);
    } finally {
      executor.shutdownNow();
    }
  }
}
