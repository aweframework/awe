package com.almis.awe.service;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the connection-lifecycle management in {@link MaintainService#launchMaintain}.
 *
 * <p>These tests guard against the connection-pool exhaustion that was observed during pentesting:
 * connections obtained for maintain operations must always be released back to HikariCP via
 * {@link DataSourceUtils#releaseConnection}, regardless of whether the operation succeeds,
 * rolls back, or throws an unexpected exception.
 *
 * <p>Key invariants verified:
 * <ul>
 *   <li>Connection is released exactly once in the {@code finally} block on success.</li>
 *   <li>Connection is released exactly once even when an {@link AWException} is thrown.</li>
 *   <li>Connection is released exactly once even when an unexpected {@link RuntimeException} is thrown.</li>
 *   <li>When {@code keepAliveConnection=true} the caller owns the connection, and it is NOT released.</li>
 *   <li>When databaseConnection is null, no release is attempted and manageConnection is set to false.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintainServiceConnectionTest {

  // -----------------------------------------------------------------------
  // Infrastructure mocks
  // -----------------------------------------------------------------------

  @Mock private MaintainLauncher maintainLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseConnection databaseConnection;

  // -----------------------------------------------------------------------
  // System under test
  // -----------------------------------------------------------------------

  private MaintainService maintainService;

  // -----------------------------------------------------------------------
  // Setup
  // -----------------------------------------------------------------------

  @BeforeEach
  void setUp() throws Exception {
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties);
    maintainService.setApplicationContext(applicationContext);

    // Locale helpers used when building exception messages
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");

    // Default: databaseConnection has a datasource and connection
    when(databaseConnection.getDataSource()).thenReturn(dataSource);
    when(databaseConnection.getConnection()).thenReturn(connection);
    when(databaseConnection.hasConnection()).thenReturn(true);
    when(databaseConnection.getDatabaseAlias()).thenReturn("default");

    // connection.setAutoCommit / commit / rollback — do nothing by default
    doNothing().when(connection).setAutoCommit(false);
    doNothing().when(connection).commit();
    doNothing().when(connection).rollback();
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Target emptyTarget(String name) {
    Target t = new Target();
    t.setName(name);
    t.setQueryList(Collections.emptyList());
    return t;
  }

  private Target targetWithInsert(String name) {
    Target t = new Target();
    t.setName(name);
    Insert insert = new Insert();
    insert.setVariableDefinitionList(Collections.emptyList());
    t.setQueryList(List.of(insert));
    return t;
  }

  private ObjectNode emptyParams() {
    return JsonNodeFactory.instance.objectNode();
  }

  // =======================================================================
  // Nested test classes
  // =======================================================================

  @Nested
  @DisplayName("Happy path — connection lifecycle")
  class HappyPath {

    @Test
    @DisplayName("Releases connection on success (empty target)")
    void releasesConnectionOnSuccessEmptyTarget() throws Exception {
      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        ServiceData result = maintainService.launchMaintain(emptyTarget("t1"), emptyParams(), databaseConnection, false);

        assertThat(result).isNotNull();
        // Connection released exactly once via DataSourceUtils
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection on success (target with Insert)")
    void releasesConnectionOnSuccessWithInsert() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), eq(databaseConnection), any()))
        .thenReturn(new ServiceData());

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        ServiceData result = maintainService.launchMaintain(targetWithInsert("t1b"), emptyParams(), databaseConnection, false);

        assertThat(result).isNotNull();
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Does NOT release connection when keepAliveConnection=true (caller owns it)")
    void doesNotReleaseConnectionWhenKeepAlive() throws Exception {
      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        maintainService.launchMaintain(emptyTarget("t2"), emptyParams(), databaseConnection, true);

        // manageConnection=false → no release
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }

    @Test
    @DisplayName("Does not release when databaseConnection is null (manageConnection set to false)")
    void doesNotReleaseWhenDatabaseConnectionIsNull() throws Exception {
      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        // null databaseConnection → getConnection() cannot be called → manageConnection = false
        ServiceData result = maintainService.launchMaintain(emptyTarget("t3"), emptyParams(), null, false);

        assertThat(result).isNotNull();
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }

    @Test
    @DisplayName("Does not release when connection inside databaseConnection is null (manageConnection set to false)")
    void doesNotReleaseWhenConnectionIsNull() throws Exception {
      when(databaseConnection.getConnection()).thenReturn(null);
      when(databaseConnection.hasConnection()).thenReturn(false);

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        ServiceData result = maintainService.launchMaintain(emptyTarget("t3b"), emptyParams(), databaseConnection, false);

        assertThat(result).isNotNull();
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }
  }

  @Nested
  @DisplayName("Error paths — connection lifecycle (regression guard for pool exhaustion)")
  class ErrorPaths {

    @Test
    @DisplayName("Releases connection when AWException is thrown during maintain execution")
    void releasesConnectionOnAWException() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenThrow(new AWException("err title", "err msg"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(
          () -> maintainService.launchMaintain(targetWithInsert("t4"), emptyParams(), databaseConnection, false))
          .isInstanceOf(AWException.class);

        // Connection must be released exactly once despite the exception
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Releases connection when RuntimeException is thrown — regression guard for pool exhaustion")
    void releasesConnectionOnRuntimeException() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenThrow(new RuntimeException("unexpected DB failure"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        // RuntimeException is wrapped in AWException by the catch block
        assertThatThrownBy(
          () -> maintainService.launchMaintain(targetWithInsert("t5"), emptyParams(), databaseConnection, false))
          .isInstanceOf(AWException.class);

        // Connection MUST be released even after a RuntimeException
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Rolls back and releases connection when AWException is thrown")
    void rollsBackAndReleasesOnAWException() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenThrow(new AWException("title", "msg"));

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(
          () -> maintainService.launchMaintain(targetWithInsert("t6"), emptyParams(), databaseConnection, false))
          .isInstanceOf(AWException.class);

        // Rollback must have been called on the underlying connection
        verify(connection, times(1)).rollback();
        // And the connection must be released
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }

    @Test
    @DisplayName("Does NOT release connection if setAutoCommit itself fails (hasConnection still true, but rollback+release happen)")
    void releasesConnectionIfSetAutoCommitFails() throws Exception {
      doThrow(new SQLException("cannot set autocommit")).when(connection).setAutoCommit(false);

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(
          () -> maintainService.launchMaintain(emptyTarget("t7"), emptyParams(), databaseConnection, false))
          .isInstanceOf(AWException.class);

        // Connection must still be released in the final block
        dsu.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource), times(1));
      }
    }
  }

  @Nested
  @DisplayName("doRollback — does not close connection (only rolls back)")
  class DoRollbackTests {

    @Test
    @DisplayName("doRollback calls rollback() but does NOT close the connection")
    void doRollbackCallsRollbackWithoutClosingConnection() throws Exception {
      maintainService.doRollback(databaseConnection, "", true);

      verify(connection, times(1)).rollback();
      verify(connection, never()).close();
    }

    @Test
    @DisplayName("doRollback wraps SQLException from rollback() into AWException")
    void doRollbackWrapsSQLException() throws Exception {
      doThrow(new SQLException("rollback failed")).when(connection).rollback();

      assertThatThrownBy(() -> maintainService.doRollback(databaseConnection, "stmt", true))
        .isInstanceOf(AWException.class);
    }

    @Test
    @DisplayName("doRollback does nothing when manageConnection=false")
    void doRollbackDoesNothingWhenNotManaged() throws Exception {
      maintainService.doRollback(databaseConnection, "", false);

      verify(connection, never()).rollback();
    }

    @Test
    @DisplayName("doRollback does nothing when connection was never obtained (hasConnection=false)")
    void doRollbackDoesNothingWhenConnectionNeverObtained() throws Exception {
      when(databaseConnection.hasConnection()).thenReturn(false);
      maintainService.doRollback(databaseConnection, "", true);

      verify(connection, never()).rollback();
    }
  }

  @Nested
  @DisplayName("doCommit — does not close connection (only commits)")
  class DoCommitTests {

    @Test
    @DisplayName("doCommit calls commit() but does NOT close the connection")
    void doCommitCallsCommitWithoutClosingConnection() throws Exception {
      maintainService.doCommit(databaseConnection, "", true);

      verify(connection, times(1)).commit();
      verify(connection, never()).close();
    }

    @Test
    @DisplayName("doCommit rolls back and wraps SQLException from commit() into AWException")
    void doCommitRollsBackOnSQLException() throws Exception {
      doThrow(new SQLException("commit failed")).when(connection).commit();

      assertThatThrownBy(() -> maintainService.doCommit(databaseConnection, "stmt", true))
        .isInstanceOf(AWException.class);

      verify(connection, times(1)).rollback();
    }

    @Test
    @DisplayName("doCommit does nothing when manageConnection=false")
    void doCommitDoesNothingWhenNotManaged() throws Exception {
      maintainService.doCommit(databaseConnection, "", false);

      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("doCommit does nothing when connection was never obtained (hasConnection=false)")
    void doCommitDoesNothingWhenConnectionNeverObtained() throws Exception {
      when(databaseConnection.hasConnection()).thenReturn(false);
      maintainService.doCommit(databaseConnection, "", true);

      verify(connection, never()).commit();
    }
  }

  @Nested
  @DisplayName("DatabaseConnection.hasConnection() — guards lazy acquisition")
  class HasConnectionTests {

    @Test
    @DisplayName("hasConnection() returns false when no connection is pre-set")
    void hasConnectionReturnsFalseWhenNotSet() {
      DatabaseConnection real = new DatabaseConnection("postgresql", dataSource, "default");
      assertThat(real.hasConnection()).isFalse();
    }

    @Test
    @DisplayName("hasConnection() returns true after setConnection() is called")
    void hasConnectionReturnsTrueAfterSet() {
      DatabaseConnection real = new DatabaseConnection("postgresql", dataSource, "default");
      real.setConnection(connection);
      assertThat(real.hasConnection()).isTrue();
    }
  }

  @Nested
  @DisplayName("prepareMaintain is called before getDatabaseConnection — no connection leak on invalid maintainId")
  class PrepareMaintainBeforeConnectionTests {

    @Test
    @DisplayName("launchMaintain(String, ObjectNode) does not open connection if maintainId does not exist")
    void noConnectionLeakOnInvalidMaintainId() throws Exception {
      // Configure elements to return null for unknown maintaining
      when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
      when(aweElements.getMaintain(anyString())).thenReturn(null);

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(
          () -> maintainService.launchMaintain("NonExistentMaintain", emptyParams()))
          .isInstanceOf(AWException.class);

        // No connection should have been obtained since prepareMaintain fails first
        dsu.verify(() -> DataSourceUtils.getConnection(any(DataSource.class)), never());
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }

    @Test
    @DisplayName("launchPrivateMaintain(String, ObjectNode) does not open connection if maintainId does not exist")
    void noConnectionLeakOnInvalidPrivateMaintainId() throws Exception {
      when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
      when(aweElements.getMaintain(anyString())).thenReturn(null);

      try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
        assertThatThrownBy(
          () -> maintainService.launchPrivateMaintain("NonExistentMaintain", emptyParams()))
          .isInstanceOf(AWException.class);

        dsu.verify(() -> DataSourceUtils.getConnection(any(DataSource.class)), never());
        dsu.verify(() -> DataSourceUtils.releaseConnection(any(), any(DataSource.class)), never());
      }
    }
  }
}
