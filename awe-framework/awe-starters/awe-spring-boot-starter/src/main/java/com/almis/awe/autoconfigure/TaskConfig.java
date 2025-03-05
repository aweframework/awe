package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.TaskConfigProperties;
import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.almis.awe.executor.ContextAwarePoolExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Class used to launch initial load treads
 */
@Configuration
@EnableConfigurationProperties(value = TaskConfigProperties.class)
@EnableAsync
public class TaskConfig {

  // Autowired beans
  private final TaskConfigProperties properties;

  /**
   * Executor task config constructor
   * @param properties Task executor configuration properties
   */
  public TaskConfig(TaskConfigProperties properties) {
    this.properties = properties;
  }

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE task decorator
   * @return Thread pool executor bean
   */
  @Bean("threadPoolTaskExecutor")
  public ContextAwarePoolExecutor getContextAwareTaskExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ContextAwarePoolExecutor executor = new ContextAwarePoolExecutor();
    executor.setCorePoolSize(properties.getSize());
    executor.setMaxPoolSize(properties.getMaxSize());
    executor.setQueueCapacity(properties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) properties.getAwaitTermination().getSeconds());
    executor.setThreadNamePrefix(properties.getThreadPrefix());
    executor.setTaskDecorator(aweMDCTaskDecorator);
    executor.initialize();
    return executor;
  }

  /**
   * Returns the asynchronous executor task
   * @param aweMDCTaskDecorator AWE task decorator
   * @return Thread pool executor bean
   */
  @Bean("threadHelpPoolTaskExecutor")
  public ContextAwarePoolExecutor getHelpContextAwareTaskExecutor(AweMDCTaskDecorator aweMDCTaskDecorator) {
    ContextAwarePoolExecutor executor = new ContextAwarePoolExecutor();
    executor.setCorePoolSize(properties.getSize());
    executor.setMaxPoolSize(properties.getMaxSize());
    executor.setQueueCapacity(properties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) properties.getAwaitTermination().getSeconds());
    executor.setThreadNamePrefix(properties.getHelpThreadPrefix());
    executor.setTaskDecorator(aweMDCTaskDecorator);
    return executor;
  }

  /**
   * Returns the asynchronous executor task
   * @return Thread pool executor bean
   */
  @Bean("contextLessTaskExecutor")
  public ThreadPoolTaskExecutor getContextLessTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getSize());
    executor.setMaxPoolSize(properties.getMaxSize());
    executor.setQueueCapacity(properties.getQueueSize());
    executor.setAwaitTerminationSeconds((int) properties.getAwaitTermination().getSeconds());
    executor.setThreadNamePrefix(properties.getContextlessThreadPrefix());
    return executor;
  }

  /**
   * Awe MDC Task decorator
   * @param requestDataHolder request data holder
   * @param prototypeRequestBeanHolder prototype request bean holder
   * @return awe MDC task decorator
   */
  @Bean
  public AweMDCTaskDecorator aweMDCTaskDecorator(ObjectProvider<RequestDataHolder> requestDataHolder, PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    return new AweMDCTaskDecorator(requestDataHolder, prototypeRequestBeanHolder);
  }
}
