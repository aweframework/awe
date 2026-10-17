package com.almis.awe.observability.instrumentation;

import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.security.audit.SecurityAuditSink;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.Supplier;

/**
 * Counts the security audit events on the {@value #METER} counter, tagged by type, outcome, channel and cause. The
 * user, address and other free values are never tags, so the number of series stays bounded.
 */
public class MicrometerSecurityAuditSink implements SecurityAuditSink {

  /** Counter name */
  public static final String METER = "awe.security.audit.events";
  private static final String NONE = "none";

  private final Supplier<MeterRegistry> registry;

  /**
   * Constructor
   *
   * @param registry Meter registry supplier (may supply null)
   */
  public MicrometerSecurityAuditSink(Supplier<MeterRegistry> registry) {
    this.registry = registry;
  }

  @Override
  public void record(SecurityAuditEvent event) {
    MeterRegistry meterRegistry = registry.get();
    if (meterRegistry == null) {
      return;
    }
    Counter.builder(METER)
      .description("Security audit events")
      .tag("type", name(event.getType()))
      .tag("outcome", name(event.getOutcome()))
      .tag("channel", name(event.getChannel()))
      .tag("cause", name(event.getCause()))
      .register(meterRegistry)
      .increment();
  }

  private static String name(Enum<?> value) {
    return value == null ? NONE : value.name();
  }
}
