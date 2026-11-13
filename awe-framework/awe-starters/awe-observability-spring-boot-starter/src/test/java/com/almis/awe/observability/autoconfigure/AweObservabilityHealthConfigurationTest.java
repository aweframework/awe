package com.almis.awe.observability.autoconfigure;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.observability.health.AweDatabasesHealthIndicator;
import com.almis.awe.observability.health.AweQueueHealthIndicator;
import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AweObservabilityHealthConfigurationTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withBean(ObservationRegistry.class, ObservationRegistry::create)
    .withConfiguration(AutoConfigurations.of(AweObservabilityAutoConfiguration.class));

  @Test
  @DisplayName("No AWE connector, no health indicator")
  void nothingToCheck() {
    runner.run(context -> {
      assertThat(context).doesNotHaveBean(AweDatabasesHealthIndicator.class);
      assertThat(context).doesNotHaveBean(AweQueueHealthIndicator.class);
    });
  }

  @Test
  @DisplayName("The databases indicator is registered with multidatabase, named aweDatabases")
  void databasesIndicator() {
    runner.withBean(AweDatabaseContextHolder.class, () -> mock(AweDatabaseContextHolder.class))
      .withPropertyValues("awe.database.multidatabase-enable=true")
      .run(context -> assertThat(context).hasBean("aweDatabasesHealthIndicator"));
  }

  @Test
  @DisplayName("The databases indicator is not registered when AWE uses a single datasource (Spring Boot already checks it)")
  void noDatabasesIndicatorWithASingleDatasource() {
    runner.withBean(AweDatabaseContextHolder.class, () -> mock(AweDatabaseContextHolder.class))
      .run(context -> assertThat(context).doesNotHaveBean(AweDatabasesHealthIndicator.class));
  }

  @Test
  @DisplayName("The queue indicator is registered when AWE JMS is enabled, named aweQueue")
  void queueIndicator() {
    runner.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
      .withPropertyValues("awe.application.jms.enabled=true")
      .run(context -> assertThat(context).hasBean("aweQueueHealthIndicator"));
  }

  @Test
  @DisplayName("The queue indicator is not registered when AWE JMS is not enabled")
  void noQueueIndicatorWithoutJms() {
    runner.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
      .run(context -> assertThat(context).doesNotHaveBean(AweQueueHealthIndicator.class));
  }

  @Test
  @DisplayName("Each indicator can be switched off with the standard management.health property")
  void canBeSwitchedOff() {
    runner.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
      .withBean(AweDatabaseContextHolder.class, () -> mock(AweDatabaseContextHolder.class))
      .withPropertyValues("awe.application.jms.enabled=true", "awe.database.multidatabase-enable=true",
        "management.health.awe-queue.enabled=false", "management.health.awe-databases.enabled=false")
      .run(context -> {
        assertThat(context).doesNotHaveBean(AweQueueHealthIndicator.class);
        assertThat(context).doesNotHaveBean(AweDatabasesHealthIndicator.class);
      });
  }
}
