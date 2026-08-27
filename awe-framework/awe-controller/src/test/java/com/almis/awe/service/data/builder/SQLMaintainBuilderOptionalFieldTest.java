package com.almis.awe.service.data.builder;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.Update;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Table;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.MaintainBuildOperation;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.AbstractSQLClause;
import com.querydsl.sql.dml.AbstractSQLUpdateClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Focused tests for the maintain {@code optional} field attribute (issue #344).
 * <p>
 * An optional field whose variable resolves to no value is left out of the generated
 * INSERT/UPDATE, so the column keeps its database default (insert) or its stored value
 * (update) instead of being overwritten with null.
 * <p>
 * The critical invariant these tests protect is <strong>alignment</strong>: columns and values
 * are built by separate loops in {@link SQLMaintainBuilder}, so omitting a field on one side
 * and not the other would silently pair column N with value N+1.
 */
@ExtendWith(MockitoExtension.class)
class SQLMaintainBuilderOptionalFieldTest {

  @Mock
  private Connection connection;

  private SQLQueryFactory factory;
  private QueryUtil queryUtil;

  @BeforeEach
  void setUp() {
    factory = new SQLQueryFactory(new Configuration(new HSQLDBTemplates()), () -> connection);
    // Real QueryUtil so the optional check runs the actual isEmptyVariable — the same semantics
    // filters already use — instead of a stub. Only addToVariableMap is neutralised: it resolves
    // default variables from the live request, which no unit test has, and the variable map is
    // supplied directly by each test.
    queryUtil = new QueryUtil(new BaseConfigProperties(), auditProperties(), new ObjectMapper(), null) {
      @Override
      public void addToVariableMap(Map<String, QueryParameter> variableMap, Query query, ObjectNode parameters) {
        // Variables are provided by the test fixture
      }
    };
  }

  // -------------------------------------------------------------------------
  // INSERT
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("An optional field with a null value is left out of the insert")
  void optionalFieldWithNullValueIsLeftOutOfInsert() throws Exception {
    String sql = buildInsertSql(nullNode());

    assertThat(sql).contains("IdeThm", "Act");
    assertThat(sql).doesNotContain("Nam");
  }

  @Test
  @DisplayName("An optional field with a value is kept in the insert")
  void optionalFieldWithValueIsKeptInInsert() throws Exception {
    String sql = buildInsertSql(textNode("sunset"));

    assertThat(sql).contains("IdeThm", "Nam", "Act");
  }

  @Test
  @DisplayName("An optional field with an empty value is left out, matching filter optional semantics")
  void optionalFieldWithEmptyValueIsLeftOutOfInsert() throws Exception {
    String sql = buildInsertSql(textNode(""));

    assertThat(sql).doesNotContain("Nam");
  }

  @Test
  @DisplayName("A field without the attribute is still inserted when its value is null")
  void nonOptionalFieldWithNullValueIsStillInserted() throws Exception {
    Insert insert = insertMaintain(false);

    String sql = build(insert, variables(nullNode(), false)).toString();

    assertThat(sql).contains("IdeThm", "Nam", "Act");
  }

  @Test
  @DisplayName("Columns and values stay aligned when an optional field is omitted")
  void columnsAndValuesStayAlignedWhenOptionalFieldIsOmitted() throws Exception {
    String sql = buildInsertSql(nullNode());

    // HSQLDB renders "insert into AweThm (cols) values (?, ?)" — one placeholder per column.
    // A misalignment between the two loops shows up here as a count mismatch.
    assertThat(columnCount(sql))
      .as("column count must equal placeholder count, otherwise values are shifted")
      .isEqualTo(placeholderCount(sql));
    assertThat(columnCount(sql)).isEqualTo(2);
  }

  // -------------------------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("An optional field with a null value is left out of the update")
  void optionalFieldWithNullValueIsLeftOutOfUpdate() throws Exception {
    SQLUpdateClause clause = (SQLUpdateClause) build(updateMaintain(true), variables(nullNode(), false));

    assertThat(updatedColumns(clause)).containsExactlyInAnyOrder("IdeThm", "Act");
  }

  @Test
  @DisplayName("An optional field with a value is kept in the update")
  void optionalFieldWithValueIsKeptInUpdate() throws Exception {
    SQLUpdateClause clause = (SQLUpdateClause) build(updateMaintain(true), variables(textNode("sunset"), false));

    assertThat(updatedColumns(clause)).containsExactlyInAnyOrder("IdeThm", "Nam", "Act");
  }

  @Test
  @DisplayName("A field without the attribute is still updated when its value is null")
  void nonOptionalFieldWithNullValueIsStillUpdated() throws Exception {
    SQLUpdateClause clause = (SQLUpdateClause) build(updateMaintain(false), variables(nullNode(), false));

    assertThat(updatedColumns(clause)).containsExactlyInAnyOrder("IdeThm", "Nam", "Act");
  }

  // -------------------------------------------------------------------------
  // Boundaries
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("An optional field bound to a list variable is never omitted, since a batch shares its columns")
  void optionalFieldBoundToListVariableIsNeverOmitted() throws Exception {
    JsonNode list = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.nullNode());

    String sql = build(insertMaintain(true), variables(list, true)).toString();

    assertThat(sql).contains("IdeThm", "Nam", "Act");
  }

  @Test
  @DisplayName("An optional field whose variable declares a literal value is never omitted")
  void optionalFieldWithLiteralVariableValueIsNeverOmitted() throws Exception {
    Insert insert = insertMaintain(true);
    insert.getVariableDefinitionList().stream()
      .filter(variable -> "themeName".equals(variable.getId()))
      .forEach(variable -> variable.setValue("fixed-name"));

    String sql = build(insert, variables(nullNode(), false)).toString();

    assertThat(sql).contains("IdeThm", "Nam", "Act");
  }

  @Test
  @DisplayName("An optional field with no variable at all is never omitted")
  void optionalFieldWithoutVariableIsNeverOmitted() throws Exception {
    Insert insert = insertMaintain(true);
    insert.getSqlFieldList().stream()
      .filter(field -> "Nam".equals(field.getId()))
      .forEach(field -> field.setVariable(null));

    String sql = build(insert, variables(nullNode(), false)).toString();

    assertThat(sql).contains("IdeThm", "Act");
  }

  // -------------------------------------------------------------------------
  // Fixtures
  // -------------------------------------------------------------------------

  private String buildInsertSql(JsonNode themeName) throws Exception {
    return build(insertMaintain(true), variables(themeName, false)).toString();
  }

  /**
   * Three-column maintain where the middle field ("Nam") is the one under test, so an
   * off-by-one between columns and values cannot pass unnoticed.
   */
  private Insert insertMaintain(boolean optionalName) {
    Insert insert = new Insert();
    insert.setId("InsertThemeOptional");
    insert.setTableList(List.of(Table.builder().id("AweThm").build()));
    insert.setSqlFieldList(fields(optionalName));
    insert.setVariableDefinitionList(variableDefinitions());
    return insert;
  }

  private Update updateMaintain(boolean optionalName) {
    Update update = new Update();
    update.setId("UpdateThemeOptional");
    update.setTableList(List.of(Table.builder().id("AweThm").build()));
    update.setSqlFieldList(fields(optionalName));
    update.setVariableDefinitionList(variableDefinitions());
    return update;
  }

  private List<com.almis.awe.model.entities.queries.SqlField> fields(boolean optionalName) {
    List<com.almis.awe.model.entities.queries.SqlField> fields = new ArrayList<>();
    fields.add(Field.builder().id("IdeThm").variable("themeId").build());
    Field name = Field.builder().id("Nam").variable("themeName").build();
    if (optionalName) {
      name.setOptional(true);
    }
    fields.add(name);
    fields.add(Field.builder().id("Act").variable("themeActive").build());
    return fields;
  }

  private List<Variable> variableDefinitions() {
    List<Variable> variables = new ArrayList<>();
    variables.add(variableDefinition("themeId", ParameterType.INTEGER));
    variables.add(variableDefinition("themeName", ParameterType.STRING));
    variables.add(variableDefinition("themeActive", ParameterType.INTEGER));
    return variables;
  }

  private Variable variableDefinition(String id, ParameterType type) {
    Variable variable = new Variable();
    variable.setId(id);
    variable.setType(type.name());
    return variable;
  }

  private Map<String, QueryParameter> variables(JsonNode themeName, boolean nameIsList) {
    Map<String, QueryParameter> variables = new LinkedHashMap<>();
    variables.put("themeId", new QueryParameter(JsonNodeFactory.instance.numberNode(7), false, ParameterType.INTEGER));
    variables.put("themeName", new QueryParameter(themeName, nameIsList, ParameterType.STRING));
    variables.put("themeActive", new QueryParameter(JsonNodeFactory.instance.numberNode(1), false, ParameterType.INTEGER));
    return variables;
  }

  private AbstractSQLClause<?> build(com.almis.awe.model.entities.maintain.MaintainQuery maintain,
                                     Map<String, QueryParameter> variables) throws Exception {
    StubMaintainBuilder builder = new StubMaintainBuilder(queryUtil, auditProperties());
    builder.setMaintain(maintain)
      .setFactory(factory)
      .setVariables(variables)
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.NO_BATCH)
      .setVariableIndex(0);
    return builder.build();
  }

  private DatabaseConfigProperties auditProperties() {
    DatabaseConfigProperties properties = new DatabaseConfigProperties();
    properties.setAuditUser("HISope");
    properties.setAuditDate("HISdat");
    properties.setAuditAction("HISact");
    return properties;
  }

  @SuppressWarnings("unchecked")
  private List<String> updatedColumns(SQLUpdateClause clause) throws Exception {
    java.lang.reflect.Field updatesField = AbstractSQLUpdateClause.class.getDeclaredField("updates");
    updatesField.setAccessible(true);
    Map<Path<?>, com.querydsl.core.types.Expression<?>> updates =
      (Map<Path<?>, com.querydsl.core.types.Expression<?>>) updatesField.get(clause);
    return updates.keySet().stream().map(path -> path.getMetadata().getName()).toList();
  }

  private int columnCount(String sql) {
    int open = sql.indexOf('(');
    int close = sql.indexOf(')', open);
    return sql.substring(open + 1, close).split(",").length;
  }

  private int placeholderCount(String sql) {
    int valuesAt = sql.lastIndexOf("values");
    return sql.substring(valuesAt).split("\\?", -1).length - 1;
  }

  private JsonNode nullNode() {
    return JsonNodeFactory.instance.nullNode();
  }

  private JsonNode textNode(String value) {
    return JsonNodeFactory.instance.textNode(value);
  }

  private static class StubMaintainBuilder extends SQLMaintainBuilder {
    StubMaintainBuilder(QueryUtil queryUtil, DatabaseConfigProperties databaseConfigProperties) {
      super(queryUtil, mock(EncodeService.class), databaseConfigProperties);
    }

    @Override
    public AweElements getElements() {
      return mock(AweElements.class);
    }

    @Override
    protected String getUser() {
      return "Anonymous";
    }
  }
}
