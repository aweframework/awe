package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.Logs;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Failure evidence of the utilities (screenshot, page source and browser console), taken through the driver port. It
 * is a help to diagnose the failure: it never fails by itself and never replaces the failure that is being reported
 */
class SeleniumUtilitiesEvidenceTest {

  private static final String FAILURE = "Some failure";

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private WebDriver.Options options;
  private Logs logs;
  private SeleniumModel model;
  private SeleniumUtilities utilities;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));
    when(driver.getPageSource()).thenReturn("<html>failed page</html>");
    options = mock(WebDriver.Options.class);
    logs = mock(Logs.class);
    when(driver.manage()).thenReturn(options);
    when(options.logs()).thenReturn(logs);
    when(logs.get(LogType.BROWSER)).thenReturn(new LogEntries(List.of(new LogEntry(Level.SEVERE, 1000L, "crash"))));
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(150));
    model = new SeleniumModel().setDriver(driver).setCurrentOption("unit-test").setProperties(properties);

    AngularAweInstructions instructions = new AngularAweInstructions();
    instructions.setSeleniumModel(model);
    utilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", instructions);
  }

  private void fail(String message) {
    ReflectionTestUtils.invokeMethod(utilities, "assertWithScreenshot", message, false, new Throwable[0]);
  }

  private List<String> evidenceFiles() throws IOException {
    try (Stream<Path> files = Files.list(tempDir)) {
      return files.map(path -> path.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void shouldStoreTheScreenshotTheSourceAndTheConsoleOfAFailure() throws IOException {
    assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    assertThat(evidenceFiles()).hasSize(3)
      .anyMatch(name -> name.endsWith("some_failure.png"))
      .anyMatch(name -> name.endsWith("some_failure.html"))
      .anyMatch(name -> name.endsWith("some_failure.console.log"));
    assertThat(Files.readAllBytes(tempDir.resolve(evidenceFiles().stream().filter(name -> name.endsWith(".png")).findFirst()
      .orElseThrow()))).containsExactly(1, 2, 3);
    assertThat(model.isScreenshotTaken()).isTrue();
  }

  @Test
  void shouldReadTheDestructiveConsoleOnlyOnceForAFailure() {
    assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    verify(logs, times(1)).get(LogType.BROWSER);
  }

  @Test
  void shouldReportTheOriginalFailureWhenTheScreenshotFails() throws IOException {
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenThrow(new WebDriverException("session is gone"));

    AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    assertThat(error).hasMessageStartingWith(FAILURE);
    assertThat(model.isScreenshotTaken()).isFalse();
    // The source and the console are still stored
    assertThat(evidenceFiles()).anyMatch(name -> name.endsWith(".html")).anyMatch(name -> name.endsWith(".console.log"));
  }

  @Test
  void shouldReportTheOriginalFailureWhenTheBrowserCannotTakeScreenshots() {
    ReflectionTestUtils.setField(utilities, "seleniumModel", model.setDriver(mock(WebDriver.class)));

    AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    assertThat(error).hasMessageStartingWith(FAILURE);
    assertThat(model.isScreenshotTaken()).isFalse();
  }

  @Test
  void shouldReportTheOriginalFailureWhenThereIsNoBrowserAtAll() {
    ReflectionTestUtils.setField(utilities, "seleniumModel", model.setDriver(null));

    AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    assertThat(error).hasMessageStartingWith(FAILURE);
  }

  @Test
  void shouldReportTheOriginalFailureWhenThePageSourceAndTheConsoleFail() throws IOException {
    when(driver.getPageSource()).thenThrow(new WebDriverException("session is gone"));
    when(logs.get(LogType.BROWSER)).thenThrow(new WebDriverException("session is gone"));

    AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> fail(FAILURE));

    assertThat(error).hasMessageStartingWith(FAILURE);
    assertThat(evidenceFiles()).hasSize(1).allMatch(name -> name.endsWith(".png"));
  }

  @Test
  void shouldStoreNothingWhenTheConditionHolds() throws IOException {
    ReflectionTestUtils.invokeMethod(utilities, "assertWithScreenshot", FAILURE, true, new Throwable[0]);

    assertThat(evidenceFiles()).isEmpty();
    verify(((TakesScreenshot) driver), times(0)).getScreenshotAs(OutputType.BYTES);
  }
}
