package com.almis.awe.model.type;

import java.util.Arrays;
import java.util.Optional;

/**
 * Type of the filter of a grid column (attribute {@code filter} of a column and {@code type} of a column filter)
 */
public enum ColumnFilterType {
  VALUES("values"),
  TEXT("text"),
  NUMBER("number"),
  DATE("date");

  private final String code;

  ColumnFilterType(String code) {
    this.code = code;
  }

  /**
   * Retrieve the code of the type in the request and the screen XML
   *
   * @return Type code
   */
  public String getCode() {
    return code;
  }

  /**
   * Retrieve the type of a code
   *
   * @param code Type code
   * @return Type, empty when the code is not one of the types
   */
  public static Optional<ColumnFilterType> fromCode(String code) {
    return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
  }
}
