package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Column values action of the grid autofilter (#912, S12): {@code /action/column-values/<query>} lists the distinct
 * values of a column of the user list (UsrLst) with the label the grid shows and the rows of each one, with the column
 * filters of the other columns in effect. The rejected fields are proven by the unit tests of QueryUtil and QueryService.
 */
@Tag("integration")
@DisplayName("Column values action tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class ColumnValuesActionTest extends AbstractSpringAppIntegrationTest {

  private static final String QUERY = "UsrLst";

  @Test
  @DisplayName("A translated column lists its codes with the translation and the rows of each one, as the grid shows them, "
    + "following the filters of the other columns and without its own")
  void translatedColumnValues() throws Exception {
    String otherFilter = "{\"id\":\"Nom\",\"type\":\"text\",\"conditions\":[{\"operator\":\"eq\",\"value\":\"test\"}]}";
    String ownFilter = "{\"id\":\"LanTxt\",\"fields\":[\"Lan\"],\"type\":\"values\",\"values\":[\"no-such-language\"]}";
    List<Tuple> all = expectedValues("[]");
    List<Tuple> filtered = expectedValues("[" + otherFilter + "]");

    assertThat(columnValues("[]")).containsExactlyElementsOf(all);
    assertThat(columnValues("[" + ownFilter + "," + otherFilter + "]")).containsExactlyElementsOf(filtered);
    assertThat(filtered).isNotEmpty().isNotEqualTo(all);
  }

  /**
   * Values, labels and counts of the language column, from the rows of the grid with the given column filters, sorted by
   * value with the empty values last
   */
  private List<Tuple> expectedValues(String columnFilters) throws Exception {
    Map<Object, List<Object>> expected = new TreeMap<>(Comparator.nullsLast(Comparator.comparing(Object::toString)));
    for (Map<String, Object> row : action("data", "{\"max\":0,\"columnFilters\":" + columnFilters + "}", "fill", "datalist")) {
      expected.computeIfAbsent(row.get("Lan"), language -> new ArrayList<>(List.of(row.get("LanTxt"), 0)));
      expected.get(row.get("Lan")).set(1, (Integer) expected.get(row.get("Lan")).get(1) + 1);
    }
    return expected.entrySet().stream().map(entry -> tuple(entry.getKey(), entry.getValue().get(0), entry.getValue().get(1))).toList();
  }

  private List<Tuple> columnValues(String columnFilters) throws Exception {
    return action("column-values", "{\"columnValues\":{\"id\":\"LanTxt\",\"fields\":[\"Lan\"],\"type\":\"values\"},"
      + "\"columnFilters\":" + columnFilters + "}", "column-values", "datalist").stream()
      .map(row -> tuple(row.get("value"), row.get("label"), row.get("count"))).toList();
  }

  private List<Map<String, Object>> action(String action, String parameters, String type, String parameter) throws Exception {
    String result = perform(action, parameters);
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == '" + type + "')].parameters." + parameter + ".rows");
    assertThat(rows).as("%s of %s: %s", type, action, result).hasSize(1);
    return rows.getFirst();
  }

  private String perform(String action, String parameters) throws Exception {
    return mockMvc.perform(post("/action/" + action + "/" + QUERY)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(parameters)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private MockHttpSession signedInSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", "test").session(session)).andReturn();
    return session;
  }
}
