package com.almis.awe.developer.autoconfigure;

import com.almis.awe.developer.translators.TranslationServiceEnum;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Developer module properties
 */
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "developer")
public class DeveloperConfigProperties {
  /**
   * Developer path
   * Default value ${user.home}/awe-developer
   */
  @Value("${user.home}/awe-developer")
  private String path;
  /**
   * Path file name
   * Default value path.properties
   */
  private String pathFile = "path.properties";
  /**
   * Path property name
   * Default value path.project
   */
  private String pathProperty = "path.project";
  /**
   * Translation service
   * Default value MY_MEMORY
   */
  private TranslationServiceEnum translationService = TranslationServiceEnum.MY_MEMORY;
}