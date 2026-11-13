package com.almis.awe.model.entities.screen.component.chart.echarts;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reports Highcharts options that the ECharts model cannot translate.
 * <p>
 * Every option is reported once. The number of remembered options is bounded, so a flood of distinct dynamic
 * parameter names cannot grow the memory of the server.
 * </p>
 */
@Slf4j
public class UnsupportedOptionReporter {

  /**
   * Maximum number of distinct options remembered
   */
  static final int MAX_REMEMBERED = 500;

  private static final UnsupportedOptionReporter SHARED = new UnsupportedOptionReporter(log::warn);

  private final Set<String> reported = new HashSet<>();
  private final Consumer<String> sink;

  /**
   * Create a reporter
   *
   * @param sink Receives each message the first time an option is reported
   */
  public UnsupportedOptionReporter(Consumer<String> sink) {
    this.sink = sink;
  }

  /**
   * Retrieve the reporter shared by the server, which writes to the log at warn level
   *
   * @return Shared reporter
   */
  public static UnsupportedOptionReporter shared() {
    return SHARED;
  }

  /**
   * Report a chart parameter without ECharts translation
   *
   * @param scope Scope where the parameter appears
   * @param path  Dotted path of the Highcharts option
   */
  void parameter(ParameterScope scope, String path) {
    report(scope + ":" + path, "Highcharts chart-parameter '" + path + "' has no ECharts translation yet (scope: "
      + scope.label() + "); it is ignored by the ECharts model");
  }

  /**
   * Report a value that is approximated or replaced in the ECharts model
   *
   * @param key     Identity of the report, used to report it once
   * @param message Message to log
   */
  void approximation(String key, String message) {
    report("approximation:" + key, message);
  }

  private synchronized void report(String key, String message) {
    if (reported.size() >= MAX_REMEMBERED) {
      return;
    }
    if (reported.add(key)) {
      sink.accept(message);
      if (reported.size() == MAX_REMEMBERED) {
        sink.accept("The ECharts model reached " + MAX_REMEMBERED + " distinct unsupported options; "
          + "further ones are not reported");
      }
    }
  }
}
