package com.almis.awe.builder.screen.grid;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bar column attributes in the column builders")
class BarColumnBuilderTest {

  @Test
  @DisplayName("A column built with a bar scale, a second field, a stack and leaf only keeps them")
  void columnKeepsBarAttributes() throws AWException {
    Column column = new TextColumnBuilder().setId("progress")
      .setBarMin(-10.0)
      .setBarMax(40.0)
      .setBarSecondField("target")
      .setBarStackFields("done,doing")
      .setBarLeafOnly(true)
      .build();

    assertEquals(-10.0, column.getBarMin());
    assertEquals(40.0, column.getBarMax());
    assertEquals("target", column.getBarSecondField());
    assertEquals("done,doing", column.getBarStackFields());
    assertTrue(column.getBarLeafOnly());
  }

  @Test
  @DisplayName("A column built without them keeps them unset")
  void columnWithoutBarAttributes() throws AWException {
    Column column = new TextColumnBuilder().setId("progress").build();

    assertNull(column.getBarMin());
    assertNull(column.getBarMax());
    assertNull(column.getBarSecondField());
    assertNull(column.getBarStackFields());
    assertNull(column.getBarLeafOnly());
  }
}
