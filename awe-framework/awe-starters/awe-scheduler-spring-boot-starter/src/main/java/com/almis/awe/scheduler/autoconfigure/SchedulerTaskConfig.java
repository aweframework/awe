package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.component.AweMDCTaskDecorator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConfigurationProperties(prefix = "scheduler.task-pool")
@EnableAsync
@Data
public class SchedulerTaskConfig {

  private Integer size;
  private Integer maxSize;
  private Integer queueSize;
  private Integer terminationSeconds;

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE MDC task decorator
   * @return Thread pool executor bean
   */
  @Bean("schedulerTaskPool")
  public ThreadPoolTaskExecutor getSchedulerTaskPoolExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(getSize());
    executor.setMaxPoolSize(getMaxSize());
    executor.setQueueCapacity(getQueueSize());
    executor.setAwaitTerminationSeconds(getTerminationSeconds());
    executor.setThreadNamePrefix("schedulerTaskPool");
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
    executor.setCorePoolSize(getSize());
    executor.setMaxPoolSize(getMaxSize());
    executor.setQueueCapacity(getQueueSize());
    executor.setAwaitTerminationSeconds(getTerminationSeconds());
    executor.setThreadNamePrefix("schedulerJobPool");
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
    executor.setCorePoolSize(getSize());
    executor.setMaxPoolSize(getMaxSize());
    executor.setQueueCapacity(getQueueSize());
    executor.setAwaitTerminationSeconds(getTerminationSeconds());
    executor.setThreadNamePrefix("schedulerTimeoutPool");
    executor.setTaskDecorator(aweMDCTaskDecorator);
    return executor;
  }

}
