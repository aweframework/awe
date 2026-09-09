package com.almis.awe.service.data.builder;

import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.types.dsl.SimpleExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for the {@code buildPath} overloads in {@link SQLBuilder}.
 * <p>
 * {@code buildPath(String)} returns {@code null} when the node is {@code null}, so
 * {@code buildPath(parent, node, alias)} must not apply the alias over a {@code null} path.
 * These methods are {@code protected}, and this test lives in the same package, so they are
 * exercised directly through a concrete {@link SQLQueryBuilder}.
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderBuildPathTest {

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private EncodeService encodeService;

  private SQLQueryBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new SQLQueryBuilder(queryUtil, encodeService);
  }

  @Test
  @DisplayName("buildPath with table, field and alias returns an aliased path")
  void buildPathWithTableFieldAndAliasReturnsAliasedPath() {
    SimpleExpression<Object> path = builder.buildPath("myTable", "myCol", "myAlias");

    assertThat(path).isNotNull();
    assertThat(path.toString()).contains("myAlias");
  }

  @Test
  @DisplayName("buildPath without table but with field and alias returns an aliased path")
  void buildPathWithoutTableReturnsAliasedPath() {
    SimpleExpression<Object> path = builder.buildPath(null, "myCol", "myAlias");

    assertThat(path).isNotNull();
    assertThat(path.toString()).contains("myAlias");
  }

  @Test
  @DisplayName("buildPath without table and without field returns null instead of throwing when an alias is given")
  void buildPathWithoutTableAndFieldDoesNotThrowWhenAliasIsGiven() {
    assertThatCode(() -> builder.buildPath(null, null, "myAlias")).doesNotThrowAnyException();

    assertThat(builder.buildPath(null, null, "myAlias")).isNull();
  }

  @Test
  @DisplayName("buildPath without table, field and alias returns null")
  void buildPathWithoutTableFieldAndAliasReturnsNull() {
    assertThat(builder.buildPath(null, null, null)).isNull();
  }

  @Test
  @DisplayName("buildPath with a null node returns null")
  void buildPathWithNullNodeReturnsNull() {
    assertThat(builder.buildPath((String) null)).isNull();
  }

  /**
   * A field expression that could not be resolved is a null path by contract (see the tests above).
   * The aggregate branches of {@code getExpressionFunction} already accept a null expression — the
   * {@code over} rendering relies on it — but the {@code ABS} branch read {@code getType()} off it
   * and threw a NullPointerException. Only that dereference is guarded; the null keeps flowing.
   */
  @Test
  @DisplayName("getSimpleFieldExpression with a null field and the ABS function does not throw")
  void getSimpleFieldExpressionWithNullFieldAndAbsFunctionDoesNotThrow() {
    assertThatCode(() -> builder.getSimpleFieldExpression(null, null, "ABS")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("getSimpleFieldExpression with a null field and an aggregate function does not throw")
  void getSimpleFieldExpressionWithNullFieldAndAggregateFunctionDoesNotThrow() {
    assertThatCode(() -> builder.getSimpleFieldExpression(null, null, "AVG")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("getSimpleFieldExpression with a null field and no function returns null")
  void getSimpleFieldExpressionWithNullFieldAndNoFunctionReturnsNull() {
    assertThat(builder.getSimpleFieldExpression(null, null, null)).isNull();
  }

  @Test
  @DisplayName("getSimpleFieldExpression still applies the function over a resolvable field")
  void getSimpleFieldExpressionAppliesFunctionOverResolvableField() {
    assertThat(builder.getSimpleFieldExpression("myTable", "myCol", "ABS")).isNotNull();
  }
}
