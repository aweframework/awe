package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.*;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Focused unit tests for the SUBSTRING validation and generation branches in {@link SQLBuilder}.
 * <p>
 * {@code SQLBuilder} is abstract; we use a minimal anonymous subclass that stubs out
 * {@link SQLBuilder#getElements()} so that {@code getLocale()} calls do not require a Spring context.
 * All SUBSTRING-specific private methods are exercised through the public entry point
 * {@link SQLBuilder#getOperationExpression(Operation)}, which is the only calller of those methods.
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderSubstringTest {

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  private SQLQueryFactory factory;

  @BeforeEach
  void setUp() {
    factory = new SQLQueryFactory(new Configuration(new HSQLDBTemplates()), () -> connection);
  }

  // -------------------------------------------------------------------------
  // Happy paths — expression generation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("SUBSTRING(source, begin) with 2 constant args generates SUBSTR_1ARG expression")
  void substringTwoArgConstantsGeneratesSubstr1ArgExpression() throws Exception {
    // source field: table.col
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // begin index as INTEGER constant
    Constant beginIndex = (Constant) new Constant().setType("INTEGER").setValue("3");

    Operation operation = buildSubstringOperation(sourceField, beginIndex);

    Expression<?> result = invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation);

    assertThat(result).isNotNull();
    // QueryDSL encodes the op type in the toString / operator
    assertThat(result.toString()).containsIgnoringCase("substr");
  }

  @Test
  @DisplayName("SUBSTRING(source, begin, end) with 3 constant args generates SUBSTR_2ARGS expression")
  void substringThreeArgConstantsGeneratesSubstr2ArgsExpression() throws Exception {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Constant beginIndex = (Constant) new Constant().setType("INTEGER").setValue("2");
    Constant endIndex = (Constant) new Constant().setType("INTEGER").setValue("5");

    Operation operation = buildSubstringOperation(sourceField, beginIndex, endIndex);

    Expression<?> result = invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation);

    assertThat(result).isNotNull();
    assertThat(result.toString()).containsIgnoringCase("substr");
  }

  @Test
  @DisplayName("SUBSTRING with LONG constant index is accepted and returns non-null expression")
  void substringWithLongConstantIndexIsAccepted() throws Exception {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Constant beginIndex = (Constant) new Constant().setType("LONG").setValue("10");

    Operation operation = buildSubstringOperation(sourceField, beginIndex);

    Expression<?> result = invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("SUBSTRING with variable-backed Field of INTEGER type is accepted")
  void substringWithIntegerVariableFieldIsAccepted() throws Exception {
    // Variable definition in query
    Variable var = Variable.builder().id("startIdx").type("INTEGER").value("3").build();
    Query query = queryWithVariable(var);

    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // Index operand references the variable
    Field beginField = (Field) new Field().setVariable("startIdx");

    Operation operation = buildSubstringOperation(sourceField, beginField);

    // Provide a variables map so getVariableExpression does not NPE after validation
    Map<String, QueryParameter> variablesMap = Map.of(
      "startIdx", queryParameter("3", ParameterType.INTEGER));

    StubSQLQueryBuilder builder = builderWithQuery(query);
    builder.setVariables(variablesMap);

    // Should NOT throw — validation passes for INTEGER variable
    Expression<?> result = invokeGetOperationExpression(builder, operation);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("SUBSTRING with variable-backed Field of LONG type is accepted")
  void substringWithLongVariableFieldIsAccepted() throws Exception {
    Variable var = Variable.builder().id("startIdx").type("LONG").value("5").build();
    Query query = queryWithVariable(var);

    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Field beginField = (Field) new Field().setVariable("startIdx");

    Operation operation = buildSubstringOperation(sourceField, beginField);

    Map<String, QueryParameter> variablesMap = Map.of(
      "startIdx", queryParameter("5", ParameterType.LONG));

    StubSQLQueryBuilder builder = builderWithQuery(query);
    builder.setVariables(variablesMap);

    Expression<?> result = invokeGetOperationExpression(builder, operation);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("SUBSTRING index Field with no variable reference (column path) is accepted (no variable check)")
  void substringFieldWithoutVariableReferenceIsAllowedThrough() throws Exception {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // A field pointing to a DB column — no variable attribute
    Field beginField = Field.builder().id("startCol").table("myTable").build();

    Operation operation = buildSubstringOperation(sourceField, beginField);

    // Column fields go through buildPath — no variables map needed
    Expression<?> result = invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("SUBSTRING with unresolved variable (not in query definitions) is allowed through")
  void substringWithUnresolvedVariableIsAllowedThrough() throws Exception {
    // Query has NO variable definitions — simulates runtime parameter
    Query query = emptyQuery();

    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Field beginField = (Field) new Field().setVariable("runtimeParam");

    Operation operation = buildSubstringOperation(sourceField, beginField);

    // validateFieldIntegerOperand returns early when varDef == null.
    // After validation, getVariableExpression is called — provide a variables map
    // so it does not NPE. The variable value can be null (returns null expression).
    StubSQLQueryBuilder builder = builderWithQuery(query);
    builder.setVariables(Map.of("runtimeParam", queryParameter("7", ParameterType.INTEGER)));

    Expression<?> result = invokeGetOperationExpression(builder, operation);
    assertThat(result).isNotNull();
  }

  // -------------------------------------------------------------------------
  // Error paths — validation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("SUBSTRING with 1 operand (only source) throws AWException — invalid count")
  void substringWithOneOperandThrowsAWException() {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Operation operation = buildSubstringOperation(sourceField);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation));
  }

  @Test
  @DisplayName("SUBSTRING with 4 operands throws AWException — invalid count")
  void substringWithFourOperandsThrowsAWException() {
    Field source = Field.builder().id("myCol").table("myTable").build();
    Constant idx1 = (Constant) new Constant().setType("INTEGER").setValue("1");
    Constant idx2 = (Constant) new Constant().setType("INTEGER").setValue("5");
    Constant extra = (Constant) new Constant().setType("INTEGER").setValue("9");
    Operation operation = buildSubstringOperation(source, idx1, idx2, extra);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation));
  }

  @Test
  @DisplayName("SUBSTRING with STRING constant as beginIndex throws AWException — invalid type")
  void substringWithStringConstantBeginIndexThrowsAWException() {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // No type set → defaults to STRING
    Constant badIndex = new Constant().setValue("abc");

    Operation operation = buildSubstringOperation(sourceField, badIndex);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation));
  }

  @Test
  @DisplayName("SUBSTRING with FLOAT constant as endIndex throws AWException — invalid type")
  void substringWithFloatConstantEndIndexThrowsAWException() {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Constant goodBegin = (Constant) new Constant().setType("INTEGER").setValue("1");
    Constant badEnd = (Constant) new Constant().setType("FLOAT").setValue("3.5");

    Operation operation = buildSubstringOperation(sourceField, goodBegin, badEnd);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), operation));
  }

  @Test
  @DisplayName("SUBSTRING with STRING variable as index throws AWException — invalid variable type")
  void substringWithStringVariableIndexThrowsAWException() {
    Variable var = Variable.builder().id("badIdx").type("STRING").value("foo").build();
    Query query = queryWithVariable(var);

    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    Field badField = (Field) new Field().setVariable("badIdx");

    Operation operation = buildSubstringOperation(sourceField, badField);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(query), operation));
  }

  @Test
  @DisplayName("SUBSTRING with Operation (non-Constant, non-Field) as index throws AWException — uses getSimpleName in message")
  void substringWithNonConstantNonFieldOperandThrowsAWExceptionUsingSimpleName() {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // Operation is a SqlField subclass that is neither Constant nor Field.
    // This exercises the else-branch in validateIntegerCompatibleOperand that calls
    // operand.getClass().getSimpleName() for the error message.
    Operation nestedOp = new Operation();
    nestedOp.setOperator("ADD");

    Operation outerOp = buildSubstringOperation(sourceField, nestedOp);

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), outerOp));
  }

  @Test
  @DisplayName("SUBSTRING with null as index operand throws AWException — uses 'null' literal in message")
  void substringWithNullIndexOperandThrowsAWException() {
    Field sourceField = Field.builder().id("myCol").table("myTable").build();
    // Build the operand list manually because List.of() rejects null elements.
    // This exercises the else-branch where operand == null → "null" string is used.
    Operation op = new Operation();
    op.setOperator("SUBSTRING");
    op.setOperandList(Arrays.asList(sourceField, null));

    assertThrows(AWException.class,
      () -> invokeGetOperationExpression(builderWithQuery(emptyQuery()), op));
  }

  // -------------------------------------------------------------------------
  // generateOperationExpression — direct dispatch verification
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("generateOperationExpression SUBSTRING 2-arg path returns SUBSTR_1ARG expression")
  void generateOperationExpressionSubstringTwoArgReturnsCorrectOp() {
    Expression<?> src = Expressions.path(Object.class, "src");
    Expression<?> begin = Expressions.constant(3);

    Expression<?> result = builderWithQuery(emptyQuery())
      .generateOperationExpression(substringOp(), src, begin);

    assertThat(result).isNotNull();
    assertThat(result.toString()).containsIgnoringCase("substr");
  }

  @Test
  @DisplayName("generateOperationExpression SUBSTRING 3-arg path returns SUBSTR_2ARGS expression")
  void generateOperationExpressionSubstringThreeArgReturnsCorrectOp() {
    Expression<?> src = Expressions.path(Object.class, "src");
    Expression<?> begin = Expressions.constant(2);
    Expression<?> end = Expressions.constant(7);

    Expression<?> result = builderWithQuery(emptyQuery())
      .generateOperationExpression(substringOp(), src, begin, end);

    assertThat(result).isNotNull();
    assertThat(result.toString()).containsIgnoringCase("substr");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /** Build a SUBSTRING Operation with the given operands. */
  @SafeVarargs
  private <T extends SqlField> Operation buildSubstringOperation(T... operands) {
    Operation op = new Operation();
    op.setOperator("SUBSTRING");
    op.setOperandList(List.of(operands));
    return op;
  }

  private Operation substringOp() {
    Operation op = new Operation();
    op.setOperator("SUBSTRING");
    return op;
  }

  private Query emptyQuery() {
    return new Query();
  }

  private Query queryWithVariable(Variable variable) {
    Query query = new Query();
    query.setVariableDefinitionList(List.of(variable));
    return query;
  }

  /**
   * Build a {@link QueryParameter} carrying a scalar text value with the given type.
   * Used to populate the {@code variables} map so that expression-building code
   * (post-validation) does not NPE when it tries to look up the variable value.
   */
  private QueryParameter queryParameter(String textValue, ParameterType type) {
    return new QueryParameter(JsonNodeFactory.instance.textNode(textValue), false, type);
  }

  /**
   * Build a minimal {@link StubSQLQueryBuilder} with a fixed {@link Query} already set.
   * The stub overrides {@link SQLBuilder#getElements()} with a Mockito mock so that
   * {@code getLocale()} calls return {@code null} (acceptable for AWException title/message).
   */
  private StubSQLQueryBuilder builderWithQuery(Query query) {
    StubSQLQueryBuilder builder = new StubSQLQueryBuilder(queryUtil, encodeService);
    builder.setQuery(query);
    builder.setFactory(factory);
    return builder;
  }

  /**
   * Reflectively invoke the package-visible {@code getOperationExpression} on the builder.
   * This method is {@code protected} — accessible from the same package, but we use
   * reflection here for consistency with the pattern used in {@link SQLMaintainBuilderTest}.
   */
  private Expression<?> invokeGetOperationExpression(StubSQLQueryBuilder builder, Operation operation)
    throws AWException {
    try {
      Method method = SQLBuilder.class.getDeclaredMethod("getOperationExpression", Operation.class);
      method.setAccessible(true);
      return (Expression<?>) method.invoke(builder, operation);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof AWException awException) {
        throw awException;
      }
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new AssertionError("Unexpected reflection failure", cause);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Reflection setup failed", e);
    }
  }

  // -------------------------------------------------------------------------
  // Inner stub subclass
  // -------------------------------------------------------------------------

  /**
   * Minimal concrete extension of {@link SQLQueryBuilder} that replaces Spring-managed
   * infrastructure with safe test stubs.  Only {@code getElements()} is overridden so
   * that {@code getLocale()} does not blow up without an application context.
   */
  private static class StubSQLQueryBuilder extends SQLQueryBuilder {

    private final AweElements aweElements = mock(AweElements.class);

    StubSQLQueryBuilder(QueryUtil queryUtil, EncodeService encodeService) {
      super(queryUtil, encodeService);
      // Locale calls return the key — AWException(key, key) is fine for these tests.
      // Use lenient() so Mockito strict mode does not complain about tests that
      // never trigger a locale lookup (happy paths that never throw).
      lenient().when(aweElements.getLocaleWithLanguage(org.mockito.ArgumentMatchers.anyString(),
          org.mockito.ArgumentMatchers.isNull()))
        .thenAnswer(inv -> inv.getArgument(0));
      lenient().when(aweElements.getLocaleWithLanguage(org.mockito.ArgumentMatchers.anyString(),
          org.mockito.ArgumentMatchers.isNull(),
          org.mockito.ArgumentMatchers.any(Object[].class)))
        .thenAnswer(inv -> inv.getArgument(0));
    }

    @Override
    public AweElements getElements() {
      return aweElements;
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      throw new UnsupportedOperationException("Subquery execution not supported in unit tests");
    }
  }
}
