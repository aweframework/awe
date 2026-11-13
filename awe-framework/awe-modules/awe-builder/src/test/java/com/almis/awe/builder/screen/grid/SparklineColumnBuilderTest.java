package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.SparklineType;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sparkline column builder")
class SparklineColumnBuilderTest {

  @Test
  @DisplayName("A sparkline column keeps its drawing options")
  void sparklineColumnKeepsItsOptions() throws AWException {
    Column column = new SparklineColumnBuilder()
      .setId("load")
      .setLabel("LOAD")
      .setSparklineType(SparklineType.BAR)
      .setSparklineColor("#3b82f6")
      .setSparklineNegativeColor("#d64550")
      .setSparklineLastPoint(true)
      .setSparklineMinMax(true)
      .build();

    assertEquals("sparkline", column.getComponentType());
    assertEquals("load", column.getId());
    assertEquals("bar", column.getSparklineType());
    assertEquals("#3b82f6", column.getSparklineColor());
    assertEquals("#d64550", column.getSparklineNegativeColor());
    assertTrue(column.getSparklineLastPoint());
    assertTrue(column.getSparklineMinMax());
  }

  @Test
  @DisplayName("Without options the column leaves the defaults to the clients")
  void sparklineColumnWithoutOptions() throws AWException {
    Column column = new SparklineColumnBuilder().setId("load").build();

    assertEquals("sparkline", column.getComponentType());
    assertNull(column.getSparklineType());
    assertNull(column.getSparklineColor());
    assertNull(column.getSparklineNegativeColor());
    assertNull(column.getSparklineLastPoint());
    assertNull(column.getSparklineMinMax());
  }
}
