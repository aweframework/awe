package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Instant queries of the Playwright adapter of the driver port, on a real headless Chromium
 */
class PlaywrightBrowserDriverQueriesTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ITEM = Locator.css(".item");
  private static final Locator ROW_OF_CELL = Locator.xpath("ancestor-or-self::tr[1]");

  @Test
  void shouldCountAndFindElementsWithCssAndXpath() {
    show("<p class='item'>one</p><p class='item'>two</p>");

    assertThat(browser.count(ITEM)).isEqualTo(2);
    assertThat(browser.exists(ITEM)).isTrue();
    assertThat(browser.count(Locator.xpath("//p[@class='item']"))).isEqualTo(2);
    assertThat(browser.count(Locator.css(".none"))).isZero();
    assertThat(browser.exists(Locator.css(".none"))).isFalse();
  }

  @Test
  void shouldAnswerInstantlyWhenTheElementIsMissing() {
    show("<p>nothing</p>");

    long start = System.nanoTime();
    assertThat(browser.count(ITEM)).isZero();
    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isEnabled(ITEM)).isFalse();
    assertThatThrownBy(() -> browser.text(ITEM)).isInstanceOf(ElementNotFoundException.class);

    // Playwright waits up to 30 seconds for a missing element in its own API: the port must not
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
  }

  @Test
  void shouldReportVisibilityAndEnabledStateOfTheFirstMatch() {
    show("<button class='item' disabled>Go</button><button class='item'>Other</button>");

    assertThat(browser.isVisible(ITEM)).isTrue();
    assertThat(browser.isEnabled(ITEM)).isFalse();
    assertThat(browser.isEnabled(Locator.xpath("(//button)[2]"))).isTrue();
  }

  @Test
  void shouldTakeAHiddenElementAsNotVisible() {
    show("<p class='item' style='display:none'>hidden</p><p class='item2' style='visibility:hidden'>hidden</p>");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isVisible(Locator.css(".item2"))).isFalse();
    assertThat(browser.exists(ITEM)).isTrue();
  }

  @Test
  void shouldReadTheRenderedTextOfTheFirstMatchAndOfAllTheMatches() {
    show("<p class='item'>  Hello   <b>world</b> </p><p class='item'>two</p><p class='item' style='display:none'>hidden</p>");

    assertThat(browser.text(ITEM)).isEqualTo("Hello world");
    assertThat(browser.texts(ITEM)).containsExactly("Hello world", "two", "");
    assertThat(browser.texts(Locator.css(".none"))).isEmpty();
  }

  @Test
  void shouldReadAttributesAndPropertiesOfTheFirstMatch() {
    show("<input class='item' value='42' data-x='x1' type='text'><a id='link' href='http://example.test/path'>l</a>"
      + "<input type='checkbox' id='check' checked>");
    page.evaluate("document.querySelector('.item').value = 'typed'");

    // As Selenium does: the current value (a property), the attribute when there is no property
    assertThat(browser.attribute(ITEM, "value")).isEqualTo("typed");
    assertThat(browser.attribute(ITEM, "data-x")).isEqualTo("x1");
    assertThat(browser.attribute(ITEM, "missing")).isNull();
    assertThat(browser.attribute(Locator.css("#check"), "checked")).isEqualTo("true");
    assertThat(browser.attribute(Locator.css(".item"), "disabled")).isNull();
    assertThat(browser.attribute(Locator.css("#link"), "href")).isEqualTo("http://example.test/path");
    assertThat(browser.tagName(ITEM)).isEqualTo("input");
  }

  @Test
  void shouldReadTheSelectedOptionOfANativeSelect() {
    show("<select class='item'><option>10</option><option selected>25</option><option>50</option></select>");

    assertThat(browser.selectedOptionText(ITEM)).isEqualTo("25");
  }

  @Test
  void shouldFailWithANeutralExceptionWhenTheElementIsMissing() {
    show("<p>nothing</p>");

    assertThatThrownBy(() -> browser.text(ITEM)).isInstanceOf(ElementNotFoundException.class).hasMessageContaining(".item");
    assertThatThrownBy(() -> browser.attribute(ITEM, "id")).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.tagName(ITEM)).isInstanceOf(ElementNotFoundException.class);
    assertThatThrownBy(() -> browser.selectedOptionText(ITEM)).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldReadAnAttributeOfAnElementRelativeToAContext() {
    show("<table><tr row-id='7'><td class='item'>cell</td></tr><tr row-id='8'><td>other</td></tr></table>");

    assertThat(ROW_OF_CELL.isContextRelative()).isTrue();
    assertThat(browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isEqualTo("7");
  }

  @Test
  void shouldFailNamingTheRelativeLocatorWhenTheRelativeElementIsMissing() {
    show("<div class='item'>no row</div>");

    assertThatThrownBy(() -> browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isInstanceOf(ElementNotFoundException.class)
      .hasMessageContaining(ROW_OF_CELL.toString()).hasMessageNotContaining(ITEM.toString());
  }

  @Test
  void shouldFailNamingTheContextLocatorWhenTheContextIsMissing() {
    show("<div>nothing</div>");

    assertThatThrownBy(() -> browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isInstanceOf(ElementNotFoundException.class)
      .hasMessageContaining(ITEM.toString()).hasMessageNotContaining(ROW_OF_CELL.toString());
  }

  @Test
  void shouldReadANullAttributeOfAnElementRelativeToAContext() {
    show("<table><tr><td class='item'>cell</td></tr></table>");

    assertThat(browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isNull();
  }

  @Test
  void shouldGiveElementsAnIdentityAndKnowWhetherTheyAreStillAttached() {
    show("<p class='item'>one</p><p class='item'>two</p>");

    List<ElementRef> first = browser.elements(ITEM);
    List<ElementRef> second = browser.elements(ITEM);

    assertThat(first).hasSize(2);
    assertThat(first.get(0)).isEqualTo(second.get(0)).hasSameHashCodeAs(second.get(0));
    assertThat(first.get(0)).isNotEqualTo(first.get(1)).isNotEqualTo(null);
    assertThat(first.get(0).isAttached()).isTrue();

    page.evaluate("document.querySelector('.item').remove()");

    assertThat(first.get(0).isAttached()).isFalse();
    assertThat(first.get(1).isAttached()).isTrue();
  }

  @Test
  void shouldTellWhetherEachReferencedElementIsVisibleAndEnabled() {
    show("<button class='item' disabled>shown</button><button class='item' style='display:none'>hidden</button>");

    List<ElementRef> refs = browser.elements(ITEM);

    assertThat(refs.get(0).isVisible()).isTrue();
    assertThat(refs.get(0).isEnabled()).isFalse();
    assertThat(refs.get(1).isVisible()).isFalse();
    assertThat(refs.get(1).isEnabled()).isTrue();
  }

  @Test
  void shouldFailAsReplacedWhenAReferencedElementIsReadAfterItWasReplaced() {
    show("<button class='item'>old</button>");
    ElementRef ref = browser.elements(ITEM).get(0);
    page.evaluate("document.querySelector('.item').outerHTML = '<button class=\"item\">new</button>'");

    assertThatThrownBy(ref::isVisible).isInstanceOf(ElementReplacedException.class).hasMessageContaining(ITEM.toString());
    assertThatThrownBy(ref::isEnabled).isInstanceOf(ElementReplacedException.class);
    assertThat(browser.text(ITEM)).isEqualTo("new");
  }

  @Test
  void shouldTakeAReplacedElementAsNotVisibleAndNotEnabledOnceItIsGone() {
    show("<button class='item'>old</button>");
    page.evaluate("document.querySelector('.item').remove()");

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isEnabled(ITEM)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(Key.class)
  void shouldMapEveryNeutralKeyToAPlaywrightKeyName(Key key) {
    assertThat(PlaywrightBrowserDriver.toPlaywright(key)).isNotBlank();
  }
}
