package com.almis.awe.model.type;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Closed list of the operators of a grid column filter condition
 */
public enum ColumnFilterOperator {
  EQ("eq", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  NE("ne", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  CONTAINS("contains", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  NOT_CONTAINS("notContains", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  STARTS_WITH("startsWith", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  ENDS_WITH("endsWith", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  NOT_STARTS_WITH("notStartsWith", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  NOT_ENDS_WITH("notEndsWith", true, ColumnFilterType.VALUES, ColumnFilterType.TEXT),
  GT("gt", true, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  GE("ge", true, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  LT("lt", true, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  LE("le", true, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  BETWEEN("between", true, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  EMPTY("empty", false, ColumnFilterType.VALUES, ColumnFilterType.TEXT, ColumnFilterType.NUMBER, ColumnFilterType.DATE),
  NOT_EMPTY("notEmpty", false, ColumnFilterType.VALUES, ColumnFilterType.TEXT, ColumnFilterType.NUMBER, ColumnFilterType.DATE);

  private final String code;
  private final boolean valueRequired;
  private final Set<ColumnFilterType> types;

  ColumnFilterOperator(String code, boolean valueRequired, ColumnFilterType first, ColumnFilterType... rest) {
    this.code = code;
    this.valueRequired = valueRequired;
    this.types = EnumSet.of(first, rest);
  }

  /**
   * Retrieve the code of the operator in the request
   *
   * @return Operator code
   */
  public String getCode() {
    return code;
  }

  /**
   * Whether the operator compares with a value
   *
   * @return The condition needs a value
   */
  public boolean isValueRequired() {
    return valueRequired;
  }

  /**
   * Whether the operator applies to a filter type
   *
   * @param type Filter type
   * @return The operator applies to the type
   */
  public boolean appliesTo(ColumnFilterType type) {
    return types.contains(type);
  }

  /**
   * Retrieve the operator of a code
   *
   * @param code Operator code
   * @return Operator, empty when the code is not one of the operators
   */
  public static Optional<ColumnFilterOperator> fromCode(String code) {
    return Arrays.stream(values()).filter(operator -> operator.code.equals(code)).findFirst();
  }
}
