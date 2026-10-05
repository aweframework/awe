package com.almis.awe.testing.driver;

import java.time.Duration;
import java.util.List;

/**
 * Port of the browser automation that the test utilities need, independent of the tool that runs it (Selenium today).
 *
 * <p>It works with {@link Locator}s that are resolved on every call (no element is kept between calls, so none goes
 * stale) and exposes <em>instant</em> queries: waiting is a poll that the caller does over them. Queries on the first
 * match fail with {@link ElementNotFoundException} when there is none, except {@link #isVisible} and {@link #isEnabled}
 * which answer false. Any operation fails with {@link ElementReplacedException} when the element is replaced while it is
 * used. Internal preview: no compatibility promise yet.</p>
 */
public interface BrowserDriver {

  /**
   * Count the elements that match
   *
   * @param locator Locator
   * @return Number of matches
   */
  int count(Locator locator);

  /**
   * Check whether any element matches
   *
   * @param locator Locator
   * @return true if there is at least one match
   */
  default boolean exists(Locator locator) {
    return count(locator) > 0;
  }

  /**
   * Check whether the first match is displayed
   *
   * @param locator Locator
   * @return true if there is a match and it is displayed
   */
  boolean isVisible(Locator locator);

  /**
   * Check whether the first match is enabled
   *
   * @param locator Locator
   * @return true if there is a match and it is enabled
   */
  boolean isEnabled(Locator locator);

  /**
   * Get the rendered text of the first match
   *
   * @param locator Locator
   * @return Text
   */
  String text(Locator locator);

  /**
   * Get the rendered text of every match
   *
   * @param locator Locator
   * @return Texts, in document order
   */
  List<String> texts(Locator locator);

  /**
   * Get an attribute (or property) of the first match
   *
   * @param locator Locator
   * @param name    Attribute name
   * @return Value, or null if it has none
   */
  String attribute(Locator locator, String name);

  /**
   * Get an attribute of the first match of a locator that is relative to the first match of a context
   * ({@link Locator#isContextRelative()}), for instance the row of a cell
   *
   * @param context  Locator of the context element
   * @param relative Locator, resolved from the context element
   * @param name     Attribute name
   * @return Value, or null if it has none
   */
  String attribute(Locator context, Locator relative, String name);

  /**
   * Get the tag name of the first match
   *
   * @param locator Locator
   * @return Tag name
   */
  String tagName(Locator locator);

  /**
   * Get the text of the selected option of a native select
   *
   * @param locator Locator of the select
   * @return Text of its first selected option
   */
  String selectedOptionText(Locator locator);

  /**
   * Get references to every match
   *
   * @param locator Locator
   * @return References, in document order
   */
  List<ElementRef> elements(Locator locator);

  /**
   * Click on the first match, away from the edges of the viewport
   *
   * @param locator Locator
   */
  void click(Locator locator);

  /**
   * Double click on the first match, as one gesture
   *
   * @param locator Locator
   */
  void doubleClick(Locator locator);

  /**
   * Right click on the first match
   *
   * @param locator Locator
   */
  void contextClick(Locator locator);

  /**
   * Move the pointer over the first match
   *
   * @param locator Locator
   */
  void hover(Locator locator);

  /**
   * Put the pointer over the first match in one jump, instead of travelling to it
   *
   * @param locator Locator
   */
  void hoverInstantly(Locator locator);

  /**
   * Move the pointer from where it is
   *
   * @param dx Horizontal offset in pixels
   * @param dy Vertical offset in pixels
   */
  void moveMouseBy(int dx, int dy);

  /**
   * Click where the pointer is
   */
  void clickAtPointer();

  /**
   * Type text into the first match
   *
   * @param locator Locator
   * @param text    Text
   */
  void type(Locator locator, CharSequence text);

  /**
   * Clear the value of the first match, if it has one
   *
   * @param locator Locator
   */
  void clear(Locator locator);

  /**
   * Press a key on the page
   *
   * @param key Key
   */
  void press(Key key);

  /**
   * Press a key on the first match
   *
   * @param locator Locator
   * @param key     Key
   */
  void press(Locator locator, Key key);

  /**
   * Pause the browser actions
   *
   * @param duration Time to pause
   */
  void pause(Duration duration);
}
