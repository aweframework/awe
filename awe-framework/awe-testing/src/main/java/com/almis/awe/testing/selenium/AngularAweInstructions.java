package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public class AngularAweInstructions implements IAweInstructions {
  // Constants
  private static final String PARENT_ELEMENT = "')]/..";
  private static final String TEXT_VALUE = " text: '";
  private static final String CELL_SEARCH_CONTAINS_XPATH = "//*[contains(@class,'ui-grid-row')]//*[contains(@class,'ui-grid-cell-contents')]//text()[contains(.,'";
  private static final String SELECT_DROP_CONTENTS_XPATH = "//*[@id='select2-drop']//*[contains(@class,'select2-result-label')]//text()[contains(.,'";
  private static final String DAY = "day";
  private static final String MONTH = "month";
  private static final String YEAR = "year";
  private static final By DATEPICKER = By.cssSelector(".datepicker");
  private static final By GRID_LOADER_SELECTOR = By.cssSelector(".grid-loader");
  private static final By SELECT_DROP_INPUT = By.cssSelector("#select2-drop input.select2-input");
  private static final By SELECT_DROP_INPUT_NOT_HIDDEN = By.cssSelector("#select2-drop :not(.select2-search-hidden) input.select2-input");
  private static final By SELECT_DROP_RESULTS = By.cssSelector("#select2-drop .select2-results li");
  private static final ExpectedCondition<Boolean> GRID_LOADER_IS_NOT_VISIBLE = invisibilityOfElementLocated(GRID_LOADER_SELECTOR);
  private static final ExpectedCondition<Boolean> LOADER_IS_NOT_VISIBLE = invisibilityOfElementLocated(By.cssSelector(".loader"));

  private SeleniumModel seleniumModel;

  public WebDriver getDriver() {
    return this.seleniumModel.getDriver();
  }

  public IAweInstructions setSeleniumModel(SeleniumModel seleniumModel) {
    this.seleniumModel = seleniumModel;
    return this;
  }
}
