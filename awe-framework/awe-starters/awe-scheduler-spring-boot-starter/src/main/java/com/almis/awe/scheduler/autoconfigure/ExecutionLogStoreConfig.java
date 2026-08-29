package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.autoconfigure.config.SchedulerConfigProperties;
import com.almis.awe.scheduler.log.DatabaseExecutionLogStore;
import com.almis.awe.scheduler.log.ExecutionLogAppenderInstaller;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.log.ExecutionLogWriter;
import com.almis.awe.scheduler.log.FileExecutionLogStore;
import com.almis.awe.scheduler.log.callback.ExecutionCallbackLogFilter;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the active {@link ExecutionLogStore} adapter, selected by
 * {@code awe.scheduler.execution-log-store}.
 * <p>
 * Kept as its own configuration class (mirroring {@code SchedulerTaskConfig} and
 * {@code XmlHotReloadConfig} in {@code awe-spring-boot-starter}) so each store adapter can be
 * exercised in isolation without wiring the whole {@code SchedulerConfig} bean graph.
 */
@Configuration
@EnableConfigurationProperties(SchedulerConfigProperties.class)
public class ExecutionLogStoreConfig {

  /**
   * File-backed execution log store: the default, reproducing the pre-existing on-disk behavior.
   *
   * @param schedulerConfigProperties Scheduler configuration properties
   * @return File execution log store
   */
  @Bean
  @ConditionalOnProperty(name = "awe.scheduler.execution-log-store", havingValue = "file", matchIfMissing = true)
  public ExecutionLogStore fileExecutionLogStore(SchedulerConfigProperties schedulerConfigProperties) {
    return new FileExecutionLogStore(schedulerConfigProperties.getExecutionLogPath());
  }

  /**
   * Database-mode capture boundary: bounded async queue, windowed batched flush (ADR-3/ADR-4).
   * Started eagerly on bean creation so the writer is ready before any task can begin logging;
   * stopped by {@code ExecutionLogAppenderInstaller} on context shutdown, bundled with detaching
   * the capture appender.
   *
   * @param maintainService           Maintain service
   * @param queryUtil                 Query utilities
   * @param schedulerConfigProperties Scheduler configuration properties
   * @return Execution log writer
   */
  @Bean
  @ConditionalOnProperty(name = "awe.scheduler.execution-log-store", havingValue = "database")
  public ExecutionLogWriter executionLogWriter(MaintainService maintainService, QueryUtil queryUtil,
                                                SchedulerConfigProperties schedulerConfigProperties) {
    ExecutionLogWriter writer = new ExecutionLogWriter(maintainService, queryUtil,
      schedulerConfigProperties.getExecutionLogMaxLines(), schedulerConfigProperties.getExecutionLogQueueCapacity(),
      schedulerConfigProperties.getExecutionLogBatchSize(), schedulerConfigProperties.getExecutionLogFlushInterval(),
      schedulerConfigProperties.getTaskTimeout());
    writer.start();
    return writer;
  }

  /**
   * Database-backed execution log store: reads, purges, and addresses executions through
   * {@code AweSchExeLog} instead of the filesystem.
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param queryUtil       Query utilities
   * @param writer          Execution log writer
   * @return Database execution log store
   */
  @Bean
  @ConditionalOnProperty(name = "awe.scheduler.execution-log-store", havingValue = "database")
  public ExecutionLogStore databaseExecutionLogStore(QueryService queryService, MaintainService maintainService,
                                                      QueryUtil queryUtil, ExecutionLogWriter writer) {
    return new DatabaseExecutionLogStore(queryService, maintainService, queryUtil, writer);
  }

  /**
   * Installs the capture appender onto the root logger (ADR-1) for the lifetime of the
   * application context; its {@code stop()} also drains the {@link ExecutionLogWriter}.
   *
   * @param databaseExecutionLogStore Database-backed execution log store the appender feeds
   * @param writer                    Execution log writer
   * @param schedulerConfigProperties Scheduler configuration properties
   * @return Execution log appender installer
   */
  @Bean
  @ConditionalOnProperty(name = "awe.scheduler.execution-log-store", havingValue = "database")
  public ExecutionLogAppenderInstaller executionLogAppenderInstaller(ExecutionLogStore databaseExecutionLogStore, ExecutionLogWriter writer,
                                                                      SchedulerConfigProperties schedulerConfigProperties) {
    return new ExecutionLogAppenderInstaller(databaseExecutionLogStore, writer,
      schedulerConfigProperties.getExecutionLogPattern(), schedulerConfigProperties.getExecutionLogMaxLineLength());
  }

  /**
   * Tags an admitted scheduler remote-maintain callback so its traces are captured under the
   * application origin (D3). Registered on {@code /api/maintain/*} only under the database
   * execution-log store, with the capture kill-switch not explicitly disabled (D7), and only in a
   * servlet web application - a non-servlet deployment has no filter chain to register into.
   * <p>
   * Explicit order, immediately after the security filter chain (V1): {@code
   * FilterRegistrationBean}'s default order is {@code LOWEST_PRECEDENCE}, which would already run
   * after Spring Security, but the order is set explicitly rather than relying on that default so
   * {@link org.springframework.security.core.context.SecurityContextHolder} is guaranteed
   * populated when admission (A3) is evaluated.
   *
   * @param databaseExecutionLogStore Database-backed execution log store completed on callback end
   * @param schedulerConfigProperties Scheduler configuration properties
   * @return Filter registration for the execution callback log filter
   */
  @Bean
  @Conditional(ExecutionCallbackCaptureCondition.class)
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public FilterRegistrationBean<ExecutionCallbackLogFilter> executionCallbackLogFilter(
      ExecutionLogStore databaseExecutionLogStore, SchedulerConfigProperties schedulerConfigProperties) {
    ExecutionCallbackLogFilter filter = new ExecutionCallbackLogFilter(databaseExecutionLogStore,
      schedulerConfigProperties.isRemoteEnabled(), schedulerConfigProperties.isRemoteCallbackSecureEnabled(),
      schedulerConfigProperties.getRemoteCallbackUser());

    FilterRegistrationBean<ExecutionCallbackLogFilter> registration = new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/api/maintain/*");
    registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
    return registration;
  }

  /**
   * Combines the two independent activation properties (D3, D7) with AND semantics: the database
   * store must be selected, and the capture kill-switch must not be explicitly disabled.
   */
  static class ExecutionCallbackCaptureCondition extends AllNestedConditions {

    ExecutionCallbackCaptureCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = "awe.scheduler.execution-log-store", havingValue = "database")
    static class OnDatabaseStore {
    }

    @ConditionalOnProperty(name = "awe.scheduler.execution-log-callback-capture", havingValue = "true", matchIfMissing = true)
    static class OnCallbackCaptureEnabled {
    }
  }
}
