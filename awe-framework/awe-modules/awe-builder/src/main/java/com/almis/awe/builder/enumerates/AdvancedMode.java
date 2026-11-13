package com.almis.awe.builder.enumerates;

/**
 * How a window shows its advanced block
 */
public enum AdvancedMode {
  INLINE("inline"),
  SIDE_PANEL("side-panel");

  private final String value;

  AdvancedMode(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
