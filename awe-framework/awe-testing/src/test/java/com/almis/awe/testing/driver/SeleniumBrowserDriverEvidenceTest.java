package com.almis.awe.testing.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.Logs;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Frames and failure evidence (screenshot, page source and browser console) of the Selenium adapter of the driver port.
 */
class SeleniumBrowserDriverEvidenceTest {

  private static final Locator FRAME = Locator.css("iframe");

  private WebDriver driver;
  private WebDriver.TargetLocator targetLocator;
  private WebElement frame;
  private SeleniumBrowserDriver browser;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    targetLocator = mock(WebDriver.TargetLocator.class);
    frame = mock(WebElement.class);
    when(driver.findElement(FRAME.toBy())).thenReturn(frame);
    when(driver.switchTo()).thenReturn(targetLocator);
    browser = new SeleniumBrowserDriver(driver);
  }

  @Test
  void shouldRunInsideAFrameAndComeBack() {
    AtomicInteger runs = new AtomicInteger();

    browser.inFrame(FRAME, () -> {
      verify(targetLocator).frame(frame);
      verify(targetLocator, never()).defaultContent();
      runs.incrementAndGet();
    });

    assertThat(runs).hasValue(1);
    verify(targetLocator).defaultContent();
  }

  @Test
  void shouldComeBackFromTheFrameWhenItFails() {
    IllegalStateException failure = new IllegalStateException("inside");

    assertThatThrownBy(() -> browser.inFrame(FRAME, () -> {
      throw failure;
    })).isSameAs(failure);

    verify(targetLocator).frame(frame);
    verify(targetLocator).defaultContent();
  }

  @Test
  void shouldNotSwitchWhenTheFrameIsNotThere() {
    when(driver.findElement(FRAME.toBy())).thenThrow(new NoSuchElementException("none"));
    Runnable body = () -> {
      throw new IllegalStateException("must not run");
    };

    assertThatThrownBy(() -> browser.inFrame(FRAME, body)).isInstanceOf(ElementNotFoundException.class);

    verify(driver, never()).switchTo();
  }

  @Test
  void shouldTakeAScreenshot() {
    byte[] png = {1, 2, 3};
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(png);

    assertThat(browser.screenshot()).hasValue(png);
  }

  @Test
  void shouldHaveNoScreenshotWhenTheBrowserCannotTakeIt() {
    SeleniumBrowserDriver plain = new SeleniumBrowserDriver(mock(WebDriver.class));

    assertThat(plain.screenshot()).isEqualTo(Optional.empty());
  }

  @Test
  void shouldReadThePageSource() {
    when(driver.getPageSource()).thenReturn("<html></html>");

    assertThat(browser.pageSource()).isEqualTo("<html></html>");
  }

  @Test
  void shouldReadTheBrowserConsole() {
    WebDriver.Options options = mock(WebDriver.Options.class);
    Logs logs = mock(Logs.class);
    when(driver.manage()).thenReturn(options);
    when(options.logs()).thenReturn(logs);
    when(logs.get(LogType.BROWSER)).thenReturn(new LogEntries(List.of(
      new LogEntry(Level.SEVERE, 1000L, "crash"), new LogEntry(Level.INFO, 2000L, "hello"))));

    List<ConsoleEntry> entries = browser.consoleEntries();

    assertThat(entries).extracting(ConsoleEntry::level).containsExactly("SEVERE", "INFO");
    assertThat(entries).extracting(ConsoleEntry::message).containsExactly("crash", "hello");
    assertThat(entries).extracting(ConsoleEntry::isSevere).containsExactly(true, false);
    assertThat(entries.get(0)).hasToString(new LogEntry(Level.SEVERE, 1000L, "crash").toString());
  }

  @Test
  void shouldHaveAnEmptyConsoleWhenTheBrowserDoesNotExposeIt() {
    WebDriver.Options options = mock(WebDriver.Options.class);
    Logs logs = mock(Logs.class);
    when(driver.manage()).thenReturn(options);
    when(options.logs()).thenReturn(logs);
    when(logs.get(LogType.BROWSER)).thenThrow(new UnsupportedOperationException("no logs"));

    assertThat(browser.consoleEntries()).isEmpty();
  }
}
