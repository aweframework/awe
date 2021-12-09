package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.WebDriver;

public interface IAweInstructions {
  WebDriver getDriver();
  IAweInstructions setSeleniumModel(SeleniumModel seleniumModel);
}
