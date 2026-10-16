package com.almis.awe.test.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.data.builder.DataListBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test service of the deferred loading of dialogs and tabs (#824). The queries of the components of the test screen
 * go through it, so the screen can show how many times each one of them has run.
 */
@Service
public class DeferredLoad extends ServiceConfig {

  private static final String STRING = "STRING";
  private static final String INTEGER = "INTEGER";

  // Components of the test screen, in the order they are listed
  private static final List<String> COMPONENTS = List.of("DefSelect", "DefRows", "DefText", "EagSelect", "TabFirstSelect",
    "TabSecondSelect", "TabSecondRows");

  // Rows of the heavy queries
  private static final int HEAVY_ROWS = 3000;

  // Calls by session and component, so sessions running at the same time do not see each other's calls
  private static final Map<String, Map<String, AtomicInteger>> CALLS = new ConcurrentHashMap<>();

  private Map<String, AtomicInteger> sessionCalls() {
    String sessionId = String.valueOf(getSession().getSessionId());
    return CALLS.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>());
  }

  private void register(String component) {
    sessionCalls().computeIfAbsent(component, key -> new AtomicInteger()).incrementAndGet();
  }

  /**
   * Retrieve a list of options for a component, counting the call
   *
   * @param component Component which asks for the data
   * @return Options
   * @throws AWException Error building the data
   */
  public ServiceData loadList(String component) throws AWException {
    register(component);
    DataList dataList = new DataListBuilder()
      .addColumn("id", List.of("1", "2", "3"), STRING)
      .addColumn("value", List.of("1", "2", "3"), STRING)
      .addColumn("label", List.of("One", "Two", "Three"), STRING)
      .addColumn("eag", List.of("E1", "E2", "E3"), STRING)
      .build();
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the initial value of a component, counting the call
   *
   * @param component Component which asks for the data
   * @return Initial value
   * @throws AWException Error building the data
   */
  public ServiceData loadValue(String component) throws AWException {
    register(component);
    DataList dataList = new DataListBuilder()
      .addColumn("value", List.of("Loaded"), STRING)
      .addColumn("label", List.of("Loaded"), STRING)
      .build();
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve a long list of rows, counting the call. It stands for the heavy queries of a big dialog or tab
   *
   * @param component Component which asks for the data
   * @return Rows
   * @throws AWException Error building the data
   */
  public ServiceData loadHeavy(String component) throws AWException {
    register(component);
    List<String> ids = new ArrayList<>();
    List<String> values = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    for (int row = 0; row < HEAVY_ROWS; row++) {
      ids.add(String.valueOf(row));
      values.add("value-" + row);
      labels.add("Label of the row " + row + " of " + component);
    }
    DataList dataList = new DataListBuilder()
      .addColumn("id", ids, STRING)
      .addColumn("value", values, STRING)
      .addColumn("label", labels, STRING)
      .addColumn("other", labels, STRING)
      .addColumn("first", values, STRING)
      .addColumn("second", labels, STRING)
      .addColumn("third", values, STRING)
      .addColumn("fourth", labels, STRING)
      .build();
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the number of times each component of the test screen has asked for its data
   *
   * @return Calls by component
   * @throws AWException Error building the data
   */
  public ServiceData getCalls() throws AWException {
    List<Integer> calls = new ArrayList<>();
    COMPONENTS.forEach(component -> calls.add(sessionCalls().getOrDefault(component, new AtomicInteger()).get()));
    DataList dataList = new DataListBuilder()
      .addColumn("id", COMPONENTS, STRING)
      .addColumn("calls", calls, INTEGER)
      .build();
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Forget the calls of every component
   *
   * @return Service data
   */
  public ServiceData reset() {
    sessionCalls().clear();
    return new ServiceData();
  }
}
