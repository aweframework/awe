package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class used for testing queries through ActionController
 */
@Slf4j
@Tag("integration")
@WithMockUser(username = "test", password = "test")
class SchedulerQueriesTest extends AbstractSpringAppIntegrationTest {

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       Result
   * @param expectedRows Expected rows
   * @param page         Page
   * @param totalPages   Total pages
   * @param records      Total records
   * @throws Exception Test error
   */
  public void assertResultJson(String queryName, String result, int expectedRows, int page, int totalPages, int records) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(page, dataList.get("page").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(expectedRows, dataListRows.size());
    if (totalPages > -1) {
      assertTrue(totalPages <= dataList.get("total").asInt());
    }
    if (records > -1) {
      assertTrue(records <= dataList.get("records").asInt());
    }

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("--------------------------------------------------------------------------------------");

  }

  /**
   * Performs the mock request and returns the response as a string
   *
   * @param queryName Query ID
   * @param variables Variables
   * @param expected  Expected result
   * @return Output
   * @throws Exception Error performing request
   */
  private String performRequest(String queryName, String variables, String expected) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/action/data/" + queryName)
            .with(csrf())
            .content("{" + variables + "}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(expected))
            .andReturn();
    return mvcResult.getResponse().getContentAsString();
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testExecutionsToPurge() throws Exception {
    String queryName = "getExecutionsToPurge";
    String variables = "\"taskId\":2,\"executions\":5";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"id\":1,\"executionId\":5},{\"id\":2,\"executionId\":4},{\"id\":3,\"executionId\":3},{\"id\":4,\"executionId\":2},{\"id\":5,\"executionId\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, expected);
    logger.warn(result);
    assertResultJson(queryName, result, 5, 1, 1, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testExecutionsToPurgeBig() throws Exception {
    String queryName = "getExecutionsToPurge";
    String variables = "\"taskId\":2,\"executions\":12";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, expected);
    logger.warn(result);
    assertResultJson(queryName, result, 0, 1, 1, 0);
  }

  // ====================================================================================
  // GitLab #685 regression: the AweSchTsk INNER JOIN was removed from getLastExecution
  // and getTaskExecution. The HSQLDB seed (testdata-hsqldb.sql) inserts 10 AweSchExe rows
  // for taskId=2 / group 'MANUAL' but NO AweSchTsk rows, so taskId=2 executions are
  // naturally orphaned. Before #685 the INNER JOIN silently dropped these rows (zero
  // results); after the fix they must still surface. These tests prove the SQL behaviour
  // at runtime (the unit tests only check DAO mapping from mocked rows).
  // ====================================================================================

  /**
   * Performs the mock request asserting only HTTP 200 (no strict content match) and
   * returns the response body, so callers can assert row counts / fields flexibly.
   *
   * @param queryName Query ID
   * @param variables Variables JSON fragment (without braces)
   * @return Response body
   * @throws Exception Error performing request
   */
  private String performRequestStatusOnly(String queryName, String variables) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/action/data/" + queryName)
            .with(csrf())
            .content("{" + variables + "}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    return mvcResult.getResponse().getContentAsString();
  }

  /**
   * Extracts the datalist rows array from a fill action response.
   *
   * @param result Raw response body
   * @return Rows array
   * @throws Exception Parse error
   */
  private ArrayNode extractRows(String result) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode dataList = (ObjectNode) ((ObjectNode) fillAction.get("parameters")).get("datalist");
    return (ArrayNode) dataList.get("rows");
  }

  /**
   * #685: getLastExecution must return exactly the single latest execution row for an
   * orphaned task (no AweSchTsk row). Before the fix the AweSchTsk INNER JOIN dropped it.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetLastExecutionOrphanReturnsSingleLatestRow() throws Exception {
    String result = performRequestStatusOnly("getLastExecution", "\"taskId\":2,\"taskGroup\":\"MANUAL\"");
    ArrayNode rows = extractRows(result);
    assertEquals(1, rows.size(), "getLastExecution must return exactly 1 row for the orphaned task");
    // The latest execution (max IniDat) for taskId=2 is executionId=10
    assertEquals(10, rows.get(0).get("executionId").asInt());
    // taskId column preserved
    assertEquals(2, rows.get(0).get("taskId").asInt());
    logger.warn("getLastExecution orphan row: {}", rows.get(0));
  }

  /**
   * #685: getTaskExecution must return exactly the requested execution row for an
   * orphaned task (no AweSchTsk row), with a null-tolerant name produced by the
   * {@code <field query="getTaskName"/>} scalar subquery instead of dropping the row.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetTaskExecutionOrphanReturnsSingleRowWithNullName() throws Exception {
    String result = performRequestStatusOnly("getTaskExecution", "\"taskId\":2,\"taskExecution\":5");
    ArrayNode rows = extractRows(result);
    assertEquals(1, rows.size(), "getTaskExecution must return exactly 1 row for the orphaned task");
    assertEquals(5, rows.get(0).get("executionId").asInt());
    assertEquals(2, rows.get(0).get("taskId").asInt());
    // name comes from the scalar subquery; NULL when the AweSchTsk row is missing
    // (the row must NOT be dropped just because name is unavailable)
    assertTrue(rows.get(0).get("name") == null || rows.get(0).get("name").isNull(),
        "name must be null for an orphaned task, but the row must still be returned");
    logger.warn("getTaskExecution orphan row: {}", rows.get(0));
  }

  /**
   * #685: getTaskExecution parameterised row selection still works after the join
   * removal — a different executionId returns that specific single row.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetTaskExecutionReturnsRequestedExecutionRow() throws Exception {
    String result = performRequestStatusOnly("getTaskExecution", "\"taskId\":2,\"taskExecution\":3");
    ArrayNode rows = extractRows(result);
    assertEquals(1, rows.size());
    assertEquals(3, rows.get(0).get("executionId").asInt());
    logger.warn("getTaskExecution requested row: {}", rows.get(0));
  }
}
