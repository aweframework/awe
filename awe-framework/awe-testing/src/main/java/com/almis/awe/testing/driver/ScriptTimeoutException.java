package com.almis.awe.testing.driver;

import java.time.Duration;

/**
 * A script did not finish within the script timeout (a page whose script thread is blocked never answers). Tool-neutral
 * port exception that the Playwright adapter throws, as Selenium fails a step with its own script timeout. Internal
 * preview of the driver port: no compatibility promise yet.
 */
public class ScriptTimeoutException extends RuntimeException {

  /**
   * Create the exception
   *
   * @param timeout Script timeout that was exceeded
   * @param cause   Exception of the automation tool
   */
  public ScriptTimeoutException(Duration timeout, Throwable cause) {
    super("The script did not finish within " + timeout.toMillis() + " ms", cause);
  }
}
