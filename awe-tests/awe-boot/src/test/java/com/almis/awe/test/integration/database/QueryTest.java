package com.almis.awe.test.integration.database;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.service.EncodeService;
import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.factory.WithMockCustomUser;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class used for testing queries through ActionController
 */
@DisplayName("Query tests")
@Slf4j
@WithMockCustomUser
public class QueryTest extends AbstractSpringAppIntegrationTest {

  // Constants
  private static final String DATABASE = null;

  @Autowired
  DataSource dataSource;

  @Autowired
  private AweDatabaseContextHolder aweDatabaseContextHolder;

  @Autowired
  private EncodeService encodeService;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleGetAll() throws Exception {
    String queryName = "SimpleGetAll";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"Ide\":6},{\"Ide\":8},{\"Ide\":9},{\"Ide\":6},{\"Ide\":8},{\"Ide\":9},{\"Ide\":7},{\"Ide\":15},{\"Ide\":16},{\"Ide\":7},{\"Ide\":15},{\"Ide\":16}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 12, 1, 1, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleGetValue() throws Exception {
    String queryName = "SimpleGetValue";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"IdeSit\":17}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySubqueryInTable() throws Exception {
    String queryName = "SubqueryInTable";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"Asdf\":6},{\"Asdf\":8},{\"Asdf\":9},{\"Asdf\":6},{\"Asdf\":8},{\"Asdf\":9},{\"Asdf\":7},{\"Asdf\":15},{\"Asdf\":16},{\"Asdf\":7},{\"Asdf\":15},{\"Asdf\":16}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySubqueryInJoin() throws Exception {
    String queryName = "SubqueryInJoin";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"nameAll\":\"test\",\"name\":\"test\",\"id\":1},{\"nameAll\":\"donald\",\"name\":\"donald\",\"id\":2},{\"nameAll\":\"jorgito\",\"name\":\"jorgito\",\"id\":3},{\"nameAll\":\"juanito\",\"name\":\"juanito\",\"id\":4},{\"nameAll\":\"jaimito\",\"name\":\"jaimito\",\"id\":5}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 5);
  }

  /**
   * Test of launch query without tables.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryWithOutTables() throws Exception {
    String queryName = "testQueryWithOutTables";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"Sum\":2}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryTwoTables() throws Exception {
    String queryName = "QueryTwoTables";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":8,\"rows\":[{\"IdeModPro\":62,\"Nam\":\"Base\"},{\"IdeModPro\":65,\"Nam\":\"Base\"},{\"IdeModPro\":74,\"Nam\":\"Base\"},{\"IdeModPro\":937,\"Nam\":\"Base\"},{\"IdeModPro\":62,\"Nam\":\"Test\"},{\"IdeModPro\":65,\"Nam\":\"Test\"},{\"IdeModPro\":74,\"Nam\":\"Test\"},{\"IdeModPro\":937,\"Nam\":\"Test\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 8);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFieldFunctions() throws Exception {
    String queryName = "TestFieldFunctions";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"Sum\":24,\"Max\":2584,\"AbsOpe\":2584,\"AbsIde\":15934,\"AbsInteger\":1212,\"AbsDouble\":1212.12123,\"Avg\":10.16666666666666666666666666666666666667,\"CntDst\":3\"Cnt\":12,\"Min\":60,\"Trim\":\"as as  daef\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    logger.debug(expected);

    ArrayNode dataListRows = assertResultJson(queryName, result, 1);

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      assertEquals(24, component.get("Sum").asInt());
      assertEquals(2584, component.get("Max").asInt());
      assertEquals(2584, component.get("AbsOpe").asInt());
      assertEquals(15934, component.get("AbsIde").asInt());
      assertEquals(10, component.get("Avg").asInt());
      assertEquals(12, component.get("Cnt").asInt());
      assertEquals(3, component.get("CntDst").asInt());
      assertEquals(60, component.get("Min").asInt());
      assertEquals("as as  daef", component.get("Trim").asText());
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFieldGroupByFunctions() throws Exception {

    assumeTrue(!isInMemoryDatabase());

    String queryName = "TestFieldGroupByFunctions";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3},{\"First\":1,\"Last\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    logger.info("testDatabaseQueryFieldGroupByFunctions");
    logger.info(result);

    ArrayNode dataListRows = assertResultJson(queryName, result, 12);

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      assertEquals(1, component.get("First").asInt());
      assertEquals(3, component.get("Last").asInt());
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFieldDateFunctions() throws Exception {
    String queryName = "TestFieldDateFunctions";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"Sum\":24,\"Max\":2584,\"Avg\":10.16666666666666666666666666666666666667,\"CntDst\":3\"Cnt\":12,\"Min\":60}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    logger.debug(expected);

    ArrayNode dataListRows = assertResultJson(queryName, result, 1);

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      assertEquals(1978, component.get("year").asInt());
      assertEquals(10, component.get("month").asInt());
      assertEquals(23, component.get("day").asInt());
      assertEquals(15, component.get("hour").asInt());
      assertEquals(6, component.get("minute").asInt());
      assertEquals(21, component.get("second").asInt());
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySubqueryInField() throws Exception {
    String queryName = "SubqueryInField";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"},{\"subquery\":\"Onate\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySubqueryInFieldDistinct() throws Exception {
    String queryName = "SubqueryInFieldDistinct";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"subquery\":\"Onate\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryValueInField() throws Exception {
    String queryName = "QueryValueInField";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"one\":\"1\",\"IdeModPro\":62,\"id\":1},{\"one\":\"1\",\"IdeModPro\":65,\"id\":2},{\"one\":\"1\",\"IdeModPro\":74,\"id\":3},{\"one\":\"1\",\"IdeModPro\":937,\"id\":4}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryVariableInField() throws Exception {
    String queryName = "QueryVariableInField";
    String variables = "\"variable\": 1";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeModPro\":62,\"1\":1},{\"IdeModPro\":65,\"1\":1},{\"IdeModPro\":74,\"1\":1},{\"IdeModPro\":937,\"1\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.debug(result);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryStaticVariableInField() throws Exception {
    String queryName = "QueryStaticVariableInField";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeModPro\":62,\"1\":1},{\"IdeModPro\":65,\"1\":1},{\"IdeModPro\":74,\"1\":1},{\"IdeModPro\":937,\"1\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleUnion() throws Exception {
    String queryName = "SimpleUnion";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"miau\":\"com.microsoft.sqlserver.jdbc.SQLServerDriver\"},{\"miau\":\"com.sybase.jdbc3.jdbc.SybDriver\"},{\"miau\":\"oracle.jdbc.driver.OracleDriver\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleUnionAll() throws Exception {
    String queryName = "SimpleUnionAll";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"Drv\":\"com.sybase.jdbc3.jdbc.SybDriver\"},{\"Drv\":\"com.microsoft.sqlserver.jdbc.SQLServerDriver\"},{\"Drv\":\"oracle.jdbc.driver.OracleDriver\"},{\"Drv\":\"oracle.jdbc.driver.OracleDriver\"},{\"Drv\":\"com.microsoft.sqlserver.jdbc.SQLServerDriver\"},{\"Drv\":\"com.sybase.jdbc3.jdbc.SybDriver\"},{\"Drv\":\"com.sybase.jdbc3.jdbc.SybDriver\"},{\"Drv\":\"com.microsoft.sqlserver.jdbc.SQLServerDriver\"},{\"Drv\":\"oracle.jdbc.driver.OracleDriver\"},{\"Drv\":\"oracle.jdbc.driver.OracleDriver\"},{\"Drv\":\"com.microsoft.sqlserver.jdbc.SQLServerDriver\"},{\"Drv\":\"com.sybase.jdbc3.jdbc.SybDriver\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleLeftJoin() throws Exception {
    String queryName = "SimpleLeftJoin";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"thm_name\":null,\"db_name\":\"Base\"},{\"thm_name\":null,\"db_name\":\"Test\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 2);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQuerySimpleRightJoinWithAlias() throws Exception {
    String queryName = "SimpleRightJoinWithAlias";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"thm_name\":\"adminflare\",\"db_name\":null},{\"thm_name\":\"amazonia\",\"db_name\":null},{\"thm_name\":\"asphalt\",\"db_name\":null},{\"thm_name\":\"clean\",\"db_name\":null},{\"thm_name\":\"default\",\"db_name\":null},{\"thm_name\":\"dust\",\"db_name\":null},{\"thm_name\":\"eclipse\",\"db_name\":null},{\"thm_name\":\"fresh\",\"db_name\":null},{\"thm_name\":\"frost\",\"db_name\":null},{\"thm_name\":\"grass\",\"db_name\":null},{\"thm_name\":\"purple-hills\",\"db_name\":null},{\"thm_name\":\"silver\",\"db_name\":null},{\"thm_name\":\"sky\",\"db_name\":null},{\"thm_name\":\"sunny\",\"db_name\":null},{\"thm_name\":\"sunset\",\"db_name\":null},{\"thm_name\":\"white\",\"db_name\":null}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarEq() throws Exception {
    String queryName = "FilterField-Var-Eq";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":10},{\"IdeAweAppPar\":7},{\"IdeAweAppPar\":8},{\"IdeAweAppPar\":9}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldNoTableVarEq() throws Exception {
    String queryName = "FilterFieldNoTable-Var-Eq";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7},{\"IdeAweAppPar\":8},{\"IdeAweAppPar\":9},{\"IdeAweAppPar\":10}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarNe() throws Exception {
    String queryName = "FilterField-Var-Ne";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":35,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":11,\"id\":7},{\"IdeAweAppPar\":12,\"id\":8},{\"IdeAweAppPar\":13,\"id\":9},{\"IdeAweAppPar\":14,\"id\":10},{\"IdeAweAppPar\":15,\"id\":11},{\"IdeAweAppPar\":16,\"id\":12},{\"IdeAweAppPar\":17,\"id\":13},{\"IdeAweAppPar\":18,\"id\":14},{\"IdeAweAppPar\":19,\"id\":15},{\"IdeAweAppPar\":20,\"id\":16},{\"IdeAweAppPar\":21,\"id\":17},{\"IdeAweAppPar\":22,\"id\":18},{\"IdeAweAppPar\":23,\"id\":19},{\"IdeAweAppPar\":24,\"id\":20},{\"IdeAweAppPar\":25,\"id\":21},{\"IdeAweAppPar\":26,\"id\":22},{\"IdeAweAppPar\":27,\"id\":23},{\"IdeAweAppPar\":28,\"id\":24},{\"IdeAweAppPar\":29,\"id\":25},{\"IdeAweAppPar\":30,\"id\":26},{\"IdeAweAppPar\":31,\"id\":27},{\"IdeAweAppPar\":32,\"id\":28},{\"IdeAweAppPar\":33,\"id\":29},{\"IdeAweAppPar\":34,\"id\":30}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarGe() throws Exception {
    String queryName = "FilterField-Var-Ge";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":39,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":7,\"id\":7},{\"IdeAweAppPar\":8,\"id\":8},{\"IdeAweAppPar\":9,\"id\":9},{\"IdeAweAppPar\":10,\"id\":10},{\"IdeAweAppPar\":11,\"id\":11},{\"IdeAweAppPar\":12,\"id\":12},{\"IdeAweAppPar\":13,\"id\":13},{\"IdeAweAppPar\":14,\"id\":14},{\"IdeAweAppPar\":15,\"id\":15},{\"IdeAweAppPar\":16,\"id\":16},{\"IdeAweAppPar\":17,\"id\":17},{\"IdeAweAppPar\":18,\"id\":18},{\"IdeAweAppPar\":19,\"id\":19},{\"IdeAweAppPar\":20,\"id\":20},{\"IdeAweAppPar\":21,\"id\":21},{\"IdeAweAppPar\":22,\"id\":22},{\"IdeAweAppPar\":23,\"id\":23},{\"IdeAweAppPar\":24,\"id\":24},{\"IdeAweAppPar\":25,\"id\":25},{\"IdeAweAppPar\":26,\"id\":26},{\"IdeAweAppPar\":27,\"id\":27},{\"IdeAweAppPar\":28,\"id\":28},{\"IdeAweAppPar\":29,\"id\":29},{\"IdeAweAppPar\":30,\"id\":30}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarGeDouble() throws Exception {
    String queryName = "FilterField-Var-GeDouble";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":35,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":11,\"id\":7},{\"IdeAweAppPar\":12,\"id\":8},{\"IdeAweAppPar\":13,\"id\":9},{\"IdeAweAppPar\":14,\"id\":10},{\"IdeAweAppPar\":15,\"id\":11},{\"IdeAweAppPar\":16,\"id\":12},{\"IdeAweAppPar\":17,\"id\":13},{\"IdeAweAppPar\":18,\"id\":14},{\"IdeAweAppPar\":19,\"id\":15},{\"IdeAweAppPar\":20,\"id\":16},{\"IdeAweAppPar\":21,\"id\":17},{\"IdeAweAppPar\":22,\"id\":18},{\"IdeAweAppPar\":23,\"id\":19},{\"IdeAweAppPar\":24,\"id\":20},{\"IdeAweAppPar\":25,\"id\":21},{\"IdeAweAppPar\":26,\"id\":22},{\"IdeAweAppPar\":27,\"id\":23},{\"IdeAweAppPar\":28,\"id\":24},{\"IdeAweAppPar\":29,\"id\":25},{\"IdeAweAppPar\":30,\"id\":26},{\"IdeAweAppPar\":31,\"id\":27},{\"IdeAweAppPar\":32,\"id\":28},{\"IdeAweAppPar\":33,\"id\":29},{\"IdeAweAppPar\":34,\"id\":30}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarGeFloat() throws Exception {
    String queryName = "FilterField-Var-GeFloat";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":35,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":11,\"id\":7},{\"IdeAweAppPar\":12,\"id\":8},{\"IdeAweAppPar\":13,\"id\":9},{\"IdeAweAppPar\":14,\"id\":10},{\"IdeAweAppPar\":15,\"id\":11},{\"IdeAweAppPar\":16,\"id\":12},{\"IdeAweAppPar\":17,\"id\":13},{\"IdeAweAppPar\":18,\"id\":14},{\"IdeAweAppPar\":19,\"id\":15},{\"IdeAweAppPar\":20,\"id\":16},{\"IdeAweAppPar\":21,\"id\":17},{\"IdeAweAppPar\":22,\"id\":18},{\"IdeAweAppPar\":23,\"id\":19},{\"IdeAweAppPar\":24,\"id\":20},{\"IdeAweAppPar\":25,\"id\":21},{\"IdeAweAppPar\":26,\"id\":22},{\"IdeAweAppPar\":27,\"id\":23},{\"IdeAweAppPar\":28,\"id\":24},{\"IdeAweAppPar\":29,\"id\":25},{\"IdeAweAppPar\":30,\"id\":26},{\"IdeAweAppPar\":31,\"id\":27},{\"IdeAweAppPar\":32,\"id\":28},{\"IdeAweAppPar\":33,\"id\":29},{\"IdeAweAppPar\":34,\"id\":30}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLe() throws Exception {
    String queryName = "FilterField-Var-Le";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7,\"id\":1},{\"IdeAweAppPar\":8,\"id\":2},{\"IdeAweAppPar\":9,\"id\":3},{\"IdeAweAppPar\":10,\"id\":4}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarGt() throws Exception {
    String queryName = "FilterField-Var-Gt";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":35,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":11,\"id\":7},{\"IdeAweAppPar\":12,\"id\":8},{\"IdeAweAppPar\":13,\"id\":9},{\"IdeAweAppPar\":14,\"id\":10},{\"IdeAweAppPar\":15,\"id\":11},{\"IdeAweAppPar\":16,\"id\":12},{\"IdeAweAppPar\":17,\"id\":13},{\"IdeAweAppPar\":18,\"id\":14},{\"IdeAweAppPar\":19,\"id\":15},{\"IdeAweAppPar\":20,\"id\":16},{\"IdeAweAppPar\":21,\"id\":17},{\"IdeAweAppPar\":22,\"id\":18},{\"IdeAweAppPar\":23,\"id\":19},{\"IdeAweAppPar\":24,\"id\":20},{\"IdeAweAppPar\":25,\"id\":21},{\"IdeAweAppPar\":26,\"id\":22},{\"IdeAweAppPar\":27,\"id\":23},{\"IdeAweAppPar\":28,\"id\":24},{\"IdeAweAppPar\":29,\"id\":25},{\"IdeAweAppPar\":30,\"id\":26},{\"IdeAweAppPar\":31,\"id\":27},{\"IdeAweAppPar\":32,\"id\":28},{\"IdeAweAppPar\":33,\"id\":29},{\"IdeAweAppPar\":34,\"id\":30}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLt() throws Exception {
    String queryName = "FilterField-Var-Lt";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7,\"id\":1},{\"IdeAweAppPar\":8,\"id\":2},{\"IdeAweAppPar\":9,\"id\":3},{\"IdeAweAppPar\":10,\"id\":4}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarIn() throws Exception {
    String queryName = "FilterField-Var-In";
    String variables = "\"list\":[0, 1]";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7,\"id\":1},{\"IdeAweAppPar\":8,\"id\":2},{\"IdeAweAppPar\":9,\"id\":3},{\"IdeAweAppPar\":10,\"id\":4}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarNotIn() throws Exception {
    String queryName = "FilterField-Var-NotIn";
    String variables = "\"list\":[0, 1]";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":35,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":11,\"id\":7},{\"IdeAweAppPar\":12,\"id\":8},{\"IdeAweAppPar\":13,\"id\":9},{\"IdeAweAppPar\":14,\"id\":10},{\"IdeAweAppPar\":15,\"id\":11},{\"IdeAweAppPar\":16,\"id\":12},{\"IdeAweAppPar\":17,\"id\":13},{\"IdeAweAppPar\":18,\"id\":14},{\"IdeAweAppPar\":19,\"id\":15},{\"IdeAweAppPar\":20,\"id\":16},{\"IdeAweAppPar\":21,\"id\":17},{\"IdeAweAppPar\":22,\"id\":18},{\"IdeAweAppPar\":23,\"id\":19},{\"IdeAweAppPar\":24,\"id\":20},{\"IdeAweAppPar\":25,\"id\":21},{\"IdeAweAppPar\":26,\"id\":22},{\"IdeAweAppPar\":27,\"id\":23},{\"IdeAweAppPar\":28,\"id\":24},{\"IdeAweAppPar\":29,\"id\":25},{\"IdeAweAppPar\":30,\"id\":26},{\"IdeAweAppPar\":31,\"id\":27},{\"IdeAweAppPar\":32,\"id\":28},{\"IdeAweAppPar\":33,\"id\":29},{\"IdeAweAppPar\":34,\"id\":30}]}}},{\"type\":\"end-load\"}]";
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldIsNull() throws Exception {
    String queryName = "FilterField-IsNull";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":21,\"rows\":[{\"IdeAweAppPar\":8,\"id\":1},{\"IdeAweAppPar\":16,\"id\":2},{\"IdeAweAppPar\":21,\"id\":3},{\"IdeAweAppPar\":22,\"id\":4},{\"IdeAweAppPar\":23,\"id\":5},{\"IdeAweAppPar\":24,\"id\":6},{\"IdeAweAppPar\":25,\"id\":7},{\"IdeAweAppPar\":26,\"id\":8},{\"IdeAweAppPar\":27,\"id\":9},{\"IdeAweAppPar\":28,\"id\":10},{\"IdeAweAppPar\":29,\"id\":11},{\"IdeAweAppPar\":30,\"id\":12},{\"IdeAweAppPar\":31,\"id\":13},{\"IdeAweAppPar\":32,\"id\":14},{\"IdeAweAppPar\":33,\"id\":15},{\"IdeAweAppPar\":34,\"id\":16},{\"IdeAweAppPar\":35,\"id\":17},{\"IdeAweAppPar\":36,\"id\":18},{\"IdeAweAppPar\":37,\"id\":19},{\"IdeAweAppPar\":38,\"id\":20},{\"IdeAweAppPar\":39,\"id\":21}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 21);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldIsNotNull() throws Exception {
    String queryName = "FilterField-IsNotNull";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":18,\"rows\":[{\"IdeAweAppPar\":5},{\"IdeAweAppPar\":10},{\"IdeAweAppPar\":12},{\"IdeAweAppPar\":13},{\"IdeAweAppPar\":14},{\"IdeAweAppPar\":15},{\"IdeAweAppPar\":17},{\"IdeAweAppPar\":18},{\"IdeAweAppPar\":20},{\"IdeAweAppPar\":1},{\"IdeAweAppPar\":2},{\"IdeAweAppPar\":3},{\"IdeAweAppPar\":4},{\"IdeAweAppPar\":6},{\"IdeAweAppPar\":7},{\"IdeAweAppPar\":9},{\"IdeAweAppPar\":11},{\"IdeAweAppPar\":19}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 18);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringL() throws Exception {
    String queryName = "FilterField-Var-LikeStringL";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"IdeAweAppPar\":6,\"id\":1},{\"IdeAweAppPar\":11,\"id\":2},{\"IdeAweAppPar\":12,\"id\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringR() throws Exception {
    String queryName = "FilterField-Var-LikeStringR";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":11,\"id\":1},{\"IdeAweAppPar\":12,\"id\":2},{\"IdeAweAppPar\":13,\"id\":3},{\"IdeAweAppPar\":14,\"id\":4}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringB() throws Exception {
    String queryName = "FilterField-Var-LikeStringB";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":18,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":7,\"id\":7},{\"IdeAweAppPar\":8,\"id\":8},{\"IdeAweAppPar\":9,\"id\":9},{\"IdeAweAppPar\":10,\"id\":10},{\"IdeAweAppPar\":11,\"id\":11},{\"IdeAweAppPar\":12,\"id\":12},{\"IdeAweAppPar\":13,\"id\":13},{\"IdeAweAppPar\":14,\"id\":14},{\"IdeAweAppPar\":15,\"id\":15},{\"IdeAweAppPar\":16,\"id\":16},{\"IdeAweAppPar\":17,\"id\":17},{\"IdeAweAppPar\":18,\"id\":18}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 18);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringLIgnoreCase() throws Exception {
    String queryName = "FilterField-Var-LikeStringLIgnoreCase";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"IdeAweAppPar\":6,\"id\":1},{\"IdeAweAppPar\":11,\"id\":2},{\"IdeAweAppPar\":12,\"id\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringRIgnoreCase() throws Exception {
    String queryName = "FilterField-Var-LikeStringRIgnoreCase";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":11,\"id\":1},{\"IdeAweAppPar\":12,\"id\":2},{\"IdeAweAppPar\":13,\"id\":3},{\"IdeAweAppPar\":14,\"id\":4}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldVarLikeStringBIgnoreCase() throws Exception {
    String queryName = "FilterField-Var-LikeStringBIgnoreCase";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":18,\"rows\":[{\"IdeAweAppPar\":1,\"id\":1},{\"IdeAweAppPar\":2,\"id\":2},{\"IdeAweAppPar\":3,\"id\":3},{\"IdeAweAppPar\":4,\"id\":4},{\"IdeAweAppPar\":5,\"id\":5},{\"IdeAweAppPar\":6,\"id\":6},{\"IdeAweAppPar\":7,\"id\":7},{\"IdeAweAppPar\":8,\"id\":8},{\"IdeAweAppPar\":9,\"id\":9},{\"IdeAweAppPar\":10,\"id\":10},{\"IdeAweAppPar\":11,\"id\":11},{\"IdeAweAppPar\":12,\"id\":12},{\"IdeAweAppPar\":13,\"id\":13},{\"IdeAweAppPar\":14,\"id\":14},{\"IdeAweAppPar\":15,\"id\":15},{\"IdeAweAppPar\":16,\"id\":16},{\"IdeAweAppPar\":17,\"id\":17},{\"IdeAweAppPar\":18,\"id\":18}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 18);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldFieldEq() throws Exception {
    String queryName = "FilterField-Field-Eq";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7,\"id\":1},{\"IdeAweAppPar\":8,\"id\":2},{\"IdeAweAppPar\":9,\"id\":3},{\"IdeAweAppPar\":10,\"id\":4}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldFieldEqCntTable() throws Exception {
    String queryName = "FilterField-Field-EqCntTable";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"IdeAweAppPar\":7,\"id\":1},{\"IdeAweAppPar\":8,\"id\":2},{\"IdeAweAppPar\":9,\"id\":3},{\"IdeAweAppPar\":10,\"id\":4}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterFieldSubquery() throws Exception {
    String queryName = "FilterField-Subquery";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"Nam\":\"Onate\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 1);
  }

  /**
   * Test of filter condition exists in query. Result is OK
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterExistsOK() throws Exception {
    testDatabaseRequest(
      "testExistsOK",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"abs\":1,\"name\":\"donald\",\"id\":1},{\"abs\":1,\"name\":\"jaimito\",\"id\":2},{\"abs\":1,\"name\":\"jorgito\",\"id\":3},{\"abs\":1,\"name\":\"juanito\",\"id\":4},{\"abs\":1,\"name\":\"test\",\"id\":5}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of filter condition exists in query. Result is KO
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterExistsKO() throws Exception {
    testDatabaseRequest(
      "testExistsKO",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      0);
  }

  /**
   * Test of filter condition exists in query. Result is OK
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterNotExistsOK() throws Exception {
    testDatabaseRequest(
      "testNotExistsOK",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1},{\"name\":\"jaimito\",\"id\":2},{\"name\":\"jorgito\",\"id\":3},{\"name\":\"juanito\",\"id\":4},{\"name\":\"test\",\"id\":5}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of filter condition exists in query. Result is KO
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryFilterNotExistsKO() throws Exception {
    testDatabaseRequest(
      "testNotExistsKO",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      0);
  }

  /**
   * Test of coalesce operation
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseOperationCoalesce() throws Exception {
    testDatabaseRequest(
      "testCoalesce",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"nameNotNull\":\"test\",\"name\":\"test\",\"id\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      1);
  }

  /**
   * Test of replace operation
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseOperationReplace() throws Exception {
    testDatabaseRequest(
      "testReplace",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"nameWithoutSpaces\":\"namewithspaces\",\"name\":\"test\",\"id\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      1);
  }

  /**
   * Test of adding numbers with strings (cast?)
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseOperationAddNumbers() throws Exception {
    testDatabaseRequest(
      "testCastToNumber",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"castToLong\":96,\"castToString\":\"12-21\",\"castToDouble\":4.9,\"castToInteger\":19,\"name\":\"test\",\"id\":1,\"castToFloat\":1.5}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      1);
  }

  /**
   * Test of add operation with computed field
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseOperationAddNumbersWithComputed() throws Exception {
    testDatabaseRequest(
            "testOperationWithComputed",
            "",
            "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"castToInteger\":19,\"name\":\"test\",\"id\":1,\"nom\":\"test19\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
            1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryOrderBy() throws Exception {
    testDatabaseRequest(
      "SimpleOrderBy",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":39,\"rows\":[{\"ParNam\":\"PwdPat\",\"Cat\":1,\"id\":1},{\"ParNam\":\"PwdMaxNumLog\",\"Cat\":1,\"id\":2},{\"ParNam\":\"PwdExp\",\"Cat\":1,\"id\":3},{\"ParNam\":\"MinPwd\",\"Cat\":1,\"id\":4},{\"ParNam\":\"Param9\",\"Cat\":2,\"id\":5},{\"ParNam\":\"Param8\",\"Cat\":2,\"id\":6},{\"ParNam\":\"Param7\",\"Cat\":2,\"id\":7},{\"ParNam\":\"Param6\",\"Cat\":2,\"id\":8},{\"ParNam\":\"Param5\",\"Cat\":2,\"id\":9},{\"ParNam\":\"Param4\",\"Cat\":2,\"id\":10},{\"ParNam\":\"Param3\",\"Cat\":2,\"id\":11},{\"ParNam\":\"Param2\",\"Cat\":2,\"id\":12},{\"ParNam\":\"Param19\",\"Cat\":2,\"id\":13},{\"ParNam\":\"Param18\",\"Cat\":2,\"id\":14},{\"ParNam\":\"Param17\",\"Cat\":2,\"id\":15},{\"ParNam\":\"Param16\",\"Cat\":2,\"id\":16},{\"ParNam\":\"Param15\",\"Cat\":2,\"id\":17},{\"ParNam\":\"Param14\",\"Cat\":2,\"id\":18},{\"ParNam\":\"Param13\",\"Cat\":2,\"id\":19},{\"ParNam\":\"Param12\",\"Cat\":2,\"id\":20},{\"ParNam\":\"Param11\",\"Cat\":2,\"id\":21},{\"ParNam\":\"Param10\",\"Cat\":2,\"id\":22},{\"ParNam\":\"Param1\",\"Cat\":2,\"id\":23},{\"ParNam\":\"MaxFntVer\",\"Cat\":2,\"id\":24},{\"ParNam\":\"MaxFntHor\",\"Cat\":2,\"id\":25},{\"ParNam\":\"DjrVerMar\",\"Cat\":2,\"id\":26},{\"ParNam\":\"DjrSubTitStl\",\"Cat\":2,\"id\":27},{\"ParNam\":\"DjrSepTck\",\"Cat\":2,\"id\":28},{\"ParNam\":\"DjrRmvLin\",\"Cat\":2,\"id\":29},{\"ParNam\":\"DjrRepPth\",\"Cat\":2,\"id\":30}]}}},{\"type\":\"end-load\"}]",
      30);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSimplePagination() throws Exception {
    String queryName = "SimplePagination";
    String variables = "\"max\": 5";
    String result = performRequest(queryName, variables, DATABASE);

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(8, dataList.get("total").asInt());
    assertEquals(1, dataList.get("page").asInt());
    assertEquals(39, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(5, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSimplePaginationPage1Max30() throws Exception {
    String queryName = "SimplePagination";
    String variables = "\"page\": 1, \"max\": 30";
    String result = performRequest(queryName, variables, DATABASE);

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(2, dataList.get("total").asInt());
    assertEquals(1, dataList.get("page").asInt());
    assertEquals(39, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(30, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSimplePaginationPage1Max10() throws Exception {
    String queryName = "SimplePagination";
    String variables = "\"page\": 1, \"max\": 10";
    String result = performRequest(queryName, variables, DATABASE);

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(4, dataList.get("total").asInt());
    assertEquals(1, dataList.get("page").asInt());
    assertEquals(39, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(10, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSimplePaginationPage2Max10() throws Exception {
    String queryName = "SimplePagination";
    String variables = "\"page\": 2, \"max\": 10";
    String result = performRequest(queryName, variables, DATABASE);

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(4, dataList.get("total").asInt());
    assertEquals(2, dataList.get("page").asInt());
    assertEquals(39, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(10, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");
  }

  /*
   *
   * TESTS FOR QUERIES ALREADY IN QUERIES.XML FILE
   *
   */

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryHaving() throws Exception {
    String queryName = "HavTstUni";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    logger.warn(result);
    ArrayNode resultList = assertResultJson(queryName, result, 1);
    assertEquals(1, resultList.size());
    assertEquals(16, resultList.get(0).get("sumAct").asInt());
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQrySitModDbsOrd() throws Exception {
    String queryName = "QrySitModDbsOrd";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"Ord\":1,\"Als\":\"aweora1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":6,\"NamMod\":\"Test\",\"IdeSitModDbs\":2580,\"IdeMod\":916},{\"Ord\":1,\"Als\":\"aweora1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":6,\"NamMod\":\"Base\",\"IdeSitModDbs\":75,\"IdeMod\":28},{\"Ord\":1,\"Als\":\"aweora2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":7,\"NamMod\":\"Test\",\"IdeSitModDbs\":2579,\"IdeMod\":916},{\"Ord\":1,\"Als\":\"aweora2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":7,\"NamMod\":\"Base\",\"IdeSitModDbs\":60,\"IdeMod\":28},{\"Ord\":2,\"Als\":\"awesqs1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":8,\"NamMod\":\"Test\",\"IdeSitModDbs\":2582,\"IdeMod\":916},{\"Ord\":2,\"Als\":\"awesqs1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":8,\"NamMod\":\"Base\",\"IdeSitModDbs\":76,\"IdeMod\":28},{\"Ord\":3,\"Als\":\"awesybase1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":9,\"NamMod\":\"Test\",\"IdeSitModDbs\":2584,\"IdeMod\":916},{\"Ord\":3,\"Als\":\"awesybase1\",\"NamSit\":\"Madrid\",\"IdeSit\":10,\"IdeDbs\":9,\"NamMod\":\"Base\",\"IdeSitModDbs\":77,\"IdeMod\":28},{\"Ord\":2,\"Als\":\"awesqs2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":15,\"NamMod\":\"Test\",\"IdeSitModDbs\":2581,\"IdeMod\":916},{\"Ord\":2,\"Als\":\"awesqs2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":15,\"NamMod\":\"Base\",\"IdeSitModDbs\":78,\"IdeMod\":28},{\"Ord\":3,\"Als\":\"awesybase2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":16,\"NamMod\":\"Test\",\"IdeSitModDbs\":2583,\"IdeMod\":916},{\"Ord\":3,\"Als\":\"awesybase2\",\"NamSit\":\"Onate\",\"IdeSit\":17,\"IdeDbs\":16,\"NamMod\":\"Base\",\"IdeSitModDbs\":79,\"IdeMod\":28}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQrySitModDbsOrdTot() throws Exception {
    String queryName = "QrySitModDbsOrdTot";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":25,\"rows\":[{\"IdeSitModDbs\":2580,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1\",\"IdeMod\":916,\"IdeDbs\":6,\"NamMod\":\"Test\",\"id\":1},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-1\"},{\"IdeSitModDbs\":75,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1\",\"IdeMod\":28,\"IdeDbs\":6,\"NamMod\":\"Base\",\"id\":2},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-3\"},{\"IdeSitModDbs\":2579,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1\",\"IdeMod\":916,\"IdeDbs\":7,\"NamMod\":\"Test\",\"id\":3},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-5\"},{\"IdeSitModDbs\":60,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1\",\"IdeMod\":28,\"IdeDbs\":7,\"NamMod\":\"Base\",\"id\":4},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-7\"},{\"IdeSitModDbs\":2582,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2\",\"IdeMod\":916,\"IdeDbs\":8,\"NamMod\":\"Test\",\"id\":5},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-9\"},{\"IdeSitModDbs\":76,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2\",\"IdeMod\":28,\"IdeDbs\":8,\"NamMod\":\"Base\",\"id\":6},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-11\"},{\"IdeSitModDbs\":2584,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3\",\"IdeMod\":916,\"IdeDbs\":9,\"NamMod\":\"Test\",\"id\":7},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-13\"},{\"IdeSitModDbs\":77,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3\",\"IdeMod\":28,\"IdeDbs\":9,\"NamMod\":\"Base\",\"id\":8},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-15\"},{\"IdeSitModDbs\":2581,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2\",\"IdeMod\":916,\"IdeDbs\":15,\"NamMod\":\"Test\",\"id\":9},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-17\"},{\"IdeSitModDbs\":78,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2\",\"IdeMod\":28,\"IdeDbs\":15,\"NamMod\":\"Base\",\"id\":10},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-19\"},{\"IdeSitModDbs\":2583,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3\",\"IdeMod\":916,\"IdeDbs\":16,\"NamMod\":\"Test\",\"id\":11},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-21\"},{\"IdeSitModDbs\":79,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3\",\"IdeMod\":28,\"IdeDbs\":16,\"NamMod\":\"Base\",\"id\":12},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-23\"},{\"IdeSitModDbs\":null,\"Als3\":\"Total\",\"_style_\":\"TOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"24\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":null,\"id\":\"TOT-24\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 25);
    logger.debug(expected);
    logger.debug(result);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryUniTst() throws Exception {
    testDatabaseRequest(
      "QryUniTst",
      "",
      null,
      6);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryUniTstId() throws Exception {
    testDatabaseRequest(
      "QryUniTstId",
      "",
      null,
      6);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryChkPrg() throws Exception {
    testDatabaseRequest(
      "QryChkPrg",
      "",
      null,
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryEdiTst() throws Exception {
    testDatabaseRequest(
      "QryEdiTst",
      "",
      null,
      16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryEdiTstWithoutLimit() throws Exception {
    testDatabaseRequest(
      "QryEdiTst",
      "\"max\": 0",
      null,
      16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryEdiTstChk() throws Exception {
    testDatabaseRequest(
      "QryEdiTstChk",
      "",
      null,
      16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryQryEdiSug() throws Exception {
    testDatabaseRequest(
      "QryEdiSug",
      "\"suggest\": \"fr\"",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"name\":\"frost\",\"value\":7,\"label\":\"Prueba - frost\"},{\"name\":\"fresh\",\"value\":8,\"label\":\"Prueba - fresh\"}]}}},{\"type\":\"end-load\"}]",
      2);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseCriteriaDate() throws Exception {
    testDatabaseRequest(
      "CrtTstDat",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"TxtRea\":1000000.12123},{\"TxtRea\":1000000.12123},{\"TxtRea\":1000000.12123},{\"TxtRea\":1000000.12123}]}}},{\"type\":\"end-load\"}]",
      4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstUsrSug() throws Exception {
    testDatabaseRequest(
      "TstUsrSug",
      "\"suggest\": \"ito\"",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"label\":\"jaimito (Jaimito)\",\"id\":1,\"value\":\"jaimito\",\"nom\":\"Jaimito\"},{\"label\":\"jorgito (Jorgito)\",\"id\":2,\"value\":\"jorgito\",\"nom\":\"Jorgito\"},{\"label\":\"juanito (Juanito)\",\"id\":3,\"value\":\"juanito\",\"nom\":\"Juanito\"}]}}},{\"type\":\"end-load\"}]",
      3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseUserSuggest() throws Exception {
    testDatabaseRequest(
      "TstUsrSugIde",
      "\"suggest\": 1",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"label\":1,\"id\":1,\"value\":1},{\"label\":811,\"id\":2,\"value\":811},{\"label\":1702,\"id\":3,\"value\":1702}]}}},{\"type\":\"end-load\"}]",
      3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstUsrSel() throws Exception {
    testDatabaseRequest(
      "TstUsrSel",
      "\"suggest\": \"jaimito\"",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"label\":\"jaimito (Jaimito)\",\"id\":1,\"value\":\"jaimito\",\"nom\":\"Jaimito\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstChrOneDatSrc() throws Exception {
    String queryName = "TstChrOneDatSrc";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 32, 951);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstChrOneDatSrcPagination() throws Exception {
    String queryName = "TstChrOneDatSrcPagination";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 32, 951);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstChrOTwoDatSrc() throws Exception {
    String queryName = "TstChrTwoDatSrc";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 32, 951);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstChrOTwoDatSrcPagination() throws Exception {
    String queryName = "TstChrTwoDatSrcPagination";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 32, 951);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTstChrTwoSrcLab() throws Exception {
    String queryName = "TstChrTwoSrcLab";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 32, 950);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSugTstLst() throws Exception {
    String queryName = "SugTstLst";
    String variables = "\"suggest\": \"jaimito\"";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 200, 6000);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSugTstLstPagination() throws Exception {
    String queryName = "SugTstLstPagination";
    String variables = "\"suggest\": \"jaimito\"";

    String result = performRequest(queryName, variables, DATABASE);
    assertResultJson(queryName, result, 30, 1, 200, 6000);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseProModTrePro() throws Exception {
    String queryName = "ProModTrePro";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":15,\"rows\":[{\"parent\":\"\",\"TreGrd_id\":\"Proadministrator\",\"TreGrdEdi_parent\":\"\",\"TreGrd_lev\":{\"value\":\"1\",\"label\":\"1\",\"style\":\"text-danger\"},\"TreGrd_parent\":\"\",\"isLeaf\":0,\"TreGrd_Nam\":\"administrator\",\"id\":\"Proadministrator\",\"Nam\":\"administrator\",\"TreGrdEdi_Nam\":\"administrator\",\"Lev\":1,\"TreGrdEdi_id\":\"Proadministrator\",\"TreGrdEdi_lev\":\"1\"},{\"parent\":\"Proadministrator\",\"TreGrd_id\":\"Proadministrator-ModBase\",\"TreGrdEdi_parent\":\"Proadministrator\",\"TreGrd_lev\":{\"value\":\"2\",\"label\":\"2\",\"style\":\"text-success\"},\"TreGrd_parent\":\"Proadministrator\",\"isLeaf\":0,\"TreGrd_Nam\":\"Base\",\"id\":\"Proadministrator-ModBase\",\"Nam\":\"Base\",\"TreGrdEdi_Nam\":\"Base\",\"Lev\":2,\"TreGrdEdi_id\":\"Proadministrator-ModBase\",\"TreGrdEdi_lev\":\"2\"},{\"parent\":\"Proadministrator-ModBase\",\"TreGrd_id\":\"Proadministrator-ModBase-SitMadrid\",\"TreGrdEdi_parent\":\"Proadministrator-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Proadministrator-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Madrid\",\"id\":\"Proadministrator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"TreGrdEdi_Nam\":\"Madrid\",\"Lev\":3,\"TreGrdEdi_id\":\"Proadministrator-ModBase-SitMadrid\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"Proadministrator-ModBase\",\"TreGrd_id\":\"Proadministrator-ModBase-SitOnate\",\"TreGrdEdi_parent\":\"Proadministrator-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Proadministrator-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Onate\",\"id\":\"Proadministrator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"TreGrdEdi_Nam\":\"Onate\",\"Lev\":3,\"TreGrdEdi_id\":\"Proadministrator-ModBase-SitOnate\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"Proadministrator\",\"TreGrd_id\":\"Proadministrator-ModTest\",\"TreGrdEdi_parent\":\"Proadministrator\",\"TreGrd_lev\":{\"value\":\"2\",\"label\":\"2\",\"style\":\"text-success\"},\"TreGrd_parent\":\"Proadministrator\",\"isLeaf\":0,\"TreGrd_Nam\":\"Test\",\"id\":\"Proadministrator-ModTest\",\"Nam\":\"Test\",\"TreGrdEdi_Nam\":\"Test\",\"Lev\":2,\"TreGrdEdi_id\":\"Proadministrator-ModTest\",\"TreGrdEdi_lev\":\"2\"},{\"parent\":\"Proadministrator-ModTest\",\"TreGrd_id\":\"Proadministrator-ModTest-SitMadrid\",\"TreGrdEdi_parent\":\"Proadministrator-ModTest\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Proadministrator-ModTest\",\"isLeaf\":1,\"TreGrd_Nam\":\"Madrid\",\"id\":\"Proadministrator-ModTest-SitMadrid\",\"Nam\":\"Madrid\",\"TreGrdEdi_Nam\":\"Madrid\",\"Lev\":3,\"TreGrdEdi_id\":\"Proadministrator-ModTest-SitMadrid\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"Proadministrator-ModTest\",\"TreGrd_id\":\"Proadministrator-ModTest-SitOnate\",\"TreGrdEdi_parent\":\"Proadministrator-ModTest\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Proadministrator-ModTest\",\"isLeaf\":1,\"TreGrd_Nam\":\"Onate\",\"id\":\"Proadministrator-ModTest-SitOnate\",\"Nam\":\"Onate\",\"TreGrdEdi_Nam\":\"Onate\",\"Lev\":3,\"TreGrdEdi_id\":\"Proadministrator-ModTest-SitOnate\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"\",\"TreGrd_id\":\"Progeneral\",\"TreGrdEdi_parent\":\"\",\"TreGrd_lev\":{\"value\":\"1\",\"label\":\"1\",\"style\":\"text-danger\"},\"TreGrd_parent\":\"\",\"isLeaf\":0,\"TreGrd_Nam\":\"general\",\"id\":\"Progeneral\",\"Nam\":\"general\",\"TreGrdEdi_Nam\":\"general\",\"Lev\":1,\"TreGrdEdi_id\":\"Progeneral\",\"TreGrdEdi_lev\":\"1\"},{\"parent\":\"Progeneral\",\"TreGrd_id\":\"Progeneral-ModBase\",\"TreGrdEdi_parent\":\"Progeneral\",\"TreGrd_lev\":{\"value\":\"2\",\"label\":\"2\",\"style\":\"text-success\"},\"TreGrd_parent\":\"Progeneral\",\"isLeaf\":0,\"TreGrd_Nam\":\"Base\",\"id\":\"Progeneral-ModBase\",\"Nam\":\"Base\",\"TreGrdEdi_Nam\":\"Base\",\"Lev\":2,\"TreGrdEdi_id\":\"Progeneral-ModBase\",\"TreGrdEdi_lev\":\"2\"},{\"parent\":\"Progeneral-ModBase\",\"TreGrd_id\":\"Progeneral-ModBase-SitMadrid\",\"TreGrdEdi_parent\":\"Progeneral-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Progeneral-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Madrid\",\"id\":\"Progeneral-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"TreGrdEdi_Nam\":\"Madrid\",\"Lev\":3,\"TreGrdEdi_id\":\"Progeneral-ModBase-SitMadrid\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"Progeneral-ModBase\",\"TreGrd_id\":\"Progeneral-ModBase-SitOnate\",\"TreGrdEdi_parent\":\"Progeneral-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Progeneral-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Onate\",\"id\":\"Progeneral-ModBase-SitOnate\",\"Nam\":\"Onate\",\"TreGrdEdi_Nam\":\"Onate\",\"Lev\":3,\"TreGrdEdi_id\":\"Progeneral-ModBase-SitOnate\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"\",\"TreGrd_id\":\"Prooperator\",\"TreGrdEdi_parent\":\"\",\"TreGrd_lev\":{\"value\":\"1\",\"label\":\"1\",\"style\":\"text-danger\"},\"TreGrd_parent\":\"\",\"isLeaf\":0,\"TreGrd_Nam\":\"operator\",\"id\":\"Prooperator\",\"Nam\":\"operator\",\"TreGrdEdi_Nam\":\"operator\",\"Lev\":1,\"TreGrdEdi_id\":\"Prooperator\",\"TreGrdEdi_lev\":\"1\"},{\"parent\":\"Prooperator\",\"TreGrd_id\":\"Prooperator-ModBase\",\"TreGrdEdi_parent\":\"Prooperator\",\"TreGrd_lev\":{\"value\":\"2\",\"label\":\"2\",\"style\":\"text-success\"},\"TreGrd_parent\":\"Prooperator\",\"isLeaf\":0,\"TreGrd_Nam\":\"Base\",\"id\":\"Prooperator-ModBase\",\"Nam\":\"Base\",\"TreGrdEdi_Nam\":\"Base\",\"Lev\":2,\"TreGrdEdi_id\":\"Prooperator-ModBase\",\"TreGrdEdi_lev\":\"2\"},{\"parent\":\"Prooperator-ModBase\",\"TreGrd_id\":\"Prooperator-ModBase-SitMadrid\",\"TreGrdEdi_parent\":\"Prooperator-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Prooperator-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Madrid\",\"id\":\"Prooperator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"TreGrdEdi_Nam\":\"Madrid\",\"Lev\":3,\"TreGrdEdi_id\":\"Prooperator-ModBase-SitMadrid\",\"TreGrdEdi_lev\":\"3\"},{\"parent\":\"Prooperator-ModBase\",\"TreGrd_id\":\"Prooperator-ModBase-SitOnate\",\"TreGrdEdi_parent\":\"Prooperator-ModBase\",\"TreGrd_lev\":{\"value\":\"3\",\"label\":\"3\",\"style\":\"\"},\"TreGrd_parent\":\"Prooperator-ModBase\",\"isLeaf\":1,\"TreGrd_Nam\":\"Onate\",\"id\":\"Prooperator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"TreGrdEdi_Nam\":\"Onate\",\"Lev\":3,\"TreGrdEdi_id\":\"Prooperator-ModBase-SitOnate\",\"TreGrdEdi_lev\":\"3\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 15, 1, 1, 15);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseProModTreProLoa() throws Exception {
    String queryName = "ProModTreProLoa";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"TreGrdEdiLoa_lev\":\"4\",\"parent\":\"\",\"TreGrdLoa_lev\":\"2\",\"TreGrdLoaEdi_parent\":\"\",\"TreGrdLoa_id\":\"Proadministrator\",\"isLeaf\":0,\"TreGrdLoa_Nam\":\"administrator\",\"TreGrdLoaEdi_Nam\":\"administrator\",\"id\":\"Proadministrator\",\"Nam\":\"administrator\",\"TreGrdLoa_parent\":\"\",\"TreGrdLoaEdi_id\":\"Proadministrator\"},{\"TreGrdEdiLoa_lev\":\"4\",\"parent\":\"\",\"TreGrdLoa_lev\":\"2\",\"TreGrdLoaEdi_parent\":\"\",\"TreGrdLoa_id\":\"Progeneral\",\"isLeaf\":0,\"TreGrdLoa_Nam\":\"general\",\"TreGrdLoaEdi_Nam\":\"general\",\"id\":\"Progeneral\",\"Nam\":\"general\",\"TreGrdLoa_parent\":\"\",\"TreGrdLoaEdi_id\":\"Progeneral\"},{\"TreGrdEdiLoa_lev\":\"4\",\"parent\":\"\",\"TreGrdLoa_lev\":\"2\",\"TreGrdLoaEdi_parent\":\"\",\"TreGrdLoa_id\":\"Prooperator\",\"isLeaf\":0,\"TreGrdLoa_Nam\":\"operator\",\"TreGrdLoaEdi_Nam\":\"operator\",\"id\":\"Prooperator\",\"Nam\":\"operator\",\"TreGrdLoa_parent\":\"\",\"TreGrdLoaEdi_id\":\"Prooperator\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 3, 1, 1, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseProModTreProBas() throws Exception {
    String queryName = "ProModTreProBas";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":15,\"rows\":[{\"parent\":\"\",\"id\":\"Proadministrator\",\"Nam\":\"administrator\",\"Lev\":1,\"isLeaf\":0},{\"parent\":\"Proadministrator\",\"id\":\"Proadministrator-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Proadministrator-ModBase\",\"id\":\"Proadministrator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator-ModBase\",\"id\":\"Proadministrator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator\",\"id\":\"Proadministrator-ModTest\",\"Nam\":\"Test\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Proadministrator-ModTest\",\"id\":\"Proadministrator-ModTest-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator-ModTest\",\"id\":\"Proadministrator-ModTest-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"\",\"id\":\"Progeneral\",\"Nam\":\"general\",\"Lev\":1,\"isLeaf\":0},{\"parent\":\"Progeneral\",\"id\":\"Progeneral-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Progeneral-ModBase\",\"id\":\"Progeneral-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Progeneral-ModBase\",\"id\":\"Progeneral-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"\",\"id\":\"Prooperator\",\"Nam\":\"operator\",\"Lev\":1,\"isLeaf\":0},{\"parent\":\"Prooperator\",\"id\":\"Prooperator-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Prooperator-ModBase\",\"id\":\"Prooperator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Prooperator-ModBase\",\"id\":\"Prooperator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 15, 1, 1, 15);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseProModTreMod() throws Exception {
    String queryName = "ProModTreMod";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"parent\":\"Proadministrator\",\"id\":\"Proadministrator-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Proadministrator\",\"id\":\"Proadministrator-ModTest\",\"Nam\":\"Test\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Progeneral\",\"id\":\"Progeneral-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0},{\"parent\":\"Prooperator\",\"id\":\"Prooperator-ModBase\",\"Nam\":\"Base\",\"Lev\":2,\"isLeaf\":0}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 4, 1, 1, 4);
    logger.debug(expected);
    logger.debug(result);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseProSitTreMod() throws Exception {
    String queryName = "ProSitTreMod";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":8,\"rows\":[{\"parent\":\"Proadministrator-ModBase\",\"id\":\"Proadministrator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator-ModBase\",\"id\":\"Proadministrator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator-ModTest\",\"id\":\"Proadministrator-ModTest-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Proadministrator-ModTest\",\"id\":\"Proadministrator-ModTest-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Progeneral-ModBase\",\"id\":\"Progeneral-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Progeneral-ModBase\",\"id\":\"Progeneral-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Prooperator-ModBase\",\"id\":\"Prooperator-ModBase-SitMadrid\",\"Nam\":\"Madrid\",\"Lev\":3,\"isLeaf\":1},{\"parent\":\"Prooperator-ModBase\",\"id\":\"Prooperator-ModBase-SitOnate\",\"Nam\":\"Onate\",\"Lev\":3,\"isLeaf\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultJson(queryName, result, 8, 1, 1, 8);
    logger.debug(expected);
    logger.debug(result);
  }

  // *****************************************************************************************************************//
  // CASE WHEN ELSE TESTS
  // **************************************************************************************************************** //

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testCaseWhenElseDistinct() throws Exception {
    String queryName = "testCaseWhenElseDistinct";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"other\":3,\"another\":\"purple-hills\",\"label\":null,\"id\":1,\"value\":null},{\"other\":3,\"another\":\"sunset\",\"label\":\"SUNSET\",\"id\":2,\"value\":1},{\"other\":3,\"another\":\"purple-hills\",\"label\":\"3333\",\"id\":3,\"value\":2},{\"other\":3,\"another\":\"purple-hills\",\"label\":\"PURPLE-HILLS\",\"id\":4,\"value\":3}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(expected);
    logger.warn(result);
    assertResultJson(queryName, result, 4, 1, 1, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testCaseWhenElse() throws Exception {
    String queryName = "testCaseWhenElse";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":\"other\",\"id\":1,\"value\":0},{\"label\":\"other\",\"id\":2,\"value\":0},{\"label\":\"other\",\"id\":3,\"value\":0},{\"label\":\"other\",\"id\":4,\"value\":0},{\"label\":\"other\",\"id\":5,\"value\":0},{\"label\":\"other\",\"id\":6,\"value\":0},{\"label\":\"other\",\"id\":7,\"value\":0},{\"label\":\"other\",\"id\":8,\"value\":0},{\"label\":\"other\",\"id\":9,\"value\":0},{\"label\":\"other\",\"id\":10,\"value\":0},{\"label\":\"PURPLE-HILLS\",\"id\":11,\"value\":3},{\"label\":\"other\",\"id\":12,\"value\":0},{\"label\":\"other\",\"id\":13,\"value\":0},{\"label\":\"SUNNY\",\"id\":14,\"value\":2},{\"label\":\"SUNSET\",\"id\":15,\"value\":1},{\"label\":\"other\",\"id\":16,\"value\":0}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(expected);
    logger.warn(result);
    assertResultJson(queryName, result, 16, 1, 1, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testCaseWhenElseInFilter() throws Exception {
    String queryName = "testCaseWhenElseInFilter";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"label\":\"SUNSET\",\"id\":1,\"value\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(expected);
    logger.warn(result);
    assertResultJson(queryName, result, 1, 1, 1, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDiffDates() throws Exception {
    String queryName = "testDiffDates";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"diffWeeks\":14,\"diffMonths\":252,\"diffDays\":6,\"diffYears\":34,\"name\":\"donald\",\"id\":1,\"diffSeconds\":24648},{\"diffWeeks\":14,\"diffMonths\":252,\"diffDays\":6,\"diffYears\":34,\"name\":\"jaimito\",\"id\":2,\"diffSeconds\":24648},{\"diffWeeks\":14,\"diffMonths\":252,\"diffDays\":6,\"diffYears\":34,\"name\":\"jorgito\",\"id\":3,\"diffSeconds\":24648},{\"diffWeeks\":14,\"diffMonths\":252,\"diffDays\":6,\"diffYears\":34,\"name\":\"juanito\",\"id\":4,\"diffSeconds\":24648},{\"diffWeeks\":14,\"diffMonths\":252,\"diffDays\":6,\"diffYears\":34,\"name\":\"test\",\"id\":5,\"diffSeconds\":24648}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(expected);
    logger.warn(result);
    assertResultJson(queryName, result, 5, 1, 1, 5);
  }

  // *****************************************************************************************************************//
  // QUERY RESULT TESTS
  // **************************************************************************************************************** //

  private void assertQueryResultJson(String queryName, String result, int expectedRows) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(1, dataList.get("total").asInt());
    assertEquals(1, dataList.get("page").asInt());
    assertEquals(expectedRows, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(expectedRows, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("--------------------------------------------------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformBoolean() throws Exception {
    String queryName = "TransformBoolean";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"boolean2\":\"false\",\"boolean3\":\"true\",\"boolean1\":\"true\",\"id\":1,\"boolean4\":\"false\"},{\"boolean2\":\"false\",\"boolean3\":\"true\",\"boolean1\":\"true\",\"id\":2,\"boolean4\":\"false\"},{\"boolean2\":\"false\",\"boolean3\":\"true\",\"boolean1\":\"true\",\"id\":3,\"boolean4\":\"false\"},{\"boolean2\":\"false\",\"boolean3\":\"true\",\"boolean1\":\"true\",\"id\":4,\"boolean4\":\"false\"},{\"boolean2\":\"false\",\"boolean3\":\"true\",\"boolean1\":\"true\",\"id\":5,\"boolean4\":\"false\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformNumber() throws Exception {
    String queryName = "TransformNumber";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"number\":\"811,00\",\"id\":4,\"value\":\"juanito\",\"email\":\"juanito@test.com\"},{\"number\":\"1.702,00\",\"id\":2,\"value\":\"jaimito\",\"email\":\"jaimito@test.com\"},{\"number\":\"1,00\",\"id\":5,\"value\":\"test\",\"email\":\"test@test.com\"},{\"number\":\"3,00\",\"id\":3,\"value\":\"jorgito\",\"email\":\"jorgito@test.com\"},{\"number\":\"2,00\",\"id\":1,\"value\":\"donald\",\"email\":\"donald@test.com\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformNumberPlain() throws Exception {
    String queryName = "TransformNumberPlain";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"number\":811,\"number_transformed\":811.0,\"id\":4,\"value\":\"juanito\",\"email\":\"juanito@test.com\"},{\"number\":1702,\"number_transformed\":1702.0,\"id\":2,\"value\":\"jaimito\",\"email\":\"jaimito@test.com\"},{\"number\":1,\"number_transformed\":1.0,\"id\":5,\"value\":\"test\",\"email\":\"test@test.com\"},{\"number\":3,\"number_transformed\":3.0,\"id\":3,\"value\":\"jorgito\",\"email\":\"jorgito@test.com\"},{\"number\":2,\"number_transformed\":2.0,\"id\":1,\"value\":\"donald\",\"email\":\"donald@test.com\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDate() throws Exception {
    String queryName = "TransformDate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"04/11/2013\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"23/10/2013\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDateService() throws Exception {
    String queryName = "TransformDateService";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"date6\":\"15:06:23\",\"date5\":\"01/10/1978\",\"date4\":\"01/10/1978 15:06:23\",\"date3\":\"10-JAN-1978\",\"id\":1,\"date1\":\"10/01/1978\"},{\"date6\":\"03:30:12\",\"date5\":\"01/02/2015\",\"date4\":\"01/02/2015 03:30:12\",\"date3\":\"02-JAN-2015\",\"id\":2,\"date1\":\"02/01/2015\"},{\"date6\":\"13:26:55\",\"date5\":\"01/08/2020\",\"date4\":\"01/08/2020 13:26:55\",\"date3\":\"08-JAN-2020\",\"id\":3,\"date1\":\"08/01/2020\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTimestampService() throws Exception {
    String queryName = "TransformTimestampService";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"date2\":\"10/01/1978 15:06:23.232\",\"date1\":\"10/01/1978 15:06:23\"},{\"date2\":\"02/01/2015 03:30:12.123\",\"date1\":\"02/01/2015 03:30:12\"},{\"date2\":\"08/01/2020 13:26:55.111\",\"date1\":\"08/01/2020 13:26:55\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformElapsedTimeService() throws Exception {
    String queryName = "TransformElapsedTimeService";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"years\":\"3 years\",\"year\":\"1 year\",\"months\":\"2 months\",\"month\":\"1 month\",\"weeks\":\"2 weeks\",\"week\":\"1 week\",\"days\":\"3d\",\"hours\":\"8h\",\"minutes\":\"5m\",\"seconds\":\"7s\",\"milliseconds\":\"222ms\", \"dateSince\":\"3 years ago\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDateJavascript() throws Exception {
    String queryName = "TransformJavascriptDate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"11/04/2013\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"10/23/2013\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDateGeneric() throws Exception {
    String queryName = "TransformGenericDate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"04/11/2013 8:57\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"23/10/2013 16:02\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDateMilliseconds() throws Exception {
    String queryName = "TransformDateMilliseconds";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":1383555422000,\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":1382544122000,\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    assertQueryResultJson(queryName, result, 5);

    ArrayNode expectedList = (ArrayNode) objectMapper.readTree(expected);
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode fillExpectedAction = (ObjectNode) expectedList.get(0);
    ObjectNode fillResultAction = (ObjectNode) resultList.get(0);
    ObjectNode expectedParameters = (ObjectNode) fillExpectedAction.get("parameters");
    ObjectNode resultParameters = (ObjectNode) fillResultAction.get("parameters");
    ObjectNode expectedDatalist = (ObjectNode) expectedParameters.get("datalist");
    ObjectNode resultDatalist = (ObjectNode) resultParameters.get("datalist");
    ArrayNode expectedRows = (ArrayNode) expectedDatalist.get("rows");
    ArrayNode resultRows = (ArrayNode) resultDatalist.get("rows");
    ObjectNode expectedRow1 = (ObjectNode) expectedRows.get(2);
    ObjectNode resultRow1 = (ObjectNode) resultRows.get(4);
    ObjectNode expectedRow2 = (ObjectNode) expectedRows.get(4);
    ObjectNode resultRow2 = (ObjectNode) resultRows.get(0);

    assert (Math.abs(expectedRow1.get("fecha").asLong() - resultRow1.get("fecha").asLong()) < 7200001);
    assert (Math.abs(expectedRow2.get("fecha").asLong() - resultRow2.get("fecha").asLong()) < 7200001);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformDateRDB() throws Exception {
    String queryName = "TransformDateRDB";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":\"23-OCT-2013\",\"id\":1},{\"fecha\":null,\"id\":2},{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":4},{\"fecha\":\"04-NOV-2013\",\"id\":5}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE);
    logger.warn(result);
    assertQueryResultJson(queryName, result, 5);

    ArrayNode expectedList = (ArrayNode) objectMapper.readTree(expected);
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode fillExpectedAction = (ObjectNode) expectedList.get(0);
    ObjectNode fillResultAction = (ObjectNode) resultList.get(0);
    ObjectNode expectedParameters = (ObjectNode) fillExpectedAction.get("parameters");
    ObjectNode resultParameters = (ObjectNode) fillResultAction.get("parameters");
    ObjectNode expectedDatalist = (ObjectNode) expectedParameters.get("datalist");
    ObjectNode resultDatalist = (ObjectNode) resultParameters.get("datalist");
    ArrayNode expectedRows = (ArrayNode) expectedDatalist.get("rows");
    ArrayNode resultRows = (ArrayNode) resultDatalist.get("rows");
    ObjectNode expectedRow1 = (ObjectNode) expectedRows.get(0);
    ObjectNode resultRow1 = (ObjectNode) resultRows.get(0);
    ObjectNode expectedRow2 = (ObjectNode) expectedRows.get(4);
    ObjectNode resultRow2 = (ObjectNode) resultRows.get(4);

    assertEquals(expectedRow1.get("fecha").textValue().toUpperCase(), resultRow1.get("fecha").textValue().toUpperCase());
    assertEquals(expectedRow2.get("fecha").textValue().toUpperCase(), resultRow2.get("fecha").textValue().toUpperCase());
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTime() throws Exception {
    String queryName = "TransformTime";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"08:57:02\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"16:02:02\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTimestamp() throws Exception {
    String queryName = "TransformTimestamp";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"04/11/2013 08:57:02\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"23/10/2013 16:02:02\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTimestampJavascript() throws Exception {
    String queryName = "TransformJavascriptTimestamp";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"fecha\":null,\"id\":3},{\"fecha\":null,\"id\":2},{\"fecha\":\"11/04/2013 08:57:02\",\"id\":5},{\"fecha\":null,\"id\":4},{\"fecha\":\"10/23/2013 16:02:02\",\"id\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformEncryptDecrypt() throws Exception {
    String queryName = "TransformEncryptDecrypt";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"codigo\":\"T81NvNhwVLs=\",\"descodificado\":\"hola\",\"nombre\":\"juanito\"},{\"codigo\":\"T81NvNhwVLs=\",\"descodificado\":\"hola\",\"nombre\":\"jaimito\"},{\"codigo\":\"T81NvNhwVLs=\",\"descodificado\":\"hola\",\"nombre\":\"test\"},{\"codigo\":\"T81NvNhwVLs=\",\"descodificado\":\"hola\",\"nombre\":\"jorgito\"},{\"codigo\":\"T81NvNhwVLs=\",\"descodificado\":\"hola\",\"nombre\":\"donald\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformArrayAndList() throws Exception {
    String queryName = "TransformArrayAndList";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"lArrayNode\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"id\":1,\"lArrayList\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"nombre\":\"donald\"},{\"lArrayNode\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"id\":2,\"lArrayList\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"nombre\":\"jaimito\"},{\"lArrayNode\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"id\":3,\"lArrayList\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"nombre\":\"jorgito\"},{\"lArrayNode\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"id\":4,\"lArrayList\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"nombre\":\"juanito\"},{\"lArrayNode\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"id\":5,\"lArrayList\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"],\"nombre\":\"test\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(result);
    logger.warn(expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTextHTML() throws Exception {
    String queryName = "TransformTextHtml";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"text\":\"&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;\",\"nombre\":\"juanito\"},{\"text\":\"&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;\",\"nombre\":\"jaimito\"},{\"text\":\"&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;\",\"nombre\":\"test\"},{\"text\":\"&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;\",\"nombre\":\"jorgito\"},{\"text\":\"&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;<br/>&lt;a href='tutu'&gt;aaa&lt;/a&gt;\",\"nombre\":\"donald\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTextPlain() throws Exception {
    String queryName = "TransformTextPlain";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"text\":\"<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\",\"nombre\":\"juanito\"},{\"text\":\"<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\",\"nombre\":\"jaimito\"},{\"text\":\"<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\",\"nombre\":\"test\"},{\"text\":\"<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\",\"nombre\":\"jorgito\"},{\"text\":\"<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\\n<a href='tutu'>aaa</a>\",\"nombre\":\"donald\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTransformTextUniline() throws Exception {
    String queryName = "TransformTextUniline";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"text\":\"<a href='tutu'>aaa</a> <a href='tutu'>aaa</a> <a href='tutu'>aaa</a>\",\"nombre\":\"juanito\"},{\"text\":\"<a href='tutu'>aaa</a> <a href='tutu'>aaa</a> <a href='tutu'>aaa</a>\",\"nombre\":\"jaimito\"},{\"text\":\"<a href='tutu'>aaa</a> <a href='tutu'>aaa</a> <a href='tutu'>aaa</a>\",\"nombre\":\"test\"},{\"text\":\"<a href='tutu'>aaa</a> <a href='tutu'>aaa</a> <a href='tutu'>aaa</a>\",\"nombre\":\"jorgito\"},{\"text\":\"<a href='tutu'>aaa</a> <a href='tutu'>aaa</a> <a href='tutu'>aaa</a>\",\"nombre\":\"donald\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTranslateBasic() throws Exception {
    String queryName = "TranslateBasic";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"active\":\"Yes\",\"value\":\"frost\"},{\"active\":\"Yes\",\"value\":\"silver\"},{\"active\":\"Yes\",\"value\":\"fresh\"},{\"active\":\"Yes\",\"value\":\"clean\"},{\"active\":\"Yes\",\"value\":\"default\"},{\"active\":\"Yes\",\"value\":\"adminflare\"},{\"active\":\"Yes\",\"value\":\"dust\"},{\"active\":\"Yes\",\"value\":\"white\"},{\"active\":\"Yes\",\"value\":\"asphalt\"},{\"active\":\"Yes\",\"value\":\"purple-hills\"},{\"active\":\"Yes\",\"value\":\"amazonia\"},{\"active\":\"Yes\",\"value\":\"sunset\"},{\"active\":\"Yes\",\"value\":\"sky\"},{\"active\":\"Yes\",\"value\":\"eclipse\"},{\"active\":\"Yes\",\"value\":\"grass\"},{\"active\":\"Yes\",\"value\":\"sunny\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedBasic() throws Exception {
    String queryName = "ComputedBasic";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"label\":\"juanito (juanito@test.com)\",\"id\":4,\"value\":\"juanito\",\"email\":\"juanito@test.com\"},{\"label\":\"jaimito (jaimito@test.com)\",\"id\":2,\"value\":\"jaimito\",\"email\":\"jaimito@test.com\"},{\"label\":\"test (test@test.com)\",\"id\":5,\"value\":\"test\",\"email\":\"test@test.com\"},{\"label\":\"jorgito (jorgito@test.com)\",\"id\":3,\"value\":\"jorgito\",\"email\":\"jorgito@test.com\"},{\"label\":\"donald (donald@test.com)\",\"id\":1,\"value\":\"donald\",\"email\":\"donald@test.com\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputeAndTranslate() throws Exception {
    String queryName = "ComputeAndTranslate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":1,\"value\":\"sunset\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":2,\"value\":\"sky\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":3,\"value\":\"eclipse\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":4,\"value\":\"grass\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":5,\"value\":\"sunny\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":6,\"value\":\"purple-hills\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":7,\"value\":\"frost\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":8,\"value\":\"fresh\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":9,\"value\":\"silver\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":10,\"value\":\"clean\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":11,\"value\":\"default\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":12,\"value\":\"adminflare\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":13,\"value\":\"dust\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":14,\"value\":\"white\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":15,\"value\":\"asphalt\"},{\"Act\":1,\"ActTxt\":\"Yes\",\"id\":16,\"value\":\"amazonia\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(result);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalString() throws Exception {
    String queryName = "ComputedEvalString";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":\"Manager\",\"value\":\"adminflare\"},{\"label\":\"No manager\",\"value\":\"amazonia\"},{\"label\":\"No manager\",\"value\":\"asphalt\"},{\"label\":\"No manager\",\"value\":\"clean\"},{\"label\":\"No manager\",\"value\":\"default\"},{\"label\":\"No manager\",\"value\":\"dust\"},{\"label\":\"No manager\",\"value\":\"eclipse\"},{\"label\":\"No manager\",\"value\":\"fresh\"},{\"label\":\"No manager\",\"value\":\"frost\"},{\"label\":\"No manager\",\"value\":\"grass\"},{\"label\":\"No manager\",\"value\":\"purple-hills\"},{\"label\":\"No manager\",\"value\":\"silver\"},{\"label\":\"No manager\",\"value\":\"sky\"},{\"label\":\"No manager\",\"value\":\"sunny\"},{\"label\":\"No manager\",\"value\":\"sunset\"},{\"label\":\"No manager\",\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalNumber() throws Exception {
    String queryName = "ComputedEvalNumber";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":1,\"value\":\"adminflare\"},{\"label\":0,\"value\":\"amazonia\"},{\"label\":0,\"value\":\"asphalt\"},{\"label\":0,\"value\":\"clean\"},{\"label\":0,\"value\":\"default\"},{\"label\":0,\"value\":\"dust\"},{\"label\":0,\"value\":\"eclipse\"},{\"label\":0,\"value\":\"fresh\"},{\"label\":0,\"value\":\"frost\"},{\"label\":0,\"value\":\"grass\"},{\"label\":0,\"value\":\"purple-hills\"},{\"label\":0,\"value\":\"silver\"},{\"label\":0,\"value\":\"sky\"},{\"label\":0,\"value\":\"sunny\"},{\"label\":0,\"value\":\"sunset\"},{\"label\":0,\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalVariable() throws Exception {
    String queryName = "ComputedEvalVariable";
    String variables = "\"prueba\":\"lalala\"";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"variable\":\"lalala\",\"label\":1,\"value\":\"adminflare\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"amazonia\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"asphalt\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"clean\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"default\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"dust\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"eclipse\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"fresh\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"frost\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"grass\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"purple-hills\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"silver\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"sky\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"sunny\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"sunset\"},{\"variable\":\"lalala\",\"label\":0,\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalVariableValue() throws Exception {
    String queryName = "ComputedEvalVariableValue";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"variable\":\"prueba\",\"label\":1,\"value\":\"adminflare\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"amazonia\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"asphalt\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"clean\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"default\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"dust\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"eclipse\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"fresh\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"frost\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"grass\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"purple-hills\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"silver\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"sky\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"sunny\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"sunset\"},{\"variable\":\"prueba\",\"label\":0,\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalVariableProperty() throws Exception {
    String queryName = "ComputedEvalVariableProperty";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"variable\":\"awe-boot\",\"label\":1,\"value\":\"adminflare\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"amazonia\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"asphalt\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"clean\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"default\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"dust\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"eclipse\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"fresh\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"frost\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"grass\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"purple-hills\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"silver\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"sky\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"sunny\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"sunset\"},{\"variable\":\"awe-boot\",\"label\":0,\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalTransform() throws Exception {
    String queryName = "ComputedEvalTransform";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":\"123.456,00\",\"value\":\"adminflare\"},{\"label\":\"0,24\",\"value\":\"amazonia\"},{\"label\":\"0,24\",\"value\":\"asphalt\"},{\"label\":\"0,24\",\"value\":\"clean\"},{\"label\":\"0,24\",\"value\":\"default\"},{\"label\":\"0,24\",\"value\":\"dust\"},{\"label\":\"0,24\",\"value\":\"eclipse\"},{\"label\":\"0,24\",\"value\":\"fresh\"},{\"label\":\"0,24\",\"value\":\"frost\"},{\"label\":\"0,24\",\"value\":\"grass\"},{\"label\":\"0,24\",\"value\":\"purple-hills\"},{\"label\":\"0,24\",\"value\":\"silver\"},{\"label\":\"0,24\",\"value\":\"sky\"},{\"label\":\"0,24\",\"value\":\"sunny\"},{\"label\":\"0,24\",\"value\":\"sunset\"},{\"label\":\"0,24\",\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalTranslate() throws Exception {
    String queryName = "ComputedEvalTranslate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":\"Yes\",\"value\":\"adminflare\"},{\"label\":\"No\",\"value\":\"amazonia\"},{\"label\":\"No\",\"value\":\"asphalt\"},{\"label\":\"No\",\"value\":\"clean\"},{\"label\":\"No\",\"value\":\"default\"},{\"label\":\"No\",\"value\":\"dust\"},{\"label\":\"No\",\"value\":\"eclipse\"},{\"label\":\"No\",\"value\":\"fresh\"},{\"label\":\"No\",\"value\":\"frost\"},{\"label\":\"No\",\"value\":\"grass\"},{\"label\":\"No\",\"value\":\"purple-hills\"},{\"label\":\"No\",\"value\":\"silver\"},{\"label\":\"No\",\"value\":\"sky\"},{\"label\":\"No\",\"value\":\"sunny\"},{\"label\":\"No\",\"value\":\"sunset\"},{\"label\":\"No\",\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseComputedEvalTransformTranslate() throws Exception {
    String queryName = "ComputedEvalTransformTranslate";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"label\":\"Yes\",\"value\":\"adminflare\"},{\"label\":\"No\",\"value\":\"amazonia\"},{\"label\":\"No\",\"value\":\"asphalt\"},{\"label\":\"No\",\"value\":\"clean\"},{\"label\":\"No\",\"value\":\"default\"},{\"label\":\"No\",\"value\":\"dust\"},{\"label\":\"No\",\"value\":\"eclipse\"},{\"label\":\"No\",\"value\":\"fresh\"},{\"label\":\"No\",\"value\":\"frost\"},{\"label\":\"No\",\"value\":\"grass\"},{\"label\":\"No\",\"value\":\"purple-hills\"},{\"label\":\"No\",\"value\":\"silver\"},{\"label\":\"No\",\"value\":\"sky\"},{\"label\":\"No\",\"value\":\"sunny\"},{\"label\":\"No\",\"value\":\"sunset\"},{\"label\":\"No\",\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseCompoundBasic() throws Exception {
    String queryName = "CompoundBasic";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"compuesto\":{\"value\":\"adminflare\",\"label\":\"Yes\"},\"id\":1},{\"compuesto\":{\"value\":\"amazonia\",\"label\":\"No\"},\"id\":2},{\"compuesto\":{\"value\":\"asphalt\",\"label\":\"No\"},\"id\":3},{\"compuesto\":{\"value\":\"clean\",\"label\":\"No\"},\"id\":4},{\"compuesto\":{\"value\":\"default\",\"label\":\"No\"},\"id\":5},{\"compuesto\":{\"value\":\"dust\",\"label\":\"No\"},\"id\":6},{\"compuesto\":{\"value\":\"eclipse\",\"label\":\"No\"},\"id\":7},{\"compuesto\":{\"value\":\"fresh\",\"label\":\"No\"},\"id\":8},{\"compuesto\":{\"value\":\"frost\",\"label\":\"No\"},\"id\":9},{\"compuesto\":{\"value\":\"grass\",\"label\":\"No\"},\"id\":10},{\"compuesto\":{\"value\":\"purple-hills\",\"label\":\"No\"},\"id\":11},{\"compuesto\":{\"value\":\"silver\",\"label\":\"No\"},\"id\":12},{\"compuesto\":{\"value\":\"sky\",\"label\":\"No\"},\"id\":13},{\"compuesto\":{\"value\":\"sunny\",\"label\":\"No\"},\"id\":14},{\"compuesto\":{\"value\":\"sunset\",\"label\":\"No\"},\"id\":15},{\"compuesto\":{\"value\":\"white\",\"label\":\"No\"},\"id\":16}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTotalizeTotal() throws Exception {
    String queryName = "TotalizeTotal";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":13,\"rows\":[{\"IdeSitModDbs\":60,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":7,\"NamMod\":\"Base\",\"id\":1},{\"IdeSitModDbs\":75,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":6,\"NamMod\":\"Base\",\"id\":2},{\"IdeSitModDbs\":76,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":8,\"NamMod\":\"Base\",\"id\":3},{\"IdeSitModDbs\":77,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":9,\"NamMod\":\"Base\",\"id\":4},{\"IdeSitModDbs\":78,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":15,\"NamMod\":\"Base\",\"id\":5},{\"IdeSitModDbs\":79,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":16,\"NamMod\":\"Base\",\"id\":6},{\"IdeSitModDbs\":2579,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":7,\"NamMod\":\"Test\",\"id\":7},{\"IdeSitModDbs\":2580,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":6,\"NamMod\":\"Test\",\"id\":8},{\"IdeSitModDbs\":2581,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":15,\"NamMod\":\"Test\",\"id\":9},{\"IdeSitModDbs\":2582,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":8,\"NamMod\":\"Test\",\"id\":10},{\"IdeSitModDbs\":2583,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":16,\"NamMod\":\"Test\",\"id\":11},{\"IdeSitModDbs\":2584,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":9,\"NamMod\":\"Test\",\"id\":12},{\"IdeSitModDbs\":null,\"Als3\":\"Total\",\"_style_\":\"TOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"24,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":null,\"id\":\"TOT-12\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 13);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTotalizeSubtotal() throws Exception {
    String queryName = "TotalizeSubtotal";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":24,\"rows\":[{\"IdeSitModDbs\":2580,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":6,\"NamMod\":\"Test\",\"id\":1},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-1\"},{\"IdeSitModDbs\":75,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":6,\"NamMod\":\"Base\",\"id\":2},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-3\"},{\"IdeSitModDbs\":2579,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":7,\"NamMod\":\"Test\",\"id\":3},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-5\"},{\"IdeSitModDbs\":60,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":7,\"NamMod\":\"Base\",\"id\":4},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-7\"},{\"IdeSitModDbs\":2582,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":8,\"NamMod\":\"Test\",\"id\":5},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-9\"},{\"IdeSitModDbs\":76,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":8,\"NamMod\":\"Base\",\"id\":6},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-11\"},{\"IdeSitModDbs\":2584,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":9,\"NamMod\":\"Test\",\"id\":7},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-13\"},{\"IdeSitModDbs\":77,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":9,\"NamMod\":\"Base\",\"id\":8},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-15\"},{\"IdeSitModDbs\":2581,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":15,\"NamMod\":\"Test\",\"id\":9},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-17\"},{\"IdeSitModDbs\":78,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":15,\"NamMod\":\"Base\",\"id\":10},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-19\"},{\"IdeSitModDbs\":2583,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":16,\"NamMod\":\"Test\",\"id\":11},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-21\"},{\"IdeSitModDbs\":79,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":16,\"NamMod\":\"Base\",\"id\":12},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-23\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 24);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseTotalizeTotalSubtotal() throws Exception {
    String queryName = "TotalizeTotalSubtotal";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":25,\"rows\":[{\"IdeSitModDbs\":2580,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":6,\"NamMod\":\"Test\",\"id\":1},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-1\"},{\"IdeSitModDbs\":75,\"Als3\":\"aweora1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":6,\"NamMod\":\"Base\",\"id\":2},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-3\"},{\"IdeSitModDbs\":2579,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":916,\"IdeDbs\":7,\"NamMod\":\"Test\",\"id\":3},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-5\"},{\"IdeSitModDbs\":60,\"Als3\":\"aweora2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"1,00\",\"IdeMod\":28,\"IdeDbs\":7,\"NamMod\":\"Base\",\"id\":4},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"1,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-7\"},{\"IdeSitModDbs\":2582,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":8,\"NamMod\":\"Test\",\"id\":5},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-9\"},{\"IdeSitModDbs\":76,\"Als3\":\"awesqs1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":8,\"NamMod\":\"Base\",\"id\":6},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-11\"},{\"IdeSitModDbs\":2584,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":9,\"NamMod\":\"Test\",\"id\":7},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-13\"},{\"IdeSitModDbs\":77,\"Als3\":\"awesybase1\",\"IdeSit\":10,\"NamSit\":\"Madrid\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":9,\"NamMod\":\"Base\",\"id\":8},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-15\"},{\"IdeSitModDbs\":2581,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":916,\"IdeDbs\":15,\"NamMod\":\"Test\",\"id\":9},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-17\"},{\"IdeSitModDbs\":78,\"Als3\":\"awesqs2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"2,00\",\"IdeMod\":28,\"IdeDbs\":15,\"NamMod\":\"Base\",\"id\":10},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"2,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-19\"},{\"IdeSitModDbs\":2583,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":916,\"IdeDbs\":16,\"NamMod\":\"Test\",\"id\":11},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-21\"},{\"IdeSitModDbs\":79,\"Als3\":\"awesybase2\",\"IdeSit\":17,\"NamSit\":\"Onate\",\"Ord3\":\"3,00\",\"IdeMod\":28,\"IdeDbs\":16,\"NamMod\":\"Base\",\"id\":12},{\"IdeSitModDbs\":null,\"Als3\":null,\"_style_\":\"SUBTOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"3,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":\"Subtotal\",\"id\":\"TOT-23\"},{\"IdeSitModDbs\":null,\"Als3\":\"Total\",\"_style_\":\"TOTAL\",\"IdeSit\":null,\"NamSit\":null,\"Ord3\":\"24,00\",\"IdeMod\":null,\"IdeDbs\":null,\"NamMod\":null,\"id\":\"TOT-24\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 25);
  }

  // *****************************************************************************************************************//
  // SORT TESTS
  // **************************************************************************************************************** //

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseNoSort() throws Exception {
    String queryName = "SortGetAll";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSortOneFieldAsc() throws Exception {
    String queryName = "SortGetAll";
    String variables = "\"sort\":[{\"id\":\"order\",\"direction\":\"asc\"}]},";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSortOneFieldDesc() throws Exception {
    String queryName = "SortGetAll";
    String variables = "\"sort\":[{\"id\":\"order\",\"direction\":\"desc\"}]},";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSortTwoFieldsAscDesc() throws Exception {
    String queryName = "SortGetAll";
    String variables = "\"sort\":[{\"id\":\"order\",\"direction\":\"asc\"},{\"id\":\"module\",\"direction\":\"desc\"}]},";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSortTwoFieldsDescDesc() throws Exception {
    String queryName = "SortGetAll";
    String variables = "\"sort\":[{\"id\":\"order\",\"direction\":\"desc\"},{\"id\":\"module\",\"direction\":\"desc\"}]},";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSortThreeFieldsAscDescDesc() throws Exception {
    String queryName = "SortGetAll";
    String variables = "\"sort\":[{\"id\":\"order\",\"direction\":\"asc\"},{\"id\":\"database\",\"direction\":\"desc\"},{\"id\":\"module\",\"direction\":\"desc\"}]},";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":12,\"rows\":[{\"site\":17,\"database\":7,\"module\":916,\"id\":2579,\"order\":1},{\"site\":17,\"database\":7,\"module\":28,\"id\":60,\"order\":1},{\"site\":10,\"database\":6,\"module\":916,\"id\":2580,\"order\":1},{\"site\":10,\"database\":6,\"module\":28,\"id\":75,\"order\":1},{\"site\":17,\"database\":15,\"module\":916,\"id\":2581,\"order\":2},{\"site\":17,\"database\":15,\"module\":28,\"id\":78,\"order\":2},{\"site\":10,\"database\":8,\"module\":916,\"id\":2582,\"order\":2},{\"site\":10,\"database\":8,\"module\":28,\"id\":76,\"order\":2},{\"site\":17,\"database\":16,\"module\":916,\"id\":2583,\"order\":3},{\"site\":17,\"database\":16,\"module\":28,\"id\":79,\"order\":3},{\"site\":10,\"database\":9,\"module\":916,\"id\":2584,\"order\":3},{\"site\":10,\"database\":9,\"module\":28,\"id\":77,\"order\":3}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertQueryResultJson(queryName, result, 12);
  }

  // *****************************************************************************************************************//
  // VARIABLE TESTS
  // **************************************************************************************************************** //

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       Query result
   * @param expectedRows Expected rows number
   * @return Value list
   * @throws Exception Test error
   */
  private ArrayNode assertResultVariablesJson(String queryName, String result, int expectedRows) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(1, dataList.get("total").asInt());
    assertEquals(1, dataList.get("page").asInt());
    assertEquals(expectedRows, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(expectedRows, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : dataListRows) {
      ObjectNode component = (ObjectNode) element;
      logger.debug(component.toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("-------------------------------------------");

    return dataListRows;
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseQueryVariableStringNull() throws Exception {
    String queryName = "VariableStringNull";
    String variables = "\"stringNull\":null";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"value\":\"adminflare\"},{\"value\":\"amazonia\"},{\"value\":\"asphalt\"},{\"value\":\"clean\"},{\"value\":\"default\"},{\"value\":\"dust\"},{\"value\":\"eclipse\"},{\"value\":\"fresh\"},{\"value\":\"frost\"},{\"value\":\"grass\"},{\"value\":\"purple-hills\"},{\"value\":\"silver\"},{\"value\":\"sky\"},{\"value\":\"sunny\"},{\"value\":\"sunset\"},{\"value\":\"white\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringHash() throws Exception {
    String queryName = "VariableStringHash";
    String variables = "";
    String hashPbkResult = encodeService.encodePBKDF2WithHmacSHA1("prueba");
    String hashEncryptResult = encodeService.encryptRipEmd160("prueba");
    String hashRipEmdResult = EncodeService.encodeRipEmd160("prueba");
    String hashShaResult = encodeService.hash(EncodeService.HashingAlgorithms.SHA_256, "prueba");
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"hashEncrypt\":\"" + hashEncryptResult + "\",\"l1_nom\":\"test\",\"hashPbk\":\"" + hashPbkResult + "\",\"hashRipemd\":\"" + hashRipEmdResult + "\",\"hashSha\":\"" + hashShaResult + "\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableDate() throws Exception {
    String queryName = "VariableDate";
    String variables = "\"date\":\"22/10/3100\"";
    setParameter("date", "22/03/2011");
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"date\":\"23/10/1978\",\"dateParameter\":\"22/10/3100\",\"sessionDate\":\"22/03/2011\",\"Als\":\"Theme test\",\"id\":1,\"timestamp\":\"23/10/1978 15:03:01\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test a date list as parameter for a Java Service
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseServiceDateList() throws Exception {
    String queryName = "VariableDateList";
    String variables = "\"FilDat\":[\"11/03/1921\",\"15/01/1988\",\"28/04/2007\",\"18/06/2017\",\"10/05/2019\",\"22/10/3100\"]";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":11,\"rows\":[{\"date\":\"11/03/1921\",\"id\":1},{\"date\":\"15/01/1988\",\"id\":2},{\"date\":\"28/04/2007\",\"id\":3},{\"date\":\"18/06/2017\",\"id\":4},{\"date\":\"10/05/2019\",\"id\":5},{\"date\":\"22/10/3100\",\"id\":6},{\"date\":\"23/10/1978\",\"id\":7},{\"date\":null,\"id\":8},{\"date\":null,\"id\":9},{\"date\":null,\"id\":10},{\"date\":null,\"id\":11}]}}},{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"replace-columns\",\"target\":\"GrdMus\",\"parameters\":{\"columns\":[{\"actions\":[],\"align\":\"right\",\"autoload\":false,\"charlength\":20,\"checkEmpty\":false,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"elementList\":[],\"elementType\":\"Column\",\"frozen\":false,\"hidden\":false,\"label\":\"BUTTON_NEW ELEMENT_TYPE_COLUMN 1\",\"loadAll\":false,\"movable\":true,\"name\":\"GrdMus-newColumn1\",\"optional\":false,\"printable\":true,\"readonly\":false,\"required\":false,\"sendable\":true,\"sortable\":true,\"strict\":true,\"value\":\"1\",\"visible\":true},{\"actions\":[],\"align\":\"center\",\"autoload\":false,\"charlength\":20,\"checkEmpty\":false,\"checkInitial\":true,\"checked\":false,\"component\":\"icon\",\"contextMenu\":[],\"dependencies\":[],\"elementList\":[],\"elementType\":\"Column\",\"frozen\":false,\"hidden\":false,\"label\":\"BUTTON_NEW ELEMENT_TYPE_COLUMN 2\",\"loadAll\":false,\"movable\":true,\"name\":\"GrdMus-newColumn2\",\"optional\":false,\"printable\":true,\"readonly\":false,\"required\":false,\"sendable\":true,\"sortable\":true,\"strict\":true,\"value\":\"aaaa\",\"visible\":true}]}},{\"type\":\"fill\",\"target\":\"GrdMus\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":11,\"rows\":[{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":1,\"GrdMus-newColumn1\":\"1921-03-11\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":2,\"GrdMus-newColumn1\":\"1988-01-15\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":3,\"GrdMus-newColumn1\":\"2007-04-28\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":4,\"GrdMus-newColumn1\":\"2017-06-18\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":5,\"GrdMus-newColumn1\":\"2019-05-10\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":6,\"GrdMus-newColumn1\":\"3100-10-22\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":7,\"GrdMus-newColumn1\":\"1978-10-23\"},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":8},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":9},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":10},{\"GrdMus-newColumn2\":{\"value\":\"icono\",\"icon\":\"fa-check\",\"style\":\"text-success\"},\"id\":11}]}}}]";

    logger.warn("LAUNCHED testDatabaseServiceDateList");
    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 11);
  }


  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableSystemDate() throws Exception {
    String queryName = "VariableSystemDate";
    String variables = "";

    String result = performRequest(queryName, variables, DATABASE);

    ArrayNode data = assertResultVariablesJson(queryName, result, 1);

    // Retrieve output values
    ObjectNode firstRow = (ObjectNode) data.get(0);
    String date = firstRow.get("date").asText();
    String time = firstRow.get("time").asText();
    String timestamp = firstRow.get("timestamp").asText();

    // Check current date vs retrieved date
    Date currentDate = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    Date parsedDate = sdf.parse(date + " " + time);
    logger.debug("Retrieved date: " + parsedDate.toString() + " - Current date: " + currentDate);
    logger.debug("Difference between dates: " + (currentDate.getTime() - parsedDate.getTime()));

    sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    Date parsedTimestamp = sdf.parse(timestamp);
    logger.debug("Difference between timestamp: " + (currentDate.getTime() - parsedTimestamp.getTime()));

    // Check that the difference is less than a second
    assertTrue(parsedDate.getTime() - currentDate.getTime() < 1000);
    assertTrue(parsedTimestamp.getTime() - currentDate.getTime() < 1000);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableNull() throws Exception {
    String queryName = "VariableNull";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"null\":null,\"l1_nom\":\"test\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseOperationNullIf() throws Exception {
    String queryName = "OperationNullIf";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"null\":\"default\",\"Nam\":\"default\",\"id\":1},{\"null\":\"purple-hills\",\"Nam\":\"purple-hills\",\"id\":2},{\"null\":\"sunny\",\"Nam\":\"sunny\",\"id\":3},{\"null\":null,\"Nam\":\"sunset\",\"id\":4}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    logger.warn(result);
    assertResultVariablesJson(queryName, result, 4);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableObject() throws Exception {
    String queryName = "VariableObject";
    String variables = "\"object\":{\"total\":1,\"page\":1,\"records\":1}";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"l1_nom\":\"test\",\"object\":\"{\\\"total\\\":1,\\\"page\\\":1,\\\"records\\\":1}\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableProperty() throws Exception {
    String propertyValue = "awe-boot";
    String queryName = "VariableProperty";
    String variables = "\"object\":{\"total\":1,\"page\":1,\"records\":1}";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"l1_nom\":\"test\",\"property\":\"" + propertyValue + "\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, propertyValue, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableOptionalFilter() throws Exception {
    String queryName = "VariableOptionalFilter";
    String variables = "\"var1\":null,\"var2\":1";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"l1_nom\":\"test\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableOptional() throws Exception {
    String queryName = "VariableOptional";
    String variables = "\"var1\":1";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 0);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableOptionalWithValue() throws Exception {
    String queryName = "VariableOptional";
    String variables = "\"var1\":null,\"var2\":1";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"l1_nom\":\"test\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseVariableList() throws Exception {
    String queryName = "VariableList";
    String variables = "\"list1\":null,\"list2\":[\"1\",\"2\",\"3\",\"4\"],\"list3\":[],\"list4\":[1,3,4,5]";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"l1_nom\":\"test\"},{\"l1_nom\":\"jorgito\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 2);
  }

  /**
   * Test of filter in subquery
   *
   * @throws Exception Test error
   */
  @Test
  void testFilterInSubQuery() throws Exception {
    String queryName = "FilterInSubQuery";
    String variables = "\"test\":[\"test\",\"jaimito\",\"juanito\",\"jorgito\"]";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"l1_nom\":\"test\"},{\"l1_nom\":\"jorgito\"},{\"l1_nom\":\"jaimito\"},{\"l1_nom\":\"juanito\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultVariablesJson(queryName, result, 4);
  }

  // *****************************************************************************************************************//
  // SERVICE TESTS
  // **************************************************************************************************************** //

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       result
   * @param expectedRows Expected rows
   * @return Result list
   * @throws Exception Test error
   */
  private ArrayNode assertResultServiceJson(String queryName, String result, int expectedRows) throws Exception {
    return assertResultServiceJson(queryName, result, expectedRows, 1, 1, expectedRows);
  }

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    query name
   * @param result       result
   * @param expectedRows Expected rows
   * @param page         page
   * @param totalPages   total pages
   * @param records      records
   * @return Record list
   * @throws Exception Test error
   */
  private ArrayNode assertResultServiceJson(String queryName, String result, int expectedRows, int page, int totalPages, int records) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode fillParameters = (ObjectNode) fillAction.get("parameters");
    assertEquals(1, fillParameters.size());
    ObjectNode dataList = (ObjectNode) fillParameters.get("datalist");
    assertEquals(totalPages, dataList.get("total").asInt());
    assertEquals(page, dataList.get("page").asInt());
    assertEquals(records, dataList.get("records").asInt());
    ArrayNode dataListRows = (ArrayNode) dataList.get("rows");
    assertEquals(expectedRows, dataListRows.size());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug("There are " + dataListRows.size() + " rows as a result of launching query " + queryName);
    logger.debug("--------------------------------------------------------------------------------------");

    return dataListRows;
  }

  private void testDatabaseRequest(String query, String variables, String expected, Integer expectedRows) throws Exception {
    assertResultJson(query, performRequest(query, variables, DATABASE, expected), expectedRows);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseDatalistNoParams() throws Exception {
    testDatabaseRequest(
      "DatalistNoParams",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"id\":1,\"value\":\"0\"},{\"id\":2,\"value\":\"1\"},{\"id\":3,\"value\":\"2\"}]}}},{\"type\":\"end-load\"}]",
      3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayNoParams() throws Exception {
    testDatabaseRequest(
      "StringArrayNoParams",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":3,\"rows\":[{\"id\":1,\"value\":\"a\"},{\"id\":2,\"value\":\"b\"},{\"id\":3,\"value\":\"c\"}]}}},{\"type\":\"end-load\"}]",
      3);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayTwoStringsParams() throws Exception {
    testDatabaseRequest(
      "ServiceQueryTwoParams",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"IdeSitModDbsSrv\":\"QrySitModDbsOrd\",\"IdeSitSrv\":\"IdeSitModDbs,IdeSit,NamSit,IdeMod,NamMod,IdeDbs,Als,Ord\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayNumberParam() throws Exception {
    testDatabaseRequest(
      "StringArrayNumberParam",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"value\":\"10\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayLongParam() throws Exception {
    testDatabaseRequest(
      "StringArrayLongParam",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"value\":\"10\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayDoubleParam() throws Exception {
    testDatabaseRequest(
      "StringArrayDoubleParam",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"value\":\"10.0\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayFloatParam() throws Exception {
    testDatabaseRequest(
      "StringArrayFloatParam",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"value\":\"10.0\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseStringArrayBooleanParam() throws Exception {
    testDatabaseRequest(
      "StringArrayBooleanParam",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"id\":1,\"value\":\"true\"}]}}},{\"type\":\"end-load\"}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabasePagination() throws Exception {
    String queryName = "SimplePaginationService";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":3,\"page\":1,\"records\":65,\"rows\":[{\"id\":1,\"value\":\"0\"},{\"id\":2,\"value\":\"1\"},{\"id\":3,\"value\":\"2\"},{\"id\":4,\"value\":\"3\"},{\"id\":5,\"value\":\"4\"},{\"id\":6,\"value\":\"5\"},{\"id\":7,\"value\":\"6\"},{\"id\":8,\"value\":\"7\"},{\"id\":9,\"value\":\"8\"},{\"id\":10,\"value\":\"9\"},{\"id\":11,\"value\":\"10\"},{\"id\":12,\"value\":\"11\"},{\"id\":13,\"value\":\"12\"},{\"id\":14,\"value\":\"13\"},{\"id\":15,\"value\":\"14\"},{\"id\":16,\"value\":\"15\"},{\"id\":17,\"value\":\"16\"},{\"id\":18,\"value\":\"17\"},{\"id\":19,\"value\":\"18\"},{\"id\":20,\"value\":\"19\"},{\"id\":21,\"value\":\"20\"},{\"id\":22,\"value\":\"21\"},{\"id\":23,\"value\":\"22\"},{\"id\":24,\"value\":\"23\"},{\"id\":25,\"value\":\"24\"},{\"id\":26,\"value\":\"25\"},{\"id\":27,\"value\":\"26\"},{\"id\":28,\"value\":\"27\"},{\"id\":29,\"value\":\"28\"},{\"id\":30,\"value\":\"29\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 30, 1, 3, 65);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabasePaginationMax10() throws Exception {
    String queryName = "SimplePaginationService";
    String variables = "\"max\": 10";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":7,\"page\":1,\"records\":65,\"rows\":[{\"id\":1,\"value\":\"0\"},{\"id\":2,\"value\":\"1\"},{\"id\":3,\"value\":\"2\"},{\"id\":4,\"value\":\"3\"},{\"id\":5,\"value\":\"4\"},{\"id\":6,\"value\":\"5\"},{\"id\":7,\"value\":\"6\"},{\"id\":8,\"value\":\"7\"},{\"id\":9,\"value\":\"8\"},{\"id\":10,\"value\":\"9\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 10, 1, 7, 65);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseManagedPagination() throws Exception {
    String queryName = "SimpleManagedPaginationService";
    String variables = "\"page\": 2, \"max\": 10";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":7,\"page\":2,\"records\":65,\"rows\":[{\"id\":1,\"value\":\"10\"},{\"id\":2,\"value\":\"11\"},{\"id\":3,\"value\":\"12\"},{\"id\":4,\"value\":\"13\"},{\"id\":5,\"value\":\"14\"},{\"id\":6,\"value\":\"15\"},{\"id\":7,\"value\":\"16\"},{\"id\":8,\"value\":\"17\"},{\"id\":9,\"value\":\"18\"},{\"id\":10,\"value\":\"19\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 10, 2, 7, 65);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testRowNumber() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testRowNumber",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"rowNumber\":1},{\"name\":\"jaimito\",\"id\":2,\"rowNumber\":2},{\"name\":\"jorgito\",\"id\":3,\"rowNumber\":3},{\"name\":\"juanito\",\"id\":4,\"rowNumber\":4},{\"name\":\"test\",\"id\":5,\"rowNumber\":5}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testCaseOver() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testCaseOver",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"value\":0},{\"name\":\"jaimito\",\"id\":2,\"value\":0},{\"name\":\"jorgito\",\"id\":3,\"value\":1},{\"name\":\"juanito\",\"id\":4,\"value\":0},{\"name\":\"test\",\"id\":5,\"value\":2}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testRowNumberWithOperation() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testRowNumberWithOperation",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"rowNumber\":2},{\"name\":\"jaimito\",\"id\":2,\"rowNumber\":3},{\"name\":\"jorgito\",\"id\":3,\"rowNumber\":4},{\"name\":\"juanito\",\"id\":4,\"rowNumber\":5},{\"name\":\"test\",\"id\":5,\"rowNumber\":6}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of POWER operation of one field
   *
   * @throws Exception Test error
   */
  @Test
  void testPowerOfFieldOperation() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testPowerOfFieldOperation",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"powerField\":2.0,\"name\":\"donald\",\"id\":1},{\"powerField\":4.0,\"name\":\"jaimito\",\"id\":2},{\"powerField\":8.0,\"name\":\"jorgito\",\"id\":3},{\"powerField\":16.0,\"name\":\"juanito\",\"id\":4},{\"powerField\":32.0,\"name\":\"test\",\"id\":5}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of ROUND operation
   * Round to nearest integer
   *
   * @throws Exception Test error
   */
  @Test
  void testRoundFieldOperation() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testRoundField",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"name\":\"donald\",\"roundField\":2,\"id\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      1);
  }

  /**
   * Test of ROUND operation
   * Round to a decimal places
   *
   * @throws Exception Test error
   */
  @Test
  void testRoundFieldOperationWithDecimals() throws Exception {
    assumeTrue(isInMemoryDatabase());
    testDatabaseRequest(
      "testRoundFieldWithDecimals",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"name\":\"donald\",\"roundField\":2.13,\"id\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      1);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testNullIf() throws Exception {
    testDatabaseRequest(
      "testNullIf",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"nullif\":\"donald\"},{\"name\":\"jaimito\",\"id\":2,\"nullif\":\"jaimito\"},{\"name\":\"jorgito\",\"id\":3,\"nullif\":\"jorgito\"},{\"name\":\"juanito\",\"id\":4,\"nullif\":\"juanito\"},{\"name\":\"test\",\"id\":5,\"nullif\":null}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSum1() throws Exception {
    testDatabaseRequest(
      "testSum1",
      "",
      "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":16,\"rows\":[{\"sum1\":1,\"id\":1},{\"sum1\":1,\"id\":2},{\"sum1\":1,\"id\":3},{\"sum1\":1,\"id\":4},{\"sum1\":1,\"id\":5},{\"sum1\":1,\"id\":6},{\"sum1\":1,\"id\":7},{\"sum1\":1,\"id\":8},{\"sum1\":1,\"id\":9},{\"sum1\":1,\"id\":10},{\"sum1\":1,\"id\":11},{\"sum1\":1,\"id\":12},{\"sum1\":1,\"id\":13},{\"sum1\":1,\"id\":14},{\"sum1\":1,\"id\":15},{\"sum1\":1,\"id\":16}]}}},{\"type\":\"end-load\",\"parameters\":{}}]",
      16);
  }

  /**
   * Test of launchAction method, of class ActionController.
   * Launches an exception
   *
   * @throws Exception Test error
   */
  @Test
  void testOverPartitionOrderNotSupported() throws Exception {
    String queryName = "testOverPartitionOrder";
    assumeTrue(isInMemoryDatabase());
    performRequest(queryName, "", DATABASE);
  }


  /**
   * Test of launchAction method, of class ActionController.
   * To be launched when launching tests on ORACLE, SQLSERVER or MYSQL databases
   *
   * @throws Exception Test error
   */
  @Test
  void testOverPartitionOrder() throws Exception {
    String queryName = "testOverPartitionOrder";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"rowNumber\":\"donald\",\"rankValue\":1},{\"name\":\"jaimito\",\"id\":2,\"rowNumber\":\"jaimito\",\"rankValue\":1},{\"name\":\"jorgito\",\"id\":3,\"rowNumber\":\"jorgito\",\"rankValue\":1},{\"name\":\"juanito\",\"id\":4,\"rowNumber\":\"juanito\",\"rankValue\":1},{\"name\":\"test\",\"id\":5,\"rowNumber\":null,\"rankValue\":1}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";
    assumeTrue(!isInMemoryDatabase());
    String result = performRequest(queryName, "", DATABASE, expected);
    assertResultJson(queryName, result, 5);
  }

  /**
   * Test of launchAction method, of class ActionController.
   * To be launched when launching tests on ORACLE, SQLSERVER or MYSQL databases
   *
   * @throws Exception Test error
   */
  @Test
  void testOverPartitionOrderOperation() throws Exception {
    String queryName = "testOverPartitionOrderOperation";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":5,\"rows\":[{\"name\":\"donald\",\"id\":1,\"rowNumber\":2},{\"name\":\"jaimito\",\"id\":2,\"rowNumber\":4},{\"name\":\"jorgito\",\"id\":3,\"rowNumber\":6},{\"name\":\"juanito\",\"id\":4,\"rowNumber\":8},{\"name\":\"test\",\"id\":5,\"rowNumber\":10}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";
    assumeTrue(!isInMemoryDatabase());
    String result = performRequest(queryName, "", DATABASE, expected);
    assertResultJson(queryName, result, 5);
  }

  // *****************************************************************************************************************//
  // INITIAL LOAD TESTS
  // **************************************************************************************************************** //

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithAnonymousUser
  void testCheckInitialQueryTarget() throws Exception {
    String expected = "[{\"type\":\"screen-data\",\"parameters\":{\"view\":\"base\",\"screenData\":{\"components\":[{\"id\":\"ComponentSelectEnum\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectEnum\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"Es1Es0\",\"visible\":true},\"model\":{\"selected\":[1],\"defaultValues\":[],\"values\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]}},{\"id\":\"ComponentSelectQuery\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectQuery\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadQuery\",\"visible\":true},\"model\":{\"selected\":[1],\"defaultValues\":[],\"values\":[{\"label\":\"test\",\"id\":1,\"value\":1},{\"label\":\"donald\",\"id\":2,\"value\":2},{\"label\":\"jorgito\",\"id\":3,\"value\":3},{\"label\":\"juanito\",\"id\":4,\"value\":811},{\"label\":\"jaimito\",\"id\":5,\"value\":1702}]}},{\"id\":\"WinDat\",\"controller\":{\"contextMenu\":[],\"dependencies\":[],\"label\":\"SCREEN_TEXT_DATA\",\"maximize\":true,\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}},{\"id\":\"ComponentSuggestValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSuggestValue\",\"label\":\"PARAMETER_SUGGEST\",\"optional\":false,\"printable\":true,\"readonly\":false,\"serverAction\":\"data\",\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}},{\"id\":\"ComponentTextValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}}],\"messages\":{},\"actions\":[],\"screen\":{\"name\":\"TestInitialLoad\",\"title\":\"SCREEN_TITLE_BUTTON_TEST\",\"option\":\"test-initial-load\"}}}},{\"type\":\"end-load\"}]";
    MvcResult mvcResult = mockMvc.perform(post("/action/screen-data")
        .with(csrf())
        .content("{\"option\":\"test-initial-load\",\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(expected))
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode screenDataAction = (ObjectNode) resultList.get(0);
    assertEquals("screen-data", screenDataAction.get("type").textValue());
    ObjectNode screenDataParameters = (ObjectNode) screenDataAction.get("parameters");
    assertEquals(2, screenDataParameters.size());
    assertEquals("base", screenDataParameters.get("view").textValue());
    ObjectNode screenData = (ObjectNode) screenDataParameters.get("screenData");
    assertEquals(0, screenData.get("actions").size());
    assertEquals(0, screenData.get("messages").size());
    ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
    assertEquals(5, screenDataComponents.size());
    assertEquals("TestInitialLoad", screenData.get("screen").get("name").textValue());
    assertEquals("test-initial-load", screenData.get("screen").get("option").textValue());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : screenDataComponents) {
      ObjectNode component = (ObjectNode) element;
      String key = component.get("id").asText();
      logger.debug(key + ": " + component.get("model").get("selected").toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithMockCustomUser(username = "LaloElMalo", roles = {"ADMIN", "USER"})
  void testCheckInitialQuerySelectedValues() throws Exception {
    setParameter("user", "LaloElMalo");
    String expected =
      "[{\"type\":\"screen-data\",\"parameters\":{\"view\":\"base\",\"screenData\":{\"components\":[{\"id\":\"ComponentSelectEnum\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectEnum\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"Es1Es0\",\"value\":\"0\",\"visible\":true},\"model\":{\"selected\":[\"0\"],\"defaultValues\":[\"0\"],\"values\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]}},{\"id\":\"ComponentSuggestCheckInitial\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checkTarget\":\"TestComponentInitialSuggestValue\",\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSuggestCheckInitial\",\"label\":\"PARAMETER_SUGGEST\",\"optional\":false,\"printable\":true,\"readonly\":false,\"serverAction\":\"data\",\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[\"1\"],\"defaultValues\":[\"1\"],\"values\":[]}},{\"id\":\"ComponentSelectQuery\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectQuery\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadQuery\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[\"1\"],\"defaultValues\":[\"1\"],\"values\":[{\"label\":\"test\",\"id\":1,\"value\":1},{\"label\":\"donald\",\"id\":2,\"value\":2},{\"label\":\"jorgito\",\"id\":3,\"value\":3},{\"label\":\"juanito\",\"id\":4,\"value\":811},{\"label\":\"jaimito\",\"id\":5,\"value\":1702}]}},{\"id\":\"ComponentTextStaticValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"value\":\"prueba\",\"visible\":true},\"model\":{\"selected\":[\"prueba\"],\"defaultValues\":[\"prueba\"],\"values\":[]}},{\"id\":\"WinDat\",\"controller\":{\"contextMenu\":[],\"dependencies\":[],\"label\":\"SCREEN_TEXT_DATA\",\"maximize\":true,\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}},{\"id\":\"ComponentSuggestValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSuggestValue\",\"label\":\"PARAMETER_SUGGEST\",\"optional\":false,\"printable\":true,\"readonly\":false,\"serverAction\":\"data\",\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}},{\"id\":\"ComponentTextValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}},{\"id\":\"ComponentTextStaticSessionValue\",\"controller\":{\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticSessionValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"visible\":true},\"model\":{\"selected\":[\"LaloElMalo\"],\"defaultValues\":[\"LaloElMalo\"],\"values\":[]}},{\"id\":\"ComponentTextStaticPropertyValue\",\"controller\":{\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticPropertyValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"visible\":true},\"model\":{\"selected\":[\"awe-boot\"],\"defaultValues\":[\"awe-boot\"],\"values\":[]}}],\"messages\":{},\"actions\":[],\"screen\":{\"name\":\"TestInitialValues\",\"title\":\"SCREEN_TITLE_BUTTON_TEST\",\"option\":\"test-initial-values\"}}}},{\"type\":\"end-load\"}]";
    MvcResult mvcResult = mockMvc.perform(post("/action/screen-data")
        .with(csrf())
        .content("{\"option\":\"test-initial-values\",\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .session(session))
      .andExpect(status().isOk())
      .andExpect(content().json(expected))
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode screenDataAction = (ObjectNode) resultList.get(0);
    assertEquals("screen-data", screenDataAction.get("type").textValue());
    ObjectNode screenDataParameters = (ObjectNode) screenDataAction.get("parameters");
    assertEquals(2, screenDataParameters.size());
    assertEquals("base", screenDataParameters.get("view").textValue());
    ObjectNode screenData = (ObjectNode) screenDataParameters.get("screenData");
    assertEquals(0, screenData.get("actions").size());
    assertEquals(0, screenData.get("messages").size());
    ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
    assertEquals(9, screenDataComponents.size());
    assertEquals("TestInitialValues", screenData.get("screen").get("name").textValue());
    assertEquals("test-initial-values", screenData.get("screen").get("option").textValue());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : screenDataComponents) {
      ObjectNode component = (ObjectNode) element;
      String key = component.get("id").asText();
      logger.debug(key + ": " + component.get("model").get("selected").toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithAnonymousUser
  void testCheckInitialVariables() throws Exception {
    String expected = "[{\"type\":\"screen-data\",\"parameters\":{\"view\":\"base\",\"screenData\":{\"components\":[{\"id\":\"ComponentSelectEnum\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectEnum\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"Es1Es0\",\"value\":\"0\",\"visible\":true},\"model\":{\"selected\":[\"1\"],\"defaultValues\":[\"1\"],\"values\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]}},{\"id\":\"ComponentSuggestCheckInitial\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checkTarget\":\"TestComponentInitialSuggestValue\",\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSuggestCheckInitial\",\"label\":\"PARAMETER_SUGGEST\",\"optional\":false,\"printable\":true,\"readonly\":false,\"serverAction\":\"data\",\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[\"1\"],\"defaultValues\":[\"1\"],\"values\":[]}},{\"id\":\"ComponentSelectQuery\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSelectQuery\",\"label\":\"PARAMETER_SELECT\",\"optional\":true,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadQuery\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[\"2\"],\"defaultValues\":[\"2\"],\"values\":[{\"label\":\"test\",\"id\":1,\"value\":1},{\"label\":\"donald\",\"id\":2,\"value\":2},{\"label\":\"jorgito\",\"id\":3,\"value\":3},{\"label\":\"juanito\",\"id\":4,\"value\":811},{\"label\":\"jaimito\",\"id\":5,\"value\":1702}]}},{\"id\":\"ComponentTextStaticValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"value\":\"prueba\",\"visible\":true},\"model\":{\"selected\":[\"variableStatic\"],\"defaultValues\":[\"variableStatic\"],\"values\":[]}},{\"id\":\"WinDat\",\"controller\":{\"contextMenu\":[],\"dependencies\":[],\"label\":\"SCREEN_TEXT_DATA\",\"maximize\":true,\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}},{\"id\":\"ComponentSuggestValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentSuggestValue\",\"label\":\"PARAMETER_SUGGEST\",\"optional\":false,\"printable\":true,\"readonly\":false,\"serverAction\":\"data\",\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"value\":\"1\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}},{\"id\":\"ComponentTextValue\",\"controller\":{\"checkEmpty\":true,\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"targetAction\":\"TestComponentInitialLoadValue\",\"visible\":true},\"model\":{\"selected\":[1.0],\"defaultValues\":[1.0],\"values\":[{\"kk\":\"1\",\"value2\":1,\"label\":\"test\",\"id\":1,\"value\":1.0}]}},{\"id\":\"ComponentTextStaticSessionValue\",\"controller\":{\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticSessionValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"visible\":true},\"model\":{\"selected\":[\"variableSession\"],\"defaultValues\":[\"variableSession\"],\"values\":[]}},{\"id\":\"ComponentTextStaticPropertyValue\",\"controller\":{\"checkInitial\":true,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"id\":\"ComponentTextStaticPropertyValue\",\"label\":\"PARAMETER_TEXT\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"col-xs-6 col-sm-3 col-lg-2\",\"visible\":true},\"model\":{\"selected\":[\"variableProperty\"],\"defaultValues\":[\"variableProperty\"],\"values\":[]}}],\"messages\":{},\"actions\":[],\"screen\":{\"name\":\"TestInitialValues\",\"title\":\"SCREEN_TITLE_BUTTON_TEST\",\"option\":\"test-initial-values\"}}}},{\"type\":\"end-load\"}]";
    String parameters = "\"SelectEnum\":\"1\",\"SelectQuery\":\"2\",\"InitialLoadValue\":\"otra\",\"StaticValue\":\"variableStatic\",\"SessionValue\":\"variableSession\",\"PropertyValue\":\"variableProperty\",";
    MvcResult mvcResult = mockMvc.perform(post("/action/screen-data")
        .with(csrf())
        .content("{" + parameters + "\"option\":\"test-initial-values\",\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      //        .andExpect(content().json(expected))
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();
    logger.info(("AYUDA:" + result));

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode screenDataAction = (ObjectNode) resultList.get(0);
    assertEquals("screen-data", screenDataAction.get("type").textValue());
    ObjectNode screenDataParameters = (ObjectNode) screenDataAction.get("parameters");
    assertEquals(2, screenDataParameters.size());
    assertEquals("base", screenDataParameters.get("view").textValue());
    ObjectNode screenData = (ObjectNode) screenDataParameters.get("screenData");
    assertEquals(0, screenData.get("actions").size());
    assertEquals(0, screenData.get("messages").size());
    ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
    assertEquals(9, screenDataComponents.size());
    assertEquals("TestInitialValues", screenData.get("screen").get("name").textValue());
    assertEquals("test-initial-values", screenData.get("screen").get("option").textValue());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : screenDataComponents) {
      ObjectNode component = (ObjectNode) element;
      String key = component.get("id").asText();
      logger.debug(key + ": " + component.get("model").get("selected").toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithAnonymousUser
  void testCheckInitialQueryTargetVariables() throws Exception {
    String parameters = "\"SelectEnum\":\"1\",\"SelectQuery\":\"2\",\"InitialLoadValue\":\"otra\",\"StaticValue\":\"variableStatic\",\"SessionValue\":\"variableSession\",\"PropertyValue\":\"variableProperty\",";
    MvcResult mvcResult = mockMvc.perform(post("/action/screen-data")
        .with(csrf())
        .content("{" + parameters + "\"option\":\"test-initial-values-load\",\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();

    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode screenDataAction = (ObjectNode) resultList.get(0);
    assertEquals("screen-data", screenDataAction.get("type").textValue());
    ObjectNode screenDataParameters = (ObjectNode) screenDataAction.get("parameters");
    assertEquals(2, screenDataParameters.size());
    assertEquals("base", screenDataParameters.get("view").textValue());
    ObjectNode screenData = (ObjectNode) screenDataParameters.get("screenData");
    assertEquals(0, screenData.get("actions").size());
    assertEquals(0, screenData.get("messages").size());
    ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
    assertEquals(9, screenDataComponents.size());
    assertEquals("TestInitialValuesLoad", screenData.get("screen").get("name").textValue());
    assertEquals("test-initial-values-load", screenData.get("screen").get("option").textValue());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : screenDataComponents) {
      ObjectNode component = (ObjectNode) element;
      String key = component.get("id").asText();
      logger.debug(key + ": " + component.get("model").get("selected").toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
    logger.debug("-------------------------------------------");
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseGridsAndChartScreen() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/action/screen-data")
        .with(csrf())
        .content("{\"option\":\"grid-and-chart\",\"view\":\"report\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();
    logger.warn(result);
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

    ObjectNode screenDataAction = (ObjectNode) resultList.get(0);
    assertEquals("screen-data", screenDataAction.get("type").textValue());
    ObjectNode screenDataParameters = (ObjectNode) screenDataAction.get("parameters");
    assertEquals(2, screenDataParameters.size());
    assertEquals("report", screenDataParameters.get("view").textValue());
    ObjectNode screenData = (ObjectNode) screenDataParameters.get("screenData");
    assertEquals(0, screenData.get("actions").size());
    assertEquals(0, screenData.get("messages").size());
    ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
    assertEquals(27, screenDataComponents.size());
    assertEquals("GrdChrPrn", screenData.get("screen").get("name").textValue());
    assertEquals("grid-and-chart", screenData.get("screen").get("option").textValue());

    ObjectNode endLoad = (ObjectNode) resultList.get(1);
    assertEquals("end-load", endLoad.get("type").textValue());

    // Test all keys
    for (JsonNode element : screenDataComponents) {
      ObjectNode component = (ObjectNode) element;
      String key = component.get("id").asText();
      logger.debug(key + ": " + component.get("model").get("selected").toString());
    }

    logger.debug("-------------------------------------------");
    logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
    logger.debug("-------------------------------------------");
  }

  // *****************************************************************************************************************//
  // ENUM TESTS
  // **************************************************************************************************************** //

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       Query result
   * @param expectedRows Expected rows
   * @return Result data lines
   * @throws Exception Test error
   */
  private ArrayNode assertResultEnumJson(String queryName, String result, int expectedRows) throws Exception {
    return assertResultServiceJson(queryName, result, expectedRows, 1, 1, expectedRows);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseSimpleEnum() throws Exception {
    String queryName = "SimpleEnum";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"id\":1,\"value\":\"0\",\"label\":\"ENUM_NO\"},{\"id\":2,\"value\":\"1\",\"label\":\"ENUM_YES\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 2);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseLongerEnum() throws Exception {
    String queryName = "LongerEnum";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":58,\"rows\":[{\"id\":1,\"value\":\"required\",\"label\":\"ENUM_SCR_ATR_REQUIRED\"},{\"id\":2,\"value\":\"validation\",\"label\":\"ENUM_SCR_ATR_VALIDATION\"},{\"id\":3,\"value\":\"style\",\"label\":\"ENUM_SCR_ATR_STYLE\"},{\"id\":4,\"value\":\"label\",\"label\":\"ENUM_SCR_ATR_LABEL\"},{\"id\":5,\"value\":\"value\",\"label\":\"ENUM_SCR_ATR_VALUE\"},{\"id\":6,\"value\":\"session\",\"label\":\"ENUM_SCR_ATR_SESSION\"},{\"id\":7,\"value\":\"variable\",\"label\":\"ENUM_SCR_ATR_VARIABLE\"},{\"id\":8,\"value\":\"component\",\"label\":\"ENUM_SCR_ATR_COMPONENT\"},{\"id\":9,\"value\":\"initialLoad\",\"label\":\"ENUM_SCR_ATR_INITLOAD\"},{\"id\":10,\"value\":\"format\",\"label\":\"ENUM_SCR_ATR_FORMAT\"},{\"id\":11,\"value\":\"numberFormat\",\"label\":\"ENUM_SCR_ATR_NUMFORMAT\"},{\"id\":12,\"value\":\"capitalize\",\"label\":\"ENUM_SCR_ATR_CAPITALIZE\"},{\"id\":13,\"value\":\"visible\",\"label\":\"ENUM_SCR_ATR_VISIBLE\"},{\"id\":14,\"value\":\"readonly\",\"label\":\"ENUM_SCR_ATR_READONLY\"},{\"id\":15,\"value\":\"checked\",\"label\":\"ENUM_SCR_ATR_CHECKED\"},{\"id\":16,\"value\":\"strict\",\"label\":\"ENUM_SCR_ATR_STRICT\"},{\"id\":17,\"value\":\"serverAction\",\"label\":\"ENUM_SCR_ATR_SERVERACTION\"},{\"id\":18,\"value\":\"targetAction\",\"label\":\"ENUM_SCR_ATR_TARGETACTION\"},{\"id\":19,\"value\":\"message\",\"label\":\"ENUM_SCR_ATR_MESSAGE\"},{\"id\":20,\"value\":\"formule\",\"label\":\"ENUM_SCR_ATR_FORMULE\"},{\"id\":21,\"value\":\"optional\",\"label\":\"ENUM_SCR_ATR_OPTIONAL\"},{\"id\":22,\"value\":\"max\",\"label\":\"ENUM_SCR_ATR_MAX\"},{\"id\":23,\"value\":\"movable\",\"label\":\"ENUM_SCR_ATR_MOVABLE\"},{\"id\":24,\"value\":\"printable\",\"label\":\"ENUM_SCR_ATR_PRINTABLE\"},{\"id\":25,\"value\":\"unit\",\"label\":\"ENUM_SCR_ATR_UNIT\"},{\"id\":26,\"value\":\"checkEmpty\",\"label\":\"ENUM_SCR_ATR_CHECKEMPTY\"},{\"id\":27,\"value\":\"checkInitial\",\"label\":\"ENUM_SCR_ATR_CHECKINITIAL\"},{\"id\":28,\"value\":\"checkTarget\",\"label\":\"ENUM_SCR_ATR_CHECKTARGET\"},{\"id\":29,\"value\":\"specific\",\"label\":\"ENUM_SCR_ATR_SPECIFIC\"},{\"id\":30,\"value\":\"timeout\",\"label\":\"ENUM_SCR_ATR_TIMEOUT\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 30, 1, 2, 58);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testDatabaseLongerPaginatedEnum() throws Exception {
    String queryName = "LongerEnum";
    String variables = "\"max\": 45, \"page\": 1";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":2,\"page\":1,\"records\":58,\"rows\":[{\"id\":1,\"value\":\"required\",\"label\":\"ENUM_SCR_ATR_REQUIRED\"},{\"id\":2,\"value\":\"validation\",\"label\":\"ENUM_SCR_ATR_VALIDATION\"},{\"id\":3,\"value\":\"style\",\"label\":\"ENUM_SCR_ATR_STYLE\"},{\"id\":4,\"value\":\"label\",\"label\":\"ENUM_SCR_ATR_LABEL\"},{\"id\":5,\"value\":\"value\",\"label\":\"ENUM_SCR_ATR_VALUE\"},{\"id\":6,\"value\":\"session\",\"label\":\"ENUM_SCR_ATR_SESSION\"},{\"id\":7,\"value\":\"variable\",\"label\":\"ENUM_SCR_ATR_VARIABLE\"},{\"id\":8,\"value\":\"component\",\"label\":\"ENUM_SCR_ATR_COMPONENT\"},{\"id\":9,\"value\":\"initialLoad\",\"label\":\"ENUM_SCR_ATR_INITLOAD\"},{\"id\":10,\"value\":\"format\",\"label\":\"ENUM_SCR_ATR_FORMAT\"},{\"id\":11,\"value\":\"numberFormat\",\"label\":\"ENUM_SCR_ATR_NUMFORMAT\"},{\"id\":12,\"value\":\"capitalize\",\"label\":\"ENUM_SCR_ATR_CAPITALIZE\"},{\"id\":13,\"value\":\"visible\",\"label\":\"ENUM_SCR_ATR_VISIBLE\"},{\"id\":14,\"value\":\"readonly\",\"label\":\"ENUM_SCR_ATR_READONLY\"},{\"id\":15,\"value\":\"checked\",\"label\":\"ENUM_SCR_ATR_CHECKED\"},{\"id\":16,\"value\":\"strict\",\"label\":\"ENUM_SCR_ATR_STRICT\"},{\"id\":17,\"value\":\"serverAction\",\"label\":\"ENUM_SCR_ATR_SERVERACTION\"},{\"id\":18,\"value\":\"targetAction\",\"label\":\"ENUM_SCR_ATR_TARGETACTION\"},{\"id\":19,\"value\":\"message\",\"label\":\"ENUM_SCR_ATR_MESSAGE\"},{\"id\":20,\"value\":\"formule\",\"label\":\"ENUM_SCR_ATR_FORMULE\"},{\"id\":21,\"value\":\"optional\",\"label\":\"ENUM_SCR_ATR_OPTIONAL\"},{\"id\":22,\"value\":\"max\",\"label\":\"ENUM_SCR_ATR_MAX\"},{\"id\":23,\"value\":\"movable\",\"label\":\"ENUM_SCR_ATR_MOVABLE\"},{\"id\":24,\"value\":\"printable\",\"label\":\"ENUM_SCR_ATR_PRINTABLE\"},{\"id\":25,\"value\":\"unit\",\"label\":\"ENUM_SCR_ATR_UNIT\"},{\"id\":26,\"value\":\"checkEmpty\",\"label\":\"ENUM_SCR_ATR_CHECKEMPTY\"},{\"id\":27,\"value\":\"checkInitial\",\"label\":\"ENUM_SCR_ATR_CHECKINITIAL\"},{\"id\":28,\"value\":\"checkTarget\",\"label\":\"ENUM_SCR_ATR_CHECKTARGET\"},{\"id\":29,\"value\":\"specific\",\"label\":\"ENUM_SCR_ATR_SPECIFIC\"},{\"id\":30,\"value\":\"timeout\",\"label\":\"ENUM_SCR_ATR_TIMEOUT\"},{\"id\":31,\"value\":\"field\",\"label\":\"ENUM_SCR_ATR_FIELD\"},{\"id\":32,\"value\":\"width\",\"label\":\"ENUM_SCR_ATR_WIDTH\"},{\"id\":33,\"value\":\"charLength\",\"label\":\"ENUM_SCR_ATR_CHARLENGTH\"},{\"id\":34,\"value\":\"align\",\"label\":\"ENUM_SCR_ATR_ALIGN\"},{\"id\":35,\"value\":\"inputType\",\"label\":\"ENUM_SCR_ATR_INPUTTYPE\"},{\"id\":36,\"value\":\"sortable\",\"label\":\"ENUM_SCR_ATR_SORTABLE\"},{\"id\":37,\"value\":\"hidden\",\"label\":\"ENUM_SCR_ATR_HIDDEN\"},{\"id\":38,\"value\":\"sendable\",\"label\":\"ENUM_SCR_ATR_SENDABLE\"},{\"id\":39,\"value\":\"summaryType\",\"label\":\"ENUM_SCR_ATR_SUMMARYTYPE\"},{\"id\":40,\"value\":\"formatter\",\"label\":\"ENUM_SCR_ATR_FORMATTER\"},{\"id\":41,\"value\":\"formatOptions\",\"label\":\"ENUM_SCR_ATR_FORMATOPTIONS\"},{\"id\":42,\"value\":\"frozen\",\"label\":\"ENUM_SCR_ATR_FROZEN\"},{\"id\":43,\"value\":\"position\",\"label\":\"ENUM_SCR_ATR_POSITION\"},{\"id\":44,\"value\":\"autoload\",\"label\":\"ENUM_SCR_ATR_AUTOLOAD\"},{\"id\":45,\"value\":\"totalize\",\"label\":\"ENUM_SCR_ATR_TOTALIZE\"}]}}},{\"type\":\"end-load\"}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 45, 1, 2, 58);
  }

  // *****************************************************************************************************************//
  // SECURITY TESTS
  // **************************************************************************************************************** //

  /**
   * Test check authenticated user query
   *
   * @throws Exception Error in test
   */

  @Test
  @WithMockCustomUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void testCheckAuthenticatedUserQuery() throws Exception {
    String queryName = "SimpleGetAll";
    String variables = "";
    MvcResult mvcResult = mockMvc.perform(post("/action/data/" + queryName)
        .with(csrf())
        .content("{" + variables + "\"option\":\"grid-and-chart\",\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();
    assertResultSecurityJson(queryName, result, 12, 1, 1, 12);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testQueryServiceNoPostProcess() throws Exception {
    String queryName = "testQueryWithOutPostProcess";
    String variables = "";
    String expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":7,\"rows\":[{\"id\":2,\"value\":null},{\"id\":4,\"value\":null},{\"id\":5,\"value\":\"BMW\"},{\"id\":3,\"value\":\"Mercedes\"},{\"id\":7,\"value\":\"Skoda\"},{\"id\":1,\"value\":\"Toyota\"},{\"id\":6,\"value\":\"Volkswagen\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]";

    String result = performRequest(queryName, variables, DATABASE, expected);
    assertResultServiceJson(queryName, result, 7);
  }

  /**
   * Test of big data evaluation
   *
   * @throws Exception Test error
   */
  @Test
  void testBigDataEvalPerformance() throws Exception {
    String queryName = "NumTstLst";
    String result = performRequest(queryName, "", DATABASE);
    assertResultServiceJson(queryName, result, 30, 1, 200, 6000);
  }

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       Result
   * @param expectedRows Expected rows
   * @param page         Page
   * @param totalPages   Total pages
   * @param records      Total records
   * @return Result list
   * @throws Exception Test error
   */
  public ArrayNode assertResultSecurityJson(String queryName, String result, int expectedRows, int page, int totalPages, int records) throws Exception {
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

    return dataListRows;
  }

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    query name
   * @param result       Result
   * @param expectedRows Expected rows
   * @return Result list
   * @throws Exception Test error
   */
  private ArrayNode assertResultJson(String queryName, String result, int expectedRows) throws Exception {
    return assertResultJson(queryName, result, expectedRows, 1, 1, expectedRows);
  }

  /**
   * Asserts the JSON in the response
   *
   * @param queryName    Query name
   * @param result       Result
   * @param expectedRows Expected rows
   * @param page         Page
   * @param totalPages   Total pages
   * @param records      Total records
   * @return Result list
   * @throws Exception Test error
   */
  public ArrayNode assertResultJson(String queryName, String result, int expectedRows, int page, int totalPages, int records) throws Exception {
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

    return dataListRows;
  }

  /**
   * Performs the mock request and returns the response as a string
   *
   * @param queryName Query ID
   * @param variables Variables
   * @param database  Database
   * @param expected  Expected result
   * @return Output
   * @throws Exception Error performing request
   */
  private String performRequest(String queryName, String variables, String database, String expected) throws Exception {
    return performRequest(queryName, variables, database, expected, null);
  }

  /**
   * Performs the mock request and returns the response as a string
   *
   * @param queryName Query ID
   * @param variables Variables
   * @param database  Database
   * @return Output
   * @throws Exception Error performing request
   */
  private String performRequest(String queryName, String variables, String database) throws Exception {
    return performRequest(queryName, variables, database, null, null);
  }

  /**
   * Performs the mock request and returns the response as a string
   *
   * @param queryName Query ID
   * @param variables Variables
   * @param database  Database
   * @return Output
   * @throws Exception Error performing request
   */
  private String performRequest(String queryName, String variables, String database, String expected, Map<String, Object> sessionAttr) throws Exception {
    setParameter("database", database);
    MockHttpServletRequestBuilder requestBuilder = post("/action/data/" + queryName)
      .with(csrf())
      .session(session)
      .content("{" + variables + "}")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON);

    if (sessionAttr != null) {
      requestBuilder.sessionAttrs(sessionAttr);
    }

    // Perform request
    ResultActions mockMvcPerform = mockMvc.perform(requestBuilder)
      .andExpect(status().isOk());

    if (expected != null) {
      logger.warn("Result: {}", mockMvcPerform.andReturn().getResponse().getContentAsString());
      logger.warn("Expect: {}", expected);
      mockMvcPerform.andExpect(content().json(expected));
    }

    MvcResult mvcResult = mockMvcPerform.andReturn();
    return mvcResult.getResponse().getContentAsString();
  }

  /**
   * Check if current database is an in memory database
   *
   * @return <code>true</code> if is memory database
   * @throws Exception Exception {@link Exception}
   */
  private boolean isInMemoryDatabase() throws Exception {
    List<String> validDatabases = Arrays.asList("hsqldb", "h2");
    return validDatabases.contains(aweDatabaseContextHolder.getDatabaseType(dataSource));
  }

  /**
   * Set parameter in session
   *
   * @param name  Parameter name
   * @param value Parameter value
   */
  private void setParameter(String name, String value) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(String.format("/session/set/%s", name))
        .with(csrf())
        .param("value", value)
        .session(session))
      .andReturn();
    mvcResult.getResponse().getContentAsString();
  }
}
