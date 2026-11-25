package com.almis.awe.scheduler.util;

import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.model.dto.ServiceData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Switch of the views of the activity screen: the executions (grid GrdExeAct, toolbar filters CrtExe*) and the
 * executions grouped by flow run (tree grid GrdExeRun, toolbar filters CrtRun*). Each grid has its own toolbar, so the
 * view that is shown receives the search and the quick filters of the view that is left. The «Group by flow» switch of
 * the screen is the view itself (it shows one grid and hides the other), so it is not set here
 */
public final class ActivityViewFilters {

  /**
   * View of the executions
   */
  public static final String EXECUTIONS = "exe";
  /**
   * View of the executions grouped by flow run
   */
  public static final String RUNS = "run";

  private ActivityViewFilters() {
  }

  /**
   * Give the filters of the toolbar that is left to the toolbar of the view that is shown. The custom range of the date
   * filter is not given (only its preset): the range is kept by the quick filter itself
   *
   * @param view   View that is shown: exe or run
   * @param search Search by task name
   * @param state  State quick filter
   * @param origin Origin quick filter
   * @param window Preset of the start quick filter (last hours)
   * @return Select actions of the toolbar filters of the view
   */
  public static ServiceData copy(String view, String search, String state, String origin, String window) {
    String prefix;
    if (RUNS.equals(view)) {
      prefix = "CrtRun";
    } else if (EXECUTIONS.equals(view)) {
      prefix = "CrtExe";
    } else {
      throw new IllegalArgumentException("Unknown view of the activity screen: " + view);
    }
    Map<String, String> values = new LinkedHashMap<>();
    values.put(prefix + "Tsk", search);
    values.put(prefix + "Qck", state);
    values.put(prefix + "Ori", origin);
    values.put(prefix + "Win", window);
    ServiceData serviceData = new ServiceData();
    values.forEach((target, value) -> serviceData.addClientAction(new SelectActionBuilder(target, value == null ? "" : value)
      .setAsync(true).setSilent(true).build()));
    return serviceData;
  }
}
