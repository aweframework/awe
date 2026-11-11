package com.almis.awe.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AweWebDefaultsEnvironmentPostProcessor}: the HTTP compression and resource chain defaults that
 * every AWE application receives (issue #965).
 */
class AweWebDefaultsEnvironmentPostProcessorTest {

  private final AweWebDefaultsEnvironmentPostProcessor postProcessor = new AweWebDefaultsEnvironmentPostProcessor();

  @Test
  void compressesScriptsStylesJsonAndSvgByDefault() {
    StandardEnvironment environment = new StandardEnvironment();

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("server.compression.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.compression.min-response-size")).isEqualTo("256");
    assertThat(Arrays.asList(environment.getProperty("server.compression.mime-types").split(",")))
      .containsExactlyInAnyOrder("text/html", "text/xml", "text/plain", "text/css", "text/javascript",
        "application/javascript", "application/json", "application/xml", "image/svg+xml");
  }

  @Test
  void servesPrecompressedResourcesByDefault() {
    StandardEnvironment environment = new StandardEnvironment();

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("spring.web.resources.chain.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("spring.web.resources.chain.compressed")).isEqualTo("true");
  }

  @Test
  void applicationValuesWinOverDefaults() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("application", Map.of(
      "server.compression.enabled", "false",
      "server.compression.mime-types", "application/json")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("server.compression.enabled")).isEqualTo("false");
    assertThat(environment.getProperty("server.compression.mime-types")).isEqualTo("application/json");
  }

  @Test
  void isRegisteredInSpringFactories() throws Exception {
    List<String> registered = new ArrayList<>();
    for (URL url : Collections.list(getClass().getClassLoader().getResources("META-INF/spring.factories"))) {
      Properties properties = new Properties();
      try (InputStream input = url.openStream()) {
        properties.load(input);
      }
      String value = properties.getProperty(EnvironmentPostProcessor.class.getName(), "");
      Arrays.stream(value.split(",")).map(String::trim).forEach(registered::add);
    }

    assertThat(registered).contains(AweWebDefaultsEnvironmentPostProcessor.class.getName());
  }
}
