package com.almis.awe.model.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebTooltip options
 *
 * @author pgarcia
 */
@Setter
@Getter
@Accessors(chain = true)
@ConfigurationProperties(prefix = "settings.numbers.options")
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
  private Float vMin;
  /**
   * Max value
   */
  @JsonProperty("vMax")
  private Float vMax;
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
