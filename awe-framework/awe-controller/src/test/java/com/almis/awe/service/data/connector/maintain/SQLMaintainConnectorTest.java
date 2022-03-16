package com.almis.awe.service.data.connector.maintain;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.builder.SQLMaintainBuilder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQLMaintainConnectorTest {

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

  @BeforeEach
  void setUp() {
    sqlMaintainConnector.setApplicationContext(context);
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
}