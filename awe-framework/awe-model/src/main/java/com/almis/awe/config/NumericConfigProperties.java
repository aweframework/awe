package com.almis.awe.config;

import com.almis.awe.model.type.NumericFormatType;
import com.almis.awe.model.type.RoundingType;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Numeric formats configuration properties
 */
@ConfigurationProperties(prefix = "awe.numeric")
@Validated
@Data
public class NumericConfigProperties {

  /**
   * Number format separator for input numeric.
   * Default value NumericFormatType.EUR (eur)
   */
  private NumericFormatType format = NumericFormatType.EUR;

  /**
   * Rounding type for numeric operations.
   * Default value Half Up Symmetric (S)
   */
  private RoundingType roundType = RoundingType.HALF_UP_SYMMETRIC;

  /**
   * Numeric format for field pattern value.
   * Default value ###,###.####
   */
  private String patternFormatted = "###,###.####";

  /**
   * Numeric format for field pattern value (plain, without a thousand separators).
   * Default value ######.####
   */
  private String patternUnformatted = "######.####";

  /**
   * Minimum value for numeric component.
   * Default value -9999999999.99
   */
  private double minValue = -9999999999.99;

  /**
   * Maximum value for numeric component.
   * Default value 9999999999.99
   */
  private double maxValue = 9999999999.99;

  /**
   * Padding with zeros to complete format number in numeric component.
   * Default value false
   */

  private boolean paddingWithZeros = false;
  /**
   * Number of decimals for numeric component.
   * Default value 5.
   */
  private int decimalNumbers = 5;

  /**
   * Thousands separator for numeric component.
   * Default value Thousands Char of NumericFormatType (awe.numeric.format)
   */
  private String separatorThousand = format.getThousandCharacter();

  /**
   * Decimal separator for numeric component.
   * Default value Thousands Char of NumericFormatType (awe.numeric.format)
   */
  private String separatorDecimal = format.getDecimalCharacter();

  /**
   * Controls the digital grouping - the placement of the thousand separator for numeric component.
   * Default value 3
   */
  private int groupDecimal = 3;

  /**
   * Desired currency symbol for numeric component.
   * Default value empty string
   */
  private String currencySign = Strings.EMPTY;

  /**
   * Controls the placement of the currency symbol for numeric component.
   * Default value s (suffix to the right). Use p for prefix to the left
   */
  private String currencyPlace = "s";

  /**
   * Controls controls input display behavior for numeric component.
   * Default value empty
   */
  private String emptyValue = "empty";
}
