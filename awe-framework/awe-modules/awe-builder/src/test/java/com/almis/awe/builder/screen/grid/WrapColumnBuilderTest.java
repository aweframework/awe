package com.almis.awe.builder.screen.grid;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Wrapping column attributes in the column builders")
class WrapColumnBuilderTest {

  @Test
  @DisplayName("A column built with wrap and a maximum number of lines keeps them")
  void columnKeepsWrapAttributes() throws AWException {
    Column column = new TextColumnBuilder().setId("description").setWrap(true).setMaxLines(3).build();

    assertTrue(column.getWrap());
    assertEquals(3, column.getMaxLines());
    assertTrue(column.isWrapped());
  }

  @Test
  @DisplayName("A column built without them does not wrap")
  void columnWithoutWrapAttributes() throws AWException {
    Column column = new TextColumnBuilder().setId("description").build();

    assertNull(column.getWrap());
    assertNull(column.getMaxLines());
    assertFalse(column.isWrapped());
  }
}
