package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.Locator;
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Steps that take a tool-neutral {@link Locator}, and the deprecated {@link By} overloads that must keep behaving as the
 * neutral ones: the outcome of a step is the same whichever type the selector is given as
 */
@SuppressWarnings("deprecation")
class SeleniumUtilitiesLocatorStepsTest {

  private static final Locator ITEM = Locator.css("#item");
  private static final By ITEM_BY = By.cssSelector("#item");

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private CustomSteps steps;

  /**
   * A step of a product: it extends the façade and works with locators and the browser only
   */
  static class CustomSteps extends SeleniumUtilities {
    boolean isPresentOnPage(Locator locator) {
      return getBrowser().exists(locator);
    }

    String textOf(String criterionCss) {
      Locator locator = waitForCssLocator(criterionCss);
      return getBrowser().text(locator);
    }
  }

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class, Interactive.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(150));
    SeleniumModel model = new SeleniumModel().setDriver(driver).setCurrentOption("unit-test").setProperties(properties);

    AngularAweInstructions instructions = new AngularAweInstructions();
    instructions.setSeleniumModel(model);
    steps = new CustomSteps();
    ReflectionTestUtils.setField(steps, "properties", properties);
    ReflectionTestUtils.setField(steps, "seleniumModel", model);
    ReflectionTestUtils.setField(steps, "frontEndInstructions", instructions);
  }

  private WebElement show(By selector, boolean displayed, String text, String value) {
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(displayed);
    when(element.isEnabled()).thenReturn(true);
    when(element.getText()).thenReturn(text);
    when(element.getAttribute("value")).thenReturn(value);
    when(driver.findElement(argThat(selector::equals))).thenReturn(element);
    when(driver.findElements(argThat(selector::equals))).thenReturn(List.of(element));
    return element;
  }

  /**
   * Run a step and describe how it ended: no failure, or the type and message of the failure
   */
  private static String outcome(Runnable step) {
    try {
      step.run();
      return "ok";
    } catch (AssertionFailedError exc) {
      return "failed: " + exc.getMessage().lines().findFirst().orElse("");
    }
  }

  @Test
  void shouldWaitForTheTextOfALocator() {
    show(ITEM_BY, true, "Hello world", "");

    assertThatCode(() -> steps.waitForText(ITEM, "world")).doesNotThrowAnyException();
    assertThatThrownBy(() -> steps.waitForText(ITEM, "bye")).isInstanceOf(AssertionFailedError.class)
      .hasMessageContaining("text 'bye' to be present in element located by css=#item");
  }

  @Test
  void shouldWaitForTheValueOfALocatorAndForItToGoAway() {
    show(ITEM_BY, true, "", "abc");

    assertThatCode(() -> steps.waitForValue(ITEM, "bc")).doesNotThrowAnyException();
    assertThatThrownBy(() -> steps.waitForValue(ITEM, "zz")).isInstanceOf(AssertionFailedError.class);
    assertThatCode(() -> steps.waitForEmptyText(ITEM, "zz")).doesNotThrowAnyException();
    assertThatThrownBy(() -> steps.waitForEmptyText(ITEM, "bc")).isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void shouldCheckTheVisibilityOfALocator() {
    show(ITEM_BY, true, "", "");

    assertThatCode(() -> steps.checkVisible(ITEM)).doesNotThrowAnyException();
    assertThatThrownBy(() -> steps.checkNotVisible(ITEM)).isInstanceOf(AssertionFailedError.class);
    assertThatCode(() -> steps.checkNotVisible(Locator.css("#gone"))).doesNotThrowAnyException();
    assertThatThrownBy(() -> steps.checkVisible(Locator.css("#gone"))).isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void shouldTypeInALocator() {
    WebElement input = show(ITEM_BY, true, "", "");

    steps.writeText(ITEM, "typed");
    steps.writeTextOnDriver(ITEM, "sent", " keys");

    verify((Interactive) driver, atLeastOnce()).perform(any());
    verify(input).sendKeys("sent", " keys");
  }

  @Test
  void shouldFailTypingInALocatorThatIsNotThere() {
    assertThatThrownBy(() -> steps.writeText(Locator.css("#missing"), "text")).isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void shouldWaitForACssSelectorAndReturnItAsALocator() {
    show(By.cssSelector(".title"), true, "Title", "");

    Locator located = steps.waitForCssLocator(".title");

    assertThat(located).isEqualTo(Locator.css(".title"));
    assertThatThrownBy(() -> steps.waitForCssLocator(".missing")).isInstanceOf(AssertionFailedError.class);
  }

  @Test
  void shouldKeepReturningASeleniumLocatorFromTheDeprecatedCssSelectorStep() {
    show(By.cssSelector(".title"), true, "Title", "");

    assertThat(steps.waitForCssSelector(".title")).isEqualTo(By.cssSelector(".title"));
  }

  @Test
  void shouldFinishTheDeprecatedSeleniumOverloadsAsTheNeutralOnesDo() {
    show(ITEM_BY, true, "Hello world", "abc");

    assertThat(outcome(() -> steps.waitForText(ITEM_BY, "world"))).isEqualTo(outcome(() -> steps.waitForText(ITEM, "world"))).isEqualTo("ok");
    assertThat(outcome(() -> steps.waitForValue(ITEM_BY, "abc"))).isEqualTo(outcome(() -> steps.waitForValue(ITEM, "abc"))).isEqualTo("ok");
    assertThat(outcome(() -> steps.waitForEmptyText(ITEM_BY, "zz"))).isEqualTo(outcome(() -> steps.waitForEmptyText(ITEM, "zz"))).isEqualTo("ok");
    assertThat(outcome(() -> steps.checkVisible(ITEM_BY))).isEqualTo(outcome(() -> steps.checkVisible(ITEM))).isEqualTo("ok");
    assertThat(outcome(() -> steps.checkNotVisible(Locator.css("#gone").toBy()))).isEqualTo(outcome(() -> steps.checkNotVisible(Locator.css("#gone")))).isEqualTo("ok");

    String failureByText = outcome(() -> steps.waitForText(ITEM_BY, "bye"));
    assertThat(failureByText).startsWith("failed: ").isEqualTo(outcome(() -> steps.waitForText(ITEM, "bye")));
    String failureByVisibility = outcome(() -> steps.checkNotVisible(ITEM_BY));
    assertThat(failureByVisibility).startsWith("failed: ").isEqualTo(outcome(() -> steps.checkNotVisible(ITEM)));
  }

  @Test
  void shouldTypeWithTheDeprecatedSeleniumOverloadsAsWithTheNeutralOnes() {
    WebElement input = show(ITEM_BY, true, "", "");

    steps.writeTextOnDriver(ITEM_BY, "by");
    steps.writeTextOnDriver(ITEM, "locator");
    steps.writeText(ITEM_BY, "by");

    verify(input).sendKeys("by");
    verify(input).sendKeys("locator");
    assertThat(outcome(() -> steps.writeText(By.cssSelector("#missing"), "x")))
      .startsWith("failed: ").isEqualTo(outcome(() -> steps.writeText(Locator.css("#missing"), "x")));
  }

  @Test
  void shouldLetAProductStepUseTheBrowserAndLocatorsWithoutSeleniumTypes() {
    show(By.cssSelector(".title"), true, "Title", "");

    assertThat(steps.isPresentOnPage(Locator.css(".title"))).isTrue();
    assertThat(steps.isPresentOnPage(Locator.css(".other"))).isFalse();
    assertThat(steps.textOf(".title")).isEqualTo("Title");
    assertThat(steps.getBrowser()).isInstanceOf(BrowserDriver.class);
  }
}
