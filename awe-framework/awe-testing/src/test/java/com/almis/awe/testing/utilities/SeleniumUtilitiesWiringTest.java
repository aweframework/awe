package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.selenium.IAweInstructions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.test.context.TestPropertySource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * A browser test class (a subclass of {@link SeleniumUtilities}) gets the selenium model from the extension only after
 * Spring has injected the test properties into it, as the browser suites of the test applications do
 */
class SeleniumUtilitiesWiringTest {

  private static final AtomicReference<AweTestConfigProperties> PROPERTIES_AT_MODEL = new AtomicReference<>();

  @Test
  void injectsThePropertiesBeforeTheExtensionSetsTheModel() {
    PROPERTIES_AT_MODEL.set(null);
    LauncherDiscoveryRequest discovery = request().selectors(selectClass(BrowserTestClass.class)).build();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    Launcher launcher = LauncherFactory.create();

    launcher.execute(discovery, listener);

    TestExecutionSummary summary = listener.getSummary();
    StringWriter failures = new StringWriter();
    summary.printFailuresTo(new PrintWriter(failures), 20);
    assertThat(summary.getTotalFailureCount()).as(failures.toString()).isZero();
    assertThat(PROPERTIES_AT_MODEL.get()).isNotNull();
  }

  /**
   * Browser test class. Its instance is prepared for the class (no browser is opened: its only test is disabled). It is
   * public so that the extension can call the overridden setter
   */
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @TestPropertySource(properties = "awe.test.start-url=http://localhost:8080/")
  public static class BrowserTestClass extends SeleniumUtilities {

    @Override
    public IAweInstructions setSeleniumModel(SeleniumModel model) {
      IAweInstructions instructions = super.setSeleniumModel(model);
      PROPERTIES_AT_MODEL.set(model.getProperties());
      return instructions;
    }

    @Test
    @Disabled("The browser is never opened: the class is only prepared")
    void browserTest() {
      // Not run
    }
  }
}
