package com.almis.awe.service.data.builder;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Table;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.data.ColumnFilterTestFixtures;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static com.almis.awe.service.data.ColumnFilterTestFixtures.stubLocales;
import static org.mockito.Mockito.mock;

/**
 * Shared fixtures of the SQL query builder tests of the grid column autofilter (#912): a users query on table
 * {@code AweUsr u} and a builder whose localization answers the key
 */
final class SQLQueryBuilderFixture {

  static final JsonMapper MAPPER = ColumnFilterTestFixtures.MAPPER;
  static final AweElements ELEMENTS = stubLocales(mock(AweElements.class));
  static final String SELECT = "select u.Nam as name, u.Eml as email, u.Amt as amount, u.Dat as created, u.Sta as status, u.Pwd as secret\n"
    + "from AweUsr u";

  private SQLQueryBuilderFixture() {
  }

  /**
   * Query utilities whose localization answers the key
   *
   * @return Query utilities
   */
  static QueryUtil queryUtil() {
    return ColumnFilterTestFixtures.queryUtil(ELEMENTS);
  }

  /**
   * Users query: name, email, amount, created, status and the noprint secret
   *
   * @return Query
   */
  static Query usersQuery() {
    Query query = new Query();
    query.setId("UsrLst");
    query.setTableList(List.of(Table.builder().id("AweUsr").alias("u").build()));
    query.setSqlFieldList(new ArrayList<>(List.of(field("Nam", "name"), field("Eml", "email"), field("Amt", "amount"),
      field("Dat", "created"), field("Sta", "status"), Field.builder().id("Pwd").table("u").alias("secret").noprint(true).build())));
    return query;
  }

  static SqlField field(String id, String alias) {
    return Field.builder().id(id).table("u").alias(alias).build();
  }

  /**
   * Builder whose localization answers the key and whose subqueries select {@code s.Nam as name from other s}
   */
  static class StubBuilder extends SQLQueryBuilder {

    private final SQLQueryFactory queryFactory;

    StubBuilder(QueryUtil queryUtil, EncodeService encodeService, SQLQueryFactory queryFactory) {
      super(queryUtil, encodeService);
      this.queryFactory = queryFactory;
    }

    @Override
    public AweElements getElements() {
      return ELEMENTS;
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      RelationalPathBase<Object> table = new RelationalPathBase<>(Object.class, "s", null, queryId);
      return queryFactory.select(new Expression<?>[]{new PathBuilder<>(Object.class, "s").get("Nam")}).from(table);
    }
  }
}
