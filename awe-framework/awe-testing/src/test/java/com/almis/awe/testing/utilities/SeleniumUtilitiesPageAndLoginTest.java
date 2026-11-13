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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Page loading and login helpers of the utilities, that reach the browser through the driver port: opening the page,
 * typing straight into an element, and the "any visible match" checks that the login and the shell wait for
 */
class SeleniumUtilitiesPageAndLoginTest {

  private static final String READY_STATE = "return document.readyState";

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private WebDriver.Timeouts timeouts;
  private SeleniumModel model;
  private SeleniumUtilities utilities;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class, Interactive.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));
    WebDriver.Options options = mock(WebDriver.Options.class);
    timeouts = mock(WebDriver.Timeouts.class);
    when(driver.manage()).thenReturn(options);
    when(options.timeouts()).thenReturn(timeouts);

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(1500));
    model = new SeleniumModel().setDriver(driver).setCurrentOption("unit-test").setProperties(properties);

    AngularAweInstructions instructions = new AngularAweInstructions();
    instructions.setSeleniumModel(model);
    utilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", instructions);
  }

  private WebElement element(boolean displayed, boolean enabled) {
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(displayed);
    when(element.isEnabled()).thenReturn(enabled);
    return element;
  }

  private void matches(By selector, WebElement... elements) {
    when(driver.findElements(selector)).thenReturn(List.of(elements));
    if (elements.length > 0) {
      doReturn(elements[0]).when(driver).findElement(selector);
    }
  }

  private boolean hasVisibleElement(By selector) {
    return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(SeleniumUtilities.class, "hasVisibleElement",
      model.getBrowser(), selector));
  }

  private boolean visibleControlReady(By selector) {
    return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(SeleniumUtilities.class, "visibleControlReady",
      model.getBrowser(), selector));
  }

  @Test
  void shouldOpenThePageAndWaitUntilItHasLoaded() {
    when(((JavascriptExecutor) driver).executeScript(READY_STATE)).thenReturn("loading", "interactive", "complete");

    utilities.goToUrl("http://localhost:8080/awe");

    verify(timeouts).scriptTimeout(Duration.ofMillis(1500));
    verify(driver).get("http://localhost:8080/awe");
    assertThat(model.getCurrentOption()).isEqualTo("login");
  }

  @Test
  void shouldFailWhenThePageNeverFinishesLoading() {
    when(((JavascriptExecutor) driver).executeScript(READY_STATE)).thenReturn("loading");
    model.getProperties().setTimeout(Duration.ofMillis(150));

    assertThatThrownBy(() -> utilities.goToUrl("http://localhost:8080/"))
      .isInstanceOf(AssertionFailedError.class).hasMessageContaining("page to finish loading");
  }

  @Test
  void shouldTypeStraightIntoTheElementWithoutScrollingOrPausing() {
    By fileInput = By.cssSelector("input[type='file']");
    WebElement input = element(true, true);
    matches(fileInput, input);

    utilities.writeTextOnDriver(fileInput, "/tmp/", "file.txt");

    verify(input).sendKeys("/tmp/", "file.txt");
    verify((Interactive) driver, never()).perform(any());
    verify((JavascriptExecutor) driver, never()).executeScript(anyString(), any(Object[].class));
  }

  @Test
  void shouldHaveAVisibleElementWhenAnyMatchIsDisplayed() {
    By selector = By.cssSelector(".item");
    matches(selector, element(false, true), element(true, false));

    assertThat(hasVisibleElement(selector)).isTrue();
  }

  @Test
  void shouldNotHaveAVisibleElementWhenNoneIsDisplayedOrThereIsNone() {
    By selector = By.cssSelector(".item");
    assertThat(hasVisibleElement(selector)).isFalse();

    matches(selector, element(false, true), element(false, true));

    assertThat(hasVisibleElement(selector)).isFalse();
  }

  @Test
  void shouldTakeAReplacedMatchAsVisibleWhileLookingForAVisibleOne() {
    By selector = By.cssSelector(".item");
    WebElement replaced = mock(WebElement.class);
    when(replaced.isDisplayed()).thenThrow(new StaleElementReferenceException("replaced"));
    matches(selector, replaced);

    assertThat(hasVisibleElement(selector)).isTrue();
  }

  @Test
  void shouldHaveAControlReadyWhenItIsVisibleAndEveryVisibleOneIsEnabled() {
    By selector = By.cssSelector(".control");
    matches(selector, element(false, false), element(true, true), element(true, true));

    assertThat(visibleControlReady(selector)).isTrue();
  }

  @Test
  void shouldNotHaveAControlReadyWhenAVisibleOneIsDisabledOrNoneIsVisibleOrOneWasReplaced() {
    By selector = By.cssSelector(".control");
    assertThat(visibleControlReady(selector)).isFalse();

    matches(selector, element(false, true));
    assertThat(visibleControlReady(selector)).isFalse();

    matches(selector, element(true, true), element(true, false));
    assertThat(visibleControlReady(selector)).isFalse();

    WebElement replaced = mock(WebElement.class);
    when(replaced.isDisplayed()).thenThrow(new StaleElementReferenceException("replaced"));
    matches(selector, replaced);
    assertThat(visibleControlReady(selector)).isFalse();
  }

  @Test
  void shouldTellWhetherAFieldOfTheLoginFormHoldsWhatWasTyped() {
    AngularAweInstructions angular = new AngularAweInstructions();
    angular.setSeleniumModel(model);
    By input = angular.getCriterionInput(angular.getCriterionCss("cod_usr"));
    // A field that cannot be read is not filled again
    assertThat(fieldHolds("cod_usr", "test")).isTrue();

    WebElement field = element(true, true);
    when(field.getAttribute("value")).thenReturn("", "test");
    matches(input, field);

    assertThat(fieldHolds("cod_usr", "test")).isFalse();
    assertThat(fieldHolds("cod_usr", "test")).isTrue();
  }

  @Test
  void shouldNotFillAgainALoginFieldThatLeftThePageWhileBeingRead() {
    By input = loginInput("cod_usr");
    WebElement field = element(true, true);
    // Listed, then gone when its value is read: the login has moved on
    when(driver.findElements(input)).thenReturn(List.of(field));

    assertThat(fieldHolds("cod_usr", "test")).isTrue();
  }

  @Test
  void shouldFillAgainALoginFieldThatWasReplacedWhileBeingRead() {
    By input = loginInput("cod_usr");
    WebElement field = element(true, true);
    // The form was drawn again while its value was read: a new form comes back empty
    when(field.getAttribute("value")).thenThrow(new StaleElementReferenceException("replaced"));
    matches(input, field);

    assertThat(fieldHolds("cod_usr", "test")).isFalse();
  }

  private By loginInput(String criterion) {
    AngularAweInstructions angular = new AngularAweInstructions();
    angular.setSeleniumModel(model);
    return angular.getCriterionInput(angular.getCriterionCss(criterion));
  }

  private boolean fieldHolds(String criterion, String expected) {
    return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(utilities, "loginFieldHolds", criterion, expected));
  }
}
