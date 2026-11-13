package com.almis.awe.model.type;

/**
 * Email server connection security modes, with the SMTP port used when the server has none
 */
public enum EmailSecurityMode {
  /**
   * Plain connection
   */
  NONE(25),
  /**
   * Plain connection upgraded to TLS with STARTTLS
   */
  STARTTLS(587),
  /**
   * Implicit TLS connection
   */
  SSL(465);

  private final int defaultPort;

  EmailSecurityMode(int defaultPort) {
    this.defaultPort = defaultPort;
  }

  /**
   * Retrieve the SMTP port used when the server has no port
   *
   * @return Default port
   */
  public int getDefaultPort() {
    return defaultPort;
  }
}
