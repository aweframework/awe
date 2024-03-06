package com.almis.awe.test.integration.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Query service Tests")
@WithMockUser
class QueryServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private QueryService queryService;

  /**
   * Test call java service by Qualifier name
   *
   * @throws AWException AWE exception
   */
  @Test
  void testCallServiceByQualifierName() throws AWException {
    // Launch query service
    ServiceData serviceData = queryService.launchQuery("testServiceByQualifier");
    assertNotNull(serviceData);
    assertTrue(serviceData.isValid());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceBeanParameter() throws Exception {
    // Launch query service
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("name", "Earth");
    parameters.put("rotationPeriod", "1d");
    parameters.put("orbitalPeriod", "365d 6h");
    parameters.put("diameter", "12313");
    parameters.put("climate", "Mixed");
    parameters.put("gravity", "9,8m/s");
    parameters.put("terrain", "Mixed");
    parameters.put("surfaceWater", "80%");
    parameters.put("population", 1231231L);
    parameters.put("created", "23/10/2015");
    parameters.put("edited", "20/05/2018");
    parameters.put("url", "https://www.dummy.url");
    ServiceData serviceData = queryService.launchQuery("testServiceBeanParameter", parameters);
    assertNotNull(serviceData);
    assertNotNull(serviceData.getDataList());
    assertTrue(serviceData.isValid());
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceBeanParameterList() throws Exception {
    // Launch query service
    ObjectMapper mapper = DataListUtil.getMapper();
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    ObjectNode parameters = nodeFactory.objectNode();
    parameters.set("name", mapper.readTree("[\"Earth\",\"Mars\",\"Jupiter\"]"));
    parameters.set("rotationPeriod", mapper.readTree("[\"1d\",\"4d\",\"8h\"]"));
    parameters.set("orbitalPeriod", mapper.readTree("[\"365d\",\"632d\",\"4y\"]"));
    parameters.set("diameter", mapper.readTree("[\"123123\",\"12123\",\"4345334\"]"));
    parameters.set("climate", mapper.readTree("[\"mixed\",\"dry\",\"stormy\"]"));
    parameters.set("gravity", mapper.readTree("[\"9,8m/s2\",\"4,8m/s2\",\"25,8m/s2\"]"));
    parameters.set("terrain", mapper.readTree("[\"mixed\",\"rock\",\"gas\"]"));
    parameters.set("surfaceWater", mapper.readTree("[\"80%\",\"2%\",\"12%\"]"));
    parameters.set("population", mapper.readTree("[13421341,4,1]"));
    parameters.set("created", mapper.readTree("[\"23/10/1978\",\"22/05/2012\",\"22/05/2019\"]"));
    parameters.set("edited", mapper.readTree("[\"23/10/2008\",\"22/05/2016\",\"22/06/2019\"]"));
    parameters.set("url", mapper.readTree("[\"https://www.dummy.url\",\"https://www.dummy.url\",\"https://www.dummy.url\"]"));
    ServiceData serviceData = queryService.launchQuery("testServiceBeanParameterList", parameters);
    assertNotNull(serviceData);
    assertNotNull(serviceData.getDataList());
    assertTrue(serviceData.isValid());
    assertEquals(3, serviceData.getDataList().getRows().size());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceBeanListParameter() throws Exception {
    // Launch query service
    ObjectMapper mapper = DataListUtil.getMapper();
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    ObjectNode parameters = nodeFactory.objectNode();
    parameters.set("nameList", mapper.readTree("[\"Earth\",\"Mars\",\"Jupiter\"]"));
    parameters.set("populationList", mapper.readTree("[13421341,4,1]"));
    ServiceData serviceData = queryService.launchQuery("testServiceBeanListParameter", parameters);
    assertNotNull(serviceData);
    assertTrue(serviceData.isValid());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceJsonBeanParameter() throws Exception {
    // Launch query service
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    ObjectNode value = JsonNodeFactory.instance.objectNode();
    value.put("name", "Earth");
    value.put("rotationPeriod", "1d");
    value.put("orbitalPeriod", "365d 6h");
    value.put("diameter", "12313");
    value.put("climate", "Mixed");
    value.put("gravity", "9,8m/s");
    value.put("terrain", "Mixed");
    value.put("surfaceWater", "80%");
    value.put("population", 1231231L);
    value.put("created", "2015-10-23");
    value.put("edited", "2018-05-20");
    value.put("url", "https://www.dummy.url");
    parameters.set("value", value);
    ServiceData serviceData = queryService.launchQuery("testServiceJsonBeanParameter", parameters);
    assertNotNull(serviceData);
    assertTrue(serviceData.isValid());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceJsonNullBean() throws Exception {
    // Launch query service
    ServiceData serviceData = queryService.launchQuery("testServiceJsonBeanParameter", JsonNodeFactory.instance.objectNode());
    assertNotNull(serviceData);
    assertTrue(serviceData.isValid());
  }

  /**
   * Test call java service with bean parameter
   *
   * @throws AWException AWE exception
   */
  @Test
  void testServiceJsonParameter() throws Exception {
    // Launch query service
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    ObjectNode value = JsonNodeFactory.instance.objectNode();
    value.put("name", "Earth");
    value.put("rotationPeriod", "1d");
    value.put("orbitalPeriod", "365d 6h");
    value.put("diameter", "12313");
    value.put("climate", "Mixed");
    value.put("gravity", "9,8m/s");
    value.put("terrain", "Mixed");
    value.put("surfaceWater", "80%");
    value.put("population", 1231231L);
    value.put("created", "23/10/2015");
    value.put("edited", "20/05/2018");
    value.put("url", "https://www.dummy.url");
    parameters.set("value", value);
    ServiceData serviceData = queryService.launchQuery("testServiceJsonParameter", parameters);
    assertNotNull(serviceData);
    assertTrue(serviceData.isValid());
    assertEquals("tutu", serviceData.getVariable(AweConstants.ACTION_MESSAGE_TITLE).getStringValue());
    assertEquals("lala", serviceData.getVariable(AweConstants.ACTION_MESSAGE_DESCRIPTION).getStringValue());
  }

  /**
   * Test of query not defined.
   */
  @Test
  void testDatabaseQueryNotDefined() {
    assertThrows(AWException.class, () -> queryService.launchQuery("QueryNotDefined"));
  }
}
