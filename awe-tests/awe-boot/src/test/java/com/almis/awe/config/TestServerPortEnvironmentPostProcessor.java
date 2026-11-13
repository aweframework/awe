package com.almis.awe.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Gives the test JVM one free port for the test application (property awe.test.server-port).
 * <p>
 * application.properties uses it as server.port and as the base url of the services that call the application itself, so
 * every Spring context of the JVM agrees on it and two test JVMs on the same machine (parallel jobs of the local pipeline)
 * never compete for a fixed port. A port given on the command line (-Dawe.test.server-port=...) wins: the system properties rank above this last source.
 */
public class TestServerPortEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String PROPERTY = "awe.test.server-port";
  private static final int PORT = freePort();

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    if (!environment.getPropertySources().contains(PROPERTY)) {
      environment.getPropertySources().addLast(new MapPropertySource(PROPERTY, Map.of(PROPERTY, PORT)));
    }
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new UncheckedIOException("No free port for the test application", exception);
    }
  }
}
