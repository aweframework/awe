package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpressions;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.WindowOver;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps the functions and casts of a query field to QueryDSL expressions. Names are matched ignoring case and
 * independently of the default locale. Used by {@code SQLBuilder}; it is not an extension point.
 * <p>
 * {@code TRUNCDATE} is typed as {@code java.util.Date}, as it always was in AWE: the type decides how the JDBC driver
 * reads the result, so it is kept until it is validated on every database.
 */
public final class SqlFieldFunctions {

  private SqlFieldFunctions() {
  }

  /**
   * Apply a function to a field expression
   *
   * @param fieldExpression Field expression, may be null when the field could not be resolved
   * @param function        Function
   * @return Expression with the function applied, or empty when the function is not supported
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Optional<Expression<?>> function(Expression fieldExpression, String function) {
    Expression<?> expression = switch (function.toUpperCase(Locale.ROOT)) {
      case "ABS" -> {
        // The field expression may be null when it could not be resolved (buildPath returns null for a
        // null node), so fall back to the same default type as an untyped expression instead of
        // dereferencing it; the aggregate branches below already accept a null expression
        Class expressionClass = fieldExpression == null || fieldExpression.getType().equals(Object.class)
          ? Integer.class : fieldExpression.getType();
        yield Expressions.numberOperation(expressionClass, Ops.MathOps.ABS, fieldExpression);
      }
      case "AVG" -> new WindowOver<>(Double.class, Ops.AggOps.AVG_AGG, fieldExpression);
      case "CNT" -> new WindowOver<>(Long.class, Ops.AggOps.COUNT_AGG, fieldExpression);
      case "CNT_DISTINCT" -> new WindowOver<>(Long.class, Ops.AggOps.COUNT_DISTINCT_AGG, fieldExpression);
      case "LAG" -> SQLExpressions.lag(fieldExpression);
      case "MAX" -> new WindowOver<>(Long.class, Ops.AggOps.MAX_AGG, fieldExpression);
      case "MIN" -> new WindowOver<>(Long.class, Ops.AggOps.MIN_AGG, fieldExpression);
      case "FIRST_VALUE" -> SQLExpressions.firstValue(fieldExpression);
      case "LAST_VALUE" -> SQLExpressions.lastValue(fieldExpression);
      case "TRIM" -> StringExpressions.ltrim(StringExpressions.rtrim(fieldExpression));
      case "TRUNCDATE" -> Expressions.dateOperation(Date.class, Ops.DateTimeOps.DATE, fieldExpression);
      case "LENGTH" -> new WindowOver<>(Integer.class, Ops.STRING_LENGTH, fieldExpression);
      case "SECOND", "MINUTE", "HOUR", "MONTH", "YEAR" -> Expressions.dateOperation(Integer.class,
        Ops.DateTimeOps.valueOf(function.toUpperCase(Locale.ROOT)), fieldExpression);
      case "DAY" -> Expressions.dateOperation(Integer.class, Ops.DateTimeOps.DAY_OF_MONTH, fieldExpression);
      case "ROW_NUMBER" -> SQLExpressions.rowNumber();
      case "RANK" -> SQLExpressions.rank();
      case "SUM" -> new WindowOver<>(Long.class, Ops.AggOps.SUM_AGG, fieldExpression);
      default -> null;
    };
    return Optional.ofNullable(expression);
  }

  /**
   * Apply a cast to a field expression
   *
   * @param fieldExpression Field expression
   * @param cast            Cast
   * @return Expression with the cast applied, or empty when the cast is not supported
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Optional<Expression<?>> cast(Expression fieldExpression, String cast) {
    Expression<?> expression = switch (cast.toUpperCase(Locale.ROOT)) {
      case "STRING" -> Expressions.stringOperation(Ops.STRING_CAST, fieldExpression);
      case "LONG" -> Expressions.asString(fieldExpression).castToNum(Long.class);
      case "FLOAT" -> Expressions.asString(fieldExpression).castToNum(Float.class);
      case "DOUBLE" -> Expressions.asString(fieldExpression).castToNum(Double.class);
      case "INTEGER" -> Expressions.asString(fieldExpression).castToNum(Integer.class);
      default -> null;
    };
    return Optional.ofNullable(expression);
  }
}
