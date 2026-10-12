package com.almis.awe.builder.screen.criteria;

import com.almis.awe.builder.enumerates.Component;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DateRangeCriteriaBuilderTest {

  @Test
  void buildsADateRangeCriterionWithItsPresets() throws AWException {
    AbstractCriteria criterion = new DateRangeCriteriaBuilder()
      .setPresets("1M,3M,YTD,12M,3Y,ALL")
      .setValue("01/01/2026,31/01/2026")
      .setId("period")
      .build();

    assertEquals(Component.DATE_RANGE.toString(), criterion.getComponentType());
    assertEquals("date-range", criterion.getComponentType());
    assertEquals("1M,3M,YTD,12M,3Y,ALL", criterion.getPresets());
    assertEquals("01/01/2026,31/01/2026", criterion.getValue());
    assertEquals("period", criterion.getId());
  }

  @Test
  void buildsADateRangeCriterionWithoutPresets() throws AWException {
    AbstractCriteria criterion = new DateRangeCriteriaBuilder().setId("period").build();

    assertNull(criterion.getPresets());
  }
}
