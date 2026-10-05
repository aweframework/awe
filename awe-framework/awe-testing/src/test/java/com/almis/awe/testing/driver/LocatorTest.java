package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocatorTest {

  @Test
  void shouldCreateCssLocator() {
    Locator locator = Locator.css("[data-testid='grid-row']");

    assertThat(locator.kind()).isEqualTo(Locator.Kind.CSS);
    assertThat(locator.expression()).isEqualTo("[data-testid='grid-row']");
  }

  @Test
  void shouldCreateXpathLocator() {
    Locator locator = Locator.xpath("//div[@id='a']");

    assertThat(locator.kind()).isEqualTo(Locator.Kind.XPATH);
    assertThat(locator.expression()).isEqualTo("//div[@id='a']");
  }

  @Test
  void shouldRejectNullAndBlankExpressions() {
    assertThatThrownBy(() -> Locator.css(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.css("  ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.xpath(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.xpath("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldHaveValueSemantics() {
    assertThat(Locator.css("a")).isEqualTo(Locator.css("a")).hasSameHashCodeAs(Locator.css("a"));
    assertThat(Locator.css("a")).isNotEqualTo(Locator.css("b"));
    assertThat(Locator.css("a")).isNotEqualTo(Locator.xpath("a"));
    Locator locator = Locator.css("a");
    // equals must reject null and objects of other types
    assertThat(locator.equals(null)).isFalse();
    assertThat(locator.equals((Object) "a")).isFalse();
  }

  @Test
  void shouldPrintKindAndExpression() {
    assertThat(Locator.css("div > a")).hasToString("css=div > a");
    assertThat(Locator.xpath("//a")).hasToString("xpath=//a");
  }

  @Test
  void shouldTellWhichLocatorsAreRelativeToTheContextElement() {
    assertThat(Locator.xpath("ancestor-or-self::*[@data-testid='grid-row'][1]").isContextRelative()).isTrue();
    assertThat(Locator.xpath("./td").isContextRelative()).isTrue();
    assertThat(Locator.xpath("//td").isContextRelative()).isFalse();
    assertThat(Locator.xpath("/html/body").isContextRelative()).isFalse();
    assertThat(Locator.xpath("(//td)[1]").isContextRelative()).isFalse();
    assertThat(Locator.xpath("(./td)[1]").isContextRelative()).isTrue();
    assertThat(Locator.css("td").isContextRelative()).isFalse();
  }

  @Test
  void shouldConvertToSelenium() {
    assertThat(Locator.css("div > a").toBy()).isEqualTo(By.cssSelector("div > a"));
    assertThat(Locator.xpath("//a").toBy()).isEqualTo(By.xpath("//a"));
  }

  @Test
  void shouldConvertFromCssAndXpath() {
    assertThat(Locator.from(By.cssSelector("div > a"))).isEqualTo(Locator.css("div > a"));
    assertThat(Locator.from(By.xpath("//a[@x='1']"))).isEqualTo(Locator.xpath("//a[@x='1']"));
  }

  @Test
  void shouldConvertIdToAnEquivalentCss() {
    assertThat(Locator.from(By.id("ButLogOut"))).isEqualTo(Locator.css("#ButLogOut"));
    assertThat(Locator.from(By.id("main-menu-toggle"))).isEqualTo(Locator.css("#main-menu-toggle"));
  }

  @Test
  void shouldEscapeIdsLikeCssEscape() {
    assertThat(Locator.from(By.id("a.b")).expression()).isEqualTo("#a\\.b");
    assertThat(Locator.from(By.id("a:b")).expression()).isEqualTo("#a\\:b");
    assertThat(Locator.from(By.id("a b")).expression()).isEqualTo("#a\\ b");
    assertThat(Locator.from(By.id("a[0]")).expression()).isEqualTo("#a\\[0\\]");
    assertThat(Locator.from(By.id("1x")).expression()).isEqualTo("#\\31 x");
    assertThat(Locator.from(By.id("-1x")).expression()).isEqualTo("#-\\31 x");
    assertThat(Locator.from(By.id("-")).expression()).isEqualTo("#\\-");
    assertThat(Locator.from(By.id("a\u0001b")).expression()).isEqualTo("#a\\1 b");
    assertThat(Locator.from(By.id("a_b-c9")).expression()).isEqualTo("#a_b-c9");
    assertThat(Locator.from(By.id("ñandú")).expression()).isEqualTo("#ñandú");
  }

  @Test
  void shouldRejectKindsThatAreNotNeutral() {
    By name = By.name("n");

    assertThatThrownBy(() -> Locator.from(name)).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(name.toString());
    By className = By.className("c");
    By linkText = By.linkText("l");
    By tagName = By.tagName("t");
    By chained = new org.openqa.selenium.support.pagefactory.ByChained(By.id("a"), By.id("b"));
    assertThatThrownBy(() -> Locator.from(className)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.from(linkText)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.from(tagName)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.from(chained)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Locator.from((By) null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldConvertLists() {
    assertThat(Locator.from(List.of(By.id("ButUsrAct"), By.cssSelector("a"), By.xpath("//b"))))
      .containsExactly(Locator.css("#ButUsrAct"), Locator.css("a"), Locator.xpath("//b"));
    assertThat(Locator.from(List.<By>of())).isEmpty();
  }
}
