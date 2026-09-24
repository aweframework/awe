package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.log.DatabaseExecutionLogStore;
import com.almis.awe.scheduler.log.ExecutionLogAppenderInstaller;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.log.ExecutionLogWriter;
import com.almis.awe.scheduler.log.FileExecutionLogStore;
import com.almis.awe.scheduler.log.callback.ExecutionCallbackLogFilter;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the active {@link ExecutionLogStore} bean matches
 * {@code awe.scheduler.execution-log-store}: {@code FileExecutionLogStore} when the property is
 * absent or {@code file}, {@code DatabaseExecutionLogStore} when it is {@code database} — and
 * never both at once.
 */
class ExecutionLogStoreConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ExecutionLogStoreConfig.class))
    .withBean(QueryService.class, () -> mock(QueryService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withBean(QueryUtil.class, () -> mock(QueryUtil.class));

  private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ExecutionLogStoreConfig.class))
    .withBean(QueryService.class, () -> mock(QueryService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withBean(QueryUtil.class, () -> mock(QueryUtil.class));

  @Test
  void fileStoreIsTheDefaultWhenPropertyIsAbsent() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(ExecutionLogStore.class);
      assertThat(context.getBean(ExecutionLogStore.class)).isInstanceOf(FileExecutionLogStore.class);
      assertThat(context).doesNotHaveBean(ExecutionLogWriter.class);
      assertThat(context).doesNotHaveBean(ExecutionLogAppenderInstaller.class);
    });
  }

  @Test
  void fileStoreIsWiredWhenPropertyIsExplicitlyFile() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=file")
      .run(context -> {
        assertThat(context).hasSingleBean(ExecutionLogStore.class);
        assertThat(context.getBean(ExecutionLogStore.class)).isInstanceOf(FileExecutionLogStore.class);
        assertThat(context).doesNotHaveBean(ExecutionLogWriter.class);
        assertThat(context).doesNotHaveBean(ExecutionLogAppenderInstaller.class);
      });
  }

  @Test
  void databaseStoreIsWiredWhenPropertyIsDatabase() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database")
      .run(context -> {
        assertThat(context).hasSingleBean(ExecutionLogStore.class);
        assertThat(context.getBean(ExecutionLogStore.class)).isInstanceOf(DatabaseExecutionLogStore.class);
        assertThat(context).hasSingleBean(ExecutionLogWriter.class);
        assertThat(context).hasSingleBean(ExecutionLogAppenderInstaller.class);
      });
  }

  /**
   * D3/D7: the application-side callback filter is present only under the database store, in a
   * servlet web application, with the capture kill-switch not explicitly disabled (default on).
   */
  @Test
  void callbackFilterIsRegisteredUnderDatabaseStoreInAServletWebApplicationByDefault() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database")
      .run(context -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterIsRegisteredWhenCaptureIsExplicitlyEnabled() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database", "awe.scheduler.execution-log-callback-capture=true")
      .run(context -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterIsAbsentWhenCaptureIsExplicitlyDisabled() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database", "awe.scheduler.execution-log-callback-capture=false")
      .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterIsAbsentInFileMode() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=file")
      .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterIsAbsentInFileModeEvenIfCaptureIsExplicitlyEnabled() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=file", "awe.scheduler.execution-log-callback-capture=true")
      .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterIsAbsentOutsideAServletWebApplication() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database")
      .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
  }

  @Test
  void callbackFilterInstanceIsAnExecutionCallbackLogFilter() {
    webContextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database")
      .run(context -> assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
        .isInstanceOf(ExecutionCallbackLogFilter.class));
  }
}
