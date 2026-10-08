package com.almis.awe.testing.database;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a Spring test class against one of the AWE test databases.
 *
 * <p>The database is configured through the {@code spring.datasource.*}, {@code spring.sql.init.*} (and, with
 * {@link #flyway()}, {@code spring.flyway.*}) properties, which take precedence over any {@code @TestPropertySource}.
 * A database that needs a container starts it the first time a context is created and shares it with the rest of the JVM.
 * With {@code -Ddb.external=true} no container starts and the connection comes from the environment.</p>
 *
 * <pre>
 * &#64;SpringBootTest
 * &#64;AweDatabaseTest(TestDatabase.POSTGRESQL)
 * class MyQueryIT { ... }
 * </pre>
 *
 * <p>The module needs the JDBC driver of the database it tests.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AweDatabaseTest {

  /**
   * Database to run against
   *
   * @return Database
   */
  TestDatabase value();

  /**
   * True to migrate the schema with Flyway instead of running the schema and data scripts
   *
   * @return Flyway variant
   */
  boolean flyway() default false;

  /**
   * AWE modules to migrate in the Flyway variant
   *
   * @return Module names
   */
  String[] migrationModules() default {"AWE", "SCHEDULER", "NOTIFIER", "awe-boot"};
}
