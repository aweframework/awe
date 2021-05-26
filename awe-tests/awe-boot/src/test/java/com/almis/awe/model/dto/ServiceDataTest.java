package com.almis.awe.model.dto;

import com.almis.awe.model.type.AnswerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Service data tests
 *
 * @author pgarcia
 */
class ServiceDataTest {

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void testServiceDataCopy() throws Exception {
    // Prepare
    ServiceData serviceData = new ServiceData();
    serviceData.setTitle("aaaa");
    serviceData.setMessage("bbbb");
    serviceData.setType(AnswerType.ERROR);

    // Run
    ServiceData serviceDataCopy = serviceData.copy();

    // Assert
    assertEquals("aaaa", serviceDataCopy.getTitle());
    assertEquals("bbbb", serviceDataCopy.getMessage());
    assertEquals(AnswerType.ERROR, serviceDataCopy.getType());
    assertFalse(serviceDataCopy.isValid());
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void testServiceDataCopyConstructor() throws Exception {
    // Prepare
    ServiceData serviceData = new ServiceData();
    serviceData.setTitle("aaaa");
    serviceData.setMessage("bbbb");
    serviceData.setType(AnswerType.WARNING);

    // Run
    ServiceData serviceDataCopy = new ServiceData(serviceData);

    // Assert
    assertEquals("aaaa", serviceDataCopy.getTitle());
    assertEquals("bbbb", serviceDataCopy.getMessage());
    assertEquals(AnswerType.WARNING, serviceDataCopy.getType());
    assertTrue(serviceDataCopy.isValid());
  }
}