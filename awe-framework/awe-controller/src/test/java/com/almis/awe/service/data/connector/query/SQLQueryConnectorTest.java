package com.almis.awe.service.data.connector.query;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.SQLQueryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SQLQueryConnector}.
 *
 * <p>Focus: verify that the JDBC connection obtained from the pool is <strong>always</strong>
 * released back via {@link DataSourceUtils#releaseConnection} regardless of whether the
 * query succeeds, throws a checked AWException, or throws an unchecked runtime exception.
 * This guards against connection-pool exhaustion (idle connections in PostgreSQL) that was
 * observed during pentesting when queries like {@code getScreenConfiguration} or
 * {@code getOptionRestrictionFromDatabase} failed at execution time.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SQLQueryConnectorTest {

  // -------------------------------------------------------------------------
  // Mocks matching SQLQueryConnector's @Autowired constructor parameters
  // -------------------------------------------------------------------------

  @Mock private AweDatabaseContextHolder contextHolder;
  @Mock private QueryUtil queryUtil;
  @Mock private DataSource dataSource;
  @Mock private BaseConfigProperties baseConfigProperties;
  @Mock private AweElements elements;
  @Mock private NumericService numericService;
  @Mock private EncodeService encodeService;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ObjectMapper mapper;

  // Spring ApplicationContext used by ServiceConfig.getBean(...)
  @Mock private ApplicationContext applicationContext;

  // -------------------------------------------------------------------------
  // Collaborator mocks returned by ApplicationContext
  // -------------------------------------------------------------------------

  @Mock private SQLQueryBuilder sqlQueryBuilder;
  @Mock private DataListBuilder dataListBuilder;
  @Mock private Configuration dbConfiguration;
  @Mock private DatabaseConnection databaseConnection;
  @Mock private Connection connection;

  @Mock @SuppressWarnings("rawtypes")
  private SQLQuery sqlQuery;          // raw type intentional – avoids generic-cast issues

  // -------------------------------------------------------------------------
  // System under test — built manually to match the constructor
  // -------------------------------------------------------------------------

  private SQLQueryConnector connector;

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    // Build the connector using its explicit constructor (mirrors Spring wiring)
    connector = new SQLQueryConnector(
      contextHolder,
      queryUtil,
      dataSource,
      baseConfigProperties,
      elements,
      numericService,
      encodeService,
      databaseConfigProperties,
      mapper
    );
    connector.setApplicationContext(applicationContext);

    // --- Common stubs for connection acquisition (primary datasource path) ---
    when(contextHolder.getDatabaseConnection(dataSource)).thenReturn(databaseConnection);
    when(databaseConnection.getConnectionType()).thenReturn("PostgreSQL");
    // The connector calls getBean(type + "DatabaseConfiguration") where type = "PostgreSQL"
    when(applicationContext.getBean("PostgreSQLDatabaseConfiguration")).thenReturn(dbConfiguration);

    // --- Common stubs for SQLQueryBuilder ---
    when(applicationContext.getBean(SQLQueryBuilder.class)).thenReturn(sqlQueryBuilder);
    when(sqlQueryBuilder.setQuery(any(Query.class))).thenReturn(sqlQueryBuilder);
    when(sqlQueryBuilder.setFactory(any(SQLQueryFactory.class))).thenReturn(sqlQueryBuilder);
    when(sqlQueryBuilder.setParameters(any(ObjectNode.class))).thenReturn(sqlQueryBuilder);
    when(sqlQueryBuilder.setComponentSort(any())).thenReturn(sqlQueryBuilder);
    when(sqlQueryBuilder.queryForCount()).thenReturn(sqlQueryBuilder);

    // --- Default variable map: pagination OFF (max=0), page=1 ---
    when(sqlQueryBuilder.getVariables()).thenReturn(buildVariableMap(0L, 1L));

    // --- SQLQuery produced by builder ---
    when(sqlQueryBuilder.build()).thenReturn(sqlQuery);

    // --- SQL metadata stubs needed for logging ---
    SQLBindings sqlBindings = mock(SQLBindings.class);
    when(sqlBindings.getSQL()).thenReturn("SELECT 1");
    when(sqlBindings.getNullFriendlyBindings()).thenReturn(Collections.emptyList());
    when(sqlQuery.getSQL()).thenReturn(sqlBindings);

    QueryMetadata metadata = mock(QueryMetadata.class);
    when(sqlQuery.getMetadata()).thenReturn(metadata);
    when(metadata.getProjection()).thenReturn(mock(Expression.class));

    // --- DataListBuilder ---
    when(applicationContext.getBean(DataListBuilder.class)).thenReturn(dataListBuilder);
    when(dataListBuilder.setQueryProjection(any())).thenReturn(dataListBuilder);
    when(dataListBuilder.setQueryResult(any())).thenReturn(dataListBuilder);
    when(dataListBuilder.setRecords(anyLong())).thenReturn(dataListBuilder);
    when(dataListBuilder.setMax(anyLong())).thenReturn(dataListBuilder);
    when(dataListBuilder.setPage(anyLong())).thenReturn(dataListBuilder);
    when(dataListBuilder.paginate(anyBoolean())).thenReturn(dataListBuilder);
    when(dataListBuilder.generateIdentifiers()).thenReturn(dataListBuilder);
    when(dataListBuilder.build()).thenReturn(new com.almis.awe.model.dto.DataList());

    // --- Locale helper (AweElements) used when building AWEQueryException messages ---
    when(applicationContext.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");

    // --- QueryUtil stub: getFullSQL must return a non-null string to avoid NPE in StringUtil.toUnilineText ---
    when(queryUtil.getFullSQL(anyString(), any())).thenReturn("SELECT 1");
    // getRequestParameter returns null by default (no database routing), which is correct for primary datasource tests
    when(queryUtil.getRequestParameter(any(), any())).thenReturn(null);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private Map<String, QueryParameter> buildVariableMap(long max, long page) {
    Map<String, QueryParameter> map = new HashMap<>();
    map.put(AweConstants.QUERY_MAX,  new QueryParameter(com.fasterxml.jackson.databind.node.LongNode.valueOf(max)));
    map.put(AweConstants.QUERY_PAGE, new QueryParameter(com.fasterxml.jackson.databind.node.LongNode.valueOf(page)));
    return map;
  }

  private Query buildQuery(String id) {
    return new Query().setId(id).setIsPublic(true);
  }

  private ObjectNode emptyParams() {
    return JsonNodeFactory.instance.objectNode();
  }

  // =========================================================================
  // Tests
  // =========================================================================

  // -------------------------------------------------------------------------
  @Nested
  @DisplayName("Connection lifecycle — happy path")
  class ConnectionLifecycleHappyPath {

    @Test
    @DisplayName("Releases connection when query succeeds (no pagination)")
		void releasesConnectionOnSuccess() throws Exception {
      when(sqlQuery.fetch()).thenReturn(Collections.emptyList());

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        ServiceData result = connector.launch(buildQuery("q1"), emptyParams());

        // Connection must be released exactly once
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
        assertThat(result).isNotNull();
      }
    }

    @Test
    @DisplayName("Returns ServiceData with non-null DataList on empty result set")
		void returnsServiceDataWithDataListOnEmptyResult() throws Exception {
      when(sqlQuery.fetch()).thenReturn(Collections.emptyList());

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        ServiceData result = connector.launch(buildQuery("qEmpty"), emptyParams());

        assertThat(result.getDataList()).isNotNull();
      }
    }

    @Test
    @DisplayName("Returns ServiceData with results when fetch() returns rows")
		void returnsServiceDataWithRowsWhenFetchReturnsData() throws Exception {
      Tuple tuple = mock(Tuple.class);
      when(sqlQuery.fetch()).thenReturn(Collections.singletonList(tuple));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        ServiceData result = connector.launch(buildQuery("qRows"), emptyParams());

        assertThat(result).isNotNull();
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when query fails during pagination count (max > 0)")
    void releasesConnectionOnPaginationCountFailure() throws Exception {
      // Pagination ON: max=10, page=2 — fetchCount will fail because the internal
      // SQLQueryFactory is not injectable; we verify the connection is always released
      when(sqlQueryBuilder.getVariables()).thenReturn(buildVariableMap(10L, 2L));
      when(sqlQuery.limit(10L)).thenReturn(sqlQuery);
      when(sqlQuery.offset(10L)).thenReturn(sqlQuery);
      when(sqlQuery.fetch()).thenReturn(Collections.emptyList());

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        // The count query will fail internally (no real datasource), which is expected;
        // the critical invariant is that the connection is released regardless
        try {
          connector.launch(buildQuery("qPaginated"), emptyParams());
        } catch (Exception ignored) {
          // Exception is expected when the internal count query fails
        }

        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }
  }

  // -------------------------------------------------------------------------
  @Nested
  @DisplayName("Connection lifecycle — error paths (core regression guard)")
  class ConnectionLifecycleErrorPaths {

    @Test
    @DisplayName("Releases connection when fetch() throws RuntimeException — regression guard for pool exhaustion")
		void releasesConnectionWhenFetchThrowsRuntimeException() {
      when(sqlQuery.fetch()).thenThrow(new RuntimeException("DB timeout"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qFetchFail"), emptyParams()))
          .isInstanceOf(AWEQueryException.class);

        // THE CRITICAL ASSERTION: connection MUST be released even when an exception occurs
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when fetch() throws a database-level exception (e.g. query timeout)")
		void releasesConnectionWhenFetchThrowsSQLException() {
      when(sqlQuery.fetch()).thenThrow(new RuntimeException(
        new java.sql.SQLException("canceling statement due to statement timeout")));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qSQLFail"), emptyParams()))
          .isInstanceOf(AWEQueryException.class);

        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when DataList building fails after successful fetch()")
		void releasesConnectionWhenDataListBuildingFails() throws Exception {
      when(sqlQuery.fetch()).thenReturn(Collections.emptyList());
      when(dataListBuilder.build()).thenThrow(new RuntimeException("Mapping error"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qDataListFail"), emptyParams()))
          .isInstanceOf(AWEQueryException.class);

        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when SQLQueryBuilder.build() throws AWException")
		void releasesConnectionWhenBuilderThrowsAWException() throws Exception {
      when(sqlQueryBuilder.build()).thenThrow(new AWException("build error", "detail"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qBuildFail"), emptyParams()))
          .isInstanceOf(AWException.class);

        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when getVariables() throws RuntimeException")
    void releasesConnectionWhenGetVariablesThrows() throws AWException {
      when(sqlQueryBuilder.getVariables()).thenThrow(new RuntimeException("variables error"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qVarFail"), emptyParams()))
          .isInstanceOf(RuntimeException.class);

        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Wraps fetch() exception with query SQL information in AWEQueryException")
		void wrapsExceptionWithQuerySQLInformation() {
      when(sqlQuery.fetch()).thenThrow(new RuntimeException("original cause"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qWrapped"), emptyParams()))
          .isInstanceOf(AWEQueryException.class)
          .hasRootCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("original cause");
      }
    }
  }

  // -------------------------------------------------------------------------
  @Nested
  @DisplayName("Alternate datasource routing")
  class AlternateDatasourceRouting {

    @Mock private DataSource alternateDataSource;
    @Mock private DatabaseConnection alternateDatabaseConnection;
    @Mock private Connection altConnection;

    @Test
    @DisplayName("Uses alternate datasource and releases its connection when 'database' param is set")
		void usesAlternateDataSourceAndReleasesItsConnection() throws Exception {
      String dbParamName = "database";
      when(databaseConfigProperties.getParameterName()).thenReturn(dbParamName);

      ObjectNode params = JsonNodeFactory.instance.objectNode();
      params.put(dbParamName, "secondary");

      // Override default stub: return "secondary" for routing
      when(queryUtil.getRequestParameter(eq(dbParamName), any())).thenReturn(
        JsonNodeFactory.instance.textNode("secondary"));

      when(contextHolder.getDatabaseConnection("secondary")).thenReturn(alternateDatabaseConnection);
      when(alternateDatabaseConnection.getConnectionType()).thenReturn("PostgreSQL");
      when(alternateDatabaseConnection.getDataSource()).thenReturn(alternateDataSource);

      when(sqlQuery.fetch()).thenReturn(Collections.emptyList());

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(alternateDataSource)).thenReturn(altConnection);

        connector.launch(buildQuery("qAlt"), params);

        // Must release from the ALTERNATE datasource, NOT the primary one
        dsu.verify(() -> DataSourceUtils.releaseConnection(altConnection, alternateDataSource), times(1));
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), eq(dataSource)), never());
      }
    }

    @Test
    @DisplayName("Releases alternate connection even if query fails on alternate datasource")
		void releasesAlternateConnectionOnFailure() throws Exception {
      String dbParamName = "database";
      when(databaseConfigProperties.getParameterName()).thenReturn(dbParamName);

      ObjectNode params = JsonNodeFactory.instance.objectNode();
      params.put(dbParamName, "secondary");

      // Override default stub: return "secondary" for routing
      when(queryUtil.getRequestParameter(eq(dbParamName), any())).thenReturn(
        JsonNodeFactory.instance.textNode("secondary"));

      when(contextHolder.getDatabaseConnection("secondary")).thenReturn(alternateDatabaseConnection);
      when(alternateDatabaseConnection.getConnectionType()).thenReturn("PostgreSQL");
      when(alternateDatabaseConnection.getDataSource()).thenReturn(alternateDataSource);

      when(sqlQuery.fetch()).thenThrow(new RuntimeException("network error"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(alternateDataSource)).thenReturn(altConnection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qAltFail"), params))
          .isInstanceOf(AWEQueryException.class);

        dsu.verify(() -> DataSourceUtils.releaseConnection(altConnection, alternateDataSource), times(1));
      }
    }
  }

  // -------------------------------------------------------------------------
  @Nested
  @DisplayName("Connection not obtained if factory setup fails")
  class ConnectionNotObtainedIfSetupFails {

    @Test
    @DisplayName("Does not attempt to release connection if contextHolder throws before DataSourceUtils.getConnection")
    void doesNotReleaseIfContextHolderFails() throws Exception {
      when(contextHolder.getDatabaseConnection(dataSource))
        .thenThrow(new RuntimeException("context holder failure"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(() -> connector.launch(buildQuery("qCtxFail"), emptyParams()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("context holder failure");

        // Connection was never obtained, so it must never be released
        dsu.verify(() -> DataSourceUtils.getConnection(any(DataSource.class)), never());
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }

    @Test
    @DisplayName("Releases connection immediately if getConfiguration() fails after connection was obtained — regression guard for pool leak")
    void releasesConnectionImmediatelyIfGetConfigurationFails() {
      // Simulate the bean not found → getConfiguration returns null → NPE on setUseLiterals
      when(applicationContext.getBean("PostgreSQLDatabaseConfiguration")).thenReturn(null);

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        dsu.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);

        assertThatThrownBy(() -> connector.launch(buildQuery("qConfigFail"), emptyParams()))
          .isInstanceOf(NullPointerException.class);

        // Connection MUST be released even though the failure happened inside getQueryFactoryWithConnection
        dsu.verify(() -> DataSourceUtils.getConnection(dataSource), times(1));
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }
  }

  // -------------------------------------------------------------------------
  @Nested
  @DisplayName("subscribe() is not supported")
  class SubscribeNotSupported {

    @Test
    @DisplayName("subscribe() throws UnsupportedOperationException")
    void subscribeThrowsUnsupportedOperationException() {
      assertThatThrownBy(() -> connector.subscribe(buildQuery("q"), null, emptyParams()))
        .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}















