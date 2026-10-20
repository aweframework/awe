package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.queries.*;
import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Operation;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.data.builder.sql.SqlDateValues;
import com.almis.awe.service.data.builder.sql.SqlFieldFunctions;
import com.almis.awe.service.data.builder.sql.SqlFilterConditions;
import com.almis.awe.service.data.builder.sql.SqlListValues;
import com.almis.awe.service.data.builder.sql.SqlOperations;
import com.almis.awe.service.data.builder.sql.SqlPaths;
import com.almis.awe.service.data.builder.sql.SubstringValidator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.*;
import tools.jackson.databind.JsonNode;

import java.sql.Clob;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.model.type.ParameterType.LIST_TO_STRING;

/**
 * Generates sql codes
 * <p>
 * The protected methods are the extension points of the project subclasses, and they keep calling each other as
 * they always did. The pure pieces that call no overridable method live in
 * {@code com.almis.awe.service.data.builder.sql}.
 */
public abstract class SQLBuilder extends AbstractQueryBuilder {

  private static final String ERROR_TITLE_GENERATING_FILTER = "ERROR_TITLE_GENERATING_FILTER";
  private static final String ERROR_TITLE_LAUNCHING_SQL_QUERY = "ERROR_TITLE_LAUNCHING_SQL_QUERY";
  private static final String ERROR_MESSAGE_UNSUPPORTED_FUNCTION = "ERROR_MESSAGE_UNSUPPORTED_FUNCTION";
  private static final String ERROR_MESSAGE_UNSUPPORTED_CAST = "ERROR_MESSAGE_UNSUPPORTED_CAST";

  /**
   * Collects the non-empty string constants of an expression. Subqueries keep their own values.
   */
  private static final Visitor<Void, Set<String>> LOG_MASKED_VALUES_COLLECTOR = new Visitor<>() {
    @Override
    public Void visit(com.querydsl.core.types.Constant<?> expression, Set<String> values) {
      if (expression.getConstant() instanceof String value && !value.isEmpty()) {
        values.add(value);
      }
      return null;
    }

    @Override
    public Void visit(FactoryExpression<?> expression, Set<String> values) {
      expression.getArgs().forEach(argument -> argument.accept(this, values));
      return null;
    }

    @Override
    public Void visit(com.querydsl.core.types.Operation<?> expression, Set<String> values) {
      expression.getArgs().forEach(argument -> argument.accept(this, values));
      return null;
    }

    @Override
    public Void visit(ParamExpression<?> expression, Set<String> values) {
      return null;
    }

    @Override
    public Void visit(Path<?> expression, Set<String> values) {
      return null;
    }

    @Override
    public Void visit(SubQueryExpression<?> expression, Set<String> values) {
      return null;
    }

    @Override
    public Void visit(TemplateExpression<?> expression, Set<String> values) {
      expression.getArgs().stream()
        .filter(Expression.class::isInstance)
        .forEach(argument -> ((Expression<?>) argument).accept(this, values));
      return null;
    }
  };

  private SQLQueryFactory factory;
  private final EncodeService encodeService;
  private final Clock clock = Clock.systemDefaultZone();
  private final Set<String> logMaskedFields = new HashSet<>();
  private final Set<String> logMaskedValues = new HashSet<>();

  /**
   * Autowired constructor
   *
   * @param queryUtil Query utilities
   * @param encodeService Encode service
   */
  protected SQLBuilder(QueryUtil queryUtil, EncodeService encodeService) {
    super(queryUtil);
    this.encodeService = encodeService;
  }

  /**
   * Retrieve sql factory
   *
   * @return SQL Factory
   */
  protected SQLQueryFactory getFactory() {
    return factory;
  }

  /**
   * Sets the SQLQueryFactory used by QueryDSL to create the SQLQuery
   *
   * @param factory Factory
   * @return this
   */
  public SQLBuilder setFactory(SQLQueryFactory factory) {
    this.factory = factory;
    return this;
  }

  /**
   * Sets the columns whose values must be masked in the logged SQL (case-insensitive)
   *
   * @param fields Column names
   * @return this
   */
  public SQLBuilder setLogMaskedFields(String... fields) {
    logMaskedFields.clear();
    if (fields != null) {
      for (String field : fields) {
        logMaskedFields.add(field.toLowerCase(Locale.ROOT));
      }
    }
    return this;
  }

  /**
   * Retrieve the values the statement binds that must be masked in the logged SQL: the values of hashed and
   * encrypted variables and the values written to or compared with the masked columns
   *
   * @return Values to mask
   */
  public Set<String> getLogMaskedValues() {
    return logMaskedValues;
  }

  /**
   * Check whether a column is one of the masked columns
   *
   * @param field Column name
   * @return Column values must be masked
   */
  protected boolean isLogMaskedField(String field) {
    return field != null && logMaskedFields.contains(field.toLowerCase(Locale.ROOT));
  }

  /**
   * Keep the string values of an expression to mask them in the logged SQL
   *
   * @param expression Expression
   * @return The same expression
   */
  protected Expression addLogMaskedValues(Expression<?> expression) {
    if (expression != null) {
      expression.accept(LOG_MASKED_VALUES_COLLECTOR, logMaskedValues);
    }
    return expression;
  }

  /**
   * Generates a SQLQuery as a subquery
   *
   * @param queryId Id of the query to generate
   * @return subquery
   * @throws AWException Error retrieving subquery
   */
  protected SQLQuery<Tuple> getSubquery(String queryId) throws AWException {
    SQLQueryBuilder builder = getBean(SQLQueryBuilder.class);
    builder.setLogMaskedFields(logMaskedFields.toArray(new String[0]));
    SQLQuery<Tuple> subquery = builder
      .setQuery(getElements().getQuery(queryId).copy())
      .setFactory(getFactory())
      .setParameters(getParameters())
      .build();
    logMaskedValues.addAll(builder.getLogMaskedValues());
    return subquery;
  }

  /**
   * Retrieve field expression
   *
   * @param field Field to retrieve
   * @return Expression field expression
   * @throws AWException Error retrieving field expression
   */
  protected Expression getFieldExpression(Field field) throws AWException {
    Expression fieldExpression;

    if (field.getQuery() != null) {
      // Field as Subquery
      fieldExpression = getSubquery(field.getQuery());
    } else if (field.getVariable() != null) {
      // Field as variable
      fieldExpression = getVariableExpression(field.getVariable());
    } else {
      // Field from specifying its table
      fieldExpression = buildPath(field.getTable(), field.getId());
    }

    return fieldExpression;
  }

  /**
   * Retrieve field expression from table, field and function
   *
   * @param table    table
   * @param field    field
   * @param function function
   * @return Field expression
   */
  protected Expression getSimpleFieldExpression(String table, String field, String function) throws AWEQueryException {
    return applyFunctionToField(function, buildPath(table, field), field);
  }

  /**
   * Apply function to field
   *
   * @param function        Function to apply
   * @param fieldExpression Field expression
   * @param fieldName       Field name, used to report an unsupported function
   * @return Field expression with function applied
   * @throws AWEQueryException The function is not supported
   */
  private Expression applyFunctionToField(String function, Expression fieldExpression, String fieldName) throws AWEQueryException {
    // Apply function to field
    // An empty function means none, as when it is not set
    if (function != null && !function.isBlank()) {
      fieldExpression = getExpressionFunction(fieldExpression, function, fieldName);
    }

    return fieldExpression;
  }

  /**
   * Apply cast to field
   *
   * @param field           Field to apply cast
   * @param fieldExpression Field expression
   * @return Field expression with function applied
   * @throws AWEQueryException The cast is not supported
   */
  private Expression applyCastToField(SqlField field, Expression fieldExpression) throws AWEQueryException {
    // Apply function to field
    // An empty cast means none, as when it is not set
    if (field.getCast() != null && !field.getCast().isBlank()) {
      fieldExpression = getExpressionCast(fieldExpression, field.getCast(), getFieldName(field));
    }

    return fieldExpression;
  }

  /**
   * Retrieve field expression
   *
   * @param field Field to retrieve
   * @return Expression field expression
   * @throws AWException Error retrieving field expression
   */
  protected Expression getConstantExpression(Constant field) throws AWException {
    ParameterType type = ParameterType.STRING;
    if (field.getType() != null) {
      type = ParameterType.valueOf(field.getType());
    }
    return getVariableAsExpression(field.getValue(), type);
  }

  /**
   * Retrieve a single variable as an Expression
   *
   * @param variableName Variable name
   * @return Expression
   * @throws AWException
   */
  protected Expression getVariableExpression(String variableName) throws AWException {
    Expression variableExpression = null;

    Variable variable = getQuery().getVariableDefinition(variableName);
    if (variable != null) {
      // Get variable values from previously prepared map
      JsonNode variableValue = variables.get(variable.getId()).getValue();
      boolean isList = variables.get(variable.getId()).isList();
      if (isList) {
        throw new AWException(getLocale(ERROR_TITLE_LAUNCHING_SQL_QUERY),
          getLocale("ERROR_MESSAGE_MALFORMED_QUERY_LIST_FIELD", variableName));
      }
      variableExpression = getVariableAsExpression(variableValue, ParameterType.valueOf(variable.getType()));
    }

    return variableExpression;
  }

  /**
   * Retrieve field expression
   *
   * @param field Field to retrieve
   * @param table Table of the field
   * @return Expression field expression
   */
  protected Expression getFieldAliasExpression(SqlField field, String table) {
    Expression fieldExpression = null;

    if (field.getIdentifier() != null) {
      fieldExpression = buildPath(table, field.getIdentifier(), field.getIdentifier());
    }

    return fieldExpression;
  }

  /**
   * Retrieve operation expression
   *
   * @param operation Operation to manage
   * @return Expression concat expression
   */
  protected Expression getOperationExpression(Operation operation) throws AWException {
    Expression operationExpression = null;
    List<Expression> operands = new ArrayList<>();

    if (operation.getOperandList() != null) {
      if ("SUBSTRING".equals(operation.getOperator())) {
        validateSubstringOperation(operation);
      }
      for (SqlField operand : operation.getOperandList()) {
        operands.add(getOperandExpression(operand));
      }
      operationExpression = generateOperationExpression(operation, operands.toArray(new Expression[0]));
    }

    return operationExpression;
  }

  /**
   * Validate a SUBSTRING operation (see {@link SubstringValidator})
   *
   * @param operation SUBSTRING operation to validate
   * @throws AWException if the operand count is not 2 or 3, or if an index operand is not integer-compatible
   */
  private void validateSubstringOperation(Operation operation) throws AWException {
    Optional<SubstringValidator.Violation> violation = SubstringValidator.validate(operation.getOperandList(),
      variable -> getQuery().getVariableDefinition(variable));
    if (violation.isPresent()) {
      throw new AWException(
        getLocale(ERROR_TITLE_LAUNCHING_SQL_QUERY),
        getLocale(violation.get().messageKey(), violation.get().arguments().toArray(new String[0])));
    }
  }

  /**
   * Apply operand into operation
   *
   * @param operation Operation
   * @param operands  Operand list
   * @return Operation expression
   */
  protected Expression generateOperationExpression(Operation operation, Expression... operands) {
    return SqlOperations.operation(operation.getOperator(), operands);
  }

  /**
   * Retrieve operand template
   *
   * @param operand Operand to retrieve template
   * @return Expression concat expression
   */
  protected Expression getOperandExpression(SqlField operand) throws AWException {
    Expression expression;
    if (operand instanceof Constant constant) {
      expression = getConstantExpression(constant);
    } else if (operand instanceof Field field) {
      expression = getFieldExpression(field);
    } else if (operand instanceof Operation operation) {
      expression = getOperationExpression(operation);
    } else if (operand instanceof Over over) {
      expression = getOverExpression(over);
    } else if (operand instanceof Case case1) {
      expression = getCaseExpression(case1);
    } else {
      return null;
    }

    // Apply cast
    Expression castedField = applyCastToField(operand, expression);

    // Apply function
    return applyFunctionToField(operand.getFunction(), castedField, getFieldName(operand));
  }

  /**
   * Retrieve operand template
   *
   * @param field Operand to retrieve template
   * @return Expression concat expression
   */
  protected Expression getSqlFieldExpression(SqlField field) throws AWException {
    Expression expression = getOperandExpression(field);

    // Add an alias to the field if specified
    if (field.getIdentifier() != null) {
      expression = Expressions.as(expression, buildPath(field.getIdentifier()));
    }

    return expression;
  }

  /**
   * Retrieve case expression
   *
   * @param field Field to apply the case when condition
   * @return Expression caseWhen expression
   */
  protected Expression getCaseExpression(Case field) throws AWException {
    CaseBuilder initialCase = new CaseBuilder();
    CaseBuilder.Cases caseList = null;
    Expression caseElse = getOperandExpression(getSqlFieldFromTransition(field.getCaseElse()));

    if (field.getCaseWhenList() != null) {
      for (CaseWhen caseWhen : field.getCaseWhenList()) {
        BooleanExpression filter = caseWhen.getFilterAnd() != null ? getFilterGroups(caseWhen.getFilterAnd()) : getFilters(caseWhen);
        Expression caseThen = getOperandExpression(getSqlFieldFromTransition(caseWhen.getThenOperand()));
        if (caseList == null) {
          caseList = initialCase.when(filter).then(caseThen);
        } else {
          caseList.when(filter).then(caseThen);
        }
      }
    }

    return caseList == null ? caseElse : caseList.otherwise(caseElse);
  }

  /**
   * Retrieve over expression
   *
   * @param field Field to apply the case when condition
   * @return Expression caseWhen expression
   */
  protected Expression getOverExpression(Over field) throws AWException {
    WindowOver overField = (WindowOver) getOperandExpression(field.getFieldList().get(0));
    WindowFunction overFunction = overField.over();

    // Add partition by aggregators
    if (field.getPartitionByList() != null) {
      for (PartitionBy partitionBy : field.getPartitionByList()) {
        overFunction = overFunction.partitionBy(getGroupByExpression(partitionBy));
      }
    }

    // Add order by sorters
    if (field.getOrderByList() != null) {
      for (OrderBy orderBy : field.getOrderByList()) {
        overFunction = overFunction.orderBy(getOrderByExpression(orderBy));
      }
    }

    return overFunction;
  }

  /**
   * Retrieve the expression to group or partition by. A nested case takes precedence, and
   * otherwise the field path is wrapped in the configured function, so composite expressions
   * are honoured identically in a group-by and in a window function's partition-by (both are
   * modelled by {@link GroupBy}).
   *
   * @param groupBy Group by or partition by element
   * @return Expression
   * @throws AWException Error building the expression
   */
  protected Expression getGroupByExpression(GroupBy groupBy) throws AWException {
    return groupBy.getGroupByCase() != null
      ? getCaseExpression(groupBy.getGroupByCase())
      : getSimpleFieldExpression(groupBy.getTable(), groupBy.getField(), groupBy.getFunction());
  }

  /**
   * Retrieve order by expression
   *
   * @param orderBy Order by
   * @return Expression
   */
  protected OrderSpecifier getOrderByExpression(OrderBy orderBy) throws AWEQueryException {
    Order order = orderBy.getType() != null ? Order.valueOf(orderBy.getType().toUpperCase()) : Order.ASC;
    OrderSpecifier orderSpecifier = new OrderSpecifier(order, getSimpleFieldExpression(orderBy.getTable(), orderBy.getField(), orderBy.getFunction()));
    if (orderBy.getNulls() != null) {
      if ("FIRST".equalsIgnoreCase(orderBy.getNulls())) {
        orderSpecifier = orderSpecifier.nullsFirst();
      } else {
        orderSpecifier = orderSpecifier.nullsLast();
      }
    }
    return orderSpecifier;
  }

  /**
   * Retrieve table expression
   *
   * @param table Table
   * @return Table expression
   * @throws AWException Error generating table expression
   */
  protected Object getTableExpression(Table table, boolean withAlias) throws AWException {
    // If subquery is defined, obtain it and alias if defined
    if (table.getQuery() != null) {
      SQLQuery<Tuple> subquery = getSubquery(table.getQuery());
      if (withAlias) {
        return subquery.as(buildPath(table.getAlias()));
      } else {
        return subquery;
      }

      // Otherwise, take table name (also schema and alias if defined)
    } else {
      // Check table value
      if (table.getId() == null || table.getId().isEmpty()) {
        throw new AWException("Malformed table expression", "Bad table definition: '" + table + "'");
      }

      // Generate table path
      return getTable(table, withAlias);
    }
  }

  /**
   * Get table expression with schema
   *
   * @param table Table
   * @return Table expression
   */
  protected RelationalPath getTable(Table table, boolean withAlias) {
    String alias = withAlias && table.getAlias() != null ? table.getAlias() : table.getId();
    return new RelationalPathBase<>(Object.class, alias, table.getSchema(), table.getId());
  }

  /**
   * Retrieve filter operand
   *
   * @param filter       Filter
   * @param operandField Operand field
   * @param fieldId      Field id
   * @param table        Table
   * @param variable     Variable
   * @param query        Query
   * @param function     Function
   * @return Filter operand
   * @throws AWException Error generating filter
   */
  private Expression getFilterOperand(Filter filter, TransitionField operandField, String fieldId, String table, String variable, String query, String function) throws AWException {
    Expression operandExpression = null;
    SqlField operand = getSqlFieldFromTransition(operandField);

    if (operand == null) {
      Field field = (Field) new Field()
        .setQuery(query)
        .setVariable(variable)
        .setFunction(function)
        .setId(fieldId)
        .setTable(table);
      operand = field;
    }

    if (operand instanceof Field field && operand.getVariable() != null) {
      operandExpression = getOperandVariableExpression(filter, field);
      if (operandExpression == null) {
        return null;
      }
    } else if (operand instanceof Constant constant) {
      operandExpression = getOperandConstantExpression(filter, constant);
      if (operandExpression == null) {
        return null;
      }
    } else {
      operandExpression = getOperandExpression(operand);
    }


    // If filter is set to ignore cases or trim, trim operands
    if (operand.getId() != null && (filter.isTrim() || filter.isIgnoreCase())) {
      operandExpression = Expressions.stringTemplate("{0}", operandExpression).trim();
    }

    return operandExpression == null ? Expressions.nullExpression() : operandExpression;
  }

  /**
   * Retrieve sql field from transition field (operand, then, else)
   *
   * @param transitionField Transition field
   * @return SQL Field from transition field
   */
  private SqlField getSqlFieldFromTransition(TransitionField transitionField) {
    return transitionField == null ? null : transitionField.getField();
  }

  /**
   * Get variable as expression of a JSON node
   *
   * @param nodeValue Node value
   * @param type      Parameter type
   * @return Variable as expression
   * @throws AWException Error retrieving variable as expression
   */
  Expression getVariableAsExpression(JsonNode nodeValue, ParameterType type) throws AWException {
    if (LIST_TO_STRING.equals(type)) {
      return getListAsStringExpression(nodeValue);
    } else {
      String value = getVariableAsString(nodeValue);
      if ((nodeValue == null || nodeValue.isNull()) && ParameterType.STRING.equals(type)) {
        value = "";
      }
      return getVariableAsExpression(value, type);
    }
  }

  /**
   * Generate variable for parameter
   *
   * @param value Variable value
   * @param type  Variable type
   * @return Variable as expression
   * @throws AWException Variable replacement was wrong
   */
  protected Expression getVariableAsExpression(String value, ParameterType type) throws AWException {
    // Act selecting the variable type
    switch (type) {
      case STRINGL:
        return queryUtil.isEmptyString(value) ? getStringExpression("%") : getStringExpression("%" + value);
      case STRINGR:
        return queryUtil.isEmptyString(value) ? getStringExpression("%") : getStringExpression(value + "%");
      case STRINGB:
        return queryUtil.isEmptyString(value) ? getStringExpression("%%") : getStringExpression("%" + value + "%");
      case CLOB:
        return ExpressionUtils.template(Clob.class, "{0}", value);
      case SYSTEM_DATE:
        return Expressions.currentDate();
      case SYSTEM_TIME:
        return getStringExpression(SqlDateValues.currentTime(clock));
      case SYSTEM_TIMESTAMP:
        return Expressions.currentTimestamp();
      case STRING:
      case STRING_HASH_RIPEMD160:
      case STRING_HASH_PASSWORD:
      case STRING_HASH_SHA:
      case STRING_ENCRYPT:
      case STRING_HASH_PBKDF_2_W_HMAC_SHA_1:
        return getVariableAsExpressionOrEmpty(value, type);
      case DATE:
      case TIMESTAMP:
      case FLOAT:
      case DOUBLE:
      case LONG:
      case INTEGER:
      case STRINGN:
      case MULTIPLE_SEQUENCE:
      case SEQUENCE:
      default:
        return getVariableAsExpressionOrNull(value, type);
    }
  }

  /**
   * Generate variable for parameter
   *
   * @param value Variable value
   * @param type  Variable type
   * @return Variable as expression
   * @throws AWException Variable replacement was wrong
   */
  protected Expression getVariableAsExpressionOrEmpty(String value, ParameterType type) throws AWException {
    if (queryUtil.isEmptyString(value)) {
      return getStringExpression("");
    }

    // Act selecting the variable type
    // Hashed and encrypted values are never written to the logs
    switch (type) {
      case STRING_HASH_RIPEMD160:
        return addLogMaskedValues(getStringExpression(EncodeService.encodeRipEmd160(value)));
      case STRING_HASH_PASSWORD:
        return addLogMaskedValues(getStringExpression(encodeService.encodePassword(value)));
      case STRING_HASH_SHA:
        return addLogMaskedValues(getStringExpression(encodeService.hash(EncodeService.HashingAlgorithms.SHA_256, value)));
      case STRING_HASH_PBKDF_2_W_HMAC_SHA_1:
        return addLogMaskedValues(getStringExpression(encodeService.encodePBKDF2WithHmacSHA1(value)));
      case STRING_ENCRYPT:
        return addLogMaskedValues(getStringExpression(encodeService.encryptRipEmd160(value)));
      case STRING:
      default:
        return getStringExpression(value);
    }
  }

  /**
   * Generate variable for parameter
   *
   * @param value Variable value
   * @param type  Variable type
   * @return Variable as expression
   */
  protected Expression getVariableAsExpressionOrNull(String value, ParameterType type) {
    if (queryUtil.isEmptyString(value)) {
      return Expressions.nullExpression();
    }

    // Act selecting the variable type
    switch (type) {
      case DATE:
        return Expressions.asDate(DateUtil.web2SqlDate(value));
      case TIMESTAMP:
        return Expressions.asDateTime(SqlDateValues.timestamp(value));
      case FLOAT:
        return Expressions.asNumber(Float.valueOf(value));
      case DOUBLE:
        return Expressions.asNumber(Double.valueOf(value));
      case LONG:
      case SEQUENCE:
			case MULTIPLE_SEQUENCE:
				return Expressions.asNumber(Long.valueOf(value));
      case INTEGER:
        return Expressions.asNumber(Integer.valueOf(value));
      case BOOLEAN:
        return Expressions.asBoolean(Boolean.parseBoolean(value));
      case NULL:
        return Expressions.nullExpression();
      case STRINGN:
      default:
        return getStringExpression(value);
    }
  }

  /**
   * Retrieve the name of a field to report it in an error: its alias, its id or its variable
   *
   * @param field Field
   * @return Field name
   */
  private String getFieldName(SqlField field) {
    return Optional.ofNullable(field.getIdentifier())
      .or(() -> Optional.ofNullable(field.getId()))
      .orElse(field.getVariable());
  }

  /**
   * Add function to expression
   *
   * @param fieldExpression Current field expression
   * @param function        Function
   * @param fieldName       Field name, used to report an unsupported function
   * @return Expression
   * @throws AWEQueryException The function is not supported
   */
  protected Expression getExpressionFunction(Expression fieldExpression, String function, String fieldName) throws AWEQueryException {
    return SqlFieldFunctions.function(fieldExpression, function)
      .orElseThrow(() -> getUnsupportedValueException(ERROR_MESSAGE_UNSUPPORTED_FUNCTION, fieldName, function));
  }

  /**
   * Retrieve the exception of a function or cast that the engine does not support
   *
   * @param message   Locale key of the message
   * @param fieldName Field that uses the value
   * @param value     Unsupported value
   * @return Exception naming the query, the field and the value
   */
  private AWEQueryException getUnsupportedValueException(String message, String fieldName, String value) {
    String queryId = getQuery() == null ? null : getQuery().getId();
    return new AWEQueryException(getLocale(ERROR_TITLE_LAUNCHING_SQL_QUERY),
      getLocale(message, queryId, fieldName, value), queryId, null);
  }

  /**
   * Add cast to expression
   *
   * @param fieldExpression Current field expression
   * @param cast            Cast
   * @param fieldName       Field name, used to report an unsupported cast
   * @return Expression
   * @throws AWEQueryException The cast is not supported
   */
  protected Expression getExpressionCast(Expression fieldExpression, String cast, String fieldName) throws AWEQueryException {
    return SqlFieldFunctions.cast(fieldExpression, cast)
      .orElseThrow(() -> getUnsupportedValueException(ERROR_MESSAGE_UNSUPPORTED_CAST, fieldName, cast));
  }

  /**
   * Adds filter groups conditions
   *
   * @param group FilterGroup to add
   * @return BooleanExpression result of filter
   * @throws AWException Filter group statement generation error
   */
  protected BooleanExpression getFilterGroups(FilterGroup group) throws AWException {
    // List of partial Expressions from filters
    List<BooleanExpression> groupExpressions = new ArrayList<>();

    // If this group has groups nested, evaluate them
    if (group.getFilterGroupList() != null) {
      for (FilterGroup filterGroup : group.getFilterGroupList()) {
        groupExpressions.add(getFilterGroups(filterGroup));
      }
    }

    // If this group has filters, add their result to the list
    if (group.getFilterList() != null) {
      for (Filter filter : group.getFilterList()) {
        BooleanExpression filterExpression = getFilters(filter);
        if (filterExpression != null) {
          groupExpressions.add(filterExpression);
        }
      }
    }

    // Evaluate all expressions added to the list
    return SqlFilterConditions.join(groupExpressions, group.getUnion());
  }

  /**
   * Evaluates a filter
   *
   * @param filter Filter to be evaluated
   * @return BooleanExpression result of evaluation
   * @throws AWException Filter statement generation error
   */
  private BooleanExpression getFilters(Filter filter) throws AWException {
    try {
      // The left side of the comparison has to be either a static value of a
      // column from a table (or concatenation of several)
      Expression firstOperand = getFilterOperand(filter, filter.getLeftOperand(), filter.getLeftField(), filter.getLeftTable(), filter.getLeftVariable(), null, filter.getLeftFunction());
      Expression secondOperand = getFilterOperand(filter, filter.getRightOperand(), filter.getRightField(), filter.getRightTable(), filter.getRightVariable(), filter.getQuery(), filter.getRightFunction());

      // If one of the operands return TRUE, it means optional filter with a null value
      if (firstOperand == null || secondOperand == null) {
        return null;
      }

      // The value compared with a masked column is never written to the logs
      if (isLogMaskedField(getFilterFieldId(filter.getLeftOperand(), filter.getLeftField()))) {
        addLogMaskedValues(secondOperand);
      }
      if (isLogMaskedField(getFilterFieldId(filter.getRightOperand(), filter.getRightField()))) {
        addLogMaskedValues(firstOperand);
      }

      // Create the condition for the comparison and execute
      return SqlFilterConditions.condition(filter.getCondition().toLowerCase(), filter.isIgnoreCase(), firstOperand,
        secondOperand);
    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_GENERATING_FILTER),
        getLocale("ERROR_MESSAGE_GENERATING_FILTER", filter.toString()), exc);
    }
  }

  /**
   * Retrieve the column of a filter side, from its operand or from its field
   *
   * @param operand Filter operand
   * @param fieldId Filter field
   * @return Column name
   */
  private String getFilterFieldId(TransitionField operand, String fieldId) {
    SqlField field = getSqlFieldFromTransition(operand);
    return field == null ? fieldId : field.getId();
  }

  /**
   * Generate querydsl expression from variable
   *
   * @param filter Filter
   * @param field  Field to
   * @return Filter variable as expression
   * @throws AWException Conversion was wrong
   */
  private Expression getOperandVariableExpression(Filter filter, Field field) throws AWException {
    Variable variable = getQuery().getVariableDefinition(field.getVariable());
    if (variable == null) {
      throw new AWException(getLocale("ERROR_TITLE_GENERATING_VARIABLE_VALUE"), getLocale("ERROR_MESSAGE_GENERATING_VARIABLE_VALUE", field.getVariable()));
    }

    // Get variable values from previously prepared map
    ParameterType parameterType = ParameterType.valueOf(variable.getType());
    JsonNode variableValue = variables.get(field.getVariable()).getValue();
    boolean isList = variables.get(field.getVariable()).isList();

    return getOperandFilterExpression(filter, field, isList, parameterType, variableValue, getVariableAsString(variableValue));
  }

  /**
   * Generate querydsl expression from variable
   *
   * @param filter Filter
   * @param field  Field to
   * @return Filter variable as expression
   * @throws AWException Conversion was wrong
   */
  private Expression getOperandConstantExpression(Filter filter, Constant field) throws AWException {
    // Get variable values from previously prepared map
    String type = field.getType() == null ? "STRING" : field.getType();
    ParameterType parameterType = ParameterType.valueOf(type);
    JsonNode variableValue = queryUtil.getParameter(field.getValue(), null, parameterType, null);

    return getOperandFilterExpression(filter, field, variableValue.isArray(), parameterType, variableValue, field.getValue());
  }

  /**
   * Generate querydsl expression from variable
   *
   * @param filter Filter
   * @param field  Field to
   * @return Filter variable as expression
   * @throws AWException Conversion was wrong
   */
  private Expression getOperandFilterExpression(Filter filter, SqlField field, boolean isList, ParameterType type, JsonNode variableValue, String stringValue) throws AWException {
    List<Expression> expressionList = new ArrayList<>();
    Expression expression;

    if (isList && (getQuery().getMultiple() == null || "audit".equalsIgnoreCase(getQuery().getMultiple())) && getVariableIndex() == null) {
      // If variable is a list, generate a IN statement filtering out empty lists. Query should not be multiple
      expression = convertVariableListToExpression(expressionList, variableValue, type, filter);
    } else if (isList) {
      // MULTIPLE query, getting index variable (each line a variable)
      return convertVariableListToExpressionWithIndex(variableValue, type, stringValue, filter);
    } else if (filter.isOptional() && queryUtil.isEmptyVariable(variableValue)) {
      // OPTIONAL filter and EMPTY variable, removing filter
      return null;
    } else if (!filter.isOptional() && queryUtil.isEmptyVariable(variableValue) && ParameterType.STRINGN.equals(type)) {
      // NON OPTIONAL filter and EMPTY variable and STRINGN Type, set IS NULL
      return Expressions.nullExpression();
    } else {
      // Retrieve field expression
      return getOperandExpression(field);
    }

    // If variable value is empty, don't generate the filter
    boolean variableOrParameterExpressionEmpty = queryUtil.isEmptyString(stringValue) || expressionList.isEmpty();
    if (filter.isOptional() && variableOrParameterExpressionEmpty) {
      return null;
    }

    return expression;
  }

  /**
   * Variable list as expression (IN)
   *
   * @param expressionList Expression list
   * @param variableValue  Variable value
   * @param type           Variable type
   * @param filter         Filter
   * @return Variable as expression
   * @throws AWException Error converting variable list to expression
   */
  private Expression convertVariableListToExpression(List<Expression> expressionList, JsonNode variableValue, ParameterType type, Filter filter) throws AWException {
    for (JsonNode value : SqlListValues.valuesToBind(variableValue)) {
      expressionList.add(getVariableAsExpression(value, type));
    }
    if (!expressionList.isEmpty()) {
      return Expressions.list(expressionList.toArray(new Expression[0]));
    } else if (filter.isOptional()) {
      // OPTIONAL in, add nothing
      return Expressions.TRUE;
    } else {
      // NON OPTIONAL in, fail
      throw new AWException(getLocale(ERROR_TITLE_GENERATING_FILTER),
        getLocale("ERROR_MESSAGE_EMPTY_LIST_OPTIONAL", filter.toString()));
    }
  }

  /**
   * Variable list as expression for each index
   *
   * @param variableValue Variable value
   * @param type          Variable type
   * @param value         String value
   * @param filter        Filter
   * @return Variable as expression
   * @throws AWException Error converting variable list to expression with index
   */
  private Expression convertVariableListToExpressionWithIndex(JsonNode variableValue, ParameterType type, String value, Filter filter) throws AWException {
    // OPTIONAL filter and EMPTY variable, removing filter
    if (filter.isOptional() && queryUtil.isEmptyString(value)) {
      return null;
    } else if (!filter.isOptional() && queryUtil.isEmptyString(value)) {
      return Expressions.nullExpression();
    } else {
      return getVariableAsExpression(variableValue.get(getVariableIndex()), type);
    }
  }

  /**
   * Retrieve string expression with quotes
   *
   * @param value String value
   * @return String expression with quotes
   */
  protected Expression getStringExpression(String value) {
    if (value != null) {
      return Expressions.asString(value);
    } else {
      return Expressions.nullExpression();
    }
  }

  /**
   * Retrieve string expression with quotes
   *
   * @param valueList Json value list
   * @return String expression with quotes
   */
  private Expression getListAsStringExpression(JsonNode valueList) {
    String stringValue = SqlListValues.join(valueList, this::getVariableAsString);
    return stringValue.isEmpty() ? Expressions.nullExpression() : getStringExpression(stringValue);
  }

  /**
   * Build a path with pathbuilder
   *
   * @param parent Parent
   * @param node   Node
   * @param alias  Alias
   * @return Field path
   */
  protected SimpleExpression<Object> buildPath(String parent, String node, String alias) {
    SimpleExpression<Object> target = buildPath(parent, node);

    // Generate table alias (there is no path to alias when neither parent nor node are defined)
    if (target != null && alias != null) {
      target = target.as(buildPath(alias));
    }

    return target;
  }

  /**
   * Build a path with pathbuilder
   *
   * @param parent Parent node
   * @param node   Node
   * @return path
   */
  protected PathBuilder<Object> buildPath(String parent, String node) {
    PathBuilder<Object> target;
    if (parent != null) {
      target = buildPath(parent).get(node);
    } else {
      target = buildPath(node);
    }

    return target;
  }

  /**
   * Build a path with pathbuilder
   *
   * @param node Node
   * @return path
   */
  protected PathBuilder<Object> buildPath(String node) {
    return SqlPaths.path(node);
  }
}
