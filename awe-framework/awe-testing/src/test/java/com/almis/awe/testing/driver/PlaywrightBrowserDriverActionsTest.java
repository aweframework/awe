package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gestures of the Playwright adapter of the driver port, on a real headless Chromium: what the page receives is what
 * Selenium sends (a click is move, press, release; a double click is one gesture; keys go to the focused element)
 */
class PlaywrightBrowserDriverActionsTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ITEM = Locator.css("#item");
  private static final Locator INPUT = Locator.css("#input");
  private static final String RECORDER = "<script>window.events = [];"
    + "['click','dblclick','contextmenu','mouseover','mousedown','mouseup'].forEach(function(type) {"
    + "document.addEventListener(type, function(e) { window.events.push(type + ':' + (e.target.id || e.target.tagName) + ':' + e.button); }, true);});"
    + "['keydown'].forEach(function(type) {"
    + "document.addEventListener(type, function(e) { window.events.push(type + ':' + e.key); }, true);});</script>";

  private String events() {
    return String.valueOf(eval("window.events.join(',')"));
  }

  @Test
  void shouldClickTheCenterOfTheElementAndLetTheClickSettle() {
    show(RECORDER + "<button id='item' style='margin:100px;width:80px;height:40px'>Go</button>");

    long start = System.nanoTime();
    browser.click(ITEM);

    assertThat(events()).isEqualTo("mouseover:item:0,mousedown:item:0,mouseup:item:0,click:item:0");
    assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(100);
  }

  @Test
  void shouldScrollAnElementFarBelowTheViewportBeforeClickingIt() {
    show(RECORDER + "<div style='height:3000px'></div><button id='item'>Far</button><div style='height:200px'></div>");

    browser.click(ITEM);

    assertThat(events()).endsWith("click:item:0");
    assertThat(((Number) eval("window.scrollY")).intValue()).isPositive();
  }

  @Test
  void shouldBringAnElementCloseToTheViewportEdgeToTheCenterBeforeClickingIt() {
    // The last pixels of the viewport lose the click
    show(RECORDER + "<div style='height:560px'></div><button id='item' style='height:30px'>Edge</button><div style='height:3000px'></div>");

    browser.click(ITEM);

    assertThat(events()).endsWith("click:item:0");
    assertThat(((Number) eval("document.getElementById('item').getBoundingClientRect().bottom")).intValue()).isLessThan(540);
  }

  @Test
  void shouldDoubleClickInOneGesture() {
    show(RECORDER + "<button id='item' style='width:80px;height:40px'>Go</button>");

    browser.doubleClick(ITEM);

    assertThat(events().split(",")).contains("dblclick:item:0").filteredOn(event -> event.startsWith("mousedown")).hasSize(2);
  }

  @Test
  void shouldContextClickWithTheSecondButton() {
    show(RECORDER + "<button id='item' style='width:80px;height:40px'>Go</button>");

    browser.contextClick(ITEM);

    assertThat(events()).contains("mousedown:item:2").contains("contextmenu:item:2");
  }

  @Test
  void shouldHoverWithoutClicking() {
    show(RECORDER + "<button id='item' style='width:80px;height:40px'>Go</button>");

    browser.hover(ITEM);

    assertThat(events()).contains("mouseover:item:0").doesNotContain("mousedown");
  }

  @Test
  void shouldHoverInOneJumpWithoutPausing() {
    show(RECORDER + "<button id='item' style='width:80px;height:40px'>Go</button>");

    long start = System.nanoTime();
    browser.hoverInstantly(ITEM);

    assertThat(events()).contains("mouseover:item:0").doesNotContain("mousedown");
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
  }

  @Test
  void shouldMoveThePointerByAnOffsetAndClickWhereItIs() {
    show(RECORDER + "<button id='item' style='position:absolute;left:100px;top:100px;width:100px;height:60px'>Go</button>");
    browser.hoverInstantly(ITEM);
    page.evaluate("window.events.length = 0");

    // The center of the button is (150, 130): move to its top left corner area
    browser.moveMouseBy(-40, -20);
    browser.clickAtPointer();

    assertThat(events()).contains("click:item:0");
  }

  @Test
  void shouldMoveThePointerFromTheOriginWhenItWasNeverMoved() {
    show(RECORDER + "<button id='item' style='position:absolute;left:10px;top:10px;width:100px;height:60px'>Go</button>");

    browser.moveMouseBy(30, 30);
    browser.clickAtPointer();

    assertThat(events()).contains("click:item:0");
  }

  @Test
  void shouldTypeIntoTheElementCharacterByCharacter() {
    show(RECORDER + "<input id='input' type='text'>");

    long start = System.nanoTime();
    browser.type(INPUT, "ab");

    assertThat(eval("document.getElementById('input').value")).isEqualTo("ab");
    assertThat(events()).contains("keydown:a", "keydown:b");
    assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(200);
  }

  @Test
  void shouldSendKeysStraightToAFileInputAsTheFileToUpload(@TempDir Path directory) throws IOException {
    Path file = Files.writeString(directory.resolve("upload.txt"), "content");
    show("<input id='input' type='file'>");

    browser.sendKeys(INPUT, directory.toString() + "/", "upload.txt");

    assertThat(eval("document.getElementById('input').files[0].name")).isEqualTo(file.getFileName().toString());
  }

  @Test
  void shouldSendKeysStraightToATextElementWithoutMovingToIt() {
    show(RECORDER + "<input id='input' type='text'>");

    browser.sendKeys(INPUT, "hel", "lo");

    assertThat(eval("document.getElementById('input').value")).isEqualTo("hello");
    assertThat(events()).doesNotContain("mouseover").doesNotContain("mousedown");
  }

  @Test
  void shouldClearTheValueAndSendOneBackspaceMoreThanItsCharacters() {
    show(RECORDER + "<input id='input' type='text' value='abc'>");

    browser.clear(INPUT);

    assertThat(eval("document.getElementById('input').value")).isEqualTo("");
    assertThat(events().split(",")).filteredOn("keydown:Backspace"::equals).hasSize(4);
  }

  @Test
  void shouldNotClearAnEmptyValue() {
    show(RECORDER + "<input id='input' type='text'>");

    browser.clear(INPUT);

    assertThat(events()).isEmpty();
  }

  @Test
  void shouldPressAKeyOnThePageAndOnAnElement() {
    show(RECORDER + "<input id='input' type='text'>");

    browser.press(Key.ESCAPE);
    browser.press(INPUT, Key.ENTER);
    browser.press(Key.ARROW_DOWN);

    assertThat(events()).contains("keydown:Escape", "keydown:Enter", "keydown:ArrowDown");
  }

  @Test
  void shouldPressTheKeyOnTheElementThatItTargets() {
    show("<input id='input' type='text' value='abc' style='width:200px'>"
      + "<script>document.addEventListener('keydown', function(e) { window.target = e.target.id; });</script>");

    browser.press(INPUT, Key.BACK_SPACE);

    assertThat(eval("window.target")).isEqualTo("input");
  }

  @Test
  void shouldPause() {
    show("<p>page</p>");

    long start = System.nanoTime();
    browser.pause(Duration.ofMillis(250));

    assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(240);
  }

  @Test
  void shouldFailAnActionOnAMissingElementWithANeutralExceptionWithoutWaiting() {
    show("<p>nothing</p>");

    long start = System.nanoTime();
    assertThatThrownBy(() -> browser.click(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.doubleClick(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.contextClick(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.hover(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.hoverInstantly(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.type(ITEM, "x")).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.sendKeys(ITEM, "x")).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.press(ITEM, Key.TAB)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.clear(ITEM)).isInstanceOf(ElementNotFoundException.class);

    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
  }

  @Test
  void shouldFailAsReplacedWhenTheElementHasNoBoxToActOn() {
    // An element that is in the document but takes no space cannot be pointed at: the caller looks it up again
    show("<button id='item' style='display:none'>Go</button>");

    assertThatThrownBy(() -> browser.click(ITEM)).isInstanceOf(ElementReplacedException.class);
    assertThatThrownBy(() -> browser.hover(ITEM)).isInstanceOf(ElementReplacedException.class);
  }
}
