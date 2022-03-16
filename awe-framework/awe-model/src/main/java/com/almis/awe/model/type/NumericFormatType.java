package com.almis.awe.model.type;

/**
 * Numeric Formats type Enum
 */
public enum NumericFormatType {

  /**
   * European numeric format ('.' as thousands character and ',' as decimal character).
   * Ex.: [10.000.000,00]
   */
  EUR(".", ","),
  /**
   * American numeric format (',' as thousands character and '.' as decimal character).
   * Ex.: [10,000,000.00]
   */
  AME(",", "."),
  /**
   * European numeric format without thousands character ('.' as decimal character).
   * Ex.: [10000000.00]
   */
  EUR_NO("", ","),
  /**
   * American numeric format without thousands character (',' as decimal character).
   * Ex.: [10000000,00]
   */
  AME_NO("", ".");

  // Numeric format fields
  private final String thousandCharacter;
  private final String decimalCharacter;

  /**
   * NumericFormatType constructor
   * @param thousandCharacter Thousands character
   * @param decimalCharacter Decimal character
   */
  NumericFormatType(String thousandCharacter, String decimalCharacter) {
    this.thousandCharacter = thousandCharacter;
    this.decimalCharacter = decimalCharacter;
  }

  /**
   * Get thousands character
   * @return thousands character
   */
  public String getThousandCharacter() {
    return thousandCharacter;
  }

  /**
   * Get decimals character
   * @return decimals character
   */
  public String getDecimalCharacter() {
    return decimalCharacter;
  }
}
