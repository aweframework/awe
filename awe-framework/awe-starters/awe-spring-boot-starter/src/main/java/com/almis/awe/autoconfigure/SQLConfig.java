package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.component.AweRoutingDataSource;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.listener.SpringSQLCloseListener;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import com.almis.awe.service.data.builder.SQLMaintainBuilder;
import com.almis.awe.service.data.builder.SQLQueryBuilder;
import com.almis.awe.service.data.connector.maintain.SQLMaintainConnector;
import com.almis.awe.service.data.connector.query.SQLQueryConnector;
import com.almis.awe.template.FixedOracleTemplates;
import com.almis.awe.template.FixedSQLServerTemplates;
import com.querydsl.sql.*;
import com.querydsl.sql.types.ClobType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.sql.DataSource;

/**
 * Class used to launch initial load treads
 */
@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(name = "awe.database.enabled", havingValue = "true", matchIfMissing = true)
public class SQLConfig {

  /**
   * Database context holder
   *
   * @param elements                 Awe elements
   * @param queryService             Query service
   * @param sessionService           Session service
   * @param dataSourceProperties     DataSource properties
   * @param databaseConfigProperties Database config properties
   * @return Database context holder bean
   */
  @Bean
  @ConditionalOnMissingBean
  @ConfigurationProperties("spring.datasource.hikari")
  public AweDatabaseContextHolder aweDatabaseContextHolder(AweElements elements, QueryService queryService, SessionService sessionService, DataSourceProperties dataSourceProperties, DatabaseConfigProperties databaseConfigProperties) {
    return new AweDatabaseContextHolder(elements, queryService, sessionService, dataSourceProperties, databaseConfigProperties);
  }

  /**
   * Abstract Routing Datasource.
   *
   * @param databaseContextHolder Database context holder
   * @return Datasource bean
   */
  @Bean
  @ConditionalOnProperty(name = "awe.database.multidatabase-enable", havingValue = "true")
  @ConditionalOnMissingBean
  public AweRoutingDataSource aweRoutingDataSource(AweDatabaseContextHolder databaseContextHolder) {
    return new AweRoutingDataSource(databaseContextHolder);
  }

  /**
   * Oracle database configuration
   *
   * @return Oracle database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration oracleDatabaseConfiguration() {
    return getConfiguration(new FixedOracleTemplates());
  }

  /**
   * SQL Server database configuration
   *
   * @return SQL Server database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration sqlserverDatabaseConfiguration() {
    return getConfiguration(new FixedSQLServerTemplates());
  }

  /**
   * Sybase database configuration
   *
   * @return Sybase database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration sybaseDatabaseConfiguration() {
    return getConfiguration(SQLTemplates.DEFAULT);
  }

  /**
   * HSQL database configuration
   *
   * @return HSQL database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration hsqldbDatabaseConfiguration() {
    return getConfiguration(HSQLDBTemplates.builder().build());
  }

  /**
   * H2 database configuration
   *
   * @return HSQL database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration h2DatabaseConfiguration() {
    return getConfiguration(H2Templates.builder().build());
  }

  /**
   * MySQL database configuration
   *
   * @return MySQL database configuration bean
   */
  @Bean
  @Scope("prototype")
  public Configuration mysqlDatabaseConfiguration() {
    return getConfiguration(MySQLTemplates.builder().build());
  }

  /**
   * Get configuration with listener
   *
   * @param templates SQL Templates
   * @return Configuration bean
   */
  private Configuration getConfiguration(SQLTemplates templates) {
    Configuration configuration = new Configuration(templates);
    configuration.addListener(new SpringSQLCloseListener());
    configuration.register(new ClobType());
    return configuration;
  }

  /////////////////////////////////////////////
  // CONNECTORS
  /////////////////////////////////////////////

  /**
   * SQL Query connector
   *
   * @param contextHolder            Context holder
   * @param queryUtil                Query util
   * @param dataSource               Datasource
   * @param baseConfigProperties     Base configuration properties
   * @param elements                 AWE elements
   * @param numericService           Numeric Service
   * @param encodeService            Encode Service
   * @param databaseConfigProperties Database configuration properties
   * @return SQL Query connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public SQLQueryConnector sqlQueryConnector(AweDatabaseContextHolder contextHolder, QueryUtil queryUtil, DataSource dataSource, BaseConfigProperties baseConfigProperties, AweElements elements, NumericService numericService, EncodeService encodeService, DatabaseConfigProperties databaseConfigProperties) {
    return new SQLQueryConnector(contextHolder, queryUtil, dataSource, baseConfigProperties, elements, numericService, encodeService, databaseConfigProperties);
  }

  /**
   *  SQL Maintain connector
   * @param queryUtil QueryUtil service
   * @param databaseConfigProperties Database configuration properties
   * @return SQL Query connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public SQLMaintainConnector sqlMaintainConnector(QueryUtil queryUtil, DatabaseConfigProperties databaseConfigProperties) {
    return new SQLMaintainConnector(queryUtil, databaseConfigProperties);
  }

  /////////////////////////////////////////////
  // BUILDERS
  /////////////////////////////////////////////

  /**
   * SQL Query builder
   *
   * @param queryUtil Query utilities
   * @param encodeService Encode service
   * @return SQL Query builder bean
   */
  @Bean
  @ConditionalOnMissingBean
  @Scope("prototype")
  public SQLQueryBuilder sqlQueryBuilder(QueryUtil queryUtil, EncodeService encodeService) {
    return new SQLQueryBuilder(queryUtil, encodeService);
  }

  /**
   * SQL Maintain builder
   * @param queryUtil QueryUtil service
   * @param encodeService Encode service
   * @param databaseConfigProperties Database properties
   * @return SQLMaintainBuilder bean
   */
  @Bean
  @ConditionalOnMissingBean
  @Scope("prototype")
  public SQLMaintainBuilder sqlMaintainBuilder(QueryUtil queryUtil, EncodeService encodeService, DatabaseConfigProperties databaseConfigProperties) {
    return new SQLMaintainBuilder(queryUtil, encodeService, databaseConfigProperties);
  }
}
