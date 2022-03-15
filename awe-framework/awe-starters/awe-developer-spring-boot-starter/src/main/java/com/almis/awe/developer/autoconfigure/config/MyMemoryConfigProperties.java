package com.almis.awe.developer.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Scheduler module properties
 */
@Data
@ConfigurationProperties(prefix = "awe.developer.translation.mymemory")
public class MyMemoryConfigProperties {
  /**
   * Flag to load tasks on start application
   * Default value 10ecdc1ed89ac3ebed3b
   */
  private String key = "10ecdc1ed89ac3ebed3b";
  /**
   * Translation service host
   * Default value api.mymemory.translated.net
   */
  private String host = "api.mymemory.translated.net";
  /**
   * Translation service reference email
   * Default value awe@almis.com
   */
  private String email = "awe@almis.com";
}