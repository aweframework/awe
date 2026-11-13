package com.almis.awe.security.authentication.encoder;

import com.almis.awe.config.PasswordEncoderType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * AWE password encoder. New passwords are hashed with the configured algorithm, and stored hashes of every algorithm
 * keep validating: hashes with an {@code {id}} prefix use that encoder and hashes without prefix are the legacy
 * RIPEMD-160 of AWE 4. With BCrypt, legacy hashes ask to be upgraded, so they are rehashed on the next successful
 * login. Stored hashes may come from {@code char} columns, so their trailing padding is ignored.
 */
public class AwePasswordEncoder implements PasswordEncoder {

  /** Identifier (prefix) of BCrypt hashes */
  public static final String BCRYPT_ID = "bcrypt";

  private final PasswordEncoderType type;
  private final PasswordEncoder legacyEncoder = new Ripemd160PasswordEncoder();
  private final DelegatingPasswordEncoder delegatingEncoder;

  /**
   * Constructor
   *
   * @param type Algorithm used to hash new passwords
   */
  public AwePasswordEncoder(PasswordEncoderType type) {
    this.type = type;
    this.delegatingEncoder = new DelegatingPasswordEncoder(BCRYPT_ID, Map.of(BCRYPT_ID, new BCryptPasswordEncoder()));
    this.delegatingEncoder.setDefaultPasswordEncoderForMatches(legacyEncoder);
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return type == PasswordEncoderType.RIPEMD160 ? legacyEncoder.encode(rawPassword) : delegatingEncoder.encode(rawPassword);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    String storedPassword = StringUtils.stripEnd(encodedPassword, null);
    if (rawPassword == null || StringUtils.isEmpty(storedPassword)) {
      return false;
    }
    return delegatingEncoder.matches(rawPassword, storedPassword);
  }

  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    String storedPassword = StringUtils.stripEnd(encodedPassword, null);
    if (type == PasswordEncoderType.RIPEMD160 || StringUtils.isEmpty(storedPassword)) {
      return false;
    }
    return delegatingEncoder.upgradeEncoding(storedPassword);
  }
}
