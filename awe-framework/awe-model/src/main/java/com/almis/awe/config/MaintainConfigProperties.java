package com.almis.awe.config;

import jakarta.validation.Valid;
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
  @Valid
  private Multiple multiple = new Multiple();

  /**
   * Maintain transaction configuration.
   */
  @NestedConfigurationProperty
  @Valid
  private Transaction transaction = new Transaction();

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
    @Valid
    private MaintainValidationMode validationMode = MaintainValidationMode.STRICT;
  }

  /**
   * Maintain transaction configuration properties
   */
  @Data
  public static class Transaction {
    /**
     * Run each maintain inside a transaction of its DataSource. Nested maintains and retrieve-data steps on the same
     * DataSource join it, and any failure or ERROR result rolls the whole maintain back. Disable it to keep the
     * previous behavior: each maintain commits its own connection independently.
     * Default value true
     */
    private boolean enabled = true;
  }
}
