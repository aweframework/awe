package com.almis.awe.scheduler.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code awe.scheduler.deferred} properties of {@link SchedulerConfigProperties}
 */
class SchedulerConfigPropertiesDeferredTest {

  @Test
  void theDefaultsMatchTheDocumentedOnes() {
    SchedulerConfigProperties.Deferred deferred = new SchedulerConfigProperties().getDeferred();

    assertThat(deferred.getDefaultTimeout()).isEqualTo(Duration.ofHours(24));
    assertThat(deferred.getTimeoutCheckInterval()).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void theExternalizedValuesAreBound() {
    Map<String, String> source = Map.of(
      "awe.scheduler.deferred.default-timeout", "2h",
      "awe.scheduler.deferred.timeout-check-interval", "15s");

    SchedulerConfigProperties properties = new Binder(new MapConfigurationPropertySource(source))
      .bind("awe.scheduler", SchedulerConfigProperties.class).get();

    assertThat(properties.getDeferred().getDefaultTimeout()).isEqualTo(Duration.ofHours(2));
    assertThat(properties.getDeferred().getTimeoutCheckInterval()).isEqualTo(Duration.ofSeconds(15));
  }
}
