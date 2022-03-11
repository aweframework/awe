package com.almis.awe.security.authentication.encoder;

import com.almis.awe.service.EncodeService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Created by dfuentes on 19/06/2017.
 */
public class Ripemd160PasswordEncoder implements PasswordEncoder {
  @Override
  public String encode(CharSequence rawPassword) {
    return EncodeService.encodeRipEmd160(rawPassword.toString());
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return EncodeService.encodeRipEmd160(rawPassword.toString()).equalsIgnoreCase(encodedPassword);
  }
}
