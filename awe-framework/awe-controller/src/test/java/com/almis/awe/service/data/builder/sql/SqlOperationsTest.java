package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlOperations}
 */
class SqlOperationsTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final Expression<?> A = new PathBuilder<>(Object.class, "t").get("a");
  private static final Expression<?> B = new PathBuilder<>(Object.class, "t").get("b");
  private static final Expression<?> C = new PathBuilder<>(Object.class, "t").get("c");

  @Test
  @DisplayName("CONCAT and arithmetic operators chain every operand from the left")
  void chainedOperators() {
    assertThat(sql(SqlOperations.operation("CONCAT", A, B, C))).isEqualTo("t.a || t.b || t.c");
    assertThat(sql(SqlOperations.operation("ADD", A, B, C))).isEqualTo("t.a + t.b + t.c");
    assertThat(sql(SqlOperations.operation("SUB", A, B))).isEqualTo("t.a - t.b");
    assertThat(sql(SqlOperations.operation("MULT", A, B))).isEqualTo("t.a * t.b");
    assertThat(SqlOperations.operation("ADD", A).getType()).isEqualTo(Object.class);
    assertThat(SqlOperations.operation("ADD", A, B).getType()).isEqualTo(Long.class);
  }

  @Test
  @DisplayName("String operators render their function")
  void stringOperators() {
    assertThat(sql(SqlOperations.operation("REPLACE", A, B, C))).isEqualTo("replace(t.a,t.b,t.c)");
    Expression<?> begin = Expressions.constant(1);
    Expression<?> end = Expressions.constant(3);
    assertThat(sql(SqlOperations.operation("SUBSTRING", A, begin))).isEqualTo("substr(t.a,1+1)");
    assertThat(sql(SqlOperations.operation("SUBSTRING", A, begin, end))).isEqualTo("substr(t.a,2,2)");
    assertThat(sql(SqlOperations.operation("COALESCE", A, B))).isEqualTo("coalesce(t.a, t.b)");
    assertThat(sql(SqlOperations.operation("NULLIF", A, B))).isEqualTo("nullif(t.a,t.b)");
  }

  @Test
  @DisplayName("ROUND takes the decimals when there is a second operand")
  void round() {
    assertThat(sql(SqlOperations.operation("ROUND", A))).isEqualTo("round(t.a,0)");
    assertThat(sql(SqlOperations.operation("ROUND", A, B))).isEqualTo("round(t.a,t.b)");
    assertThat(sql(SqlOperations.operation("POWER", A, B))).isEqualTo("power(t.a,t.b)");
  }

  @Test
  @DisplayName("Date additions are java.util.Date operations and subtractions add the negated amount")
  void dateOperations() {
    Expression<?> addDays = SqlOperations.operation("ADD_DAYS", A, B);
    Expression<?> subDays = SqlOperations.operation("SUB_DAYS", A, B);
    Expression<?> diffDays = SqlOperations.operation("DIFF_DAYS", A, B);

    assertThat(sql(addDays)).isEqualTo("dateadd('dd', t.b, t.a)");
    assertThat(addDays.getType()).isEqualTo(java.util.Date.class);
    assertThat(sql(subDays)).isEqualTo("dateadd('dd', t.b * ?, t.a)");
    assertThat(subDays.getType()).isEqualTo(java.util.Date.class);
    assertThat(sql(diffDays)).isEqualTo("datediff('dd', t.a, t.b)");
    assertThat(diffDays.getType()).isEqualTo(Integer.class);
  }

  @Test
  @DisplayName("An unknown operator has no expression")
  void unknownOperator() {
    assertThat(SqlOperations.operation("UNKNOWN", A, B)).isNull();
  }

  @Test
  @DisplayName("A constant operand is bound")
  void constantOperandIsBound() {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(SqlOperations.operation("CONCAT", A, Expressions.constant("x")));

    assertThat(serializer.getConstants()).containsExactly("x");
  }

  private static String sql(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.toString();
  }
}
