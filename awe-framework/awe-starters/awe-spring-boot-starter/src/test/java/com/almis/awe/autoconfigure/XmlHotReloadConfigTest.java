package com.almis.awe.autoconfigure;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.service.hotreload.XmlHotReloadService;
import com.almis.awe.service.hotreload.XmlHotReloadWatcher;
import com.almis.awe.service.hotreload.XmlReloadHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  /**
   * With the property enabled and zero {@link XmlReloadHandler} beans, the wired service must
   * still be a pure no-op for unrecognized files (no consumer depends on the extension point yet)
   */
  @Test
  void hotReloadServiceIsNoOpWithoutHandlerBeans() {
    Path moduleRoot = Paths.get("/tmp/module-one");

    contextRunner
      .withPropertyValues("awe.application.xml-hot-reload=true")
      .run(context -> {
        XmlHotReloadService service = context.getBean(XmlHotReloadService.class);
        // Unrecognized file, no handler registered: must not throw
        assertDoesNotThrow(() -> service.reloadFor(moduleRoot, moduleRoot.resolve("global/Treatments.xml")));
      });
  }

  /**
   * A registered {@link XmlReloadHandler} bean must be wired into the {@link XmlHotReloadService}
   * and consulted for a changed file the built-in classification does not recognize
   */
  @Test
  void registeredHandlerBeanIsWiredIntoTheService() {
    XmlReloadHandler handler = mock(XmlReloadHandler.class);
    Path moduleRoot = Paths.get("/tmp/module-one");
    Path changedFile = moduleRoot.resolve("global/Treatments.xml");
    when(handler.supports(moduleRoot, changedFile)).thenReturn(true);
    when(handler.reload(moduleRoot, changedFile)).thenReturn(XmlReloadHandler.ReloadResult.HANDLED);

    contextRunner
      .withPropertyValues("awe.application.xml-hot-reload=true")
      .withBean("treatmentsHandler", XmlReloadHandler.class, () -> handler)
      .run(context -> {
        assertThat(context).hasSingleBean(XmlHotReloadService.class);
        XmlHotReloadService service = context.getBean(XmlHotReloadService.class);

        service.reloadFor(moduleRoot, changedFile);

        verify(handler).reload(moduleRoot, changedFile);
      });
  }
}
