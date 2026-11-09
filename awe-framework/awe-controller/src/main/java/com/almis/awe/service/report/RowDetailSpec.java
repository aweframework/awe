package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.awe.model.entities.screen.component.grid.Grid;

import java.util.List;

/**
 * Row detail that a printed grid asks for (`print="true"`): a text template or a nested grid
 *
 * @param template       Template of the text (null in grid mode)
 * @param nestedGrid     Grid of the detail (null in text mode)
 * @param nestedTemplate Report grid with the columns of the nested grid, which ADE prints as the nested table
 * @param nestedFields   Fields of the nested table, in the order of its columns
 * @param maxRows        Maximum rows of the nested table of each row
 */
record RowDetailSpec(String template, Grid nestedGrid, ReportGrid nestedTemplate, List<String> nestedFields, int maxRows) {

  /** Hidden field of the report data that holds the detail of each row */
  static final String FIELD = "_row_detail_";

  boolean isGrid() {
    return nestedGrid != null;
  }
}
