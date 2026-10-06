package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@code getDriver()} is Selenium specific: with another tool, which is a browser without a Selenium driver behind it, it
 * says so instead of returning null. Every way to ask for the driver behaves the same.
 */
@SuppressWarnings("deprecation")
class SeleniumUtilitiesToolTest {

  private static SeleniumModel otherToolModel() {
    return new SeleniumModel().setBrowser(mock(BrowserDriver.class));
  }

  private static SeleniumUtilities facade(SeleniumModel model) {
    SeleniumUtilities utilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    return utilities;
  }

  @Test
  void theFacadeGivesTheSeleniumDriverWithTheSeleniumTool() {
    WebDriver driver = mock(WebDriver.class);

    assertThat(facade(new SeleniumModel().setDriver(driver)).getDriver()).isSameAs(driver);
  }

  @Test
  void theFacadeRefusesTheSeleniumDriverWithAnotherTool() {
    SeleniumUtilities utilities = facade(otherToolModel());

    assertThatThrownBy(utilities::getDriver)
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("only available with the Selenium tool")
      .hasMessageContaining("getBrowser()");
  }

  @Test
  void theAngularProfileRefusesTheSeleniumDriverWithAnotherTool() {
    IAweInstructions profile = new AngularAweInstructions().setSeleniumModel(otherToolModel());

    assertThatThrownBy(profile::getDriver).isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("only available with the Selenium tool");
  }

  @Test
  void theReactProfileRefusesTheSeleniumDriverWithAnotherTool() {
    IAweInstructions profile = new ReactAweInstructions().setSeleniumModel(otherToolModel());

    assertThatThrownBy(profile::getDriver).isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("only available with the Selenium tool");
  }

  @Test
  void theProfilesGiveTheSeleniumDriverWithTheSeleniumTool() {
    WebDriver driver = mock(WebDriver.class);
    SeleniumModel model = new SeleniumModel().setDriver(driver);

    assertThat(new AngularAweInstructions().setSeleniumModel(model).getDriver()).isSameAs(driver);
    assertThat(new ReactAweInstructions().setSeleniumModel(model).getDriver()).isSameAs(driver);
  }
}
