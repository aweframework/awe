package com.almis.awe.testing.driver;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

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

  /**
   * Open a page
   *
   * @param url Url
   */
  void open(String url);

  /**
   * Check whether the page has finished loading (an instant query: the caller polls it)
   *
   * @return true if the document is complete
   */
  boolean isPageLoaded();

  /**
   * Set how long a script may run before it is given up
   *
   * @param timeout Timeout
   */
  void setScriptTimeout(Duration timeout);

  /**
   * Run a script in the page
   *
   * @param script Script body; it reads its arguments from {@code arguments}
   * @param args   Arguments (strings, numbers, booleans)
   * @return What the script returns
   * @throws UnsupportedOperationException If the browser cannot run scripts
   */
  Object executeScript(String script, Object... args);

  /**
   * Run a script in the page on the first match, which is {@code arguments[0]}; the arguments follow
   *
   * @param locator Locator
   * @param script  Script body
   * @param args    Arguments, from {@code arguments[1]}
   * @return What the script returns
   * @throws UnsupportedOperationException If the browser cannot run scripts
   */
  Object executeScriptOn(Locator locator, String script, Object... args);

  /**
   * Scroll the content of the first match
   *
   * @param locator Locator
   * @param x       Horizontal position in pixels
   * @param y       Vertical position in pixels
   */
  void scrollTo(Locator locator, int x, int y);

  /**
   * Bring the first match to the center of the viewport. It is a help for what comes next: when the browser cannot
   * script it does nothing, as the action that follows scrolls on its own
   *
   * @param locator Locator
   */
  void scrollToCenter(Locator locator);

  /**
   * Run something inside a frame, and come back to the page afterwards, even if it fails
   *
   * @param frame Locator of the frame
   * @param body  What to run, with the frame as the current page
   */
  void inFrame(Locator frame, Runnable body);

  /**
   * Take a screenshot of the page
   *
   * @return PNG image, or empty if the browser cannot take screenshots
   */
  Optional<byte[]> screenshot();

  /**
   * Get the source of the page
   *
   * @return Source
   */
  String pageSource();

  /**
   * Get the entries of the browser console
   *
   * @return Entries, or none if the browser does not expose its console (Firefox, remote drivers...)
   */
  List<ConsoleEntry> consoleEntries();

  /**
   * Resize the browser window
   *
   * @param width  Width in pixels
   * @param height Height in pixels
   */
  void setWindowSize(int width, int height);

  /**
   * Move the browser window
   *
   * @param x Horizontal position in pixels
   * @param y Vertical position in pixels
   */
  void setWindowPosition(int x, int y);

  /**
   * Close the browser and release the driver
   */
  void quit();
}
