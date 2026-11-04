package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.ColumnFilterTestFixtures;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.ServiceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.service.data.ColumnFilterTestFixtures.stubLocales;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Grid column filters of non-SQL queries (#912, S6): the connector filters the rows in Java unless the service
 * manages the pagination, and rejects a filter on a field the query does not print before the service runs.
 */
@ExtendWith(MockitoExtension.class)
class AbstractQueryConnectorColumnFilterTest {

  private static final JsonMapper MAPPER = new JsonMapper();
  private static final String FILTER = "[{'id':'name','type':'text','conditions':[{'operator':'startsWith','value':'a'}]}]";

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private AweElements elements;

  @Mock
  private NumericService numericService;

  @Mock
  private EncodeService encodeService;

  @Mock
  private ObjectMapper mapper;

  @Mock
  private ApplicationContext context;

  private QueryUtil queryUtil;
  private ServiceQueryConnector connector;

  @BeforeEach
  void setUp() {
    lenient().when(context.getBean(DataListBuilder.class)).thenAnswer(invocation -> new DataListBuilder());
    lenient().when(context.getBean(AweElements.class)).thenReturn(elements);
    stubLocales(elements);
    queryUtil = ColumnFilterTestFixtures.queryUtil(elements);
    connector = new ServiceQueryConnector(queryUtil, baseConfigProperties, elements, numericService, encodeService, mapper);
    connector.setApplicationContext(context);
  }

  @Test
  @DisplayName("The rows of a service query are filtered by the column filters")
  void serviceRowsAreFiltered() throws Exception {
    DataList output = connector.generateResults(serviceData(), query(), variables(FILTER)).getDataList();

    assertThat(DataListUtil.getColumn(output, "name")).extracting("stringValue").containsExactly("Ann", "anna");
    assertThat(output.getRecords()).isEqualTo(2L);
  }

  @Test
  @DisplayName("A translated field of a service query is filtered by its code, the value the list of values sends")
  void translatedFieldIsFilteredByItsCode() throws Exception {
    EnumeratedGroup countries = mock(EnumeratedGroup.class);
    lenient().when(countries.findLabel(anyString())).thenAnswer(invocation -> "ES".equals(invocation.getArgument(0)) ? "Spain" : "France");
    lenient().when(elements.getEnumerated("EsCtr")).thenReturn(countries);
    Query query = query();
    query.getSqlFieldList().add(Field.builder().id("country").translate("EsCtr").build());
    ServiceData serviceData = serviceData();
    DataListUtil.addColumn(serviceData.getDataList(), "country", List.of("ES", "FR", "ES"));

    DataList output = connector.generateResults(serviceData, query,
      variables("[{'id':'country','type':'values','values':['ES']}]")).getDataList();

    assertThat(DataListUtil.getColumn(output, "country")).extracting("stringValue").containsExactly("Spain", "Spain");
  }

  @Test
  @DisplayName("A query whose service manages the pagination is not filtered in Java")
  void paginationManagedIsNotFiltered() throws Exception {
    Query query = query().setPaginationManaged(true);

    DataList output = connector.generateResults(serviceData(), query, variables(FILTER)).getDataList();

    assertThat(DataListUtil.getColumn(output, "name")).extracting("stringValue").containsExactly("Ann", "Bob", "anna");
  }

  @Test
  @DisplayName("A filter on a noprint field is rejected before the service runs")
  void noprintFieldIsRejectedBeforeTheService() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, MAPPER.readTree(FILTER.replace("'name'", "'secret'").replace('\'', '"')));

    assertThatThrownBy(() -> connector.launch(query(), parameters)).isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("field secret of column filter secret is not printed");
    verify(context, never()).getBean(ServiceBuilder.class);
  }

  private static Query query() {
    List<SqlField> fields = new ArrayList<>(List.of(Field.builder().id("name").build(),
      Field.builder().id("secret").noprint(true).build()));
    return new Query().setId("UsrSrv").setService("usrSrv").setSqlFieldList(fields);
  }

  private static ServiceData serviceData() {
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "name", List.of("Ann", "Bob", "anna"));
    DataListUtil.addColumn(dataList, "secret", List.of("1", "2", "3"));
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, QueryParameter> variables(String columnFilters) {
    Map<String, QueryParameter> variables = new HashMap<>();
    variables.put(AweConstants.QUERY_MAX, new QueryParameter(JsonNodeFactory.instance.numberNode(0L)));
    variables.put(AweConstants.QUERY_PAGE, new QueryParameter(JsonNodeFactory.instance.numberNode(1L)));
    variables.put(AweConstants.QUERY_COLUMN_FILTERS, new QueryParameter(MAPPER.readTree(columnFilters.replace('\'', '"'))));
    return variables;
  }
}
