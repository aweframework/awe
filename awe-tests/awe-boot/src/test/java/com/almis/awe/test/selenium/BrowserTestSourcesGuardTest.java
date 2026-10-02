package com.almis.awe.test.selenium;

import com.almis.awe.testing.guard.BrowserTestSourceGuard;
import com.almis.awe.testing.guard.BrowserTestSourceGuard.Report;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the Selenium tests of this application free of selectors and automation-tool types: they only express screen
 * steps, and the way the elements are located lives in the front-end instructions of {@code awe-testing}.
 *
 * <p>It is a unit test over the sources, so it runs with the unit tests and does not need a browser.</p>
 */
// The surefire run of this module (All UT) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class BrowserTestSourcesGuardTest {

  private static final Path SELENIUM_TESTS = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
    .resolve("src/test/java/com/almis/awe/test/selenium");

  @Test
  void shouldKeepTheSeleniumTestsFreeOfSelectorsAndAutomationToolTypes() throws IOException {
    Report report = BrowserTestSourceGuard.create()
      .allow("IntegrationTestsIT.java", "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"angular-filemanager\")",
        "The file manager is a third-party application inside a frame, so its content is not part of the AWE vocabulary")
      .scan(SELENIUM_TESTS);

    assertThat(report.isClean()).as(report.describe()).isTrue();
  }
}
