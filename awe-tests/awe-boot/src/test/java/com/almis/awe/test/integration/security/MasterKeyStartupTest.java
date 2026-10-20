package com.almis.awe.test.integration.security;

import com.almis.awe.AppBootApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileSystemUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The whole awe-boot application does not start without a master key, and the error a user gets names the property and
 * explains why it is required (not a placeholder resolution error of the properties that follow the master key)
 */
@Tag("integration")
@DisplayName("Master key application startup tests")
class MasterKeyStartupTest {

  private static final String MASTER_KEY_PROPERTY = "awe.security.master-key";
  private static final String REQUIRED_MESSAGE = "awe.security.master-key is required";

  private Path configDirectory;

  @BeforeEach
  void createConfigDirectory() throws Exception {
    configDirectory = Files.createTempDirectory("awe-master-key");
  }

  @AfterEach
  void deleteConfigDirectory() throws Exception {
    FileSystemUtils.deleteRecursively(configDirectory);
  }

  @ParameterizedTest(name = "master key line: \"{0}\"")
  @ValueSource(strings = {"", MASTER_KEY_PROPERTY + "=", MASTER_KEY_PROPERTY + "=  "})
  @DisplayName("Startup fails with the clear master key error when the key is missing or blank")
  void missingMasterKey_failsWithTheClearError(String masterKeyLine) throws Exception {
    Path properties = applicationPropertiesWith(masterKeyLine);

    Throwable failure = catchThrowable(() -> {
      try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(AppBootApplication.class)
        .run("--spring.config.location=file:" + properties.toAbsolutePath())) {
        // The application must not start
      }
    });

    assertThat(failure).isNotNull();
    assertThat(causeMessages(failure))
      .contains(REQUIRED_MESSAGE)
      .doesNotContain("Could not resolve placeholder");
  }

  /**
   * Test application properties with the master key line replaced, an in-memory database (the file database belongs to
   * the other tests) and a random port
   */
  private Path applicationPropertiesWith(String masterKeyLine) throws Exception {
    String content;
    try (InputStream input = new ClassPathResource("application.properties").getInputStream()) {
      content = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
    }
    String properties = content.lines()
      .map(line -> line.startsWith(MASTER_KEY_PROPERTY + "=") ? masterKeyLine : line)
      .map(line -> line.startsWith("spring.datasource.url=") ? "spring.datasource.url=jdbc:hsqldb:mem:awe-master-key" : line)
      .map(line -> line.startsWith("server.port=") ? "server.port=0" : line)
      .collect(Collectors.joining("\n"));
    assertThat(properties).doesNotContain(MASTER_KEY_PROPERTY + "=B1Le3s");
    Path file = configDirectory.resolve("application.properties");
    Files.writeString(file, properties, StandardCharsets.ISO_8859_1);
    return file;
  }

  private static String causeMessages(Throwable failure) {
    StringBuilder messages = new StringBuilder();
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append('\n');
    }
    return messages.toString();
  }
}
