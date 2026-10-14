package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.service.observation.ExecutionObserver;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;

/**
 * QueryLauncher Class
 * Factory class of interface QueryConnector
 *
 */
@Slf4j
public class QueryLauncher extends ServiceConfig {

  // Observer of the executions (metrics, traces...). Optional: without one the execution just runs
  private ExecutionObserver executionObserver = ExecutionObserver.NONE;

  /**
   * Set the observer of the executions
   *
   * @param executionObserver Execution observer (optional, defined by the application when it needs one)
   */
  @Autowired(required = false)
  public void setExecutionObserver(ExecutionObserver executionObserver) {
    this.executionObserver = executionObserver == null ? ExecutionObserver.NONE : executionObserver;
  }

  /**
   * Launches a query
   *
   * @param query      Query
   * @param parameters Parameters
   * @return Query data
   * @throws AWException Query has failed
   */
  @Cacheable(value = "queryData", condition = "#p0.isCacheable()", key = "#p0.getQueryKeys(#p1)")
  public ServiceData launchQuery(@NonNull Query query, ObjectNode parameters) throws AWException {
    return executionObserver.observe(ExecutionObserver.ExecutionType.QUERY, query.getId(),
      Map.of("connector", getConnectorType(query)), () -> launchObservedQuery(query, parameters), data -> data == null ? null : data.getType());
  }

  /**
   * Launches a query once it is being observed
   *
   * @param query      Query
   * @param parameters Parameters
   * @return Query data
   * @throws AWException Query has failed
   */
  private ServiceData launchObservedQuery(Query query, ObjectNode parameters) throws AWException {
    QueryConnector queryLauncher;
    try {
      queryLauncher = getQueryConnector(query);
    } catch (AWException exc) {
      log.error(exc.getMessage(), exc);
      return new ServiceData().setDataList(new DataList());
    }
    return queryLauncher.launch(query, parameters);
  }

  /**
   * Subscribe to a query
   *
   * @param query      Query
   * @param address    Component address
   * @param parameters Parameters
   * @return Query data
   * @throws AWException Query has failed
   */
  public ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) throws AWException {
    QueryConnector queryLauncher;
    try {
      queryLauncher = getQueryConnector(query);
    } catch (AWException exc) {
      log.error("Query connector not found. Perhaps database is not activated?", exc);
      return new ServiceData().setDataList(new DataList());
    }
    return queryLauncher.subscribe(query, address, parameters);
  }

  /**
   * Get the kind of connector that runs a query, as a fixed label
   *
   * @param query Query
   * @return service, enum, queue or sql
   */
  private String getConnectorType(Query query) {
    if (query.getService() != null) {
      return "service";
    } else if (query.getEnumerated() != null) {
      return "enum";
    } else if (query.getQueue() != null) {
      return "queue";
    }
    return "sql";
  }

  /**
   * Get connector to query
   *
   * @param query Query
   * @return QueryConnector
   */
  private QueryConnector getQueryConnector(Query query) throws AWException {

    QueryConnector connector;

    if (query.getService() != null) {
      connector = getBean(ServiceQueryConnector.class);
    } else if (query.getEnumerated() != null) {
      connector = getBean(EnumQueryConnector.class);
    } else if (query.getQueue() != null) {
      try {
        connector = getBean(QueueQueryConnector.class);
      } catch (Exception exc) {
        throw new AWException("Queue query connector not found. Perhaps JMS is not activated?", exc);
      }
    } else {
      try {
        connector = getBean(SQLQueryConnector.class);
      } catch (Exception exc) {
        throw new AWException("SQL query connector not found. Perhaps SQL DATABASE is not activated?", exc);
      }
    }

    return connector;
  }

}
