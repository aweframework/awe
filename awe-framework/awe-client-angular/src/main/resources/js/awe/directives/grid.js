import {aweApplication} from "../awe";
import {DefaultGridOptions, DefaultSpin} from "../data/options";
import "./../services/grid/base";
import "./gridHeader";

const GRID_TEMPLATE = `<div ng-attr-id="{{::gridId}}" ng-show="controller.visible" class="grid expandible-vertical table-light {{::component.gridStyle}}" ui-dependency="dependencies" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-if="component.fixedHeaders" ng-attr-id="grid-header-{{::gridId}}" awe-grid-header="gridOptions" ng-cloak></div>
  <div ng-attr-id="scope-{{::gridId}}" class="expand expandible-vertical grid-container" ng-cloak>
    <div ng-if="::component.editable" class="save-button">
      <button id="{{::gridId}}-grid-row-cancel" type="button" ng-class="::component.gridButtonClass" class="btn-awe btn-danger grid-row-cancel" title="{{'BUTTON_CANCEL'| translateMultiple}}" ng-click="onCancelRow()" ng-attr-tabindex="{{component.isEditing ? 0 : -1}}" ng-disabled="component.savingRow">
        <i class="fa" ng-class="{'fa-refresh fa-spin': component.rowAction === 'cancel' && component.savingRow, 'fa-close': component.rowAction !== 'cancel'}"></i>
        <span class="button-text" translate-multiple="BUTTON_CANCEL"></span>
      </button>
      <button id="{{::gridId}}-grid-row-save" type="button" ng-class="::component.gridButtonClass" class="btn-awe btn-primary grid-row-save" title="{{'BUTTON_SAVE_ROW'| translateMultiple}}" ng-click="onSaveRow()" ng-attr-tabindex="{{component.isEditing ? 0 : -1}}" ng-disabled="component.savingRow">
        <i class="fa" ng-class="{'fa-refresh fa-spin': component.rowAction === 'save' && component.savingRow, 'fa-save': component.rowAction !== 'save'}"></i>
        <span class="button-text" translate-multiple="BUTTON_SAVE_ROW"></span>
      </button>
    </div>
    <div ng-attr-id="grid-{{::gridId}}" ui-grid="gridOptions" ui-grid-selection ui-grid-pagination ui-grid-resize-columns ui-grid-move-columns ui-grid-pinning class="ag-awe grid-node no-border-hr"></div>
  </div>
  <div class="table-footer" ng-cloak>
    <div class="pagination-content grid-buttons fixed-height {{component.footerButtonStyle}}">
      <div ng-transclude></div>
    </div>
    <div ng-if="::!controller.disablePagination" class="pagination-content {{component.footerPaginationStyle}}">
      <div ng-if="model.records > 0 && component.bigGrid">
        <div class="hidden-xs col-sm-8 text-center pagination-content">
          <ul uib-pagination ng-change="component.changePage()" total-items="model.records" items-per-page="component.getMax()" ng-model="component.currentPage" num-pages="model.total" max-size="5" class="pagination pagination-{{::size}}"
                      previous-text="{{'SCREEN_TEXT_GRID_PREVIOUS'| translateMultiple}}" next-text="{{'SCREEN_TEXT_GRID_NEXT'| translateMultiple}}" force-ellipses="true" boundary-link-numbers="true"
                      first-text="{{'SCREEN_TEXT_GRID_FIRST'| translateMultiple}}" last-text="{{'SCREEN_TEXT_GRID_LAST'| translateMultiple}}"></ul>
          <input ng-if="model.total > 1" class="pagination-goto" placeholder="..." type="number" ng-blur="onGoToPageChanged($event)" ng-keypress="onGoToPageChanged($event)" />
        </div>
        <div class="hidden-xs col-sm-4 text-right pagination-content">
          {{component.paginationText}}
          <div ng-if="::controller.pagerValues.length" class="pagination-pager">
            <select class="grid-pager input input-group-{{::size}}" ng-options="value as value for value in controller.pagerValues" ng-model="controller.max" ng-change="component.updateRowsByPage()"></select>
          </div>
        </div>
      </div>
      <div class="col-xs-12 pagination-content text-right" ng-if="model.records > 0 && !component.bigGrid">
        <ul ng-class="::component.gridPaginationClass">
          <li ng-class="{disabled: model.page <= 1}">
            <a href="#" ng-click="component.setPage(model.page - 1)" title="{{'SCREEN_TEXT_GRID_PREVIOUS'| translateMultiple}}"><i class="fa fa-arrow-circle-left"></i></a>
          </li>
        </ul>
        <div class="pagination-content pagination-number">{{component.paginationTextSmall}}</div>
        <ul ng-class="::component.gridPaginationClass">
          <li ng-class="{disabled: model.page >= model.total}">
            <a href="#" ng-click="component.setPage(model.page + 1)" title="{{'SCREEN_TEXT_GRID_NEXT'| translateMultiple}}"><i class="fa fa-arrow-circle-right"></i></a>
          </li>
        </ul>
      </div>
    </div>
  </div>
  <awe-loader class="loader grid-loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

// Grid directive
aweApplication.directive('aweGrid',
  ['ServerData', 'AweSettings', 'GridBase', 'AweUtilities',
    function (serverData, $settings, GridBase, $utilities) {
      // Retrieve default $settings

      // Set default options
      let options = {
        ...DefaultGridOptions,
        // Elements per page
        rowNum: $settings.get("recordsPerPage"),
        // Total width
        totalWidth: true,
        /* **************************
         * Default formatting options
         * ***************************/
        formatOptions: {
          // Decimal separator
          decimalSeparator: $settings.get("numericOptions").aDec,
          // Thousands separator
          thousandsSeparator: $settings.get("numericOptions").aSep,
          // Decimal places
          decimalPlaces: $settings.get("numericOptions").mDec
        }
      };

      return {
        restrict: 'E',
        replace: true,
        transclude: true,
        template: GRID_TEMPLATE,
        scope: {
          'gridId': '@'
        },
        compile: function () {
          return {
            /**
             * Pregeneration function
             * @param {Object} scope
             * @param {Object} elem
             */
            pre: function (scope, elem) {
              // Init as component
              let component = new GridBase(scope, scope.gridId, elem);
              if (component.asGrid()) {
                // Set bigGrid value
                component.bigGrid = true;

                // Set spin options
                scope.spinOptions = DefaultSpin.big;

                // Update grid styles
                component.gridStyle = "grid-" + scope.size + " " + (component.controller.style || "");
                component.gridButtonClass = "btn btn-" + scope.size;
                component.gridPaginationClass = "pagination pagination-" + scope.size;
                component.enableSorting = !$utilities.isEmpty(component.controller.targetAction) || component.controller.loadAll;

                // Fix column model
                component.fixColumnModel(true);
                scope.gridOptions = _.assign({}, options, {
                  columnDefs: component.controller.columnModel,
                  data: [],
                  enableRowSelection: true,
                  enableFullRowSelection: true,
                  enableRowHeaderSelection: false,
                  enablePagination: true,
                  enablePaginationControls: false,
                  paginationPageSize: component.getMax(),
                  multiSelect: component.controller.multiselect,
                  noUnselect: component.controller.editable && !component.controller.multiselect,
                  enableColumnResizing: true,
                  enableFiltering: component.controller.enableFilters,
                  enableSorting: component.enableSorting,
                  enableColumnMoving: true,
                  useExternalSorting: !component.controller.loadAll,
                  useExternalPagination: !component.controller.loadAll,
                  paginationCurrentPage: component.model.page,
                  fastWatch: true,
                  flatEntityAccess: true,
                  rowTemplate: "grid/row",
                  paginationTemplate: "grid/pagination",
                  rowHeight: (parseInt(component.controller.rowHeight, 10) || 27),
                  enableMinHeightCheck: false,
                  excessRows: 50,
                  excessColumns: 10,
                  scrollThreshold: 25,
                  virtualizationThreshold: 50,
                  //headerHeight: 28,
                  node: elem,
                  //rowNum: scope.controller.max,
                  showColumnFooter: component.controller.showTotals,
                  icons: {
                    menu: '<i class="fa fa-bars"/>',
                    filter: '<i class="fa fa-filter"/>',
                    sortAscending: '<i class="fa fa-sort-alpha-asc"/>',
                    sortDescending: '<i class="fa fa-sort-alpha-desc"/>'
                  },
                  onRegisterApi: function (gridApi) {
                    component.grid.api = gridApi;
                    // Init grid
                    component.initGrid();
                  },
                  customScroller: (uiGridViewport, scrollHandler) => component.customScroller(uiGridViewport, scrollHandler)
                });
                scope.onGoToPageChanged = function (event) {
                  if (event.type === "keypress") {
                    // If key pressed is ENTER
                    if (event.keyCode === 13) {
                      component.setPage(parseInt(event.currentTarget.value, 10));
                      event.stopPropagation();
                      event.preventDefault();
                      event.currentTarget.value = "";
                    }
                  } else if (event.type === "blur") {
                    let page = parseInt(event.currentTarget.value, 10);
                    if (component.model.page !== page) {
                      component.setPage(page);
                      event.currentTarget.value = "";
                    }
                  }
                };
              } else {
                scope.gridOptions = {data: []};
              }
            }
          };
        }
      };
    }
  ]);
