package com.almis.awe.testing.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Page, script, scroll and window operations of the Selenium adapter of the driver port.
 */
class SeleniumBrowserDriverPageTest {

  private static final Locator ITEM = Locator.css(".item");

  private WebDriver driver;
  private WebElement element;
  private WebDriver.Options options;
  private WebDriver.Window window;
  private SeleniumBrowserDriver browser;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
    element = mock(WebElement.class);
    options = mock(WebDriver.Options.class);
    window = mock(WebDriver.Window.class);
    when(driver.findElement(ITEM.toBy())).thenReturn(element);
    when(driver.manage()).thenReturn(options);
    when(options.window()).thenReturn(window);
    browser = new SeleniumBrowserDriver(driver);
  }

  @Test
  void shouldOpenAPage() {
    browser.open("http://localhost:8080/");

    verify(driver).get("http://localhost:8080/");
  }

  @Test
  void shouldTellWhenThePageHasLoaded() {
    when(((JavascriptExecutor) driver).executeScript("return document.readyState")).thenReturn("complete");

    assertThat(browser.isPageLoaded()).isTrue();
  }

  @Test
  void shouldTellThatAPageThatIsStillLoadingHasNotLoaded() {
    when(((JavascriptExecutor) driver).executeScript("return document.readyState")).thenReturn("interactive");

    assertThat(browser.isPageLoaded()).isFalse();
  }

  @Test
  void shouldSetTheScriptTimeout() {
    WebDriver.Timeouts timeouts = mock(WebDriver.Timeouts.class);
    when(options.timeouts()).thenReturn(timeouts);

    browser.setScriptTimeout(Duration.ofSeconds(7));

    verify(timeouts).scriptTimeout(Duration.ofSeconds(7));
  }

  @Test
  void shouldRunAScriptWithItsArguments() {
    when(((JavascriptExecutor) driver).executeScript("return arguments[0] + arguments[1]", 1, 2)).thenReturn(3L);

    assertThat(browser.executeScript("return arguments[0] + arguments[1]", 1, 2)).isEqualTo(3L);
  }

  @Test
  void shouldRunAScriptOnTheElementFirst() {
    browser.executeScriptOn(ITEM, "arguments[0].focus();", "extra");

    verify((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element, "extra");
  }

  @Test
  void shouldFailToRunAScriptOnAnElementThatIsNotThere() {
    when(driver.findElement(ITEM.toBy())).thenThrow(new NoSuchElementException("none"));

    assertThatThrownBy(() -> browser.executeScriptOn(ITEM, "arguments[0].focus();"))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldRefuseToRunScriptsWhenTheBrowserCannotScript() {
    SeleniumBrowserDriver plain = new SeleniumBrowserDriver(mock(WebDriver.class));

    assertThatThrownBy(() -> plain.executeScript("return 1"))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldScrollTheContentOfAnElement() {
    browser.scrollTo(ITEM, 120, 30);

    verify((JavascriptExecutor) driver).executeScript("arguments[0].scrollTo(arguments[1], arguments[2]);", element, 120, 30);
  }

  @Test
  void shouldBringAnElementToTheCenter() {
    browser.scrollToCenter(ITEM);

    verify((JavascriptExecutor) driver).executeScript(contains("block: 'center'"), eq(element));
  }

  @Test
  void shouldNotFailWhenTheScrollToTheCenterCannotRun() {
    doThrow(new IllegalStateException("script")).when((JavascriptExecutor) driver).executeScript(any(String.class), eq(element));

    browser.scrollToCenter(ITEM);

    verify((JavascriptExecutor) driver).executeScript(contains("block: 'center'"), eq(element));
  }

  @Test
  void shouldLetTheMissingElementOfTheScrollToTheCenterBeKnown() {
    when(driver.findElement(ITEM.toBy())).thenThrow(new NoSuchElementException("none"));

    assertThatThrownBy(() -> browser.scrollToCenter(ITEM)).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldResizeAndMoveTheWindow() {
    browser.setWindowSize(1280, 800);
    browser.setWindowPosition(10, 20);

    verify(window).setSize(new Dimension(1280, 800));
    verify(window).setPosition(new Point(10, 20));
  }

  @Test
  void shouldQuitTheBrowser() {
    browser.quit();

    verify(driver).quit();
  }
}
