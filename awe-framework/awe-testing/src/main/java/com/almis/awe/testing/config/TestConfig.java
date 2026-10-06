package com.almis.awe.testing.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AweTestConfigProperties.class)
public class TestConfig {

  /**
   * Converter of the {@code awe.test.tool} property
   *
   * @return Converter
   */
  @Bean
  @ConfigurationPropertiesBinding
  public static BrowserToolConverter browserToolConverter() {
    return new BrowserToolConverter();
  }
}
