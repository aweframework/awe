package com.almis.awe.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Publishes the HTTP delivery defaults of an AWE application as a lowest-precedence property source.
 *
 * <ul>
 *   <li>Response compression is enabled for HTML, scripts, styles, JSON, XML and SVG. The list is the Spring Boot
 *   default plus {@code image/svg+xml} (logos, icon sprite). Spring Boot serves {@code .js} files as
 *   {@code text/javascript}, so a list without that type leaves the JavaScript bundles uncompressed (issue #965).</li>
 *   <li>The resource chain serves the {@code .br} and {@code .gz} files that the client builds produce next to each
 *   bundle, so browsers download them already compressed and the server does not compress them on every request.</li>
 * </ul>
 *
 * <p>The defaults are added with {@code addLast}: any value of the application (properties, YAML, command line,
 * environment variables) wins over them.</p>
 */
public class AweWebDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /**
   * Name of the property source that holds the defaults.
   */
  static final String PROPERTY_SOURCE_NAME = "awe-web-defaults";

  private static final Map<String, Object> DEFAULTS = Map.of(
    "server.compression.enabled", "true",
    "server.compression.min-response-size", "256",
    "server.compression.mime-types", String.join(",",
      "text/html", "text/xml", "text/plain", "text/css", "text/javascript", "application/javascript",
      "application/json", "application/xml", "image/svg+xml"),
    "spring.web.resources.chain.enabled", "true",
    "spring.web.resources.chain.compressed", "true");

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
  }
}
