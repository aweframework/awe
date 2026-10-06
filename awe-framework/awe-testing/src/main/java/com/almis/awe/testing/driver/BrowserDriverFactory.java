package com.almis.awe.testing.driver;

import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserTool;

import java.io.IOException;
import java.util.Objects;

/**
 * Opens the browser of a test run with the automation tool that {@code awe.test} selects ({@code awe.test.tool}). The
 * JUnit extension only knows this seam: a new tool is one more implementation, chosen in {@link #forTool(BrowserTool)}.
 */
public interface BrowserDriverFactory {

  /**
   * Open the browser the properties of the model ask for and publish it in the model, which is where the steps read it
   * from ({@link SeleniumModel#getBrowser()})
   *
   * @param model    Model of the test run. Its properties choose the browser, size and remote services
   * @param testName Name of the test that asks for the browser (the Docker browsers name their recording after it)
   * @return Session to close when the tests have finished
   * @throws IOException If the address of a remote browser is not valid
   */
  BrowserSession create(SeleniumModel model, String testName) throws IOException;

  /**
   * Get the factory of a tool
   *
   * @param tool Tool
   * @return Factory
   */
  static BrowserDriverFactory forTool(BrowserTool tool) {
    return switch (Objects.requireNonNull(tool, "The test tool is required")) {
      case SELENIUM -> new SeleniumBrowserDriverFactory();
    };
  }
}
