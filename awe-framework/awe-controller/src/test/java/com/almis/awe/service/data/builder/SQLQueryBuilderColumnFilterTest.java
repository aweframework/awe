package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Filter;
import com.almis.awe.model.entities.queries.FilterAnd;
import com.almis.awe.model.entities.queries.Operation;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Union;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.data.builder.SQLQueryBuilderFixture.StubBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;

import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Grid column filters of a SQL query (#912, S5 and S7): one condition group per column ANDed to the WHERE, on the
 * field expressions of the query, with bound values from a closed list of operators.
 */
@ExtendWith(MockitoExtension.class)
class SQLQueryBuilderColumnFilterTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final String SELECT = SQLQueryBuilderFixture.SELECT + "\n";

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  private QueryUtil queryUtil;
  private SQLQueryFactory factory;
  private Query query;

  @BeforeEach
  void setUp() {
    queryUtil = SQLQueryBuilderFixture.queryUtil();
    factory = new SQLQueryFactory(CONFIGURATION, () -> connection);
    query = SQLQueryBuilderFixture.usersQuery();
  }

  @Test
  @DisplayName("contains ignores case and matches % and _ typed by the user literally")
  void containsEscapesWildcards() throws AWException {
    SQLBindings sql = build("[{'id':'name','type':'text','conditions':[{'operator':'contains','value':'50%_OFF'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where lower(u.Nam) like ? escape '\\'");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("%50\\%\\_off%");
  }

  @Test
  @DisplayName("With the literals of the connector the value is an escaped constant, never SQL")
  void literalValueIsEscaped() throws AWException {
    Configuration literals = new Configuration(new HSQLDBTemplates());
    literals.setUseLiterals(true);
    factory = new SQLQueryFactory(literals, () -> connection);

    SQLBindings sql = build("[{'id':'name','type':'text','conditions':[{'operator':'eq','value':'x\\u0027 or \\u00271\\u0027=\\u00271'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where lower(u.Nam) = 'x'' or ''1''=''1'");
  }

  @Test
  @DisplayName("values become an IN, plus IS NULL when empty is true")
  void valuesWithEmpty() throws AWException {
    SQLBindings sql = build("[{'id':'status','type':'values','values':['A','B'],'empty':true}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where u.Sta in (?, ?) or u.Sta is null");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("A", "B");
  }

  @Test
  @DisplayName("between dates is inclusive and covers the whole to day")
  void betweenDates() throws AWException {
    SQLBindings sql = build("[{'id':'created','type':'date','conditions':[{'operator':'between','value':'01/11/2026','to':'03/11/2026'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where u.Dat >= ? and u.Dat < ?");
    assertThat(sql.getNullFriendlyBindings()).containsExactly(Date.valueOf("2026-11-01"), Date.valueOf("2026-11-04"));
  }

  @Test
  @DisplayName("A number is compared as a number")
  void numberGreaterThan() throws AWException {
    SQLBindings sql = build("[{'id':'amount','type':'number','conditions':[{'operator':'gt','value':'10'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where u.Amt > ?");
    assertThat(sql.getNullFriendlyBindings()).containsExactly(10L);
  }

  @Test
  @DisplayName("empty is IS NULL")
  void emptyIsNull() throws AWException {
    SQLBindings sql = build("[{'id':'name','type':'text','conditions':[{'operator':'empty'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where u.Nam is null");
    assertThat(sql.getNullFriendlyBindings()).isEmpty();
  }

  @Test
  @DisplayName("Two conditions joined by or, and two column filters joined by and")
  void twoConditionsWithOr() throws AWException {
    SQLBindings sql = build("[{'id':'name','type':'text','union':'or','conditions':[{'operator':'startsWith','value':'a'},"
      + "{'operator':'notEndsWith','value':'z'}]},{'id':'amount','type':'number','conditions':[{'operator':'le','value':'2.5'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT
      + "where (lower(u.Nam) like ? escape '\\' or not (lower(u.Nam) like ? escape '\\')) and u.Amt <= ?");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("a%", "%z", 2.5);
  }

  @Test
  @DisplayName("A filter on several fields matches when any field matches")
  void severalFieldsJoinedByOr() throws AWException {
    SQLBindings sql = build("[{'id':'name','fields':['name','email'],'type':'text','conditions':[{'operator':'eq','value':'Ann'}]}]");

    assertThat(sql.getSQL()).isEqualTo(SELECT + "where lower(u.Nam) = ? or lower(u.Eml) = ?");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("ann", "ann");
  }

  @Test
  @DisplayName("The column filters are ANDed to the WHERE of the query, and the count query carries them too")
  void andedToQueryWhereAndCount() throws AWException {
    query.setVariableDefinitionList(new ArrayList<>(List.of(Variable.builder().id("act").type("INTEGER").build())));
    query.setFilterGroup((FilterAnd) new FilterAnd().setFilterList(new ArrayList<>(List.of(
      new Filter().setLeftTable("u").setLeftField("Act").setCondition("eq").setRightVariable("act")))));
    Map<String, QueryParameter> variables = new HashMap<>();
    variables.put("act", new QueryParameter(JsonNodeFactory.instance.numberNode(1), false, ParameterType.INTEGER));
    StubBuilder builder = builder("[{'id':'amount','type':'number','conditions':[{'operator':'ge','value':'3'}]}]", variables);

    String where = "where u.Act = ? and u.Amt >= ?";
    assertThat(builder.build().getSQL().getSQL()).isEqualTo(SELECT + where);
    assertThat(builder.queryForCount().build().getSQL().getSQL()).isEqualTo(SELECT + where);
  }

  @Test
  @DisplayName("On a UNION query the column filters apply to the wrapper, by the alias of the field")
  void unionAppliesToWrapper() throws AWException {
    query.setUnionList(new ArrayList<>(List.of(new Union().setQuery("other"))));

    SQLBindings sql = build("[{'id':'name','type':'text','conditions':[{'operator':'notContains','value':'x'}]}]");

    assertThat(sql.getSQL()).endsWith(") as T\nwhere not (lower(T.name) like ? escape '\\')");
    assertThat(sql.getSQL()).containsOnlyOnce("like");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("%x%");
  }

  @Test
  @DisplayName("An empty list of values keeps no row, as every value unchecked in the list of values")
  void emptyValuesListKeepsNoRow() throws AWException {
    assertThat(build("[{'id':'status','type':'values','values':[]}]").getSQL()).isEqualTo(SELECT + "where 1 = 0");
  }

  @ParameterizedTest(name = "{0} of {1}")
  @CsvSource({"ADD,SUM", "CONCAT,STRING_AGG"})
  @DisplayName("A field whose operation holds an aggregate cannot be filtered")
  void aggregateInsideOperationIsRejected(String operator, String function) {
    query.getSqlFieldList().add(Operation.builder().operator(operator).alias("tot").operandList(new ArrayList<>(List.of(
      Field.builder().id("Amt").table("u").function(function).build(), Field.builder().id("Amt").table("u").build()))).build());
    StubBuilder builder = builder("[{'id':'tot','type':'number','conditions':[{'operator':'gt','value':'1'}]}]");

    assertThatThrownBy(builder::build).isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("field tot of column filter tot is an aggregate or window function");
  }

  @Test
  @DisplayName("The value compared with a masked column is masked in the logged SQL")
  void maskedColumnValueIsMasked() throws AWException {
    StubBuilder builder = builder("[{'id':'email','type':'text','conditions':[{'operator':'contains','value':'Secret'}]}]");
    builder.setLogMaskedFields("Eml");

    SQLBindings sql = builder.build().getSQL();

    assertThat(queryUtil.getFullSQL(sql.getSQL(), sql.getNullFriendlyBindings(), builder.getLogMaskedValues()))
      .isEqualTo(SELECT + "where lower(u.Eml) like '********' escape '\\'");
  }

  @Test
  @DisplayName("Without column filters, or with an empty list, the SQL is the same as before")
  void noColumnFiltersKeepTheSql() throws AWException {
    String before = builder(null).build().getSQL().getSQL();

    assertThat(before).isEqualTo(SELECT.trim());
    assertThat(builder("[]").build().getSQL().getSQL()).isEqualTo(before);
  }

  // -------------------------------------------------------------------------
  // Fixtures
  // -------------------------------------------------------------------------

  private SQLBindings build(String columnFilters) throws AWException {
    return builder(columnFilters).build().getSQL();
  }

  private StubBuilder builder(String columnFilters) {
    return builder(columnFilters, new HashMap<>());
  }

  private StubBuilder builder(String columnFilters, Map<String, QueryParameter> variables) {
    StubBuilder builder = new StubBuilder(queryUtil, encodeService, factory);
    builder.setQuery(query).setFactory(factory).setVariables(variables);
    if (columnFilters != null) {
      try {
        builder.setColumnFilters(queryUtil.getColumnFilterList(SQLQueryBuilderFixture.MAPPER.readTree(columnFilters.replace('\'', '"'))));
      } catch (AWException exc) {
        throw new IllegalStateException(exc);
      }
    }
    return builder;
  }
}
