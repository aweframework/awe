package com.almis.awe.model.type;

import java.util.Locale;

/**
 * What the header of a screen with the window template shows
 */
public enum WindowHeaderType {

  /**
   * The menu breadcrumb, ending with the screen title
   */
  BREADCRUMB,
  /**
   * No breadcrumb and no title: only the buttons row
   */
  NONE;

  /**
   * Retrieve the value published to the clients (lowercase)
   *
   * @return Lowercase value
   */
  public String getValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Parse a header value, as written in the screen attribute
   *
   * @param value    Value to parse (case insensitive)
   * @param fallback Header to use when the value is empty or unknown
   * @return Parsed header
   */
  public static WindowHeaderType parse(String value, WindowHeaderType fallback) {
    for (WindowHeaderType type : values()) {
      if (type.getValue().equalsIgnoreCase(value == null ? null : value.trim())) {
        return type;
      }
    }
    return fallback;
  }
}
