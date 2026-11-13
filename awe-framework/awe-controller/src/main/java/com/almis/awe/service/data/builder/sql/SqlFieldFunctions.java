package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.TemplateFactory;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLServerTemplates;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.WindowOver;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Maps the functions and casts of a query field to QueryDSL expressions. Names are matched ignoring case and
 * independently of the default locale. Used by {@code SQLBuilder}; it is not an extension point.
 * <p>
 * {@code TRUNCDATE} is typed as {@code java.util.Date}, as it always was in AWE: the type decides how the JDBC driver
 * reads the result, so it is kept until it is validated on every database.
 * <p>
 * {@code STRING_AGG} has no portable SQL, so its template depends on the database: the templates of the query factory
 * choose it. Its separator is written into the template as a string literal escaped by those templates.
 * <p>
 * The functions that join values ({@link #SEPARATED_FUNCTIONS}) accept the separator of the field; today only
 * {@code STRING_AGG}.
 */
public final class SqlFieldFunctions {

  /**
   * Separator of the values joined by a function when the field does not set one
   */
  public static final String DEFAULT_SEPARATOR = ", ";

  /**
   * Functions that join values and accept the separator of the field. A new function that needs a separator only adds
   * itself here
   */
  public static final Set<String> SEPARATED_FUNCTIONS = Set.of("STRING_AGG");

  private SqlFieldFunctions() {
  }

  /**
   * Apply a function to a field expression, with the SQL of the generic templates
   *
   * @param fieldExpression Field expression, may be null when the field could not be resolved
   * @param function        Function
   * @return Expression with the function applied, or empty when the function is not supported
   */
  @SuppressWarnings("rawtypes")
  public static Optional<Expression<?>> function(Expression fieldExpression, String function) {
    return function(fieldExpression, function, null);
  }

  /**
   * Apply a function to a field expression, with the default separator
   *
   * @param fieldExpression Field expression, may be null when the field could not be resolved
   * @param function        Function
   * @param templates       Templates of the database, which choose the SQL of {@code STRING_AGG}; null for the generic one
   * @return Expression with the function applied, or empty when the function is not supported
   */
  @SuppressWarnings("rawtypes")
  public static Optional<Expression<?>> function(Expression fieldExpression, String function, SQLTemplates templates) {
    return function(fieldExpression, function, templates, null);
  }

  /**
   * Returns if a function joins values and accepts a separator ({@link #SEPARATED_FUNCTIONS}), ignoring case
   *
   * @param function Function, may be null
   * @return The function accepts a separator
   */
  public static boolean usesSeparator(String function) {
    return function != null && SEPARATED_FUNCTIONS.contains(function.toUpperCase(Locale.ROOT));
  }

  /**
   * Returns if a separator can be used by a function that joins values: any text, the empty one included, without an
   * opening brace, which the QueryDSL templates read as the start of an argument
   *
   * @param separator Separator
   * @return The separator is valid
   */
  public static boolean isValidSeparator(String separator) {
    return separator == null || separator.indexOf('{') < 0;
  }

  /**
   * Apply a function to a field expression
   *
   * @param fieldExpression Field expression, may be null when the field could not be resolved
   * @param function        Function
   * @param templates       Templates of the database, which choose the SQL of {@code STRING_AGG}; null for the generic one
   * @param separator       Separator of the functions that join values, null for {@link #DEFAULT_SEPARATOR}; ignored
   *                        by the other functions
   * @return Expression with the function applied, or empty when the function is not supported
   * @throws IllegalArgumentException The separator is not valid ({@link #isValidSeparator(String)})
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Optional<Expression<?>> function(Expression fieldExpression, String function, SQLTemplates templates,
                                                 String separator) {
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
      case "STRING_AGG" -> new StringAggregation(stringAggregationTemplate(templates, separator), fieldExpression);
      default -> null;
    };
    return Optional.ofNullable(expression);
  }

  /**
   * SQL of {@code STRING_AGG} in a database: the values of the group ordered by value, joined by the separator. HSQLDB,
   * MySQL and MariaDB (and the generic templates) use {@code group_concat}. The separator is a string literal and not a
   * bind parameter, as {@code group_concat} only accepts a literal: the templates of the database escape it (quotes, and
   * backslashes in MySQL and MariaDB).
   *
   * @param templates Templates of the database, null for the generic ones
   * @param separator Separator, null for {@link #DEFAULT_SEPARATOR}
   * @return Template, with the field as its only argument
   */
  private static String stringAggregationTemplate(SQLTemplates templates, String separator) {
    if (!isValidSeparator(separator)) {
      throw new IllegalArgumentException("The separator cannot contain an opening brace: " + separator);
    }
    String value = separator == null ? DEFAULT_SEPARATOR : separator;
    String literal = "'" + (templates == null ? SQLTemplates.DEFAULT : templates).escapeLiteral(value) + "'";
    if (templates instanceof OracleTemplates || templates instanceof H2Templates) {
      return "listagg({0}, " + literal + ") within group (order by {0})";
    } else if (templates instanceof SQLServerTemplates) {
      return "string_agg({0}, " + literal + ") within group (order by {0})";
    } else if (templates instanceof PostgreSQLTemplates) {
      return "string_agg({0}, " + literal + " order by {0})";
    }
    return "group_concat({0} order by {0} separator " + literal + ")";
  }

  /**
   * Values of a group joined into a string ({@code STRING_AGG}): an aggregate, so it cannot be compared in a WHERE
   */
  public static final class StringAggregation extends StringTemplate {

    private StringAggregation(String template, Expression<?> fieldExpression) {
      super(TemplateFactory.DEFAULT.create(template), Collections.singletonList(fieldExpression));
    }
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
