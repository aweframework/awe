package com.almis.awe.model.type;

import java.util.Arrays;

/**
 * Email server authentication types, stored as codes in AweEmlSrv.Ath
 */
public enum EmailAuthenticationType {
  /**
   * No authentication (0)
   */
  NONE(0),
  /**
   * User and password (1)
   */
  BASIC(1),
  /**
   * OAuth2 access token (2)
   */
  OAUTH2(2);

  private final int code;

  EmailAuthenticationType(int code) {
    this.code = code;
  }

  /**
   * Retrieve the stored code
   *
   * @return Code
   */
  public int getCode() {
    return code;
  }

  /**
   * Retrieve the authentication type of a stored code
   *
   * @param code Stored code
   * @return Authentication type
   * @throws IllegalArgumentException if the code is unknown
   */
  public static EmailAuthenticationType fromCode(int code) {
    return Arrays.stream(values())
      .filter(type -> type.code == code)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown email server authentication code: " + code));
  }
}
