package com.almis.awe.builder.enumerates;

/**
 * Drawing type of a sparkline column
 *
 * @author pgarcia
 */
public enum SparklineType {

  LINE("line"),
  BAR("bar"),
  AREA("area");

  private final String value;

  SparklineType(String value) {
    this.value = value;
  }

  /**
   * Equals method
   *
   * @param value Value to compare
   * @return Value is the type
   */
  public boolean equalsStr(String value) {
    return this.value.equals(value);
  }

  @Override
  public String toString() {
    return this.value;
  }
}
