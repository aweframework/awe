package com.almis.awe.observability.autoconfigure;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.observability.instrumentation.AweConnectionMetrics;
import com.almis.awe.observability.instrumentation.AweTagValueLimitFilter;
import com.almis.awe.observability.instrumentation.MicrometerExecutionObserver;
import com.almis.awe.observability.instrumentation.MicrometerFileTransferObserver;
import com.almis.awe.observability.instrumentation.MicrometerSecurityAuditSink;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.SQLQueryConnector;
import com.almis.awe.service.observation.ExecutionObserver;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import com.almis.awe.service.observation.FileTransferObserver;
import com.almis.awe.service.observation.FileTransferObserver.Direction;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;

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
  @DisplayName("Every usage family is on by default, with a cap of 1000 values per tag")
  void usageDefaults() {
    runner.withBean(SimpUserRegistry.class, DefaultSimpUserRegistry::new).run(context -> {
      AweObservabilityProperties properties = context.getBean(AweObservabilityProperties.class);
      assertThat(properties.getUsage().getScreens().isEnabled()).isTrue();
      assertThat(properties.getUsage().getExports().isEnabled()).isTrue();
      assertThat(properties.getUsage().getFiles().isEnabled()).isTrue();
      assertThat(properties.getUsage().getUsers().isEnabled()).isTrue();
      assertThat(properties.getMaxTagValues()).isEqualTo(1000);
      assertThat(context).hasSingleBean(MicrometerFileTransferObserver.class);
      assertThat(context).hasSingleBean(AweConnectionMetrics.class);
      assertThat(context).hasSingleBean(AweTagValueLimitFilter.class);
    });
  }

  @Test
  @DisplayName("Out of the box, screens, exports, files and users produce their meters")
  void usageMetersOutOfTheBox() {
    meteredRunner().withBean(SimpUserRegistry.class, DefaultSimpUserRegistry::new).run(context -> {
      MeterRegistry meters = useEveryFamily(context);

      assertThat(meters.get("awe.screen").tags("id", "opt1", "screen", "scr1").timer().count()).isEqualTo(1);
      assertThat(meters.get("awe.export").tags("id", "scr1", "format", "pdf").timer().count()).isEqualTo(1);
      assertThat(meters.get("awe.files").tags("direction", "upload").summary().totalAmount()).isEqualTo(10.0);
      assertThat(meters.get("awe.users.connected").gauge().value()).isEqualTo(1.0);
      assertThat(meters.get("awe.connections.active").gauge().value()).isEqualTo(1.0);
    });
  }

  @Test
  @DisplayName("Turning a usage family off removes its meters and leaves the others")
  void usageFamiliesCanBeDisabled() {
    Map<String, String> meterOfFamily = Map.of(
      "screens", "awe.screen",
      "exports", "awe.export",
      "files", "awe.files",
      "users", "awe.users.connected");

    meterOfFamily.forEach((family, meter) -> meteredRunner().withBean(SimpUserRegistry.class, DefaultSimpUserRegistry::new)
      .withPropertyValues("awe.observability.usage." + family + ".enabled=false")
      .run(context -> {
        MeterRegistry meters = useEveryFamily(context);

        assertThat(meters.find(meter).meters()).as("Meter of the disabled family %s", family).isEmpty();
        meterOfFamily.values().stream().filter(other -> !other.equals(meter))
          .forEach(other -> assertThat(meters.find(other).meters()).as("Meter %s with %s off", other, family).isNotEmpty());
      }));

    runner.withBean(SimpUserRegistry.class, DefaultSimpUserRegistry::new).withPropertyValues("awe.observability.usage.files.enabled=false",
        "awe.observability.usage.users.enabled=false")
      .run(context -> {
        assertThat(context).doesNotHaveBean(MicrometerFileTransferObserver.class);
        assertThat(context).doesNotHaveBean(AweConnectionMetrics.class);
      });
  }

  @Test
  @DisplayName("Without a WebSocket user registry there are no connected-users gauges")
  void noUserRegistryNoGauges() {
    runner.run(context -> assertThat(context).doesNotHaveBean(AweConnectionMetrics.class));
  }

  @Test
  @DisplayName("awe.observability.max-tag-values sets the cap of the tag value filter")
  void maxTagValues() {
    runner.withPropertyValues("awe.observability.max-tag-values=2").run(context -> {
      MeterFilter filter = context.getBean(AweTagValueLimitFilter.class);

      assertThat(idAfter(filter, "Q1")).isEqualTo("Q1");
      assertThat(idAfter(filter, "Q2")).isEqualTo("Q2");
      assertThat(idAfter(filter, "Q3")).isEqualTo(AweTagValueLimitFilter.OTHER);
      assertThat(idAfter(filter, "Q1")).isEqualTo("Q1");
    });
  }

  @Test
  @DisplayName("Usage beans defined by the application replace the starter ones")
  void applicationUsageBeansWin() {
    runner.withBean(SimpUserRegistry.class, DefaultSimpUserRegistry::new)
      .withBean("customFiles", FileTransferObserver.class, () -> FileTransferObserver.NONE)
      .withBean("customConnections", AweConnectionMetrics.class, () -> new AweConnectionMetrics(new DefaultSimpUserRegistry()))
      .withBean("customFilter", AweTagValueLimitFilter.class, () -> new AweTagValueLimitFilter(5))
      .run(context -> {
        assertThat(context).hasSingleBean(FileTransferObserver.class).doesNotHaveBean(MicrometerFileTransferObserver.class);
        assertThat(context).hasSingleBean(AweConnectionMetrics.class).hasBean("customConnections");
        assertThat(context).hasSingleBean(AweTagValueLimitFilter.class).hasBean("customFilter");
      });
  }

  /**
   * Runner whose observations become meters of a simple registry, as with Spring Boot metrics
   */
  private ApplicationContextRunner meteredRunner() {
    return new ApplicationContextRunner()
      .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
      .withBean(ObservationRegistry.class, () -> ObservationRegistry.create())
      .withConfiguration(AutoConfigurations.of(AweObservabilityAutoConfiguration.class));
  }

  /**
   * Open a screen, export it, upload a file and connect a user's WebSocket through the beans of the context
   */
  private static MeterRegistry useEveryFamily(org.springframework.context.ApplicationContext context) throws Exception {
    MeterRegistry meters = context.getBean(MeterRegistry.class);
    ObservationRegistry observations = context.getBean(ObservationRegistry.class);
    observations.observationConfig().observationHandler(new DefaultMeterObservationHandler(meters));
    context.getBeansOfType(MeterBinder.class).values().forEach(binder -> binder.bindTo(meters));

    ExecutionObserver observer = context.getBean(ExecutionObserver.class);
    observer.observe(ExecutionType.SCREEN, "opt1", Map.of("screen", "scr1"), () -> "built");
    observer.observe(ExecutionType.EXPORT, "scr1", Map.of("format", "pdf"), () -> "file");
    context.getBeanProvider(FileTransferObserver.class).getIfAvailable(() -> FileTransferObserver.NONE)
      .transferred(Direction.UPLOAD, 10);
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionId("s1");
    Principal user = () -> "jdoe";
    ((DefaultSimpUserRegistry) context.getBean(SimpUserRegistry.class)).onApplicationEvent(
      new SessionConnectedEvent(context, MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), user));
    return meters;
  }

  private static String idAfter(MeterFilter filter, String id) {
    return filter.map(new Meter.Id("awe.query", Tags.of("id", id), null, null, Meter.Type.TIMER)).getTag("id");
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
