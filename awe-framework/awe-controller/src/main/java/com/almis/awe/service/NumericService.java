package com.almis.awe.service;

import com.almis.awe.config.NumericConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.type.NumericFormatType;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Numeric service Class
 * Numeric Utilities for AWE
 *
 * @author Pablo GARCIA - 16/APR/2012
 */
@Slf4j
public class NumericService extends ServiceConfig {

  // Static variables
  private static final NumberFormat AMERICAN_NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
  private static final NumberFormat EUROPEAN_NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);

  // Autowired services
  private final NumericConfigProperties numericConfigProperties;

  private final NumberFormat americanNumberFormat;

  /**
   * Hide the constructor
   * @param numericConfigProperties Numeric configuration properties
   */
  public NumericService(NumericConfigProperties numericConfigProperties) {
    this.numericConfigProperties = numericConfigProperties;
    americanNumberFormat = AMERICAN_NUMBER_FORMAT;
    americanNumberFormat.setRoundingMode(numericConfigProperties.getRoundType().getRoundingMode());
  }

  /**
   * Fixes an string value for a criteria
   *
   * @param pattern pattern to apply
   * @param value   string number value
   * @return number formatted
   */
  public String applyPattern(String pattern, Double value) {
    // Get pattern
    String patternToSet = pattern == null ? numericConfigProperties.getPatternFormatted() : pattern;
    return applyPatternWithLocale(patternToSet, value, getNumberFormat(numericConfigProperties.getFormat()));
  }

  /**
   * Fixes a string value for a criteria in a raw pattern Use Locale US
   *
   * @param pattern Number pattern
   * @param value   String value
   * @return Value fixed
   */
  public String applyRawPattern(String pattern, Double value) {
    String patternToSet = pattern == null ? numericConfigProperties.getPatternUnformatted() : pattern;
    return applyPatternWithLocale(patternToSet, value, getNumberFormat(numericConfigProperties.getFormat()));
  }

  /**
   * Fixes a string value for a criteria in a raw pattern with locale
   *
   * @param pattern patter to apply
   * @param value   number value
   * @param format  LOCALE to apply
   * @return String with pattern applied
   */
  public String applyPatternWithLocale(String pattern, Double value, String format) {
    return applyPatternWithLocale(pattern, value, getNumberFormat(NumericFormatType.valueOf(format)));
  }

  /**
   * Fixes a string value for a criteria in a raw pattern with locale
   *
   * @param pattern      patter to apply
   * @param value        number value
   * @param numberFormat LOCALE to apply
   * @return String with pattern applied
   */
  public String applyPatternWithLocale(String pattern, Double value, NumberFormat numberFormat) {
    ((DecimalFormat) numberFormat).applyPattern(pattern);
    return formatNumber(numberFormat, value);
  }

  /**
   * Fixes a string value for a criteria in a raw pattern with locale
   *
   * @param format Number format
   * @param value  number value
   * @return Number formatted
   */
  public String formatNumber(NumberFormat format, Double value) {
    try {
      return format.format(value);
    } catch (Exception exc) {
      log.error("Error formatting number {} with formatter {}", value, format, exc);
    }
    return String.valueOf(value);
  }

  /**
   * Parses a string value to a number. It reads the numeric type from
   * numeric.properties
   *
   * @param val String value with format as defined in numeric.properties
   * @return parsedValue parsed number
   * @throws ParseException Error parsing number
   */
  public Number parseNumericString(String val) throws ParseException {
    return getNumberFormat(numericConfigProperties.getFormat()).parse(val);
  }

  /**
   * Parses a string value to a number. It parses a string that represents a raw formatted numeric
   *
   * @param val String value with format as defined in numeric.properties
   * @return parsedValue parsed number
   * @throws ParseException Error parsing number
   */
  public Number parseRawNumericString(String val) throws ParseException {
    return americanNumberFormat.parse(val);
  }

  /**
   * Returns decimals number a string that represents a formatted numeric
   *
   * @param val String value with format as defined in numeric.properties
   * @return decimals number
   * @throws ParseException Error parsing number
   */
  public int getDecimalsNumberInNumericString(String val) throws ParseException {
    NumberFormat numberFormat = getNumberFormat(numericConfigProperties.getFormat());
    numberFormat.parse(val);

    int separatorIndex = val.indexOf(((DecimalFormat) numberFormat).getDecimalFormatSymbols().getDecimalSeparator());
    return (separatorIndex > -1) ? val.length() - separatorIndex - 1 : 0;
  }

  /**
   * Returns decimals number a string that represents a raw formatted numeric
   *
   * @param val String value with format as defined in numeric.properties
   * @return decimals number
   * @throws ParseException Error parsing number
   */
  public int getDecimalsNumberInRawNumericString(String val) throws ParseException {
    americanNumberFormat.parse(val);
    String decimalToSplit = String.valueOf(((DecimalFormat) americanNumberFormat).getDecimalFormatSymbols().getDecimalSeparator());
    String[] decimalSplit = val.split(Pattern.quote(decimalToSplit));
    return (decimalSplit.length > 1) ? decimalSplit[1].length() : 0;
  }

  /**
   * Gets the numeric format defined in numeric.properties
   *
   * @return numeric format
   */
  private NumberFormat getNumberFormat(NumericFormatType type) {
    NumberFormat numberFormat;

    switch (type) {
      case AME:
      case AME_NO:
        // American formatting
        numberFormat = AMERICAN_NUMBER_FORMAT;
        break;
      default:
        // European formatting
        numberFormat = EUROPEAN_NUMBER_FORMAT;
        break;
    }
    // Apply rounding based
    numberFormat.setRoundingMode(numericConfigProperties.getRoundType().getRoundingMode());

    return numberFormat;
  }
}
