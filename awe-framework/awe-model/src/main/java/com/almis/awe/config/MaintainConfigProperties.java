package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Maintain configuration properties
 */
@ConfigurationProperties(prefix = "awe.maintain")
@Validated
@Data
public class MaintainConfigProperties {

  /**
   * Multiple maintain configuration.
   */
  @NestedConfigurationProperty
  private Multiple multiple = new Multiple();

  /**
   * Multiple maintain configuration properties
   */
  @Data
  public static class Multiple {
    /**
     * Validation behavior when a multiple maintain has no declared non-audit list variable.
     * <ul>
     *   <li>{@code strict} (default): throw an error, protecting new development.</li>
     *   <li>{@code warn}: log a warning with the operation id and skip the operation, preserving
     *   pre-4.12.1 no-op semantics during migration.</li>
     * </ul>
     * Default value strict
     */
    private MaintainValidationMode validationMode = MaintainValidationMode.STRICT;
  }
}
