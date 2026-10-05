package com.almis.awe.testing.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Actions of the Selenium adapter of the driver port: the gestures sent to the browser and the scripts that precede them.
 */
class SeleniumBrowserDriverActionsTest {

  private static final Locator ITEM = Locator.css(".item");

  private WebDriver driver;
  private WebElement element;
  private SeleniumBrowserDriver browser;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class, Interactive.class));
    element = mock(WebElement.class);
    when(driver.findElement(ITEM.toBy())).thenReturn(element);
    browser = new SeleniumBrowserDriver(driver);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> performedActions() {
    ArgumentCaptor<Collection<Sequence>> captor = ArgumentCaptor.forClass(Collection.class);
    verify((Interactive) driver, times(1)).perform(captor.capture());
    return captor.getValue().stream()
      .flatMap(sequence -> ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream())
      .toList();
  }

  private List<Object> types(List<Map<String, Object>> actions) {
    return actions.stream().map(action -> action.get("type")).toList();
  }

  private boolean pausedFor(List<Map<String, Object>> actions, long millis) {
    return actions.stream().anyMatch(action -> "pause".equals(action.get("type"))
      && Long.valueOf(millis).equals(((Number) action.get("duration")).longValue()));
  }

  @Test
  void shouldClickAwayFromTheViewportEdgeAndLetTheClickSettle() {
    browser.click(ITEM);

    verify((JavascriptExecutor) driver).executeScript(contains("rect.top < 60"), eq(element));
    List<Map<String, Object>> actions = performedActions();
    assertThat(types(actions)).contains("pointerDown", "pointerUp");
    assertThat(pausedFor(actions, 100)).isTrue();
  }

  @Test
  void shouldClickEvenWhenTheScrollScriptFails() {
    doThrow(new IllegalStateException("no script")).when((JavascriptExecutor) driver).executeScript(contains("rect.top"), eq(element));

    browser.click(ITEM);

    assertThat(types(performedActions())).contains("pointerDown");
  }

  @Test
  void shouldDoubleClickInOneGestureAfterScrollingIntoItsContainer() {
    browser.doubleClick(ITEM);

    verify((JavascriptExecutor) driver).executeScript(contains("behavior: 'instant'"), eq(element));
    List<Map<String, Object>> actions = performedActions();
    assertThat(types(actions).stream().filter("pointerDown"::equals)).hasSize(2);
    assertThat(pausedFor(actions, 100)).isTrue();
  }

  @Test
  void shouldContextClickWithTheSecondButton() {
    browser.contextClick(ITEM);

    verify((JavascriptExecutor) driver).executeScript(contains("behavior: 'instant'"), eq(element));
    List<Map<String, Object>> actions = performedActions();
    assertThat(actions).anyMatch(action -> "pointerDown".equals(action.get("type")) && Integer.valueOf(2).equals(action.get("button")));
  }

  @Test
  void shouldHoverWithoutClicking() {
    browser.hover(ITEM);

    List<Map<String, Object>> actions = performedActions();
    assertThat(types(actions)).contains("pointerMove").doesNotContain("pointerDown");
    assertThat(pausedFor(actions, 100)).isTrue();
  }

  @Test
  void shouldHoverInOneJumpThroughTheInteractiveDriver() {
    browser.hoverInstantly(ITEM);

    List<Map<String, Object>> actions = performedActions();
    assertThat(actions).hasSize(1).allMatch(action -> "pointerMove".equals(action.get("type"))
      && ((Number) action.get("duration")).longValue() == 0L);
  }

  @Test
  void shouldMoveThePointerByAnOffsetAndClickWhereItIs() {
    browser.moveMouseBy(5, -30);

    List<Map<String, Object>> moved = performedActions();
    assertThat(moved).anyMatch(action -> "pointerMove".equals(action.get("type"))
      && ((Number) action.get("x")).intValue() == 5 && ((Number) action.get("y")).intValue() == -30);
  }

  @Test
  void shouldClickWhereThePointerIs() {
    browser.clickAtPointer();

    List<Map<String, Object>> actions = performedActions();
    assertThat(types(actions)).contains("pointerDown", "pointerUp");
    assertThat(pausedFor(actions, 100)).isTrue();
  }

  @Test
  void shouldTypeAfterScrollingTheElementIntoView() {
    browser.type(ITEM, "hello");

    verify((JavascriptExecutor) driver).executeScript(contains("block: 'nearest'"), eq(element));
    List<Map<String, Object>> actions = performedActions();
    assertThat(types(actions)).contains("keyDown", "keyUp");
    assertThat(pausedFor(actions, 200)).isTrue();
  }

  @Test
  void shouldClearTheValueAndSendOneBackspaceMoreThanItsCharacters() {
    when(element.getAttribute("value")).thenReturn("abc");

    browser.clear(ITEM);

    verify(element).clear();
    List<Object> keys = mockingDetails(element).getInvocations().stream()
      .filter(invocation -> "sendKeys".equals(invocation.getMethod().getName()))
      .flatMap(invocation -> Arrays.stream(invocation.getArguments()))
      .toList();
    assertThat(keys).hasSize(4).allMatch(key -> key.toString().equals(Keys.BACK_SPACE.toString()));
  }

  @Test
  void shouldNotClearAnEmptyValue() {
    when(element.getAttribute("value")).thenReturn("");

    browser.clear(ITEM);

    verify(element, never()).clear();
  }

  @Test
  void shouldPressAKeyOnThePageAndOnAnElement() {
    browser.press(Key.ESCAPE);

    assertThat(types(performedActions())).contains("keyDown", "keyUp");
  }

  @Test
  void shouldPressAKeyOnAnElement() {
    browser.press(ITEM, Key.ENTER);

    assertThat(types(performedActions())).contains("keyDown", "keyUp");
  }

  @Test
  void shouldPause() {
    browser.pause(Duration.ofMillis(250));

    assertThat(pausedFor(performedActions(), 250)).isTrue();
  }

  @Test
  void shouldMapAStaleOrMovedElementOfAnActionToAReplacedElement() {
    doThrow(new StaleElementReferenceException("replaced")).when((Interactive) driver).perform(any());

    assertThatThrownBy(() -> browser.click(ITEM)).isInstanceOf(ElementReplacedException.class);

    doThrow(new MoveTargetOutOfBoundsException("origin is not displayed")).when((Interactive) driver).perform(any());

    assertThatThrownBy(() -> browser.hover(ITEM)).isInstanceOf(ElementReplacedException.class);
  }

  @Test
  void shouldFailAnActionOnAMissingElementWithANeutralException() {
    doThrow(new NoSuchElementException("missing")).when(driver).findElement(ITEM.toBy());

    assertThatThrownBy(() -> browser.doubleClick(ITEM)).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldNotNeedAScriptEngineToAct() {
    WebDriver plain = mock(WebDriver.class, withSettings().extraInterfaces(Interactive.class));
    when(plain.findElement(ITEM.toBy())).thenReturn(element);

    new SeleniumBrowserDriver(plain).click(ITEM);

    verify((Interactive) plain).perform(any());
  }

  @Test
  void shouldLeaveItToActionsWhenTheDriverIsNotInteractive() {
    WebDriver plain = mock(WebDriver.class);
    when(plain.findElement(ITEM.toBy())).thenReturn(element);

    assertThatThrownBy(() -> new SeleniumBrowserDriver(plain).hoverInstantly(ITEM)).isInstanceOf(ClassCastException.class);
  }
}
