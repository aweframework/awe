package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.observation.ExecutionObserver;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AweTagValueLimitFilterTest {

  private static final int CAP = 10;

  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    meterRegistry.config().meterFilter(new AweTagValueLimitFilter(CAP));
  }

  @Test
  @DisplayName("Opening screens with 1000 random options never creates more series than the cap plus other")
  void capsRandomScreenOptions() throws AWException {
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
    ExecutionObserver observer = new MicrometerExecutionObserver(() -> registry);

    observer.observe(ExecutionType.SCREEN, "home", Map.of("screen", "home"), () -> "built");
    for (int i = 0; i < 1000; i++) {
      String random = UUID.randomUUID().toString();
      observer.observe(ExecutionType.SCREEN, random, Map.of("screen", random), () -> "built");
    }
    observer.observe(ExecutionType.SCREEN, "home", Map.of("screen", "home"), () -> "built");

    Set<String> ids = tagValues("awe.screen", "id");
    assertThat(ids).hasSizeLessThanOrEqualTo(CAP + 1).contains("home", AweTagValueLimitFilter.OTHER);
    assertThat(tagValues("awe.screen", "screen")).hasSizeLessThanOrEqualTo(CAP + 1).contains(AweTagValueLimitFilter.OTHER);
    assertThat(meterRegistry.find("awe.screen").meters()).hasSizeLessThanOrEqualTo(CAP + 1);
    assertThat(meterRegistry.get("awe.screen").tags("id", "home", "screen", "home").timer().count())
      .as("A value admitted before the cap keeps its own series").isEqualTo(2);
    assertThat(meterRegistry.get("awe.screen").tags("id", AweTagValueLimitFilter.OTHER).timer().count())
      .isEqualTo(1000 - (CAP - 1));
  }

  @Test
  @DisplayName("The long task timer of an observation shares the values of its timer (the .active suffix is ignored)")
  void activeSharesTheTimerValues() {
    for (int i = 0; i < CAP; i++) {
      counter("awe.query", "id", "Q" + i);
    }
    counter("awe.query.active", "id", "Q0");
    counter("awe.query.active", "id", "new");

    assertThat(tagValues("awe.query.active", "id")).containsExactlyInAnyOrder("Q0", AweTagValueLimitFilter.OTHER);
  }

  @Test
  @DisplayName("Each meter and tag key has its own cap; other tags, other keys and non-AWE meters are untouched")
  void scope() {
    for (int i = 0; i < CAP + 5; i++) {
      counter("awe.query", "id", "Q" + i);
      counter("awe.query", "connector", "c" + i);
      counter("http.server.requests", "id", "H" + i);
    }
    counter("awe.maintain", "id", "M1");

    assertThat(tagValues("awe.query", "id")).hasSize(CAP + 1);
    assertThat(tagValues("awe.query", "connector")).hasSize(CAP + 5);
    assertThat(tagValues("http.server.requests", "id")).hasSize(CAP + 5);
    assertThat(tagValues("awe.maintain", "id")).containsExactly("M1");
  }

  @Test
  @DisplayName("Concurrent registrations never admit more values than the cap")
  void threadSafe() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    for (int i = 0; i < 2000; i++) {
      String value = "V" + i;
      executor.execute(() -> counter("awe.action", "id", value));
    }
    executor.shutdown();
    assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(tagValues("awe.action", "id")).hasSize(CAP + 1).contains(AweTagValueLimitFilter.OTHER);
  }

  private void counter(String name, String key, String value) {
    Counter.builder(name).tag(key, value).register(meterRegistry).increment();
  }

  private Set<String> tagValues(String name, String key) {
    return meterRegistry.find(name).meters().stream()
      .map(Meter::getId)
      .map(id -> id.getTag(key))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
