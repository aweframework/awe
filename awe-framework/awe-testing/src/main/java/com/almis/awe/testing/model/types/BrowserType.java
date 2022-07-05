package com.almis.awe.testing.model.types;

public enum BrowserType {
  /**
   * Chrome browser in local host
   */
  CHROME("chrome"),
  /**
   * Edge browser in local host
   */
  EDGE("edge"),
  /**
   * Firefox browser in local host
   */
  FIREFOX("firefox"),
  /**
   * Internet Explorer browser in local host
   */
  IE("IE"),
  /**
   * Opera browser in local host
   */
  OPERA("opera"),
  /**
   * Headless Firefox browser
   */
  HEADLESS_FIREFOX("headless-firefox"),
  /**
   * Headless Chrome browser
   */
  HEADLESS_CHROME("headless-chrome"),
  /**
   * Headless Firefox browser
   */
  REMOTE_FIREFOX("headless-firefox"),
  /**
   * Chrome browser in remote host
   */
  REMOTE_CHROME("remote-chrome"),
  /**
   * Firefox browser in docker container as service
   */
  SERVICE_FIREFOX("service-firefox"),
  /**
   * Chrome browser in docker container as service
   */
  SERVICE_CHROME("service-chrome"),
  /**
   * Edge browser in docker container as service
   */
  SERVICE_EDGE("service-edge"),
  /**
   * Opera browser in docker container as service
   */
  SERVICE_OPERA("service-opera"),
  /**
   * Safari browser in docker container as service
   */
  SERVICE_SAFARI("service-safari");

  private final String name;

  /**
   * Browser type enum
   * @param name Browser name
   */
  BrowserType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
