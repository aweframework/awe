package com.almis.awe.testing.driver;

import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Page, script, scroll, window, frame and evidence operations of the Playwright adapter of the driver port, on a real
 * headless Chromium
 */
class PlaywrightBrowserDriverPageTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ITEM = Locator.css(".item");
  private static final Locator FRAME = Locator.css("iframe");
  // Blocks the script thread of the page for ten seconds: much longer than any of the bounds that the tests check, which are
  // generous too, so that a slow machine does not make them flaky. The page is closed with the context long before
  private static final String BUSY_LOOP = "var end = Date.now() + 10000; while (Date.now() < end) { }";
  // The calls that must give up are bounded far below the loop and well above their timeout
  private static final long GIVE_UP_BOUND_MILLIS = 5_000;
  private static final String PNG_SIGNATURE = "\u0089PNG";

  @Test
  void shouldOpenAPageAndTellWhenItHasLoaded() {
    browser.open("data:text/html,<p class='item'>opened</p>");

    assertThat(browser.isPageLoaded()).isTrue();
    assertThat(browser.text(ITEM)).isEqualTo("opened");
  }

  @Test
  void shouldTellThatAPageThatIsStillLoadingHasNotLoaded() {
    show("<p>loaded</p>");
    page.evaluate("Object.defineProperty(document, 'readyState', {get: function() { return 'interactive'; }})");

    assertThat(browser.isPageLoaded()).isFalse();
  }

  @Test
  void shouldRunAScriptWithItsArgumentsAndReturnWhatItReturns() {
    show("<p>page</p>");

    assertThat(browser.executeScript("return arguments[0] + arguments[1]", 1, 2)).isEqualTo(3L);
    assertThat(browser.executeScript("return arguments[0] + '-' + arguments[1]", "a", "b")).isEqualTo("a-b");
    assertThat(browser.executeScript("return arguments.length === 0 && document.body !== null")).isEqualTo(true);
    assertThat(browser.executeScript("return arguments[0] * 1.5", 3)).isEqualTo(4.5d);
    assertThat(browser.executeScript("return null")).isNull();
  }

  @Test
  void shouldGiveScriptsThirtySecondsByDefaultAsSeleniumDoes() {
    assertThat(browser.scriptTimeout()).isEqualTo(Duration.ofSeconds(30));

    browser.setScriptTimeout(Duration.ofSeconds(7));

    assertThat(browser.scriptTimeout()).isEqualTo(Duration.ofSeconds(7));
  }

  @Test
  void shouldRefuseAScriptTimeoutThatIsNotPositive() {
    // Playwright takes a timeout of zero as no timeout at all
    assertThatThrownBy(() -> browser.setScriptTimeout(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> browser.setScriptTimeout(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> browser.setScriptTimeout(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldFailAScriptThatDoesNotFinishWithinTheScriptTimeout() {
    show("<p>page</p>");
    browser.setScriptTimeout(Duration.ofMillis(300));

    long start = System.nanoTime();
    assertThatThrownBy(() -> browser.executeScript(BUSY_LOOP)).isInstanceOf(ScriptTimeoutException.class)
      .hasMessageContaining("300 ms").hasCauseInstanceOf(PlaywrightException.class);

    // The step fails when the timeout is over, instead of waiting for a page that never answers
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(GIVE_UP_BOUND_MILLIS);
  }

  @Test
  void shouldFailAScriptOnAnElementThatDoesNotFinishWithinTheScriptTimeout() {
    show("<p class='item'>one</p>");
    browser.setScriptTimeout(Duration.ofMillis(300));

    long start = System.nanoTime();
    assertThatThrownBy(() -> browser.executeScriptOn(ITEM, BUSY_LOOP)).isInstanceOf(ScriptTimeoutException.class);

    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(GIVE_UP_BOUND_MILLIS);
  }

  @Test
  void shouldFailTheLoadCheckOfAPageWhoseScriptThreadIsBlocked() {
    show("<p>page</p>");
    browser.setScriptTimeout(Duration.ofMillis(300));
    page.evaluate("setTimeout(function() { " + BUSY_LOOP + " }, 0)");

    long start = System.nanoTime();
    assertThatThrownBy(() -> browser.isPageLoaded()).isInstanceOf(ScriptTimeoutException.class);

    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(GIVE_UP_BOUND_MILLIS);
  }

  @Test
  void shouldNotStartALaterScriptThatTimedOutBeforeItBegan() {
    AtomicInteger runs = new AtomicInteger();
    page.exposeFunction("recordRun", arguments -> {
      runs.incrementAndGet();
      return null;
    });
    show("<p>page</p>");
    browser.setScriptTimeout(Duration.ofMillis(300));
    // The page is busy for two seconds: the script cannot begin before its timeout
    page.evaluate("setTimeout(function() { var end = Date.now() + 2000; while (Date.now() < end) { } }, 0)");

    assertThatThrownBy(() -> browser.executeScript("window.recordRun();")).isInstanceOf(ScriptTimeoutException.class);
    // When the page is free again the script that was given up does not run
    page.waitForTimeout(3_000);

    assertThat(runs).hasValue(0);
    assertThat(browser.executeScript("return 1 + 1")).isEqualTo(2L);
  }

  @ParameterizedTest
  @ValueSource(strings = {"location.href = 'about:blank'", "location.href = 'data:text/html,<p>new</p>'",
    "document.getElementById('form').submit()"})
  void shouldRunOnceAndReturnNothingAScriptThatNavigatesThePage(String navigation) {
    AtomicInteger runs = new AtomicInteger();
    page.exposeFunction("recordRun", arguments -> {
      runs.incrementAndGet();
      return null;
    });
    show("<form id='form' action='about:blank'><input></form>");

    // The page that the script leaves is not the one that answers, and Playwright runs again what it waits for in the new
    // document: the script must neither fail nor run twice
    Object result = browser.executeScript("window.recordRun(); " + navigation + ";");
    page.waitForTimeout(500);

    assertThat(result).isNull();
    assertThat(runs).hasValue(1);
    assertThat(browser.executeScript("return 1 + 1")).isEqualTo(2L);
  }

  @Test
  void shouldRunOnceAScriptOnAnElementThatNavigatesThePage() {
    AtomicInteger runs = new AtomicInteger();
    page.exposeFunction("recordRun", arguments -> {
      runs.incrementAndGet();
      return null;
    });
    show("<form class='item' action='about:blank'><input></form>");

    assertThat(browser.executeScriptOn(ITEM, "window.recordRun(); arguments[0].submit();")).isNull();
    page.waitForTimeout(500);

    assertThat(runs).hasValue(1);
  }

  @Test
  void shouldHandOverTheResultOfAScriptThatDoesNotNavigate() {
    show("<p class='item'>one</p>");

    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> result = (java.util.Map<String, Object>) browser.executeScript("return {name: 'x', list: [1, 2], none: null}");

    assertThat(result).containsEntry("name", "x").containsEntry("list", List.of(1, 2)).containsKey("none");
  }

  @Test
  void shouldRunAScriptThatReturnsNothingAndChangesThePage() {
    show("<p>page</p>");

    // The statements of a script that does not return, as the mouse follower of the tests
    assertThat(browser.executeScript("let marker = document.createElement('span'); marker.id = 'marker'; document.body.appendChild(marker);"))
      .isNull();

    assertThat(browser.exists(Locator.css("#marker"))).isTrue();
  }

  @Test
  void shouldFailWhenTheScriptFails() {
    show("<p>page</p>");

    assertThatThrownBy(() -> browser.executeScript("throw new Error('boom')")).hasMessageContaining("boom");
  }

  @Test
  void shouldRunAScriptOnTheElementFirst() {
    show("<p class='item'>one</p><p class='item'>two</p>");

    assertThat(browser.executeScriptOn(ITEM, "return arguments[0].textContent + arguments[1]", "!")).isEqualTo("one!");
  }

  @Test
  void shouldFailToRunAScriptOnAnElementThatIsNotThere() {
    show("<p>none</p>");

    assertThatThrownBy(() -> browser.executeScriptOn(ITEM, "return 1")).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldScrollTheContentOfAnElement() {
    show("<div class='item' style='width:100px;height:100px;overflow:auto'><div style='width:1000px;height:1000px'></div></div>");

    browser.scrollTo(ITEM, 120, 30);

    assertThat(eval("document.querySelector('.item').scrollLeft")).isEqualTo(120);
    assertThat(eval("document.querySelector('.item').scrollTop")).isEqualTo(30);
  }

  @Test
  void shouldBringAnElementToTheCenter() {
    show("<div style='height:3000px'></div><p class='item'>far</p><div style='height:3000px'></div>");

    browser.scrollToCenter(ITEM);

    double top = ((Number) eval("document.querySelector('.item').getBoundingClientRect().top")).doubleValue();
    assertThat(top).isBetween(200d, 400d);
  }

  @Test
  void shouldLetTheMissingElementOfTheScrollToTheCenterBeKnown() {
    show("<p>none</p>");

    assertThatThrownBy(() -> browser.scrollToCenter(ITEM)).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldResizeTheWindowAndAcceptAPositionThatAViewportCannotHave() {
    show("<p>page</p>");

    browser.setWindowSize(500, 400);
    browser.setWindowPosition(10, 20);
    browser.setScriptTimeout(Duration.ofSeconds(7));

    assertThat(eval("window.innerWidth + 'x' + window.innerHeight")).isEqualTo("500x400");
  }

  @Test
  void shouldRunInsideAFrameAndComeBackToTheTopDocument() {
    show("<p class='item'>top</p><iframe srcdoc=\"<p class='item'>inside</p><p id='only-inside'>x</p>\"></iframe>");
    page.frames().forEach(frame -> frame.waitForLoadState());
    AtomicInteger runs = new AtomicInteger();

    browser.inFrame(FRAME, () -> {
      assertThat(browser.text(ITEM)).isEqualTo("inside");
      assertThat(browser.exists(Locator.css("#only-inside"))).isTrue();
      assertThat(browser.executeScript("return document.querySelector('.item').textContent")).isEqualTo("inside");
      assertThat(browser.pageSource()).contains("only-inside");
      runs.incrementAndGet();
    });

    assertThat(runs).hasValue(1);
    assertThat(browser.text(ITEM)).isEqualTo("top");
    assertThat(browser.exists(Locator.css("#only-inside"))).isFalse();
  }

  @Test
  void shouldActInsideAFrameWithTheCoordinatesOfThePage() {
    show("<div style='height:200px'></div><iframe style='border:0;margin-left:50px' width='300' height='200' "
      + "srcdoc=\"<button id='inside' style='margin:20px' onclick='document.title=&quot;clicked&quot;'>Go</button>\"></iframe>");
    page.frames().forEach(frame -> frame.waitForLoadState());

    browser.inFrame(FRAME, () -> browser.click(Locator.css("#inside")));

    assertThat(page.frames().get(1).title()).isEqualTo("clicked");
  }

  @Test
  void shouldComeBackFromTheFrameWhenItFails() {
    show("<p class='item'>top</p><iframe srcdoc=\"<p class='item'>inside</p>\"></iframe>");
    page.frames().forEach(frame -> frame.waitForLoadState());
    IllegalStateException failure = new IllegalStateException("inside");

    assertThatThrownBy(() -> browser.inFrame(FRAME, () -> {
      throw failure;
    })).isSameAs(failure);

    assertThat(browser.text(ITEM)).isEqualTo("top");
  }

  @Test
  void shouldNotSwitchWhenTheFrameIsNotThere() {
    show("<p class='item'>top</p>");
    Runnable body = () -> {
      throw new IllegalStateException("must not run");
    };

    assertThatThrownBy(() -> browser.inFrame(FRAME, body)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.inFrame(ITEM, body)).isInstanceOf(ElementNotFoundException.class)
      .hasMessageContaining(ITEM.toString());
  }

  @Test
  void shouldTakeAScreenshotAsPng() {
    show("<p>page</p>");

    Optional<byte[]> png = browser.screenshot();

    assertThat(png).isPresent();
    assertThat(new String(png.get(), 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo(PNG_SIGNATURE);
  }

  @Test
  void shouldNeverFailToTakeAScreenshot() {
    show("<p>page</p>");
    context.close();

    assertThat(browser.screenshot()).isEmpty();
  }

  @Test
  void shouldReadThePageSource() {
    show("<p id='source'>source</p>");

    assertThat(browser.pageSource()).contains("<p id=\"source\">source</p>");
  }

  @Test
  void shouldReadTheBrowserConsoleAndEmptyItWhenRead() {
    show("<p>page</p>");
    page.evaluate("console.log('hello'); console.warn('careful'); console.error('crash')");

    List<ConsoleEntry> entries = browser.consoleEntries();

    assertThat(entries).extracting(ConsoleEntry::level).containsExactly("INFO", "WARNING", "SEVERE");
    assertThat(entries).extracting(ConsoleEntry::message).containsExactly("hello", "careful", "crash");
    assertThat(entries).extracting(ConsoleEntry::isSevere).containsExactly(false, false, true);
    assertThat(entries.get(0).timestamp()).isPositive();
    assertThat(browser.consoleEntries()).isEmpty();

    page.evaluate("console.log('later')");

    assertThat(browser.consoleEntries()).extracting(ConsoleEntry::message).containsExactly("later");
  }

  @Test
  void shouldReadAnUncaughtErrorOfThePageAsASevereEntry() {
    show("<p>page</p>");
    page.evaluate("setTimeout(function() { throw new Error('uncaught boom'); }, 0)");
    browser.pause(Duration.ofMillis(200));

    assertThat(browser.consoleEntries()).filteredOn(ConsoleEntry::isSevere).extracting(ConsoleEntry::message)
      .anyMatch(message -> message.contains("uncaught boom"));
  }

  @Test
  void shouldGiveTheBufferedConsoleWhenThePageDoesNotAnswerInsteadOfWaitingForIt() {
    show("<p>page</p>");
    page.evaluate("console.log('before the block')");
    page.evaluate("setTimeout(function() { " + BUSY_LOOP + " }, 0)");

    long start = System.nanoTime();
    List<ConsoleEntry> entries = browser.consoleEntries();

    assertThat(entries).extracting(ConsoleEntry::message).containsExactly("before the block");
    // Bounded by the time that the console waits for the page (two seconds), not by the ten of the page
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(GIVE_UP_BOUND_MILLIS);
  }

  @Test
  void shouldNeverFailToReadTheConsole() {
    show("<p>page</p>");
    context.close();

    assertThatCode(() -> browser.consoleEntries()).doesNotThrowAnyException();
  }

  @Test
  void shouldQuitTheBrowserPage() {
    show("<p>page</p>");

    browser.quit();

    assertThat(page.isClosed()).isTrue();
  }
}
