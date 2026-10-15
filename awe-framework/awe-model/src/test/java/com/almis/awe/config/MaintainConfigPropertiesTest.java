package com.almis.awe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maintain configuration properties tests
 */
class MaintainConfigPropertiesTest {

  @Test
  @DisplayName("Maintain transaction boundaries are enabled by default")
  void transactionEnabledByDefault() {
    assertThat(new MaintainConfigProperties().getTransaction().isEnabled()).isTrue();
  }

  @Test
  @DisplayName("Maintain transaction boundaries are enabled when the property is not defined")
  void transactionEnabledWhenPropertyIsMissing() {
    assertThat(bind(Map.of()).getTransaction().isEnabled()).isTrue();
  }

  @Test
  @DisplayName("awe.maintain.transaction.enabled=false disables maintain transaction boundaries")
  void transactionCanBeDisabled() {
    assertThat(bind(Map.of("awe.maintain.transaction.enabled", "false")).getTransaction().isEnabled()).isFalse();
  }

  private MaintainConfigProperties bind(Map<String, String> properties) {
    return new Binder(new MapConfigurationPropertySource(properties))
      .bindOrCreate("awe.maintain", MaintainConfigProperties.class);
  }
}
