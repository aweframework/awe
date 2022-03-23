package com.almis.awe.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email engine configuration properties
 */
@ConfigurationProperties(prefix = "awe.mail")
@Data
public class EmailConfigProperties {
  /**
   * Flag to enable awe email features. The Default value is false.
   */
  private boolean enabled = false;
  /**
   * Email authentication. Flag to specify if authentication is needed.
   * The Default value is false.
   */
  private boolean auth = false;
  /**
   * Email server host
   */
  private String host;
  /**
   * Email server port. The Default value is 25.
   */
  private int port = 25;
  /**
   * Email server auth username
   */
  private String user;
  /**
   * Email server auth password
   */
  private String pass;
  /**
   * Execute email in debug mode. The Default value is false.
   */
  private boolean debug = false;
  /**
   * Flag to specify the usage of SSL protocol. The Default value is false.
   */
  private boolean ssl = false;
  /**
   * Flag to specify the usage of TLS protocol. The Default value is false.
   */
  private boolean tls = false;
  /**
   * Name of local smtp server. The Default value is localhost.
   */
  private String localhost = "localhost";
}
