package com.almis.awe.testing.driver;

import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The condition that runs the real-browser tests only where a browser is installed, and fails where CI requires one
 */
class RequiresPlaywrightBrowserTest {

  private static final String REQUIRED = "awe.test.playwright.required";
  private static final String ENGINE = "awe.test.playwright.engine";
  private final String initialRequired = System.getProperty(REQUIRED);

  @AfterEach
  void restoreTheProperty() {
    if (initialRequired == null) {
      System.clearProperty(REQUIRED);
    } else {
      System.setProperty(REQUIRED, initialRequired);
    }
  }

  @Test
  void theTestsRunWhenTheBrowserIsAvailable() {
    assertThat(RequiresPlaywrightBrowser.Condition.evaluate("Chromium", null, false).isDisabled()).isFalse();
    assertThat(RequiresPlaywrightBrowser.Condition.evaluate("Chromium", null, true).isDisabled()).isFalse();
  }

  @Test
  void theTestsAreSkippedWithTheReasonWhenNoBrowserIsInstalled() {
    assertThat(RequiresPlaywrightBrowser.Condition.evaluate("Firefox", "no browser installed", false).isDisabled()).isTrue();
    assertThat(RequiresPlaywrightBrowser.Condition.evaluate("Firefox", "no browser installed", false).getReason()).hasValue("no browser installed");
  }

  @Test
  void theTestsFailInsteadOfBeingSkippedWhenTheBrowserIsRequiredNamingTheEngine() {
    assertThatThrownBy(() -> RequiresPlaywrightBrowser.Condition.evaluate("Firefox", "no browser installed", true))
      .isInstanceOf(ExtensionConfigurationException.class)
      .hasMessageContaining("Playwright Firefox")
      .hasMessageContaining(REQUIRED)
      .hasMessageContaining("no browser installed");
  }

  @Test
  void theEngineOfTheTestsIsChromiumUnlessFirefoxIsAskedFor() {
    String initial = System.getProperty(ENGINE);
    try {
      System.clearProperty(ENGINE);
      assertThat(PlaywrightTestBrowser.engine()).isEqualTo("chromium");
      assertThat(PlaywrightTestBrowser.engineName()).isEqualTo("Chromium");

      System.setProperty(ENGINE, "firefox");
      assertThat(PlaywrightTestBrowser.engine()).isEqualTo("firefox");
      assertThat(PlaywrightTestBrowser.engineName()).isEqualTo("Firefox");
    } finally {
      if (initial == null) {
        System.clearProperty(ENGINE);
      } else {
        System.setProperty(ENGINE, initial);
      }
    }
  }

  @Test
  void aBrowserThatIsNotInstalledIsToldApartFromOneThatFailsToLaunch() {
    assertThat(PlaywrightTestBrowser.isNotInstalled(new PlaywrightException("Error {\n message='browserType.launch: Executable doesn't exist at /x/firefox'"))).isTrue();
    assertThat(PlaywrightTestBrowser.isNotInstalled(new PlaywrightException("Host system is missing dependencies to run browsers"))).isFalse();
    assertThat(PlaywrightTestBrowser.isNotInstalled(new PlaywrightException("Process exited with code 1"))).isFalse();
    assertThat(PlaywrightTestBrowser.isNotInstalled(new PlaywrightException(null))).isFalse();
  }

  @Test
  void theBrowserIsRequiredOnlyWhenThePropertyIsTrue() {
    System.clearProperty(REQUIRED);
    assertThat(PlaywrightTestBrowser.isRequired()).isFalse();

    System.setProperty(REQUIRED, "false");
    assertThat(PlaywrightTestBrowser.isRequired()).isFalse();

    System.setProperty(REQUIRED, "true");
    assertThat(PlaywrightTestBrowser.isRequired()).isTrue();
  }

  @Test
  void theTestsNeverAskPlaywrightToDownloadABrowser() {
    assertThat(PlaywrightTestBrowser.driverEnvironment())
      .containsEntry("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
      // The rest of the environment goes on to the driver (PLAYWRIGHT_BROWSERS_PATH, PATH, proxies...)
      .containsAllEntriesOf(System.getenv().entrySet().stream()
        .filter(entry -> !entry.getKey().equals("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD"))
        .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)));
  }
}
