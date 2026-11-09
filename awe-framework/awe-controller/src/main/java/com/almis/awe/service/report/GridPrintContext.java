package com.almis.awe.service.report;

import com.almis.awe.model.entities.screen.component.grid.Column;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the design of one grid needs besides the columns the client sends: the column definitions of the screen (read by
 * name, the print attributes are not sent by the clients), the output profile, the rewriting of the cells and the fields
 * that must be read from the data without being a column of the report (secondary and status fields)
 *
 * @param definitions Column definitions of the screen by column name
 * @param spreadsheet The report is a spreadsheet: secondary lines go to their own column
 * @param printer     Cell rewriting rules, filled while the columns are generated
 * @param groupLevels Fields that group the rows now (the levels chosen at runtime), empty when they are not grouped
 * @param subtotals   Each group is closed with a subtotal row (`group-subtotals`, on unless it is `false`)
 * @param rowDetail   Row detail to print under each row, null when the grid prints none
 * @param detailCells Detail of each data row, filled while the data is read
 * @param extraFields Fields to read from the data besides the columns
 */
record GridPrintContext(Map<String, Column> definitions, boolean spreadsheet, GridCellPrinter printer, List<String> groupLevels,
                        boolean subtotals, RowDetailSpec rowDetail, List<Object> detailCells,
                        Set<String> extraFields) {

  GridPrintContext(Map<String, Column> definitions, boolean spreadsheet, GridCellPrinter printer, List<String> groupLevels,
                  boolean subtotals, RowDetailSpec rowDetail) {
    this(definitions, spreadsheet, printer, groupLevels, subtotals, rowDetail, new ArrayList<>(), new LinkedHashSet<>(groupLevels));
  }

  /**
   * Retrieve the definition of a column
   *
   * @param name Column name
   * @return Column definition, null when the screen has none with this name
   */
  Column definition(String name) {
    return name == null ? null : definitions.get(name);
  }
}
