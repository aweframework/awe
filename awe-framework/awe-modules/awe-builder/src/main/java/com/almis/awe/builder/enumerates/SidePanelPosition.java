package com.almis.awe.builder.enumerates;

/**
 * Edge of the screen a side panel is anchored to
 */
public enum SidePanelPosition {
  LEFT("left"),
  RIGHT("right");

  private final String value;

  SidePanelPosition(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
