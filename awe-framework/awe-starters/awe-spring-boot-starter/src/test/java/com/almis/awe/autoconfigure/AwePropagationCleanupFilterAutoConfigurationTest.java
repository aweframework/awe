package com.almis.awe.autoconfigure;

import com.almis.awe.component.AwePropagationCleanupFilter;
import com.almis.awe.config.*;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.*;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.*;
import com.almis.awe.service.connector.JavaConnector;
import com.almis.awe.service.connector.MicroserviceConnector;
import com.almis.awe.service.connector.RestConnector;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.maintain.ServiceMaintainConnector;
import com.almis.awe.service.data.connector.query.EnumQueryConnector;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.ServiceQueryConnector;
import com.almis.awe.service.report.ReportDesigner;
import com.almis.awe.service.report.ReportGenerator;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import com.almis.awe.service.screen.ScreenConfigurationGenerator;
import com.almis.awe.service.screen.ScreenModelGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that {@link AwePropagationCleanupFilter} is registered as a Spring-managed bean
 * by the real {@link AweAutoConfiguration}, and that a custom override is respected.
 *
 * <p>Uses the actual exported auto-configuration class via
 * {@link AutoConfigurations#of(Class[])} so the test exercises the live
 * {@code @ConditionalOnMissingBean} contract, not a hand-copied local mirror.</p>
 *
 * <p>Heavy transitive dependencies are satisfied with Mockito mocks registered via
 * {@code .withBean()} — this keeps the context slim while still loading the real
 * configuration class and its bean methods.</p>
 *
 * <p>Only beans that are annotated {@code @ConditionalOnMissingBean} in
 * {@link AweAutoConfiguration} are pre-registered here; that allows Spring's condition
 * evaluation to skip those bean definitions without a {@link
 * org.springframework.beans.factory.support.BeanDefinitionOverrideException}.
 * Prototype-scoped beans without that annotation (e.g. {@code dataListBuilder},
 * {@code enumBuilder}) are never pre-registered — they are created by
 * {@code AweAutoConfiguration} directly and their lightweight no-arg constructors succeed
 * without a full application context.</p>
 */
@ExtendWith(SpringExtension.class)
class AwePropagationCleanupFilterAutoConfigurationTest {

  /**
   * Shared runner that targets the real {@link AweAutoConfiguration} and supplies
   * mocks for its conditional bean dependencies so only the propagation-related
   * beans need their real implementations.
   *
   * <p>Every {@code .withBean(..., () -> mock(...))} call pre-registers a bean that
   * satisfies a {@code @ConditionalOnMissingBean} guard inside {@link AweAutoConfiguration},
   * preventing it from trying to build the real (heavyweight) bean for that type.
   * The two beans under test — {@code prototypeRequestBeanHolder} and
   * {@code awePropagationCleanupFilter} — are intentionally NOT pre-registered so
   * {@link AweAutoConfiguration} creates them.</p>
   */
  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(AweAutoConfiguration.class))
      .withPropertyValues(
          "awe.application.module-list=awe",
          "spring.cache.type=NONE",
          "awe.database.enabled=false"
      )
      .withBean(NoOpCacheManager.class)
      .withBean(DefaultConversionService.class)
      .withBean(ObjectMapper.class)
      // Property classes registered as @EnableConfigurationProperties — provide real instances
      // so they bind cleanly from the property values above.
      .withBean(BaseConfigProperties.class)
      .withBean(SecurityConfigProperties.class)
      .withBean(DatabaseConfigProperties.class)
      // AweSession is defined in SessionConfig (not loaded here) — mock it so aweLoggingFilter can wire.
      .withBean(AweSession.class, () -> mock(AweSession.class))
      .withBean(MultiTenantOAuth2Config.class, () -> mock(MultiTenantOAuth2Config.class))
      .withBean(MultiTenantFilter.class, () -> mock(MultiTenantFilter.class))
      // Pre-register mocks for all @ConditionalOnMissingBean services.
      // These mocks prevent AweAutoConfiguration from constructing the real beans,
      // which would require the full AWE application context.
      .withBean(NumericService.class, () -> mock(NumericService.class))
      .withBean(EncodeService.class, () -> mock(EncodeService.class))
      .withBean(AweElements.class, () -> mock(AweElements.class))
      .withBean(AweElementsDao.class, () -> mock(AweElementsDao.class))
      .withBean(QueryUtil.class, () -> mock(QueryUtil.class))
      .withBean(DataListService.class, () -> mock(DataListService.class))
      .withBean(PropertyService.class, () -> mock(PropertyService.class))
      .withBean(ActionService.class, () -> mock(ActionService.class))
      .withBean(MaintainService.class, () -> mock(MaintainService.class))
      .withBean(QueryService.class, () -> mock(QueryService.class))
      .withBean(LauncherService.class, () -> mock(LauncherService.class))
      .withBean(FileService.class, () -> mock(FileService.class))
      .withBean(LogService.class, () -> mock(LogService.class))
      .withBean(BroadcastService.class, () -> mock(BroadcastService.class))
      .withBean(SessionService.class, () -> mock(SessionService.class))
      .withBean(ScreenService.class, () -> mock(ScreenService.class))
      .withBean(InitService.class, () -> mock(InitService.class))
      .withBean(MaintainLauncher.class, () -> mock(MaintainLauncher.class))
      .withBean(QueryLauncher.class, () -> mock(QueryLauncher.class))
      .withBean(JavaConnector.class, () -> mock(JavaConnector.class))
      .withBean(RestConnector.class, () -> mock(RestConnector.class))
      .withBean(MicroserviceConnector.class, () -> mock(MicroserviceConnector.class))
      .withBean(ServiceMaintainConnector.class, () -> mock(ServiceMaintainConnector.class))
      .withBean(ServiceQueryConnector.class, () -> mock(ServiceQueryConnector.class))
      .withBean(EnumQueryConnector.class, () -> mock(EnumQueryConnector.class))
      .withBean(ReportGenerator.class, () -> mock(ReportGenerator.class))
      .withBean(ReportDesigner.class, () -> mock(ReportDesigner.class))
      .withBean(ScreenModelGenerator.class, () -> mock(ScreenModelGenerator.class))
      .withBean(ScreenConfigurationGenerator.class, () -> mock(ScreenConfigurationGenerator.class))
      .withBean(ScreenComponentGenerator.class, () -> mock(ScreenComponentGenerator.class))
      .withBean(InitialLoadDao.class, () -> mock(InitialLoadDao.class))
      .withBean(ErrorPageService.class, () -> mock(ErrorPageService.class));

  /**
   * Core registration test: the filter must be auto-configured as a singleton bean named
   * {@code awePropagationCleanupFilter} when no user-supplied override is present.
   *
   * <p>This test exercises the real {@link AweAutoConfiguration#awePropagationCleanupFilter}
   * method — not a copy — so any future rename or removal of that method will immediately
   * surface as a test failure.</p>
   */
  @Test
  void awePropagationCleanupFilter_isRegisteredByAutoConfiguration() {
    contextRunner.run(context -> {
      assertThat(context).hasBean("awePropagationCleanupFilter");
      assertThat(context).hasSingleBean(AwePropagationCleanupFilter.class);
    });
  }

  /**
   * Dependency wiring test: the filter must be wired to the <em>same singleton</em>
   * {@link PrototypeRequestBeanHolder} instance that {@link AweAutoConfiguration} also creates.
   *
   * <p>A filter wired to a <em>different</em> holder instance would silently fail to clean up
   * the ThreadLocal, causing cross-request leakage that is nearly impossible to diagnose
   * in production.</p>
   *
   * <p>The assertion reads {@code AwePropagationCleanupFilter#prototypeRequestBeanHolder}
   * via reflection and uses {@code isSameAs} to verify object identity — not just
   * non-null equality. This catches any wiring regression where two separate
   * {@link PrototypeRequestBeanHolder} instances are created (one injected into the filter,
   * one exposed as the context bean).</p>
   */
  @Test
  void awePropagationCleanupFilter_isWiredToSingletonPrototypeRequestBeanHolder() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(PrototypeRequestBeanHolder.class);
      assertThat(context).hasSingleBean(AwePropagationCleanupFilter.class);

      PrototypeRequestBeanHolder contextHolder = context.getBean(PrototypeRequestBeanHolder.class);
      AwePropagationCleanupFilter filter = context.getBean(AwePropagationCleanupFilter.class);

      // Extract the private field to verify identity, not just non-null presence.
      Field holderField = ReflectionUtils.findField(AwePropagationCleanupFilter.class, "prototypeRequestBeanHolder");
      assertThat(holderField).as("Field 'prototypeRequestBeanHolder' must exist on AwePropagationCleanupFilter").isNotNull();
      ReflectionUtils.makeAccessible(holderField);
      PrototypeRequestBeanHolder wiredHolder = (PrototypeRequestBeanHolder) ReflectionUtils.getField(holderField, filter);

      assertThat(wiredHolder)
          .as("Filter must be wired to the same PrototypeRequestBeanHolder instance registered in the context — " +
              "a different instance would silently skip ThreadLocal cleanup causing cross-request leakage")
          .isSameAs(contextHolder);
    });
  }

  /**
   * Override test: when the application supplies its own {@link AwePropagationCleanupFilter}
   * bean, the auto-configuration must back off ({@code @ConditionalOnMissingBean}) and not
   * register a second instance.
   *
   * <p>This ensures operators can replace the default filter (e.g. to add metrics or
   * custom cleanup) without triggering a conflicting bean definition error.</p>
   */
  @Test
  void awePropagationCleanupFilter_backOffWhenUserBeanPresent() {
    PrototypeRequestBeanHolder customHolder = new PrototypeRequestBeanHolder();
    AwePropagationCleanupFilter customFilter = new AwePropagationCleanupFilter(customHolder);

    contextRunner
        .withBean("awePropagationCleanupFilter", AwePropagationCleanupFilter.class, () -> customFilter)
        .run(context -> {
          // Still exactly one bean — the user-supplied one
          assertThat(context).hasSingleBean(AwePropagationCleanupFilter.class);
          // Must be the custom instance, not a second auto-configured one
          assertThat(context.getBean(AwePropagationCleanupFilter.class)).isSameAs(customFilter);
        });
  }
}
