import {aweApplication} from "../awe";
import {DefaultGridOptions, DefaultSpin} from "../data/options";
import {iconTemplate} from "../services/icon";
import "../services/grid/tree";
import {getGridRowHeight, getTreeIndent} from "../services/grid/density";
import "./gridSkeleton";
import "./gridToolbar";
import {getSelectionButtons} from "./gridSelectionBar";

// Treegrid directive
aweApplication.directive('aweTreeGrid',
  ['ServerData', 'GridTree', 'AweSettings',
    function (serverData, GridTree, $settings) {
      // Retrieve default $settings

      // Set default options
      let  options = {
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
        priority: 1001,
        replace: true,
        transclude: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('treegrid');
        },
        scope: {
          'gridId': '@treeGridId'
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
              let  component = new GridTree(scope, scope.gridId, elem);
              if (component.asTree()) {
                // Set bigGrid value
                component.bigGrid = true;

                // Set spin options
                scope.spinOptions = DefaultSpin.big;

                // Define default values
                component.controller.treeId = component.controller.treeId || "id";
                component.controller.treeParent = component.controller.treeParent || "parent";
                component.controller.treeLeaf = component.controller.treeLeaf || "isLeaf";

                // Update grid styles
                component.gridStyle = "grid-" + scope.size + " " + (component.controller.style || "");
                component.gridButtonClass = "btn btn-" + scope.size;
                // Buttons of the selection bar (selection="true"): out of the footer
                component.selectionButtons = getSelectionButtons(component.controller);
                component.enableSorting = false;

                // Fix column model
                component.fixColumnModel(true);
                scope.gridOptions = _.assign({}, options, {
                  columnDefs: component.initColumnLayout(),
                  data: [],
                  enableRowSelection: true,
                  enableFullRowSelection: true,
                  enableRowHeaderSelection: false,
                  multiSelect: component.controller.multiselect,
                  noUnselect: component.controller.editable && !component.controller.multiselect,
                  enableColumnResizing: true,
                  enableFiltering: component.controller.enableFilters,
                  enableSorting: component.enableSorting,
                  useExternalSorting: !component.controller.loadAll,
                  useExternalPagination: !component.controller.loadAll,
                  fastWatch: true,
                  flatEntityAccess: true,
                  rowTemplate: "grid/row",
                  paginationTemplate: "grid/pagination",
                  // The row height of the screen, or the one of the density (followed by the grid commons)
                  rowHeight: getGridRowHeight(component.controller),
                  // Indent of each level
                  treeIndent: getTreeIndent(),
                  enableMinHeightCheck: false,
                  //headerHeight: 28,
                  node: elem,
                  virtualizationThreshold: 50,
                  showColumnFooter: component.controller.showTotals,
                  icons: {
                    menu: iconTemplate("bars"),
                    filter: iconTemplate("filter"),
                    sortAscending: iconTemplate("sort-alpha-asc"),
                    sortDescending: iconTemplate("sort-alpha-desc"),
                    expand: "fa-" + (component.controller.iconExpand || 'plus-square'),
                    collapse: "fa-" + (component.controller.iconCollapse || 'minus-square'),
                    leaf: "fa-" + (component.controller.iconLeaf || 'dot-circle-o'),
                    loading: "fa-refresh fa-spin"
                  },
                  onRegisterApi: function (gridApi) {
                    component.grid.api = gridApi;
                    // Init grid
                    component.initGrid();
                  }
                });
              } else {
                scope.gridOptions = {data: []};
              }
            }
          };
        }
      };
    }
  ]);
