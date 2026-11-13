package com.almis.awe.builder.screen.criteria;

import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploaderCriteriaBuilderTest {

  @Test
  void buildWithoutOptionsKeepsTheDefaults() {
    AbstractCriteria criteria = new UploaderCriteriaBuilder().build();

    assertNull(criteria.getDropZone());
    assertNull(criteria.getCapture());
    assertNull(criteria.getDestination());
  }

  @Test
  void buildWithDropZoneAndCapture() {
    AbstractCriteria criteria = new UploaderCriteriaBuilder()
      .setDestination("docs")
      .setDropZone(true)
      .setCapture("environment")
      .build();

    assertEquals("docs", criteria.getDestination());
    assertTrue(criteria.getDropZone());
    assertEquals("environment", criteria.getCapture());
  }
}
