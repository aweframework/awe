package com.almis.awe.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.DefaultExemplarSampler;
import io.prometheus.client.exemplars.tracer.otel_agent.OpenTelemetryAgentSpanContextSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricConfig {
  @Bean
  public OpenTelemetryAgentSpanContextSupplier openTelemetryAgentSpanContextSupplier() {
    // For supporting prometheus examplers
    return new OpenTelemetryAgentSpanContextSupplier();
  }

  @Bean
  @ConditionalOnProperty(name = "management.metrics.export.prometheus.enabled", havingValue = "true")
  public PrometheusMeterRegistry prometheusMeterRegistryWithExemplar
    (PrometheusConfig prometheusConfig, CollectorRegistry collectorRegistry,
     Clock clock) {
    return new PrometheusMeterRegistry(prometheusConfig, collectorRegistry,
      clock, new DefaultExemplarSampler(new OpenTelemetryAgentSpanContextSupplier() {

      @Override
      public String getTraceId() {
        if (!Span.current().getSpanContext().isSampled()) {
          return null;
        }
        return super.getTraceId();
      }
    })
    );
  }
}
