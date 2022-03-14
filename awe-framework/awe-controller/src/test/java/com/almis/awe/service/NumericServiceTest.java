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
import java.util.Locale;

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
    when(numericConfigProperties.getRoundType()).thenReturn(RoundingType.CEILING);
    assertEquals(6, numericService.getDecimalsNumberInNumericString("11.212.212,123112"));
    assertEquals(0, numericService.getDecimalsNumberInNumericString("11.212.212"));
  }

  @Test
  void getDecimalsNumberInRawNumericString() {
    assertEquals(5, numericService.getDecimalsNumberInRawNumericString("11212212.12112"));
    assertEquals(0, numericService.getDecimalsNumberInRawNumericString("1121221212112"));
  }
}