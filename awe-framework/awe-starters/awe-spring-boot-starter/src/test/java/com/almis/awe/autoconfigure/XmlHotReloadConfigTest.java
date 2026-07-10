package com.almis.awe.autoconfigure;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.service.hotreload.XmlHotReloadService;
import com.almis.awe.service.hotreload.XmlHotReloadWatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the XML hot reload beans are only registered when
 * {@code awe.application.xml-hot-reload} is enabled (zero production footprint by default).
 */
class XmlHotReloadConfigTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(XmlHotReloadConfig.class))
    .withPropertyValues("awe.application.module-list=awe")
    .withBean(AweElements.class, () -> mock(AweElements.class));

  /**
   * By default (property missing) no hot reload bean may be registered
   */
  @Test
  void hotReloadBeansAreNotRegisteredByDefault() {
    contextRunner.run(context -> {
      assertThat(context).doesNotHaveBean(XmlHotReloadService.class);
      assertThat(context).doesNotHaveBean(XmlHotReloadWatcher.class);
    });
  }

  /**
   * With the property explicitly disabled no hot reload bean may be registered
   */
  @Test
  void hotReloadBeansAreNotRegisteredWhenDisabled() {
    contextRunner
      .withPropertyValues("awe.application.xml-hot-reload=false")
      .run(context -> {
        assertThat(context).doesNotHaveBean(XmlHotReloadService.class);
        assertThat(context).doesNotHaveBean(XmlHotReloadWatcher.class);
      });
  }

  /**
   * With the property enabled both beans must be registered, even without a cache manager
   */
  @Test
  void hotReloadBeansAreRegisteredWhenEnabled() {
    contextRunner
      .withPropertyValues("awe.application.xml-hot-reload=true")
      .run(context -> {
        assertThat(context).hasSingleBean(XmlHotReloadService.class);
        assertThat(context).hasSingleBean(XmlHotReloadWatcher.class);
      });
  }

  /**
   * With the property enabled and a cache manager available the context must still wire cleanly
   */
  @Test
  void hotReloadBeansAreRegisteredWhenEnabledWithCacheManager() {
    contextRunner
      .withPropertyValues("awe.application.xml-hot-reload=true")
      .withBean(ConcurrentMapCacheManager.class)
      .run(context -> {
        assertThat(context).hasSingleBean(XmlHotReloadService.class);
        assertThat(context).hasSingleBean(XmlHotReloadWatcher.class);
      });
  }
}
