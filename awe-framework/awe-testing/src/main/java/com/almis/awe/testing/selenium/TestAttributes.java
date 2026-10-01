package com.almis.awe.testing.selenium;

/**
 * Names of the attributes that carry the test hooks. State is exposed as a data attribute whose value is
 * {@code "true"} or {@code "false"}, so tests do not depend on library state classes.
 *
 * <p>Each constant mirrors the entry of the JavaScript constant {@code TestAttributes} (awe-client-angular,
 * {@code js/awe/data/testIds.js}) whose name is the camel case of the constant name. {@code TestIdsVocabularyTest}
 * fails if one of them drifts from the JavaScript vocabulary.</p>
 */
public final class TestAttributes {

  public static final String TEST_ID = "data-testid";
  public static final String OWNER = "data-testid-owner";
  public static final String SELECTED = "data-selected";
  public static final String ACTIVE = "data-active";
  public static final String DISABLED = "data-disabled";
  public static final String OUTSIDE_MONTH = "data-outside-month";
  public static final String OPEN = "data-open";
  public static final String LOADING = "data-loading";
  public static final String TYPE = "data-type";
  public static final String CONTAINER = "data-container";
  public static final String ICON = "data-icon";

  private TestAttributes() {
    // Constants only
  }

  /**
   * Css selector of the elements whose state attribute has a value
   *
   * @param attribute Attribute name
   * @param value     Attribute value
   * @return Css selector, for instance {@code [data-selected='true']}
   */
  public static String css(String attribute, Object value) {
    return "[" + attribute + "='" + value + "']";
  }

  /**
   * Xpath condition of the elements whose state attribute has a value
   *
   * @param attribute Attribute name
   * @param value     Attribute value
   * @return Xpath condition, for instance {@code @data-selected='true'}
   */
  public static String xpath(String attribute, Object value) {
    return "@" + attribute + "='" + value + "'";
  }
}
