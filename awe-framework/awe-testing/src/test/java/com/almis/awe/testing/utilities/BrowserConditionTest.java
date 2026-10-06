package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserConditionTest {

  private static final Locator ITEM = Locator.css("#item");

  private final BrowserDriver browser = mock(BrowserDriver.class);

  @Test
  void shouldBePresentWhenSomethingMatches() {
    when(browser.exists(ITEM)).thenReturn(false, true);

    assertThat(BrowserCondition.present(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.present(ITEM).isMet(browser)).isTrue();
  }

  @Test
  void shouldBeVisibleOrInvisibleAsTheBrowserReportsIt() {
    when(browser.isVisible(ITEM)).thenReturn(true, false);

    assertThat(BrowserCondition.visible(ITEM).isMet(browser)).isTrue();
    assertThat(BrowserCondition.invisible(ITEM).isMet(browser)).isTrue();
  }

  @Test
  void shouldBeClickableOnlyWhenItIsVisibleAndEnabled() {
    when(browser.isVisible(ITEM)).thenReturn(false, true, true);
    when(browser.isEnabled(ITEM)).thenReturn(false, true);

    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isTrue();
  }

  @Test
  void shouldMatchTheTextOfTheFirstMatch() {
    when(browser.text(ITEM)).thenReturn("Hello world");

    assertThat(BrowserCondition.textContains(ITEM, "world").isMet(browser)).isTrue();
    assertThat(BrowserCondition.textContains(ITEM, "moon").isMet(browser)).isFalse();
  }

  @Test
  void shouldTakeAReplacedElementAsNotMatchingYetButNotAMissingOne() {
    when(browser.text(ITEM)).thenThrow(new ElementReplacedException(ITEM, new RuntimeException("stale")));
    when(browser.attribute(ITEM, "value")).thenThrow(new ElementNotFoundException(ITEM, new RuntimeException()));

    assertThat(BrowserCondition.textContains(ITEM, "x").isMet(browser)).isFalse();
    assertThatThrownBy(() -> BrowserCondition.valueContains(ITEM, "x").isMet(browser))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldMatchTheValueOfTheFirstMatchAndNeverAMissingOne() {
    when(browser.attribute(ITEM, "value")).thenReturn("typed text", null);

    assertThat(BrowserCondition.valueContains(ITEM, "typed").isMet(browser)).isTrue();
    assertThat(BrowserCondition.valueContains(ITEM, "typed").isMet(browser)).isFalse();
  }

  @Test
  void shouldRequireAllTheConditionsAndStopAtTheFirstOneThatFails() {
    BrowserCondition failing = BrowserCondition.of("failing", driver -> false);
    BrowserCondition second = BrowserCondition.of("second", driver -> {
      throw new IllegalStateException("must not be checked");
    });

    assertThat(BrowserCondition.allOf(failing, second).isMet(browser)).isFalse();
    assertThat(BrowserCondition.allOf(BrowserCondition.of("a", driver -> true), BrowserCondition.of("b", driver -> true))
      .isMet(browser)).isTrue();
  }

  @Test
  void shouldNegateTheConditionButLetAMissingElementThrough() {
    when(browser.attribute(ITEM, "value")).thenReturn("typed");

    assertThat(BrowserCondition.not(BrowserCondition.valueContains(ITEM, "typed")).isMet(browser)).isFalse();
    assertThat(BrowserCondition.not(BrowserCondition.valueContains(ITEM, "other")).isMet(browser)).isTrue();

    when(browser.attribute(ITEM, "value")).thenThrow(new ElementNotFoundException(ITEM, new RuntimeException()));
    assertThatThrownBy(() -> BrowserCondition.not(BrowserCondition.valueContains(ITEM, "typed")).isMet(browser))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldDescribeItselfForTheFailureMessage() {
    assertThat(BrowserCondition.present(ITEM)).hasToString("presence of element located by css=#item");
    assertThat(BrowserCondition.visible(ITEM)).hasToString("visibility of element located by css=#item");
    assertThat(BrowserCondition.invisible(ITEM)).hasToString("element located by css=#item to be invisible");
    assertThat(BrowserCondition.clickable(ITEM)).hasToString("element to be clickable: css=#item");
    assertThat(BrowserCondition.textContains(ITEM, "a")).hasToString("text 'a' to be present in element located by css=#item");
    assertThat(BrowserCondition.valueContains(ITEM, "a")).hasToString("text 'a' to be present in the value of element located by css=#item");
    assertThat(BrowserCondition.allOf(BrowserCondition.present(ITEM), BrowserCondition.visible(ITEM)))
      .hasToString("all conditions to be valid: presence of element located by css=#item and visibility of element located by css=#item");
    assertThat(BrowserCondition.not(BrowserCondition.present(ITEM)))
      .hasToString("condition to not be valid: presence of element located by css=#item");
  }
}
