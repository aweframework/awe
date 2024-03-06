package com.almis.awe.test.integration.util;

import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class TestUtil {

  /**
   * Asserts the JSON in the response
   *
   * @param maintainName            Maintain name
   * @param result                  Result
   * @param expectedOperationNumber Expected operations number
   * @param expectedOperations      Expected operations
   * @throws Exception Error in asser
   */
  public static void assertResultJson(String maintainName, String result, int expectedOperationNumber, MaintainResultDetails[] expectedOperations) throws Exception {
    ArrayNode resultList = (ArrayNode) DataListUtil.getMapper().readTree(result);
    ObjectNode messageAction = (ObjectNode) resultList.get(1);
    assertEquals("message", messageAction.get("type").textValue());
    ObjectNode messageParameters = (ObjectNode) messageAction.get("parameters");
    assertEquals(4, messageParameters.size());
    ArrayNode resultDetails = (ArrayNode) messageParameters.get("result_details");
    assertTrue(expectedOperationNumber <= resultDetails.size());

    if (expectedOperationNumber > 0 && expectedOperations != null) {
      for (int i = 0; i < resultDetails.size(); i++) {
        ObjectNode operationDetails = (ObjectNode) resultDetails.get(i);
        MaintainResultDetails expected = expectedOperations[i];

        assertEquals(expected.getOperationType().name(), operationDetails.get("operationType").asText());
        assertTrue(expected.getRowsAffected() <= operationDetails.get("rowsAffected").asLong());
      }
    }

    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug("There are " + resultDetails.size() + " operations as a result of launching maintain " + maintainName);
    logger.debug("--------------------------------------------------------------------------------------");
  }

  /**
   * Read a test file as Text
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static String readFileAsText(String path) throws IOException {
    Resource resource = new ClassPathResource(path);
    return FileUtils.readFileToString(resource.getFile(), StandardCharsets.UTF_8.name());
  }
}
