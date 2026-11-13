package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserType;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlaywrightBrowserDriverFactoryTest {

  @ParameterizedTest
  @EnumSource(value = BrowserType.class, names = {"CHROME", "HEADLESS_CHROME"})
  void chromeBrowsersRunInChromium(BrowserType type) {
    assertThat(PlaywrightBrowserDriverFactory.engineOf(type)).isEqualTo(PlaywrightBrowserDriverFactory.Engine.CHROMIUM);
  }

  @ParameterizedTest
  @EnumSource(value = BrowserType.class, names = {"FIREFOX", "HEADLESS_FIREFOX"})
  void firefoxBrowsersRunInFirefox(BrowserType type) {
    assertThat(PlaywrightBrowserDriverFactory.engineOf(type)).isEqualTo(PlaywrightBrowserDriverFactory.Engine.FIREFOX);
  }

  @ParameterizedTest
  @EnumSource(value = BrowserType.class, names = {"HEADLESS_CHROME", "HEADLESS_FIREFOX"})
  void headlessBrowsersRunHeadless(BrowserType type) {
    assertThat(PlaywrightBrowserDriverFactory.isHeadless(type)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = BrowserType.class, names = {"CHROME", "FIREFOX"})
  void localBrowsersAreShown(BrowserType type) {
    assertThat(PlaywrightBrowserDriverFactory.isHeadless(type)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(value = BrowserType.class, names = {"EDGE", "IE", "OPERA", "REMOTE_FIREFOX", "REMOTE_CHROME", "SERVICE_FIREFOX",
    "SERVICE_CHROME", "SERVICE_EDGE", "SERVICE_OPERA", "SERVICE_SAFARI"})
  void otherBrowsersFailFastBeforeAnythingIsStarted(BrowserType type) {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setBrowser(type);
    SeleniumModel model = new SeleniumModel().setProperties(properties);

    assertThatThrownBy(() -> new PlaywrightBrowserDriverFactory().create(model, "test"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("'" + type.getName() + "'")
      .hasMessageContaining("Playwright pilot")
      .hasMessageContaining("chrome, headless-chrome, firefox, headless-firefox")
      .hasMessageContaining("Remote and service browsers are not supported yet");

    assertThat(model.getBrowser()).isNull();
  }

  @Test
  void chromiumAlwaysAvoidsTheSharedMemoryOfAContainerAndKeepsItsSandboxOnADesktop() {
    assertThat(PlaywrightBrowserDriverFactory.chromiumArguments(false, null, false)).containsExactly("--disable-dev-shm-usage");
  }

  @Test
  void aHeadlessChromiumRunsWithoutItsSandbox() {
    assertThat(PlaywrightBrowserDriverFactory.chromiumArguments(true, null, false)).containsExactly("--disable-dev-shm-usage", "--no-sandbox");
  }

  @Test
  void aChromiumOfARootUserOrAContainerRunsWithoutItsSandboxEvenWhenItIsShown() {
    assertThat(PlaywrightBrowserDriverFactory.chromiumArguments(false, null, true)).containsExactly("--disable-dev-shm-usage", "--no-sandbox");
  }

  @Test
  void theSandboxOverrideWinsOverTheAutomaticRule() {
    assertThat(PlaywrightBrowserDriverFactory.chromiumArguments(true, false, true)).containsExactly("--disable-dev-shm-usage");
    assertThat(PlaywrightBrowserDriverFactory.chromiumArguments(false, true, false)).containsExactly("--disable-dev-shm-usage", "--no-sandbox");
  }

  @Test
  void aRootUserIsRecognized() {
    assertThat(PlaywrightBrowserDriverFactory.isRootOrContainer("root", path -> false, name -> null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/.dockerenv", "/run/.containerenv"})
  void aDockerOrPodmanContainerIsRecognizedByItsMarkerFile(String marker) {
    assertThat(PlaywrightBrowserDriverFactory.isRootOrContainer("pablo", path -> path.equals(marker), name -> null)).isTrue();
  }

  @Test
  void aKubernetesPodIsRecognizedByItsServiceVariable() {
    assertThat(PlaywrightBrowserDriverFactory.isRootOrContainer("pablo", path -> false,
      name -> "KUBERNETES_SERVICE_HOST".equals(name) ? "10.0.0.1" : null)).isTrue();
    assertThat(PlaywrightBrowserDriverFactory.isRootOrContainer("pablo", path -> false,
      name -> "KUBERNETES_SERVICE_HOST".equals(name) ? " " : null)).isFalse();
  }

  @Test
  void aUserOnADesktopIsNotARootOrContainerSession() {
    assertThat(PlaywrightBrowserDriverFactory.isRootOrContainer("pablo", path -> false, name -> null)).isFalse();
  }

  @Test
  void aContextThatCannotBeCreatedDoesNotLeaveTheFolderOfTheVideoBehind(@org.junit.jupiter.api.io.TempDir java.nio.file.Path evidence) throws java.io.IOException {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setBrowser(BrowserType.HEADLESS_CHROME);
    properties.setScreenshotPath(evidence.toString());
    SeleniumModel model = new SeleniumModel().setProperties(properties);
    Playwright playwright = mock(Playwright.class);
    Browser browser = mock(Browser.class);
    org.mockito.Mockito.when(browser.newContext(org.mockito.ArgumentMatchers.any(Browser.NewContextOptions.class)))
      .thenThrow(new PlaywrightException("cannot create the context"));
    PlaywrightBrowserDriverFactory factory = new PlaywrightBrowserDriverFactory() {
      @Override
      protected Playwright createPlaywright() {
        return playwright;
      }

      @Override
      protected Browser launch(Playwright started, Engine engine, boolean headless, Boolean noSandbox) {
        return browser;
      }
    };

    assertThatThrownBy(() -> factory.create(model, "test")).isInstanceOf(PlaywrightException.class);

    try (java.util.stream.Stream<java.nio.file.Path> left = java.nio.file.Files.list(evidence)) {
      assertThat(left).isEmpty();
    }
  }

  @Test
  void theVideoIsRecordedAtHalfTheSizeOfTheWindow(@org.junit.jupiter.api.io.TempDir java.nio.file.Path evidence) {
    // Encoding the video of a full size window made the Chromium suites about 50% slower on the CI runners
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setBrowser(BrowserType.HEADLESS_CHROME);
    properties.setBrowserWidth(1280);
    properties.setBrowserHeight(1024);
    properties.setScreenshotPath(evidence.toString());
    SeleniumModel model = new SeleniumModel().setProperties(properties);
    Browser browser = mock(Browser.class);
    org.mockito.ArgumentCaptor<Browser.NewContextOptions> options = org.mockito.ArgumentCaptor.forClass(Browser.NewContextOptions.class);
    org.mockito.Mockito.when(browser.newContext(options.capture())).thenThrow(new PlaywrightException("stop after the options"));
    PlaywrightBrowserDriverFactory factory = new PlaywrightBrowserDriverFactory() {
      @Override
      protected Playwright createPlaywright() {
        return mock(Playwright.class);
      }

      @Override
      protected Browser launch(Playwright started, Engine engine, boolean headless, Boolean noSandbox) {
        return browser;
      }
    };

    assertThatThrownBy(() -> factory.create(model, "test")).isInstanceOf(PlaywrightException.class);

    assertThat(options.getValue().viewportSize.get().width).isEqualTo(1280);
    assertThat(options.getValue().viewportSize.get().height).isEqualTo(1024);
    assertThat(options.getValue().recordVideoSize.width).isEqualTo(640);
    assertThat(options.getValue().recordVideoSize.height).isEqualTo(512);
  }

  @Test
  void aBrowserThatCannotBeLaunchedDoesNotLeaveThePlaywrightDriverRunning() {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setBrowser(BrowserType.HEADLESS_CHROME);
    SeleniumModel model = new SeleniumModel().setProperties(properties);
    Playwright playwright = mock(Playwright.class);
    PlaywrightBrowserDriverFactory factory = new PlaywrightBrowserDriverFactory() {
      @Override
      protected Playwright createPlaywright() {
        return playwright;
      }

      @Override
      protected Browser launch(Playwright started, Engine engine, boolean headless, Boolean noSandbox) {
        throw new PlaywrightException("cannot launch");
      }
    };

    assertThatThrownBy(() -> factory.create(model, "test")).isInstanceOf(PlaywrightException.class).hasMessageContaining("cannot launch");

    verify(playwright).close();
    assertThat(model.getBrowser()).isNull();
  }
}
