package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.ColumnsPanel;
import com.almis.awe.builder.screen.base.AbstractColumnBuilder;
import com.almis.awe.builder.screen.base.AbstractComponentBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.component.GridAttributes;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * @author dfuentes
 */
@Getter(AccessLevel.PRIVATE)
public class GridBuilder extends AbstractComponentBuilder<GridBuilder, Grid> {

  private final GridAttributes<GridBuilder> gridAttributes;
  private boolean savedSearches;

  public GridBuilder() {
    super();
    this.gridAttributes = new GridAttributes<>(this);
  }

  @Override
  public Grid build() {
    return build(new Grid());
  }

  @Override
  public Grid build(Grid grid) {
    getGridAttributes().asGrid(grid);
    grid.setSavedSearches(isSavedSearches() ? Boolean.TRUE : null);
    return super.build(grid);
  }

  /**
   * Set check multiselect
   *
   * @param checkMultiselect
   * @return
   */
  public GridBuilder setCheckMultiselect(boolean checkMultiselect) {
    getGridAttributes().setCheckMultiselect(checkMultiselect);
    return this;
  }

  /**
   * Set editable
   *
   * @param editable
   * @return
   */
  public GridBuilder setEditable(boolean editable) {
    getGridAttributes().setEditable(editable);
    return this;
  }

  /**
   * Set multiselect
   *
   * @param multiselect
   * @return
   */
  public GridBuilder setMultiselect(boolean multiselect) {
    getGridAttributes().setMultiselect(multiselect);
    return this;
  }

  /**
   * Set move rows (drag and drop of rows)
   *
   * @param moveRows
   * @return
   */
  public GridBuilder setMoveRows(boolean moveRows) {
    getGridAttributes().setMoveRows(moveRows);
    return this;
  }

  /**
   * Set pagination disabled
   *
   * @param paginationDisabled
   * @return
   */
  public GridBuilder setPaginationDisabled(boolean paginationDisabled) {
    getGridAttributes().setPaginationDisabled(paginationDisabled);
    return this;
  }

  /**
   * Set row numbers
   *
   * @param rowNumbers
   * @return
   */
  public GridBuilder setRowNumbers(boolean rowNumbers) {
    getGridAttributes().setRowNumbers(rowNumbers);
    return this;
  }

  /**
   * Set send all
   *
   * @param sendAll
   * @return
   */
  public GridBuilder setSendAll(boolean sendAll) {
    getGridAttributes().setSendAll(sendAll);
    return this;
  }

  /**
   * Set send operations
   *
   * @param sendOperations
   * @return
   */
  public GridBuilder setSendOperations(boolean sendOperations) {
    getGridAttributes().setSendOperations(sendOperations);
    return this;
  }

  /**
   * Set show totals
   *
   * @param showTotals
   * @return
   */
  public GridBuilder setShowTotals(boolean showTotals) {
    getGridAttributes().setShowTotals(showTotals);
    return this;
  }

  /**
   * Group the rows by some columns (comma separated, one grouping level each). The grid loads every row
   *
   * @param groupBy Column names
   * @return Grid builder
   */
  public GridBuilder setGroupBy(String groupBy) {
    getGridAttributes().setGroupBy(groupBy);
    return this;
  }

  /**
   * Show the "Group by" control in the toolbar to change the grouping at runtime. The grid loads every row
   *
   * @param groupSelector Show the group selector
   * @return Grid builder
   */
  public GridBuilder setGroupSelector(boolean groupSelector) {
    getGridAttributes().setGroupSelector(groupSelector);
    return this;
  }

  /**
   * Close each group with a subtotal row. It is shown by default
   *
   * @param groupSubtotals Show the subtotal rows
   * @return Grid builder
   */
  public GridBuilder setGroupSubtotals(boolean groupSubtotals) {
    getGridAttributes().setGroupSubtotals(groupSubtotals);
    return this;
  }

  /**
   * Set tree grid
   *
   * @param treeGrid
   * @return
   */
  public GridBuilder setTreeGrid(boolean treeGrid) {
    getGridAttributes().setTreeGrid(treeGrid);
    return this;
  }

  /**
   * Show the grid toolbar (columns dialog and maximize). It is shown by default
   *
   * @param showToolbar Show the toolbar
   * @return Grid builder
   */
  public GridBuilder setShowToolbar(boolean showToolbar) {
    getGridAttributes().setShowToolbar(showToolbar);
    return this;
  }

  /**
   * Show the Columns button of the grid toolbar. It is shown by default. When hidden, the stored column layout of the
   * user is ignored
   *
   * @param showColumnsButton Show the Columns button
   * @return Grid builder
   */
  public GridBuilder setShowColumnsButton(boolean showColumnsButton) {
    getGridAttributes().setShowColumnsButton(showColumnsButton);
    return this;
  }

  /**
   * Show the Maximize button of the grid toolbar. It is shown by default
   *
   * @param showMaximizeButton Show the Maximize button
   * @return Grid builder
   */
  public GridBuilder setShowMaximizeButton(boolean showMaximizeButton) {
    getGridAttributes().setShowMaximizeButton(showMaximizeButton);
    return this;
  }

  /**
   * Show the label of the end buttons of the grid toolbar (true) or only their icon, with the label as tooltip (false).
   * Null follows the application default (awe.application.component.grid-toolbar-labels)
   *
   * @param toolbarLabels Show the labels
   * @return Grid builder
   */
  public GridBuilder setToolbarLabels(Boolean toolbarLabels) {
    return setToolbarLabels(toolbarLabels == null ? null : toolbarLabels.toString());
  }

  /**
   * Choose which end buttons of the grid toolbar show their label, as the XML attribute toolbar-labels: true (all),
   * false (none, only the icon with the label as tooltip) or a comma-separated list of group-by, saved-searches,
   * columns and maximize. Null follows the application default (awe.application.component.grid-toolbar-labels)
   *
   * @param toolbarLabels Buttons that show their label
   * @return Grid builder
   */
  public GridBuilder setToolbarLabels(String toolbarLabels) {
    getGridAttributes().setToolbarLabels(toolbarLabels);
    return this;
  }

  /**
   * Offer the saved searches of the grid in its toolbar (#912). Not offered by default
   *
   * @param savedSearches Offer the saved searches
   * @return Grid builder
   */
  public GridBuilder setSavedSearches(boolean savedSearches) {
    this.savedSearches = savedSearches;
    return this;
  }

  /**
   * Choose how the toolbar shows the Columns: a modal dialog (default) or a side panel
   *
   * @param columnsPanel Columns panel
   * @return Grid builder
   */
  public GridBuilder setColumnsPanel(ColumnsPanel columnsPanel) {
    getGridAttributes().setColumnsPanel(columnsPanel);
    return this;
  }

  /**
   * Set validate on save
   *
   * @param validateOnSave
   * @return
   */
  public GridBuilder setValidateOnSave(boolean validateOnSave) {
    getGridAttributes().setValidateOnSave(validateOnSave);
    return this;
  }

  /**
   * Set initial level
   *
   * @param level
   * @return
   */
  public GridBuilder setInitialLevel(Integer level) {
    getGridAttributes().setInitialLevel(level);
    return this;
  }

  /**
   * Set expand column
   *
   * @param column
   * @return
   */
  public GridBuilder setExpandColumn(String column) {
    getGridAttributes().setExpandColumn(column);
    return this;
  }

  /**
   * Set collapse icon
   *
   * @param icon
   * @return
   */
  public GridBuilder setIconCollapse(String icon) {
    getGridAttributes().setIconCollapse(icon);
    return this;
  }

  /**
   * Set leaf icon
   *
   * @param icon
   * @return
   */
  public GridBuilder setIconLeaf(String icon) {
    getGridAttributes().setIconLeaf(icon);
    return this;
  }

  /**
   * Set pager values
   *
   * @param pagerValues
   * @return
   */
  public GridBuilder setPagerValues(String pagerValues) {
    getGridAttributes().setPagerValues(pagerValues);
    return this;
  }

  /**
   * Set tree id
   *
   * @param id
   * @return
   */
  public GridBuilder setTreeId(String id) {
    getGridAttributes().setTreeId(id);
    return this;
  }

  /**
   * Set tree leaf
   *
   * @param treeLeaf
   * @return
   */
  public GridBuilder setTreeLeaf(String treeLeaf) {
    getGridAttributes().setTreeLeaf(treeLeaf);
    return this;
  }

  /**
   * Set tree parent
   *
   * @param parent
   * @return
   */
  public GridBuilder setTreeParent(String parent) {
    getGridAttributes().setTreeParent(parent);
    return this;
  }

  /**
   * Add column
   *
   * @param columnBuilders
   * @return
   */
  public GridBuilder addColumn(AbstractColumnBuilder... columnBuilders) {
    addAllElements(columnBuilders);
    return this;
  }

  /**
   * Add button
   *
   * @param button
   * @return
   */
  public GridBuilder addButton(ButtonBuilder... button) {
    addAllElements(button);
    return this;
  }

  /**
   * Add group header
   *
   * @param groupHeader
   * @return
   */
  public GridBuilder addGroupHeader(GroupHeaderBuilder... groupHeader) {
    addAllElements(groupHeader);
    return this;
  }
}
