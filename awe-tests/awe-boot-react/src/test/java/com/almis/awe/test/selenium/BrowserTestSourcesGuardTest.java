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
 * <p>The tests are still being migrated, so the check works as a ratchet over {@code browser-test-guard-baseline.txt}: a
 * violation that is not in the baseline fails the build, and so does a baselined violation that no longer exists (remove
 * its line to shrink the baseline). The migration ends when the baseline is empty, and the file is deleted.</p>
 *
 * <p>It is a unit test over the sources, so it runs with the unit tests and does not need a browser.</p>
 */
// The surefire run of this module (All UT) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class BrowserTestSourcesGuardTest {

  private static final Path BASEDIR = Path.of(System.getProperty("basedir", System.getProperty("user.dir")));
  private static final Path SELENIUM_TESTS = BASEDIR.resolve("src/test/java/com/almis/awe/test/selenium");
  private static final Path BASELINE = BASEDIR.resolve("src/test/resources/browser-test-guard-baseline.txt");

  @Test
  void shouldNotAddSelectorsOrAutomationToolTypesToTheSeleniumTests() throws IOException {
    Report report = BrowserTestSourceGuard.create()
      .allowBaseline(BASELINE, "Pending migration to semantic steps (#812)")
      .scan(SELENIUM_TESTS);

    assertThat(report.isClean()).as(report.describe()).isTrue();
  }
}
