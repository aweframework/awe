package com.almis.awe.scheduler.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Registration guard for {@link SchedulerDefaultsEnvironmentPostProcessor}.
 *
 * <p>An {@code EnvironmentPostProcessor} is discovered through the
 * {@code META-INF/spring.factories} declaration. A typo in the fully-qualified
 * class name or a missing entry would silently disable the post-processor
 * without any compile-time failure. This test asserts the registration is
 * present, points at the expected class, and that the declared name is
 * loadable, without booting a full application context.</p>
 */
class SchedulerDefaultsEnvironmentPostProcessorRegistrationTest {

  private static final String SPRING_FACTORIES = "META-INF/spring.factories";
  private static final String ENVIRONMENT_POST_PROCESSOR_KEY =
    "org.springframework.boot.env.EnvironmentPostProcessor";

  @Test
  void environmentPostProcessorIsRegisteredInSpringFactories() throws Exception {
    Properties properties = new Properties();
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(SPRING_FACTORIES)) {
      assertNotNull(input, "spring.factories must be present on the classpath");
      properties.load(input);
    }

    String registered = properties.getProperty(ENVIRONMENT_POST_PROCESSOR_KEY);

    assertEquals(SchedulerDefaultsEnvironmentPostProcessor.class.getName(), registered);
    assertDoesNotThrow(() -> Class.forName(registered));
  }
}
