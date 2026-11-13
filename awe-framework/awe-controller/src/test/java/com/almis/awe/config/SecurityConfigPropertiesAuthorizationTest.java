package com.almis.awe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the awe.security.authorization.mode property (issue #914)
 */
class SecurityConfigPropertiesAuthorizationTest {

  @Test
  @DisplayName("Authorization is enforced by default")
  void enforcedByDefault() {
    assertThat(new SecurityConfigProperties().getAuthorization().getMode()).isEqualTo(AuthorizationMode.ENFORCE);
  }

  @Test
  @DisplayName("awe.security.authorization.mode binds off, warn and enforce")
  void bindsEveryMode() {
    for (AuthorizationMode mode : AuthorizationMode.values()) {
      Binder binder = new Binder(new MapConfigurationPropertySource(
        Map.of("awe.security.authorization.mode", mode.name().toLowerCase())));
      SecurityConfigProperties properties = binder.bind("awe.security", SecurityConfigProperties.class)
        .orElseGet(SecurityConfigProperties::new);
      assertThat(properties.getAuthorization().getMode()).isEqualTo(mode);
    }
  }
}
