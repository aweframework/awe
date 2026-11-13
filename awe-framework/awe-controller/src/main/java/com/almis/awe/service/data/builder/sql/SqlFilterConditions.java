package com.almis.awe.service.data.builder.sql;

import com.almis.awe.model.type.UnionType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.NullExpression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleOperation;
import com.querydsl.sql.SQLQuery;

import java.util.List;

/**
 * Builds the boolean condition of a filter from its already resolved operands, and joins the conditions of a filter
 * group. Used by {@code SQLBuilder}; it is not an extension point.
 */
public final class SqlFilterConditions {

  private SqlFilterConditions() {
  }

  /**
   * Build the condition of a filter
   *
   * @param filterCondition Filter condition, in lower case ({@code eq}, {@code in}, {@code like}...)
   * @param ignoreCase      Compare ignoring case
   * @param firstOperand    First operand
   * @param secondOperand   Second operand
   * @return Filter condition
   * @throws IllegalArgumentException The condition is not a QueryDSL operator
   */
  public static BooleanOperation condition(String filterCondition, boolean ignoreCase, Expression<?> firstOperand,
                                           Expression<?> secondOperand) {
    boolean inCondition = secondOperand instanceof SimpleOperation || secondOperand instanceof SQLQuery;
    return switch (filterCondition) {
      case "ge" -> Expressions.booleanOperation(Ops.GOE, firstOperand, secondOperand);
      case "le" -> Expressions.booleanOperation(Ops.LOE, firstOperand, secondOperand);
      case "in" -> in(inCondition, firstOperand, secondOperand);
      case "not in" -> Expressions.booleanOperation(Ops.NOT, in(inCondition, firstOperand, secondOperand));
      case "is null" -> Expressions.booleanOperation(Ops.IS_NULL, firstOperand);
      case "is not null" -> Expressions.booleanOperation(Ops.IS_NOT_NULL, firstOperand);
      case "like" -> like(ignoreCase, firstOperand, secondOperand);
      case "not like" -> Expressions.booleanOperation(Ops.NOT, like(ignoreCase, firstOperand, secondOperand));
      case "exists" -> exists(firstOperand, secondOperand);
      case "not exists" -> Expressions.booleanOperation(Ops.NOT, exists(firstOperand, secondOperand));
      case "eq" -> eq(ignoreCase, firstOperand, secondOperand);
      case "ne" -> Expressions.booleanOperation(Ops.NOT, eq(ignoreCase, firstOperand, secondOperand));
      default -> Expressions.booleanOperation(Ops.valueOf(filterCondition.toUpperCase()), firstOperand, secondOperand);
    };
  }

  /**
   * Join the conditions of a filter group
   *
   * @param conditions Conditions, in order
   * @param union      Union of the group ({@code AND} joins with and, anything else with or)
   * @return Joined condition, or null when there are no conditions
   */
  public static BooleanExpression join(List<BooleanExpression> conditions, String union) {
    BooleanExpression result = null;
    for (BooleanExpression condition : conditions) {
      if (result == null) {
        result = condition;
      } else if (UnionType.AND.toString().equalsIgnoreCase(union)) {
        result = result.and(condition);
      } else {
        result = result.or(condition);
      }
    }
    return result;
  }

  private static BooleanOperation eq(boolean ignoreCase, Expression<?> firstOperand, Expression<?> secondOperand) {
    if (secondOperand instanceof NullExpression) {
      return Expressions.booleanOperation(Ops.IS_NULL, firstOperand);
    } else if (ignoreCase) {
      return Expressions.booleanOperation(Ops.EQ_IGNORE_CASE, firstOperand, secondOperand);
    } else {
      return Expressions.booleanOperation(Ops.EQ, firstOperand, secondOperand);
    }
  }

  private static BooleanOperation in(boolean inCondition, Expression<?> firstOperand, Expression<?> secondOperand) {
    if (secondOperand instanceof NullExpression) {
      return Expressions.booleanOperation(Ops.IS_NULL, firstOperand);
    } else if (inCondition) {
      return Expressions.booleanOperation(Ops.IN, firstOperand, secondOperand);
    } else {
      return Expressions.booleanOperation(Ops.EQ, firstOperand, secondOperand);
    }
  }

  private static BooleanOperation like(boolean ignoreCase, Expression<?> firstOperand, Expression<?> secondOperand) {
    return Expressions.booleanOperation(ignoreCase ? Ops.LIKE_IC : Ops.LIKE, firstOperand, secondOperand);
  }

  private static BooleanOperation exists(Expression<?> firstOperand, Expression<?> secondOperand) {
    Expression<?> operand = firstOperand instanceof SQLQuery ? firstOperand : secondOperand;
    return Expressions.booleanOperation(Ops.EXISTS, operand);
  }
}
