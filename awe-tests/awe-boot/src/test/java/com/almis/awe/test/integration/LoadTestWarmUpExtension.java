package com.almis.awe.test.integration;

import org.jsmart.zerocode.core.domain.TestMapping;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

/**
 * Warms up the Spring test context of the classes mapped by zerocode load tests.
 * <p>
 * Zerocode runs every load request as a separate JUnit launch of the mapped test class. When the first requests start
 * at the same time they bootstrap the same Spring test context concurrently and Spring rejects one of them
 * ("AOT attributes cannot be overridden"). Running every mapped test method once before the load, from a
 * {@link BeforeAllCallback} (always executed before the zerocode {@code beforeEach} that starts the load), creates and
 * caches the context in the JVM-wide Spring context cache, so the parallel requests only reuse it. The load itself
 * stays fully parallel. It may be registered anywhere in {@code @ExtendWith}, since
 * every {@code beforeAll} callback runs before any {@code beforeEach} callback.
 */
public class LoadTestWarmUpExtension implements BeforeAllCallback {

  private static final Logger log = LoggerFactory.getLogger(LoadTestWarmUpExtension.class);

  @Override
  public void beforeAll(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    Arrays.stream(testClass.getDeclaredMethods())
        .flatMap(method -> Arrays.stream(method.getAnnotationsByType(TestMapping.class)))
        .forEach(mapping -> warmUp(mapping.testClass(), mapping.testMethod()));
  }

  private void warmUp(Class<?> mappedClass, String mappedMethod) {
    log.info("Warming up the Spring context of {}#{}", mappedClass.getName(), mappedMethod);
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectMethod(mappedClass, mappedMethod))
        .build();
    Launcher launcher = LauncherFactory.create();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.execute(request, listener);
    TestExecutionSummary summary = listener.getSummary();
    if (summary.getTestsSucceededCount() == 0) {
      // No test ran or all failed: the context may not be cached, so the parallel load can hit the bootstrap race again
      log.warn("Warm-up of {}#{} did not run successfully ({} found, {} failed): the load starts without a cached context",
          mappedClass.getName(), mappedMethod, summary.getTestsFoundCount(), summary.getTotalFailureCount());
    }
  }

}
