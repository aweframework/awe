package com.almis.awe.model.entities.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Transition field tests
 */
class TransitionFieldTest {

  @Test
  void toStringDoesNotFailWhenThereIsNoField() {
    assertEquals("null", new TransitionField().toString());
  }

  @Test
  void toStringDelegatesToTheField() {
    SqlField field = new Constant().setValue("1").setType("INTEGER");

    assertEquals(field.toString(), new TransitionField().setField(field).toString());
  }
}
