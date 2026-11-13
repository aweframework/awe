package com.almis.awe.service.data.builder.sql;

import com.almis.awe.template.FixedOracleTemplates;
import com.almis.awe.template.FixedPostgreSQLTemplates;
import com.almis.awe.template.FixedSQLServerTemplates;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLSerializer;
import com.querydsl.sql.SQLTemplates;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  static Stream<Arguments> stringAggregations() {
    return Stream.of(
      Arguments.of("HSQLDB", new HSQLDBTemplates(), "group_concat(t.a order by t.a separator ', ')"),
      Arguments.of("H2", new H2Templates(), "listagg(t.a, ', ') within group (order by t.a)"),
      Arguments.of("MySQL", new MySQLTemplates(), "group_concat(t.a order by t.a separator ', ')"),
      Arguments.of("Oracle", new FixedOracleTemplates(), "listagg(t.a, ', ') within group (order by t.a)"),
      Arguments.of("SQL Server", new FixedSQLServerTemplates(), "string_agg(t.a, ', ') within group (order by t.a)"),
      Arguments.of("PostgreSQL", new FixedPostgreSQLTemplates(), "string_agg(t.a, ', ' order by t.a)"));
  }

  @ParameterizedTest(name = "{0} renders {2}")
  @MethodSource("stringAggregations")
  @DisplayName("STRING_AGG joins the values ordered by value with \", \" in the SQL of each database")
  void stringAggregationRendersTheSqlOfEachDatabase(String database, SQLTemplates templates, String sql) {
    assertThat(SqlFieldFunctions.function(COLUMN, "String_Agg", templates)).get()
      .satisfies(expression -> assertThat(expression.getType()).isEqualTo(String.class))
      .extracting(expression -> sql(expression, new Configuration(templates))).isEqualTo(sql);
  }

  static Stream<Arguments> stringAggregationSeparators() {
    return Stream.of(
      Arguments.of("HSQLDB", new HSQLDBTemplates(), "|", "group_concat(t.a order by t.a separator '|')"),
      Arguments.of("HSQLDB", new HSQLDBTemplates(), "", "group_concat(t.a order by t.a separator '')"),
      Arguments.of("H2", new H2Templates(), "' |", "listagg(t.a, ''' |') within group (order by t.a)"),
      Arguments.of("MySQL", new MySQLTemplates(), "\\' |", "group_concat(t.a order by t.a separator '\\\\'' |')"),
      Arguments.of("Oracle", new FixedOracleTemplates(), "' |", "listagg(t.a, ''' |') within group (order by t.a)"),
      Arguments.of("SQL Server", new FixedSQLServerTemplates(), "' |", "string_agg(t.a, ''' |') within group (order by t.a)"),
      Arguments.of("PostgreSQL", new FixedPostgreSQLTemplates(), "' |", "string_agg(t.a, ''' |' order by t.a)"));
  }

  @ParameterizedTest(name = "{0} renders [{2}] as {3}")
  @MethodSource("stringAggregationSeparators")
  @DisplayName("STRING_AGG joins the values with its separator, a literal escaped for each database")
  void stringAggregationRendersItsSeparator(String database, SQLTemplates templates, String separator, String sql) {
    assertThat(SqlFieldFunctions.function(COLUMN, "STRING_AGG", templates, separator)).get()
      .extracting(expression -> sql(expression, new Configuration(templates))).isEqualTo(sql);
  }

  @Test
  @DisplayName("A STRING_AGG separator with an opening brace is rejected: the SQL templates reserve it")
  void stringAggregationSeparatorWithBraceIsRejected() {
    HSQLDBTemplates templates = new HSQLDBTemplates();

    assertThatThrownBy(() -> SqlFieldFunctions.function(COLUMN, "STRING_AGG", templates, "{0}"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThat(SqlFieldFunctions.isValidSeparator("{")).isFalse();
    assertThat(SqlFieldFunctions.isValidSeparator("} |")).isTrue();
  }

  @ParameterizedTest(name = "separator [{0}] joins {1}")
  @CsvSource(delimiter = '#', quoteCharacter = '"', nullValues = "NULL", value = {
    "NULL#Admin, Contributor, Reader",
    "|#Admin|Contributor|Reader",
    "' or '1'='1#Admin' or '1'='1Contributor' or '1'='1Reader",
    "\"\"#AdminContributorReader"
  })
  @DisplayName("STRING_AGG on HSQLDB returns the values ordered and joined by the separator (\", \" by default), and null without rows")
  void stringAggregationRunsOnHsqldb(String separator, String joined) throws SQLException {
    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:string-agg-" + UUID.randomUUID());
    dataSource.setUser("sa");
    String aggregation = sql(SqlFieldFunctions.function(COLUMN, "STRING_AGG", new HSQLDBTemplates(), separator).orElseThrow());
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE t (g int, a varchar(20))");
      statement.execute("INSERT INTO t VALUES (1, 'Reader'), (1, 'Admin'), (1, 'Contributor'), (2, 'Other')");

      assertThat(values(statement, "SELECT " + aggregation + " FROM t WHERE g = 1")).containsExactly(joined);
      assertThat(values(statement, "SELECT " + aggregation + " FROM t WHERE g = 3")).containsExactly((String) null);
      statement.execute("SHUTDOWN");
    }
  }

  private static List<String> values(Statement statement, String sql) throws SQLException {
    List<String> values = new ArrayList<>();
    try (ResultSet result = statement.executeQuery(sql)) {
      while (result.next()) {
        values.add(result.getString(1));
      }
    }
    return values;
  }

  private static String sql(Expression<?> expression) {
    return sql(expression, CONFIGURATION);
  }

  private static String sql(Expression<?> expression, Configuration configuration) {
    SQLSerializer serializer = new SQLSerializer(configuration);
    serializer.handle(expression);
    return serializer.toString();
  }
}
