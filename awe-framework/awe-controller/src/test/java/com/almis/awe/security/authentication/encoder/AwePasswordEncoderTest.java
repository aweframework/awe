package com.almis.awe.security.authentication.encoder;

import com.almis.awe.config.PasswordEncoderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AwePasswordEncoderTest {

  private static final String PASSWORD = "test";
  // RIPEMD-160 of "test", as stored by AWE 4 (no prefix)
  private static final String LEGACY_HASH = "5e52fee47e6b070565f74372468cdc699de89107";

  @Test
  void bcryptEncoder_encodesNewPasswordsWithBcryptPrefix() {
    AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

    String hash = encoder.encode(PASSWORD);

    assertThat(hash).startsWith("{bcrypt}$2");
    assertThat(encoder.matches(PASSWORD, hash)).isTrue();
    assertThat(encoder.matches("other", hash)).isFalse();
    assertThat(encoder.upgradeEncoding(hash)).isFalse();
  }

  @Test
  void bcryptEncoder_keepsValidatingLegacyHashesAndAsksToUpgradeThem() {
    AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

    assertThat(encoder.matches(PASSWORD, LEGACY_HASH)).isTrue();
    assertThat(encoder.matches(PASSWORD, LEGACY_HASH.toUpperCase())).isTrue();
    assertThat(encoder.matches("other", LEGACY_HASH)).isFalse();
    assertThat(encoder.upgradeEncoding(LEGACY_HASH)).isTrue();
  }

  @Test
  void bcryptEncoder_ignoresTrailingPaddingOfCharColumns() {
    AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);
    String hash = encoder.encode(PASSWORD);

    assertThat(encoder.matches(PASSWORD, hash + "    ")).isTrue();
    assertThat(encoder.matches(PASSWORD, LEGACY_HASH + "  ")).isTrue();
    assertThat(encoder.upgradeEncoding(hash + "    ")).isFalse();
  }

  @Test
  void ripemd160Encoder_keepsTheLegacyFormatAndNeverUpgrades() {
    AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.RIPEMD160);
    String bcryptHash = new AwePasswordEncoder(PasswordEncoderType.BCRYPT).encode(PASSWORD);

    assertThat(encoder.encode(PASSWORD)).isEqualTo(LEGACY_HASH);
    assertThat(encoder.matches(PASSWORD, LEGACY_HASH)).isTrue();
    assertThat(encoder.matches(PASSWORD, bcryptHash)).isTrue();
    assertThat(encoder.upgradeEncoding(LEGACY_HASH)).isFalse();
    assertThat(encoder.upgradeEncoding(bcryptHash)).isFalse();
  }

  @Test
  void emptyStoredPassword_neverMatches() {
    AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

    assertThat(encoder.matches(PASSWORD, null)).isFalse();
    assertThat(encoder.matches(PASSWORD, "")).isFalse();
    assertThat(encoder.matches(PASSWORD, "   ")).isFalse();
    assertThat(encoder.upgradeEncoding(null)).isFalse();
  }
}
