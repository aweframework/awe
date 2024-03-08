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
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
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

    // Generate the corresponding query factory
    SQLQueryFactory queryFactory = getQueryFactory(parameters);

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
    boolean paginate = query.isPaginationManaged() && (elementsPerPage > 0);

    // Build query
    SQLQuery<Tuple> queryBuilt = builder.build();
    SQLQuery<Tuple> queryCount = null;
    if (paginate) {
      queryCount = builder.queryForCount().build();
    }

    // Get query preparation time
    String sql = StringUtil.toUnilineText(getQueryUtil().getFullSQL(queryBuilt.getSQL().getSQL(), queryBuilt.getSQL().getNullFriendlyBindings()));
    LogUtil.checkpoint(timeLapse);

    List<Tuple> results;
    long records;
    try {
      if (paginate) {
        long page = variableMap.get(AweConstants.QUERY_PAGE).getValue().asLong();
        queryBuilt.limit(elementsPerPage).offset(elementsPerPage * (page - 1));
      }

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
  }

  /**
   * Retrieve corresponding query factory
   *
   * @param parameters Parameters
   * @return Query factory
   * @throws AWException Error retrieving query factory
   */
  private SQLQueryFactory getQueryFactory(ObjectNode parameters) throws AWException {
    // Retrieve current datasource
    DataSource currentDataSource = dataSource;
    DatabaseConnection databaseConnection = contextHolder.getDatabaseConnection(dataSource);

    // Check if call refers to a specific database
    String database = Optional.ofNullable(getQueryUtil().getRequestParameter(databaseConfigProperties.getParameterName(), parameters))
      .orElse(JsonNodeFactory.instance.nullNode()).textValue();
    if (database != null) {
      databaseConnection = contextHolder.getDatabaseConnection(database);
      currentDataSource = databaseConnection.getDataSource();
    }

    // Retrieve query factory
    return new SQLQueryFactory(getConfiguration(databaseConnection.getConnectionType()), currentDataSource);
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
