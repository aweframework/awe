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
  /**
   * Row being edited. Only the React client renders it: it can edit a row that is not selected
   */
  public static final String EDITING = "data-editing";
  /**
   * Row marked to be deleted when the grid is saved. Only the React client renders it
   */
  public static final String DELETED = "data-deleted";
  public static final String ACTIVE = "data-active";
  public static final String DISABLED = "data-disabled";
  public static final String OUTSIDE_MONTH = "data-outside-month";
  public static final String OPEN = "data-open";
  public static final String LOADING = "data-loading";
  public static final String TYPE = "data-type";
  public static final String CONTAINER = "data-container";
  public static final String ICON = "data-icon";
  /**
   * Value of a control whose text repeats it, such as the page size of a grid. Only the React client renders it
   */
  public static final String VALUE = "data-value";
  /**
   * The component drew its content, such as a chart. Only the React client renders it
   */
  public static final String RENDERED = "data-rendered";
  /**
   * Number of a wizard step, which the step may replace with an icon. Only the React client renders it
   */
  public static final String STEP_NUMBER = "data-step-number";

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
