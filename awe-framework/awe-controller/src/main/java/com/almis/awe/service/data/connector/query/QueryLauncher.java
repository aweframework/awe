package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.service.observation.ExecutionObserver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;

/**
 * QueryLauncher Class
 * Factory class of interface QueryConnector
 *
 */
@Slf4j
public class QueryLauncher extends ServiceConfig {

  // Observer of the executions (metrics, traces...). Optional: without one the execution just runs
  private ExecutionObserver executionObserver = ExecutionObserver.NONE;

  // Database configuration. Optional: without it only the session database keys the cache
  private DatabaseConfigProperties databaseConfigProperties;

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
  @Cacheable(value = "queryData", condition = "#p0.isCacheable()", key = "#root.target.getCacheKey(#p0, #p1)")
  public ServiceData launchQuery(@NonNull Query query, ObjectNode parameters) throws AWException {
    return executionObserver.observe(ExecutionObserver.ExecutionType.QUERY, query.getId(),
      Map.of("connector", getConnectorType(query)), () -> launchObservedQuery(query, parameters), data -> data == null ? null : data.getType());
  }

  /**
   * Set the database configuration, used to read the database the request targets
   *
   * @param databaseConfigProperties Database configuration properties
   */
  @Autowired(required = false)
  public void setDatabaseConfigProperties(DatabaseConfigProperties databaseConfigProperties) {
    this.databaseConfigProperties = databaseConfigProperties;
  }

  /**
   * Build the cache key of a query. Besides the query parameters it includes the session values the query
   * declares, the language the results are translated to and the database the query runs on: all of them
   * change the result, so a cached result must never be shared across them.
   *
   * @param query      Query
   * @param parameters Parameters
   * @return Cache key
   */
  public String getCacheKey(@NonNull Query query, ObjectNode parameters) {
    AweSession session = getSession();
    String language = getRequestParameter(parameters, AweConstants.QUERY_LANGUAGE)
      .orElseGet(() -> getElements().getLanguage());
    String database = getRequestParameter(parameters, databaseConfigProperties == null ? null : databaseConfigProperties.getParameterName())
      .orElseGet(() -> session == null ? null : session.getParameter(String.class, AweConstants.SESSION_DATABASE));
    return query.getQueryKeys(parameters, name -> session == null ? null : session.getParameter(name))
      + "|language=" + language + "|database=" + database;
  }

  /**
   * Read a text request parameter
   *
   * @param parameters Parameters
   * @param name       Parameter name
   * @return Parameter value, when present
   */
  private Optional<String> getRequestParameter(ObjectNode parameters, String name) {
    return Optional.ofNullable(parameters)
      .filter(p -> name != null)
      .map(p -> p.get(name))
      .filter(value -> !value.isNull())
      .map(JsonNode::asString);
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
