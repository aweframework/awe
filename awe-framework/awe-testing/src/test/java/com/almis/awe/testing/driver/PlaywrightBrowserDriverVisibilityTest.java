package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Visibility of the Playwright adapter of the driver port, on a real headless browser. It is the rule of the Selenium
 * adapter (the {@code isDisplayed} atom), not the one of Playwright: an element that takes no space is shown when
 * something inside it does, and an element with no opacity is not shown
 */
class PlaywrightBrowserDriverVisibilityTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ITEM = Locator.css(".item");
  private static final String SIZED_CHILD = "<div style='float:left;width:40px;height:20px'>child</div>";

  @Test
  void shouldShowAnElementWithNoSizeWhenAChildHasOne() {
    // The pinned column of a tree grid: a wrapper with a width and no height, around floated cells
    show("<div class='item' style='width:40px;height:0'>" + SIZED_CHILD + "</div>");

    assertThat(browser.isVisible(ITEM)).isTrue();
    assertThat(browser.elements(ITEM).get(0).isVisible()).isTrue();
    assertThat(browser.text(ITEM)).isEqualTo("child");
  }

  @Test
  void shouldShowAnElementWithNoSizeWhenAGrandchildHasOne() {
    show("<div class='item' style='width:0;height:0'><div style='width:0;height:0'>" + SIZED_CHILD + "</div></div>");

    assertThat(browser.isVisible(ITEM)).isTrue();
  }

  @Test
  void shouldShowAnElementWithNoSizeWhenItHoldsText() {
    show("<div class='item' style='width:0;height:0'>some text</div>");

    assertThat(browser.isVisible(ITEM)).isTrue();
  }

  @Test
  void shouldNotShowAnElementWithNoSizeAndNothingInside() {
    show("<div class='item' style='width:40px;height:0'></div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.elements(ITEM).get(0).isVisible()).isFalse();
  }

  @Test
  void shouldNotShowAnElementWithNoSizeThatHidesItsOverflow() {
    show("<div class='item' style='width:40px;height:0;overflow:hidden'>" + SIZED_CHILD + "</div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.text(ITEM)).isEmpty();
  }

  @Test
  void shouldNotShowAnElementInsideAnElementThatIsNotDisplayed() {
    show("<div style='display:none'><div class='item' style='width:40px;height:20px'>child</div></div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.exists(ITEM)).isTrue();
    assertThat(browser.text(ITEM)).isEmpty();
  }

  @Test
  void shouldNotShowAnElementWithoutOpacityNorOneInsideSuchAnElement() {
    show("<div class='item' style='opacity:0;width:40px;height:20px'>child</div>"
      + "<div style='opacity:0'><div class='inner' style='width:40px;height:20px'>child</div></div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isVisible(Locator.css(".inner"))).isFalse();
    assertThat(browser.elements(Locator.css(".inner")).get(0).isVisible()).isFalse();
    assertThat(browser.text(Locator.css(".inner"))).isEmpty();
  }

  @Test
  void shouldShowAnElementOnceItsOpacityIsBack() {
    show("<div class='item' style='opacity:0;width:40px;height:20px'>child</div>");
    page.evaluate("document.querySelector('.item').style.opacity = '0.4'");

    assertThat(browser.isVisible(ITEM)).isTrue();
  }

  @Test
  void shouldNotShowAnElementThatIsHiddenWithVisibility() {
    show("<div class='item' style='visibility:hidden;width:40px;height:20px'>child</div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.text(ITEM)).isEmpty();
  }

  @Test
  void shouldShowAnElementWithASize() {
    show("<button class='item' style='width:40px;height:20px'>Go</button>");

    assertThat(browser.isVisible(ITEM)).isTrue();
    assertThat(browser.text(ITEM)).isEqualTo("Go");
  }

  @Test
  void shouldAnswerFalseInstantlyForAnElementThatIsMissingOrWasReplaced() {
    show("<button class='item'>Go</button>");
    List<ElementRef> refs = browser.elements(ITEM);
    page.evaluate("document.querySelector('.item').remove()");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThatThrownBy(() -> refs.get(0).isVisible()).isInstanceOf(ElementReplacedException.class);
  }

  @Test
  void shouldRenderATransparentElementThatHasABoxAlthoughItIsNotShown() {
    // The loader that fades in: transparent in its first frame, but already there and covering what it loads
    show("<div class='item' style='opacity:0;width:40px;height:20px'>child</div>"
      + "<div style='opacity:0'><div class='inner' style='width:40px;height:20px'>child</div></div>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isRendered(ITEM)).isTrue();
    assertThat(browser.isVisible(Locator.css(".inner"))).isFalse();
    assertThat(browser.isRendered(Locator.css(".inner"))).isTrue();
  }

  @Test
  void shouldRenderAnElementWithNoSizeWhenAChildHasOneAndNotOneThatHasNothingInside() {
    show("<div class='item' style='opacity:0;width:40px;height:0'>" + SIZED_CHILD + "</div>"
      + "<div class='empty' style='width:40px;height:0'></div>");

    assertThat(browser.isRendered(ITEM)).isTrue();
    assertThat(browser.isRendered(Locator.css(".empty"))).isFalse();
  }

  @Test
  void shouldNotRenderAnElementThatIsMissingNotDisplayedOrHiddenWithVisibility() {
    show("<div class='none' style='display:none;width:40px;height:20px'>child</div>"
      + "<div style='display:none'><div class='inside' style='width:40px;height:20px'>child</div></div>"
      + "<div class='hidden' style='visibility:hidden;width:40px;height:20px'>child</div>");

    assertThat(browser.isRendered(ITEM)).isFalse();
    assertThat(browser.isRendered(Locator.css(".none"))).isFalse();
    assertThat(browser.isRendered(Locator.css(".inside"))).isFalse();
    assertThat(browser.isRendered(Locator.css(".hidden"))).isFalse();
  }

  @Test
  void shouldRenderAnElementThatIsShown() {
    show("<button class='item' style='width:40px;height:20px'>Go</button>");

    assertThat(browser.isRendered(ITEM)).isTrue();
    assertThat(browser.isVisible(ITEM)).isTrue();
  }

  @Test
  void shouldKeepRenderingAFadingInLoaderUntilItIsRemoved() throws InterruptedException {
    // What a wait for the loader to be gone sees: a first frame at opacity 0 (not shown), then the fade, then the removal
    show("<style>#loader { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0; transition: opacity 150ms linear; }"
      + "#loader.in { opacity: 1; }</style><div id='grid'>rows</div>");
    page.evaluate("(() => { const loader = document.createElement('div'); loader.id = 'loader'; document.body.appendChild(loader);"
      + "loader.getBoundingClientRect(); requestAnimationFrame(() => loader.classList.add('in'));"
      + "setTimeout(() => loader.remove(), 500); })()");
    Locator loader = Locator.css("#loader");

    // The first frame: transparent but rendered, so a wait for it to be gone does not take it as gone
    assertThat(browser.isRendered(loader)).isTrue();
    int looks = 1;
    while (browser.isRendered(loader)) {
      Thread.sleep(20);
      looks++;
    }

    // It is the removal that ended the loop, not the first frame
    assertThat(browser.exists(loader)).isFalse();
    assertThat(looks).isGreaterThan(1);
  }
}
