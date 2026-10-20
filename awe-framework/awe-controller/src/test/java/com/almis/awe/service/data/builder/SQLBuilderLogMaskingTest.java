package com.almis.awe.service.data.builder;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.maintain.Delete;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.Update;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Filter;
import com.almis.awe.model.entities.queries.FilterAnd;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Table;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.MaintainBuildOperation;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.querydsl.core.Tuple;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * The SQL logged (and carried by query errors) masks the values of hashed and encrypted variables and the values
 * written to or compared with the sensitive columns, while the statement executed keeps them (#778, S10).
 */
@ExtendWith(MockitoExtension.class)
class SQLBuilderLogMaskingTest {

  private static final String MASK = "'********'";
  private static final String SECRET = "secret";
  private static final String STORED_HASH = "{bcrypt}$2a$10$storedHash";

  @Mock
  private EncodeService encodeService;

  @Mock
  private Connection connection;

  private QueryUtil queryUtil;

  @BeforeEach
  void setUp() throws Exception {
    // Variables are provided by each test: the default ones come from the live request
    queryUtil = new QueryUtil(new BaseConfigProperties(), new DatabaseConfigProperties(), new JsonMapper(), null) {
      @Override
      public void addToVariableMap(Map<String, QueryParameter> variableMap, Query query, ObjectNode parameters) {
        // Variables are provided by the test fixture
      }
    };
    lenient().when(encodeService.encodePassword(SECRET)).thenReturn("{bcrypt}$2a$10$passwordHash");
    lenient().when(encodeService.hash(EncodeService.HashingAlgorithms.SHA_256, SECRET)).thenReturn("sha-hash");
    lenient().when(encodeService.encodePBKDF2WithHmacSHA1(SECRET)).thenReturn("pbkdf2-hash");
    lenient().when(encodeService.encryptRipEmd160(SECRET)).thenReturn("encrypted-value");
  }

  @ParameterizedTest
  @EnumSource(value = ParameterType.class, names = {"STRING_HASH_RIPEMD160", "STRING_HASH_SHA",
    "STRING_HASH_PBKDF_2_W_HMAC_SHA_1", "STRING_HASH_PASSWORD", "STRING_ENCRYPT"})
  @DisplayName("The value of a hashed or encrypted variable is masked in the logged SQL, whatever its column")
  void hashedVariableIsMasked(ParameterType type) throws Exception {
    Insert insert = maintain(new Insert(), List.of(field("Nam", "name"), field("Pwd", "password")));
    Map<String, QueryParameter> variables = variables(insert, "name", ParameterType.STRING, "sunset", "password", type, SECRET);

    MaintainBuilder builder = maintainBuilder(insert, variables);
    SQLBindings bindings = builder.build().getSQL().get(0);
    String logged = logged(bindings, builder);

    String hashed = hashOf(type);
    assertThat(bindings.getNullFriendlyBindings()).containsExactly("sunset", hashed);
    assertThat(logged).isEqualTo("insert into AweThm (Nam, Pwd)\nvalues ('sunset', " + MASK + ")");
    assertThat(logged).doesNotContain(hashed, SECRET);
  }

  @Test
  @DisplayName("A STRING value inserted into the password column is masked, the other values are not")
  void stringInsertedIntoPasswordColumnIsMasked() throws Exception {
    Insert insert = maintain(new Insert(), List.of(field("l1_nom", "user"), field("L1_PAS", "password")));
    Map<String, QueryParameter> variables = variables(insert, "user", ParameterType.STRING, "pvidal", "password", ParameterType.STRING, STORED_HASH);

    MaintainBuilder builder = maintainBuilder(insert, variables);
    SQLBindings bindings = builder.build().getSQL().get(0);

    assertThat(bindings.getNullFriendlyBindings()).containsExactly("pvidal", STORED_HASH);
    assertThat(logged(bindings, builder)).isEqualTo("insert into AweThm (l1_nom, L1_PAS)\nvalues ('pvidal', " + MASK + ")");
  }

  @Test
  @DisplayName("A STRING value set into the password column is masked, the filter on another column is not")
  void stringUpdatedIntoPasswordColumnIsMasked() throws Exception {
    Update update = maintain(new Update(), List.of(field("l1_pas", "password")));
    update.setFilterGroup(and(filter("l1_nom", "user")));
    Map<String, QueryParameter> variables = variables(update, "user", ParameterType.STRING, "pvidal", "password", ParameterType.STRING, STORED_HASH);

    MaintainBuilder builder = maintainBuilder(update, variables);
    SQLBindings bindings = builder.build().getSQL().get(0);

    assertThat(bindings.getNullFriendlyBindings()).containsExactly(STORED_HASH, "pvidal");
    assertThat(logged(bindings, builder)).isEqualTo("update AweThm\nset l1_pas = " + MASK + "\nwhere l1_nom = 'pvidal'");
  }

  @Test
  @DisplayName("A STRING value compared with the password column in a maintain filter is masked")
  void stringComparedWithPasswordColumnInMaintainIsMasked() throws Exception {
    Delete delete = maintain(new Delete(), new ArrayList<>());
    delete.setFilterGroup(and(filter("l1_nom", "user"), filter("l1_pas", "password")));
    Map<String, QueryParameter> variables = variables(delete, "user", ParameterType.STRING, "pvidal", "password", ParameterType.STRING, STORED_HASH);

    MaintainBuilder builder = maintainBuilder(delete, variables);
    SQLBindings bindings = builder.build().getSQL().get(0);

    assertThat(bindings.getNullFriendlyBindings()).containsExactly("pvidal", STORED_HASH);
    assertThat(logged(bindings, builder)).isEqualTo("delete from AweThm\nwhere l1_nom = 'pvidal' and l1_pas = " + MASK);
  }

  @Test
  @DisplayName("A query built with literals masks the value compared with the password column, not the others")
  void stringComparedWithPasswordColumnInQueryIsMasked() throws Exception {
    Query query = new Query();
    query.setId("UsrChk");
    query.setTableList(List.of(Table.builder().id("ope").build()));
    query.setSqlFieldList(new ArrayList<>(List.of(Field.builder().id("IdeOpe").build())));
    query.setFilterGroup(and(filter("l1_nom", "user"), filter("l1_pas", "password")));
    Map<String, QueryParameter> variables = variables(query, "user", ParameterType.STRING, "pvidal", "password", ParameterType.STRING, STORED_HASH);

    Configuration configuration = new Configuration(new HSQLDBTemplates());
    configuration.setUseLiterals(true);
    QueryBuilder builder = new QueryBuilder(queryUtil, encodeService);
    builder.setQuery(query);
    builder.setFactory(new SQLQueryFactory(configuration, () -> connection));
    builder.setVariables(variables);
    builder.setLogMaskedFields("l1_pas");
    SQLQuery<Tuple> built = builder.build();
    SQLBindings bindings = built.getSQL();

    assertThat(bindings.getSQL()).contains("l1_pas = '" + STORED_HASH + "'");
    assertThat(logged(bindings, builder)).isEqualTo("select IdeOpe as IdeOpe\nfrom ope ope\nwhere l1_nom = 'pvidal' and l1_pas = " + MASK);
  }

  @Test
  @DisplayName("awe.database.log-masked-fields replaces the sensitive columns")
  void maskedFieldsCanBeConfigured() throws Exception {
    Insert insert = maintain(new Insert(), List.of(field("l1_pas", "password"), field("ApiKey", "key")));
    Map<String, QueryParameter> variables = variables(insert, "password", ParameterType.STRING, STORED_HASH, "key", ParameterType.STRING, "api-secret");
    DatabaseConfigProperties properties = new DatabaseConfigProperties();
    properties.setLogMaskedFields(new String[]{"apikey"});

    MaintainBuilder builder = maintainBuilder(insert, variables, properties);
    SQLBindings bindings = builder.build().getSQL().get(0);

    assertThat(bindings.getNullFriendlyBindings()).containsExactly(STORED_HASH, "api-secret");
    assertThat(logged(bindings, builder)).isEqualTo("insert into AweThm (l1_pas, ApiKey)\nvalues ('" + STORED_HASH + "', " + MASK + ")");
  }

  // -------------------------------------------------------------------------
  // Fixtures
  // -------------------------------------------------------------------------

  private String hashOf(ParameterType type) {
    return switch (type) {
      case STRING_HASH_RIPEMD160 -> EncodeService.encodeRipEmd160(SECRET);
      case STRING_HASH_SHA -> "sha-hash";
      case STRING_HASH_PBKDF_2_W_HMAC_SHA_1 -> "pbkdf2-hash";
      case STRING_HASH_PASSWORD -> "{bcrypt}$2a$10$passwordHash";
      default -> "encrypted-value";
    };
  }

  private String logged(SQLBindings bindings, SQLBuilder builder) {
    return queryUtil.getFullSQL(bindings.getSQL(), bindings.getNullFriendlyBindings(), builder.getLogMaskedValues());
  }

  private <T extends MaintainQuery> T maintain(T maintain, List<SqlField> fields) {
    maintain.setId("MaskingMaintain");
    maintain.setTableList(List.of(Table.builder().id("AweThm").build()));
    maintain.setSqlFieldList(new ArrayList<>(fields));
    return maintain;
  }

  private static SqlField field(String id, String variable) {
    return Field.builder().id(id).variable(variable).build();
  }

  private static Filter filter(String field, String variable) {
    return new Filter().setLeftField(field).setCondition("eq").setRightVariable(variable);
  }

  private static FilterAnd and(Filter... filters) {
    return (FilterAnd) new FilterAnd().setFilterList(new ArrayList<>(Arrays.asList(filters)));
  }

  private Map<String, QueryParameter> variables(Query query, String firstId, ParameterType firstType, String firstValue,
                                                String secondId, ParameterType secondType, String secondValue) {
    query.setVariableDefinitionList(new ArrayList<>(List.of(
      Variable.builder().id(firstId).type(firstType.name()).build(),
      Variable.builder().id(secondId).type(secondType.name()).build())));
    Map<String, QueryParameter> variables = new LinkedHashMap<>();
    variables.put(firstId, new QueryParameter(JsonNodeFactory.instance.textNode(firstValue), false, firstType));
    variables.put(secondId, new QueryParameter(JsonNodeFactory.instance.textNode(secondValue), false, secondType));
    return variables;
  }

  private MaintainBuilder maintainBuilder(MaintainQuery maintain, Map<String, QueryParameter> variables) {
    return maintainBuilder(maintain, variables, new DatabaseConfigProperties());
  }

  private MaintainBuilder maintainBuilder(MaintainQuery maintain, Map<String, QueryParameter> variables,
                                          DatabaseConfigProperties properties) {
    MaintainBuilder builder = new MaintainBuilder(queryUtil, encodeService, properties);
    builder.setMaintain(maintain)
      .setFactory(new SQLQueryFactory(new Configuration(new HSQLDBTemplates()), () -> connection))
      .setVariables(variables)
      .setParameters(JsonNodeFactory.instance.objectNode())
      .setOperation(MaintainBuildOperation.NO_BATCH);
    return builder;
  }

  private static AweElements elements() {
    AweElements elements = mock(AweElements.class);
    lenient().when(elements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    return elements;
  }

  private static class MaintainBuilder extends SQLMaintainBuilder {
    private final AweElements elements = elements();

    MaintainBuilder(QueryUtil queryUtil, EncodeService encodeService, DatabaseConfigProperties properties) {
      super(queryUtil, encodeService, properties);
    }

    @Override
    public AweElements getElements() {
      return elements;
    }

    @Override
    protected String getUser() {
      return "Anonymous";
    }
  }

  private static class QueryBuilder extends SQLQueryBuilder {
    private final AweElements elements = elements();

    QueryBuilder(QueryUtil queryUtil, EncodeService encodeService) {
      super(queryUtil, encodeService);
    }

    @Override
    public AweElements getElements() {
      return elements;
    }
  }
}
