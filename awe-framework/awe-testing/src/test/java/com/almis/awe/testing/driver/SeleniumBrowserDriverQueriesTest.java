package com.almis.awe.testing.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instant queries of the Selenium adapter of the driver port, on a mocked {@code WebDriver}.
 */
class SeleniumBrowserDriverQueriesTest {

  private static final Locator ITEM = Locator.css(".item");
  private static final Locator ROW_OF_CELL = Locator.xpath("ancestor-or-self::tr[1]");

  private WebDriver driver;
  private SeleniumBrowserDriver browser;

  @BeforeEach
  void setUp() {
    driver = mock(WebDriver.class);
    browser = new SeleniumBrowserDriver(driver);
    doThrow(new NoSuchElementException("missing")).when(driver).findElement(ITEM.toBy());
    when(driver.findElements(ITEM.toBy())).thenReturn(List.of());
  }

  private WebElement present() {
    WebElement element = mock(WebElement.class);
    doReturn(element).when(driver).findElement(ITEM.toBy());
    when(driver.findElements(ITEM.toBy())).thenReturn(List.of(element));
    return element;
  }

  @Test
  void shouldCountAndFindElements() {
    assertThat(browser.count(ITEM)).isZero();
    assertThat(browser.exists(ITEM)).isFalse();

    present();

    assertThat(browser.count(ITEM)).isEqualTo(1);
    assertThat(browser.exists(ITEM)).isTrue();
  }

  @Test
  void shouldReportVisibilityAndEnabledStateOfTheFirstMatch() {
    WebElement element = present();
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(false);

    assertThat(browser.isVisible(ITEM)).isTrue();
    assertThat(browser.isEnabled(ITEM)).isFalse();
  }

  @Test
  void shouldTakeAMissingOrReplacedElementAsNotVisibleAndNotEnabled() {
    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isEnabled(ITEM)).isFalse();

    WebElement element = present();
    when(element.isDisplayed()).thenThrow(new StaleElementReferenceException("replaced"));
    when(element.isEnabled()).thenThrow(new StaleElementReferenceException("replaced"));

    assertThat(browser.isVisible(ITEM)).isFalse();
    assertThat(browser.isEnabled(ITEM)).isFalse();
  }

  @Test
  void shouldReadTheRenderedTextAttributeAndTagOfTheFirstMatch() {
    WebElement element = present();
    when(element.getText()).thenReturn("Hello");
    when(element.getAttribute("value")).thenReturn("42");
    when(element.getTagName()).thenReturn("input");

    assertThat(browser.text(ITEM)).isEqualTo("Hello");
    assertThat(browser.attribute(ITEM, "value")).isEqualTo("42");
    assertThat(browser.tagName(ITEM)).isEqualTo("input");
  }

  @Test
  void shouldReadTheTextOfAllTheMatches() {
    WebElement first = mock(WebElement.class);
    WebElement second = mock(WebElement.class);
    when(first.getText()).thenReturn("one");
    when(second.getText()).thenReturn("two");
    when(driver.findElements(ITEM.toBy())).thenReturn(List.of(first, second));

    assertThat(browser.texts(ITEM)).containsExactly("one", "two");
  }

  @Test
  void shouldReadTheSelectedOptionOfANativeSelect() {
    WebElement select = present();
    when(select.getTagName()).thenReturn("select");
    WebElement unselected = mock(WebElement.class);
    WebElement selected = mock(WebElement.class);
    when(selected.isSelected()).thenReturn(true);
    when(selected.getText()).thenReturn("25");
    when(select.findElements(By.tagName("option"))).thenReturn(List.of(unselected, selected));

    assertThat(browser.selectedOptionText(ITEM)).isEqualTo("25");
  }

  @Test
  void shouldFailWithANeutralExceptionWhenTheElementIsMissing() {
    assertThatThrownBy(() -> browser.text(ITEM))
      .isInstanceOf(ElementNotFoundException.class)
      .hasMessageContaining(".item")
      .hasCauseInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldFailWithAReplacedElementExceptionWhenTheElementIsStale() {
    WebElement element = present();
    when(element.getText()).thenThrow(new StaleElementReferenceException("replaced"));

    assertThatThrownBy(() -> browser.text(ITEM))
      .isInstanceOf(ElementReplacedException.class)
      .hasCauseInstanceOf(StaleElementReferenceException.class);
  }

  @Test
  void shouldReadAnAttributeOfAnElementRelativeToAContext() {
    WebElement cell = present();
    WebElement row = mock(WebElement.class);
    when(row.getAttribute("row-id")).thenReturn("7");
    when(cell.findElement(ROW_OF_CELL.toBy())).thenReturn(row);

    assertThat(ROW_OF_CELL.isContextRelative()).isTrue();
    assertThat(browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isEqualTo("7");
  }

  @Test
  void shouldFailWhenTheRelativeElementIsMissing() {
    WebElement cell = present();
    doThrow(new NoSuchElementException("no row")).when(cell).findElement(ROW_OF_CELL.toBy());

    assertThatThrownBy(() -> browser.attribute(ITEM, ROW_OF_CELL, "row-id")).isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldGiveElementsAnIdentityAndKnowWhetherTheyAreStillAttached() {
    WebElement element = present();

    List<ElementRef> first = browser.elements(ITEM);
    List<ElementRef> second = browser.elements(ITEM);

    assertThat(first).hasSize(1);
    assertThat(first.get(0)).isEqualTo(second.get(0)).hasSameHashCodeAs(second.get(0));
    assertThat(first.get(0).isAttached()).isTrue();

    when(element.isEnabled()).thenThrow(new StaleElementReferenceException("replaced"));

    assertThat(first.get(0).isAttached()).isFalse();
  }

  @Test
  void shouldNotTakeDifferentElementsAsTheSame() {
    WebElement other = mock(WebElement.class);
    when(driver.findElements(ITEM.toBy())).thenReturn(List.of(mock(WebElement.class), other));

    List<ElementRef> refs = browser.elements(ITEM);

    assertThat(refs.get(0)).isNotEqualTo(refs.get(1)).isNotEqualTo(null);
  }

  @ParameterizedTest
  @EnumSource(Key.class)
  void shouldMapEveryNeutralKeyToTheSeleniumKeyWithTheSameName(Key key) {
    assertThat(SeleniumBrowserDriver.toSelenium(key).name()).isEqualTo(key.name());
  }
}
