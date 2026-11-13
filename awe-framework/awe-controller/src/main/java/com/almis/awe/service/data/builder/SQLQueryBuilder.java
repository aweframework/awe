package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.ColumnFilterCondition;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.queries.*;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.type.JoinType;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.type.UnionType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.DateFormats;
import com.almis.awe.model.util.data.date.DateTimeParser;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.data.builder.sql.SqlFieldFunctions;
import com.almis.awe.service.data.builder.sql.SqlFilterConditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLSerializer;
import com.querydsl.sql.WindowFunction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates sql codes
 */
public class SQLQueryBuilder extends SQLBuilder {

  // Alias of the query that wraps a UNION
  private static final String UNION_ALIAS = "T";
  // Aliases of the grid query wrapped to list the values of a column, of the union of its fields and of their value
  private static final String VALUES_ALIAS = "V";
  private static final String VALUES_UNION_ALIAS = "U";
  private static final String VALUE_ALIAS = "column_value";
  private static final String ERROR_TITLE_GENERATING_COLUMN_FILTER = "ERROR_TITLE_GENERATING_FILTER";
  private static final String ERROR_MESSAGE_GENERATING_COLUMN_FILTER = "ERROR_MESSAGE_GENERATING_FILTER";

  // Query is for count only
  boolean queryIsForCount = false;

  // Grid column filters
  private List<ColumnFilter> columnFilterList;

  /**
   * Autowired constructor
   *
   * @param queryUtil Query utilities
   * @param encodeService Encode service
   */
  public SQLQueryBuilder(QueryUtil queryUtil, EncodeService encodeService) {
    super(queryUtil, encodeService);
  }

  /**
   * Generate the sortlist from component sort
   *
   * @param sortList Component sort
   * @return this
   */
  public SQLQueryBuilder setComponentSort(ArrayNode sortList) {
    super.addComponentSort(sortList);
    return this;
  }

  @Override
  public SQLQueryBuilder setQuery(Query query) {
    super.setQuery(query);
    return this;
  }

  @Override
  public SQLQueryBuilder setFactory(SQLQueryFactory factory) {
    super.setFactory(factory);
    return this;
  }

  @Override
  public SQLQueryBuilder setVariables(Map<String, QueryParameter> variableMap) {
    super.setVariables(variableMap);
    return this;
  }

  @Override
  public SQLQueryBuilder setParameters(ObjectNode parameters) {
    super.setParameters(parameters);
    return this;
  }

  /**
   * Set the grid column filters, ANDed to the WHERE of the query (to the wrapper of a UNION query)
   *
   * @param columnFilters Column filters
   * @return this
   */
  public SQLQueryBuilder setColumnFilters(List<ColumnFilter> columnFilters) {
    this.columnFilterList = columnFilters;
    return this;
  }

  /**
   * Set query for count only
   *
   * @return this
   */
  public SQLQueryBuilder queryForCount() {
    queryIsForCount = true;
    return this;
  }

  /**
   * Builds the SQLQuery
   *
   * @return SQLQuery prepared for fetch
   * @throws AWException Error building query
   */
  public SQLQuery<Tuple> build() throws AWException {
    // Throws exception if elements are not assigned
    if (getElements() == null) {
      throw new NullPointerException("Define elements before building the SQL query");
    }

    // Throws exception if query is not defined
    if (getQuery() == null) {
      throw new NullPointerException(getLocale("ERROR_TITLE_NOT_DEFINED", "query"));
    }

    // Throws exceptions if factory is not defined
    if (getFactory() == null) {
      throw new NullPointerException(getLocale("ERROR_TITLE_NOT_DEFINED", "factory"));
    }

    // Prepare query variables
    if (getVariables() == null) {
      setVariables(queryUtil.getVariableMap(getQuery(), getParameters()));
    }

    // Resolve the column filter fields first: an unknown or noprint field builds no SQL
    List<List<SqlField>> columnFilterFields = getColumnFilterFields();

    // Builds the basic query (SELECT FROM)
    SQLQuery<Tuple> finalQuery = getFactory().select(getFields()).from(getTables());

    // Do distinct query
    if (getQuery().isDistinct()) {
      finalQuery.distinct();
    }

    // If JOIN operations were defined, they are added
    if (getQuery().getJoinList() != null) {
      doJoin(finalQuery);
    }

    // If WHERE operations were defined, apply them
    if (getQuery().getFilterGroup() != null) {
      doWhere(finalQuery);
    }

    // Add the column filters to the WHERE, unless the query is wrapped as a UNION
    if (getQuery().getUnionList() == null) {
      doColumnFilters(finalQuery, columnFilterFields, null);
    }

    // If GROUP BY operations were defined, apply
    if (getQuery().getGroupByList() != null) {
      doGroupBy(finalQuery);
    }

    // If HAVING operations were defined, apply
    if (getQuery().getHavingGroup() != null) {
      doHaving(finalQuery);
    }

    // If UNION operations were defined, apply them
    SQLQuery<Tuple> finalExpression = finalQuery;
    if (getQuery().getUnionList() != null) {
      finalExpression = doUnion(finalQuery);
      doColumnFilters(finalExpression, columnFilterFields, UNION_ALIAS);
    }

    // Do ORDER BY
    if (!queryIsForCount && (getQuery().getOrderByList() != null || componentSortList != null)) {
      doOrderBy(finalExpression);
    }

    return finalExpression;
  }

  /**
   * Builds the distinct values of a grid column (#912): the query as built for the grid (with its column filters, without
   * ORDER BY nor pagination) wrapped as subquery V, grouped by the value of the filtered fields with the count of its
   * rows and the smallest value of each field the label needs, sorted by value with the empty values last.
   *
   * @param valueFields Filtered fields; with several, a row counts once for each distinct value of its fields
   * @param labelFields Fields the label of the column needs, empty when the label is the value
   * @param type        Column filter type: dates are grouped by day
   * @param search      Text the value contains, ignoring case; null for none
   * @param limit       Values retrieved at most
   * @return SQLQuery selecting the value, its rows count and the label fields
   * @throws AWException Error building the query
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public SQLQuery<Tuple> buildColumnValues(List<SqlField> valueFields, List<SqlField> labelFields, ColumnFilterType type,
                                           String search, long limit) throws AWException {
    queryIsForCount = true;
    SQLQuery<Tuple> gridQuery = build();

    Expression<?> value;
    SQLQuery<Tuple> valuesQuery;
    if (valueFields.size() == 1) {
      value = getColumnValue(valueFields.get(0), type);
      List<Expression<?>> projection = new ArrayList<>(List.of(value, Wildcard.count));
      for (SqlField labelField : labelFields) {
        // Typed as Object: the JDBC driver reads the value as the type of the field
        projection.add(Expressions.template(Object.class, "min({0})", buildPath(VALUES_ALIAS, labelField.getIdentifier())));
      }
      valuesQuery = getFactory().select(projection.toArray(new Expression<?>[0])).from(gridQuery, buildPath(VALUES_ALIAS));
    } else {
      value = buildPath(VALUES_UNION_ALIAS, VALUE_ALIAS);
      valuesQuery = getFactory().select(new Expression<?>[]{value, Wildcard.count})
        .from(getColumnValuesUnion(gridQuery, valueFields, type), buildPath(VALUES_UNION_ALIAS));
    }

    if (search != null) {
      Expression<?> searchText = ExpressionUtils.extract(getVariableAsExpression(search, ParameterType.STRING));
      valuesQuery.where(Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, value, searchText));
    }

    return valuesQuery.groupBy(value)
      .orderBy(new OrderSpecifier(Order.ASC, value, OrderSpecifier.NullHandling.NullsLast))
      .limit(limit);
  }

  /**
   * Retrieve the union of the values of several filtered fields: each field skips the rows whose value is already the
   * value of a previous field, so a row counts once for each distinct value
   *
   * @param gridQuery Grid query
   * @param fields    Filtered fields
   * @param type      Column filter type
   * @return Union of the values, as column {@code column_value}
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private com.querydsl.sql.Union<Tuple> getColumnValuesUnion(SQLQuery<Tuple> gridQuery, List<SqlField> fields, ColumnFilterType type) {
    // QueryDSL leaves out the parentheses of a subquery in a union branch when the dialect does not wrap the unions
    Expression grid = getFactory().getConfiguration().getTemplates().isUnionsWrapped()
      ? gridQuery : Expressions.template(Object.class, "({0})", gridQuery);
    List<SubQueryExpression<Tuple>> branches = new ArrayList<>();
    for (int index = 0; index < fields.size(); index++) {
      Expression<?> value = getColumnValue(fields.get(index), type);
      SQLQuery<Tuple> branch = getFactory().select(new Expression<?>[]{ExpressionUtils.as(value, VALUE_ALIAS)})
        .from(ExpressionUtils.as(grid, (Path) buildPath(VALUES_ALIAS)));
      for (int previous = 0; previous < index; previous++) {
        branch.where(isDistinct(value, getColumnValue(fields.get(previous), type)));
      }
      branches.add(branch);
    }
    return ((SQLQuery<Tuple>) getFactory().query()).unionAll(branches.toArray(new SubQueryExpression[0]));
  }

  /**
   * Retrieve the value of a filtered field in the wrapped grid query, truncated to the day for dates
   *
   * @param field Filtered field
   * @param type  Column filter type
   * @return Value expression
   */
  private Expression<?> getColumnValue(SqlField field, ColumnFilterType type) {
    Expression<?> path = buildPath(VALUES_ALIAS, field.getIdentifier());
    return type == ColumnFilterType.DATE ? SqlFieldFunctions.function(path, "TRUNCDATE").orElseThrow() : path;
  }

  /**
   * Whether two values are distinct, a null value being distinct from any value but null
   *
   * @param value1 Value
   * @param value2 Value
   * @return Condition
   */
  private static BooleanExpression isDistinct(Expression<?> value1, Expression<?> value2) {
    return Expressions.booleanOperation(Ops.IS_NULL, value1).and(Expressions.booleanOperation(Ops.IS_NOT_NULL, value2))
      .or(Expressions.booleanOperation(Ops.IS_NOT_NULL, value1).and(Expressions.booleanOperation(Ops.IS_NULL, value2)
        .or(Expressions.booleanOperation(Ops.NE, value1, value2))));
  }

  /**
   * Forms the Expressions for the SELECT operation
   *
   * @return Expression[] expressions
   * @throws AWException Error retrieving fields
   */
  protected Expression[] getFields() throws AWException {
    List<SqlField> fieldList = this.getQuery().getSqlFieldList();
    Expression[] fields = new Expression[fieldList.size()];

    // For each field, obtain its expression
    int i = 0;
    for (SqlField field : fieldList) {
      // Store the expression created
      fields[i] = getSqlFieldExpression(field);
      i++;
    }

    return fields;
  }

  /**
   * Forms the Expressions for the SELECT operation
   *
   * @param table Field table
   * @return Expression[] expressions
   */
  protected Expression[] getFieldsAlias(String table) {
    List<SqlField> fieldList = this.getQuery().getSqlFieldList();
    Expression[] fields = new Expression[fieldList.size()];

    // For each field, obtain its expression
    int i = 0;
    for (SqlField field : fieldList) {
      // Store the expression created
      fields[i] = getFieldAliasExpression(field, table);
      i++;
    }

    return fields;
  }

  /**
   * Forms the Expressions for the FROM operation
   *
   * @return Expression[] expressions
   * @throws AWException Error retrieving tables
   */
  protected Expression[] getTables() throws AWException {
    List<Table> tableList = getQuery().getTableList();
    Expression[] tables = new Expression[0];
    if (tableList != null) {
      tables = new Expression[tableList.size()];

      // For each table, obtain its path
      int i = 0;
      for (Table table : tableList) {
        tables[i] = (Expression) getTableExpression(table, table.getAlias() != null);
        i++;
      }
    }
    return tables;
  }

  /**
   * Adds WHERE operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   * @throws AWException Where statement generation error
   */
  protected void doWhere(SQLQuery<Tuple> finalQuery) throws AWException {
    // Obtain the result of applying the expressions contained in the group of filters
    BooleanExpression result = getFilterGroups(this.getQuery().getFilterGroup());

    // Apply the WHERE
    finalQuery.where(result);
  }

  /**
   * Adds JOIN operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   * @throws AWException Error generating join
   */
  private void doJoin(SQLQuery<Tuple> finalQuery) throws AWException {
    if (getQuery().getJoinList() != null) {
      // For each join defined, add an operation
      for (Join join : this.getQuery().getJoinList()) {
        addJoinOperation(join, finalQuery);
      }
    }
  }

  /**
   * Adds JOIN operation to the SQLQuery
   *
   * @param join       Join
   * @param finalQuery SQLQuery created in build method
   * @throws AWException Error adding join operation
   */
  private void addJoinOperation(Join join, SQLQuery<Tuple> finalQuery) throws AWException {
    // For each table defined in join
    Table table = join.getTable();
    Method expressionCondition = null;
    String joinType = getJoinOperator(join.getJoinType());
    FilterGroup joinFilterGroup = join.getFilterGroup();

    try {
      // If subquery is defined, obtain it and alias if defined
      Object tableExpression = getTableExpression(table, false);

      // Generate the alias
      PathBuilder<Object> alias = buildPath(table.getAlias());

      // Do the join
      Class expressionClass = tableExpression instanceof EntityPath ? EntityPath.class : SubQueryExpression.class;
      SQLQuery<Tuple> joinQuery;
      if (alias != null) {
        expressionCondition = finalQuery.getClass().getMethod(joinType, expressionClass, Path.class);
        joinQuery = (SQLQuery<Tuple>) (expressionCondition.invoke(finalQuery, expressionClass.cast(tableExpression), alias));
      } else {
        expressionCondition = finalQuery.getClass().getMethod(joinType, expressionClass);
        joinQuery = (SQLQuery<Tuple>) (expressionCondition.invoke(finalQuery, expressionClass.cast(tableExpression)));
      }

      // If filters are defined, add them
      if (joinFilterGroup != null) {
        joinQuery.on(getFilterGroups(joinFilterGroup));
      }
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_LAUNCHING_SQL_QUERY"),
        getLocale("ERROR_MESSAGE_QUERY_JOIN", join.toString(), getQuery().getId()), exc);
    }
  }

  /**
   * Retrieve the join type
   *
   * @param joinType Join type
   * @return Join operator
   */
  private String getJoinOperator(JoinType joinType) {
    String stringType;
    switch (joinType) {
      case FULL:
        stringType = "fullJoin";
        break;
      case LEFT:
        stringType = "leftJoin";
        break;
      case RIGHT:
        stringType = "rightJoin";
        break;
      default:
        stringType = "innerJoin";
    }
    return stringType;
  }

  /**
   * Adds GROUP BY operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   */
  private void doGroupBy(SQLQuery<Tuple> finalQuery) throws AWException {
    // List of partial Expressions from filters
    Expression[] groupExpressions = new Expression[getQuery().getGroupByList().size()];

    int i = 0;
    for (GroupBy groupby : getQuery().getGroupByList()) {
      // Store the path created
      groupExpressions[i] = getGroupByExpression(groupby);
      i++;
    }

    finalQuery.groupBy(groupExpressions);
  }

  /**
   * Adds HAVING operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   * @throws AWException Having statement generation error
   */
  private void doHaving(SQLQuery<Tuple> finalQuery) throws AWException {
    // Obtain the result of applying the expressions contained in the group of
    // filters
    BooleanExpression result = getFilterGroups(this.getQuery().getHavingGroup());

    // Apply the HAVING
    finalQuery.having(result);
  }

  /**
   * Adds UNION operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   * @return SQLQuery with UNIONs added
   * @throws AWException Union statement generation error
   */
  private SQLQuery<Tuple> doUnion(SQLQuery<Tuple> finalQuery) throws AWException {
    com.querydsl.sql.Union<Tuple> finalUnion = null;
    for (Union union : getQuery().getUnionList()) {
      SQLQuery<Tuple> subquery = getSubquery(union.getQuery());
      SQLQuery<Tuple> dummy = (SQLQuery<Tuple>) getFactory().query();

      if ("ALL".equals(union.getType())) {
        if (finalUnion == null) {
          finalUnion = dummy.unionAll(finalQuery, subquery);
        } else {
          finalUnion = dummy.unionAll(finalUnion, subquery);
        }
      } else {
        if (finalUnion == null) {
          finalUnion = dummy.union(finalQuery, subquery);
        } else {
          finalUnion = dummy.union(finalUnion, subquery);
        }
      }
    }
    return getFactory().select(getFieldsAlias(UNION_ALIAS)).from(finalUnion, buildPath(UNION_ALIAS));
  }

  /**
   * Resolve the query fields of each column filter
   *
   * @return Query fields of each column filter, in the order of the column filters
   * @throws AWException A field is not a field of the query or it is not printed
   */
  private List<List<SqlField>> getColumnFilterFields() throws AWException {
    List<List<SqlField>> columnFilterFields = new ArrayList<>();
    if (columnFilterList != null) {
      for (ColumnFilter columnFilter : columnFilterList) {
        columnFilterFields.add(queryUtil.getColumnFilterFields(getQuery(), columnFilter));
      }
    }
    return columnFilterFields;
  }

  /**
   * Adds the column filters to the WHERE: one condition group per column filter, joined by AND
   *
   * @param finalQuery         Query
   * @param columnFilterFields Query fields of each column filter
   * @param table              Alias of the wrapper of a UNION query, null to filter by the field expressions
   * @throws AWException Error generating the column filters
   */
  private void doColumnFilters(SQLQuery<Tuple> finalQuery, List<List<SqlField>> columnFilterFields, String table) throws AWException {
    List<BooleanExpression> conditions = new ArrayList<>();
    for (int index = 0; index < columnFilterFields.size(); index++) {
      ColumnFilter columnFilter = columnFilterList.get(index);
      List<BooleanExpression> fieldConditions = new ArrayList<>();
      for (SqlField field : columnFilterFields.get(index)) {
        Expression<?> fieldExpression = table == null ? getOperandExpression(field) : buildPath(table, field.getIdentifier());
        if (hasAggregate(fieldExpression)) {
          throw new AWEQueryException(getLocale(ERROR_TITLE_GENERATING_COLUMN_FILTER), getLocale(ERROR_MESSAGE_GENERATING_COLUMN_FILTER,
            "field " + field.getIdentifier() + " of column filter " + columnFilter.getId() + " is an aggregate or window function"),
            getQuery().getId(), null);
        }
        BooleanExpression fieldCondition = getColumnFilterCondition(columnFilter, fieldExpression);
        if (fieldCondition != null) {
          addColumnFilterLogMaskedValues(field, fieldCondition);
          fieldConditions.add(fieldCondition);
        }
      }
      BooleanExpression condition = SqlFilterConditions.join(fieldConditions, UnionType.OR.name());
      if (condition != null) {
        conditions.add(condition);
      }
    }

    BooleanExpression result = SqlFilterConditions.join(conditions, UnionType.AND.name());
    if (result != null) {
      finalQuery.where(result);
    }
  }

  /**
   * Whether an expression holds an aggregate or a window function, which cannot be compared in a WHERE. Subqueries are
   * not inspected: an aggregate inside a subquery is valid in a WHERE.
   *
   * @param expression Field expression
   * @return The expression holds an aggregate or a window function
   */
  private static boolean hasAggregate(Expression<?> expression) {
    if (expression instanceof WindowFunction<?> || expression instanceof SqlFieldFunctions.StringAggregation) {
      return true;
    }
    if (expression instanceof com.querydsl.core.types.Operation<?> operation) {
      return operation.getOperator() instanceof Ops.AggOps || operation.getArgs().stream().anyMatch(SQLQueryBuilder::hasAggregate);
    }
    if (expression instanceof TemplateExpression<?> template) {
      return template.getArgs().stream().anyMatch(arg -> arg instanceof Expression<?> argument && hasAggregate(argument));
    }
    return false;
  }

  /**
   * Keep the values compared with a masked column, as the SQL binds them, to mask them in the logged SQL
   *
   * @param field     Query field
   * @param condition Column filter condition on the field
   */
  private void addColumnFilterLogMaskedValues(SqlField field, BooleanExpression condition) {
    if (isLogMaskedField(field.getId())) {
      SQLSerializer serializer = new SQLSerializer(new Configuration(getFactory().getConfiguration().getTemplates()));
      serializer.handle(condition);
      serializer.getConstants().stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(value -> !value.isEmpty())
        .forEach(getLogMaskedValues()::add);
    }
  }

  /**
   * Retrieve the condition of a column filter on one field: the values (or empty) and the conditions, joined by AND
   *
   * @param columnFilter Column filter
   * @param field        Field expression
   * @return Condition, null when the filter adds none
   * @throws AWException Error generating a value
   */
  private BooleanExpression getColumnFilterCondition(ColumnFilter columnFilter, Expression<?> field) throws AWException {
    List<BooleanExpression> parts = new ArrayList<>();
    BooleanExpression values = getColumnFilterValuesCondition(columnFilter, field);
    if (values != null) {
      parts.add(values);
    }

    List<BooleanExpression> conditions = new ArrayList<>();
    for (ColumnFilterCondition condition : columnFilter.getConditions()) {
      conditions.add(getColumnFilterCondition(columnFilter, condition, field));
    }
    BooleanExpression joinedConditions = SqlFilterConditions.join(conditions, columnFilter.getUnion().name());
    if (joinedConditions != null) {
      parts.add(joinedConditions);
    }

    return SqlFilterConditions.join(parts, UnionType.AND.name());
  }

  /**
   * Retrieve the values condition of a column filter: IN the values, OR IS NULL when empty is true
   *
   * @param columnFilter Column filter
   * @param field        Field expression
   * @return Condition, null when the filter has no values nor empty
   * @throws AWException Error generating a value
   */
  private BooleanExpression getColumnFilterValuesCondition(ColumnFilter columnFilter, Expression<?> field) throws AWException {
    List<Expression> values = new ArrayList<>();
    boolean empty = columnFilter.isEmpty();
    for (JsonNode value : columnFilter.getValues() == null ? List.<JsonNode>of() : columnFilter.getValues()) {
      if (value.isNull()) {
        empty = true;
      } else {
        values.add(getVariableAsExpression(value, getValueType(value)));
      }
    }

    // Every value unchecked in the list of values keeps no row
    if (columnFilter.getValues() != null && values.isEmpty() && !empty) {
      return Expressions.booleanTemplate("1 = 0");
    }

    List<BooleanExpression> conditions = new ArrayList<>();
    if (!values.isEmpty()) {
      Expression<?> list = values.size() == 1 ? values.get(0) : Expressions.list(values.toArray(new Expression[0]));
      conditions.add(SqlFilterConditions.condition("in", false, field, list));
    }
    if (empty) {
      conditions.add(Expressions.booleanOperation(Ops.IS_NULL, field));
    }
    return SqlFilterConditions.join(conditions, UnionType.OR.name());
  }

  /**
   * Retrieve the condition of an operator. Text ignores case, number compares numbers and date compares whole days.
   *
   * @param columnFilter Column filter
   * @param condition    Condition
   * @param field        Field expression
   * @return Condition
   * @throws AWException Error generating a value
   */
  private BooleanExpression getColumnFilterCondition(ColumnFilter columnFilter, ColumnFilterCondition condition,
                                                     Expression<?> field) throws AWException {
    return switch (condition.getOperator()) {
      case EMPTY -> Expressions.booleanOperation(Ops.IS_NULL, field);
      case NOT_EMPTY -> Expressions.booleanOperation(Ops.IS_NOT_NULL, field);
      default -> switch (columnFilter.getType()) {
        case NUMBER -> getNumberCondition(condition, field);
        case DATE -> getDateCondition(condition, field);
        default -> getTextCondition(condition, field);
      };
    };
  }

  /**
   * Retrieve a text condition, ignoring case. The % and _ of the value are matched literally.
   *
   * @param condition Condition
   * @param field     Field expression
   * @return Condition
   * @throws AWException Error generating the value
   */
  private BooleanExpression getTextCondition(ColumnFilterCondition condition, Expression<?> field) throws AWException {
    // The constant itself (not a wrapper) lets QueryDSL lower it and escape it for LIKE with the dialect escape
    Expression<?> value = ExpressionUtils.extract(getVariableAsExpression(condition.getValue(), ParameterType.STRING));
    return switch (condition.getOperator()) {
      case EQ -> Expressions.booleanOperation(Ops.EQ_IGNORE_CASE, field, value);
      case NE -> Expressions.booleanOperation(Ops.EQ_IGNORE_CASE, field, value).not();
      case CONTAINS -> Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, field, value);
      case NOT_CONTAINS -> Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, field, value).not();
      case STARTS_WITH -> Expressions.booleanOperation(Ops.STARTS_WITH_IC, field, value);
      case NOT_STARTS_WITH -> Expressions.booleanOperation(Ops.STARTS_WITH_IC, field, value).not();
      case ENDS_WITH -> Expressions.booleanOperation(Ops.ENDS_WITH_IC, field, value);
      case NOT_ENDS_WITH -> Expressions.booleanOperation(Ops.ENDS_WITH_IC, field, value).not();
      default -> throw new IllegalArgumentException(condition.getOperator().getCode());
    };
  }

  /**
   * Retrieve a number condition
   *
   * @param condition Condition
   * @param field     Field expression
   * @return Condition
   * @throws AWException Error generating the value
   */
  private BooleanExpression getNumberCondition(ColumnFilterCondition condition, Expression<?> field) throws AWException {
    Expression<?> value = getNumberExpression(condition.getValue());
    return switch (condition.getOperator()) {
      case BETWEEN -> SqlFilterConditions.condition("ge", false, field, value)
        .and(SqlFilterConditions.condition("le", false, field, getNumberExpression(condition.getTo())));
      default -> SqlFilterConditions.condition(condition.getOperator().getCode(), false, field, value);
    };
  }

  /**
   * Retrieve a date condition on whole days: a day goes from its start to the start of the next day
   *
   * @param condition Condition
   * @param field     Field expression
   * @return Condition
   * @throws AWException Error generating the value
   */
  private BooleanExpression getDateCondition(ColumnFilterCondition condition, Expression<?> field) throws AWException {
    String day = condition.getValue();
    return switch (condition.getOperator()) {
      case EQ -> getDayRange(field, day, day);
      case NE -> getDayRange(field, day, day).not();
      case GT -> SqlFilterConditions.condition("ge", false, field, getDateExpression(day, 1));
      case GE -> SqlFilterConditions.condition("ge", false, field, getDateExpression(day, 0));
      case LT -> SqlFilterConditions.condition("lt", false, field, getDateExpression(day, 0));
      case LE -> SqlFilterConditions.condition("lt", false, field, getDateExpression(day, 1));
      case BETWEEN -> getDayRange(field, day, condition.getTo());
      default -> throw new IllegalArgumentException(condition.getOperator().getCode());
    };
  }

  private BooleanExpression getDayRange(Expression<?> field, String from, String to) throws AWException {
    return SqlFilterConditions.condition("ge", false, field, getDateExpression(from, 0))
      .and(SqlFilterConditions.condition("lt", false, field, getDateExpression(to, 1)));
  }

  private Expression<?> getDateExpression(String day, long plusDays) throws AWException {
    String date = DateFormats.WEB_DATE.format(DateTimeParser.parseWebDate(day).plusDays(plusDays));
    return getVariableAsExpression(date, ParameterType.DATE);
  }

  private Expression<?> getNumberExpression(String number) throws AWException {
    return getVariableAsExpression(number, isLong(number) ? ParameterType.LONG : ParameterType.DOUBLE);
  }

  private static ParameterType getValueType(JsonNode value) {
    if (value.isNumber()) {
      return isLong(value.asString("")) ? ParameterType.LONG : ParameterType.DOUBLE;
    }
    return ParameterType.STRING;
  }

  private static boolean isLong(String number) {
    try {
      Long.parseLong(number);
      return true;
    } catch (NumberFormatException exc) {
      return false;
    }
  }

  /**
   * Adds ORDER BY operations to the SQLQuery
   *
   * @param finalQuery SQLQuery created in build method
   * @throws AWException Order by statement generation error
   */
  private void doOrderBy(SQLQuery<Tuple> finalQuery) throws AWException {
    // Add component sorts
    Set<String> addedSorts = addComponentSorts(finalQuery);

    // Add query sorts
    addQuerySorts(addedSorts, finalQuery);
  }

  /**
   * Add component sorts
   *
   * @param finalQuery Query
   * @return Added sorts
   * @throws AWException Error adding component sorts
   */
  private Set<String> addComponentSorts(SQLQuery<Tuple> finalQuery) throws AWException {
    Set<String> addedSorts = new HashSet<>();

    // Add component sort first
    if (componentSortList != null) {
      for (SortColumn sort : componentSortList) {
        OrderBy orderBy = new OrderBy()
          .setField(sort.getColumnId())
          .setType(sort.getDirection());

        // Store
        addedSorts.add(sort.getColumnId());
        finalQuery.orderBy(getOrderByExpression(orderBy));
      }
    }

    return addedSorts;
  }

  /**
   * Add query sorts
   *
   * @param addedSorts Added sorts
   * @param finalQuery Query
   * @throws AWException Error sorting
   */
  private void addQuerySorts(Set<String> addedSorts, SQLQuery<Tuple> finalQuery) throws AWException {
    // Add query sort
    if (getQuery().getOrderByList() != null) {
      for (OrderBy orderBy : getQuery().getOrderByList()) {
        if (!addedSorts.contains(orderBy.getField())) {
          // Add sort to added sorts
          addedSorts.add(orderBy.getField());
          finalQuery.orderBy(getOrderByExpression(orderBy));
        }
      }
    }
  }
}
