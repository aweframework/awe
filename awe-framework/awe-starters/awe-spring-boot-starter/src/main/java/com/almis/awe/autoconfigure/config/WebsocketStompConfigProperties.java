package com.almis.awe.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

/**
 * Websocket STOMP configuration properties
 */
@ConfigurationProperties(prefix = "awe.websocket.stomp")
@Validated
@Data
public class WebsocketStompConfigProperties {
  /**
   * Enable STOMP broker relay instead of simple broker
   * Default value false
   */
  private boolean enableStompBrokerRelay = false;

  /**
   * Relay host for STOMP broker
   * Default value localhost
   */
  private String relayHost = "localhost";

  /**
   * Relay port for STOMP broker
   * Default value 61613
   */
  private int relayPort = 61613;

  /**
   * Client login for STOMP broker
   */
  private String clientLogin;

  /**
   * Client password for STOMP broker
   */
  private String clientPasscode;

  /**
   * System login for STOMP broker
   */
  private String systemLogin;

  /**
   * System password for STOMP broker
   */
  private String systemPasscode;

  /**
   * Destination prefixes for message broker
   * Default value [/topic, /queue]
   */
  private List<String> destinationPrefixes = Arrays.asList("/topic", "/queue");
}