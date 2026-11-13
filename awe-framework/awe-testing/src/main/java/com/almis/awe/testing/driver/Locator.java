package com.almis.awe.testing.driver;

import org.openqa.selenium.By;

import java.util.List;
import java.util.Objects;

/**
 * Tool-neutral locator of an element: a CSS selector or an XPath expression, with value semantics.
 *
 * <p>The engine profiles ({@code IAweFrontEndInstructions}) still build Selenium {@link By} instances in 5.0, because
 * their return types are part of the public API. A {@code Locator} is <em>derived</em> from a {@code By} with
 * {@link #from(By)}, so that code that must not depend on Selenium (a future driver port with a Playwright adapter)
 * can work with it. Only the two kinds that the profiles use are supported: CSS and XPath ({@code By.id} is converted
 * to the equivalent CSS id selector).</p>
 *
 * <p><b>Context.</b> A locator is resolved against the page, or against a context element when the caller resolves it
 * from that element (for instance the row of a cell: {@code ancestor-or-self::...}, see
 * {@link #isContextRelative()}). No parent/child composition is modelled: the adapter decides the context.</p>
 *
 * <p><b>Selenium types</b> are confined to {@link #toBy()} and {@link #from(By)}. A later slice of the driver port may
 * move those two to the Selenium adapter, leaving this class free of Selenium.</p>
 */
public final class Locator {

  /** Kind of expression */
  public enum Kind {
    /** CSS selector */
    CSS,
    /** XPath expression */
    XPATH
  }

  private static final String USING_CSS = "css selector";
  private static final String USING_XPATH = "xpath";
  private static final String USING_ID = "id";

  private final Kind kind;
  private final String expression;

  private Locator(Kind kind, String expression) {
    if (expression == null || expression.isBlank()) {
      throw new IllegalArgumentException("The " + kind + " expression of a locator cannot be blank");
    }
    this.kind = kind;
    this.expression = expression;
  }

  /**
   * Create a CSS locator
   *
   * @param expression CSS selector
   * @return Locator
   */
  public static Locator css(String expression) {
    return new Locator(Kind.CSS, expression);
  }

  /**
   * Create an XPath locator
   *
   * @param expression XPath expression
   * @return Locator
   */
  public static Locator xpath(String expression) {
    return new Locator(Kind.XPATH, expression);
  }

  /**
   * Derive a locator from a Selenium {@code By}. It reads the remote parameters of the {@code By}
   * ({@link By.Remotable#getRemoteParameters()}): {@code css selector} and {@code xpath} are kept as they are and
   * {@code id} becomes the CSS id selector, with the id escaped as {@code CSS.escape} does
   *
   * @param by Selenium locator
   * @return Locator
   * @throws IllegalArgumentException If the {@code By} is null or of another kind (name, class name, link text, tag
   *                                  name, chained...), which the profiles do not use
   */
  public static Locator from(By by) {
    if (!(by instanceof By.Remotable remotable)) {
      throw new IllegalArgumentException("Cannot convert to a neutral locator (only css, xpath and id are supported): " + by);
    }
    By.Remotable.Parameters parameters = remotable.getRemoteParameters();
    String value = String.valueOf(parameters.value());
    return switch (parameters.using()) {
      case USING_CSS -> css(value);
      case USING_XPATH -> xpath(value);
      case USING_ID -> css("#" + escapeCssIdentifier(value));
      default -> throw new IllegalArgumentException("Cannot convert to a neutral locator (only css, xpath and id are supported): " + by);
    };
  }

  /**
   * Derive the locators of a list of Selenium locators, keeping the order
   *
   * @param by Selenium locators
   * @return Locators (unmodifiable)
   */
  public static List<Locator> from(List<By> by) {
    return by.stream().map(Locator::from).toList();
  }

  /**
   * Get the kind of expression
   *
   * @return Kind
   */
  public Kind kind() {
    return kind;
  }

  /**
   * Get the expression
   *
   * @return CSS selector or XPath expression
   */
  public String expression() {
    return expression;
  }

  /**
   * Check whether the locator is meant to be resolved from a context element. It is the case of an XPath expression that
   * does not start from the root of the document ({@code ancestor-or-self::...}, {@code ./td}); a CSS selector, or an
   * XPath that starts with {@code /} or {@code //}, is not
   *
   * @return true if the expression is relative to the element it is resolved from
   */
  public boolean isContextRelative() {
    if (kind != Kind.XPATH) {
      return false;
    }
    int start = 0;
    while (start < expression.length() && (expression.charAt(start) == '(' || Character.isWhitespace(expression.charAt(start)))) {
      start++;
    }
    return start >= expression.length() || expression.charAt(start) != '/';
  }

  /**
   * Convert to a Selenium locator
   *
   * @return {@code By.cssSelector} or {@code By.xpath}
   */
  public By toBy() {
    return kind == Kind.CSS ? By.cssSelector(expression) : By.xpath(expression);
  }

  /**
   * Escape a string as a CSS identifier, as {@code CSS.escape} does (CSSOM, "serialize an identifier")
   */
  private static String escapeCssIdentifier(String value) {
    StringBuilder escaped = new StringBuilder();
    int[] codePoints = value.codePoints().toArray();
    for (int i = 0; i < codePoints.length; i++) {
      int c = codePoints[i];
      if (c == 0) {
        escaped.append('�');
      } else if (c <= 0x1F || c == 0x7F
        || (i == 0 && isDigit(c))
        || (i == 1 && isDigit(c) && codePoints[0] == '-')) {
        escaped.append('\\').append(Integer.toHexString(c)).append(' ');
      } else if (i == 0 && c == '-' && codePoints.length == 1) {
        escaped.append("\\-");
      } else if (c >= 0x80 || c == '-' || c == '_' || isDigit(c) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
        escaped.appendCodePoint(c);
      } else {
        escaped.append('\\').appendCodePoint(c);
      }
    }
    return escaped.toString();
  }

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9';
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Locator locator && kind == locator.kind && expression.equals(locator.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, expression);
  }

  @Override
  public String toString() {
    return (kind == Kind.CSS ? "css=" : "xpath=") + expression;
  }
}
