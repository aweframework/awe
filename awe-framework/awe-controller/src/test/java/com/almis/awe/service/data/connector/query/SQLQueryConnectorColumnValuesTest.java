package com.almis.awe.service.data.connector.query;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.ColumnFilterTestFixtures;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.SQLQueryBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.almis.awe.service.data.ColumnFilterTestFixtures.stubLocales;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Distinct values of a grid column on a SQL query (#912, S9 and S10): which labels and search the database resolves,
 * the limit of the grouped query and the labels computed from the representative values.
 */
@ExtendWith(MockitoExtension.class)
class SQLQueryConnectorColumnValuesTest {

  @Mock
  private AweDatabaseContextHolder contextHolder;

  @Mock
  private DataSource dataSource;

  @Mock
  private AweElements elements;

  @Mock
  private ApplicationContext context;

  @Mock
  private DatabaseConnection databaseConnection;

  @Mock
  private Connection connection;

  @Mock
  private EnumeratedGroup statuses;

  @Mock
  private SQLQuery<Tuple> valuesQuery;

  private SQLQueryBuilder builder;
  private SQLQueryConnector connector;
  private MockedStatic<DataSourceUtils> dataSourceUtils;

  @BeforeEach
  void setUp() throws Exception {
    connector = new SQLQueryConnector(contextHolder, ColumnFilterTestFixtures.queryUtil(elements), dataSource, new BaseConfigProperties(), elements,
      mock(NumericService.class), mock(EncodeService.class), new DatabaseConfigProperties(), mock(ObjectMapper.class));
    connector.setApplicationContext(context);
    builder = mock(SQLQueryBuilder.class, RETURNS_SELF);
    Map<String, QueryParameter> variables = new HashMap<>();
    variables.put(AweConstants.QUERY_MAX, new QueryParameter(JsonNodeFactory.instance.numberNode(0L)));

    when(contextHolder.getDatabaseConnection(dataSource)).thenReturn(databaseConnection);
    when(databaseConnection.getConnectionType()).thenReturn("hsqldb");
    when(context.getBean("hsqldbDatabaseConfiguration")).thenReturn(new Configuration(new HSQLDBTemplates()));
    when(context.getBean(SQLQueryBuilder.class)).thenReturn(builder);
    lenient().when(context.getBean(DataListBuilder.class)).thenAnswer(invocation -> new DataListBuilder());
    lenient().when(context.getBean(AweElements.class)).thenReturn(elements);
    when(builder.getVariables()).thenReturn(variables);
    when(builder.getLogMaskedValues()).thenReturn(new HashSet<>());
    when(builder.buildColumnValues(anyList(), anyList(), any(), any(), anyLong())).thenReturn(valuesQuery);
    when(valuesQuery.getSQL()).thenReturn(new SQLBindings("select", List.of()));
    lenient().when(elements.getEnumerated("EsStatus")).thenReturn(statuses);
    lenient().when(statuses.findLabel(anyString())).thenAnswer(invocation -> "1".equals(invocation.getArgument(0)) ? "Active" : "Blocked");
    stubLocales(elements);
    dataSourceUtils = mockStatic(DataSourceUtils.class);
    dataSourceUtils.when(() -> DataSourceUtils.getConnection(dataSource)).thenReturn(connection);
  }

  @AfterEach
  void tearDown() {
    dataSourceUtils.close();
  }

  @Test
  @DisplayName("A translated column groups its codes in the database with the code as label field, up to the cap plus one")
  void translatedColumnIsLabelledFromItsCode() throws Exception {
    when(valuesQuery.fetch()).thenReturn(List.of(tuple(0, 3L, 0), tuple(1, 1L, 1), tuple(null, 2L, null)));

    ServiceData out = connector.launchColumnValues(query(), parameters(), new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, null));

    verify(builder).buildColumnValues(eq(List.of(query().getSqlFieldList().get(1))), eq(List.of(query().getSqlFieldList().get(1))),
      eq(ColumnFilterType.VALUES), isNull(), eq(1001L));
    assertThat(out.getDataList().getRows()).extracting(row -> row.get("value").getValue(), row -> row.get("label").getValue(),
        row -> row.get("count").getValue())
      .containsExactly(org.assertj.core.groups.Tuple.tuple(0, "Blocked", 3L), org.assertj.core.groups.Tuple.tuple(1, "Active", 1L),
        org.assertj.core.groups.Tuple.tuple(null, null, 2L));
    dataSourceUtils.verify(() -> DataSourceUtils.releaseConnection(connection, dataSource));
  }

  @Test
  @DisplayName("The search of a column whose label is its value runs in the database, before the cap")
  void searchOfAPlainColumnRunsInTheDatabase() throws Exception {
    when(valuesQuery.fetch()).thenReturn(List.of(tuple("Anna", 1L), tuple("Hanna", 2L)));

    ServiceData out = connector.launchColumnValues(query(), parameters(), new ColumnValues("name", List.of("name"), ColumnFilterType.TEXT, "ann"));

    verify(builder).buildColumnValues(anyList(), eq(List.of()), eq(ColumnFilterType.TEXT), eq("ann"), eq(1001L));
    assertThat(out.getDataList().getRows()).hasSize(2);
  }

  @Test
  @DisplayName("The search of a plain column of values runs on the labels: its field may be numeric, which SQL cannot search as text")
  void searchOfAPlainColumnOfValuesRunsOnTheLabels() throws Exception {
    when(valuesQuery.fetch()).thenReturn(List.of(tuple(1, 1L), tuple(811, 1L), tuple(25, 1L)));

    ServiceData out = connector.launchColumnValues(query(), parameters(), new ColumnValues("name", List.of("name"), ColumnFilterType.VALUES, "1"));

    verify(builder).buildColumnValues(anyList(), eq(List.of()), eq(ColumnFilterType.VALUES), isNull(), eq(10001L));
    assertThat(out.getDataList().getRows()).extracting(row -> row.get("value").getValue()).containsExactly(1, 811);
  }

  @Test
  @DisplayName("The search of a translated column runs on the labels of the first 10,000 values; beyond them more values exist")
  void searchOfATranslatedColumnRunsOnTheLabels() throws Exception {
    List<Tuple> tuples = new ArrayList<>(IntStream.range(0, 10001).mapToObj(index -> tuple(index % 2, 1L, index % 2)).toList());
    when(valuesQuery.fetch()).thenReturn(tuples);

    ServiceData out = connector.launchColumnValues(query(), parameters(), new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, "act"));

    verify(builder).buildColumnValues(anyList(), anyList(), eq(ColumnFilterType.VALUES), isNull(), eq(10001L));
    assertThat(out.getDataList().getRows()).hasSize(ColumnValues.MAX_VALUES).allSatisfy(row -> assertThat(row).containsEntry("label", new CellData("Active")));
    assertThat(out.getVariableMap().get(AweConstants.ACTION_MORE_VALUES).getValue()).isEqualTo(true);
  }

  private static Query query() {
    List<SqlField> fields = new ArrayList<>(List.of(Field.builder().id("name").build(), Field.builder().id("status").translate("EsStatus").build()));
    return new Query().setId("UsrLst").setSqlFieldList(fields);
  }

  private static ObjectNode parameters() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, JsonNodeFactory.instance.arrayNode());
    return parameters;
  }

  private static Tuple tuple(Object... values) {
    return new Tuple() {
      @Override
      public <T> T get(int index, Class<T> type) {
        return type.cast(values[index]);
      }

      @Override
      public <T> T get(Expression<T> expr) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int size() {
        return values.length;
      }

      @Override
      public Object[] toArray() {
        return values;
      }
    };
  }
}
