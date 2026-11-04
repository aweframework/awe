package com.almis.awe.model.util.data;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.ColumnFilterOperator;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.type.UnionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.util.data.ColumnFilterTestFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Request parameter {@code columnFilters} of the grid column autofilter (#912, S3): parsed from the request into a
 * closed contract, anything else rejected before a query is run.
 */
class QueryUtilColumnFilterTest {

  private QueryUtil queryUtil;

  @BeforeEach
  void setUp() {
    queryUtil = ColumnFilterTestFixtures.queryUtil();
  }

  @Test
  @DisplayName("A complete column filter is parsed with its fields, type, union and conditions")
  void completeFilterIsParsed() throws Exception {
    List<ColumnFilter> filters = queryUtil.getColumnFilterList(json("[{'id':'user','fields':['Nam','Eml'],'type':'text',"
      + "'union':'or','conditions':[{'operator':'contains','value':'50%'},{'operator':'notEmpty'}]},"
      + "{'id':'amount','type':'number','conditions':[{'operator':'between','value':'1','to':5.5}]},"
      + "{'id':'status','type':'values','values':['A',1],'empty':true}]"));

    assertThat(filters).hasSize(3);
    ColumnFilter user = filters.get(0);
    assertThat(user.getId()).isEqualTo("user");
    assertThat(user.getFields()).containsExactly("Nam", "Eml");
    assertThat(user.getType()).isEqualTo(ColumnFilterType.TEXT);
    assertThat(user.getUnion()).isEqualTo(UnionType.OR);
    assertThat(user.getConditions()).extracting("operator").containsExactly(ColumnFilterOperator.CONTAINS, ColumnFilterOperator.NOT_EMPTY);
    assertThat(user.getConditions().get(0).getValue()).isEqualTo("50%");
    ColumnFilter amount = filters.get(1);
    assertThat(amount.getFields()).containsExactly("amount");
    assertThat(amount.getUnion()).isEqualTo(UnionType.AND);
    assertThat(amount.getConditions().get(0).getValue()).isEqualTo("1");
    assertThat(amount.getConditions().get(0).getTo()).isEqualTo("5.5");
    ColumnFilter status = filters.get(2);
    assertThat(status.getValues()).extracting(JsonNode::toString).containsExactly("\"A\"", "1");
    assertThat(status.isEmpty()).isTrue();
    assertThat(status.getConditions()).isEmpty();
  }

  @Test
  @DisplayName("No columnFilters, a null or an empty array means no column filters")
  void absentOrEmptyMeansNoFilters() throws Exception {
    assertThat(queryUtil.getColumnFilterList(null)).isEmpty();
    assertThat(queryUtil.getColumnFilterList(JsonNodeFactory.instance.nullNode())).isEmpty();
    assertThat(queryUtil.getColumnFilterList(json("[]"))).isEmpty();
  }

  @ParameterizedTest(name = "{1}: {0}")
  @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
    "[{'id':'a','type':'text','conditions':[{'operator':'regex','value':'x'}]}]|unknown operator",
    "[{'id':'a','type':'text','conditions':[{'operator':'eq','value':'x'},{'operator':'eq','value':'y'},{'operator':'eq','value':'z'}]}]|more than 2 conditions",
    "[{'id':'a','type':'text','union':'xor','conditions':[{'operator':'eq','value':'x'}]}]|unknown union",
    "[{'id':'a','type':'select','values':['x']}]|unknown type",
    "[{'id':'a','values':['x']}]|unknown type",
    "{'id':'a','type':'text'}|must be an array",
    "'a'|must be an array",
    "['a']|must be an object",
    "[{'type':'text','conditions':[{'operator':'eq','value':'x'}]}]|needs an id",
    "[{'id':'a','fields':'Nam','type':'text'}]|fields must be",
    "[{'id':'a','fields':[],'type':'text'}]|fields must be",
    "[{'id':'a','type':'text','conditions':{'operator':'eq','value':'x'}}]|conditions must be a list",
    "[{'id':'a','type':'text','conditions':[{'operator':'eq'}]}]|needs a value",
    "[{'id':'a','type':'number','conditions':[{'operator':'gt','value':'ten'}]}]|is not a number",
    "[{'id':'a','type':'number','conditions':[{'operator':'contains','value':'1'}]}]|unknown operator for type number",
    "[{'id':'a','type':'number','conditions':[{'operator':'eq','value':'1e2000000000'}]}]|is not a number",
    "[{'id':'a','type':'number','conditions':[{'operator':'eq','value':1e400}]}]|is not a number",
    "[{'id':'a','type':'text','conditions':[{'operator':'gt','value':'b'}]}]|unknown operator for type text",
    "[{'id':'a','type':'date','conditions':[{'operator':'eq','value':'2026-11-03'}]}]|is not a date",
    "[{'id':'a','type':'date','conditions':[{'operator':'between','value':'01/11/2026'}]}]|needs a value",
    "[{'id':'a','type':'values','values':[['x']]}]|plain values",
    "[{'id':'a','type':'values','empty':'yes'}]|empty must be a boolean"
  })
  @DisplayName("An unknown operator, union or type, more than two conditions or a malformed entry is rejected")
  void invalidFilterIsRejected(String columnFilters, String reason) {
    JsonNode node = json(columnFilters);

    assertThatThrownBy(() -> queryUtil.getColumnFilterList(node))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageStartingWith("ERROR_MESSAGE_GENERATING_FILTER")
      .hasMessageContaining(reason)
      .extracting("title").isEqualTo("ERROR_TITLE_GENERATING_FILTER");
  }

  @ParameterizedTest(name = "{1}")
  @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
    "[{'id':'name','fields':['u.Pwd'],'type':'text','conditions':[{'operator':'eq','value':'x'}]}]|unknown field u.Pwd",
    "[{'id':'secret','type':'text','conditions':[{'operator':'startsWith','value':'a'}]}]|field secret of column filter secret is not printed",
    "[{'id':'total','type':'number','conditions':[{'operator':'gt','value':'1'}]}]"
      + "|field total of column filter total is an aggregate or window function"
  })
  @DisplayName("An unknown field, a noprint field (a filter cannot probe values the query never sends) or an aggregate field is rejected")
  void fieldOutsideTheAllowListIsRejected(String columnFilters, String reason) throws Exception {
    Query query = new Query().setId("UsrLst").setSqlFieldList(new ArrayList<>(List.<SqlField>of(
      Field.builder().id("Nam").table("u").alias("name").build(),
      Field.builder().id("Pwd").table("u").alias("secret").noprint(true).build(),
      Field.builder().id("Ide").table("u").alias("total").function("CNT").build())));
    ColumnFilter columnFilter = queryUtil.getColumnFilterList(json(columnFilters)).getFirst();

    assertThatThrownBy(() -> queryUtil.getColumnFilterFields(query, columnFilter))
      .isInstanceOf(AWEQueryException.class)
      .hasMessageContaining(reason);
  }

  @Test
  @DisplayName("The columnFilters request parameter is kept in the query variables, as the sort")
  void columnFiltersReachTheVariables() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    JsonNode columnFilters = json("[{'id':'a','type':'text','conditions':[{'operator':'eq','value':'x'}]}]");
    parameters.set(AweConstants.COMPONENT_COLUMN_FILTERS, columnFilters);

    Map<String, QueryParameter> variables = queryUtil.getDefaultVariableMap(parameters);

    assertThat(variables.get(AweConstants.QUERY_COLUMN_FILTERS).getValue()).isEqualTo(columnFilters);
    assertThat(queryUtil.getDefaultVariableMap(JsonNodeFactory.instance.objectNode())).doesNotContainKey(AweConstants.QUERY_COLUMN_FILTERS);
  }
}
