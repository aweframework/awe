package com.almis.awe.service;

import com.almis.awe.model.dto.ServiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Long-running demo service for the scheduler execution log: emits a trace every few seconds for
 * about five minutes, so the "Show execution log" viewer can be watched tailing a live execution.
 * Reachable through the {@code SlowTraceMaintain} maintain target.
 */
@Slf4j
@Service
public class SlowTraceService {

  private static final int STEPS = 100;
  private static final long STEP_MILLIS = 3000L;

  /**
   * Emit one trace per step until the run completes
   *
   * @return Service data
   * @throws InterruptedException Interrupted while waiting between steps
   */
  public ServiceData traceSlowly() throws InterruptedException {
    logger.info("[SlowTrace] starting: {} steps every {} ms", STEPS, STEP_MILLIS);
    for (int step = 1; step <= STEPS; step++) {
      logger.info("[SlowTrace] step {}/{} - still working", step, STEPS);
      Thread.sleep(STEP_MILLIS);
    }
    logger.info("[SlowTrace] finished successfully");
    return new ServiceData().setMessage("Slow trace finished");
  }
}
