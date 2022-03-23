package com.almis.awe.security.authentication.encoder;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.EncodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author  dfuentes on 19/06/2017.
 */
@Slf4j
public class Sha512PasswordEncoder implements PasswordEncoder {

  // Autowired services
  private final EncodeService encodeService;

  /**
   * Autowired constructor
   *
   * @param encodeService Encode service
   */
  public Sha512PasswordEncoder(EncodeService encodeService) {
    this.encodeService = encodeService;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    try {
      return encodeService.hash(EncodeService.HashingAlgorithms.SHA_512, rawPassword.toString());
    } catch (AWException e) {
      log.error("Error authenticating, could not hash given password");
      return rawPassword.toString();
    }
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    try {
      return encodeService.hash(EncodeService.HashingAlgorithms.SHA_256, rawPassword.toString()).equalsIgnoreCase(encodedPassword);
    } catch (AWException e) {
      log.error("Error authenticating, could not hash given password");
    }
    return false;
  }
}
