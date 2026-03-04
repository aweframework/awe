package com.almis.awe.model.entities.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DatabaseConnection Class
 * Bean class with database connection
 *
 * @author Pablo GARCIA - 03/Aug/2017
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class DatabaseConnection {
  private String connectionType;
  private String databaseAlias;
  private DataSource dataSource;
  private Connection connection;

  /**
   * Constructor
   * @param connectionType Connection type
   * @param dataSource DataSource
   * @param alias DatabaseAlias
   */
  public DatabaseConnection(String connectionType, DataSource dataSource, String alias) {
    this.connectionType = connectionType;
    this.dataSource = dataSource;
    this.databaseAlias = alias;
  }

  /**
   * Check if a connection has already been assigned to this instance
   * without triggering the lazy-acquisition logic in {@link #getConnection()}.
   *
   * @return {@code true} if a connection object is already held
   */
  public boolean hasConnection() {
    return connection != null;
  }

  /**
   * Get Connection
   * @return connection
   */
  public Connection getConnection() {
    if (connection == null) {
      try {
        connection = dataSource.getConnection();
      } catch (SQLException exc) {
        connection = null;
      }
    }
    return connection;
  }

  public String getConfigurationBean() {
    return getConnectionType() + "DatabaseConfiguration";
  }
}
