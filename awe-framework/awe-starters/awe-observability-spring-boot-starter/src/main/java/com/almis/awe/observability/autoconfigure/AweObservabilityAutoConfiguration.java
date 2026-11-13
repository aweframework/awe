package com.almis.awe.observability.autoconfigure;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.observability.health.AweDatabasesHealthIndicator;
import com.almis.awe.observability.health.AweQueueHealthIndicator;
import com.almis.awe.observability.instrumentation.AweConnectionMetrics;
import com.almis.awe.observability.instrumentation.AweTagValueLimitFilter;
import com.almis.awe.observability.instrumentation.MicrometerExecutionObserver;
import com.almis.awe.observability.instrumentation.MicrometerFileTransferObserver;
import com.almis.awe.observability.instrumentation.MicrometerSecurityAuditSink;
import com.almis.awe.service.observation.ExecutionObserver;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import com.almis.awe.service.observation.FileTransferObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.EnumSet;
import java.util.Set;

/**
 * AWE observability auto-configuration: health checks, metrics and traces of the AWE connectors, queries,
 * maintains and actions, and usage metrics (screens, exports, files and connected users).
 *
 * <p>It is active only when the starter is on the classpath, so projects that do not add the dependency see no
 * change. It runs after the Spring Boot observation and AWE auto-configurations, because it needs their beans (the
 * WebSocket user registry comes from the {@code @EnableWebSocketMessageBroker} of AWE's {@code WebsocketConfig}).</p>
 */
@AutoConfiguration(
  afterName = {
    "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration",
    "com.almis.awe.autoconfigure.AweAutoConfiguration",
    "com.almis.awe.autoconfigure.SQLConfig",
    "com.almis.awe.autoconfigure.JmsConfig",
    "com.almis.awe.autoconfigure.WebsocketConfig",
    "org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration",
    "org.springframework.boot.jms.autoconfigure.JndiConnectionFactoryAutoConfiguration",
    "org.springframework.boot.activemq.autoconfigure.ActiveMQAutoConfiguration",
    "org.springframework.boot.artemis.autoconfigure.ArtemisAutoConfiguration"
  })
@ConditionalOnProperty(prefix = "awe.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ObservationRegistry.class)
@EnableConfigurationProperties(AweObservabilityProperties.class)
public class AweObservabilityAutoConfiguration {

  /**
   * Metrics and traces of the AWE query, maintain, action, screen and export executions. AWE uses this bean on its
   * own: it is the {@link ExecutionObserver} its services look for. The screens and exports whose usage family is
   * off run plainly.
   *
   * @param registry   Observation registry provider
   * @param properties Observability properties
   * @return Execution observer bean
   */
  @Bean
  @ConditionalOnMissingBean(ExecutionObserver.class)
  MicrometerExecutionObserver aweExecutionObserver(ObjectProvider<ObservationRegistry> registry,
                                                   AweObservabilityProperties properties) {
    Set<ExecutionType> disabledTypes = EnumSet.noneOf(ExecutionType.class);
    if (!properties.getUsage().getScreens().isEnabled()) {
      disabledTypes.add(ExecutionType.SCREEN);
    }
    if (!properties.getUsage().getExports().isEnabled()) {
      disabledTypes.add(ExecutionType.EXPORT);
    }
    return new MicrometerExecutionObserver(registry::getIfAvailable, disabledTypes);
  }

  /**
   * Files uploaded and downloaded ({@code awe.observability.usage.files.enabled}, on by default)
   *
   * @param registry Meter registry provider
   * @return File transfer observer bean
   */
  @Bean
  @ConditionalOnMissingBean(FileTransferObserver.class)
  @ConditionalOnProperty(prefix = "awe.observability.usage.files", name = "enabled", havingValue = "true", matchIfMissing = true)
  MicrometerFileTransferObserver aweFileTransferObserver(ObjectProvider<MeterRegistry> registry) {
    return new MicrometerFileTransferObserver(registry::getIfAvailable);
  }

  /**
   * Logged users connected and their WebSocket sessions open right now, when the application has a WebSocket message
   * broker ({@code awe.observability.usage.users.enabled}, on by default)
   *
   * @param userRegistry Registry of the users with a live WebSocket session
   * @return Connection gauges binder
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(SimpUserRegistry.class)
  @ConditionalOnProperty(prefix = "awe.observability.usage.users", name = "enabled", havingValue = "true", matchIfMissing = true)
  AweConnectionMetrics aweConnectionMetrics(SimpUserRegistry userRegistry) {
    return new AweConnectionMetrics(userRegistry);
  }

  /**
   * Caps the distinct values of the id and screen tags of the AWE meters ({@code awe.observability.max-tag-values})
   *
   * @param properties Observability properties
   * @return Meter filter bean
   */
  @Bean
  @ConditionalOnMissingBean
  AweTagValueLimitFilter aweTagValueLimitFilter(AweObservabilityProperties properties) {
    return new AweTagValueLimitFilter(properties.getMaxTagValues());
  }

  /**
   * Micrometer sink of the security audit: counts the events on {@code awe.security.audit.events}
   * ({@code awe.security.audit.metrics.enabled}, on by default)
   *
   * @param registry Meter registry provider
   * @return Security audit metrics sink
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "awe.security.audit", name = {"enabled", "metrics.enabled"}, havingValue = "true", matchIfMissing = true)
  MicrometerSecurityAuditSink aweSecurityAuditMetricsSink(ObjectProvider<MeterRegistry> registry) {
    return new MicrometerSecurityAuditSink(registry::getIfAvailable);
  }

  /**
   * Health check of the databases AWE connects to by alias
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(AweDatabaseContextHolder.class)
  @ConditionalOnProperty(name = "awe.database.multidatabase-enable", havingValue = "true")
  @ConditionalOnEnabledHealthIndicator("awe-databases")
  static class DatabasesHealthConfiguration {

    /**
     * Databases health indicator
     *
     * @param contextHolder AWE database context holder
     * @param properties    Observability properties
     * @return Health indicator bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "aweDatabasesHealthIndicator")
    AweDatabasesHealthIndicator aweDatabasesHealthIndicator(AweDatabaseContextHolder contextHolder,
                                                            AweObservabilityProperties properties) {
      return new AweDatabasesHealthIndicator(contextHolder::getDataSourceMap, properties.getHealthTimeout());
    }
  }

  /**
   * Health check of the JMS broker AWE queues use
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ConnectionFactory.class)
  @ConditionalOnBean(ConnectionFactory.class)
  @ConditionalOnProperty(name = "awe.application.jms.enabled", havingValue = "true")
  @ConditionalOnEnabledHealthIndicator("awe-queue")
  static class QueueHealthConfiguration {

    /**
     * Queue health indicator
     *
     * @param connectionFactory JMS connection factory
     * @param properties        Observability properties
     * @return Health indicator bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "aweQueueHealthIndicator")
    AweQueueHealthIndicator aweQueueHealthIndicator(ConnectionFactory connectionFactory,
                                                    AweObservabilityProperties properties) {
      return new AweQueueHealthIndicator(connectionFactory, properties.getHealthTimeout());
    }
  }
}
