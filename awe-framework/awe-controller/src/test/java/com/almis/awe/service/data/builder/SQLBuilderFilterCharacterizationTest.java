package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Filter;
import com.almis.awe.model.entities.queries.FilterAnd;
import com.almis.awe.model.entities.queries.FilterOr;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.TransitionField;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Characterization of the filters {@link SQLBuilder} generates, pinned before splitting the class (#782): every
 * condition, ignore-case and trim, optional and empty variables, lists, subqueries and filter groups. Each case
 * pins the SQL serialized with the HSQLDB templates and the bound constants.
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderFilterCharacterizationTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
  private static final String SUBQUERY_SQL = "(select s.id\nfrom sub s)";

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  private Query query;
  private Map<String, QueryParameter> variables;
  private StubBuilder builder;

  @BeforeEach
  void setUp() {
    SQLQueryFactory factory = new SQLQueryFactory(CONFIGURATION, () -> connection);
    query = new Query();
    query.setId("filterQuery");
    query.setVariableDefinitionList(new ArrayList<>());
    variables = new HashMap<>();
    builder = new StubBuilder(new QueryUtil(null, null, null, null), encodeService, factory);
    builder.setQuery(query);
    builder.setFactory(factory);
    builder.setVariables(variables);
  }

  // -------------------------------------------------------------------------
  // Conditions
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("eq between two columns compares both paths")
  void eqBetweenColumns() throws AWException {
    Filter filter = columns("eq");

    assertFilter(filter, "t.a = u.b");
  }

  @ParameterizedTest(name = "{0} renders {1}")
  @CsvSource(delimiter = '|', value = {
    "eq|t.a = ?",
    "ne|not (t.a = ?)",
    "ge|t.a >= ?",
    "le|t.a <= ?",
    "gt|t.a > ?",
    "lt|t.a < ?",
    "in|t.a = ?",
    "not in|not (t.a = ?)",
    "like|t.a like ? escape '\\'",
    "not like|not (t.a like ? escape '\\')",
    "EQ|t.a = ?"
  })
  @DisplayName("A condition against a STRING variable renders its operator")
  void conditionAgainstStringVariable(String condition, String sql) throws AWException {
    stringVariable("var", "x");

    assertFilter(variableFilter(condition, "var"), sql, "x");
  }

  @Test
  @DisplayName("An unknown condition fails generating the filter")
  void unknownConditionFails() {
    stringVariable("var", "x");
    FilterAnd group = and(variableFilter("unknown", "var"));

    assertThatThrownBy(() -> builder.getFilterGroups(group))
      .isInstanceOf(AWException.class)
      .hasMessageContaining("ERROR_MESSAGE_GENERATING_FILTER")
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("eq and like with ignore case use the ignore-case operators")
  void ignoreCase() throws AWException {
    stringVariable("var", "x");

    assertFilter(variableFilter("eq", "var").setIgnoreCase(true), "lower(trim(both from t.a)) = lower(?)", "x");
    assertFilter(variableFilter("ne", "var").setIgnoreCase(true), "not (lower(trim(both from t.a)) = lower(?))", "x");
    assertFilter(variableFilter("like", "var").setIgnoreCase(true), "lower(trim(both from t.a)) like lower(?)", "x");
    assertFilter(variableFilter("not like", "var").setIgnoreCase(true),
      "not (lower(trim(both from t.a)) like lower(?))", "x");
  }

  @Test
  @DisplayName("Trim applies only to the operands that are columns")
  void trimAppliesToColumns() throws AWException {
    stringVariable("var", "x");

    assertFilter(variableFilter("eq", "var").setTrim(true), "trim(both from t.a) = ?", "x");
    assertFilter(columns("eq").setTrim(true), "trim(both from t.a) = trim(both from u.b)");
  }

  @Test
  @DisplayName("A left function is applied to the left column")
  void leftFunctionIsApplied() throws AWException {
    stringVariable("var", "x");

    assertFilter(variableFilter("eq", "var").setLeftFunction("TRIM"), "ltrim(rtrim(t.a)) = ?", "x");
  }

  @Test
  @DisplayName("is null and is not null only use the left operand")
  void isNullConditions() throws AWException {
    assertFilter(column("is null"), "t.a is null");
    assertFilter(column("is not null"), "t.a is not null");
  }

  @Test
  @DisplayName("in and exists against a subquery use the subquery")
  void subqueryConditions() throws AWException {
    assertFilter(column("in").setQuery("sub"), "t.a in " + SUBQUERY_SQL);
    assertFilter(column("not in").setQuery("sub"), "not (t.a in " + SUBQUERY_SQL + ")");
    assertFilter(new Filter().setCondition("exists").setQuery("sub"), "exists " + SUBQUERY_SQL);
    assertFilter(new Filter().setCondition("not exists").setQuery("sub"), "not (exists " + SUBQUERY_SQL + ")");
    assertFilter(column("eq").setQuery("sub"), "t.a = " + SUBQUERY_SQL);
  }

  // -------------------------------------------------------------------------
  // Variable types, empty and optional values
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A typed variable binds the converted value")
  void typedVariableBindsConvertedValue() throws AWException {
    variable("num", ParameterType.INTEGER, NODES.numberNode(5), false);
    variable("day", ParameterType.DATE, NODES.textNode("15/10/2026"), false);
    variable("text", ParameterType.STRINGB, NODES.textNode("x"), false);

    assertFilter(variableFilter("eq", "num"), "t.a = ?", 5);
    assertFilter(variableFilter("ge", "day"), "t.a >= ?", java.sql.Date.valueOf("2026-10-15"));
    assertFilter(variableFilter("like", "text"), "t.a like ? escape '\\'", "%x%");
  }

  @Test
  @DisplayName("A non optional empty variable is compared as an empty string or a null expression")
  void nonOptionalEmptyVariable() throws AWException {
    stringVariable("text", "");
    variable("textn", ParameterType.STRINGN, NODES.textNode(""), false);
    variable("num", ParameterType.INTEGER, NODES.nullNode(), false);

    assertFilter(variableFilter("eq", "text"), "t.a = ?", "");
    assertFilter(variableFilter("eq", "textn"), "t.a is null");
    assertFilter(variableFilter("in", "textn"), "t.a is null");
    assertFilter(variableFilter("ne", "textn"), "not (t.a is null)");
    assertFilter(variableFilter("ge", "textn"), "t.a >= null");
    assertFilter(variableFilter("eq", "num"), "t.a is null");
  }

  @Test
  @DisplayName("An optional filter with an empty variable is removed")
  void optionalEmptyVariableRemovesFilter() throws AWException {
    stringVariable("empty", "");
    variable("missing", ParameterType.INTEGER, NODES.nullNode(), false);
    stringVariable("full", "x");

    assertThat(builder.getFilterGroups(and(variableFilter("eq", "empty").setOptional(true)))).isNull();
    assertThat(builder.getFilterGroups(and(variableFilter("eq", "missing").setOptional(true)))).isNull();
    assertFilter(variableFilter("eq", "full").setOptional(true), "t.a = ?", "x");
  }

  @Test
  @DisplayName("A filter over an undefined variable fails")
  void undefinedVariableFails() {
    FilterAnd group = and(variableFilter("eq", "undefined"));

    assertThatThrownBy(() -> builder.getFilterGroups(group))
      .isInstanceOf(AWException.class)
      .hasMessageContaining("ERROR_MESSAGE_GENERATING_VARIABLE_VALUE");
  }

  // -------------------------------------------------------------------------
  // Lists
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A list variable with several values renders an IN list")
  void listVariableRendersInList() throws AWException {
    variable("ids", ParameterType.INTEGER, NODES.arrayNode().add(1).add(2).add(3), true);

    assertFilter(variableFilter("in", "ids"), "t.a in (?, ?, ?)", 1, 2, 3);
    assertFilter(variableFilter("not in", "ids"), "not (t.a in (?, ?, ?))", 1, 2, 3);
    assertFilter(variableFilter("eq", "ids"), "t.a = (?, ?, ?)", 1, 2, 3);
  }

  @Test
  @DisplayName("A list with several values keeps its empty values")
  void listKeepsEmptyValuesWhenSeveral() throws AWException {
    variable("ids", ParameterType.STRING, NODES.arrayNode().add("a").add(""), true);

    assertFilter(variableFilter("in", "ids"), "t.a in (?, ?)", "a", "");
  }

  @Test
  @DisplayName("A list variable with a single value is compared as a single value")
  void singleValueListIsComparedAsValue() throws AWException {
    variable("ids", ParameterType.STRING, NODES.arrayNode().add("a"), true);

    assertFilter(variableFilter("in", "ids"), "t.a = ?", "a");
  }

  @Test
  @DisplayName("An empty list variable removes an optional filter and fails a non optional one")
  void emptyListVariable() throws AWException {
    variable("empty", ParameterType.STRING, NODES.arrayNode(), true);
    variable("blank", ParameterType.STRING, NODES.arrayNode().add(""), true);

    assertThat(builder.getFilterGroups(and(variableFilter("in", "empty").setOptional(true)))).isNull();
    assertThat(builder.getFilterGroups(and(variableFilter("in", "blank").setOptional(true)))).isNull();
    FilterAnd group = and(variableFilter("in", "blank"));
    assertThatThrownBy(() -> builder.getFilterGroups(group))
      .isInstanceOf(AWException.class)
      .hasMessageContaining("ERROR_MESSAGE_EMPTY_LIST_OPTIONAL");
  }

  @Test
  @DisplayName("A list variable in an audit multiple query still renders an IN list")
  void auditQueryRendersInList() throws AWException {
    query.setMultiple("audit");
    variable("ids", ParameterType.LONG, NODES.arrayNode().add(1).add(2), true);

    assertFilter(variableFilter("in", "ids"), "t.a in (?, ?)", 1L, 2L);
  }

  @Test
  @DisplayName("A list variable in a multiple query uses the value at the variable index")
  void multipleQueryUsesVariableIndex() throws AWException {
    query.setMultiple("true");
    builder.setIndex(1);
    variable("ids", ParameterType.LONG, NODES.arrayNode().add(1).add(2), true);
    variable("blank", ParameterType.LONG, NODES.textNode(""), true);

    assertFilter(variableFilter("eq", "ids"), "t.a = ?", 2L);
    assertFilter(variableFilter("eq", "blank"), "t.a is null");
    assertThat(builder.getFilterGroups(and(variableFilter("eq", "blank").setOptional(true)))).isNull();
  }

  // -------------------------------------------------------------------------
  // Constant operands
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A constant operand binds its typed value")
  void constantOperand() throws AWException {
    assertFilter(constantFilter("eq", "INTEGER", "5"), "t.a = ?", 5);
    assertFilter(constantFilter("eq", null, "5"), "t.a = ?", "5");
    assertFilter(constantFilter("in", "STRING_TO_LIST", "1, 2"), "t.a in (?, ?)", "1", "2");
  }

  @Test
  @DisplayName("An empty constant operand removes an optional filter")
  void emptyConstantOperand() throws AWException {
    assertThat(builder.getFilterGroups(and(constantFilter("eq", null, "").setOptional(true)))).isNull();
    assertFilter(constantFilter("eq", null, ""), "t.a = ?", "");
    assertFilter(constantFilter("eq", "STRINGN", ""), "t.a is null");
  }

  // -------------------------------------------------------------------------
  // Filter groups
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Filter groups join their filters and nested groups with their union")
  void filterGroups() throws AWException {
    stringVariable("var", "x");
    stringVariable("empty", "");
    FilterOr nested = (FilterOr) new FilterOr().setFilterList(List.of(column("is null"), columns("eq")));
    FilterAnd group = and(variableFilter("eq", "var"), variableFilter("eq", "empty").setOptional(true));
    group.setFilterGroupList(List.of(nested));

    assertThat(sql(builder.getFilterGroups(group))).isEqualTo("(t.a is null or t.a = u.b) and t.a = ?");
    assertThat(builder.getFilterGroups(new FilterAnd())).isNull();
  }

  // -------------------------------------------------------------------------
  // Extension points
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Overrides of the protected methods of a project subclass still apply to the filters")
  void subclassOverridesStillApply() throws AWException {
    StubBuilder overriding = new StubBuilder(new QueryUtil(null, null, null, null), encodeService, null) {
      @Override
      protected PathBuilder<Object> buildPath(String node) {
        return super.buildPath(node == null ? null : "p_" + node);
      }

      @Override
      protected Expression getStringExpression(String value) {
        return super.getStringExpression(value == null ? null : value.toUpperCase());
      }

      @Override
      protected String getVariableAsString(JsonNode varValue) {
        String value = super.getVariableAsString(varValue);
        return value == null ? null : value + "!";
      }
    };
    overriding.setQuery(query);
    overriding.setVariables(variables);
    stringVariable("var", "x");
    variable("list", ParameterType.LIST_TO_STRING, NODES.arrayNode().add("a").add("b"), false);

    Expression<?> filter = overriding.getFilterGroups(and(variableFilter("eq", "var")));
    Expression<?> list = overriding.getFilterGroups(and(variableFilter("eq", "list")));

    assertThat(sql(filter)).isEqualTo("p_t.a = ?");
    assertThat(constants(filter)).containsExactly("X!");
    assertThat(constants(list)).containsExactly("A!,B!");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void assertFilter(Filter filter, String sql, Object... constants) throws AWException {
    Expression<?> expression = builder.getFilterGroups(and(filter));

    assertThat(sql(expression)).as(filter.toString()).isEqualTo(sql);
    List<Object> bound = constants(expression);
    assertThat(bound).as(filter.toString()).containsExactly(constants);
    for (int i = 0; i < constants.length; i++) {
      assertThat(bound.get(i).getClass()).as("class of constant %d", i).isEqualTo(constants[i].getClass());
    }
  }

  private static FilterAnd and(Filter... filters) {
    return (FilterAnd) new FilterAnd().setFilterList(new ArrayList<>(Arrays.asList(filters)));
  }

  private static Filter column(String condition) {
    return new Filter().setLeftTable("t").setLeftField("a").setCondition(condition);
  }

  private static Filter columns(String condition) {
    return column(condition).setRightTable("u").setRightField("b");
  }

  private static Filter variableFilter(String condition, String variable) {
    return column(condition).setRightVariable(variable);
  }

  private static Filter constantFilter(String condition, String type, String value) {
    Constant constant = (Constant) new Constant().setType(type).setValue(value);
    return column(condition).setRightOperand(new TransitionField().setField(constant));
  }

  private void stringVariable(String name, String value) {
    variable(name, ParameterType.STRING, NODES.textNode(value), false);
  }

  private void variable(String name, ParameterType type, JsonNode value, boolean isList) {
    query.getVariableDefinitionList().add(Variable.builder().id(name).type(type.name()).build());
    variables.put(name, new QueryParameter(value, isList, type));
  }

  private static String sql(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.toString();
  }

  private static List<Object> constants(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.getConstants();
  }

  /**
   * Concrete builder whose localization answers the key, whose subqueries select {@code s.id from sub s}
   * and whose variable index can be set as in a multiple maintain
   */
  private static class StubBuilder extends SQLQueryBuilder {

    private final AweElements aweElements = mock(AweElements.class);
    private final SQLQueryFactory queryFactory;
    private Integer index;

    StubBuilder(QueryUtil queryUtil, EncodeService encodeService, SQLQueryFactory queryFactory) {
      super(queryUtil, encodeService);
      this.queryFactory = queryFactory;
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull()))
        .thenAnswer(invocation -> invocation.getArgument(0));
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    }

    void setIndex(Integer index) {
      this.index = index;
    }

    @Override
    public Integer getVariableIndex() {
      return index;
    }

    @Override
    public AweElements getElements() {
      return aweElements;
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      RelationalPathBase<Object> table = new RelationalPathBase<>(Object.class, "s", null, queryId);
      return queryFactory.select(new Expression<?>[]{new PathBuilder<>(Object.class, "s").get("id")}).from(table);
    }
  }
}
