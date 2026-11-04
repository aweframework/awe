package com.almis.awe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the awe.security.screen-access property (issue #914)
 */
class SecurityConfigPropertiesScreenAccessTest {

  @Test
  @DisplayName("Screen access is enforced by default")
  void enforcedByDefault() {
    assertThat(new SecurityConfigProperties().getScreenAccess()).isEqualTo(ScreenAccessMode.ENFORCE);
  }

  @Test
  @DisplayName("Screen access binds off, warn and enforce")
  void bindsEveryMode() {
    for (ScreenAccessMode mode : ScreenAccessMode.values()) {
      Binder binder = new Binder(new MapConfigurationPropertySource(
        Map.of("awe.security.screen-access", mode.name().toLowerCase())));
      SecurityConfigProperties properties = binder.bind("awe.security", SecurityConfigProperties.class)
        .orElseGet(SecurityConfigProperties::new);
      assertThat(properties.getScreenAccess()).isEqualTo(mode);
    }
  }
}
