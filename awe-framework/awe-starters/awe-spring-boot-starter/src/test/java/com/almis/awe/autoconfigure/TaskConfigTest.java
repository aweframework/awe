package com.almis.awe.autoconfigure;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the AWE task executors honor {@code spring.threads.virtual.enabled}
 */
class TaskConfigTest {

  private static final String[] EXECUTORS = {"threadPoolTaskExecutor", "threadHelpPoolTaskExecutor", "contextLessTaskExecutor"};
  private static final String[] PREFIXES = {"AweThread-", "AweHelpThread-", "AweInitThread-"};

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withBean(RequestDataHolder.class, RequestDataHolder::new)
    .withBean(PrototypeRequestBeanHolder.class, PrototypeRequestBeanHolder::new)
    .withUserConfiguration(TaskConfig.class)
    .withPropertyValues("awe.application.task.pool.size=3", "awe.application.task.pool.max-size=7");

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
    for (int i = 0; i < EXECUTORS.length; i++) {
      ThreadPoolTaskExecutor executor = context.getBean(EXECUTORS[i], ThreadPoolTaskExecutor.class);
      Thread thread = executor.submitCompletable(Thread::currentThread).get(10, TimeUnit.SECONDS);
      assertThat(thread.isVirtual()).as(EXECUTORS[i] + " virtual").isEqualTo(virtual);
      assertThat(thread.getName()).as(EXECUTORS[i] + " thread name").startsWith(PREFIXES[i]);
      // Pool limits still apply with virtual threads
      assertThat(executor.getCorePoolSize()).isEqualTo(3);
      assertThat(executor.getMaxPoolSize()).isEqualTo(7);
    }
  }
}
