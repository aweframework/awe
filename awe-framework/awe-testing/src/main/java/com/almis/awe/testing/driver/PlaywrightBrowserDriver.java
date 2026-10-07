package com.almis.awe.testing.driver;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.MouseButton;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Playwright adapter of the {@link BrowserDriver} port (pilot). It gives every operation the semantics of the Selenium
 * adapter, which the browser tests were tuned with, on top of what Playwright offers:
 *
 * <ul>
 *   <li><b>Instant queries.</b> Playwright waits for elements in most of its calls (up to 30 seconds), but the port is
 *   polled by its caller. Queries are evaluated in the page in one round trip ({@code evalOnSelectorAll}, which does
 *   not wait) and a missing element is {@link ElementNotFoundException} at once.</li>
 *   <li><b>Gestures.</b> They are pointer and keyboard events sent at the coordinates of the element, as the Selenium
 *   actions are, so Playwright's own actionability checks (and their waits) do not take part: a click reaches what lies
 *   at the center of the element. The pauses and the scrolls that precede a gesture are the ones of the Selenium
 *   adapter. An element that has no box (it was removed or hidden) is {@link ElementReplacedException}.</li>
 *   <li><b>Locators.</b> CSS is the {@code css=} engine and XPath the {@code xpath=} engine of Playwright. A CSS
 *   selector also pierces open shadow roots, which Selenium does not.</li>
 *   <li><b>Scripts.</b> A script (and the load check and the console refresh, which run one) is given up with a
 *   {@link ScriptTimeoutException} when the page does not answer within {@link #setScriptTimeout(Duration)} (thirty
 *   seconds by default, as in Selenium), so a page with a blocked script thread fails the step instead of hanging the
 *   run. The console refresh waits two seconds at most and then returns what it has. The other queries are not
 *   bounded. A script runs once, even when it navigates the page (changing the location or submitting a form): it then
 *   returns null, because what it returned went with the page. <b>A script that timed out may still be running in the
 *   page</b>: the browser cannot be told to stop it, and the page stays blocked until it ends. One that had not begun
 *   (the page was busy with something else) is not started later. The page and the adapter talk through a function
 *   named {@code __aweTestingScript} and an identity ({@code __aweTestingDocument}) that Playwright installs in each
 *   document of the page. {@link #executeScriptOn(Locator, String, Object...)} finds its element in the page with the
 *   DOM ({@code querySelector} or {@code document.evaluate}), so it does not pierce shadow roots.</li>
 * </ul>
 *
 * <p>Differences that Playwright imposes: {@link #setWindowPosition(int, int)} has no equivalent (a page has a viewport,
 * not a window on a screen), so it is logged and ignored; {@link #quit()} closes the context of the page and the factory session closes the browser;
 * {@link #isVisible(Locator)} follows the {@code isDisplayed} rule of Selenium, not the one of Playwright (see
 * {@link #IS_SHOWN}), {@link #isRendered(Locator)} is the same rule without the opacity and {@link #isEnabled(Locator)} adds {@code aria-disabled} to the {@code disabled} property; the
 * text of an element that is not shown is empty, as Selenium reports it.</p>
 */
@Slf4j
public class PlaywrightBrowserDriver implements BrowserDriver {

  private static final int CLICK_PAUSE_MILLIS = 100;
  private static final int TYPE_PAUSE_MILLIS = 200;
  // What a user holds the right button for, at the very least: browsers fire contextmenu when it is pressed
  private static final int CONTEXT_PRESS_MILLIS = 100;
  private static final int FRAMES_GIVE_UP_MILLIS = 500;
  private static final String VALUE = "value";
  private static final String BACKSPACE = "Backspace";
  // Selenium gives a script thirty seconds unless it is told otherwise
  private static final Duration DEFAULT_SCRIPT_TIMEOUT = Duration.ofSeconds(30);
  // How long the console waits for the page to deliver its latest messages. A page that is stuck must not hold back the
  // evidence of a failure
  private static final Duration CONSOLE_FLUSH_TIMEOUT = Duration.ofSeconds(2);
  // A click on the last pixels of the viewport is lost by the browser: an element closer than this to an edge is brought
  // to the center
  private static final int VIEWPORT_EDGE_MARGIN_PX = 60;
  // A gesture waits for the element to stop moving (a modal that scales in, a group of the menu that expands) and to receive
  // the pointer at its center (not clipped nor covered): both must hold in this many frames in a row. It does not wait longer
  // than the cap, so an element that never stops (an endless animation) or is always covered still gets its gesture, where it
  // is at that moment
  private static final int STABLE_FRAMES = 3;
  private static final int STABLE_BOX_CAP_MILLIS = 1500;
  // Playwright polls the function that it waits for: a function that returns an object is truthy at the first run
  private static final double SCRIPT_POLLING_MILLIS = 100;
  // The page tells the adapter what happens to a script through this function, which Playwright installs in every document of
  // the page (it survives the navigations). The calls do not wait for an answer: a script that waits for anything before it
  // runs cannot be given up on time, because Playwright has to reach the page to cancel it
  private static final String SCRIPT_BINDING = "__aweTestingScript";
  private static final String STARTED = "started";
  private static final String RESULT = "result";
  private static final String MISSING = "missing";
  private static final String DOCUMENT = "document";
  // Gives every document that the page loads an identity. A script is bound to the document that it was asked to run in:
  // Playwright runs again what it waits for in the document that replaces it (the page navigated), and there the script
  // must not run a second time
  private static final String DOCUMENT_ID_SCRIPT = "window.__aweTestingDocument = Math.random().toString(36).slice(2) + Date.now().toString(36);";
  private static final String NOTIFY = "const notify = (phase, value) => { try { window." + SCRIPT_BINDING
    + "(phase, request.id, value === undefined ? null : value).catch(() => {}); } catch (error) { } };";
  private static final String CURRENT_DOCUMENT = "(window.__aweTestingDocument || null)";
  // Reads the identity of the document that a script is going to run in
  private static final String DOCUMENT_QUERY = "request => { " + NOTIFY + " notify('" + DOCUMENT + "', " + CURRENT_DOCUMENT + "); return {done: true}; }";
  // Runs a script, whose body reads its arguments from {@code arguments}. It does not run when its time is over (the page was
  // busy and the caller gave up) or when it is in another document than the one that was asked for. The element that the
  // script is about, if any, is its first argument
  private static final String SCRIPT_PREFIX = "request => { " + NOTIFY
    + " if (Date.now() > request.deadline || " + CURRENT_DOCUMENT + " !== (request.document || null)) { return {skipped: true}; }"
    + " notify('" + STARTED + "');"
    + " let args = request.args;"
    + " if (request.target) {"
    + " const target = request.target;"
    + " const element = target.xpath ? document.evaluate(target.expression, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue"
    + " : document.querySelector(target.expression);"
    + " if (!element) { notify('" + MISSING + "'); return {done: true}; }"
    + " args = [element].concat(args); }"
    + " const value = (function() {";
  private static final String SCRIPT_SUFFIX = "\n}).apply(null, args);"
    + " notify('" + RESULT + "', value); return {done: true}; }";
  // The script is started again when the document changed between the moment that its identity was read and the moment
  // that it ran, which only happens while the page is navigating
  private static final int SCRIPT_ATTEMPTS = 3;

  // Functions of the element that the page evaluates. The scrolls are instant: a smooth one would still be moving when the
  // box of the element is read
  // An element inside a list with its own scroll (the options of a select) is first shown inside that list, or a click
  // on it would reach what lies below the list. Then, if it is close to an edge of the viewport, it goes to the center
  private static final String SCROLL_AWAY_FROM_EDGE = "e => { e.scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'instant'});"
    + "const rect = e.getBoundingClientRect();"
    + "if (rect.top < " + VIEWPORT_EDGE_MARGIN_PX + " || rect.bottom > window.innerHeight - " + VIEWPORT_EDGE_MARGIN_PX + ") {"
    + "e.scrollIntoView({block: 'center', inline: 'nearest', behavior: 'instant'});} }";
  // Resolves when, in STABLE_FRAMES animation frames in a row, the box of the element was the same and the point at its center
  // went to the element or to something inside it (what Playwright calls "receives events"), or when the cap is over. The
  // second part matters for an element that does not move but is not where the pointer would land yet: clipped by a container
  // that is still expanding (the submenu of a panel menu) or covered by something that is about to go away. The point is the
  // center of the box, also when another part of the element is free: the gesture goes there, so it is what has to be
  // reachable. An element with no area cannot be hit and is only waited for to stop, and a point outside the viewport is not
  // judged. The point is looked up in the document of the element (a frame has its own); the answer is whether it was ready
  // (false when the cap was over). A page that does not run animation frames (it is hidden) is given up by the timer
  private static final String WAIT_UNTIL_READY = "(e, limits) => new Promise(resolve => {"
    + "const read = () => { const r = e.getBoundingClientRect(); return [r.x, r.y, r.width, r.height]; };"
    + "const receives = box => { if (box[2] <= 0 || box[3] <= 0) { return true; }"
    + "const x = box[0] + box[2] / 2; const y = box[1] + box[3] / 2;"
    + "let hit = e.ownerDocument.elementFromPoint(x, y); if (!hit) { return true; }"
    + "while (hit.shadowRoot) { const inner = hit.shadowRoot.elementFromPoint(x, y); if (!inner || inner === hit) { break; } hit = inner; }"
    // The hit is the element when it or one of its ancestors is, also across shadow roots (a shadow root has no parent node,
    // its host is the way up), so a web component that renders its own content is not waited for
    + "for (let n = hit; n; n = n.parentNode || n.host || null) { if (n === e) { return true; } } return false; };"
    + "let last = read(); let ready = 0;"
    + "const timer = setTimeout(() => resolve(false), limits.cap);"
    + "const frame = () => { const box = read();"
    + "ready = box.every((value, index) => value === last[index]) && receives(box) ? ready + 1 : 0; last = box;"
    + "if (ready >= limits.frames) { clearTimeout(timer); resolve(true); } else { requestAnimationFrame(frame); } };"
    + "requestAnimationFrame(frame); })";
  // An element can be inside the viewport but clipped by a container with its own scroll
  private static final String SCROLL_NEAREST = "e => e.scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'instant'})";
  // A suggest panel opens aligned to its search box, so a scroll after that would leave it misplaced
  private static final String SCROLL_TO_CENTER = "e => e.scrollIntoView({block: 'center', inline: 'nearest', behavior: 'instant'})";
  /**
   * Function of the page that tells whether an element is shown, with the rule of the {@code isDisplayed} atom of Selenium
   * (the one that the browser tests were tuned with) instead of the one of Playwright, which needs a box that is not empty:
   * the element must be rendered ({@code display}), not {@code visibility:hidden} and not transparent (its own opacity and
   * the one of its ancestors), and it must have a size or, if it has none, something inside it that has one (a child
   * element, or a text), unless it hides its overflow. The wrapper of the pinned columns of a tree grid has a width and
   * no height around floated cells, and is shown for Selenium and not for Playwright.
   */
  static final String IS_SHOWN = shownFunction(true);
  /**
   * Function of the page that tells whether an element is still rendered: the rule of {@link #IS_SHOWN} without its opacity
   * check. An element that fades in has opacity 0 in its first frames, and it is already there and covers what is under
   * it, so a wait for it to be gone must not take it as gone
   */
  static final String IS_RENDERED = shownFunction(false);
  // Rendered text, as Selenium reports it: empty when the element is not shown
  private static final String TEXT = "e => ((" + IS_SHOWN + ")(e) ? "
    + "(e.innerText !== undefined ? e.innerText : e.textContent) : '')";
  // As Selenium does: the property when the element has one (the current value of an input), otherwise the attribute;
  // a boolean property is 'true' or nothing
  private static final String ATTRIBUTE = "(e, name) => { const v = e[name];"
    + "if (typeof v === 'boolean') { return v ? 'true' : null; }"
    + "if (v !== undefined && v !== null && typeof v !== 'object' && typeof v !== 'function') { return String(v); }"
    + "return e.getAttribute(name); }";
  private static final String TAG_NAME = "e => e.tagName.toLowerCase()";
  private static final String SELECTED_OPTION_TEXT = "e => { const options = e.selectedOptions;"
    + "return options && options.length ? options[0].text : null; }";
  // States that the page answers when it looks for an element of a context
  private static final String NO_CONTEXT = "noContext";
  private static final String NO_RELATIVE = "noRelative";
  private static final String FOUND = "found";
  // An element of the context is searched from the context element, as Selenium does: an absolute XPath still starts
  // from the root of the document
  private static final String RELATIVE_ATTRIBUTE = "(context, request) => { const found = request.xpath"
    + " ? context.ownerDocument.evaluate(request.expression, context, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue"
    + " : context.querySelector(request.expression);"
    + "return found ? {state: '" + FOUND + "', value: (" + ATTRIBUTE + ")(found, request.name)} : {state: '" + NO_RELATIVE + "'}; }";
  private static final String FILE_INPUT = "e => e.tagName === 'INPUT' && e.type === 'file'";
  static final String IS_CONNECTED = "e => e.isConnected";
  // Playwright hands out a new handle on each query: the identity of an element is stamped on it
  private static final String ELEMENT_ID = "e => { const key = Symbol.for('awe.testing.elementId');"
    + "if (!e[key]) { Object.defineProperty(e, key, {value: Math.random().toString(36).slice(2) + Date.now().toString(36)}); }"
    + "return e[key]; }";

  private final Page page;
  private final Queue<ConsoleEntry> console = new ConcurrentLinkedQueue<>();
  // Document that queries and scripts run in: the page, or a frame while inFrame runs
  private Frame current;
  // Where the pointer is. Playwright does not tell, and the pointer moves by offsets and clicks where it is
  private double pointerX;
  private double pointerY;
  private Duration scriptTimeout = DEFAULT_SCRIPT_TIMEOUT;
  // What the page told about the scripts that are waiting for it: those that it started, those whose element is not there
  // and what they handed over (the identity of a document, or the result of a script)
  private final Set<String> pendingScripts = ConcurrentHashMap.newKeySet();
  private final Set<String> startedScripts = ConcurrentHashMap.newKeySet();
  private final Set<String> missingScripts = ConcurrentHashMap.newKeySet();
  private final Map<String, Optional<Object>> scriptResults = new ConcurrentHashMap<>();

  /**
   * Create the adapter and start collecting the console of the page
   *
   * @param page Playwright page
   */
  public PlaywrightBrowserDriver(Page page) {
    this.page = page;
    this.current = page.mainFrame();
    page.onConsoleMessage(message -> console.add(new ConsoleEntry(System.currentTimeMillis(), levelOf(message.type()), message.text())));
    // The uncaught exceptions are in the browser log of Selenium, as severe entries
    page.onPageError(error -> console.add(new ConsoleEntry(System.currentTimeMillis(), "SEVERE", error)));
    page.exposeFunction(SCRIPT_BINDING, this::scriptBinding);
    page.addInitScript(DOCUMENT_ID_SCRIPT);
  }

  /**
   * Convert a neutral key to the name that Playwright gives it
   *
   * @param key Neutral key
   * @return Playwright key name
   */
  static String toPlaywright(Key key) {
    return switch (key) {
      case ESCAPE -> "Escape";
      case BACK_SPACE -> BACKSPACE;
      case DELETE -> "Delete";
      case ENTER -> "Enter";
      case TAB -> "Tab";
      case ARROW_UP -> "ArrowUp";
      case ARROW_DOWN -> "ArrowDown";
      case ARROW_LEFT -> "ArrowLeft";
      case ARROW_RIGHT -> "ArrowRight";
    };
  }

  /**
   * Check whether Playwright failed because the element was replaced or the document went away while it was used
   *
   * @param exc Exception of Playwright
   * @return true if the element should be looked up again
   */
  static boolean isReplaced(PlaywrightException exc) {
    String message = exc.getMessage() == null ? "" : exc.getMessage().toLowerCase(Locale.ROOT);
    return message.contains("not attached to the dom")
      || message.contains("execution context was destroyed")
      || message.contains("cannot find context")
      || message.contains("element is detached");
  }

  private static String shownFunction(boolean checkOpacity) {
    return "e => {"
      + "if (!e.checkVisibility({visibilityProperty: true, opacityProperty: " + checkOpacity + "})) { return false; }"
      + "const sized = (node) => {"
      + "const rect = node.getBoundingClientRect();"
      + "if (rect.width > 0 && rect.height > 0) { return true; }"
      + "const style = getComputedStyle(node);"
      + "if (style.overflowX === 'hidden' && style.overflowY === 'hidden') { return false; }"
      + "for (const child of node.childNodes) {"
      + "if (child.nodeType === Node.ELEMENT_NODE) { if (sized(child)) { return true; } }"
      + "else if (child.nodeType === Node.TEXT_NODE) {"
      + "const range = document.createRange(); range.selectNodeContents(child);"
      + "const box = range.getBoundingClientRect(); if (box.width > 0 && box.height > 0) { return true; } } }"
      + "return false; };"
      + "return sized(e); }";
  }

  @Override
  public int count(Locator locator) {
    return guard(locator, () -> current.locator(selector(locator)).count());
  }

  @Override
  public boolean isVisible(Locator locator) {
    try {
      // Evaluated in the page, which does not wait, and false when nothing matches
      return Boolean.TRUE.equals(guard(locator, () -> current.evalOnSelectorAll(selector(locator),
        "elements => elements.length > 0 && (" + IS_SHOWN + ")(elements[0])")));
    } catch (ElementReplacedException exc) {
      return false;
    }
  }

  @Override
  public boolean isRendered(Locator locator) {
    try {
      return Boolean.TRUE.equals(guard(locator, () -> current.evalOnSelectorAll(selector(locator),
        "elements => elements.length > 0 && (" + IS_RENDERED + ")(elements[0])")));
    } catch (ElementReplacedException exc) {
      return false;
    }
  }

  @Override
  public boolean isEnabled(Locator locator) {
    try {
      return withHandle(locator, ElementHandle::isEnabled);
    } catch (ElementNotFoundException | ElementReplacedException exc) {
      return false;
    }
  }

  @Override
  public String text(Locator locator) {
    return ((String) evalFirst(locator, TEXT, null)).strip();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> texts(Locator locator) {
    return guard(locator, () -> ((List<String>) current.evalOnSelectorAll(selector(locator), "elements => elements.map(" + TEXT + ")")).stream()
      .map(String::strip)
      .toList());
  }

  @Override
  public String attribute(Locator locator, String name) {
    return (String) evalFirst(locator, ATTRIBUTE, name);
  }

  @Override
  public String attribute(Locator context, Locator relative, String name) {
    Map<String, Object> request = Map.of("xpath", relative.kind() == Locator.Kind.XPATH, "expression", relative.expression(), "name", name);
    Object result = guard(context, () -> current.evalOnSelectorAll(selector(context),
      "(elements, request) => elements.length === 0 ? {state: '" + NO_CONTEXT + "'} : (" + RELATIVE_ATTRIBUTE + ")(elements[0], request)", request));
    Map<?, ?> answer = (Map<?, ?>) result;
    if (NO_CONTEXT.equals(answer.get("state"))) {
      throw new ElementNotFoundException(context, null);
    }
    if (NO_RELATIVE.equals(answer.get("state"))) {
      throw new ElementNotFoundException(relative, null);
    }
    return (String) answer.get("value");
  }

  @Override
  public String tagName(Locator locator) {
    return (String) evalFirst(locator, TAG_NAME, null);
  }

  @Override
  public String selectedOptionText(Locator locator) {
    Object text = evalFirst(locator, SELECTED_OPTION_TEXT, null);
    if (text == null) {
      throw new ElementNotFoundException(locator, new PlaywrightException("No option is selected"));
    }
    return String.valueOf(text).strip();
  }

  @Override
  public List<ElementRef> elements(Locator locator) {
    return guard(locator, () -> {
      List<ElementRef> refs = new ArrayList<>();
      for (ElementHandle handle : current.querySelectorAll(selector(locator))) {
        refs.add(new PlaywrightElementRef(handle, locator, (String) handle.evaluate(ELEMENT_ID)));
      }
      return refs;
    });
  }

  @Override
  public void click(Locator locator) {
    act(locator, handle -> {
      runScript(handle, SCROLL_AWAY_FROM_EDGE);
      pointAt(locator, handle);
      mouse().down();
      mouse().up();
      pause(CLICK_PAUSE_MILLIS);
    });
  }

  @Override
  public void doubleClick(Locator locator) {
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      BoundingBox center = pointAt(locator, handle);
      // One gesture: the browser counts the second press as part of the same double click however long the machine takes
      mouse().dblclick(center.x, center.y);
      pause(CLICK_PAUSE_MILLIS);
    });
  }

  @Override
  public void contextClick(Locator locator) {
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      pointAt(locator, handle);
      // Chromium and Firefox (macOS, Linux) fire contextmenu on press, not on release. A client may show its menu a few
      // frames after the handler (an animation), behind a mask that closes it on mouseup: pressing and releasing at once
      // lands the release on that mask and closes the menu that was just opened. A user releases when the page has
      // rendered the menu under the pointer, and so does this: it waits for the page to render, and holds the button for
      // at least what a user does
      mouse().down(new Mouse.DownOptions().setButton(MouseButton.RIGHT));
      try {
        waitForRenderedFrames();
        pause(CONTEXT_PRESS_MILLIS);
      } finally {
        // Whatever happens while it is held (the page navigates, the frame goes away), the button is never left pressed
        mouse().up(new Mouse.UpOptions().setButton(MouseButton.RIGHT));
      }
      pause(CLICK_PAUSE_MILLIS);
    });
  }

  @Override
  public void hover(Locator locator) {
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      pointAt(locator, handle);
      pause(CLICK_PAUSE_MILLIS);
    });
  }

  @Override
  public void hoverInstantly(Locator locator) {
    // The pointer of Playwright always jumps (it travels only when asked for steps), so there is no trip over what lies on
    // the way and no pause either
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      pointAt(locator, handle);
    });
  }

  @Override
  public void moveMouseBy(int dx, int dy) {
    movePointer(pointerX + dx, pointerY + dy);
    pause(CLICK_PAUSE_MILLIS);
  }

  @Override
  public void clickAtPointer() {
    mouse().down();
    mouse().up();
    pause(CLICK_PAUSE_MILLIS);
  }

  @Override
  public void type(Locator locator, CharSequence text) {
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      // Selenium clicks the element to give it the focus, and then types
      focusWithClick(locator, handle);
      page.keyboard().type(text.toString());
      pause(TYPE_PAUSE_MILLIS);
    });
  }

  @Override
  public void sendKeys(Locator locator, CharSequence... text) {
    String joined = String.join("", Arrays.stream(text).map(CharSequence::toString).toList());
    act(locator, handle -> {
      if (Boolean.TRUE.equals(handle.evaluate(FILE_INPUT))) {
        // A file input takes the path of the file to upload
        handle.setInputFiles(Path.of(joined));
      } else {
        handle.focus();
        page.keyboard().type(joined);
      }
    });
  }

  @Override
  public void clear(Locator locator) {
    String value = attribute(locator, VALUE);
    if (value != null && !value.isEmpty()) {
      // The checks of the action are not waited for: Selenium does not wait either
      act(locator, handle -> handle.fill("", new ElementHandle.FillOptions().setForce(true)));
      // One backspace more than the characters of the value, as the front end tests have always sent
      act(locator, handle -> {
        handle.focus();
        for (int index = -1; index < value.length(); index++) {
          page.keyboard().press(BACKSPACE);
        }
      });
    }
  }

  @Override
  public void press(Key key) {
    page.keyboard().press(toPlaywright(key));
  }

  @Override
  public void press(Locator locator, Key key) {
    act(locator, handle -> {
      runScript(handle, SCROLL_NEAREST);
      focusWithClick(locator, handle);
      page.keyboard().press(toPlaywright(key));
    });
  }

  @Override
  public void pause(Duration duration) {
    pause(duration.toMillis());
  }

  @Override
  public void open(String url) {
    page.navigate(url);
  }

  @Override
  public boolean isPageLoaded() {
    return "complete".equals(executeScript("return document.readyState"));
  }

  @Override
  public void setScriptTimeout(Duration timeout) {
    Objects.requireNonNull(timeout, "The script timeout is required");
    if (timeout.isZero() || timeout.isNegative()) {
      // Playwright takes a timeout of zero as no timeout at all
      throw new IllegalArgumentException("The script timeout must be positive: " + timeout);
    }
    this.scriptTimeout = timeout;
  }

  /**
   * Get how long a script may run before it is given up
   *
   * @return Script timeout
   */
  Duration scriptTimeout() {
    return scriptTimeout;
  }

  @Override
  public Object executeScript(String script, Object... args) {
    return normalize(runScript(scriptTimeout, script, null, Arrays.asList(args)));
  }

  @Override
  public Object executeScriptOn(Locator locator, String script, Object... args) {
    return normalize(runScript(scriptTimeout, script, locator, Arrays.asList(args)));
  }

  @Override
  public void scrollTo(Locator locator, int x, int y) {
    evalFirst(locator, "(e, position) => e.scrollTo(position[0], position[1])", List.of(x, y));
  }

  @Override
  public void scrollToCenter(Locator locator) {
    act(locator, handle -> runScript(handle, SCROLL_TO_CENTER));
  }

  @Override
  public void inFrame(Locator frame, Runnable body) {
    Frame target = withHandle(frame, ElementHandle::contentFrame);
    if (target == null) {
      throw new ElementNotFoundException(frame, new PlaywrightException("The element is not a frame"));
    }
    current = target;
    try {
      body.run();
    } finally {
      // Back to the top document, as Selenium does: not to the frame that was current before
      current = page.mainFrame();
    }
  }

  @Override
  public Optional<byte[]> screenshot() {
    try {
      return Optional.ofNullable(page.screenshot());
    } catch (Exception exc) {
      // Evidence of a failure must not fail the test again
      log.debug("Could not take a screenshot", exc);
      return Optional.empty();
    }
  }

  @Override
  public String pageSource() {
    return current.content();
  }

  @Override
  public List<ConsoleEntry> consoleEntries() {
    try {
      // Playwright delivers the events of the page while one of its calls is waiting: make sure none is pending, without
      // waiting for a page that does not answer
      runScript(CONSOLE_FLUSH_TIMEOUT, "return 0;", null, List.of());
    } catch (Exception exc) {
      log.debug("The browser console could not be refreshed: the entries that arrived are returned", exc);
    }
    List<ConsoleEntry> entries = new ArrayList<>();
    ConsoleEntry entry = console.poll();
    while (entry != null) {
      entries.add(entry);
      entry = console.poll();
    }
    return entries;
  }

  @Override
  public void setWindowSize(int width, int height) {
    page.setViewportSize(width, height);
  }

  @Override
  public void setWindowPosition(int x, int y) {
    // The page of Playwright has a viewport, not a window on a screen
    log.debug("A Playwright page has no window position: ignoring the position {},{}", x, y);
  }

  @Override
  public void quit() {
    page.context().close();
  }

  /**
   * Run a script in the current document, once, and give up when the page does not answer in time. Playwright has no
   * timeout for an evaluation, and a page whose script thread is blocked never answers it; {@code waitForFunction}, in
   * which the driver enforces the timeout whatever the page does, does. But it runs the function again in the new
   * document when the page navigates (a script that changes the location, or submits a form), and it cannot hand over a
   * result that the navigation destroys. So the script is bound to the identity of the document that it was asked to run
   * in (it does nothing in another one) and the page hands its result over through a binding, without waiting for an
   * answer, before the function returns.
   *
   * @param timeout   Time to wait
   * @param script    Body of the script, which reads its arguments from {@code arguments}
   * @param target    Element that is the first argument of the script, or null
   * @param arguments Arguments of the script
   * @return What the script returned, or null if the page navigated while it ran
   * @throws ElementNotFoundException If the element of the script is not there
   * @throws ScriptTimeoutException   If the page did not answer within the timeout
   */
  private Object runScript(Duration timeout, String script, Locator target, List<Object> arguments) {
    for (int attempt = 0; attempt < SCRIPT_ATTEMPTS; attempt++) {
      String id = UUID.randomUUID().toString();
      Map<String, Object> request = new HashMap<>();
      request.put("id", id);
      request.put("document", documentOfTheScript(timeout));
      // Playwright does not take a long: the page reads the deadline as a number of milliseconds since the epoch
      request.put("deadline", (double) (System.currentTimeMillis() + timeout.toMillis()));
      request.put("args", arguments);
      if (target != null) {
        request.put("target", Map.of("xpath", target.kind() == Locator.Kind.XPATH, "expression", target.expression()));
      }
      pendingScripts.add(id);
      try {
        waitFor(timeout, SCRIPT_PREFIX + script + SCRIPT_SUFFIX, request, () -> startedScripts.contains(id));
        if (startedScripts.contains(id)) {
          if (missingScripts.contains(id)) {
            throw new ElementNotFoundException(target, null);
          }
          Optional<Object> result = scriptResults.get(id);
          return result == null ? null : result.orElse(null);
        }
      } finally {
        forget(id);
      }
    }
    throw new IllegalStateException("The page kept changing while the script was started: it did not run");
  }

  /**
   * Read the identity of the document that a script is going to run in
   */
  private String documentOfTheScript(Duration timeout) {
    String id = UUID.randomUUID().toString();
    pendingScripts.add(id);
    try {
      waitFor(timeout, DOCUMENT_QUERY, Map.of("id", id), () -> false);
      Optional<Object> document = scriptResults.get(id);
      return document == null ? null : (String) document.orElse(null);
    } finally {
      forget(id);
    }
  }

  /**
   * Wait for a function that returns an object, so that it is truthy at the first run
   *
   * @param started Tells whether the function began to run, so that the page that goes away while it runs is not a failure
   */
  private void waitFor(Duration timeout, String function, Map<String, Object> request, BooleanSupplier started) {
    JSHandle answer = null;
    try {
      answer = current.waitForFunction(function, request,
        new Frame.WaitForFunctionOptions().setTimeout(timeout.toMillis()).setPollingInterval(SCRIPT_POLLING_MILLIS));
    } catch (TimeoutError exc) {
      throw new ScriptTimeoutException(timeout, exc);
    } catch (PlaywrightException exc) {
      if (!isReplaced(exc)) {
        throw exc;
      }
      // The page that was running it is gone. If it had begun, the script ran, and what it returned went with the page
      log.debug("The page navigated while the script was waited for (started: {})", started.getAsBoolean(), exc);
    } finally {
      dispose(answer);
    }
  }

  private void forget(String id) {
    pendingScripts.remove(id);
    startedScripts.remove(id);
    missingScripts.remove(id);
    scriptResults.remove(id);
  }

  /**
   * Receive what the page tells about a script that waits for it
   */
  private Object scriptBinding(Object... arguments) {
    String id = (String) arguments[1];
    // A script that nobody waits for any longer leaves nothing behind
    if (pendingScripts.contains(id)) {
      switch ((String) arguments[0]) {
        case STARTED -> startedScripts.add(id);
        case MISSING -> missingScripts.add(id);
        default -> scriptResults.put(id, Optional.ofNullable(arguments.length > 2 ? arguments[2] : null));
      }
    }
    return null;
  }

  private static String selector(Locator locator) {
    return (locator.kind() == Locator.Kind.CSS ? "css=" : "xpath=") + locator.expression();
  }

  private static String levelOf(String type) {
    return switch (type) {
      case "error" -> "SEVERE";
      case "warning" -> "WARNING";
      default -> "INFO";
    };
  }

  private static Object normalize(Object value) {
    // Selenium answers whole numbers as long
    return value instanceof Integer number ? Long.valueOf(number) : value;
  }

  private Mouse mouse() {
    return page.mouse();
  }

  /**
   * Wait until the page has rendered two frames, so what a handler scheduled for the next frame is on the screen. A page
   * that renders no frames (a hidden one) does not stop the wait for more than half a second
   */
  private void waitForRenderedFrames() {
    current.evaluate("() => new Promise(resolve => { const giveUp = setTimeout(resolve, " + FRAMES_GIVE_UP_MILLIS + ");"
      + "requestAnimationFrame(() => requestAnimationFrame(() => { clearTimeout(giveUp); resolve(); })); })");
  }

  private void pause(long millis) {
    page.waitForTimeout(millis);
  }

  private void movePointer(double x, double y) {
    mouse().move(x, y);
    pointerX = x;
    pointerY = y;
  }

  /**
   * Move the pointer to the center of an element that has stopped moving and receives the pointer there. The wait runs in
   * the page; the box that the pointer goes to is then read from Playwright, which gives it in the coordinates of the page
   * also when the element is in a frame
   */
  private BoundingBox pointAt(Locator locator, ElementHandle handle) {
    boundingBoxOf(locator, handle);
    if (!Boolean.TRUE.equals(handle.evaluate(WAIT_UNTIL_READY, Map.of("frames", STABLE_FRAMES, "cap", STABLE_BOX_CAP_MILLIS)))) {
      log.debug("The element did not stop or did not receive the pointer in {} ms: going on as it is {}", STABLE_BOX_CAP_MILLIS, locator);
    }
    BoundingBox box = boundingBoxOf(locator, handle);
    BoundingBox center = new BoundingBox();
    center.x = box.x + box.width / 2;
    center.y = box.y + box.height / 2;
    movePointer(center.x, center.y);
    return center;
  }

  private static BoundingBox boundingBoxOf(Locator locator, ElementHandle handle) {
    BoundingBox box = handle.boundingBox();
    if (box == null) {
      // The element has no box: it was removed or hidden after it was found
      throw new ElementReplacedException(locator, new PlaywrightException("The element is not displayed"));
    }
    return box;
  }

  private void focusWithClick(Locator locator, ElementHandle handle) {
    pointAt(locator, handle);
    mouse().down();
    mouse().up();
  }

  private void runScript(ElementHandle handle, String script) {
    try {
      handle.evaluate(script);
    } catch (PlaywrightException exc) {
      // The action itself finds the element again and fails if it has gone
      log.debug("Could not scroll the element before acting on it", exc);
    }
  }

  /**
   * Evaluate a function of the first match in the page, without waiting for it
   */
  private Object evalFirst(Locator locator, String function, Object argument) {
    Object result = guard(locator, () -> current.evalOnSelectorAll(selector(locator),
      "(elements, argument) => elements.length === 0 ? null : {value: (" + function + ")(elements[0], argument)}", argument));
    if (result == null) {
      throw new ElementNotFoundException(locator, null);
    }
    return ((Map<?, ?>) result).get("value");
  }

  private void act(Locator locator, java.util.function.Consumer<ElementHandle> action) {
    withHandle(locator, handle -> {
      action.accept(handle);
      return null;
    });
  }

  /**
   * Run an operation on a handle of the first match, which is released afterwards
   */
  private <T> T withHandle(Locator locator, Function<ElementHandle, T> operation) {
    return guard(locator, () -> {
      ElementHandle handle = current.querySelector(selector(locator));
      if (handle == null) {
        throw new ElementNotFoundException(locator, null);
      }
      try {
        return operation.apply(handle);
      } finally {
        dispose(handle);
      }
    });
  }

  private static void dispose(JSHandle handle) {
    if (handle == null) {
      return;
    }
    try {
      handle.dispose();
    } catch (PlaywrightException exc) {
      log.debug("Could not release the element", exc);
    }
  }

  /**
   * Run an operation of the page, mapping the Playwright exceptions to the neutral ones
   */
  private static <T> T guard(Locator locator, Supplier<T> operation) {
    try {
      return operation.get();
    } catch (PlaywrightException exc) {
      if (isReplaced(exc)) {
        throw new ElementReplacedException(locator, exc);
      }
      throw exc;
    }
  }
}
