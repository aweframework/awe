package com.almis.awe.observability.autoconfigure;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.observability.instrumentation.MicrometerExecutionObserver;
import com.almis.awe.observability.instrumentation.MicrometerSecurityAuditSink;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.SQLQueryConnector;
import com.almis.awe.service.observation.ExecutionObserver;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AweObservabilityAutoConfigurationTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withBean(ObservationRegistry.class, ObservationRegistry::create)
    .withConfiguration(AutoConfigurations.of(AweObservabilityAutoConfiguration.class));

  @Test
  @DisplayName("The auto-configuration is registered in the Spring Boot imports file")
  void isRegistered() throws IOException {
    Path imports = Path.of("src", "main", "resources", "META-INF", "spring",
      "org.springframework.boot.autoconfigure.AutoConfiguration.imports");

    assertThat(Files.readAllLines(imports)).contains(AweObservabilityAutoConfiguration.class.getName());
  }

  @Test
  @DisplayName("It contributes the execution observer and its properties with no configuration at all")
  void worksWithDefaults() {
    runner.run(context -> {
      assertThat(context).hasSingleBean(MicrometerExecutionObserver.class);
      assertThat(context.getBean(AweObservabilityProperties.class).isEnabled()).isTrue();
      assertThat(context.getBean(AweObservabilityProperties.class).getHealthTimeout()).isEqualTo(Duration.ofSeconds(2));
    });
  }

  @Test
  @DisplayName("awe.observability.enabled=false switches the whole starter off")
  void canBeDisabled() {
    runner.withPropertyValues("awe.observability.enabled=false").run(context -> {
      assertThat(context).doesNotHaveBean(AweObservabilityProperties.class);
      assertThat(context).doesNotHaveBean(ExecutionObserver.class);
    });
  }

  @Test
  @DisplayName("It stays out of the way when there is no observation registry (actuator not active)")
  void needsAnObservationRegistry() {
    new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(AweObservabilityAutoConfiguration.class))
      .run(context -> assertThat(context).doesNotHaveBean(AweObservabilityProperties.class));
  }

  @Test
  @DisplayName("An observer defined by the application replaces the starter one")
  void applicationObserverWins() {
    ExecutionObserver custom = ExecutionObserver.NONE;

    runner.withBean("customObserver", ExecutionObserver.class, () -> custom)
      .run(context -> assertThat(context).hasSingleBean(ExecutionObserver.class).doesNotHaveBean(MicrometerExecutionObserver.class));
  }

  @Test
  @DisplayName("AWE picks the observer up on its own: a query launched through the application context is observed")
  void aweUsesTheObserver() {
    new ApplicationContextRunner()
      .withBean(ObservationRegistry.class, TestObservationRegistry::create)
      .withBean(SQLQueryConnector.class, () -> {
        SQLQueryConnector connector = mock(SQLQueryConnector.class);
        try {
          when(connector.launch(any(), any())).thenReturn(new ServiceData());
        } catch (Exception exc) {
          throw new IllegalStateException(exc);
        }
        return connector;
      })
      .withBean(QueryLauncher.class)
      .withConfiguration(AutoConfigurations.of(AweObservabilityAutoConfiguration.class))
      .run(context -> {
        context.getBean(QueryLauncher.class).launchQuery(new Query().setId("Q1"), null);

        assertThat((TestObservationRegistry) context.getBean(ObservationRegistry.class))
          .hasSingleObservationThat()
          .hasNameEqualTo("awe.query")
          .hasLowCardinalityKeyValue("id", "Q1")
          .hasLowCardinalityKeyValue("connector", "sql");
      });
  }

  @Test
  @DisplayName("It counts the security audit events unless awe.security.audit.metrics.enabled is false")
  void securityAuditMetrics() {
    runner.run(context -> assertThat(context).hasSingleBean(MicrometerSecurityAuditSink.class));
    runner.withPropertyValues("awe.security.audit.metrics.enabled=false")
      .run(context -> assertThat(context).doesNotHaveBean(MicrometerSecurityAuditSink.class));
    runner.withPropertyValues("awe.security.audit.enabled=false")
      .run(context -> assertThat(context).doesNotHaveBean(MicrometerSecurityAuditSink.class));
  }
}
