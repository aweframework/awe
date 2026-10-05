package com.almis.awe.testing.driver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Selenium adapter of the {@link BrowserDriver} port. The gestures are the ones that {@code SeleniumUtilities} has always
 * sent (and that fixed the flakiness of the browser tests): the pauses, the scrolls that precede an action, the double
 * click as a single gesture and the pointer that jumps to an element. It maps the Selenium exceptions to the neutral ones:
 * a missing element to {@link ElementNotFoundException}; a stale element, or the move target that Firefox reports as out
 * of bounds when the element was replaced while the action ran, to {@link ElementReplacedException}.
 */
@Slf4j
public class SeleniumBrowserDriver implements BrowserDriver {

  private static final String VALUE = "value";
  private static final int CLICK_PAUSE_MILLIS = 100;
  private static final int TYPE_PAUSE_MILLIS = 200;
  // An element inside a list with its own scroll (the options of a select) is first shown inside that list, or a click
  // on it would reach what lies below the list (Firefox does not scroll the list on its own). A click on the last pixels
  // of the viewport is lost by the browser, so the elements close to an edge are brought to the center
  private static final String SCROLL_AWAY_FROM_EDGE = "arguments[0].scrollIntoView({block: 'nearest', inline: 'nearest'});"
    + "var rect = arguments[0].getBoundingClientRect();"
    + "if (rect.top < 60 || rect.bottom > window.innerHeight - 60) {"
    + "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});}";
  // An element can be inside the viewport but clipped by a container with its own scroll
  private static final String SCROLL_INTO_CONTAINER = "arguments[0].scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'instant'});";
  // The Actions API does not scroll: Firefox refuses to move to an element outside the viewport
  private static final String SCROLL_NEAREST = "arguments[0].scrollIntoView({block: 'nearest', inline: 'nearest'});";

  // Brings an element to the center of the viewport (a suggest panel opens aligned to its search box, so a scroll after
  // that would leave it misplaced)
  private static final String SCROLL_TO_CENTER = "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});";

  private final WebDriver driver;

  /**
   * Create the adapter
   *
   * @param driver Selenium driver
   */
  public SeleniumBrowserDriver(WebDriver driver) {
    this.driver = driver;
  }

  /**
   * Convert a neutral key to the Selenium key
   *
   * @param key Neutral key
   * @return Selenium key
   */
  static Keys toSelenium(Key key) {
    return Keys.valueOf(key.name());
  }

  @Override
  public int count(Locator locator) {
    return guard(locator, () -> driver.findElements(locator.toBy()).size());
  }

  @Override
  public boolean isVisible(Locator locator) {
    return orFalse(locator, WebElement::isDisplayed);
  }

  @Override
  public boolean isEnabled(Locator locator) {
    return orFalse(locator, WebElement::isEnabled);
  }

  @Override
  public String text(Locator locator) {
    return on(locator, WebElement::getText);
  }

  @Override
  public List<String> texts(Locator locator) {
    return guard(locator, () -> driver.findElements(locator.toBy()).stream().map(WebElement::getText).toList());
  }

  @Override
  public String attribute(Locator locator, String name) {
    return on(locator, element -> element.getAttribute(name));
  }

  @Override
  public String attribute(Locator context, Locator relative, String name) {
    return guard(relative, () -> driver.findElement(context.toBy()).findElement(relative.toBy()).getAttribute(name));
  }

  @Override
  public String tagName(Locator locator) {
    return on(locator, WebElement::getTagName);
  }

  @Override
  public String selectedOptionText(Locator locator) {
    return on(locator, element -> new Select(element).getFirstSelectedOption().getText());
  }

  @Override
  public List<ElementRef> elements(Locator locator) {
    return guard(locator, () -> driver.findElements(locator.toBy()).stream().<ElementRef>map(SeleniumElementRef::new).toList());
  }

  @Override
  public void click(Locator locator) {
    act(locator, element -> {
      runScript(SCROLL_AWAY_FROM_EDGE, element);
      new Actions(driver).moveToElement(element).click(element).pause(CLICK_PAUSE_MILLIS).perform();
    });
  }

  @Override
  public void doubleClick(Locator locator) {
    act(locator, element -> {
      runScript(SCROLL_INTO_CONTAINER, element);
      // The pointer goes to the element once: a click on the element finds it again for each one, and on a loaded
      // machine the clicks end up farther apart than the double click interval of the browser, which then takes them as
      // two single clicks
      new Actions(driver).moveToElement(element).doubleClick().pause(CLICK_PAUSE_MILLIS).perform();
    });
  }

  @Override
  public void contextClick(Locator locator) {
    act(locator, element -> {
      runScript(SCROLL_INTO_CONTAINER, element);
      new Actions(driver).moveToElement(element).contextClick(element).pause(CLICK_PAUSE_MILLIS).perform();
    });
  }

  @Override
  public void hover(Locator locator) {
    act(locator, element -> new Actions(driver).moveToElement(element).pause(CLICK_PAUSE_MILLIS).perform());
  }

  @Override
  public void hoverInstantly(Locator locator) {
    // The pointer of an action travels from where it was (Firefox moves it step by step), over whatever lies on the way
    act(locator, element -> {
      if (driver instanceof Interactive interactive) {
        PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, "default mouse");
        interactive.perform(List.of(new Sequence(mouse, 0)
          .addAction(mouse.createPointerMove(Duration.ZERO, PointerInput.Origin.fromElement(element), 0, 0))));
      } else {
        new Actions(driver).moveToElement(element).build().perform();
      }
    });
  }

  @Override
  public void moveMouseBy(int dx, int dy) {
    new Actions(driver).moveByOffset(dx, dy).pause(CLICK_PAUSE_MILLIS).perform();
  }

  @Override
  public void clickAtPointer() {
    new Actions(driver).click().pause(CLICK_PAUSE_MILLIS).perform();
  }

  @Override
  public void type(Locator locator, CharSequence text) {
    act(locator, element -> {
      runScript(SCROLL_NEAREST, element);
      new Actions(driver).sendKeys(element, text).pause(TYPE_PAUSE_MILLIS).perform();
    });
  }

  @Override
  public void clear(Locator locator) {
    String value = attribute(locator, VALUE);
    if (value != null && !value.isEmpty()) {
      act(locator, WebElement::clear);
      // One backspace more than the characters of the value, as the front end tests have always sent
      act(locator, element -> element.sendKeys(IntStream.range(-1, value.length())
        .mapToObj(index -> Keys.BACK_SPACE)
        .toArray(CharSequence[]::new)));
    }
  }

  @Override
  public void press(Key key) {
    new Actions(driver).sendKeys(toSelenium(key)).perform();
  }

  @Override
  public void press(Locator locator, Key key) {
    act(locator, element -> new Actions(driver).sendKeys(element, toSelenium(key)).perform());
  }

  @Override
  public void pause(Duration duration) {
    new Actions(driver).pause(duration).perform();
  }

  @Override
  public void open(String url) {
    driver.get(url);
  }

  @Override
  public boolean isPageLoaded() {
    return "complete".equals(executeScript("return document.readyState"));
  }

  @Override
  public void setScriptTimeout(Duration timeout) {
    driver.manage().timeouts().scriptTimeout(timeout);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    return executor().executeScript(script, args);
  }

  @Override
  public Object executeScriptOn(Locator locator, String script, Object... args) {
    JavascriptExecutor executor = executor();
    return on(locator, element -> executor.executeScript(script, withFirst(element, args)));
  }

  @Override
  public void scrollTo(Locator locator, int x, int y) {
    executeScriptOn(locator, "arguments[0].scrollTo(arguments[1], arguments[2]);", x, y);
  }

  @Override
  public void scrollToCenter(Locator locator) {
    act(locator, element -> runScript(SCROLL_TO_CENTER, element));
  }

  @Override
  public void inFrame(Locator frame, Runnable body) {
    WebElement frameElement = on(frame, element -> element);
    driver.switchTo().frame(frameElement);
    try {
      body.run();
    } finally {
      driver.switchTo().defaultContent();
    }
  }

  @Override
  public Optional<byte[]> screenshot() {
    if (driver instanceof TakesScreenshot camera) {
      return Optional.of(camera.getScreenshotAs(OutputType.BYTES));
    }
    return Optional.empty();
  }

  @Override
  public String pageSource() {
    return driver.getPageSource();
  }

  @Override
  public List<ConsoleEntry> consoleEntries() {
    try {
      List<ConsoleEntry> entries = new ArrayList<>();
      driver.manage().logs().get(LogType.BROWSER).forEach(entry ->
        entries.add(new ConsoleEntry(entry.getTimestamp(), entry.getLevel().getName(), entry.getMessage())));
      return entries;
    } catch (Exception exc) {
      // Firefox and the remote drivers do not expose the browser logs
      log.debug("The browser console is not available", exc);
      return List.of();
    }
  }

  @Override
  public void setWindowSize(int width, int height) {
    driver.manage().window().setSize(new Dimension(width, height));
  }

  @Override
  public void setWindowPosition(int x, int y) {
    driver.manage().window().setPosition(new Point(x, y));
  }

  @Override
  public void quit() {
    driver.quit();
  }

  private <T> T on(Locator locator, Function<WebElement, T> operation) {
    return guard(locator, () -> operation.apply(driver.findElement(locator.toBy())));
  }

  private void act(Locator locator, Consumer<WebElement> action) {
    on(locator, element -> {
      action.accept(element);
      return null;
    });
  }

  private boolean orFalse(Locator locator, Function<WebElement, Boolean> check) {
    try {
      return on(locator, check);
    } catch (ElementNotFoundException | ElementReplacedException exc) {
      return false;
    }
  }

  /**
   * Run an operation of the driver, mapping the Selenium exceptions to the neutral ones
   */
  private <T> T guard(Locator locator, Supplier<T> operation) {
    try {
      return operation.get();
    } catch (NoSuchElementException exc) {
      throw new ElementNotFoundException(locator, exc);
    } catch (StaleElementReferenceException | MoveTargetOutOfBoundsException exc) {
      throw new ElementReplacedException(locator, exc);
    }
  }

  private JavascriptExecutor executor() {
    if (driver instanceof JavascriptExecutor executor) {
      return executor;
    }
    throw new UnsupportedOperationException("The browser cannot run scripts");
  }

  private static Object[] withFirst(Object first, Object[] rest) {
    Object[] all = new Object[rest.length + 1];
    all[0] = first;
    System.arraycopy(rest, 0, all, 1, rest.length);
    return all;
  }

  private void runScript(String script, WebElement element) {
    if (driver instanceof JavascriptExecutor executor) {
      try {
        executor.executeScript(script, element);
      } catch (Exception exc) {
        // The action itself scrolls the element into view
        log.debug("Could not scroll the element before acting on it", exc);
      }
    }
  }
}
