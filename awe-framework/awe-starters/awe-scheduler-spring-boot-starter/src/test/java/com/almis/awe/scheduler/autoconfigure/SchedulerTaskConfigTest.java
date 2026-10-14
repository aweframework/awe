package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the scheduler task executors honor {@code spring.threads.virtual.enabled}
 */
class SchedulerTaskConfigTest {

  private static final String[] EXECUTORS = {"schedulerTaskPool", "schedulerJobPool", "schedulerTimeoutPool"};

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withUserConfiguration(DecoratorConfig.class, SchedulerTaskConfig.class)
    .withPropertyValues("awe.scheduler.task-pool.size=3", "awe.scheduler.task-pool.max-size=7");

  @Test
  void executorsUsePlatformThreadsByDefault() {
    runner.run(context -> assertExecutors(context, false));
  }

  @Test
  void executorsUsePlatformThreadsWhenVirtualThreadsAreDisabled() {
    runner.withPropertyValues("spring.threads.virtual.enabled=false")
      .run(context -> assertExecutors(context, false));
  }

  @Test
  void executorsUseVirtualThreadsWhenVirtualThreadsAreEnabled() {
    runner.withPropertyValues("spring.threads.virtual.enabled=true")
      .run(context -> assertExecutors(context, true));
  }

  private void assertExecutors(AssertableApplicationContext context, boolean virtual) throws Exception {
    assertThat(context).hasNotFailed();
    for (String name : EXECUTORS) {
      ThreadPoolTaskExecutor executor = context.getBean(name, ThreadPoolTaskExecutor.class);
      Thread thread = executor.submitCompletable(Thread::currentThread).get(10, TimeUnit.SECONDS);
      assertThat(thread.isVirtual()).as(name + " virtual").isEqualTo(virtual);
      assertThat(thread.getName()).as(name + " thread name").startsWith(name);
      // Pool limits still apply with virtual threads
      assertThat(executor.getCorePoolSize()).isEqualTo(3);
      assertThat(executor.getMaxPoolSize()).isEqualTo(7);
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class DecoratorConfig {

    @Bean
    RequestDataHolder requestDataHolder() {
      return new RequestDataHolder();
    }

    @Bean
    AweMDCTaskDecorator aweMDCTaskDecorator(ObjectProvider<RequestDataHolder> requestDataHolder) {
      return new AweMDCTaskDecorator(requestDataHolder, new PrototypeRequestBeanHolder());
    }
  }
}
