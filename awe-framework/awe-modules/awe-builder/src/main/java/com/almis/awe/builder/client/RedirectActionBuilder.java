package com.almis.awe.builder.client;

/**
 * Redirect action builder
 *
 * @author pgarcia
 */
public class RedirectActionBuilder extends ClientActionBuilder<RedirectActionBuilder> {

  private static final String TYPE = "redirect";

  /**
   * Empty constructor
   */
  public RedirectActionBuilder() {
    setType(TYPE);
  }

  /**
   * Constructor with an option
   *
   * @param url   Redirect URL
   */
  public RedirectActionBuilder(String url) {
    setType(TYPE)
      .setTarget(url);
  }

  /**
   * Constructor with url and new window
   *
   * @param url   Redirect URL
   * @param newWindow Open in new window
   */
  public RedirectActionBuilder(String url, boolean newWindow) {
    setType(TYPE)
      .setTarget(url)
      .addParameter("newWindow", newWindow);
  }
}
