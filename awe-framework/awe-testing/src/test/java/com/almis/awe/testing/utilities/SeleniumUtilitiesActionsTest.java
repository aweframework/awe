package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Actions of the utilities: the gestures that reach the browser through the driver port, whatever the number of calls
 * that the port needs to send them
 */
class SeleniumUtilitiesActionsTest {

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private SeleniumUtilities utilities;
  private IAweFrontEndInstructions instructions;

  @BeforeEach
  void setUp() throws Exception {
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

    use(new AngularAweInstructions(), properties, model);
  }

  private void use(IAweFrontEndInstructions frontEndInstructions, AweTestConfigProperties properties, SeleniumModel model) {
    frontEndInstructions.setSeleniumModel(model);
    instructions = frontEndInstructions;
    utilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", frontEndInstructions);
  }

  private WebElement show(By selector) {
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(true);
    when(driver.findElement(argThat(selector::equals))).thenReturn(element);
    when(driver.findElements(argThat(selector::equals))).thenReturn(List.of(element));
    return element;
  }

  /**
   * Every action that was sent to the browser, in order, whatever the number of times it was performed
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> performedActions() {
    ArgumentCaptor<Collection<Sequence>> captor = ArgumentCaptor.forClass(Collection.class);
    verify((Interactive) driver, atLeast(0)).perform(captor.capture());
    return captor.getAllValues().stream()
      .flatMap(Collection::stream)
      .flatMap(sequence -> ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream())
      .toList();
  }

  private List<Map<String, Object>> pointerMoves() {
    return performedActions().stream().filter(action -> "pointerMove".equals(action.get("type"))).toList();
  }

  private static boolean movesBy(Map<String, Object> action, int x, int y) {
    return "pointer".equals(action.get("origin")) && ((Number) action.get("x")).intValue() == x
      && ((Number) action.get("y")).intValue() == y;
  }

  @Test
  void shouldClickAgainWhenTheElementWasReplacedWhileBeingClicked() {
    By button = By.cssSelector("#replaced-button");
    show(button);
    // The first click finds a replaced element, the second one reaches the new one
    doThrow(new StaleElementReferenceException("replaced")).doNothing().when((Interactive) driver).perform(any());

    ReflectionTestUtils.invokeMethod(utilities, "click", button);

    verify((Interactive) driver, times(2)).perform(any());
  }

  @Test
  void shouldFailAfterTheRetriesWhenTheElementIsReplacedOnEveryClick() {
    By button = By.cssSelector("#replaced-button");
    show(button);
    doThrow(new StaleElementReferenceException("replaced")).when((Interactive) driver).perform(any());

    assertThrows(AssertionFailedError.class, () -> ReflectionTestUtils.invokeMethod(utilities, "click", button));

    verify((Interactive) driver, times(3)).perform(any());
  }

  @Test
  void shouldMoveTheMouseAwayFromAHelpPopoverUntilItIsGone() {
    By popover = instructions.getPopover();
    WebElement help = mock(WebElement.class);
    when(driver.findElement(argThat(popover::equals))).thenReturn(help);
    AtomicInteger checks = new AtomicInteger();
    // The popover stays for two moves
    when(driver.findElements(argThat(popover::equals)))
      .thenAnswer(invocation -> checks.incrementAndGet() <= 2 ? List.of(help) : Collections.emptyList());

    ReflectionTestUtils.invokeMethod(utilities, "moveMouse");

    // Over the popover and 30 pixels down from it, as many times as it takes to leave
    List<Map<String, Object>> moves = pointerMoves();
    assertThat(moves).hasSize(4);
    assertThat(movesBy(moves.get(0), 0, 0)).isFalse();
    assertThat(movesBy(moves.get(1), 0, 30)).isTrue();
    assertThat(movesBy(moves.get(2), 0, 0)).isFalse();
    assertThat(movesBy(moves.get(3), 0, 30)).isTrue();
  }

  @Test
  void shouldNotMoveTheMouseWhenNoPopoverIsShown() {
    ReflectionTestUtils.invokeMethod(utilities, "moveMouse");

    verify((Interactive) driver, never()).perform(any());
  }

  @Test
  void shouldNotFailWhenTheMouseCannotBeMovedAwayFromThePopover() {
    By popover = instructions.getPopover();
    show(popover);
    doThrow(new StaleElementReferenceException("replaced")).when((Interactive) driver).perform(any());

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(utilities, "moveMouse")).doesNotThrowAnyException();
  }

  @Test
  void shouldMoveUpAndClickOutOfACriterion() {
    ReflectionTestUtils.invokeMethod(utilities, "moveMouseOutOfCriterion");

    List<Map<String, Object>> actions = performedActions();
    List<Object> types = actions.stream().map(action -> action.get("type")).toList();
    assertThat(actions).filteredOn(action -> "pointerMove".equals(action.get("type"))).singleElement()
      .matches(action -> movesBy(action, 0, -30));
    assertThat(types.indexOf("pointerMove")).isLessThan(types.indexOf("pointerDown"));
    assertThat(types).contains("pointerUp");
  }

  @Test
  void shouldMoveOverAnElementOnceItIsVisibleWithoutClickingIt() {
    By option = By.cssSelector("#option");
    show(option);

    ReflectionTestUtils.invokeMethod(utilities, "moveTo", option);

    List<Map<String, Object>> actions = performedActions();
    assertThat(actions).filteredOn(action -> "pointerMove".equals(action.get("type"))).hasSize(1);
    assertThat(actions).noneMatch(action -> "pointerDown".equals(action.get("type")));
  }

  @Test
  void shouldFailWhenTheElementToMoveOverIsReplaced() {
    By option = By.cssSelector("#option");
    show(option);
    doThrow(new StaleElementReferenceException("replaced")).when((Interactive) driver).perform(any());

    assertThrows(AssertionFailedError.class, () -> ReflectionTestUtils.invokeMethod(utilities, "moveTo", option));
  }

  @Test
  void shouldClearTheTextOfACriterionAndWaitForItToBeEmpty() {
    By input = By.cssSelector("#criterion");
    WebElement element = show(input);
    when(element.getAttribute("value")).thenReturn("abc", "abc", "");

    ReflectionTestUtils.invokeMethod(utilities, "clearText", input);

    verify(element).clear();
    List<Object> keys = mockingDetails(element).getInvocations().stream()
      .filter(invocation -> "sendKeys".equals(invocation.getMethod().getName()))
      .flatMap(invocation -> java.util.Arrays.stream(invocation.getArguments()))
      .toList();
    // One backspace more than the characters, as the front end tests have always sent
    assertThat(keys).hasSize(4).allMatch(key -> key.toString().equals(Keys.BACK_SPACE.toString()));
  }

  @Test
  void shouldNotTouchACriterionThatHasNoText() {
    By input = By.cssSelector("#criterion");
    WebElement element = show(input);
    when(element.getAttribute("value")).thenReturn("");

    ReflectionTestUtils.invokeMethod(utilities, "clearText", input);

    verify(element, never()).clear();
  }

  @Test
  void shouldScrollTheViewportOfAGridToAPosition() {
    use(new ReactAweInstructions(), (AweTestConfigProperties) ReflectionTestUtils.getField(utilities, "properties"),
      (SeleniumModel) ReflectionTestUtils.getField(utilities, "seleniumModel"));
    WebElement zone = show(instructions.getGridScrollZone("Grd"));

    utilities.scrollGrid("Grd", 15, 40);

    verify((JavascriptExecutor) driver).executeScript(contains("scrollTo"), eq(zone), eq(15), eq(40));
  }

  @Test
  void shouldFailWithANeutralExceptionWhenTheGridToScrollIsNotThere() {
    assertThatThrownBy(() -> utilities.scrollGrid("Grd", 0, 40)).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldPauseTheBrowserActions() {
    utilities.pause(250);

    assertThat(performedActions()).anyMatch(action -> "pause".equals(action.get("type"))
      && ((Number) action.get("duration")).longValue() == 250L);
  }

  @Test
  void shouldCloseTheContextMenuWithTheEscapeKeyWhenThereIsNoMaskToClick() {
    use(new ReactAweInstructions(), (AweTestConfigProperties) ReflectionTestUtils.getField(utilities, "properties"),
      (SeleniumModel) ReflectionTestUtils.getField(utilities, "seleniumModel"));

    utilities.closeContextMenu();

    assertThat(performedActions()).extracting(action -> action.get("type")).contains("keyDown", "keyUp");
  }

  @Test
  void shouldFailTypingInAnElementThatIsNotThere() {
    assertThrows(AssertionFailedError.class, () -> utilities.writeText(By.cssSelector("#missing"), "text"));
  }

  @Test
  void shouldWaitForAnElementToBeThereToDoubleClickIt() {
    By cell = By.cssSelector("#cell");
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(true);
    AtomicInteger lookups = new AtomicInteger();
    // The element is drawn after the first check of the wait
    when(driver.findElement(argThat(cell::equals))).thenAnswer(invocation -> {
      if (lookups.incrementAndGet() < 2) {
        throw new NoSuchElementException("not drawn yet");
      }
      return element;
    });

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(utilities, "doubleClick", cell)).doesNotThrowAnyException();

    assertThat(performedActions()).filteredOn(action -> "pointerDown".equals(action.get("type"))).hasSize(2);
  }
}
