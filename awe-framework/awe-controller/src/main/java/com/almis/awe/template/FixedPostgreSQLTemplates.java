package com.almis.awe.template;
import com.querydsl.sql.PostgreSQLTemplates;

/**
 * Fix to QueryDSL Postgres template to behave the same as other databases
 */
public class FixedPostgreSQLTemplates extends PostgreSQLTemplates {

  @Override
  public String serialize(String literal, int jdbcType) {
    switch (jdbcType) {
      case 16: // boolean
        return literal;
      default:
        return super.serialize(literal, jdbcType);
    }
  }
}
