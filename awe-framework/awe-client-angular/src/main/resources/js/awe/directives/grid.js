import {aweApplication} from "../awe";
import {DefaultGridOptions, DefaultSpin} from "../data/options";
import {TestIds} from "../data/testIds";
import {getIconTemplate} from "../services/component";
import {iconMarkup, iconTemplate} from "../services/icon";
import "./../services/grid/base";
import {getGridRowHeight} from "../services/grid/density";
import {initRowDrag} from "../services/grid/rowDrag";
import "./gridHeader";
import "./gridSkeleton";
import "./gridToolbar";

export const GRID_TEMPLATE = `<div ng-attr-id="{{::gridId}}" ng-show="controller.visible" data-testid="${TestIds.grid}" class="grid expandible-vertical table-light {{::component.gridStyle}}" ng-class="{'maximized': maximized, 'maximizing': maximizing, 'resizing resizeTarget': panelResizing}" ng-attr-data-maximized="{{iconMaximized === true}}" ng-attr-aria-busy="{{controller.loading || undefined}}" ui-dependency="dependencies" ng-keydown="component.onMoveRowKeyDown($event)" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <awe-grid-toolbar ng-if="controller.showToolbar !== false"></awe-grid-toolbar>
  <div ng-if="component.fixedHeaders" ng-attr-id="grid-header-{{::gridId}}" awe-grid-header="gridOptions" ng-cloak></div>
  <div ng-attr-id="scope-{{::gridId}}" class="expand expandible-vertical grid-container" ng-cloak>
    <div ng-if="::component.editable" class="save-button">
      <button id="{{::gridId}}-grid-row-cancel" type="button" ng-class="::component.gridButtonClass" class="btn-awe btn-danger grid-row-cancel" data-testid="${TestIds.gridRowCancel}" title="{{'BUTTON_CANCEL'| translateMultiple}}" ng-click="onCancelRow()" ng-attr-tabindex="{{component.isEditing ? 0 : -1}}" ng-disabled="component.savingRow">
        <i>${iconMarkup("refresh fa-spin", "ng-if=\"component.rowAction === 'cancel' && component.savingRow\"")}${iconMarkup("close", "ng-if=\"component.rowAction !== 'cancel'\"")}</i>
        <span class="button-text" translate-multiple="BUTTON_CANCEL"></span>
      </button>
      <button id="{{::gridId}}-grid-row-save" type="button" ng-class="::component.gridButtonClass" class="btn-awe btn-primary grid-row-save" data-testid="${TestIds.gridRowSave}" title="{{'BUTTON_SAVE_ROW'| translateMultiple}}" ng-click="onSaveRow()" ng-attr-tabindex="{{component.isEditing ? 0 : -1}}" ng-disabled="component.savingRow">
        <i>${iconMarkup("refresh fa-spin", "ng-if=\"component.rowAction === 'save' && component.savingRow\"")}${iconMarkup("save", "ng-if=\"component.rowAction !== 'save'\"")}</i>
        <span class="button-text" translate-multiple="BUTTON_SAVE_ROW"></span>
      </button>
    </div>
    <div ng-if="component.loaded && !controller.loading && !model.values.length" class="grid-empty-state" data-testid="${TestIds.gridEmpty}" ng-cloak>
      ${getIconTemplate("grid-empty-icon", "span", "emptyState.icon")}
      <div class="grid-empty-title" data-testid="${TestIds.gridEmptyTitle}" translate-multiple="{{emptyState.title}}"></div>
      <div class="grid-empty-text" data-testid="${TestIds.gridEmptyText}" translate-multiple="{{emptyState.text}}"></div>
      <button ng-if="emptyState.button" type="button" class="btn-awe btn-default grid-empty-action" data-testid="${TestIds.gridEmptyAction}" ng-click="onEmptyAction()">
        ${getIconTemplate("", "i", "emptyState.buttonIcon")}
        <span class="button-text" translate-multiple="{{emptyState.button.label}}"></span>
      </button>
    </div>
    <div ng-attr-id="grid-{{::gridId}}" ui-grid="gridOptions" ui-grid-selection ui-grid-pagination ui-grid-resize-columns ui-grid-move-columns ui-grid-pinning class="ag-awe grid-node no-border-hr"></div>
  </div>
  <div class="table-footer" ng-cloak>
    <div class="pagination-content grid-buttons fixed-height {{component.footerButtonStyle}}">
      <div ng-transclude></div>
    </div>
    <div ng-if="::!controller.disablePagination" class="pagination-content {{component.footerPaginationStyle}}" data-testid="${TestIds.gridPagination}">
      <div ng-if="model.records > 0 && component.bigGrid">
        <div class="hidden-xs col-sm-8 text-center pagination-content">
          <ul uib-pagination ng-change="component.changePage()" total-items="model.records" items-per-page="component.getMax()" ng-model="component.currentPage" num-pages="model.total" max-size="5" class="pagination pagination-{{::size}}"
                      previous-text="{{'SCREEN_TEXT_GRID_PREVIOUS'| translateMultiple}}" next-text="{{'SCREEN_TEXT_GRID_NEXT'| translateMultiple}}" force-ellipses="true" boundary-link-numbers="true"
                      first-text="{{'SCREEN_TEXT_GRID_FIRST'| translateMultiple}}" last-text="{{'SCREEN_TEXT_GRID_LAST'| translateMultiple}}"></ul>
          <input ng-if="model.total > 1" class="pagination-goto" data-testid="${TestIds.gridGotoPage}" placeholder="..." type="number" ng-blur="onGoToPageChanged($event)" ng-keypress="onGoToPageChanged($event)" />
        </div>
        <div class="hidden-xs col-sm-4 text-right pagination-content">
          <span data-testid="${TestIds.gridPaginationInfo}">{{component.paginationText}}</span>
          <div ng-if="::controller.pagerValues.length" class="pagination-pager">
            <select class="grid-pager input input-group-{{::size}}" data-testid="${TestIds.gridPageSize}" ng-options="value as value for value in controller.pagerValues" ng-model="controller.max" ng-change="component.updateRowsByPage()"></select>
          </div>
        </div>
      </div>
      <div class="col-xs-12 pagination-content text-right" ng-if="model.records > 0 && !component.bigGrid">
        <ul ng-class="::component.gridPaginationClass">
          <li ng-class="{disabled: model.page <= 1}">
            <a href="#" data-testid="${TestIds.gridPagePrevious}" ng-attr-data-disabled="{{model.page <= 1}}" ng-click="component.setPage(model.page - 1)" title="{{'SCREEN_TEXT_GRID_PREVIOUS'| translateMultiple}}">${iconTemplate("arrow-circle-left")}</a>
          </li>
        </ul>
        <div class="pagination-content pagination-number">{{component.paginationTextSmall}}</div>
        <ul ng-class="::component.gridPaginationClass">
          <li ng-class="{disabled: model.page >= model.total}">
            <a href="#" data-testid="${TestIds.gridPageNext}" ng-attr-data-disabled="{{model.page >= model.total}}" ng-click="component.setPage(model.page + 1)" title="{{'SCREEN_TEXT_GRID_NEXT'| translateMultiple}}">${iconTemplate("arrow-circle-right")}</a>
          </li>
        </ul>
      </div>
    </div>
  </div>
  <awe-loader class="loader grid-loader" data-testid="${TestIds.gridLoader}" ng-if="controller.loading && !skeletonLoader" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
  <awe-grid-skeleton class="grid-skeleton" data-testid="${TestIds.gridLoader}" ng-if="controller.loading && skeletonLoader && !(dimReload && model.values.length)" ng-cloak></awe-grid-skeleton>
  <awe-loader class="loader grid-loader" skeleton="reload" data-testid="${TestIds.gridLoader}" ng-if="controller.loading && dimReload && model.values.length" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

// Grid directive
aweApplication.directive('aweGrid',
  ['ServerData', 'AweSettings', 'GridBase', 'AweUtilities', 'Maximize',
    function (serverData, $settings, GridBase, $utilities, Maximize) {
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
                component.gridStyle = "grid-" + scope.size + " " + (component.controller.style || "") + (component.wrapsText ? " grid-wrap" : "") + (component.rowDetail ? " grid-with-row-detail" : "");
                component.gridButtonClass = "btn btn-" + scope.size;
                component.gridPaginationClass = "pagination pagination-" + scope.size;
                component.enableSorting = !$utilities.isEmpty(component.controller.targetAction) || component.controller.loadAll;

                // The grid has its own maximize state, also when the toolbar is hidden or shown later: it never reads the one of a window
                // (the animation clone does not copy the content of the grid)
                Maximize.initMaximize(scope, elem, {contentSelector: ".grid-container"});

                // Rows moved with drag and drop, from the grip of their leading column
                if (component.controller.moveRows) {
                  initRowDrag(component, elem);
                }

                // Fix column model
                component.fixColumnModel(true);
                scope.gridOptions = _.assign({}, options, {
                  columnDefs: component.initColumnLayout(),
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
                  // The row height of the screen, or the one of the density (followed by the grid commons)
                  rowHeight: getGridRowHeight(component.controller),
                  enableMinHeightCheck: false,
                  excessRows: 50,
                  excessColumns: 10,
                  scrollThreshold: 25,
                  // Rows of different heights cannot be virtualized (ui-grid places the rendered rows by a fixed height), so a grid
                  // with wrapping columns or row details renders every row of the page
                  virtualizationThreshold: component.wrapsText || component.rowDetail ? Number.MAX_SAFE_INTEGER : 50,
                  //headerHeight: 28,
                  node: elem,
                  //rowNum: scope.controller.max,
                  showColumnFooter: component.controller.showTotals,
                  icons: {
                    menu: iconTemplate("bars"),
                    filter: iconTemplate("filter"),
                    sortAscending: iconTemplate("sort-alpha-asc"),
                    sortDescending: iconTemplate("sort-alpha-desc")
                  },
                  onRegisterApi: function (gridApi) {
                    component.grid.api = gridApi;
                    // Init grid
                    component.initGrid();
                  },
                  customScroller: (uiGridViewport, scrollHandler) => component.customScroller(uiGridViewport, scrollHandler)
                });
                // Empty state (grid without rows)
                scope.emptyState = {
                  icon: $utilities.extractIcon(component.controller.emptyIcon || "inbox"),
                  title: component.controller.emptyTitle || "SCREEN_TEXT_GRID_EMPTY_TITLE",
                  text: component.controller.emptyText || "SCREEN_TEXT_GRID_EMPTY_TEXT",
                  button: (component.controller.buttonModel || []).find(button => button.id === component.controller.emptyAction)
                };
                scope.emptyState.buttonIcon = $utilities.extractIcon(scope.emptyState.button?.icon);
                scope.onEmptyAction = function () {
                  // Runs the actions of the grid button like any other click on it, once the digest of this click is over
                  // (the button starts a digest of its own)
                  $utilities.timeout(() => document.getElementById(component.controller.emptyAction)?.click());
                };
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

// Instance of the row detail component of an open row (components mode): the nested grid, with its own scope, compiled
// inside the detail of its row. It loads the first time it is shown; its state lives until the row closes or the parent
// grid gets new data or is destroyed (see the row detail of the grid service)
aweApplication.directive('aweGridRowDetailInstance',
  ['$compile',
    function ($compile) {
      return {
        restrict: 'A',
        link: function (scope, element) {
          const parent = scope.grid.appScope.component;
          const instance = parent.getRowDetailInstance(scope.row);

          // The nested grid is not inside the scopes of the screen: it gets the view and context of its parent
          const instanceScope = scope.$new();
          instanceScope.view = parent.address.view;
          instanceScope.context = parent.context;

          const node = angular.element("<awe-grid></awe-grid>").attr("grid-id", instance.id);
          instance.buttons.forEach(id => node.append(angular.element("<awe-button></awe-button>").attr("button-id", id)));
          element.append(node);
          $compile(node)(instanceScope);

          const nestedScope = element.children().first().isolateScope();
          const nested = nestedScope?.component;
          if (nested?.api?.getData && !instance.loaded) {
            instance.loaded = true;
            nested.reload();
          }

          // The row of the parent grows with the rows of the nested grid (the parent broadcasts its own ones too)
          nestedScope?.$on("rows-rendered", (event, parameters) => {
            if (parameters?.grid === instance.id) {
              parent.updateGridScrollBars();
            }
          });

          // The context menu of the nested grid is not the one of the parent row
          element.on("contextmenu", event => event.stopPropagation());

          scope.$on("$destroy", () => {
            element.off("contextmenu");
            instanceScope.$destroy();
          });
        }
      };
    }
  ]);
