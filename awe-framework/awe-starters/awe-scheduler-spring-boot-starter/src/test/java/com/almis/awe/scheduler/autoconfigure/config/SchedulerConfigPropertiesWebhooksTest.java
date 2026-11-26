package com.almis.awe.scheduler.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code awe.scheduler.webhooks} properties of {@link SchedulerConfigProperties}
 */
class SchedulerConfigPropertiesWebhooksTest {

  @Test
  void theDefaultsMatchTheDocumentedOnes() {
    SchedulerConfigProperties.Webhooks webhooks = new SchedulerConfigProperties().getWebhooks();

    assertThat(webhooks.getMaxBodySize()).isEqualTo(DataSize.ofKilobytes(256));
    assertThat(webhooks.getTolerance()).isEqualTo(Duration.ofMinutes(5));
    assertThat(webhooks.getInboxRetention()).isEqualTo(Duration.ofDays(30));
    assertThat(webhooks.getProcessInterval()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void theExternalizedValuesAreBound() {
    Map<String, String> source = Map.of(
      "awe.scheduler.webhooks.max-body-size", "64KB",
      "awe.scheduler.webhooks.tolerance", "1m",
      "awe.scheduler.webhooks.inbox-retention", "7d",
      "awe.scheduler.webhooks.process-interval", "2s");

    SchedulerConfigProperties properties = new Binder(new MapConfigurationPropertySource(source))
      .bind("awe.scheduler", SchedulerConfigProperties.class).get();

    assertThat(properties.getWebhooks().getMaxBodySize()).isEqualTo(DataSize.ofKilobytes(64));
    assertThat(properties.getWebhooks().getTolerance()).isEqualTo(Duration.ofMinutes(1));
    assertThat(properties.getWebhooks().getInboxRetention()).isEqualTo(Duration.ofDays(7));
    assertThat(properties.getWebhooks().getProcessInterval()).isEqualTo(Duration.ofSeconds(2));
  }
}
