package com.almis.awe.observability.autoconfigure;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.observability.health.AweDatabasesHealthIndicator;
import com.almis.awe.observability.health.AweQueueHealthIndicator;
import com.almis.awe.observability.instrumentation.MicrometerExecutionObserver;
import com.almis.awe.service.observation.ExecutionObserver;
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

/**
 * AWE observability auto-configuration: health checks, metrics and traces of the AWE connectors, queries,
 * maintains and actions.
 *
 * <p>It is active only when the starter is on the classpath, so projects that do not add the dependency see no
 * change. It runs after the Spring Boot observation and AWE auto-configurations, because it needs their beans.</p>
 */
@AutoConfiguration(
  afterName = {
    "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration",
    "com.almis.awe.autoconfigure.AweAutoConfiguration",
    "com.almis.awe.autoconfigure.SQLConfig",
    "com.almis.awe.autoconfigure.JmsConfig",
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
   * Metrics and traces of the AWE query, maintain and action executions. AWE uses this bean on its own: it is the
   * {@link ExecutionObserver} its services look for.
   *
   * @param registry Observation registry provider
   * @return Execution observer bean
   */
  @Bean
  @ConditionalOnMissingBean(ExecutionObserver.class)
  MicrometerExecutionObserver aweExecutionObserver(ObjectProvider<ObservationRegistry> registry) {
    return new MicrometerExecutionObserver(registry::getIfAvailable);
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
