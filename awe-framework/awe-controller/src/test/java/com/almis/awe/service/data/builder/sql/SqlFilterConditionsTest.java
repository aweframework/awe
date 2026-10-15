package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SqlFilterConditions}
 */
class SqlFilterConditionsTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final Expression<?> COLUMN = new PathBuilder<>(Object.class, "t").get("a");
  private static final Expression<?> VALUE = Expressions.constant("x");

  @ParameterizedTest(name = "{0} renders {1}")
  @CsvSource(delimiter = '|', value = {
    "eq|t.a = ?",
    "ne|not (t.a = ?)",
    "ge|t.a >= ?",
    "le|t.a <= ?",
    "gt|t.a > ?",
    "in|t.a = ?",
    "not in|not (t.a = ?)",
    "like|t.a like ? escape '\\'",
    "not like|not (t.a like ? escape '\\')",
    "is null|t.a is null",
    "is not null|t.a is not null"
  })
  @DisplayName("A condition renders its operator")
  void conditionRendersItsOperator(String condition, String sql) {
    assertThat(sql(SqlFilterConditions.condition(condition, false, COLUMN, VALUE))).isEqualTo(sql);
  }

  @Test
  @DisplayName("Ignore case applies to eq and like only")
  void ignoreCaseAppliesToEqAndLike() {
    assertThat(sql(SqlFilterConditions.condition("eq", true, COLUMN, VALUE))).isEqualTo("lower(t.a) = ?");
    assertThat(sql(SqlFilterConditions.condition("like", true, COLUMN, VALUE))).isEqualTo("lower(t.a) like ?");
    assertThat(sql(SqlFilterConditions.condition("ge", true, COLUMN, VALUE))).isEqualTo("t.a >= ?");
  }

  @Test
  @DisplayName("eq and in against a null expression are an is null")
  void nullSecondOperandIsIsNull() {
    Expression<?> nullValue = Expressions.nullExpression();

    assertThat(sql(SqlFilterConditions.condition("eq", true, COLUMN, nullValue))).isEqualTo("t.a is null");
    assertThat(sql(SqlFilterConditions.condition("in", false, COLUMN, nullValue))).isEqualTo("t.a is null");
    assertThat(sql(SqlFilterConditions.condition("ge", false, COLUMN, nullValue))).isEqualTo("t.a >= null");
  }

  @Test
  @DisplayName("in against a list or a subquery is an IN")
  void inAgainstListOrSubquery() {
    Expression<?> list = Expressions.list(Expressions.constant(1), Expressions.constant(2));

    assertThat(sql(SqlFilterConditions.condition("in", false, COLUMN, list))).isEqualTo("t.a in (?, ?)");
    assertThat(sql(SqlFilterConditions.condition("in", false, COLUMN, subquery())))
      .isEqualTo("t.a in (select s.id\nfrom sub s)");
  }

  @Test
  @DisplayName("exists uses the operand that is a subquery, the first one when both are")
  void existsUsesTheSubquery() {
    Expression<?> nullValue = Expressions.nullExpression();

    assertThat(sql(SqlFilterConditions.condition("exists", false, nullValue, subquery())))
      .isEqualTo("exists (select s.id\nfrom sub s)");
    assertThat(sql(SqlFilterConditions.condition("not exists", false, subquery(), nullValue)))
      .isEqualTo("not (exists (select s.id\nfrom sub s))");
  }

  @Test
  @DisplayName("An unknown condition is not a QueryDSL operator")
  void unknownConditionFails() {
    assertThatThrownBy(() -> SqlFilterConditions.condition("unknown", false, COLUMN, VALUE))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("join uses and for an AND union and or for any other union, in order")
  void joinUsesTheUnion() {
    BooleanExpression first = SqlFilterConditions.condition("is null", false, COLUMN, VALUE);
    BooleanExpression second = SqlFilterConditions.condition("eq", false, COLUMN, VALUE);
    BooleanExpression third = SqlFilterConditions.condition("is not null", false, COLUMN, VALUE);

    assertThat(sql(SqlFilterConditions.join(List.of(first, second, third), "and")))
      .isEqualTo("t.a is null and t.a = ? and t.a is not null");
    assertThat(sql(SqlFilterConditions.join(List.of(first, second), "OR"))).isEqualTo("t.a is null or t.a = ?");
    assertThat(SqlFilterConditions.join(List.of(first), "AND")).isSameAs(first);
    assertThat(SqlFilterConditions.join(List.of(), "AND")).isNull();
  }

  private static SQLQuery<?> subquery() {
    return new SQLQuery<Void>(CONFIGURATION).select(new PathBuilder<>(Object.class, "s").get("id"))
      .from(new RelationalPathBase<>(Object.class, "s", null, "sub"));
  }

  private static String sql(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.toString();
  }
}
