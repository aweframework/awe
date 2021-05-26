package com.almis.awe.model.entities.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Table pojo tests
 *
 * @author pgarcia
 */
class TableTest {

  /**
   * Test of cell data null
   */
  @Test
  void testTable() {
    // Prepare
    Table table = new Table()
            .setId("tabla")
            .setAlias("alias")
            .setSchema("schema");

    Table queryTable = Table.builder().build()
            .setQuery("query")
            .setAlias("alias");

     Table copiedQueryTable = queryTable.copy()
       .setAlias(null);

    Table copiedTable = table.copy()
      .setAlias(null);

    Table copiedTableWithoutSchema = copiedTable.copy()
      .setSchema(null);

    // Run
    assertEquals("schema.tabla alias", table.toString());
    assertEquals("([SUBQUERY] query) as alias", queryTable.toString());
    assertEquals("([SUBQUERY] query)", copiedQueryTable.toString());
    assertEquals("schema.tabla", copiedTable.toString());
    assertEquals("tabla", copiedTableWithoutSchema.toString());
  }
}