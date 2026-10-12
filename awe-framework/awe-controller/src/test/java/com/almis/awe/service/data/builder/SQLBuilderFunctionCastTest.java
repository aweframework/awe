package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.OrderBy;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.type.TotalizeFunctionType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.SQLQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * The functions and casts a query accepts must be the ones the engine supports: every value the schema
 * enumerates works, and any other value fails naming the query, the field and the value.
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderFunctionCastTest {

  private static final String QUERY_ID = "myQuery";
  private static final String FIELD_ID = "myField";
  private static final String FUNCTION_MESSAGE = "ERROR_MESSAGE_UNSUPPORTED_FUNCTION";
  private static final String CAST_MESSAGE = "ERROR_MESSAGE_UNSUPPORTED_CAST";
  private static final Path QUERY_XSD = Path.of(System.getProperty("basedir", System.getProperty("user.dir")), "..", "awe-generic-screens", "src", "main", "resources", "schemas",
    "awe", "query.xsd");

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private EncodeService encodeService;

  private StubBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new StubBuilder(queryUtil, encodeService);
    Query query = new Query();
    query.setId(QUERY_ID);
    builder.setQuery(query);
  }

  static Stream<String> schemaFunctions() throws Exception {
    return Stream.concat(schemaValues("aggregationFunctions").stream(), schemaValues("fieldFunctions").stream());
  }

  static Stream<String> schemaCasts() throws Exception {
    return schemaValues("castType").stream();
  }

  @ParameterizedTest(name = "function {0} is built")
  @MethodSource("schemaFunctions")
  @DisplayName("Every function the schema enumerates is built by the engine")
  void everySchemaFunctionIsBuilt(String function) {
    assertThatCode(() -> builder.getExpressionFunction(column(), function, FIELD_ID))
      .doesNotThrowAnyException();
  }

  @ParameterizedTest(name = "cast {0} is built")
  @MethodSource("schemaCasts")
  @DisplayName("Every cast the schema enumerates is built by the engine")
  void everySchemaCastIsBuilt(String cast) {
    assertThatCode(() -> builder.getExpressionCast(column(), cast, FIELD_ID))
      .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Every aggregation function of the schema is a totalize function of the engine")
  void aggregationFunctionsMatchTotalizeFunctions() throws Exception {
    List<String> totalize = Arrays.stream(TotalizeFunctionType.values()).map(Enum::name).toList();

    assertThat(schemaValues("aggregationFunctions")).containsExactlyInAnyOrderElementsOf(totalize);
  }

  @Test
  @DisplayName("LENGTH is a field function and not a totalize function")
  void lengthIsAFieldFunctionOnly() throws Exception {
    assertThat(schemaValues("fieldFunctions")).contains("LENGTH");
    assertThat(schemaValues("aggregationFunctions")).doesNotContain("LENGTH");
  }

  @Test
  @DisplayName("An unknown function fails naming the query, the field and the value")
  void unknownFunctionFails() {
    Expression<?> column = column();

    assertThatThrownBy(() -> builder.getExpressionFunction(column, "COUNT", FIELD_ID))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining(FUNCTION_MESSAGE)
      .hasMessageContaining(QUERY_ID)
      .hasMessageContaining(FIELD_ID)
      .hasMessageContaining("COUNT")
      .extracting(e -> ((AWEQueryException) e).getQuery()).isEqualTo(QUERY_ID);
  }

  @Test
  @DisplayName("An unknown cast fails naming the query, the field and the value")
  void unknownCastFails() {
    Expression<?> column = column();

    assertThatThrownBy(() -> builder.getExpressionCast(column, "DECIMAL", FIELD_ID))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining(CAST_MESSAGE)
      .hasMessageContaining(QUERY_ID)
      .hasMessageContaining(FIELD_ID)
      .hasMessageContaining("DECIMAL");
  }

  @ParameterizedTest(name = "function {0} is case insensitive")
  @ValueSource(strings = {"sum", "Cnt"})
  @DisplayName("Function names are matched ignoring case")
  void functionIsCaseInsensitive(String function) {
    assertThatCode(() -> builder.getExpressionFunction(column(), function, FIELD_ID))
      .doesNotThrowAnyException();
  }

  @ParameterizedTest(name = "lowercase {0} is matched under a Turkish default locale")
  @ValueSource(strings = {"minute", "cnt_distinct", "string", "integer"})
  @DisplayName("Function and cast names are matched independently of the default locale")
  void matchingIgnoresTheDefaultLocale(String name) {
    Locale previous = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
      boolean isCast = "string".equals(name) || "integer".equals(name);
      assertThatCode(() -> {
        if (isCast) {
          builder.getExpressionCast(column(), name, FIELD_ID);
        } else {
          builder.getExpressionFunction(column(), name, FIELD_ID);
        }
      }).doesNotThrowAnyException();
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  @DisplayName("An unknown function in an order-by fails naming the order-by field")
  void unknownOrderByFunctionFails() {
    OrderBy orderBy = new OrderBy().setField("sortField").setFunction("COUNT");

    assertThatThrownBy(() -> builder.getOrderByExpression(orderBy))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("sortField")
      .hasMessageContaining("COUNT");
  }

  @Test
  @DisplayName("An unknown function in a field fails naming the field identifier")
  void unknownFieldFunctionFails() {
    Field field = Field.builder().id("col").table("tab").build();
    field.setFunction("COUNT").setAlias("alias");

    assertThatThrownBy(() -> builder.getOperandExpression(field))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("alias")
      .hasMessageContaining("COUNT");
  }

  @Test
  @DisplayName("An empty function or cast means none, as when they are not set")
  void emptyFunctionAndCastMeanNone() {
    Field field = Field.builder().id("col").table("tab").build();
    field.setFunction("").setCast("");

    assertThatCode(() -> builder.getOperandExpression(field)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("An unknown cast in a field without alias fails naming the field id")
  void unknownFieldCastFails() {
    Field field = Field.builder().id("col").table("tab").build();
    field.setCast("DECIMAL");

    assertThatThrownBy(() -> builder.getOperandExpression(field))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("col")
      .hasMessageContaining("DECIMAL");
  }

  private Expression<?> column() {
    return builder.buildPath("myTable", "myColumn");
  }

  private static List<String> schemaValues(String simpleType) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Document document = factory.newDocumentBuilder().parse(QUERY_XSD.toFile());
    NodeList types = document.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
    List<String> values = new ArrayList<>();
    for (int i = 0; i < types.getLength(); i++) {
      Element type = (Element) types.item(i);
      if (simpleType.equals(type.getAttribute("name"))) {
        NodeList enumerations = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
        for (int j = 0; j < enumerations.getLength(); j++) {
          values.add(((Element) enumerations.item(j)).getAttribute("value"));
        }
      }
    }
    assertThat(values).as("values of the schema type " + simpleType).isNotEmpty();
    return values;
  }

  /**
   * Concrete builder whose localization answers the key followed by its arguments
   */
  private static class StubBuilder extends SQLQueryBuilder {

    private final AweElements aweElements = mock(AweElements.class);

    StubBuilder(QueryUtil queryUtil, EncodeService encodeService) {
      super(queryUtil, encodeService);
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull()))
        .thenAnswer(invocation -> invocation.getArgument(0));
      lenient().when(aweElements.getLocaleWithLanguage(anyString(), isNull(), any(Object[].class)))
        .thenAnswer(invocation -> {
          Object[] arguments = invocation.getArguments();
          return invocation.getArgument(0) + " " + Arrays.toString(Arrays.copyOfRange(arguments, 2, arguments.length));
        });
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
