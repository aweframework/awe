import {aweApplication} from "../../awe";
import {TestIds} from "../../data/testIds";
import {iconMarkup} from "../icon";
import {buildGroupedRows, formatGroupNumber, getCellLabel, getCellValue, getGroupLevels, isDisplayRow} from "./grouping";

// Columns the grid adds before its own ones (selection checkbox and row number)
const HELPER_COLUMNS = ["chkBox", "rowNum"];

// Components whose cells show the label of an option of the column (its model values)
const OPTION_COMPONENTS = ["select", "suggest", "select-multiple", "suggest-multiple", "radio", "button-radio"];

// Row template of the grids with grouping, cached as "grid/group-row": the data rows render their cells as the
// "grid/row" template; the group and subtotal rows render the toggle, label and count of the group in the first visible
// column and the aggregates in the columns with summary type (no checkbox, row number nor component of the columns).
// A click on a group row toggles it
export const GROUP_ROW_TEMPLATE = `<div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.uid" ng-if="!row.entity.$group && !row.entity.$subtotal"
     ui-grid-one-bind-id-grid="rowRenderIndex + '-' + col.uid + '-cell'"
     class="ui-grid-cell no-animate" ng-class="{'ui-grid-row-header-cell': col.isRowHeader, 'hidden': col.colDef.hidden}"
     role="{{::col.isRowHeader ? 'rowheader' : 'gridcell'}}" ui-grid-cell></div><div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.uid" ng-if="row.entity.$group || row.entity.$subtotal"
     class="ui-grid-cell no-animate grid-group-cell {{col.getColClass(false)}}" ng-class="{'hidden': col.colDef.hidden, 'grid-group-label-cell': col.name === grid.appScope.component.groupLabelColumn}"
     role="gridcell" ng-click="grid.appScope.component.onGroupRowClick(row, $event)">
  <div class="no-animate ui-grid-cell-contents" ng-class="col.colDef.align ? 'text-' + col.colDef.align : ''" column-id="{{col.name}}" data-testid="${TestIds.gridCell}" ng-switch="col.name === grid.appScope.component.groupLabelColumn">
    <span ng-switch-when="true" class="grid-group-title">
      <button ng-if="row.entity.$group" type="button" class="grid-group-toggle" data-testid="${TestIds.gridGroupToggle}" aria-expanded="{{row.entity.$group.expanded}}"
              aria-label="{{(row.entity.$group.expanded ? 'GRID_GROUP_COLLAPSE' : 'GRID_GROUP_EXPAND') | translateMultiple}}" title="{{(row.entity.$group.expanded ? 'GRID_GROUP_COLLAPSE' : 'GRID_GROUP_EXPAND') | translateMultiple}}"
              ng-click="grid.appScope.component.toggleGroup(row.entity.$group.path, $event)">${iconMarkup("chevron-right")}</button><span ng-if="row.entity.$group" class="grid-group-label" ng-bind="row.entity.$group.label"></span><span ng-if="row.entity.$group" class="grid-group-count" ng-bind="row.entity.$group.count"></span><span ng-if="row.entity.$subtotal" class="grid-subtotal-label" ng-bind="('GRID_GROUP_SUBTOTAL' | translateMultiple) + ' ' + row.entity.$subtotal.label"></span>
    </span>
    <span ng-switch-default class="grid-group-aggregate" ng-bind="(row.entity.$group || row.entity.$subtotal).aggregates[col.name].label"></span>
  </div>
</div>`;

// Grid options of the rows while they are grouped: ui-grid neither sorts (the grouping does), nor paginates (every group
// is shown, with scroll), nor filters the display rows
const GROUPED_OPTIONS = {useExternalSorting: true, enablePagination: false, enableFiltering: false};

/**
 * Check whether a grid can group its rows: plain grids only (not tree, editable, send-operations, move-rows or
 * row-detail grids)
 * @param {object} component Grid component
 * @returns {boolean} The grid can group its rows
 */
export function isGroupingAllowed(component) {
  const controller = component.controller || {};
  return !component.isTree && !controller.treegrid && !component.editable && !controller.editable &&
    !controller.multioperation && !controller.moveRows && !controller.rowDetail;
}

// Grouping of the grid rows (#833): group-by levels (of the screen or changed at runtime with the Group by selector),
// expanded state by group path and the display rows given to ui-grid. Levels and expanded state live on the client only
aweApplication.factory('GridGrouping',
  ['AweSettings', 'AweUtilities', '$filter', '$templateCache', 'uiGridConstants',
    /**
     * Grid grouping
     * @param {service} $settings AWE settings
     * @param {service} Utilities AWE utilities
     * @param {service} $filter Filter service
     * @param {service} $templateCache Template cache
     * @param {service} uiGridConstants ui-grid constants
     */
    function ($settings, Utilities, $filter, $templateCache, uiGridConstants) {
      $templateCache.put("grid/group-row", GROUP_ROW_TEMPLATE);
      const translate = text => $filter('translateMultiple')(text);

      return {
        /**
         * Attach the grouping to a grid component (once its common methods are initialized)
         * @param {object} component Grid component
         */
        attach: function (component) {
          const controller = component.controller;
          const allowed = isGroupingAllowed(component);
          const state = {
            levels: allowed ? String(controller.groupBy || "").split(",").map(name => name.trim()).filter(Boolean) : [],
            // Groups start expanded; expand all and collapse all change the default and forget the toggled groups
            expandedByDefault: true,
            expanded: {}
          };
          const rowNumbers = new Map();

          // The grid shows the grouping controls when it has group-by levels or the Group by selector
          component.groupingConfigured = allowed && (state.levels.length > 0 || controller.groupSelector === true);
          component.groupSelector = component.groupingConfigured && controller.groupSelector === true;

          const getDataColumns = () => (controller.columnModel || []).filter(column => !HELPER_COLUMNS.includes(column.name));
          const isExpanded = path => (path in state.expanded ? state.expanded[path] : state.expandedByDefault);

          /**
           * Group-by levels of the grid (the unknown columns are ignored)
           * @returns {string[]} Column names
           */
          component.getGroupLevels = () => (allowed ? getGroupLevels(state.levels, getDataColumns()) : []);
          component.grouped = component.getGroupLevels().length > 0;

          // Label of the group of a row: the label of the option of a select column (translated), the cell label otherwise
          const getGroupLabel = (row, column) => {
            const cell = row[column.name];
            const options = OPTION_COMPONENTS.includes(column.component) ? column.model?.values : null;
            const value = getCellValue(cell);
            const option = (options || []).find(item => String(getCellValue(item)) === String(value));
            return option ? translate(getCellLabel(option)) : getCellLabel(cell);
          };

          // Label of an aggregate, with the number format of its column over the numeric options of the application
          const formatAggregate = (value, column) => {
            const numberFormat = typeof column.numberFormat === "string" ? Utilities.evalJSON(column.numberFormat) : column.numberFormat;
            return formatGroupNumber(value, {...$settings.get("numericOptions"), ...(numberFormat || {})});
          };

          // First visible column of the grid (frozen ones first): it holds the toggle and label of the groups
          const updateLabelColumn = () => {
            const gridColumns = component.grid?.api?.grid.columns || [];
            const columns = gridColumns.length > 0 ? gridColumns : (controller.columnModel || []).map(colDef => ({name: colDef.name, colDef}));
            const visible = columns.filter(column => !HELPER_COLUMNS.includes(column.name) && !column.isRowHeader && !column.colDef?.hidden && column.visible !== false);
            const frozen = visible.filter(column => column.renderContainer === "left");
            component.groupLabelColumn = (frozen[0] || visible[0] || {}).name || null;
          };
          updateLabelColumn();
          component.scope.$on("columns-changed", updateLabelColumn);

          /**
           * Rows given to ui-grid: the display rows (groups, data rows and subtotals) while the rows are grouped, the data
           * rows of the model otherwise
           * @returns {object[]} Rows
           */
          component.getDisplayValues = function () {
            if (!component.grouped) {
              return component.model.values;
            }
            updateLabelColumn();
            const rows = buildGroupedRows(component.model.values, {
              groupBy: component.getGroupLevels(),
              columns: getDataColumns(),
              sort: component.grid.sorting || [],
              expanded: isExpanded,
              subtotals: controller.groupSubtotals !== false,
              translate,
              getLabel: getGroupLabel,
              format: formatAggregate
            });
            // The display rows get the classes of their row
            rowNumbers.clear();
            rows.forEach(row => {
              if (row.$group) {
                row[component.constants.ROW_CLASS_FIELD] = `grid-group-row grid-group-level-${row.$group.level}`;
              } else if (row.$subtotal) {
                row[component.constants.ROW_CLASS_FIELD] = `grid-subtotal-row grid-subtotal-level-${row.$subtotal.level}`;
              } else {
                rowNumbers.set(row, rowNumbers.size + 1);
              }
            });
            return rows;
          };

          /**
           * Number of a data row of a grouped grid (its position among the shown data rows)
           * @param {object} row ui-grid row
           * @returns {number|string} Row number
           */
          component.getGroupedRowNumber = row => rowNumbers.get(row.entity) || "";

          // Show the rows again (a group toggled, the levels changed) without touching the model
          const refreshRows = () => {
            component.scope.gridOptions.data = component.getDisplayValues();
            component.deferRowsRendered(deferred => {
              component.updatePaginationText();
              component.updateGridScrollBars();
              deferred.resolve();
            });
          };

          /**
           * Expand or collapse a group (no server call)
           * @param {string} path Group path
           * @param {Event} [event] Click on the toggle (it does not reach the row, which would toggle it again)
           */
          component.toggleGroup = function (path, event) {
            event?.stopPropagation();
            state.expanded[path] = !isExpanded(path);
            refreshRows();
          };

          /**
           * Click on a group or subtotal row: a group toggles, neither is selected
           * @param {object} row ui-grid row
           */
          component.onGroupRowClick = function (row) {
            if (row?.entity?.$group) {
              component.toggleGroup(row.entity.$group.path);
            }
          };

          /**
           * Expand or collapse every group of every level (no server call)
           * @param {boolean} expanded Expand (true) or collapse (false)
           */
          component.setAllGroupsExpanded = function (expanded) {
            state.expandedByDefault = expanded;
            state.expanded = {};
            refreshRows();
          };

          /**
           * Grid options of the rows: grouped or not
           */
          const applyGroupedOptions = () => {
            const options = component.scope.gridOptions;
            Object.assign(options, component.grouped ? GROUPED_OPTIONS : {
              useExternalSorting: !controller.loadAll,
              enablePagination: true,
              enableFiltering: !!controller.enableFilters
            });
            component.grid.api?.core.notifyDataChange(uiGridConstants.dataChange.OPTIONS);
          };

          /**
           * Change the group-by levels (Group by selector), without a server call
           * @param {string[]} levels Column names, in order (none: no grouping)
           */
          component.setGroupLevels = function (levels) {
            state.levels = [...levels];
            const grouped = component.getGroupLevels().length > 0;
            if (grouped !== component.grouped) {
              component.grouped = grouped;
              // Back to the first page of the rows
              component.currentPage = 1;
              component.model.page = 1;
              component.scope.gridOptions.paginationCurrentPage = 1;
              applyGroupedOptions();
            }
            refreshRows();
          };

          /**
           * Add a column as the last group-by level, or remove it from the levels
           * @param {string} name Column name
           */
          component.toggleGroupLevel = function (name) {
            const levels = component.getGroupLevels();
            component.setGroupLevels(levels.includes(name) ? levels.filter(level => level !== name) : [...levels, name]);
          };

          /**
           * Columns of the Group by selector: the visible columns with a label and without summary type, and the ones
           * that are group-by levels, with their level
           * @returns {{name: string, label: string, level: number}[]} Options
           */
          component.getGroupByOptions = function () {
            const levels = component.getGroupLevels();
            return getDataColumns()
              .filter(column => levels.includes(column.name) || (!column.hidden && !!column.label && !column.summaryType))
              .map(column => ({name: column.name, label: column.label || column.name, level: levels.indexOf(column.name) + 1}));
          };

          /**
           * ui-grid options of a grid with grouping: the row template with the group rows, the display rows are never
           * selected, and the options of the grouped rows
           * @returns {object} Grid options (none for a grid without grouping)
           */
          component.getGroupingOptions = function () {
            if (!component.groupingConfigured) {
              return {};
            }
            return {
              rowTemplate: "grid/group-row",
              isRowSelectable: row => !isDisplayRow(row.entity),
              ...(component.grouped ? GROUPED_OPTIONS : {})
            };
          };

          /**
           * Initialize the grouping of the ui-grid: the selected rows of a collapsed group are selected again when it is
           * expanded (ui-grid builds their rows again)
           */
          component.initGroupRows = function () {
            if (!component.groupingConfigured) {
              return;
            }
            component.grid.api.grid.registerRowsProcessor(rows => {
              const selected = Utilities.asArray(component.model.selected);
              if (component.grouped && selected.length > 0) {
                rows.filter(row => !isDisplayRow(row.entity) && !row.isSelected && selected.includes(row.entity[component.constants.ROW_IDENTIFIER]))
                  .forEach(row => row.setSelected(true));
              }
              return rows;
            }, 60);
          };

          // A right click on a group or subtotal row has no row actions: it neither selects the row nor opens the menu
          const rowContextMenu = component.rowContextMenu;
          component.rowContextMenu = function (event, row) {
            if (isDisplayRow(row?.entity)) {
              component.hideContextMenu();
              return;
            }
            rowContextMenu(event, row);
          };
        }
      };
    }
  ]);
