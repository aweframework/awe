package com.almis.awe.service.data.connector.query;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.SQLQueryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQLQueryConnector Class
 * Connection class between QueryLauncher and SQLQueryBuilder
 *
 * @author Jorge BELLON 24-02-2017
 */
@Slf4j
public class SQLQueryConnector extends AbstractQueryConnector {

  // Constants
  private static final String ERROR_TITLE_RETRIEVING_DATA = "ERROR_TITLE_RETRIEVING_DATA";
  private static final String ERROR_MESSAGE_EXECUTING_SERVICE_QUERY = "ERROR_MESSAGE_EXECUTING_SERVICE_QUERY";

  // Autowired services
  private final AweDatabaseContextHolder contextHolder;
  private final DataSource dataSource;
  private final DatabaseConfigProperties databaseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param contextHolder            Database context holder
   * @param queryUtil                Query utilities
   * @param dataSource               Datasource
   * @param baseConfigProperties     Base configuration properties
   * @param elements                 AWE elements
   * @param numericService           Numeric service
   * @param encodeService            Encode service
   * @param databaseConfigProperties Database configuration properties
   * @param mapper                   Object mapper
   */
  @Autowired
  public SQLQueryConnector(AweDatabaseContextHolder contextHolder, QueryUtil queryUtil, DataSource dataSource,
                           BaseConfigProperties baseConfigProperties, AweElements elements,
                           NumericService numericService, EncodeService encodeService,
                           DatabaseConfigProperties databaseConfigProperties, ObjectMapper mapper) {
    super(queryUtil, baseConfigProperties, elements, numericService, encodeService, mapper);
    this.contextHolder = contextHolder;
    this.dataSource = dataSource;
    this.databaseConfigProperties = databaseConfigProperties;
  }

  /**
   * Launch query
   *
   * @param query      Query to be launched
   * @param parameters Parameters
   * @return Query output as service data
   * @throws AWException Error launching query
   */
  @Override
  public ServiceData launch(Query query, ObjectNode parameters) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Obtain the datasource and connection explicitly to guarantee release on any code path
    QueryFactoryWithConnection factoryWithConnection = getQueryFactoryWithConnection(parameters);
    DataSource currentDataSource = factoryWithConnection.dataSource();
    Connection connection = factoryWithConnection.connection();
    SQLQueryFactory queryFactory = factoryWithConnection.queryFactory();

    try {
      // Get query builder
      SQLQueryBuilder builder = getBean(SQLQueryBuilder.class);

      // Get query variables for further usage
      Map<String, QueryParameter> variableMap = builder.setQuery(query)
        .setFactory(queryFactory)
        .setParameters(parameters)
        .getVariables();

      // Set sort and order
      if (variableMap.containsKey(AweConstants.QUERY_SORT)) {
        ArrayNode sortList = (ArrayNode) variableMap.get(AweConstants.QUERY_SORT).getValue();
        builder.setComponentSort(sortList);
      }

      // Get pagination
      long elementsPerPage = variableMap.get(AweConstants.QUERY_MAX).getValue().asLong();

      // Only manage pagination by default if there is no totalization and elements per page is greater than zero
      boolean paginate = (query.getPaginationManaged() == null || query.isPaginationManaged()) &&
              (query.getTotalizeList() == null || query.getTotalizeList().isEmpty()) &&
              (elementsPerPage > 0);

      // Build query
      SQLQuery<Tuple> queryBuilt = builder.build();
      SQLQuery<Tuple> queryCount = null;
      if (paginate) {
        queryCount = builder.queryForCount().build();
      }

      // Get query preparation time
      LogUtil.checkpoint(timeLapse);

      List<Tuple> results;
      long records;

      if (paginate) {
        long page = variableMap.get(AweConstants.QUERY_PAGE).getValue().asLong();
        queryBuilt.limit(elementsPerPage).offset(elementsPerPage * (page - 1));
      }
      String sql = StringUtil.toUnilineText(getQueryUtil().getFullSQL(queryBuilt.getSQL().getSQL(), queryBuilt.getSQL().getNullFriendlyBindings()));
      try {
        // Launch query
        List<Tuple> allResults = queryBuilt.fetch();
        if (paginate) {
          records = queryFactory.select(SQLExpressions.all).from(queryCount, new PathBuilder<>(Object.class, "R")).fetchCount();
        } else {
          records = allResults.size();
        }
        results = allResults;
      } catch (Exception exc) {
        throw new AWEQueryException(getLocale(ERROR_TITLE_RETRIEVING_DATA), getLocale(ERROR_MESSAGE_EXECUTING_SERVICE_QUERY, query.getId()), sql, exc);
      }

      // Get query preparation time
      LogUtil.checkpoint(timeLapse);

      DataList datalist;
      try {
        // Generate datalist
        datalist = fillDataList(results, records, query, queryBuilt.getMetadata().getProjection(), variableMap);

        // Get query preparation time
        LogUtil.checkpoint(timeLapse);

        // Log query
        log.info("[\u001B[34m{}\u001B[0m] [{}] => {} records. Create query time: {}s - Sql time: {}s - Datalist time: {}s - {}",
          query.getId(), sql, records,
          LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
          LogUtil.getElapsed(timeLapse, AweConstants.EXECUTION_TIME),
          LogUtil.getElapsed(timeLapse, AweConstants.RESULTS_TIME),
          LogUtil.getTotalTime(timeLapse));
      } catch (Exception exc) {
        throw new AWEQueryException(getLocale(ERROR_TITLE_RETRIEVING_DATA), getLocale(ERROR_MESSAGE_EXECUTING_SERVICE_QUERY, query.getId()), sql, exc);
      }

      ServiceData out = new ServiceData();
      out.setDataList(datalist);
      return out;

    } finally {
      // Always release the connection back to the pool, even if an exception occurred
      DataSourceUtils.releaseConnection(connection, currentDataSource);
    }
  }

  /**
   * Internal record to carry the query factory together with the connection and datasource
   * needed to release the connection after use.
   */
  private record QueryFactoryWithConnection(SQLQueryFactory queryFactory, Connection connection, DataSource dataSource) {}

  /**
   * Retrieve corresponding query factory, obtaining the connection explicitly
   * so it can be released by the caller via {@link DataSourceUtils#releaseConnection}.
   *
   * @param parameters Parameters
   * @return QueryFactoryWithConnection holding the factory, the open connection and the datasource
   * @throws AWException Error retrieving query factory
   */
  private QueryFactoryWithConnection getQueryFactoryWithConnection(ObjectNode parameters) throws AWException {
    // Retrieve current datasource
    DataSource currentDataSource = dataSource;
    DatabaseConnection databaseConnection = contextHolder.getDatabaseConnection(dataSource);

    // Check if call refers to a specific database
    String database = Optional.ofNullable(getQueryUtil().getRequestParameter(databaseConfigProperties.getParameterName(), parameters))
      .orElse(JsonNodeFactory.instance.nullNode()).textValue();
    if (database != null) {
      databaseConnection = contextHolder.getDatabaseConnection(database);
      currentDataSource = databaseConnection.getDataSource();
      MDC.put(AweConstants.SESSION_DATABASE, database);
    }

    // Obtain the connection explicitly from the pool (Spring-managed) so we can release it in a finally block
    Connection connection = DataSourceUtils.getConnection(currentDataSource);
    try {
      final Connection finalConnection = connection;
      SQLQueryFactory queryFactory = new SQLQueryFactory(getConfiguration(databaseConnection.getConnectionType()), () -> finalConnection);
      return new QueryFactoryWithConnection(queryFactory, connection, currentDataSource);
    } catch (Exception e) {
      // If factory/configuration setup fails after the connection was already obtained,
      // release it immediately to avoid connection-pool leaks.
      DataSourceUtils.releaseConnection(connection, currentDataSource);
      throw e;
    }
  }

  /**
   * Get query configuration
   *
   * @param type connection type
   * @return SQL query configuration
   */
  private Configuration getConfiguration(String type) {
    Configuration config = (Configuration) getBean(type + "DatabaseConfiguration");
    config.setUseLiterals(true);

    return config;
  }

  /**
   * Fill output datalist
   *
   * @param results    Query result
   * @param records    Query records
   * @param query      Query with the information
   * @param projection Query projection
   * @return Query output as datalist
   * @throws AWException Error generating datalist
   */
  private DataList fillDataList(List<Tuple> results, long records, Query query, Expression<?> projection, Map<String, QueryParameter> variableMap) throws AWException {
    DataListBuilder builder = getBean(DataListBuilder.class);
    builder.setQueryProjection(projection)
      .setQueryResult(results)
      .setRecords(records)
      .setMax(variableMap.get(AweConstants.QUERY_MAX).getValue().asLong())
      .setPage(variableMap.get(AweConstants.QUERY_PAGE).getValue().asLong())
      .paginate(!query.isPaginationManaged())
      .generateIdentifiers();

    // Add transformations & translations
    builder = processDataList(builder, query, variableMap);

    return builder.build();
  }

  @Override
  public ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) throws AWException {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
