package com.almis.awe.service.data.builder;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Table;
import com.almis.awe.model.type.MaintainBuildOperation;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.Tuple;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQLMaintainBuilderTest {

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  @Test
  void buildAuditedInsertQueryUsesProvidedMaterializedRowsForBaseAndAuditSingleRow() throws Exception {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    builder.setMaintain(insertQueryMaintain())
      .setFactory(sqlQueryFactory())
      .setVariables(Map.of())
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.NO_BATCH)
      .setMaterializedInsertQueryRows(List.of(tuple(31, "ROW-SINGLE", 1)));

    Tuple row = builder.getMaterializedInsertQueryRows().get(0);

    assertEquals(List.of(31, "ROW-SINGLE", 1), builder.getBaseInsertQueryRowValues(row));
    assertEquals(List.of("Anonymous", "I", 31, "ROW-SINGLE", 1), builder.getAuditInsertQueryRowValues(row));
    assertEquals(0, builder.getSubqueryInvocationCount());
  }

  @Test
  void buildAuditedInsertQueryUsesProvidedMaterializedRowsForBaseAndAuditMultipleRows() throws Exception {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    builder.setMaintain(insertQueryMaintain())
      .setFactory(sqlQueryFactory())
      .setVariables(Map.of())
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.NO_BATCH)
      .setMaterializedInsertQueryRows(List.of(
        tuple(41, "ROW-MULTI-0", 1),
        tuple(42, "ROW-MULTI-1", 0)));

    List<Tuple> rows = builder.getMaterializedInsertQueryRows();

    assertEquals(2, rows.size());
    assertEquals(List.of(41, "ROW-MULTI-0", 1), builder.getBaseInsertQueryRowValues(rows.get(0)));
    assertEquals(List.of(42, "ROW-MULTI-1", 0), builder.getBaseInsertQueryRowValues(rows.get(1)));
    assertEquals(List.of("Anonymous", "I", 41, "ROW-MULTI-0", 1), builder.getAuditInsertQueryRowValues(rows.get(0)));
    assertEquals(List.of("Anonymous", "I", 42, "ROW-MULTI-1", 0), builder.getAuditInsertQueryRowValues(rows.get(1)));
    assertEquals(0, builder.getSubqueryInvocationCount());
  }

  @Test
  void buildAuditedInsertQueryWithoutMaterializedRowsStillAttemptsSubquery() {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    builder.setMaintain(insertQueryMaintain())
      .setFactory(sqlQueryFactory())
      .setVariables(Map.of())
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.NO_BATCH);

    assertThrows(UnsupportedOperationException.class, builder::getMaterializedInsertQueryRows);
    assertEquals(1, builder.getSubqueryInvocationCount());
  }

  @Test
  void batchInitialDefinitionForInsertQueryDoesNotResolveSubqueryOrPopulateClause() throws Exception {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    builder.setMaintain(insertQueryMaintain())
      .setFactory(sqlQueryFactory())
      .setVariables(Map.of())
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.BATCH_INITIAL_DEFINITION);

    SQLInsertClause clause = (SQLInsertClause) builder.build();

    assertTrue(clause.isEmpty());
    assertEquals(0, builder.getSubqueryInvocationCount());
  }

  @Test
  void batchInitialDefinitionForAuditedInsertQueryDoesNotResolveSubqueryOrPopulateClause() throws Exception {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    builder.setMaintain(insertQueryMaintain())
      .setFactory(sqlQueryFactory())
      .setVariables(Map.of())
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setAudit(true)
      .setOperation(MaintainBuildOperation.BATCH_INITIAL_DEFINITION);

    SQLInsertClause clause = (SQLInsertClause) builder.build();

    assertTrue(clause.isEmpty());
    assertEquals(0, builder.getSubqueryInvocationCount());
  }

  @Test
  void queryBackedAuditedFieldValueIsMaterializedOnlyOncePerKey() throws Exception {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    Field field = queryBackedAuditField("CachedAuditValue");
    builder.registerSubqueryRows("CachedAuditValueQuery", List.of(tuple("AUDIT-VALUE")));

    var firstValue = invokeMaterializedQueryFieldValue(builder, field, 0);
    var secondValue = invokeMaterializedQueryFieldValue(builder, field, 0);

    assertSame(firstValue, secondValue);
    assertEquals(1, builder.getSubqueryInvocationCount());
  }

  @Test
  void queryBackedAuditedFieldValuePropagatesWrongRowCountException() {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    Field field = queryBackedAuditField("RowCountAuditValue");
    builder.registerSubqueryRows("RowCountAuditValueQuery", List.of(tupleWithoutValues(), tupleWithoutValues()));

    AWException exception = assertThrows(AWException.class,
      () -> invokeMaterializedQueryFieldValue(builder, field, 0));

    assertEquals("Query-backed audited field 'RowCountAuditValue' must return exactly one row, but returned 2",
      exception.getMessage());
  }

  @Test
  void queryBackedAuditedFieldValuePropagatesWrongColumnCountException() {
    TrackingSQLMaintainBuilder builder = new TrackingSQLMaintainBuilder(queryUtil, databaseConfigProperties());
    Field field = queryBackedAuditField("ColumnCountAuditValue");
    builder.registerSubqueryRows("ColumnCountAuditValueQuery", List.of(tuple("AUDIT-VALUE", "UNEXPECTED-COLUMN")));

    AWException exception = assertThrows(AWException.class,
      () -> invokeMaterializedQueryFieldValue(builder, field, 0));

    assertEquals("Query-backed audited field 'ColumnCountAuditValue' must return exactly one column, but returned 2",
      exception.getMessage());
  }

  private SQLQueryFactory sqlQueryFactory() {
    return new SQLQueryFactory(new Configuration(new HSQLDBTemplates()), () -> connection);
  }

  private DatabaseConfigProperties databaseConfigProperties() {
    DatabaseConfigProperties properties = new DatabaseConfigProperties();
    properties.setAuditUser("HISope");
    properties.setAuditDate("HISdat");
    properties.setAuditAction("HISact");
    return properties;
  }

  private Insert insertQueryMaintain() {
    Insert insert = new Insert();
    insert.setId("InsertAuditInsertQuerySingle");
    insert.setAuditTable("HISAweThm");
    insert.setQuery("InsertQueryAuditSingleSource");
    insert.setTableList(List.of(Table.builder().id("AweThm").build()));
    insert.setSqlFieldList(List.of(
      Field.builder().id("IdeThm").build(),
      Field.builder().id("Nam").build(),
      Field.builder().id("Act").build()));
    return insert;
  }

  private Tuple tuple(Object... values) {
    Tuple tuple = mock(Tuple.class);
    when(tuple.toArray()).thenReturn(values);
    return tuple;
  }

  private Tuple tupleWithoutValues() {
    return mock(Tuple.class);
  }

  private Field queryBackedAuditField(String identifier) {
    return Field.builder()
      .id(identifier)
      .query(identifier + "Query")
      .build();
  }

  private Object invokeMaterializedQueryFieldValue(TrackingSQLMaintainBuilder builder, Field field, int index) throws Exception {
    Method method = SQLMaintainBuilder.class.getDeclaredMethod("getMaterializedQueryFieldValue", Field.class, Integer.class);
    method.setAccessible(true);
    try {
      return method.invoke(builder.setMaintain(insertQueryMaintain()), field, index);
    } catch (InvocationTargetException exc) {
      Throwable cause = exc.getCause();
      if (cause instanceof AWException awException) {
        throw awException;
      }
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new AssertionError("Unexpected reflection failure", cause);
    }
  }

  private static class TrackingSQLMaintainBuilder extends SQLMaintainBuilder {
    private int subqueryInvocationCount;
    private final Map<String, SQLQuery<Tuple>> subqueries = new HashMap<>();

    TrackingSQLMaintainBuilder(QueryUtil queryUtil, DatabaseConfigProperties databaseConfigProperties) {
      super(queryUtil, mock(EncodeService.class), databaseConfigProperties);
    }

    void registerSubqueryRows(String queryId, List<Tuple> rows) {
      SQLQuery<Tuple> query = mock(SQLQuery.class);
      when(query.fetch()).thenReturn(rows);
      subqueries.put(queryId, query);
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      subqueryInvocationCount++;
      if (subqueries.containsKey(queryId)) {
        return subqueries.get(queryId);
      }
      throw new UnsupportedOperationException("Subquery execution should not happen when rows are already materialized");
    }

    @Override
    public AweElements getElements() {
      return mock(AweElements.class);
    }

    int getSubqueryInvocationCount() {
      return subqueryInvocationCount;
    }

    @Override
    protected String getUser() {
      return "Anonymous";
    }
  }
}
