package com.almis.awe.builder.enumerates;

/**
 * Side of the anchor where a popover opens
 */
public enum PopoverPosition {
  BOTTOM("bottom"),
  TOP("top"),
  LEFT("left"),
  RIGHT("right");

  private final String value;

  PopoverPosition(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
