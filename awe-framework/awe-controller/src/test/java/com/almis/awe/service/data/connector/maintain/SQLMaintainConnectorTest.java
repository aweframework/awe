package com.almis.awe.service.data.connector.maintain;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.builder.SQLMaintainBuilder;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.dml.AbstractSQLClause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQLMaintainConnectorTest {

  private static final String GENERIC_ERROR_TITLE = "maintain error title";
  private static final String GENERIC_ERROR_MESSAGE = "maintain error message";
  private static final String MULTIPLE_LIST_VARIABLE_ID = "selectedRows";

  @InjectMocks
  private SQLMaintainConnector sqlMaintainConnector;

  @Mock
  private ApplicationContext context;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private DatabaseConfigProperties databaseConfigProperties;

  @Mock
  private DatabaseConnection databaseConnection;

  @Mock
  private SQLMaintainBuilder sqlMaintainBuilder;

  @Mock
  private AbstractSQLClause abstractSQLClause;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    sqlMaintainConnector.setApplicationContext(context);
  }

  private void mockGenericMaintainLocale() {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLanguage()).thenReturn("en");
    when(aweElements.getLocaleWithLanguage("ERROR_TITLE_DURING_MAINTAIN", "en")).thenReturn(GENERIC_ERROR_TITLE);
    when(aweElements.getLocaleWithLanguage("ERROR_MESSAGE_DURING_MAINTAIN", "en")).thenReturn(GENERIC_ERROR_MESSAGE);
  }

  private Insert newMultipleInsert(List<Variable> variables) {
    Insert insertMaintain = new Insert();
    insertMaintain.setId("TestMaintain");
    insertMaintain.setOperationId("TestMaintain");
    insertMaintain.setVariableIndex(0);
    insertMaintain.setMultiple("true");
    insertMaintain.setVariableDefinitionList(variables);
    return insertMaintain;
  }

  private Variable newVariable(String id, boolean audit) {
    Variable variable = new Variable();
    variable.setId(id);
    variable.setAudit(audit);
    return variable;
  }

  private QueryParameter listParameter(ArrayNode value) {
    return new QueryParameter(value, true, ParameterType.OBJECT);
  }

  private void mockConfigurationBean() {
    when(databaseConnection.getConfigurationBean()).thenReturn("configDummyBean");
    when(context.getBean("configDummyBean")).thenReturn(new Configuration(new HSQLDBTemplates()));
  }

  @Test
  void givenAuditFlagAsFalse_launchSingleMaintain() throws AWException {

    Insert insertMaintain = new Insert();
    insertMaintain.setVariableIndex(0);

    // When
    when(queryUtil.getDefaultVariableMap(any())).thenReturn(new HashMap<>());
    when(databaseConfigProperties.isAuditEnable()).thenReturn(false);
    when(context.getBean(SQLMaintainBuilder.class)).thenReturn(sqlMaintainBuilder);
    when(databaseConnection.getConfigurationBean()).thenReturn("configDummyBean");
    final Configuration configuration = new Configuration(new HSQLDBTemplates());
    when(context.getBean("configDummyBean")).thenReturn(configuration);
    when(sqlMaintainBuilder.setMaintain(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariableIndex(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setOperation(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setFactory(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariables(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setParameters(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.build()).thenReturn(abstractSQLClause);
    when(queryUtil.getFullSQL(any(), any())).thenReturn("insert sql");
    when(abstractSQLClause.getSQL()).thenReturn(Collections.singletonList(new SQLBindings("insert (dummy) values (value)", Collections.emptyList())));

    // Do
    ServiceData maintainOut = sqlMaintainConnector.launch(insertMaintain, databaseConnection, objectMapper.createObjectNode());

    // Asserts
    assertNotNull(maintainOut);
    assertEquals(AnswerType.OK, maintainOut.getType());
  }

  @Test
  void givenAuditFlagAsFalse_launchMultipleMaintainAudit() throws AWException {

    Insert insertMaintain = new Insert();
    insertMaintain.setVariableIndex(0);
    insertMaintain.setMultiple("audit");

    // When
    when(queryUtil.getDefaultVariableMap(any())).thenReturn(new HashMap<>());
    when(databaseConfigProperties.isAuditEnable()).thenReturn(false);
    when(context.getBean(SQLMaintainBuilder.class)).thenReturn(sqlMaintainBuilder);
    when(databaseConnection.getConfigurationBean()).thenReturn("configDummyBean");
    final Configuration configuration = new Configuration(new HSQLDBTemplates());
    when(context.getBean("configDummyBean")).thenReturn(configuration);
    when(sqlMaintainBuilder.setMaintain(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setOperation(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setFactory(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariables(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setParameters(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.build()).thenReturn(abstractSQLClause);
    when(queryUtil.getFullSQL(any(), any())).thenReturn("insert sql");
    when(abstractSQLClause.getSQL()).thenReturn(Collections.singletonList(new SQLBindings("insert (dummy) values (value)", Collections.emptyList())));

    // Do
    ServiceData maintainOut = sqlMaintainConnector.launch(insertMaintain, databaseConnection, objectMapper.createObjectNode());

    // Asserts
    assertNotNull(maintainOut);
    assertEquals(AnswerType.OK, maintainOut.getType());
  }

  @Test
  void givenExecutionFailure_wrapsExceptionWithBoundSql() throws AWException {

    Insert insertMaintain = new Insert();
    insertMaintain.setId("TestMaintain");
    insertMaintain.setOperationId("TestMaintain");
    insertMaintain.setVariableIndex(0);

    RuntimeException expectedCause = new RuntimeException("db failure");
    String placeholderSql = "insert into test_table (name) values (?)";
    String fullSql = "insert into test_table (name) values ('John')";

    when(queryUtil.getDefaultVariableMap(any())).thenReturn(new HashMap<>());
    when(databaseConfigProperties.isAuditEnable()).thenReturn(false);
    when(context.getBean(SQLMaintainBuilder.class)).thenReturn(sqlMaintainBuilder);
    when(databaseConnection.getConfigurationBean()).thenReturn("configDummyBean");
    when(context.getBean("configDummyBean")).thenReturn(new Configuration(new HSQLDBTemplates()));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("translated");
    when(sqlMaintainBuilder.setMaintain(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariableIndex(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setOperation(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setFactory(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariables(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setParameters(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.build()).thenReturn(abstractSQLClause);
    when(abstractSQLClause.getSQL()).thenReturn(Collections.singletonList(new SQLBindings(placeholderSql, Collections.singletonList("John"))));
    when(queryUtil.getFullSQL(placeholderSql, Collections.singletonList("John"))).thenReturn(fullSql);
    when(abstractSQLClause.execute()).thenThrow(expectedCause);

    AWEQueryException exception = assertThrows(AWEQueryException.class,
      () -> sqlMaintainConnector.launch(insertMaintain, databaseConnection, objectMapper.createObjectNode()));

    assertEquals(fullSql, exception.getQuery());
    assertEquals(expectedCause, exception.getCause());
    assertTrue(exception.getQuery().contains("'John'"));
    verify(queryUtil).getFullSQL(placeholderSql, Collections.singletonList("John"));
  }

  @Test
  void givenMultipleMaintainWithoutDeclaredNonAuditListParameter_throwsGenericMaintainErrorBeforeBuilderExecution() {
    Insert insertMaintain = newMultipleInsert(List.of(newVariable(MULTIPLE_LIST_VARIABLE_ID, false)));
    Map<String, QueryParameter> parameterMap = new HashMap<>();

    mockGenericMaintainLocale();
    when(queryUtil.getDefaultVariableMap(any())).thenReturn(parameterMap);

    AWException exception = assertThrows(AWException.class,
      () -> sqlMaintainConnector.launch(insertMaintain, databaseConnection, JsonNodeFactory.instance.objectNode()));

    assertEquals(GENERIC_ERROR_TITLE, exception.getTitle());
    assertEquals(GENERIC_ERROR_MESSAGE, exception.getMessage());
    verify(context, never()).getBean(SQLMaintainBuilder.class);
  }

  @Test
  void givenMultipleMaintainWithScalarNonAuditParameter_throwsGenericMaintainErrorBeforeBuilderExecution() {
    Insert insertMaintain = newMultipleInsert(List.of(newVariable(MULTIPLE_LIST_VARIABLE_ID, false)));
    Map<String, QueryParameter> parameterMap = new HashMap<>();
    parameterMap.put(MULTIPLE_LIST_VARIABLE_ID,
      new QueryParameter(JsonNodeFactory.instance.textNode("row-1"), false, ParameterType.STRING));

    mockGenericMaintainLocale();
    when(queryUtil.getDefaultVariableMap(any())).thenReturn(parameterMap);

    AWException exception = assertThrows(AWException.class,
      () -> sqlMaintainConnector.launch(insertMaintain, databaseConnection, JsonNodeFactory.instance.objectNode()));

    assertEquals(GENERIC_ERROR_TITLE, exception.getTitle());
    assertEquals(GENERIC_ERROR_MESSAGE, exception.getMessage());
    verify(context, never()).getBean(SQLMaintainBuilder.class);
  }

  @Test
  void givenMultipleMaintainWithOnlyAuditList_throwsGenericMaintainErrorBeforeBuilderExecution() {
    Insert insertMaintain = newMultipleInsert(List.of(newVariable(MULTIPLE_LIST_VARIABLE_ID, true)));
    Map<String, QueryParameter> parameterMap = new HashMap<>();
    parameterMap.put(MULTIPLE_LIST_VARIABLE_ID, listParameter(JsonNodeFactory.instance.arrayNode().add("row-1")));

    mockGenericMaintainLocale();
    when(queryUtil.getDefaultVariableMap(any())).thenReturn(parameterMap);

    AWException exception = assertThrows(AWException.class,
      () -> sqlMaintainConnector.launch(insertMaintain, databaseConnection, JsonNodeFactory.instance.objectNode()));

    assertEquals(GENERIC_ERROR_TITLE, exception.getTitle());
    assertEquals(GENERIC_ERROR_MESSAGE, exception.getMessage());
    verify(context, never()).getBean(SQLMaintainBuilder.class);
  }

  @Test
  void givenMultipleMaintainWithEmptyNonAuditList_validationPassesWithoutExecutingSql() throws AWException {
    Insert insertMaintain = newMultipleInsert(List.of(newVariable(MULTIPLE_LIST_VARIABLE_ID, false)));
    Map<String, QueryParameter> parameterMap = new HashMap<>();
    parameterMap.put(MULTIPLE_LIST_VARIABLE_ID, listParameter(JsonNodeFactory.instance.arrayNode()));

    when(queryUtil.getDefaultVariableMap(any())).thenReturn(parameterMap);
    when(context.getBean(SQLMaintainBuilder.class)).thenReturn(sqlMaintainBuilder);
    mockConfigurationBean();
    when(sqlMaintainBuilder.setMaintain(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setOperation(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setFactory(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariables(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setParameters(any())).thenReturn(sqlMaintainBuilder);

    ServiceData maintainOut = sqlMaintainConnector.launch(insertMaintain, databaseConnection, JsonNodeFactory.instance.objectNode());

    assertNotNull(maintainOut);
    assertEquals(AnswerType.OK, maintainOut.getType());
    verify(sqlMaintainBuilder, never()).build();
  }

  @Test
  void givenMultipleMaintainWithNonEmptyNonAuditList_executesMultipleMaintainPath() throws AWException {
    Insert insertMaintain = newMultipleInsert(List.of(newVariable(MULTIPLE_LIST_VARIABLE_ID, false)));
    Map<String, QueryParameter> parameterMap = new HashMap<>();
    parameterMap.put(MULTIPLE_LIST_VARIABLE_ID, listParameter(JsonNodeFactory.instance.arrayNode().add("row-1")));

    when(queryUtil.getDefaultVariableMap(any())).thenReturn(parameterMap);
    when(databaseConfigProperties.isAuditEnable()).thenReturn(false);
    when(context.getBean(SQLMaintainBuilder.class)).thenReturn(sqlMaintainBuilder);
    mockConfigurationBean();
    when(sqlMaintainBuilder.setMaintain(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setOperation(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setFactory(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariables(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setParameters(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setVariableIndex(any())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.setAudit(anyBoolean())).thenReturn(sqlMaintainBuilder);
    when(sqlMaintainBuilder.build()).thenReturn(abstractSQLClause);
    when(queryUtil.getFullSQL(any(), any())).thenReturn("insert sql");
    when(abstractSQLClause.getSQL()).thenReturn(Collections.singletonList(new SQLBindings("insert (dummy) values (value)", Collections.emptyList())));

    ServiceData maintainOut = sqlMaintainConnector.launch(insertMaintain, databaseConnection, JsonNodeFactory.instance.objectNode());

    assertNotNull(maintainOut);
    assertEquals(AnswerType.OK, maintainOut.getType());
    verify(sqlMaintainBuilder).build();
  }
}
