import {aweApplication} from "../awe";

// Maximum number of placeholder rows
export const GRID_SKELETON_MAX_ROWS = 10;

const GRID_SKELETON_TEMPLATE = `<div class="grid-skeleton-body" ng-style="bodyStyle">
  <div class="grid-skeleton-row" ng-repeat="row in rows track by $index" ng-style="::rowStyle">
    <div class="grid-skeleton-cell" ng-repeat="column in columns track by $index" ng-style="::column.style"><span class="skeleton-bar"></span></div>
  </div>
</div>`;

/**
 * Width style of a placeholder cell: the drawn width of the column when the grid is rendered, the column model
 * width otherwise. Relative widths ("*", "30%") share the remaining space.
 * @param {object} column Column model
 * @param {object} drawnWidths Drawn widths by column name
 * @returns {object} Cell style
 */
function getCellStyle(column, drawnWidths) {
  const width = drawnWidths[column.name] || column.width;
  return typeof width === "number" || /^\d+$/.test(width) ? {width: `${width}px`, flex: "none"} : {flex: "1 1 0"};
}

/**
 * Grid loading skeleton: placeholder rows below the grid header, one bar per visible column
 * @ngdoc directive
 * @name aweGridSkeleton
 */
aweApplication.directive('aweGridSkeleton',
  ['AweSettings',
    /**
     * @param {object} $settings Awe settings
     */
    function ($settings) {
      return {
        restrict: 'E',
        template: GRID_SKELETON_TEMPLATE,
        link: function (scope, elem) {
          const component = scope.component || {};
          const controller = scope.controller || {};
          const api = component.grid && component.grid.api;

          // Rows: the page size, capped
          const max = component.getMax ? component.getMax() : parseInt($settings.get("recordsPerPage"), 10);
          scope.rows = Array.from({length: max > 0 ? Math.min(max, GRID_SKELETON_MAX_ROWS) : GRID_SKELETON_MAX_ROWS});
          scope.rowStyle = scope.gridOptions && scope.gridOptions.rowHeight ? {height: `${scope.gridOptions.rowHeight}px`} : {};

          // Columns: the visible columns of the column model with their widths
          const drawnWidths = {};
          ((api && api.grid && api.grid.columns) || []).forEach(column => {
            if (column.drawnWidth > 0) {
              drawnWidths[column.name] = column.drawnWidth;
            }
          });
          scope.columns = (controller.columnModel || [])
            .filter(column => !column.hidden && column.visible !== false)
            .map(column => ({style: getCellStyle(column, drawnWidths)}));

          // Place the rows below the rendered header
          const root = elem.parent()[0];
          const headers = root ? Array.from(root.querySelectorAll(".ui-grid-header, [awe-grid-header]")) : [];
          const top = headers.reduce((bottom, header) => {
            const rect = header.getBoundingClientRect();
            return rect.height > 0 ? Math.max(bottom, rect.bottom - root.getBoundingClientRect().top) : bottom;
          }, 0);
          scope.bodyStyle = top > 0 ? {top: `${top}px`} : {};
        }
      };
    }
  ]);
