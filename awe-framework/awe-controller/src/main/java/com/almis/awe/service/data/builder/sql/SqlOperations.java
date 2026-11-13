package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Coalesce;
import com.querydsl.core.types.dsl.Expressions;

import java.util.Date;

/**
 * Builds the expression of an operation from its already resolved operands. Used by {@code SQLBuilder}; it is not an
 * extension point.
 * <p>
 * Date additions are typed as {@code java.util.Date}, as QueryDSL date operations always were in AWE: the type
 * decides how the JDBC driver reads the result, so it is kept until it is validated on every database.
 */
public final class SqlOperations {

  private SqlOperations() {
  }

  /**
   * Build an operation expression
   *
   * @param operator Operator ({@code CONCAT}, {@code ADD}, {@code ADD_DAYS}...)
   * @param operands Operands
   * @return Operation expression, or null when the operator is not supported
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Expression operation(String operator, Expression... operands) {
    Expression minusOne = Expressions.constant(-1);
    Expression result;
    switch (operator) {
      case "CONCAT":
        result = null;
        for (Expression operand : operands) {
          result = result == null ? operand : Expressions.stringOperation(Ops.CONCAT, result, operand);
        }
        return result;
      case "REPLACE":
        return Expressions.stringTemplate("replace({0},{1},{2})", operands[0], operands[1], operands[2]);
      case "SUBSTRING":
        // Uses QueryDSL native substring ops — semantics match Java String.substring().
        // 2-arg form: SUBSTRING(source, beginIndex)         → Ops.SUBSTR_1ARG
        // 3-arg form: SUBSTRING(source, beginIndex, endIndex) → Ops.SUBSTR_2ARGS
        // Both are 0-based, beginIndex inclusive, endIndex exclusive.
        if (operands.length == 2) {
          return Expressions.stringOperation(Ops.SUBSTR_1ARG, operands[0], operands[1]);
        }
        return Expressions.stringOperation(Ops.SUBSTR_2ARGS, operands[0], operands[1], operands[2]);
      case "NULLIF":
        return Expressions.simpleOperation(Object.class, Ops.NULLIF, operands);
      case "COALESCE":
        return new Coalesce(operands);
      case "ADD":
      case "SUB":
      case "MULT":
      case "DIV":
      case "MOD":
        result = null;
        for (Expression operand : operands) {
          result = result == null ? operand
            : Expressions.numberOperation(Long.class, Ops.valueOf(operator), result, operand);
        }
        return result;
      case "ADD_SECONDS":
      case "ADD_MINUTES":
      case "ADD_HOURS":
      case "ADD_DAYS":
      case "ADD_WEEKS":
      case "ADD_MONTHS":
      case "ADD_YEARS":
        return Expressions.dateOperation(Date.class, Ops.DateTimeOps.valueOf(operator), operands);
      case "DIFF_SECONDS":
      case "DIFF_MINUTES":
      case "DIFF_HOURS":
      case "DIFF_DAYS":
      case "DIFF_WEEKS":
      case "DIFF_MONTHS":
      case "DIFF_YEARS":
        return Expressions.dateOperation(Integer.class, Ops.DateTimeOps.valueOf(operator), operands);
      case "SUB_SECONDS":
      case "SUB_MINUTES":
      case "SUB_HOURS":
      case "SUB_DAYS":
      case "SUB_WEEKS":
      case "SUB_MONTHS":
      case "SUB_YEARS":
        String addOperator = operator.replace("SUB", "ADD");
        return Expressions.dateOperation(Date.class, Ops.DateTimeOps.valueOf(addOperator), operands[0],
          Expressions.numberOperation(Integer.class, Ops.MULT, operands[1], minusOne));
      case "ROUND":
        Operator roundType = operands.length == 1 ? Ops.MathOps.ROUND : Ops.MathOps.ROUND2;
        return Expressions.numberOperation(Long.class, roundType, operands);
      case "POWER":
        return Expressions.numberOperation(Long.class, Ops.MathOps.valueOf(operator), operands);
      default:
        return null;
    }
  }
}
