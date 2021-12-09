package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.WebDriver;

public class ReactAweInstructions implements IAweInstructions {
  private SeleniumModel seleniumModel;

  public WebDriver getDriver() {
    return this.seleniumModel.getDriver();
  }

  public IAweInstructions setSeleniumModel(SeleniumModel seleniumModel) {
    this.seleniumModel = seleniumModel;
    return this;
  }
}
