package com.almis.awe.developer.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Scheduler module properties
 */
@Data
@ConfigurationProperties(prefix = "awe.developer.translation.rapidapi")
public class RapidAPIConfigProperties {
  /**
   * Rapid API Key
   */
  private String key;
  /**
   * Translation service host
   * Default value translated-mymemory---translation-memory.p.rapidapi.com
   */
  private String host = "translated-mymemory---translation-memory.p.rapidapi.com";
  /**
   * Translation service reference email
   * Default value awe@almis.com
   */
  private String email = "awe@almis.com";
}