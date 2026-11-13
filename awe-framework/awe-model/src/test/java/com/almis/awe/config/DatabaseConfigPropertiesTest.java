package com.almis.awe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database configuration properties tests
 */
class DatabaseConfigPropertiesTest {

  @Test
  @DisplayName("The password column is masked in SQL logs by default")
  void logMaskedFieldsDefaultsToPasswordColumn() {
    assertThat(new DatabaseConfigProperties().getLogMaskedFields()).containsExactly("l1_pas");
    assertThat(bind(Map.of()).getLogMaskedFields()).containsExactly("l1_pas");
  }

  @Test
  @DisplayName("awe.database.log-masked-fields replaces the masked columns")
  void logMaskedFieldsCanBeOverridden() {
    assertThat(bind(Map.of("awe.database.log-masked-fields", "l1_pas,ApiKey")).getLogMaskedFields())
      .containsExactly("l1_pas", "ApiKey");
  }

  private DatabaseConfigProperties bind(Map<String, String> properties) {
    return new Binder(new MapConfigurationPropertySource(properties))
      .bindOrCreate("awe.database", DatabaseConfigProperties.class);
  }
}
