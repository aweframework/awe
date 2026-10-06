package com.almis.awe.testing.driver;

/**
 * The browser that a tool opened for a test run, and what is needed to dispose of it
 */
public interface BrowserSession {

  /**
   * Get the browser of the session
   *
   * @return Tool-neutral browser
   */
  BrowserDriver browser();

  /**
   * Dispose of the session: quit the browser and release whatever the tool started to run it
   */
  void close();
}
