package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Union;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.data.builder.SQLQueryBuilderFixture.StubBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Distinct values of a grid column on a SQL query (#912, S9-S11): the grid query, with the other column filters and
 * without ORDER BY nor pagination, wrapped as subquery V and grouped by the value of the filtered fields.
 */
@ExtendWith(MockitoExtension.class)
class SQLQueryBuilderColumnValuesTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final String GRID = "(" + SQLQueryBuilderFixture.SELECT + ") as V";

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
  @DisplayName("One field: grouped value and count over the wrapped grid query, sorted with the nulls last, up to the limit")
  void oneField() throws AWException {
    SQLBindings sql = builder(null).buildColumnValues(fields("status"), List.of(), ColumnFilterType.VALUES, null, 1001).getSQL();

    assertThat(sql.getSQL()).isEqualTo("select V.status, count(*)\nfrom " + GRID + "\ngroup by V.status\n"
      + "order by V.status asc nulls last\nlimit ?");
    assertThat(sql.getNullFriendlyBindings()).containsExactly(1001L);
  }

  @Test
  @DisplayName("The other column filters apply inside the grid query, whose sort is left out")
  void otherColumnFiltersApplyWithoutSort() throws AWException {
    StubBuilder builder = builder("[{'id':'amount','type':'number','conditions':[{'operator':'gt','value':'3'}]}]");
    ArrayNode sort = JsonNodeFactory.instance.arrayNode();
    sort.addObject().put("id", "name").put("direction", "asc");
    builder.setComponentSort(sort);

    String sql = builder.buildColumnValues(fields("status"), List.of(), ColumnFilterType.VALUES, null, 1001).getSQL().getSQL();

    assertThat(sql).contains("from AweUsr u\nwhere u.Amt > ?) as V").doesNotContain("order by u.Nam");
  }

  @Test
  @DisplayName("The label fields the column needs are retrieved as the smallest value of each group")
  void labelFieldsAreTheSmallestOfTheGroup() throws AWException {
    String sql = builder(null).buildColumnValues(fields("status"), fields("name"), ColumnFilterType.VALUES, null, 1001).getSQL().getSQL();

    assertThat(sql).startsWith("select V.status, count(*), min(V.name)\nfrom " + GRID + "\ngroup by V.status\n");
  }

  @Test
  @DisplayName("Several fields: union of their values, each skipping the rows whose value a previous field already has")
  void severalFields() throws AWException {
    String sql = builder(null).buildColumnValues(fields("name", "email"), List.of(), ColumnFilterType.TEXT, null, 1001).getSQL().getSQL();

    assertThat(sql).isEqualTo("select U.column_value, count(*)\nfrom (select V.name as column_value\nfrom " + GRID + "\n"
      + "union all\nselect V.email as column_value\nfrom " + GRID + "\n"
      + "where V.email is null and V.name is not null or V.email is not null and (V.name is null or V.email != V.name)) as U\n"
      + "group by U.column_value\norder by U.column_value asc nulls last\nlimit ?");
  }

  @Test
  @DisplayName("Several fields on a dialect that wraps the unions keep the grid query as a subquery of each branch")
  void severalFieldsWithWrappedUnions() throws AWException {
    factory = new SQLQueryFactory(new Configuration(new PostgreSQLTemplates()), () -> connection);

    String sql = builder(null).buildColumnValues(fields("name", "email"), List.of(), ColumnFilterType.TEXT, null, 1001).getSQL().getSQL();

    assertThat(sql).startsWith("select U.column_value, count(*)\nfrom ((select V.name as column_value\nfrom (select u.Nam as name")
      .contains("union all\n(select V.email as column_value\nfrom (select u.Nam as name");
  }

  @Test
  @DisplayName("A date column is grouped by day")
  void dateIsGroupedByDay() throws AWException {
    String sql = builder(null).buildColumnValues(fields("created"), List.of(), ColumnFilterType.DATE, null, 1001).getSQL().getSQL();

    assertThat(sql).startsWith("select convert(V.created, date), count(*)\n")
      .contains("group by convert(V.created, date)\norder by convert(V.created, date) asc nulls last");
  }

  @Test
  @DisplayName("The search is a case insensitive contains on the value, as an escaped constant, before the limit")
  void searchInSql() throws AWException {
    SQLBindings sql = builder(null).buildColumnValues(fields("name"), List.of(), ColumnFilterType.TEXT, "50%_A", 1001).getSQL();

    assertThat(sql.getSQL()).contains(GRID + "\nwhere lower(V.name) like ? escape '\\'\ngroup by V.name\n");
    assertThat(sql.getNullFriendlyBindings()).containsExactly("%50\\%\\_a%", 1001L);
  }

  @Test
  @DisplayName("A UNION query is wrapped whole, with its column filters on its own wrapper")
  void unionQueryIsWrapped() throws AWException {
    query.setUnionList(new ArrayList<>(List.of(new Union().setQuery("other"))));

    String sql = builder("[{'id':'name','type':'text','conditions':[{'operator':'notEmpty'}]}]")
      .buildColumnValues(fields("status"), List.of(), ColumnFilterType.VALUES, null, 1001).getSQL().getSQL();

    assertThat(sql).startsWith("select V.status, count(*)\nfrom (select T.name").contains("where T.name is not null) as V\n");
  }

  private List<SqlField> fields(String... identifiers) {
    return List.of(identifiers).stream()
      .map(identifier -> query.getSqlFieldList().stream().filter(field -> identifier.equals(field.getIdentifier())).findFirst().orElseThrow())
      .toList();
  }

  private StubBuilder builder(String columnFilters) {
    StubBuilder builder = new StubBuilder(queryUtil, encodeService, factory);
    builder.setQuery(query).setFactory(factory).setVariables(new HashMap<>());
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
