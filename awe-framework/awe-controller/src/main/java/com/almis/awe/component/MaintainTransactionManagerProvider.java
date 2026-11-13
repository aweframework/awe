package com.almis.awe.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Provides the transaction manager used to delimit the maintains of each DataSource.
 *
 * <p>Keeps one {@link DataSourceTransactionManager} per {@link DataSource} instance (compared by identity), so the
 * default DataSource, the routing DataSource and every multidatabase alias DataSource get their own manager.
 * The managers are not exposed as beans, so they do not replace the application transaction manager.
 */
@Slf4j
public class MaintainTransactionManagerProvider {

  // Transaction managers by DataSource instance
  private final Map<DataSource, DataSourceTransactionManager> transactionManagers =
    Collections.synchronizedMap(new IdentityHashMap<>());

  /**
   * Retrieve the transaction manager of a DataSource, creating it the first time
   *
   * @param dataSource DataSource
   * @return Transaction manager bound to the DataSource
   */
  public DataSourceTransactionManager getTransactionManager(DataSource dataSource) {
    return transactionManagers.computeIfAbsent(dataSource, this::createTransactionManager);
  }

  /**
   * Create the transaction manager of a DataSource
   *
   * @param dataSource DataSource
   * @return Transaction manager
   */
  private DataSourceTransactionManager createTransactionManager(DataSource dataSource) {
    log.debug("Creating maintain transaction manager for datasource {}", dataSource);
    return new DataSourceTransactionManager(dataSource);
  }
}
