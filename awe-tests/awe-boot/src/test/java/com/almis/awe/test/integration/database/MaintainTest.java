package com.almis.awe.test.integration.database;

import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.type.MaintainType;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static com.almis.awe.test.integration.util.TestUtil.assertResultJson;
import static com.almis.awe.test.integration.util.TestUtil.readFileAsText;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class used for testing maintain operations
 */
@DisplayName("Maintain tests")
@Slf4j
@WithMockUser
@Transactional
public class MaintainTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private MaintainService maintainService;

  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    session = new MockHttpSession();
  }

  @AfterEach
  void cleanData() throws Exception {
    cleanUpData("CleanUp");
    cleanUpData("CleanUpScreenConfiguration");
    cleanUpData("CleanUpSequence");
  }

  private void cleanUpData(String method) throws Exception {
    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug(" Cleaning up all the mess... ");
    logger.debug("--------------------------------------------------------------------------------------");

    mockMvc.perform(post("/action/maintain/" + method)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  /**
   * Launch a maintain test
   *
   * @param maintainName Maintain name
   * @param variables    Variables
   * @param expected     Expected value
   * @return Maintain result
   * @throws Exception exception
   */
  private String launchMaintain(String maintainName, String variables, String expected) throws Exception {
    return launchPostRequest("maintain", maintainName, variables, expected);
  }

  /**
   * Launch a query test
   *
   * @param variables Variables
   * @param expected  Expected value
   * @return Maintain result
   * @throws Exception exception
   */
  private String launchQuery(String variables, String expected) throws Exception {
    return launchPostRequest("data", "CheckRollback", variables, expected);
  }

  /**
   * Launch a request test
   *
   * @param type      Request type
   * @param name      Name
   * @param variables Variables
   * @param expected  Expected value
   * @return Maintain result
   * @throws Exception exception
   */
  private String launchPostRequest(String type, String name, String variables, String expected) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/action/" + type + "/" + name)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{" + variables + "\"max\":30}")
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
  void testSimpleSingleInsert() throws Exception {
    String maintainName = "SimpleSingleInsert";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleInsertFromVariableValue() throws Exception {
    for (int i = 0; i < 5; i++) {
      String maintainName = "SimpleSingleInsertFromVariableValue";
      String variables = "\"variable\":\"AWEBOOT-TEST-" + i + "\",";
      String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
      String result = launchMaintain(maintainName, variables, expected);
      logger.debug(result);
      assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
        new MaintainResultDetails(MaintainType.INSERT, 1L)
      });
    }
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSingleInsertWithSequence() throws Exception {
    String maintainName = "SingleInsertWithSequence";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetNextSequence() throws Exception {
    Integer sequenceValue = maintainService.getNextSequenceValue("ThmKey");
    assertSame(20, sequenceValue);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testMultipleInsertWithSequence() throws Exception {
    String maintainName = "MultipleInsertWithSequence";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSingleAndMultipleInsertWithSequence() throws Exception {
    String maintainName = "SingleAndMultipleInsertWithSequence";
    String variables = "\"variable\": [\"tutu1\", \"tutu2\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 4, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 2L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testMultipleInsertWithSequenceTwice() throws Exception {
    String maintainName = "MultipleInsertWithSequence";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";

    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });

    variables = "\"variable\": [\"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\"],";
    result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleUpdate() throws Exception {
    String maintainName = "SimpleSingleInsert";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });

    maintainName = "SimpleSingleUpdate";
    variables = "";
    expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleDelete() throws Exception {
    String maintainName = "CleanUp";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"DELETE\",\"rowsAffected\":0},{\"operationType\":\"DELETE\",\"rowsAffected\":0},{\"operationType\":\"DELETE\",\"rowsAffected\":0}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 3, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.DELETE, 0L),
      new MaintainResultDetails(MaintainType.DELETE, 0L),
      new MaintainResultDetails(MaintainType.DELETE, 0L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleInsertAudit() throws Exception {


    String maintainName = "SimpleSingleInsertAudit";
    String variables = "";
    setParameter("user", "LaloElMalo");
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleInsertAuditWithSequenceWithoutVariable() throws Exception {


    String maintainName = "InsertAuditSequenceWithoutVariable";
    String variables = "\"nam\": \"AWEBOOT-TEST-0\", \"act\":0,";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.info(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSimpleSingleInsertMultipleAuditWithSequenceWithoutVariable() throws Exception {


    String maintainName = "InsertAuditSequenceWithoutVariableMultiple";
    String variables = "\"nam\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\"], \"act\":[0, 0, 1],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.info(result);
    assertResultJson(maintainName, result, 6, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)

    });


  }

  /**
   * Launch a simple single insert from variable
   *
   * @throws Exception exception
   */
  private void launchSimpleSingleInsertFromVariable() throws Exception {
    for (int i = 0; i < 5; i++) {
      String maintainName = "SimpleSingleInsertFromVariable";
      String variables = "\"variable\":\"AWEBOOT-TEST-" + i + "\",";
      String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
      String result = launchMaintain(maintainName, variables, expected);
      logger.debug(result);
      assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
        new MaintainResultDetails(MaintainType.INSERT, 1L)
      });
    }
  }

  /**
   * Launch a simple single insert from variable optional
   *
   * @throws Exception exception
   */
  @Test
  void launchSimpleInsertUpdateDeleteFromVariableOptional() throws Exception {
    String maintainName = "SimpleSingleInsertUpdateDeleteFromVariableOptional";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"DELETE\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSingleUpdateWithVariableListAudit() throws Exception {
    launchSimpleSingleInsertFromVariable();

    String maintainName = "SingleUpdateWithVariableListAudit";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\", \"AWEBOOT-TEST-4\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":5},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 6, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 5L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testSingleUpdateWithVariableListAuditBatched() throws Exception {
    launchSimpleSingleInsertFromVariable();

    String maintainName = "SingleUpdateWithVariableListAuditBatched";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\", \"AWEBOOT-TEST-4\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":5},{\"operationType\":\"AUDIT\",\"rowsAffected\":5}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 5L),
      new MaintainResultDetails(MaintainType.AUDIT, 5L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testMultipleUpdate() throws Exception {
    launchSimpleSingleInsertFromVariable();

    String maintainName = "MultipleUpdate";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\", \"AWEBOOT-TEST-4\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 5, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testMultipleUpdateAudit() throws Exception {
    launchSimpleSingleInsertFromVariable();

    String maintainName = "MultipleUpdateAudit";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\", \"AWEBOOT-TEST-4\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 10, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testMultipleUpdateAuditBatched() throws Exception {
    launchSimpleSingleInsertFromVariable();

    String maintainName = "MultipleUpdateAuditBatched";
    String variables = "\"variable\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\", \"AWEBOOT-TEST-4\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":5},{\"operationType\":\"AUDIT\",\"rowsAffected\":5}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 5L),
      new MaintainResultDetails(MaintainType.AUDIT, 5L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testInsertUpdateDelete() throws Exception {
    String maintainName = "InsertUpdateDelete";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"DELETE\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 3, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testInsertUpdateDeleteWithVariables() throws Exception {
    String maintainName = "InsertUpdateDeleteWithVariables";
    String variables = "\"var1\": \"AWEBOOT-TEST-0\", \"var2\": \"AWEBOOT-TEST-1\",";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"UPDATE\",\"rowsAffected\":1},{\"operationType\":\"DELETE\",\"rowsAffected\":2}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 5, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 2L)
    });


  }

  /**
   * Test of insert Clob
   *
   * @throws Exception Test error
   */
  @Test
  void testInsertClob() throws Exception {
    String maintainName = "insertClobData";
    String longFile = readFileAsText("/static/tree_data.json");
    String variables = "\"file\": \"" + StringUtil.fixJSonValue(longFile) + "\",";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
    });
  }

  /**
   * Test of insert Clob with special characters like '$'
   *
   * @throws Exception Test error
   */
  @Test
  void testInsertClobWithSpecialCharacters() throws Exception {
    String maintainName = "insertClobData";
    String longFile = readFileAsText("/static/clob.txt");
    String variables = "\"file\": \"" + StringUtil.fixFormatValue(StringUtil.fixJSonValue(longFile)) + "\",";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
    });
  }

  /**
   * Test of insert Clob
   *
   * @throws Exception Test error
   */
  @Test
  void testUpdateClob() throws Exception {
    String maintainName = "updateClobData";
    String variables = "\"file\": \"\",\"flag\":\"0\",";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"UPDATE\",\"rowsAffected\":0}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.UPDATE, 0L),
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testRollback() throws Exception {
    String maintainName = "TestRollback";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"There was a problem calling a maintain process\",\"title\":\"Error in maintain operation\",\"type\":\"error\"}},{\"type\":\"cancel\"}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);

    expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\"}]";
    result = launchQuery(variables, expected);
    logger.debug(result);


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testCommit() throws Exception {
    String maintainName = "TestCommit";
    String variables = "";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"There was a problem calling a maintain process\",\"title\":\"Error in maintain operation\",\"type\":\"error\"}},{\"type\":\"cancel\"}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);

    expected = "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"HISope\":\"test\"}]}}},{\"type\":\"end-load\"}]";
    result = launchQuery(variables, expected);
    logger.debug(result);


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGridMultiple() throws Exception {
    String maintainName = "GridMultiple";
    String variables = "\"grid-RowTyp\": [\"INSERT\", \"INSERT\"], \"nam\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\"], \"act\":[0, 0],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 4, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGridMultipleInsertSingle() throws Exception {
    String maintainName = "GridMultiple";
    String variables = "\"grid-RowTyp\": [\"INSERT\"], \"nam\": [\"AWEBOOT-TEST-0\"], \"act\":[0],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testAddFavourite() throws Exception {
    String maintainName = "addToFavourites";
    String variables = "\"user\": \"test\", \"option\": \"themes\",";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });

    variables = "\"user\": \"test\", \"option\": \"users\",";
    result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  @Tag("NotOracleDatabase")
  void testGridMultipleAutoIncrement() throws Exception {
    String maintainName = "GridMultipleAutoIncrement";
    String variables = "\"grid-RowTyp\": [\"INSERT\", \"INSERT\", \"UPDATE\", \"DELETE\"], \"id\": [null, null, 101, 100], \"name\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\", \"AWEBOOT-TEST-2\", \"AWEBOOT-TEST-3\"], \"email\":[\"test@test.es\", \"test2@test.es\", \"test3@test.es\", \"test4@test.es\"],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 4, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 1L),
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testIncludeTarget() throws Exception {
    String maintainName = "testInclude";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, "", expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 3, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.UPDATE, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 1L)
    });


  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGridMultipleDeleteMultiple() throws Exception {


    String maintainName = "GridMultiple";
    String variables = "\"grid-RowTyp\": [\"INSERT\", \"INSERT\"], \"nam\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\"], \"act\":[0, 0],";
    String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 4, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.INSERT, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });

    Set<String> keys = new HashSet<>();
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode messageAction = (ObjectNode) resultList.get(1);
    ObjectNode messageParameters = (ObjectNode) messageAction.get("parameters");
    ArrayNode resultDetails = (ArrayNode) messageParameters.get("result_details");
    for (JsonNode resultDetail : resultDetails) {
      ObjectNode resultNode = (ObjectNode) resultDetail;
      ObjectNode parameterMap = (ObjectNode) resultNode.get("parameterMap");
      keys.add(parameterMap.get("varKey").asText());
    }

    maintainName = "GridMultiple";
    String key = "\"key\":[" + Joiner.on(", ").join(keys) + "],";
    variables = "\"grid-RowTyp\": [\"DELETE\", \"DELETE\"], \"nam\": [\"AWEBOOT-TEST-0\", \"AWEBOOT-TEST-1\"], \"act\":[0, 0]," + key;
    expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
    result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
    assertResultJson(maintainName, result, 4, new MaintainResultDetails[]{
      new MaintainResultDetails(MaintainType.DELETE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L),
      new MaintainResultDetails(MaintainType.DELETE, 1L),
      new MaintainResultDetails(MaintainType.AUDIT, 1L)
    });
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @ParameterizedTest
  @CsvSource(value = {
    "ServeNoParams|[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]",
    "ServeMessageParams|[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"message\",\"title\":\"Operation successful\",\"type\":\"ok\"}}]",
    "ServeTitleMessageParams|[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"message\",\"title\":\"title\",\"type\":\"ok\"}}]"
  }, delimiter = '|')
  void testService(String maintainName, String expected) throws Exception {
    String result = launchMaintain(maintainName, "", expected);
    logger.debug(result);
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testRetrieveDataAndInsertAfter() throws Exception {
    String maintainName = "testRetrieveDataAndInsertAfter";
    String variables = "";
    setParameter("user", "LaloElMalo");
    String expected = "[{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"INSERT\",\"rowsAffected\":1}]}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.debug(result);
  }

  /**
   * Test of maintain with service and date parameters
   *
   * @throws Exception Test error
   */
  @Test
  void testServiceWithDates() throws Exception {
    String maintainName = "testMaintainWithDates";
    String variables = "";
    setParameter("user", "LaloElMalo");
    String expected = "[{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]";
    String result = launchMaintain(maintainName, variables, expected);
    logger.info(result);
  }

  // *****************************************************************************************************************//
  // EMAIL TESTS
  // **************************************************************************************************************** //

  /**
   * Sends an email created with the AWE XML format
   */
  @Test
  void testXMLEmail() {
    String maintainName = "SchTskEmaRep";
    String variables = "";
    Exception ex = null;

    try {
      mockMvc.perform(post("/action/maintain/" + maintainName)
          .header("Authorization", "16617f0d-97ee-4f6b-ad54-905d6ce3c328")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{" + variables + "\"max\":30}")
          .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    } catch (Exception e) {
      ex = e;
    }

    assertNull(ex);
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