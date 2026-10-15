package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlFieldFunctions}
 */
class SqlFieldFunctionsTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final Expression<?> COLUMN = new PathBuilder<>(Object.class, "t").get("a");

  @ParameterizedTest(name = "{0} renders {1}")
  @CsvSource(delimiter = '|', value = {
    "SUM|sum(t.a)",
    "cnt|count(t.a)",
    "Cnt_Distinct|count(distinct t.a)",
    "TRIM|ltrim(rtrim(t.a))",
    "TRUNCDATE|convert(t.a, date)",
    "LENGTH|length(t.a)",
    "DAY|extract(day from t.a)",
    "year|extract(year from t.a)",
    "ROW_NUMBER|row_number()"
  })
  @DisplayName("A function is matched ignoring case and renders its SQL")
  void functionRendersItsSql(String function, String sql) {
    assertThat(SqlFieldFunctions.function(COLUMN, function)).get().extracting(SqlFieldFunctionsTest::sql)
      .isEqualTo(sql);
  }

  @Test
  @DisplayName("TRUNCDATE is a java.util.Date expression")
  void truncDateIsUtilDate() {
    assertThat(SqlFieldFunctions.function(COLUMN, "TRUNCDATE")).get()
      .extracting(Expression::getType).isEqualTo(java.util.Date.class);
  }

  @Test
  @DisplayName("ABS over an untyped or a null field is an Integer expression")
  void absOverUntypedOrNullField() {
    assertThat(SqlFieldFunctions.function(COLUMN, "ABS")).get()
      .extracting(Expression::getType).isEqualTo(Integer.class);
    assertThat(SqlFieldFunctions.function(null, "ABS")).get()
      .extracting(Expression::getType).isEqualTo(Integer.class);
  }

  @Test
  @DisplayName("An unknown function or cast has no expression")
  void unknownFunctionOrCast() {
    assertThat(SqlFieldFunctions.function(COLUMN, "COUNT")).isEmpty();
    assertThat(SqlFieldFunctions.cast(COLUMN, "DECIMAL")).isEmpty();
  }

  @ParameterizedTest(name = "{0} casts to {1}")
  @CsvSource({
    "STRING,String",
    "long,Long",
    "Float,Float",
    "DOUBLE,Double",
    "integer,Integer"
  })
  @DisplayName("A cast is matched ignoring case and has the cast type")
  void castHasItsType(String cast, String type) {
    assertThat(SqlFieldFunctions.cast(COLUMN, cast)).get()
      .extracting(expression -> expression.getType().getSimpleName()).isEqualTo(type);
  }

  private static String sql(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.toString();
  }
}
