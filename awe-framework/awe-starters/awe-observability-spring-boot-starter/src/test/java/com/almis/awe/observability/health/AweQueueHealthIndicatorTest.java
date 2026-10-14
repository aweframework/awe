package com.almis.awe.observability.health;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AweQueueHealthIndicatorTest {

  @Test
  @DisplayName("It is UP and reports the JMS provider when a connection can be opened")
  void up() throws JMSException {
    ConnectionFactory factory = mock(ConnectionFactory.class);
    Connection connection = mock(Connection.class);
    ConnectionMetaData metaData = mock(ConnectionMetaData.class);
    when(factory.createConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getJMSProviderName()).thenReturn("ActiveMQ");

    Health health = new AweQueueHealthIndicator(factory, Duration.ofSeconds(2)).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("provider", "ActiveMQ");
    verify(connection).close();
  }

  @Test
  @DisplayName("It is DOWN when the broker refuses the connection")
  void down() throws JMSException {
    ConnectionFactory factory = mock(ConnectionFactory.class);
    when(factory.createConnection()).thenThrow(new JMSException("Broker not available"));

    Health health = new AweQueueHealthIndicator(factory, Duration.ofSeconds(2)).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("error").toString()).contains("Broker not available");
  }

  @Test
  @DisplayName("A broker that hangs is DOWN after the timeout and does not block the check")
  void hangingBroker() throws JMSException {
    ConnectionFactory factory = mock(ConnectionFactory.class);
    when(factory.createConnection()).thenAnswer(invocation -> {
      Thread.sleep(10_000);
      return null;
    });

    long start = System.nanoTime();
    Health health = new AweQueueHealthIndicator(factory, Duration.ofMillis(200)).health();
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMillis).isLessThan(3_000);
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails().get("error").toString()).contains("timed out");
  }

  @Test
  @DisplayName("Repeated checks of a broker that keeps hanging do not pile up: only one call reaches it")
  void hangingBrokerDoesNotPileUpThreads() throws Exception {
    java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
    ConnectionFactory factory = mock(ConnectionFactory.class);
    when(factory.createConnection()).thenAnswer(invocation -> {
      calls.incrementAndGet();
      while (release.getCount() > 0) {
        try {
          release.await();
        } catch (InterruptedException exc) {
          // keep hanging
        }
      }
      return null;
    });
    AweQueueHealthIndicator indicator = new AweQueueHealthIndicator(factory, Duration.ofMillis(100));

    try {
      Health first = indicator.health();
      Health last = first;
      for (int i = 0; i < 20; i++) {
        last = indicator.health();
      }

      assertThat(first.getStatus()).isEqualTo(Status.DOWN);
      assertThat(first.getDetails().get("error").toString()).contains("timed out");
      assertThat(last.getStatus()).isEqualTo(Status.DOWN);
      assertThat(last.getDetails().get("error").toString()).contains("still running");
      // Only the first check reached the hung broker
      assertThat(calls.get()).isEqualTo(1);
    } finally {
      release.countDown();
    }
  }
}
