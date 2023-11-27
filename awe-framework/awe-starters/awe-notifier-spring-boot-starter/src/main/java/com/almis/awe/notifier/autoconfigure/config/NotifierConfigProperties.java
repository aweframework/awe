package com.almis.awe.notifier.autoconfigure.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "awe.notifier")
public class NotifierConfigProperties {
  /**
   * Notification email: From field name
   */
  private String fromName;
  /**
   * Notification email: From field email value
   */
  private String fromEmail;
}
