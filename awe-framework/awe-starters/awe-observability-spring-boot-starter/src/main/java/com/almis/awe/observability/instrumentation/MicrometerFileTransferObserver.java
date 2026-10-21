package com.almis.awe.observability.instrumentation;

import com.almis.awe.service.observation.FileTransferObserver;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Records every file uploaded or downloaded on the {@value #METER} distribution summary: its count is the number of
 * files and its total the bytes transferred, tagged by {@code direction} ({@code upload} or {@code download}). File
 * names and users are never tags.
 */
public class MicrometerFileTransferObserver implements FileTransferObserver {

  /** Distribution summary name */
  public static final String METER = "awe.files";

  private final Supplier<MeterRegistry> registry;

  /**
   * Constructor
   *
   * @param registry Meter registry supplier (may supply null)
   */
  public MicrometerFileTransferObserver(Supplier<MeterRegistry> registry) {
    this.registry = registry;
  }

  @Override
  public void transferred(Direction direction, long bytes) {
    MeterRegistry meterRegistry = registry.get();
    if (meterRegistry == null) {
      return;
    }
    DistributionSummary.builder(METER)
      .description("Files uploaded and downloaded")
      .baseUnit("bytes")
      .tag("direction", direction.name().toLowerCase(Locale.ROOT))
      .register(meterRegistry)
      .record(bytes);
  }
}
