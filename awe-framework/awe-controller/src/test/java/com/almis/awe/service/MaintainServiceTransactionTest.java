package com.almis.awe.service;

import com.almis.awe.component.MaintainTransactionManagerProvider;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.config.MaintainConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Commit;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.RetrieveData;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the transaction boundaries of {@link MaintainService#launchMaintain}
 * ({@code awe.maintain.transaction.enabled}).
 *
 * <p>Uses a real {@link MaintainTransactionManagerProvider} (and so a real
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}) over a mocked JDBC
 * {@link DataSource}, so commits, rollbacks and physical connections are observed on the mocked connection.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintainServiceTransactionTest {

  @Mock private MaintainLauncher maintainLauncher;
  @Mock private QueryLauncher queryLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;

  @Mock private DataSource dataSource;
  @Mock private Connection connection;

  private MaintainConfigProperties maintainConfigProperties;
  private MaintainService maintainService;

  @BeforeEach
  void setUp() throws Exception {
    maintainConfigProperties = new MaintainConfigProperties();
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties);
    maintainService.setApplicationContext(applicationContext);
    maintainService.setMaintainConfigProperties(maintainConfigProperties);
    maintainService.setMaintainTransactionManagerProvider(new MaintainTransactionManagerProvider());

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBean(QueryLauncher.class)).thenReturn(queryLauncher);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");

    when(dataSource.getConnection()).thenReturn(connection);
  }

  @AfterEach
  void noTransactionLeftBound() {
    assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  private DatabaseConnection newDatabaseConnection() {
    return new DatabaseConnection("h2", dataSource, "default");
  }

  private Target target(String name, MaintainQuery... queries) {
    Target target = new Target();
    target.setName(name);
    target.setQueryList(new ArrayList<>(Arrays.asList(queries)));
    return target;
  }

  private Insert insert() {
    Insert insert = new Insert();
    insert.setVariableDefinitionList(new ArrayList<>());
    return insert;
  }

  private ObjectNode emptyParams() {
    return JsonNodeFactory.instance.objectNode();
  }

  @Nested
  @DisplayName("Flag on — one transaction per maintain")
  class FlagOn {

    @Test
    @DisplayName("Runs the steps inside a transaction on the bound connection, commits once and releases it")
    void commitsOnceAndReleases() throws Exception {
      DatabaseConnection databaseConnection = newDatabaseConnection();
      AtomicBoolean ranInTransaction = new AtomicBoolean();
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        DatabaseConnection stepConnection = invocation.getArgument(1);
        ranInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive()
          && stepConnection.getConnection() == DataSourceUtils.getConnection(dataSource));
        return new ServiceData();
      });

      ServiceData result = maintainService.launchMaintain(target("tx-ok", insert(), insert()), emptyParams(), databaseConnection, false);

      assertThat(result.getType()).isEqualTo(AnswerType.OK);
      assertThat(ranInTransaction).isTrue();
      verify(dataSource, times(1)).getConnection();
      verify(connection, times(1)).commit();
      verify(connection, never()).rollback();
      verify(connection, times(1)).close();
      // The released connection is not kept: a later use acquires a new one
      assertThat(databaseConnection.hasConnection()).isFalse();
    }

    @Test
    @DisplayName("Rolls back and rethrows the same AWException instance")
    void rollsBackOnAWException() throws Exception {
      AWException expected = new AWException("maintain error title", "maintain error message");
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenReturn(new ServiceData())
        .thenThrow(expected);

      AWException thrown = catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-aw", insert(), insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      assertThat(thrown).isSameAs(expected);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
      verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("Rolls back and wraps unexpected exceptions as before")
    void rollsBackOnRuntimeException() throws Exception {
      RuntimeException failure = new IllegalStateException("unexpected DB failure");
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenThrow(failure);

      AWException thrown = catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-rt", insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      assertThat(thrown.getCause()).isSameAs(failure);
      assertThat(thrown.getTitle()).isEqualTo("msg");
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
      verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("Rolls back when a step returns an ERROR result, and returns the response unchanged")
    void rollsBackOnErrorResult() throws Exception {
      ServiceData error = new ServiceData().setType(AnswerType.ERROR).setTitle("Error title").setMessage("Error message");
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenReturn(error);

      ServiceData result = maintainService.launchMaintain(target("tx-error", insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(result).isSameAs(error);
      assertThat(result.getType()).isEqualTo(AnswerType.ERROR);
      assertThat(result.getMessage()).isEqualTo("Error message");
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
      verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("Rolls back when any step returns ERROR, even if a later step succeeds")
    void rollsBackOnEarlierErrorResult() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenReturn(new ServiceData().setType(AnswerType.ERROR))
        .thenReturn(new ServiceData());

      ServiceData result = maintainService.launchMaintain(target("tx-error-first", insert(), insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(result.getType()).isEqualTo(AnswerType.OK);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("<commit/> commits the bound connection in the middle of the maintain")
    void commitStepCommitsMidMaintain() throws Exception {
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenReturn(new ServiceData())
        .thenThrow(new AWException("title", "after commit"));

      catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-commit", insert(), new Commit(), insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      InOrder order = inOrder(connection);
      order.verify(connection).commit();
      order.verify(connection).rollback();
      verify(connection, times(1)).commit();
    }

    @Test
    @DisplayName("<retrieve-data> joins the maintain transaction on the same DataSource")
    void retrieveDataJoinsTransaction() throws Exception {
      AtomicBoolean joined = new AtomicBoolean();
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenReturn(new ServiceData());
      when(queryLauncher.launchQuery(any(), any())).thenAnswer(invocation -> {
        // Same lookup as SQLQueryConnector
        Connection queryConnection = DataSourceUtils.getConnection(dataSource);
        joined.set(queryConnection == connection && TransactionSynchronizationManager.isActualTransactionActive());
        DataSourceUtils.releaseConnection(queryConnection, dataSource);
        return new ServiceData();
      });

      maintainService.launchMaintain(target("tx-retrieve", insert(), new RetrieveData()), emptyParams(), newDatabaseConnection(), false);

      assertThat(joined).isTrue();
      verify(dataSource, times(1)).getConnection();
      verify(connection, times(1)).commit();
      verify(connection, times(1)).close();
    }
  }

  @Nested
  @DisplayName("Flag on — nested maintains join the outer transaction")
  class NestedMaintains {

    @Test
    @DisplayName("A nested maintain on the same DataSource reuses the connection and does not commit on its own")
    void nestedMaintainJoins() throws Exception {
      List<Connection> innerConnections = new ArrayList<>();
      Target inner = target("tx-inner", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        DatabaseConnection stepConnection = invocation.getArgument(1);
        if (inner.getQueryList().contains(query)) {
          innerConnections.add(stepConnection.getConnection());
          return new ServiceData();
        }
        // Outer step: a Java service launching another maintain
        return maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
      });

      maintainService.launchMaintain(target("tx-outer", insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(innerConnections).containsExactly(connection);
      verify(dataSource, times(1)).getConnection();
      verify(connection, times(1)).commit();
      verify(connection, never()).rollback();
      verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("A nested maintain failure rolls back the outer maintain")
    void nestedFailureRollsBackOuter() throws Exception {
      Target inner = target("tx-inner-fail", insert());
      AWException expected = new AWException("title", "inner failure");
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          throw expected;
        }
        return maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
      });

      AWException thrown = catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-outer-fail", insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      assertThat(thrown).isSameAs(expected);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("A nested failure swallowed by a Java service still rolls back the outer maintain")
    void swallowedNestedFailureRollsBackOuter() throws Exception {
      Target inner = target("tx-inner-swallowed", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          throw new AWException("title", "inner failure");
        }
        try {
          maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
        } catch (AWException exc) {
          // The service ignores the failure and reports success
        }
        return new ServiceData();
      });

      AWException thrown = catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-outer-swallowed", insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      // The participating failure marked the transaction rollback-only (globalRollbackOnParticipationFailure)
      assertThat(thrown).hasCauseInstanceOf(UnexpectedRollbackException.class);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("A nested ERROR result ignored by a Java service still rolls back the outer maintain")
    void ignoredNestedErrorRollsBackOuter() throws Exception {
      Target inner = target("tx-inner-error", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          return new ServiceData().setType(AnswerType.ERROR);
        }
        maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
        return new ServiceData();
      });

      AWException thrown = catchThrowableOfType(
        () -> maintainService.launchMaintain(target("tx-outer-error", insert()), emptyParams(), newDatabaseConnection(), false),
        AWException.class);

      assertThat(thrown).hasCauseInstanceOf(UnexpectedRollbackException.class);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("A nested ERROR result returned by the Java service rolls back and returns the ERROR response")
    void propagatedNestedErrorRollsBackOuter() throws Exception {
      Target inner = target("tx-inner-error-returned", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          return new ServiceData().setType(AnswerType.ERROR);
        }
        return maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
      });

      ServiceData result = maintainService.launchMaintain(target("tx-outer-error-returned", insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(result.getType()).isEqualTo(AnswerType.ERROR);
      verify(connection, times(1)).rollback();
      verify(connection, never()).commit();
    }

    @Test
    @DisplayName("A nested maintain on another DataSource runs in its own transaction")
    void nestedMaintainOnOtherDataSourceIsIndependent() throws Exception {
      DataSource otherDataSource = mock(DataSource.class);
      Connection otherConnection = mock(Connection.class);
      when(otherDataSource.getConnection()).thenReturn(otherConnection);
      Target inner = target("tx-inner-other", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          return new ServiceData();
        }
        return maintainService.launchMaintain(inner, emptyParams(), new DatabaseConnection("h2", otherDataSource, "other"), false);
      });

      maintainService.launchMaintain(target("tx-outer-other", insert()), emptyParams(), newDatabaseConnection(), false);

      verify(connection, times(1)).commit();
      verify(otherConnection, times(1)).commit();
      verify(otherConnection, times(1)).close();
      assertThat(TransactionSynchronizationManager.hasResource(otherDataSource)).isFalse();
    }
  }

  @Nested
  @DisplayName("Previous behavior kept")
  class PreviousBehavior {

    @Test
    @DisplayName("Flag off: no transaction, manual autocommit/commit on the maintain connection")
    void flagOffKeepsManualCommit() throws Exception {
      maintainConfigProperties.getTransaction().setEnabled(false);
      AtomicBoolean ranInTransaction = new AtomicBoolean(true);
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        ranInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        return new ServiceData();
      });

      maintainService.launchMaintain(target("no-tx", insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(ranInTransaction).isFalse();
      verify(connection, times(1)).setAutoCommit(false);
      verify(connection, times(1)).commit();
      verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("Flag off: a nested maintain uses its own connection and commits independently")
    void flagOffNestedMaintainIsIndependent() throws Exception {
      maintainConfigProperties.getTransaction().setEnabled(false);
      Target inner = target("no-tx-inner", insert());
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        MaintainQuery query = invocation.getArgument(0);
        if (inner.getQueryList().contains(query)) {
          return new ServiceData();
        }
        return maintainService.launchMaintain(inner, emptyParams(), newDatabaseConnection(), false);
      });

      maintainService.launchMaintain(target("no-tx-outer", insert()), emptyParams(), newDatabaseConnection(), false);

      verify(dataSource, times(2)).getConnection();
      verify(connection, times(2)).commit();
    }

    @Test
    @DisplayName("Flag off: an ERROR result is committed as before")
    void flagOffCommitsErrorResult() throws Exception {
      maintainConfigProperties.getTransaction().setEnabled(false);
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any()))
        .thenReturn(new ServiceData().setType(AnswerType.ERROR));

      maintainService.launchMaintain(target("no-tx-error", insert()), emptyParams(), newDatabaseConnection(), false);

      verify(connection, times(1)).commit();
      verify(connection, never()).rollback();
    }

    @Test
    @DisplayName("keepAliveConnection=true: the caller owns the connection, no transaction is opened")
    void keepAliveConnectionIsUnchanged() throws Exception {
      DatabaseConnection databaseConnection = newDatabaseConnection();
      AtomicBoolean ranInTransaction = new AtomicBoolean(true);
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        ranInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        return new ServiceData();
      });

      maintainService.launchMaintain(target("keep-alive", insert()), emptyParams(), databaseConnection, true);

      assertThat(ranInTransaction).isFalse();
      verify(connection, never()).commit();
      verify(connection, never()).close();
      assertThat(databaseConnection.getConnection()).isSameAs(connection);
    }

    @Test
    @DisplayName("Caller-supplied connection: managed manually as before, no transaction is opened")
    void callerSuppliedConnectionIsUnchanged() throws Exception {
      DatabaseConnection databaseConnection = newDatabaseConnection().setConnection(connection);
      AtomicBoolean ranInTransaction = new AtomicBoolean(true);
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        ranInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        return new ServiceData();
      });

      maintainService.launchMaintain(target("supplied", insert()), emptyParams(), databaseConnection, false);

      assertThat(ranInTransaction).isFalse();
      verify(connection, times(1)).setAutoCommit(false);
      verify(connection, times(1)).commit();
    }

    @Test
    @DisplayName("Without a transaction manager provider (no database configuration) the previous path is used")
    void withoutProviderKeepsManualCommit() throws Exception {
      maintainService.setMaintainTransactionManagerProvider(null);
      AtomicBoolean ranInTransaction = new AtomicBoolean(true);
      when(maintainLauncher.launchMaintain(any(MaintainQuery.class), any(), any())).thenAnswer(invocation -> {
        ranInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        return new ServiceData();
      });

      maintainService.launchMaintain(target("no-provider", insert()), emptyParams(), newDatabaseConnection(), false);

      assertThat(ranInTransaction).isFalse();
      verify(connection, times(1)).commit();
    }
  }
}
