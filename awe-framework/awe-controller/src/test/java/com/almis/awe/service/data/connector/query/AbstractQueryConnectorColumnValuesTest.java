package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.util.data.DataListUtil;
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
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.almis.awe.service.data.ColumnFilterTestFixtures.stubLocales;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Distinct values of a grid column on service, enumerated and queue queries (#912, S9-S11): the rows that match the
 * other column filters are grouped in Java, and each value is labelled with the processing of its column.
 */
@ExtendWith(MockitoExtension.class)
class AbstractQueryConnectorColumnValuesTest {

  private static final JsonMapper MAPPER = new JsonMapper();

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

  @Mock
  private EnumeratedGroup statuses;

  private ServiceBuilder serviceBuilder;
  private ServiceQueryConnector connector;

  @BeforeEach
  void setUp() throws Exception {
    serviceBuilder = mock(ServiceBuilder.class, RETURNS_SELF);
    lenient().when(context.getBean(DataListBuilder.class)).thenAnswer(invocation -> new DataListBuilder());
    lenient().when(context.getBean(ServiceBuilder.class)).thenReturn(serviceBuilder);
    lenient().when(context.getBean(AweElements.class)).thenReturn(elements);
    lenient().when(serviceBuilder.build()).thenReturn(serviceData());
    lenient().when(elements.getEnumerated("EsStatus")).thenReturn(statuses);
    lenient().when(statuses.findLabel(anyString())).thenAnswer(invocation -> "1".equals(invocation.getArgument(0)) ? "Active" : "Blocked");
    stubLocales(elements);
    connector = new ServiceQueryConnector(ColumnFilterTestFixtures.queryUtil(elements), new BaseConfigProperties(), elements, numericService, encodeService, mapper);
    connector.setApplicationContext(context);
  }

  @Test
  @DisplayName("The values of a translated column are its codes, labelled with the translation, after the other column filters")
  void translatedColumnValues() throws Exception {
    DataList values = connector.launchColumnValues(query(), parameters("[{'id':'name','type':'text','conditions':[{'operator':'ne','value':'Cid'}]}]"),
      new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, null)).getDataList();

    assertThat(values.getRows()).extracting(row -> row.get("value").getValue(), row -> row.get("label").getValue(), row -> row.get("count").getValue())
      .containsExactly(org.assertj.core.groups.Tuple.tuple("0", "Blocked", 2L), org.assertj.core.groups.Tuple.tuple("1", "Active", 1L));
  }

  @Test
  @DisplayName("A computed column is labelled from the first row of each value, and the search applies to the labels")
  void computedColumnValuesWithSearch() throws Exception {
    ServiceData out = connector.launchColumnValues(query(), parameters(null),
      new ColumnValues("summary", List.of("status"), ColumnFilterType.VALUES, "BLOCK"));

    assertThat(out.getDataList().getRows()).hasSize(1);
    assertThat(out.getDataList().getRows().get(0)).containsEntry("value", new CellData("0"))
      .containsEntry("label", new CellData("Bob (Blocked)")).containsEntry("count", new CellData(2L));
    assertThat(out.getVariableMap().get(AweConstants.ACTION_MORE_VALUES).getValue()).isEqualTo(false);
  }

  @Test
  @DisplayName("A query whose service manages the pagination is rejected before the service runs")
  void paginationManagedIsRejected() {
    Query query = query().setPaginationManaged(true);
    ObjectNode parameters = parameters(null);
    ColumnValues columnValues = new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, null);

    assertThatThrownBy(() -> connector.launchColumnValues(query, parameters, columnValues)).isInstanceOf(AWEQueryException.class)
      .hasMessageContaining("manages its pagination");
    verify(context, never()).getBean(ServiceBuilder.class);
  }

  private static Query query() {
    List<SqlField> fields = new ArrayList<>(List.of(Field.builder().id("name").build(),
      Field.builder().id("status").translate("EsStatus").build()));
    List<Computed> computed = new ArrayList<>(List.of(Computed.builder().alias("summary").format("[name] ([status])").build()));
    return new Query().setId("UsrSrv").setService("usrSrv").setSqlFieldList(fields).setComputedList(computed);
  }

  private static ServiceData serviceData() {
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "name", List.of("Ann", "Bob", "Cid", "Dan"));
    DataListUtil.addColumn(dataList, "status", Arrays.asList("1", "0", "1", "0"));
    return new ServiceData().setDataList(dataList);
  }

  private static ObjectNode parameters(String columnFilters) {
    ObjectNode parameters = MAPPER.createObjectNode().put(AweConstants.COMPONENT_MAX, 0).put(AweConstants.COMPONENT_PAGE, 1);
    parameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, MAPPER.readTree(columnFilters == null ? "[]" : columnFilters.replace('\'', '"')));
    return parameters;
  }
}
