package com.almis.awe.model.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Web numeric component options
 *
 * @author pgarcia
 */
@Data
@Accessors(chain = true)
public class WebNumberOptions {

  /**
   * Thousands separator
   */
  @JsonProperty("aSep")
  private String aSep;
  /**
   * Digits group
   */
  @JsonProperty("dGroup")
  private Integer dGroup;
  /**
   * Decimals separator
   */
  @JsonProperty("aDec")
  private String aDec;
  /**
   * Post sign
   */
  @JsonProperty("aSign")
  private String aSign;
  /**
   * Pre sign
   */
  @JsonProperty("pSign")
  private String pSign;
  /**
   * Min value
   */
  @JsonProperty("vMin")
  private double vMin;
  /**
   * Max value
   */
  @JsonProperty("vMax")
  private double vMax;
  /**
   * Decimal numbers
   */
  @JsonProperty("mDec")
  private Integer mDec;
  /**
   * Round method
   */
  @JsonProperty("mRound")
  private String mRound;
  /**
   * Padding
   */
  @JsonProperty("aPad")
  private Boolean aPad;
  /**
   * Empty values
   */
  @JsonProperty("wEmpty")
  private String wEmpty;
}
