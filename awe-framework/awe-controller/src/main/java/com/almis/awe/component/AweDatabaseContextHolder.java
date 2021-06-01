package com.almis.awe.component;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.DatabaseConnectionInfo;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pgarcia
 */
@Getter
@Setter
public class AweDatabaseContextHolder extends ServiceConfig {

  private static final String ERROR_TITLE_INVALID_CONNECTION = "ERROR_TITLE_INVALID_CONNECTION";

  // Autowired services
  private final AweElements elements;
  private final QueryService queryService;
  private final SessionService sessionService;
  private final LogUtil logger;
  private DataSourceProperties properties;

  @Value("${awe.database.multi-database.enable}")
  private boolean multiDatabaseEnable;

  // Store dataSource list
  private Map<Object, Object> dataSourceMap;

  // Pool properties
  private int minimumIdle = 10;
  private int maximumPoolSize = 10;
  private long connectionTimeOut = 30000;
  private String connectionTestQuery;

  /**
   * Autowired constructor
   *
   * @param elements             Awe elements
   * @param queryService         Query service
   * @param sessionService       Session Service
   * @param logger               Logger
   * @param dataSourceProperties DataSource properties
   */
  public AweDatabaseContextHolder(AweElements elements, QueryService queryService, SessionService sessionService, LogUtil logger, DataSourceProperties dataSourceProperties) {
    this.elements = elements;
    this.queryService = queryService;
    this.sessionService = sessionService;
    this.logger = logger;
    this.properties = dataSourceProperties;
    this.dataSourceMap = new HashMap<>();
  }

  /**
   * Load dataSources from current connection
   *
   * @return datasource map
   */
  public Map<Object, Object> getDataSources() {
    Map<Object, Object> dataSources = new HashMap<>();
    Map<String, DatabaseConnectionInfo> connectionInfoMap = loadDataSources();

    // Retrieve dataSources
    for (DatabaseConnectionInfo connectionInfo : connectionInfoMap.values()) {
      try {
        // Redefine dataSource properties
        properties = new DataSourceProperties();
        properties.setJndiName(connectionInfo.getJndi());
        properties.setUrl(connectionInfo.getUrl());
        properties.setUsername(connectionInfo.getUser());
        properties.setPassword(connectionInfo.getPassword());
        dataSources.put(connectionInfo.getAlias(), getDataSource(properties));
      } catch (Exception exc) {
        // Log datasource failure
        logger.log(AweDatabaseContextHolder.class, Level.ERROR, "Error retrieving datasource ''{0}''", exc, connectionInfo.getAlias());
      }
    }

    // Redefine target dataSources
    dataSourceMap = dataSources;
    return dataSources;
  }

  /**
   * Retrieve datasource definition
   *
   * @param properties DataSource properties
   * @return Datasource
   */
  DataSource getDataSource(DataSourceProperties properties) {
    HikariDataSource dataSource;
    if (properties.getJndiName() != null) {
      final JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
      dsLookup.setResourceRef(true);
      return dsLookup.getDataSource(properties.getJndiName());
    } else {
      dataSource = (HikariDataSource) properties.initializeDataSourceBuilder().build();
      // Customize pool
      dataSource.setMinimumIdle(minimumIdle);
      dataSource.setMaximumPoolSize(maximumPoolSize);
      dataSource.setConnectionTimeout(connectionTimeOut);
      dataSource.setConnectionTestQuery(connectionTestQuery);
      return dataSource;
    }
  }

  /**
   * Get a datasource connection from an alias
   *
   * @param alias Datasource alias
   * @return Datasource connection
   */
  DataSource getDataSource(String alias) throws AWException {
    if (!multiDatabaseEnable) {
      // Get default datasource
      return getBean(DataSource.class);
    } else if (dataSourceMap.containsKey(alias)) {
      return (DataSource) dataSourceMap.get(alias);
    } else {
      throw new AWException(elements.getLocaleWithLanguage(ERROR_TITLE_INVALID_CONNECTION, elements.getLanguage()),
              elements.getLocaleWithLanguage("ERROR_MESSAGE_UNDEFINED_DATASOURCE", elements.getLanguage(), alias));
    }
  }

  /**
   * Get the default datasource
   *
   * @return Datasource connection
   */
  public DataSource getDefaultDataSource() {
    return getDataSource(properties);
  }

  /**
   * Load dataSources from current connection
   *
   * @return datasource map
   */
  private Map<String, DatabaseConnectionInfo> loadDataSources() {
    Map<String, DatabaseConnectionInfo> connectionMap = new HashMap<>();
    ServiceData serviceData = null;
    try {
      serviceData = queryService.launchPrivateQuery(AweConstants.DATABASE_CONNECTIONS_QUERY, "1", "0");
    } catch (AWException exc) {
      logger.log(AweDatabaseContextHolder.class, Level.ERROR, "Error retrieving dataSources from default connection", exc);
    }

    // Retrieve dataSources
    if (serviceData != null && serviceData.getDataList() != null) {
      for (Map<String, CellData> row : serviceData.getDataList().getRows()) {
        DatabaseConnectionInfo connectionInfo = new DatabaseConnectionInfo(row);
        try {
          connectionMap.put(connectionInfo.getAlias(), connectionInfo);
        } catch (Exception exc) {
          // Log datasource failure
          logger.log(AweDatabaseContextHolder.class, Level.ERROR, "Error retrieving datasource ''{0}''", exc, connectionInfo.getAlias());
        }
      }
    }

    // Redefine target dataSources
    return connectionMap;
  }

  /**
   * Get current connection type
   *
   * @return Database type
   * @throws AWException Error retrieving database type
   */
  public String getDatabaseType(DataSource dataSource) throws AWException {
    try {
      String url = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
      return DatabaseDriver.fromJdbcUrl(url).getId();
    } catch (Exception exc) {
      throw new AWException("Error retrieving database type from datasource", dataSource.toString(), exc);
    }
  }

  /**
   * Get current database
   *
   * @return Current database
   */
  public String getCurrentDatabase() {
    try {
      return (String) sessionService.getSessionParameter(AweConstants.SESSION_DATABASE);
    } catch (Exception exc) {
      return null;
    }
  }

  /**
   * Get current database connection
   *
   * @param dataSource datasource
   * @return Database connection
   * @throws AWException error retrieving connection or database type
   */
  public DatabaseConnection getDatabaseConnection(DataSource dataSource) throws AWException {
    return new DatabaseConnection(getDatabaseType(dataSource), dataSource, getCurrentDatabase());
  }

  /**
   * Get current database connection
   *
   * @param alias Datasource alias
   * @return Database connection
   * @throws AWException error retrieving connection or database type
   */
  public DatabaseConnection getDatabaseConnection(String alias) throws AWException {
    DataSource dataSource = getDataSource(alias);
    return new DatabaseConnection(getDatabaseType(dataSource), dataSource, alias);
  }
}
