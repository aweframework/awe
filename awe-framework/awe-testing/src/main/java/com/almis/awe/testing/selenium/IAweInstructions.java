package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.WebDriver;

public interface IAweInstructions {
  /**
   * Get the Selenium driver
   *
   * @return Selenium driver
   * @throws UnsupportedOperationException When the tests run with a tool other than Selenium ({@code awe.test.tool})
   * @deprecated Selenium specific: it is only available when the tests run with the Selenium tool. Use the steps of
   * {@code SeleniumUtilities}, or its {@code getBrowser()} with a {@code Locator}. It stays available through the whole
   * 5.x line and is not removed before 6.0
   */
  @Deprecated
  WebDriver getDriver();
  IAweInstructions setSeleniumModel(SeleniumModel seleniumModel);

}
