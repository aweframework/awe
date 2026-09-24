package com.almis.awe.service.data.builder;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.queries.Case;
import com.almis.awe.model.entities.queries.CaseWhen;
import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Over;
import com.almis.awe.model.entities.queries.PartitionBy;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.TransitionField;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Focused unit tests for the {@code partition-by} expression inside a window function.
 * <p>
 * {@link PartitionBy} extends {@link com.almis.awe.model.entities.queries.GroupBy}, so it carries
 * a {@code function} attribute and an optional nested {@code case} element. The group-by builder
 * honours both; the {@code over} builder used to ignore them and partition only by the bare
 * field path, silently dropping the composite expression (issue #345).
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderPartitionByTest {

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  private SQLQueryFactory factory;

  @BeforeEach
  void setUp() {
    factory = new SQLQueryFactory(new Configuration(new HSQLDBTemplates()), () -> connection);
  }

  @Test
  @DisplayName("partition-by with a plain field partitions by the field path")
  void plainFieldPartitionsByFieldPath() throws Exception {
    Over over = overWith(PartitionBy.builder().field("name").table("ope").build());

    String sql = renderOver(over);

    assertThat(sql).contains("partition by");
    assertThat(sql).contains("ope.name");
  }

  @Test
  @DisplayName("partition-by with a function wraps the field in the function")
  void functionPartitionsByFunctionExpression() throws Exception {
    Over over = overWith(PartitionBy.builder().field("dat").table("ope").function("YEAR").build());

    String sql = renderOver(over);

    // HSQLDB renders the YEAR function as an EXTRACT expression
    assertThat(sql).containsIgnoringCase("partition by extract(year from ope.dat)");
  }

  @Test
  @DisplayName("partition-by with a TRIM function wraps the field in the function")
  void trimFunctionPartitionsByFunctionExpression() throws Exception {
    Over over = overWith(PartitionBy.builder().field("name").table("ope").function("TRIM").build());

    String sql = renderOver(over);

    assertThat(sql).containsIgnoringCase("partition by ltrim(rtrim(ope.name))");
  }

  @Test
  @DisplayName("partition-by with a nested case partitions by the case expression")
  void nestedCasePartitionsByCaseExpression() throws Exception {
    Over over = overWith(PartitionBy.builder().groupByCase(simpleCase()).build());

    String sql = renderOver(over);

    assertThat(sql).containsIgnoringCase("partition by case");
    assertThat(sql).doesNotContainIgnoringCase("partition by null");
  }

  @Test
  @DisplayName("several partition-by elements are all kept, composing the partition")
  void multiplePartitionByElementsAreAllApplied() throws Exception {
    Over over = overWith(
      PartitionBy.builder().field("dat").table("ope").function("YEAR").build(),
      PartitionBy.builder().field("id").table("ope").build());

    String sql = renderOver(over);

    assertThat(sql).containsIgnoringCase("extract(year from ope.dat)");
    assertThat(sql).contains("ope.id");
  }

  /**
   * Build an {@code over} element with a RANK window function and the given partitions.
   */
  private Over overWith(PartitionBy... partitions) {
    Over over = new Over();
    over.setFieldList(List.of(Field.builder().function("RANK").build()));
    over.setPartitionByList(List.of(partitions));
    return over;
  }

  /**
   * Build a minimal CASE WHEN expression usable as a partition expression.
   */
  private Case simpleCase() {
    CaseWhen caseWhen = CaseWhen.builder()
      .leftField("id")
      .leftTable("ope")
      .condition("eq")
      .rightField("id")
      .rightTable("ope")
      .thenOperand(transition("ONE"))
      .build();

    return Case.builder()
      .caseWhenList(List.of(caseWhen))
      .caseElse(transition("OTHER"))
      .build();
  }

  /**
   * Wrap a string constant in the transition field the case branches expect.
   */
  private TransitionField transition(String value) {
    return TransitionField.builder()
      .fields(List.of(Constant.builder().type("STRING").value(value).build()))
      .build();
  }

  /**
   * Render the over expression into SQL through a real query, so the assertion covers the
   * generated statement rather than the expression object graph.
   */
  private String renderOver(Over over) throws Exception {
    StubSQLQueryBuilder builder = new StubSQLQueryBuilder(queryUtil, encodeService);
    builder.setQuery(new Query());
    builder.setFactory(factory);

    Expression<?> expression = builder.buildOverExpression(over);

    return factory.select(expression).from(com.querydsl.core.types.dsl.Expressions.stringPath("ope")).getSQL().getSQL().toLowerCase();
  }

  /**
   * Minimal concrete extension of {@link SQLQueryBuilder} replacing Spring-managed
   * infrastructure with test stubs, and exposing the protected over-expression builder.
   */
  private static class StubSQLQueryBuilder extends SQLQueryBuilder {

    private final AweElements aweElements = mock(AweElements.class);

    StubSQLQueryBuilder(QueryUtil queryUtil, EncodeService encodeService) {
      super(queryUtil, encodeService);
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull()))
        .thenAnswer(inv -> inv.getArgument(0));
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull(), any(Object[].class)))
        .thenAnswer(inv -> inv.getArgument(0));
    }

    Expression<?> buildOverExpression(Over over) throws Exception {
      return getOverExpression(over);
    }

    @Override
    public AweElements getElements() {
      return aweElements;
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      throw new UnsupportedOperationException("Subquery execution not supported in unit tests");
    }
  }
}
