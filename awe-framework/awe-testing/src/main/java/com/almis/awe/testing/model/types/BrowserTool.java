package com.almis.awe.testing.model.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Browser automation tool that runs the tests, chosen with {@code awe.test.tool}
 */
public enum BrowserTool {
  /**
   * Selenium WebDriver
   */
  SELENIUM("selenium"),
  /**
   * Playwright (pilot): runs Chromium and Firefox locally, and has no remote or service browsers yet
   */
  PLAYWRIGHT("playwright");

  private final String name;

  /**
   * Browser tool enum
   *
   * @param name Name of the tool, as it is written in the configuration
   */
  BrowserTool(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Get a tool from its configured name, ignoring the case and the surrounding blanks
   *
   * @param value Configured name
   * @return Tool
   * @throws IllegalArgumentException If there is no such tool. The message lists the supported ones
   */
  public static BrowserTool fromName(String value) {
    String wanted = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
      .filter(tool -> tool.name.equals(wanted))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown test tool '%s' in awe.test.tool. Supported tools: %s",
        value, supportedNames())));
  }

  /**
   * Get the names of the supported tools
   *
   * @return Names, separated by commas
   */
  public static String supportedNames() {
    return Arrays.stream(values()).map(BrowserTool::getName).collect(Collectors.joining(", "));
  }
}
