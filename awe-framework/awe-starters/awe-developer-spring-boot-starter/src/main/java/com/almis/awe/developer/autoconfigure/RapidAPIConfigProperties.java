package com.almis.awe.developer.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Scheduler module properties
 */
@Data
@ConfigurationProperties(prefix = "translation.rapidapi")
public class RapidAPIConfigProperties {
  /**
   * Flag to load tasks on start application
   * Default value f20790a0d6msh98d1dad8f0e3da9p10eb19jsn671d6cab2ec5
   */
  private String key = "f20790a0d6msh98d1dad8f0e3da9p10eb19jsn671d6cab2ec5";
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