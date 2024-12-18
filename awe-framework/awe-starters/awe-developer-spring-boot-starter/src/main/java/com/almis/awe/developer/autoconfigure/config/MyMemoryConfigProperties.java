package com.almis.awe.developer.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyMemory config properties
 */
@Data
@ConfigurationProperties(prefix = "awe.developer.translation.mymemory")
public class MyMemoryConfigProperties {
  /**
   * MyMemory api key
   */
  private String key;
  /**
   * MyMemory translation service host
   * Default value api.mymemory.translated.net
   */
  private String host = "api.mymemory.translated.net";
  /**
   * Translation service reference email
   * Default value awe@almis.com
   */
  private String email = "awe@almis.com";
}