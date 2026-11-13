package com.almis.awe.testing.model;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.driver.SeleniumBrowserDriver;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeleniumModelTest {

  @Test
  void shouldBuildTheBrowserFromTheSeleniumDriverWhenNoneWasSet() {
    SeleniumModel model = new SeleniumModel().setDriver(mock(WebDriver.class));

    assertThat(model.getBrowser()).isInstanceOf(SeleniumBrowserDriver.class);
  }

  @Test
  void shouldFollowTheSeleniumDriverWhenItIsReplaced() {
    WebDriver first = mock(WebDriver.class);
    WebDriver second = mock(WebDriver.class);
    when(second.findElements(any(By.class))).thenReturn(List.of(mock(WebElement.class)));
    SeleniumModel model = new SeleniumModel().setDriver(first);

    assertThat(model.getBrowser().count(Locator.css("a"))).isZero();

    model.setDriver(second);

    assertThat(model.getBrowser().count(Locator.css("a"))).isEqualTo(1);
  }

  @Test
  void shouldKeepTheBrowserThatWasSet() {
    BrowserDriver browser = mock(BrowserDriver.class);
    SeleniumModel model = new SeleniumModel().setDriver(mock(WebDriver.class)).setBrowser(browser);

    assertThat(model.getBrowser()).isSameAs(browser);
  }

  @Test
  void shouldNotBuildABrowserWithoutADriver() {
    assertThat(new SeleniumModel().getBrowser()).isNull();
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldGiveTheSeleniumDriverOfTheSeleniumTool() {
    WebDriver driver = mock(WebDriver.class);

    assertThat(new SeleniumModel().setDriver(driver).getDriver()).isSameAs(driver);
    assertThat(new SeleniumModel().setDriver(driver).setBrowser(mock(BrowserDriver.class)).getDriver()).isSameAs(driver);
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldHaveNoSeleniumDriverBeforeTheBrowserIsOpened() {
    assertThat(new SeleniumModel().getDriver()).isNull();
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldRefuseTheSeleniumDriverWhenAnotherToolOwnsTheBrowser() {
    SeleniumModel model = new SeleniumModel().setBrowser(mock(BrowserDriver.class));

    assertThatThrownBy(model::getDriver)
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("only available with the Selenium tool")
      .hasMessageContaining("getBrowser()");
  }

  @Test
  void shouldPrintAndCompareTheModelOfAnotherToolWithoutAskingForTheSeleniumDriver() {
    BrowserDriver browser = mock(BrowserDriver.class);
    SeleniumModel model = new SeleniumModel().setBrowser(browser).setTestTitle("title");
    SeleniumModel same = new SeleniumModel().setBrowser(browser).setTestTitle("title");

    assertThat(model.toString()).contains("title");
    assertThat(model).isEqualTo(same).hasSameHashCodeAs(same);
  }
}
