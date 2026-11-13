package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gestures of the Playwright adapter on an element that is still moving (a modal that scales in, a group of the menu that
 * expands): the pointer goes to where the element ends, not to where it was when the adapter looked at it, as the human
 * (and the polling of Selenium) tests expect. On a real headless browser
 */
class PlaywrightBrowserDriverMovingElementTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ITEM = Locator.css("#item");
  private static final String RECORDER = "<script>window.events = []; window.pointer = null;"
    + "['click','mousedown','mouseup'].forEach(function(type) {"
    + "document.addEventListener(type, function(e) { window.events.push(type + ':' + (e.target.id || e.target.tagName)); }, true);});"
    + "['mousemove','mousedown'].forEach(function(type) {"
    + "document.addEventListener(type, function(e) { window.pointer = [e.clientX, e.clientY]; }, true);});</script>";
  // The element centre and the last place of the pointer, once the gesture is done
  private static final String CENTER = "(() => { const r = document.getElementById('item').getBoundingClientRect();"
    + "return [r.x + r.width / 2, r.y + r.height / 2]; })()";

  private String events() {
    return String.valueOf(eval("window.events.join(',')"));
  }

  private void assertPointerIsOnTheElement() {
    @SuppressWarnings("unchecked")
    java.util.List<Number> center = (java.util.List<Number>) eval(CENTER);
    @SuppressWarnings("unchecked")
    java.util.List<Number> pointer = (java.util.List<Number>) eval("window.pointer");
    assertThat(pointer.get(0).doubleValue()).isCloseTo(center.get(0).doubleValue(), org.assertj.core.data.Offset.offset(1.0));
    assertThat(pointer.get(1).doubleValue()).isCloseTo(center.get(1).doubleValue(), org.assertj.core.data.Offset.offset(1.0));
  }

  @Test
  void shouldClickAnElementThatSlidesInWhereItStops() {
    show("<style>#item { position: absolute; left: 0; top: 100px; width: 80px; height: 40px; transition: transform 300ms linear; }"
      + "#item.moved { transform: translateX(600px); }</style>"
      + RECORDER + "<button id='item'>Go</button>");
    page.evaluate("document.getElementById('item').getBoundingClientRect(); document.getElementById('item').classList.add('moved')");

    browser.click(ITEM);

    assertThat(events()).endsWith("click:item");
    assertPointerIsOnTheElement();
    assertThat(((Number) eval("document.getElementById('item').getBoundingClientRect().x")).intValue()).isEqualTo(600);
  }

  @Test
  void shouldClickAnElementThatTheContentAboveItPushesDown() {
    // A group of the menu that expands moves what lies below it
    show("<style>#group { max-height: 0; overflow: hidden; transition: max-height 300ms linear; } #group.open { max-height: 300px; }"
      + "#group div { height: 300px; }</style>"
      + RECORDER + "<div id='group'><div>content</div></div><button id='item' style='width:80px;height:40px'>Go</button>");
    page.evaluate("document.getElementById('group').getBoundingClientRect(); document.getElementById('group').classList.add('open')");

    browser.click(ITEM);

    assertThat(events()).endsWith("click:item");
    assertPointerIsOnTheElement();
    assertThat(((Number) eval("document.getElementById('item').getBoundingClientRect().y")).intValue()).isEqualTo(300);
  }

  @Test
  void shouldHoverAnElementThatIsStillMovingWhereItStops() {
    show("<style>#item { position: absolute; left: 0; top: 100px; width: 80px; height: 40px; transition: transform 300ms linear; }"
      + "#item.moved { transform: translateX(600px); }</style>"
      + RECORDER + "<button id='item'>Go</button>");
    page.evaluate("document.getElementById('item').getBoundingClientRect(); document.getElementById('item').classList.add('moved')");

    browser.hoverInstantly(ITEM);

    assertPointerIsOnTheElement();
  }

  @Test
  void shouldGiveUpWaitingForAnElementThatNeverStopsMoving() {
    // The pointer goes where the element is when the time is over, and the gesture is not lost for it
    show("<style>@keyframes wiggle { from { transform: translateX(0); } to { transform: translateX(10px); } }"
      + "#item { position: absolute; left: 100px; top: 100px; width: 80px; height: 40px; animation: wiggle 400ms linear infinite alternate; }</style>"
      + RECORDER + "<button id='item'>Go</button>");

    long start = System.nanoTime();
    browser.click(ITEM);
    long elapsed = (System.nanoTime() - start) / 1_000_000;

    assertThat(events()).endsWith("click:item");
    // It waited for the cap (a second and a half) and then gave up: the upper bound is generous for a slow runner, but far
    // below any action or test timeout, so a cap that stopped applying would fail it
    assertThat(elapsed).isBetween(1_400L, 15_000L);
  }

  @Test
  void shouldFailAsReplacedWhenTheElementIsGoneWhileItIsWaitedFor() {
    // It never stops moving, so the click is still waiting for it when it goes away: the timer starts right before the
    // click and leaves plenty of time for the lookup before the wait, and the wait lasts much longer than that
    show("<style>@keyframes wander { from { transform: translateX(0); } to { transform: translateX(400px); } }"
      + "#item { position: absolute; left: 0; top: 100px; width: 80px; height: 40px; animation: wander 500ms linear infinite; }</style>"
      + RECORDER + "<button id='item'>Go</button>");
    page.evaluate("setTimeout(function() { document.getElementById('item').style.display = 'none'; }, 500)");

    assertThatThrownBy(() -> browser.click(ITEM)).isInstanceOf(ElementReplacedException.class);
  }
}
