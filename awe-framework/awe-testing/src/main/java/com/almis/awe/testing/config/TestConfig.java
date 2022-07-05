package com.almis.awe.testing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AweTestConfigProperties.class)
public class TestConfig {
}
