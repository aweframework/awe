package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gestures of the Playwright adapter on an element that is not where the pointer would land yet: it is clipped by a
 * container that is still expanding (the submenu of a panel menu) or covered by something that is about to go away. Its box
 * is still, but the point that is clicked belongs to another element, so the adapter waits until the element receives the
 * pointer, as a user (and the polling of Selenium) does. On a real headless browser
 */
class PlaywrightBrowserDriverCoveredElementTest extends AbstractPlaywrightBrowserTest {

  private static final Locator LEAF = Locator.css("#leaf");
  private static final Locator ITEM = Locator.css("#item");
  private static final String RECORDER = "<script>window.events = [];"
    + "['click'].forEach(function(type) {"
    + "document.addEventListener(type, function(e) { window.events.push(type + ':' + (e.target.id || e.target.tagName)); }, true);});</script>";

  private String events() {
    return String.valueOf(eval("window.events.join(',')"));
  }

  @Test
  void shouldClickALeafOfAContainerThatIsStillExpandingAndClipsIt() {
    // The submenu of a panel menu: a container that grows from nothing and clips its overflow
    // (clip, not hidden: a hidden one is scrolled by the scroll into view that comes before the gesture). The leaf has its box from the
    // start (it does not move) but what is seen of it is only what the container has uncovered; what lies below the
    // container is at the place of the leaf until the container is open
    show("<style>#panel { height: 0; overflow: clip; transition: height 600ms linear; } #panel.open { height: 300px; }"
      + "#panel div { height: 200px; } #leaf { display: block; width: 100px; height: 40px; } #below { height: 800px; }</style>"
      + RECORDER + "<div id='panel'><div>padding</div><button id='leaf'>Leaf</button></div><div id='below'>below</div>");
    page.evaluate("document.getElementById('panel').getBoundingClientRect(); document.getElementById('panel').classList.add('open')");

    browser.click(LEAF);

    assertThat(events()).isEqualTo("click:leaf");
  }

  @Test
  void shouldClickAnElementThatIsUnderAnOverlayUntilTheOverlayIsRemoved() {
    show("<style>#cover { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; }</style>"
      + RECORDER + "<button id='item' style='margin:100px;width:80px;height:40px'>Go</button><div id='cover'></div>");
    page.evaluate("setTimeout(function() { document.getElementById('cover').remove(); }, 300)");

    browser.click(ITEM);

    assertThat(events()).isEqualTo("click:item");
    assertThat(browser.exists(Locator.css("#cover"))).isFalse();
  }

  @Test
  void shouldGoOnAndClickWhateverCoversTheElementWhenItNeverGetsFree() {
    show("<style>#cover { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; }</style>"
      + RECORDER + "<button id='item' style='margin:100px;width:80px;height:40px'>Go</button><div id='cover'></div>");

    long start = System.nanoTime();
    browser.click(ITEM);
    long elapsed = (System.nanoTime() - start) / 1_000_000;

    // It waited for the cap (a second and a half) and then did the gesture as it is done today, without failing for it
    assertThat(events()).isEqualTo("click:cover");
    assertThat(elapsed).isGreaterThanOrEqualTo(1_400L);
  }

  @Test
  void shouldNotWaitForAnElementThatIsNotCovered() {
    show(RECORDER + "<button id='item' style='margin:100px;width:80px;height:40px'>Go</button>");

    browser.click(ITEM);

    assertThat(events()).isEqualTo("click:item");
  }

  @Test
  void shouldClickAnElementWhoseDescendantReceivesThePointer() {
    show(RECORDER + "<button id='item' style='margin:100px;width:80px;height:40px'><span id='label' style='display:block;width:100%;height:100%'>Go</span></button>");

    browser.click(ITEM);

    assertThat(events()).isEqualTo("click:label");
  }

  @Test
  void shouldNotWaitForAnElementThatRendersItsContentInItsOwnShadowRoot() {
    // A web component: the point lands inside its own shadow root, which is still the element
    show(RECORDER + "<div id='item' style='margin:100px;width:80px;height:40px'></div>"
      + "<script>document.getElementById('item').attachShadow({mode: 'open'}).innerHTML ="
      + " \"<span style='display:block;width:80px;height:40px'>Go</span>\";</script>");

    long start = System.nanoTime();
    browser.click(ITEM);

    assertThat(events()).isEqualTo("click:item");
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1_400L);
  }

  @Test
  void shouldGoOnWithAnElementThatHasNoSizeOfItsOwn() {
    // An element with no area (its children are floated) cannot be hit, so it is not waited for
    show(RECORDER + "<div id='item' style='width:40px;height:0'><div style='float:left;width:40px;height:20px'>child</div></div>");

    long start = System.nanoTime();
    browser.hover(ITEM);

    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1_400L);
  }
}
