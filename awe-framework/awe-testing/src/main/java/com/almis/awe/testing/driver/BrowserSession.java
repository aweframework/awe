package com.almis.awe.testing.driver;

import java.nio.file.Path;

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

  /**
   * Tell the session that a test of a class starts. A session that collects evidence of its own (the trace and the video
   * of the Playwright tool) uses it to delimit the test; the others ignore it. It must never fail
   *
   * @param testClass Name of the test class
   * @param testName  Name of the test
   */
  default void testStarted(String testClass, String testName) {
    // Nothing to collect by default
  }

  /**
   * Tell the session that the test that started has ended
   *
   * @param testName     Name of the test
   * @param evidenceName Base name of the evidence of the test (without extension), the one of its screenshot
   * @param failed       Whether the test failed
   */
  default void testFinished(String testName, String evidenceName, boolean failed) {
    // Nothing to collect by default
  }

  /**
   * Set who is told about the evidence that the session stores, so that it is announced in the test output
   *
   * @param listener Listener of the stored evidence
   */
  default void onEvidence(EvidenceListener listener) {
    // Nothing to announce by default
  }

  /**
   * Listener of the evidence files that a session stores
   */
  @FunctionalInterface
  interface EvidenceListener {

    /**
     * An evidence file has been stored
     *
     * @param label          What it is (Failure trace, Failure video...)
     * @param file           Stored file
     * @param attachToReport Whether it belongs to the test that is running, so that its report can attach it
     */
    void stored(String label, Path file, boolean attachToReport);
  }
}
