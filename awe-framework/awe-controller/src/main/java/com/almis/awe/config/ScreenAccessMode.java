package com.almis.awe.config;

/**
 * How a client launch of a maintain target or a query is checked against the menu options whose screens use it
 */
public enum ScreenAccessMode {
  /**
   * The target is not checked against the screens
   */
  OFF,
  /**
   * A launch without access to any screen using the target is logged and audited, but allowed
   */
  WARN,
  /**
   * A launch without access to any screen using the target is refused
   */
  ENFORCE
}
