package com.almis.awe.service.data.builder;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.PasswordEncoderType;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Operation;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authentication.encoder.AwePasswordEncoder;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.sql.Clob;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Characterization of how {@link SQLBuilder} turns variable values into QueryDSL expressions, pinned before
 * splitting the class (#782): the serialized SQL and the runtime class of every bound constant, which is what
 * the JDBC driver receives. The date operations are pinned too, since their expression type is
 * {@code java.util.Date}.
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderVariableCharacterizationTest {

  private static final Configuration CONFIGURATION = new Configuration(new HSQLDBTemplates());
  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

  @Mock
  private EncodeService encodeService;

  private StubBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new StubBuilder(new QueryUtil(null, null, null, null), encodeService);
    builder.setQuery(new Query());
  }

  static Stream<Arguments> typedValues() {
    return Stream.of(
      Arguments.of(ParameterType.STRING, "abc", "?", String.class, "abc"),
      Arguments.of(ParameterType.STRINGL, "abc", "?", String.class, "%abc"),
      Arguments.of(ParameterType.STRINGR, "abc", "?", String.class, "abc%"),
      Arguments.of(ParameterType.STRINGB, "abc", "?", String.class, "%abc%"),
      Arguments.of(ParameterType.STRINGN, "abc", "?", String.class, "abc"),
      Arguments.of(ParameterType.TIME, "10:20:30", "?", String.class, "10:20:30"),
      Arguments.of(ParameterType.OBJECT, "{\"a\":1}", "?", String.class, "{\"a\":1}"),
      Arguments.of(ParameterType.DATE, "15/10/2026", "?", java.sql.Date.class, java.sql.Date.valueOf("2026-10-15")),
      Arguments.of(ParameterType.TIMESTAMP, "15/10/2026 10:20:30", "?", Timestamp.class,
        Timestamp.valueOf("2026-10-15 10:20:30")),
      Arguments.of(ParameterType.FLOAT, "1.5", "?", Float.class, 1.5f),
      Arguments.of(ParameterType.DOUBLE, "1.5", "?", Double.class, 1.5d),
      Arguments.of(ParameterType.LONG, "7", "?", Long.class, 7L),
      Arguments.of(ParameterType.SEQUENCE, "7", "?", Long.class, 7L),
      Arguments.of(ParameterType.MULTIPLE_SEQUENCE, "7", "?", Long.class, 7L),
      Arguments.of(ParameterType.INTEGER, "7", "?", Integer.class, 7),
      Arguments.of(ParameterType.BOOLEAN, "true", "?", Boolean.class, true)
    );
  }

  @ParameterizedTest(name = "{0} {1} binds a {3}")
  @MethodSource("typedValues")
  @DisplayName("A typed value is bound as a constant of a fixed runtime class")
  void typedValueIsBoundAsConstant(ParameterType type, String value, String sql, Class<?> boundClass,
                                   Object boundValue) throws AWException {
    Expression<?> expression = builder.getVariableAsExpression(value, type);

    assertThat(sql(expression)).isEqualTo(sql);
    assertThat(constants(expression)).singleElement().isInstanceOf(boundClass).isEqualTo(boundValue);
    assertThat(constants(expression).get(0).getClass()).isEqualTo(boundClass);
  }

  static Stream<Arguments> emptyValues() {
    return Stream.of(
      Arguments.of(ParameterType.STRING, "?", ""),
      Arguments.of(ParameterType.STRING_HASH_SHA, "?", ""),
      Arguments.of(ParameterType.STRING_ENCRYPT, "?", ""),
      Arguments.of(ParameterType.STRING_HASH_RIPEMD160, "?", ""),
      Arguments.of(ParameterType.STRING_HASH_PASSWORD, "?", ""),
      Arguments.of(ParameterType.STRING_HASH_PBKDF_2_W_HMAC_SHA_1, "?", ""),
      Arguments.of(ParameterType.STRINGL, "?", "%"),
      Arguments.of(ParameterType.STRINGR, "?", "%"),
      Arguments.of(ParameterType.STRINGB, "?", "%%"),
      Arguments.of(ParameterType.STRINGN, "null", null),
      Arguments.of(ParameterType.DATE, "null", null),
      Arguments.of(ParameterType.TIMESTAMP, "null", null),
      Arguments.of(ParameterType.INTEGER, "null", null),
      Arguments.of(ParameterType.LONG, "null", null),
      Arguments.of(ParameterType.FLOAT, "null", null),
      Arguments.of(ParameterType.DOUBLE, "null", null),
      Arguments.of(ParameterType.BOOLEAN, "null", null),
      Arguments.of(ParameterType.NULL, "null", null)
    );
  }

  @ParameterizedTest(name = "empty {0} renders {1}")
  @MethodSource("emptyValues")
  @DisplayName("An empty value is an empty string, a like wildcard or a null expression depending on the type")
  void emptyValue(ParameterType type, String sql, String boundValue) throws AWException {
    for (String value : new String[]{"", null}) {
      Expression<?> expression = builder.getVariableAsExpression(value, type);

      assertThat(sql(expression)).as("value %s", value).isEqualTo(sql);
      if (boundValue == null) {
        assertThat(constants(expression)).isEmpty();
      } else {
        assertThat(constants(expression)).containsExactly(boundValue);
      }
    }
    verifyNoInteractions(encodeService);
  }

  @Test
  @DisplayName("A NULL typed variable is a null expression even with a value")
  void nullTypeIsNullExpression() throws AWException {
    assertThat(sql(builder.getVariableAsExpression("abc", ParameterType.NULL))).isEqualTo("null");
  }

  @Test
  @DisplayName("A CLOB value is a Clob template rendering the value as SQL text")
  void clobValueIsTemplate() throws AWException {
    Expression<?> expression = builder.getVariableAsExpression("abc", ParameterType.CLOB);

    assertThat(expression.getType()).isEqualTo(Clob.class);
    assertThat(expression).hasToString("abc");
    assertThat(sql(expression)).isEqualTo("?");
    assertThat(constants(expression)).singleElement().isInstanceOf(String.class).isEqualTo("abc");
  }

  @Test
  @DisplayName("SYSTEM_DATE and SYSTEM_TIMESTAMP are database functions, not bound values")
  void systemDateAndTimestampAreDatabaseFunctions() throws AWException {
    Expression<?> date = builder.getVariableAsExpression("ignored", ParameterType.SYSTEM_DATE);
    Expression<?> timestamp = builder.getVariableAsExpression("ignored", ParameterType.SYSTEM_TIMESTAMP);

    assertThat(sql(date)).isEqualTo("current_date");
    assertThat(constants(date)).isEmpty();
    assertThat(sql(timestamp)).isEqualTo("current_timestamp");
    assertThat(constants(timestamp)).isEmpty();
  }

  @Test
  @DisplayName("SYSTEM_TIME is the current application time bound as a HH:mm:ss string")
  void systemTimeIsBoundAsWebTimeString() throws AWException {
    Expression<?> expression = builder.getVariableAsExpression("ignored", ParameterType.SYSTEM_TIME);

    assertThat(sql(expression)).isEqualTo("?");
    assertThat(constants(expression)).singleElement().isInstanceOf(String.class)
      .asString().matches("\\d{2}:\\d{2}:\\d{2}");
  }

  @Test
  @DisplayName("An invalid date fails with the parser exception")
  void invalidDateFails() {
    assertThatThrownBy(() -> builder.getVariableAsExpression("2026-10-15", ParameterType.DATE))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("An invalid timestamp fails building the constant")
  void invalidTimestampFails() {
    assertThatThrownBy(() -> builder.getVariableAsExpression("2026-10-15", ParameterType.TIMESTAMP))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("An invalid number fails with NumberFormatException")
  void invalidNumberFails() {
    assertThatThrownBy(() -> builder.getVariableAsExpression("abc", ParameterType.INTEGER))
      .isInstanceOf(NumberFormatException.class);
  }

  @Test
  @DisplayName("Hashed and encrypted strings bind the encoded value")
  void hashedStringsBindEncodedValue() throws AWException {
    when(encodeService.hash(EncodeService.HashingAlgorithms.SHA_256, "abc")).thenReturn("sha");
    when(encodeService.encodePBKDF2WithHmacSHA1("abc")).thenReturn("pbkdf2");
    when(encodeService.encryptRipEmd160("abc")).thenReturn("encrypted");

    assertThat(constants(builder.getVariableAsExpression("abc", ParameterType.STRING_HASH_SHA)))
      .containsExactly("sha");
    assertThat(constants(builder.getVariableAsExpression("abc", ParameterType.STRING_HASH_PBKDF_2_W_HMAC_SHA_1)))
      .containsExactly("pbkdf2");
    assertThat(constants(builder.getVariableAsExpression("abc", ParameterType.STRING_ENCRYPT)))
      .containsExactly("encrypted");
    assertThat(constants(builder.getVariableAsExpression("abc", ParameterType.STRING_HASH_RIPEMD160)))
      .containsExactly(EncodeService.encodeRipEmd160("abc"));
  }

  @Test
  @DisplayName("A password binds the hash of the configured password encoder")
  void passwordBindsConfiguredEncoderHash() throws AWException {
    SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
    StubBuilder passwordBuilder = new StubBuilder(new QueryUtil(null, null, null, null),
      new EncodeService(new BaseConfigProperties(), securityConfigProperties));
    passwordBuilder.setQuery(new Query());

    Expression<?> expression = passwordBuilder.getVariableAsExpression("abc", ParameterType.STRING_HASH_PASSWORD);

    assertThat(sql(expression)).isEqualTo("?");
    assertThat(constants(expression)).singleElement().isInstanceOf(String.class).asString()
      .startsWith("{bcrypt}")
      .matches(hash -> new AwePasswordEncoder(PasswordEncoderType.BCRYPT).matches("abc", hash), "matches the password");
  }

  @Test
  @DisplayName("A password binds the legacy RIPEMD-160 hash when the encoder is ripemd160")
  void passwordBindsLegacyHashWithRipemd160Encoder() throws AWException {
    SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
    securityConfigProperties.setPasswordEncoder(PasswordEncoderType.RIPEMD160);
    StubBuilder passwordBuilder = new StubBuilder(new QueryUtil(null, null, null, null),
      new EncodeService(new BaseConfigProperties(), securityConfigProperties));
    passwordBuilder.setQuery(new Query());

    assertThat(constants(passwordBuilder.getVariableAsExpression("abc", ParameterType.STRING_HASH_PASSWORD)))
      .containsExactly(EncodeService.encodeRipEmd160("abc"));
  }

  @Test
  @DisplayName("A null JSON value is an empty string for STRING and a null expression for other types")
  void nullJsonValue() throws AWException {
    assertThat(constants(builder.getVariableAsExpression((JsonNode) null, ParameterType.STRING))).containsExactly("");
    assertThat(constants(builder.getVariableAsExpression(NODES.nullNode(), ParameterType.STRING)))
      .containsExactly("");
    assertThat(sql(builder.getVariableAsExpression(NODES.nullNode(), ParameterType.INTEGER))).isEqualTo("null");
    assertThat(sql(builder.getVariableAsExpression(NODES.nullNode(), ParameterType.STRINGB)))
      .isEqualTo("?");
  }

  @Test
  @DisplayName("A JSON number is converted through its text")
  void jsonNumberIsConvertedThroughItsText() throws AWException {
    assertThat(constants(builder.getVariableAsExpression(NODES.numberNode(7), ParameterType.LONG)))
      .containsExactly(7L);
    assertThat(constants(builder.getVariableAsExpression(NODES.textNode("7"), ParameterType.INTEGER)))
      .containsExactly(7);
  }

  @Test
  @DisplayName("LIST_TO_STRING joins the non-empty values of a list with commas")
  void listToStringJoinsNonEmptyValues() throws AWException {
    ArrayNode list = NODES.arrayNode().add("a").add("").add(NODES.nullNode()).add(3).add("b");

    Expression<?> expression = builder.getVariableAsExpression(list, ParameterType.LIST_TO_STRING);

    assertThat(sql(expression)).isEqualTo("?");
    assertThat(constants(expression)).containsExactly("a,3,b");
  }

  @Test
  @DisplayName("LIST_TO_STRING over a scalar binds the scalar text")
  void listToStringOverScalar() throws AWException {
    assertThat(constants(builder.getVariableAsExpression(NODES.textNode("x"), ParameterType.LIST_TO_STRING)))
      .containsExactly("x");
  }

  @Test
  @DisplayName("LIST_TO_STRING over an empty list, empty text or null is a null expression")
  void listToStringOverEmptyValues() throws AWException {
    assertThat(sql(builder.getVariableAsExpression(NODES.arrayNode(), ParameterType.LIST_TO_STRING)))
      .isEqualTo("null");
    assertThat(sql(builder.getVariableAsExpression(NODES.arrayNode().add(""), ParameterType.LIST_TO_STRING)))
      .isEqualTo("null");
    assertThat(sql(builder.getVariableAsExpression(NODES.textNode(""), ParameterType.LIST_TO_STRING)))
      .isEqualTo("null");
    assertThat(sql(builder.getVariableAsExpression((JsonNode) null, ParameterType.LIST_TO_STRING)))
      .isEqualTo("null");
  }

  @Test
  @DisplayName("A constant without type is a STRING constant")
  void constantWithoutTypeIsString() throws AWException {
    assertThat(constants(builder.getConstantExpression(new Constant().setValue("5")))).containsExactly("5");
    assertThat(constants(builder.getConstantExpression((Constant) new Constant().setType("INTEGER").setValue("5"))))
      .containsExactly(5);
  }

  @Test
  @DisplayName("A query variable is converted with its declared type")
  void queryVariableUsesDeclaredType() throws AWException {
    builder.setQuery(queryWith(Variable.builder().id("var").type("DATE").build()));
    builder.setVariables(variables("var", new QueryParameter(NODES.textNode("15/10/2026"), false, ParameterType.DATE)));

    Expression<?> expression = builder.getVariableExpression("var");

    assertThat(constants(expression)).singleElement().isInstanceOf(java.sql.Date.class)
      .isEqualTo(java.sql.Date.valueOf("2026-10-15"));
  }

  @Test
  @DisplayName("An unknown query variable is a null expression reference")
  void unknownQueryVariableIsNull() throws AWException {
    assertThat(builder.getVariableExpression("missing")).isNull();
  }

  @Test
  @DisplayName("A list query variable used as a field fails")
  void listQueryVariableFails() {
    builder.setQuery(queryWith(Variable.builder().id("var").type("STRING").build()));
    builder.setVariables(variables("var",
      new QueryParameter(NODES.arrayNode().add("a").add("b"), true, ParameterType.STRING)));

    assertThatThrownBy(() -> builder.getVariableExpression("var"))
      .isInstanceOf(AWException.class)
      .hasMessageContaining("ERROR_MESSAGE_MALFORMED_QUERY_LIST_FIELD");
  }

  static Stream<Arguments> dateOperations() {
    return Stream.of(
      Arguments.of("ADD_DAYS", "dateadd('dd', 2, t.d)", java.util.Date.class),
      Arguments.of("ADD_MONTHS", "dateadd('mm', 2, t.d)", java.util.Date.class),
      Arguments.of("ADD_SECONDS", "dateadd('ss', 2, t.d)", java.util.Date.class),
      Arguments.of("SUB_DAYS", "dateadd('dd', ? * ?, t.d)", java.util.Date.class),
      Arguments.of("SUB_YEARS", "dateadd('yy', ? * ?, t.d)", java.util.Date.class),
      Arguments.of("DIFF_DAYS", "datediff('dd', t.d, ?)", Integer.class)
    );
  }

  @ParameterizedTest(name = "{0} renders {1}")
  @MethodSource("dateOperations")
  @DisplayName("Date operations keep their SQL and their QueryDSL expression type")
  void dateOperationsKeepSqlAndType(String operator, String sql, Class<?> type) {
    Operation operation = new Operation();
    operation.setOperator(operator);

    Expression<?> expression = builder.generateOperationExpression(operation,
      builder.buildPath("t", "d"), Expressions.constant(2));

    assertThat(sql(expression)).isEqualTo(sql);
    assertThat(expression.getType()).isEqualTo(type);
  }

  @Test
  @DisplayName("TRUNCDATE keeps its SQL and its java.util.Date expression type")
  void truncDateKeepsSqlAndType() throws Exception {
    Expression<?> expression = builder.getExpressionFunction(builder.buildPath("t", "d"), "TRUNCDATE", "d");

    assertThat(sql(expression)).isEqualTo("convert(t.d, date)");
    assertThat(expression.getType()).isEqualTo(java.util.Date.class);
  }

  private static String sql(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.toString();
  }

  private static List<Object> constants(Expression<?> expression) {
    SQLSerializer serializer = new SQLSerializer(CONFIGURATION);
    serializer.handle(expression);
    return serializer.getConstants();
  }

  private static Query queryWith(Variable variable) {
    Query query = new Query();
    query.setVariableDefinitionList(List.of(variable));
    return query;
  }

  private static Map<String, QueryParameter> variables(String name, QueryParameter parameter) {
    Map<String, QueryParameter> variables = new HashMap<>();
    variables.put(name, parameter);
    return variables;
  }

  /**
   * Concrete builder whose localization answers the key
   */
  private static class StubBuilder extends SQLQueryBuilder {

    private final AweElements aweElements = mock(AweElements.class);

    StubBuilder(QueryUtil queryUtil, EncodeService encodeService) {
      super(queryUtil, encodeService);
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull()))
        .thenAnswer(invocation -> invocation.getArgument(0));
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Override
    public AweElements getElements() {
      return aweElements;
    }

    @Override
    protected SQLQuery<Tuple> getSubquery(String queryId) {
      throw new UnsupportedOperationException("Subqueries are not used in these tests");
    }
  }
}
