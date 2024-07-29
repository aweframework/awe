package com.almis.awe.builder.client.css;

/**
 * Add classes action builder
 *
 * @author pgarcia
 */
public class ToggleCssClassActionBuilder extends CssClassActionBuilder<ToggleCssClassActionBuilder> {

  private static final String TYPE = "toggle-class";

  /**
   * Empty constructor
   */
  public ToggleCssClassActionBuilder() {
    super(TYPE);
  }

  /**
   * Constructor with an option
   *
   * @param cssSelector CSS selector
   * @param classes     Classes to add
   */
  public ToggleCssClassActionBuilder(String cssSelector, String... classes) {
    super(TYPE, cssSelector, classes);
  }
}
