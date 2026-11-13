package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.TaskConfigProperties;
import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.executor.ContextAwarePoolExecutor;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
  private final boolean virtualThreads;

  /**
   * Executor task config constructor
   * @param properties Task executor configuration properties
   * @param environment Environment, used to read {@code spring.threads.virtual.enabled}
   */
  public TaskConfig(TaskConfigProperties properties, Environment environment) {
    this.properties = properties;
    this.virtualThreads = Threading.VIRTUAL.isActive(environment);
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
    executor.setVirtualThreads(virtualThreads);
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
    executor.setVirtualThreads(virtualThreads);
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
    executor.setVirtualThreads(virtualThreads);
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
