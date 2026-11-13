package com.almis.awe.testing.database;

import lombok.Getter;

/**
 * Databases the AWE integration tests can run against.
 *
 * <p>The embedded ones (HSQLDB, H2) run inside the JVM; the others run in a Testcontainers container (see
 * {@link DatabaseContainers}). Each entry carries the conventions the test suites share: the JDBC driver and the
 * {@code classpath:sql/} scripts that create and fill the AWE test schema.</p>
 */
@Getter
public enum TestDatabase {

  HSQLDB(null, "jdbc:hsqldb:file:target/tests/db/awe-test", "org.hsqldb.jdbc.JDBCDriver", "hsqldb", "hsqldb", true),
  H2(null, "jdbc:h2:mem:awe-test;NON_KEYWORDS=value,user,day,year,month,hour,minute,second", "org.h2.Driver", "h2", "h2", false),
  MYSQL("mysql", null, "com.mysql.cj.jdbc.Driver", "mysqldb", "mysql", true),
  POSTGRESQL("postgresql", null, "org.postgresql.Driver", "postgresql", "postgresql", true),
  SQLSERVER("sqlserver", null, "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserverdb", "sqlserverdb", true),
  ORACLE("oracle", null, "oracle.jdbc.OracleDriver", "oracledb", "oracledb", true);

  /**
   * Key of the image in {@code awe-testing-images.properties}; {@code null} for the embedded databases
   */
  private final String imageKey;

  /**
   * JDBC url of the embedded database; {@code null} for the databases that run in a container
   */
  private final String embeddedUrl;

  private final String driverClassName;
  private final String schemaScript;
  private final String dataScript;
  private final boolean continueOnError;

  TestDatabase(String imageKey, String embeddedUrl, String driverClassName, String schemaSuffix, String dataSuffix,
               boolean continueOnError) {
    this.imageKey = imageKey;
    this.embeddedUrl = embeddedUrl;
    this.driverClassName = driverClassName;
    this.schemaScript = "classpath:sql/schema-" + schemaSuffix + ".sql";
    this.dataScript = "classpath:sql/testdata-" + dataSuffix + ".sql";
    this.continueOnError = continueOnError;
  }

  /**
   * Whether the database runs in a container (true) or inside the JVM (false)
   *
   * @return True when a container is needed
   */
  public boolean needsContainer() {
    return imageKey != null;
  }
}
