package com.almis.awe.observability.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AweDatabasesHealthIndicatorTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private DataSource validDataSource() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(anyInt())).thenReturn(true);
    return dataSource;
  }

  private AweDatabasesHealthIndicator indicator(Map<Object, Object> dataSources) {
    return new AweDatabasesHealthIndicator(() -> dataSources, TIMEOUT);
  }

  @Test
  @DisplayName("It is UP and reports every alias when all the databases answer")
  void allDatabasesUp() throws SQLException {
    Map<Object, Object> dataSources = new LinkedHashMap<>();
    dataSources.put("main", validDataSource());
    dataSources.put("reports", validDataSource());

    Health health = indicator(dataSources).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("main", "UP").containsEntry("reports", "UP");
  }

  @Test
  @DisplayName("The connection is validated with the configured timeout, in seconds")
  void usesTheTimeout() throws SQLException {
    DataSource dataSource = validDataSource();

    indicator(Map.of("main", dataSource)).health();

    verify(dataSource.getConnection()).isValid(3);
  }

  @Test
  @DisplayName("It is DOWN when one database does not give a connection, and says which one")
  void oneDatabaseDown() throws SQLException {
    DataSource broken = mock(DataSource.class);
    when(broken.getConnection()).thenThrow(new SQLException("Connection refused"));
    Map<Object, Object> dataSources = new LinkedHashMap<>();
    dataSources.put("main", validDataSource());
    dataSources.put("legacy", broken);

    Health health = indicator(dataSources).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("main", "UP");
    assertThat(health.getDetails().get("legacy").toString()).startsWith("DOWN").contains("Connection refused");
  }

  @Test
  @DisplayName("It is DOWN when a connection is not valid")
  void invalidConnection() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(anyInt())).thenReturn(false);

    Health health = indicator(Map.of("main", dataSource)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("main").toString()).startsWith("DOWN");
  }

  @Test
  @DisplayName("The connection is always closed")
  void closesTheConnection() throws SQLException {
    DataSource dataSource = validDataSource();

    indicator(Map.of("main", dataSource)).health();

    verify(dataSource.getConnection()).close();
  }

  @Test
  @DisplayName("It is UNKNOWN while no database has been loaded yet")
  void nothingLoaded() {
    Health health = indicator(Map.of()).health();

    assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
  }

  @Test
  @DisplayName("An entry that is not a datasource is reported as down instead of breaking the check")
  void notADatasource() {
    Health health = indicator(Map.of("odd", "not a datasource")).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("odd").toString()).startsWith("DOWN");
  }

  @Test
  @DisplayName("A database that hangs is DOWN after the timeout and does not block the check")
  void hangingDatabase() throws SQLException {
    DataSource hanging = mock(DataSource.class);
    when(hanging.getConnection()).thenAnswer(invocation -> {
      Thread.sleep(10_000);
      return null;
    });
    Map<Object, Object> dataSources = new LinkedHashMap<>();
    dataSources.put("hung", hanging);
    dataSources.put("main", validDataSource());

    long start = System.nanoTime();
    Health health = new AweDatabasesHealthIndicator(() -> dataSources, Duration.ofMillis(200)).health();
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMillis).isLessThan(3_000);
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("hung").toString()).startsWith("DOWN").contains("timed out");
    assertThat(health.getDetails()).containsEntry("main", "UP");
  }

  @Test
  @DisplayName("Repeated checks of a database that keeps hanging do not pile up: only one call reaches it")
  void hangingDatabaseDoesNotPileUpThreads() throws Exception {
    java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
    DataSource hanging = mock(DataSource.class);
    when(hanging.getConnection()).thenAnswer(invocation -> {
      calls.incrementAndGet();
      // Ignores interruption, like a driver blocked on a socket
      while (release.getCount() > 0) {
        try {
          release.await();
        } catch (InterruptedException exc) {
          // keep hanging
        }
      }
      return null;
    });
    AweDatabasesHealthIndicator indicator = new AweDatabasesHealthIndicator(() -> Map.of("hung", hanging), Duration.ofMillis(100));

    try {
      Health first = indicator.health();
      Health last = first;
      for (int i = 0; i < 20; i++) {
        last = indicator.health();
      }

      assertThat(first.getDetails().get("hung").toString()).contains("timed out");
      assertThat(last.getStatus()).isEqualTo(Status.DOWN);
      assertThat(last.getDetails().get("hung").toString()).contains("still running");
      // Only the first check reached the hung resource, so only one thread was ever blocked in it
      assertThat(calls.get()).isEqualTo(1);
    } finally {
      release.countDown();
    }
  }
}
