package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.scheduler.autoconfigure.config.SchedulerTaskConfigProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Data
@EnableConfigurationProperties({SchedulerTaskConfigProperties.class})
public class SchedulerTaskConfig {

  private final SchedulerTaskConfigProperties schedulerTaskConfigProperties;
  private final boolean virtualThreads;

  /**
   * Scheduler task config constructor
   * @param schedulerTaskConfigProperties Scheduler task pool configuration properties
   * @param environment Environment, used to read {@code spring.threads.virtual.enabled}
   */
  @Autowired
  public SchedulerTaskConfig(SchedulerTaskConfigProperties schedulerTaskConfigProperties, Environment environment) {
    this.schedulerTaskConfigProperties = schedulerTaskConfigProperties;
    this.virtualThreads = Threading.VIRTUAL.isActive(environment);
  }

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE MDC task decorator
   * @return Thread pool executor bean
   */
  @Bean("schedulerTaskPool")
  public ThreadPoolTaskExecutor getSchedulerTaskPoolExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(schedulerTaskConfigProperties.getSize());
    executor.setMaxPoolSize(schedulerTaskConfigProperties.getMaxSize());
    executor.setQueueCapacity(schedulerTaskConfigProperties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) schedulerTaskConfigProperties.getTermination().getSeconds());
    executor.setThreadNamePrefix("schedulerTaskPool");
    executor.setVirtualThreads(virtualThreads);
    executor.setTaskDecorator(aweMDCTaskDecorator);
    return executor;
  }

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE MDC task decorator
   * @return Thread pool executor bean
   */
  @Bean("schedulerJobPool")
  public ThreadPoolTaskExecutor getSchedulerJobPoolExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(schedulerTaskConfigProperties.getSize());
    executor.setMaxPoolSize(schedulerTaskConfigProperties.getMaxSize());
    executor.setQueueCapacity(schedulerTaskConfigProperties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) schedulerTaskConfigProperties.getTermination().getSeconds());
    executor.setThreadNamePrefix("schedulerJobPool");
    executor.setVirtualThreads(virtualThreads);
    executor.setTaskDecorator(aweMDCTaskDecorator);
    return executor;
  }

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE MDC task decorator
   * @return Thread pool executor bean
   */
  @Bean("schedulerTimeoutPool")
  public ThreadPoolTaskExecutor getSchedulerTimeoutPoolExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(schedulerTaskConfigProperties.getSize());
    executor.setMaxPoolSize(schedulerTaskConfigProperties.getMaxSize());
    executor.setQueueCapacity(schedulerTaskConfigProperties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) schedulerTaskConfigProperties.getTermination().getSeconds());
    executor.setThreadNamePrefix("schedulerTimeoutPool");
    executor.setVirtualThreads(virtualThreads);
    executor.setTaskDecorator(aweMDCTaskDecorator);
    return executor;
  }

}
