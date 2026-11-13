package com.almis.awe.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Maintain transaction manager provider tests
 */
class MaintainTransactionManagerProviderTest {

  private final MaintainTransactionManagerProvider provider = new MaintainTransactionManagerProvider();

  @Test
  @DisplayName("Returns a transaction manager bound to the requested DataSource")
  void returnsManagerForDataSource() {
    DataSource dataSource = mock(DataSource.class);

    DataSourceTransactionManager manager = provider.getTransactionManager(dataSource);

    assertThat(manager).isNotNull();
    assertThat(manager.getDataSource()).isSameAs(dataSource);
  }

  @Test
  @DisplayName("Returns the same transaction manager for the same DataSource instance")
  void cachesManagerPerDataSource() {
    DataSource dataSource = mock(DataSource.class);

    assertThat(provider.getTransactionManager(dataSource)).isSameAs(provider.getTransactionManager(dataSource));
  }

  @Test
  @DisplayName("Returns a different transaction manager for a different DataSource instance")
  void differentManagerForDifferentDataSource() {
    DataSource first = mock(DataSource.class);
    DataSource second = mock(DataSource.class);

    assertThat(provider.getTransactionManager(first)).isNotSameAs(provider.getTransactionManager(second));
    assertThat(provider.getTransactionManager(second).getDataSource()).isSameAs(second);
  }

  @Test
  @DisplayName("Caches by instance identity, not by equals()")
  void cachesByIdentity() {
    DataSource first = new EqualDataSource();
    DataSource second = new EqualDataSource();

    assertThat(first).isEqualTo(second);
    assertThat(provider.getTransactionManager(first)).isNotSameAs(provider.getTransactionManager(second));
  }

  @Test
  @DisplayName("Concurrent requests for the same DataSource share a single transaction manager")
  void concurrentRequestsShareManager() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    int threads = 8;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<DataSourceTransactionManager>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          return provider.getTransactionManager(dataSource);
        }));
      }
      start.countDown();

      Set<DataSourceTransactionManager> managers = ConcurrentHashMap.newKeySet();
      for (Future<DataSourceTransactionManager> future : futures) {
        managers.add(future.get());
      }
      assertThat(managers).hasSize(1);
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * DataSource whose instances are all equal to each other
   */
  private static class EqualDataSource extends AbstractDataSource {
    @Override
    public Connection getConnection() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection(String username, String password) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof EqualDataSource;
    }

    @Override
    public int hashCode() {
      return 1;
    }
  }
}
