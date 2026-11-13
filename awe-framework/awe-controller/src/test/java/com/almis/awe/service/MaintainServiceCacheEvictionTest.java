package com.almis.awe.service;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Commit;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.RetrieveData;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.node.JsonNodeFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The {@code queryData} cache must be evicted again once a maintain has committed its data
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintainServiceCacheEvictionTest {

  @Mock private MaintainLauncher maintainLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseConnection databaseConnection;
  @Mock private CacheManager cacheManager;
  @Mock private Cache queryDataCache;

  private MaintainService maintainService;

  @BeforeEach
  void setUp() {
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties, (targetId, kind) -> true);
    maintainService.setApplicationContext(applicationContext);
    maintainService.setCacheManager(cacheManager);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(cacheManager.getCache("queryData")).thenReturn(queryDataCache);
    when(databaseConnection.getDataSource()).thenReturn(dataSource);
    when(databaseConnection.getConnection()).thenReturn(connection);
    when(databaseConnection.hasConnection()).thenReturn(true);
    when(databaseConnection.getDatabaseAlias()).thenReturn("default");
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private Target targetWithInsert() {
    Target target = new Target();
    target.setName("t1");
    Insert insert = new Insert();
    insert.setVariableDefinitionList(List.of());
    target.setQueryList(List.of(insert));
    return target;
  }

  private Target targetWith(MaintainQuery... queries) {
    Target target = new Target();
    target.setName("t1");
    target.setQueryList(List.of(queries));
    return target;
  }

  private Insert insert() {
    Insert insert = new Insert();
    insert.setVariableDefinitionList(List.of());
    return insert;
  }

  private void launch(boolean keepAlive) throws Exception {
    launch(targetWithInsert(), keepAlive);
  }

  private void launch(Target target, boolean keepAlive) throws Exception {
    try (MockedStatic<DataSourceUtils> ignored = mockStatic(DataSourceUtils.class)) {
      maintainService.launchMaintain(target, JsonNodeFactory.instance.objectNode(), databaseConnection, keepAlive);
    }
  }

  @Test
  void evictsAfterCompletionWhenATransactionSynchronizationIsActive() throws Exception {
    TransactionSynchronizationManager.initSynchronization();

    launch(true);

    // Caller keeps the connection: only the synchronization evicts, on completion
    verify(queryDataCache, never()).clear();
    List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
    synchronizations.forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    verify(queryDataCache).clear();
  }

  @Test
  void evictsAfterTheManualCommitWithoutTransaction() throws Exception {
    launch(false);

    var order = inOrder(connection, queryDataCache);
    order.verify(connection).commit();
    order.verify(queryDataCache).clear();
  }

  @Test
  void doesNotEvictWhenTheCallerKeepsTheConnection() throws Exception {
    launch(true);

    verify(queryDataCache, never()).clear();
  }

  @Test
  void worksWithoutCacheManager() throws Exception {
    maintainService.setCacheManager(null);

    launch(false);

    // Without a cache manager the maintain still commits and does not fail
    verify(connection).commit();
  }

  @Test
  void evictsAfterACommitStepInTheMiddleOfTheList() throws Exception {
    launch(targetWith(insert(), new Commit(), insert()), false);

    // The commit step evicts before the next step runs, and the final commit evicts again
    var order = inOrder(maintainLauncher, connection, queryDataCache);
    order.verify(maintainLauncher).launchMaintain(any(), any(), any());
    order.verify(connection).commit();
    order.verify(queryDataCache).clear();
    order.verify(maintainLauncher).launchMaintain(any(), any(), any());
    order.verify(connection).commit();
    order.verify(queryDataCache).clear();
  }

  @Test
  void neverEvictsWhenNoMaintainStepRuns() throws Exception {
    QueryLauncher queryLauncher = mock(QueryLauncher.class);
    when(applicationContext.getBean(QueryLauncher.class)).thenReturn(queryLauncher);
    when(queryLauncher.launchQuery(any(), any())).thenReturn(new ServiceData());

    launch(targetWith(new RetrieveData(), new Commit()), false);

    verify(connection, atLeastOnce()).commit();
    verify(queryDataCache, never()).clear();
  }

  @Test
  void registersASingleSynchronizationForSeveralSteps() throws Exception {
    TransactionSynchronizationManager.initSynchronization();

    launch(targetWith(insert(), insert(), insert()), true);

    assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
  }

  @Test
  void aFailingCacheDoesNotFailTheCommittedMaintain() throws Exception {
    doThrow(new IllegalStateException("redis down")).when(queryDataCache).clear();

    launch(false);

    verify(connection).commit();
    verify(queryDataCache).clear();
  }

  @Test
  void evictsAfterTheManualCommitEvenWithAnUnrelatedSynchronization() throws Exception {
    TransactionSynchronizationManager.initSynchronization();

    launch(false);

    var order = inOrder(connection, queryDataCache);
    order.verify(connection).commit();
    order.verify(queryDataCache).clear();
  }

  @Test
  void aFailedMaintainIsNotCommittedAndEvictsOnlyOnRollbackCompletion() throws Exception {
    TransactionSynchronizationManager.initSynchronization();
    when(maintainLauncher.launchMaintain(any(), any(), any())).thenThrow(new IllegalStateException("insert failed"));

    assertThatThrownBy(() -> launch(false)).hasRootCauseMessage("insert failed");

    // Nothing was committed, so nothing is evicted until the transaction completes
    verify(connection, never()).commit();
    verify(queryDataCache, never()).clear();
    TransactionSynchronizationManager.getSynchronizations()
      .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verify(queryDataCache).clear();
  }
}
