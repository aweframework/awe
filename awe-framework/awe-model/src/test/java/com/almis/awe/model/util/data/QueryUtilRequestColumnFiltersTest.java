package com.almis.awe.model.util.data;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.queries.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static com.almis.awe.model.util.data.ColumnFilterTestFixtures.MAPPER;
import static com.almis.awe.model.util.data.ColumnFilterTestFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The grid column filters of a request apply only to the query the client launched, its target action (#791, S18):
 * any other query launched while serving the request (the screen restriction query of the screen access check, a
 * private or a service query) takes them only when its caller passes its own.
 */
class QueryUtilRequestColumnFiltersTest {

  private static final String CLIENT_QUERY = "AdvSchInlCst";
  private static final JsonNode REQUEST_FILTERS = json("[{'id':'InlSta','type':'values','values':['A']}]");

  private AweRequest request;
  private QueryUtil queryUtil;

  @BeforeEach
  void setUp() {
    request = new AweRequest(mock(HttpServletRequest.class), mock(HttpServletResponse.class), MAPPER);
    request.setTargetAction(CLIENT_QUERY);
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, REQUEST_FILTERS);
    request.setParameterList(requestParameters);
    queryUtil = new QueryUtil(new BaseConfigProperties(), new DatabaseConfigProperties(), MAPPER, new PrototypeRequestBeanHolder()) {
      @Override
      public AweRequest getRequest() {
        return request;
      }
    };
  }

  @Test
  @DisplayName("The query the client launched keeps the column filters of the request, copied or read from the request")
  void clientQueryKeepsTheRequestFilters() throws Exception {
    assertThat(columnFilters(CLIENT_QUERY, queryUtil.getParameters(null, "1", "0"))).isEqualTo(REQUEST_FILTERS);
    assertThat(columnFilters(CLIENT_QUERY, JsonNodeFactory.instance.objectNode())).isEqualTo(REQUEST_FILTERS);
  }

  @Test
  @DisplayName("Another query launched while serving the request does not take the column filters of the request")
  void otherQueryDoesNotTakeTheRequestFilters() throws Exception {
    assertThat(columnFilters("ScrRes", queryUtil.getParameters(null, "1", "0"))).isNull();
    assertThat(columnFilters("ScrRes", JsonNodeFactory.instance.objectNode())).isNull();
  }

  @Test
  @DisplayName("Another query takes the column filters its caller passes")
  void otherQueryTakesTheFiltersOfItsCaller() throws Exception {
    JsonNode callerFilters = json("[{'id':'Nam','type':'text','conditions':[{'operator':'eq','value':'x'}]}]");
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, callerFilters);

    assertThat(columnFilters("ScrRes", parameters)).isEqualTo(callerFilters);
  }

  private JsonNode columnFilters(String queryId, ObjectNode parameters) throws Exception {
    var variable = queryUtil.getVariableMap(new Query().setId(queryId), parameters).get(AweConstants.QUERY_COLUMN_FILTERS);
    return variable == null ? null : variable.getValue();
  }
}
