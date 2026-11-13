package com.almis.awe.model.dto;

import com.almis.awe.model.type.ColumnFilterOperator;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Condition of a grid column filter: an operator of the closed list and its value ({@code to} closes a between).
 * Number values are kept as plain decimal text and date values as web dates ({@code dd/MM/yyyy}).
 */
@Data
@AllArgsConstructor
public class ColumnFilterCondition {

  private ColumnFilterOperator operator;
  private String value;
  private String to;
}
