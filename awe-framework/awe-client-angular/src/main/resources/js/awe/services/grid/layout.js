import {aweApplication} from "../../awe";
import {
  applyColumnLayout,
  getColumnGroups,
  mergeColumnLayout,
  moveColumn,
  moveColumnTo,
  setColumnVisible
} from "./layoutRules";

// Grid layout service: the order and visibility of the columns chosen by the user (grid toolbar)
aweApplication.factory('GridLayout',
  ['ActionController', 'AweSettings', 'AweUtilities',
    /**
     * Grid layout methods
     *
     * @param {service} $actionController Action controller service
     * @param {service} $settings AWE settings
     * @param {service} Utilities AWE utilities
     */
    function ($actionController, $settings, Utilities) {
      /**
       * Call one of the grid layout services. The user is never sent: the server takes it from the session.
       * The request parameter names are specific on purpose: the values of the screen criteria are added to the call.
       *
       * @param {string} target Maintain target
       * @param {object} values Service parameters
       */
      function launchLayoutService(target, values) {
        let action = {
          type: 'server',
          async: false,
          silent: false,
          parameters: {...values}
        };
        action.parameters[$settings.get("serverActionKey")] = "maintain";
        action.parameters[$settings.get("targetActionKey")] = target;
        $actionController.addActionList([action], true, {address: {}, context: ""});
      }

      /**
       * Make ui-grid follow the order of the column definitions. When the columns are rebuilt, ui-grid keeps the order it
       * has (its move columns feature restores it from a cache), so the columns and that cache are reordered first.
       *
       * @param {object} api ui-grid api
       * @param {Array} defs Column definitions in the order to show
       */
      function redrawColumns(api, defs) {
        const uiGrid = api.grid;
        const byName = {};
        uiGrid.columns.forEach(column => {
          byName[column.name] = column;
        });
        const headers = uiGrid.columns.filter(column => column.isRowHeader);
        const ordered = defs.map(def => byName[def.name]).filter(column => column && !column.isRowHeader);
        // Columns the grid has that the definitions do not know stay at the end
        const rest = uiGrid.columns.filter(column => !column.isRowHeader && ordered.indexOf(column) < 0);
        uiGrid.columns.splice(0, uiGrid.columns.length, ...headers, ...ordered, ...rest);
        if (uiGrid.moveColumns) {
          uiGrid.moveColumns.orderCache = ordered.concat(rest);
        }
        if (uiGrid.queueGridRefresh) {
          uiGrid.queueGridRefresh();
        }
        api.core.notifyDataChange("column");
      }

      return {
        /**
         * Add the column layout methods to a grid component
         *
         * @param {object} component Grid component
         */
        attach: function (component) {
          /**
           * Retrieve the columns the user can manage: the ones of the screen (not the ones the grid adds)
           * @return {Array} Columns
           */
          const getBaseColumns = () => component.layoutBase || [];

          /**
           * Retrieve the group headers
           * @return {Array} Headers
           */
          const getHeaders = () => component.controller.headerModel || [];

          /**
           * Check if the grid has the toolbar
           * @return {boolean} Toolbar enabled
           */
          component.hasColumnLayout = () => component.controller.showToolbar !== false && getBaseColumns().length > 0;

          /**
           * Store the columns of the screen, before the grid adds its own ones
           */
          component.storeLayoutBase = function () {
            component.layoutBase = component.controller.columnModel.slice();
          };

          /**
           * Reorder the column definitions and hide the columns hidden by the layout
           *
           * @param {object} state Layout
           * @param {Array} defs Column definitions of the grid (reordered in place)
           */
          function applyState(state, defs) {
            const hidden = state.hidden;
            getBaseColumns().forEach(column => {
              const hide = hidden.indexOf(column.name) >= 0;
              if (hide && !column.layoutHidden) {
                column.hidden = true;
                column.layoutHidden = true;
                column.width = 0;
                column.enableColumnResizing = false;
                column.enableColumnMoving = false;
              } else if (!hide && column.layoutHidden) {
                column.hidden = false;
                column.layoutHidden = false;
                column.width = column.initialWidth;
                column.enableColumnResizing = undefined;
                column.enableColumnMoving = undefined;
              }
            });

            // The columns of the screen take the positions they have in the definitions, in the layout order
            const byName = {};
            getBaseColumns().filter(column => !column.hidden || column.layoutHidden).forEach(column => {
              byName[column.name] = column;
            });
            const positions = [];
            defs.forEach((def, index) => {
              if (byName[def.name] === def) {
                positions.push(index);
              }
            });
            positions.forEach((position, index) => {
              defs[position] = byName[state.order[index]];
            });
            component.columnLayoutState = state;
          }

          /**
           * Build the column definitions of the grid, with the layout of the user applied
           *
           * @return {Array} Column definitions
           */
          component.initColumnLayout = function () {
            let defs = component.controller.columnModel;
            if (component.hasColumnLayout()) {
              defs = defs.slice();
              applyState(mergeColumnLayout(getBaseColumns(), getHeaders(), component.controller.columnLayout), defs);
            }
            component.layoutDefs = defs;
            return defs;
          };

          /**
           * Refresh the nodes the columns dialog shows: columns without group and groups with their columns
           */
          component.refreshLayoutNodes = function () {
            const base = getBaseColumns();
            const headers = getHeaders();
            const state = mergeColumnLayout(base, headers, component.columnLayoutState);
            const groups = getColumnGroups(headers, base);
            const byName = {};
            base.forEach(column => {
              byName[column.name] = column;
            });
            const nodes = [];
            state.order.forEach(name => {
              const group = groups[name];
              const siblings = state.order.filter(other => groups[other] === group);
              const column = {
                name: name,
                label: byName[name].label,
                visible: state.hidden.indexOf(name) < 0,
                first: siblings[0] === name,
                last: siblings[siblings.length - 1] === name
              };
              const last = nodes[nodes.length - 1];
              if (group === undefined) {
                nodes.push({column: column});
              } else if (last && last.group === group) {
                last.columns.push(column);
              } else {
                nodes.push({group: group, label: headers[group].label || headers[group].titleText, columns: [column]});
              }
            });
            component.layoutNodes = nodes;
            component.layoutVisible = state.order.length - state.hidden.length;
            component.layoutTotal = state.order.length;
          };

          /**
           * Apply a new layout to the grid
           *
           * @param {object} state Layout
           */
          component.changeColumnLayout = function (state) {
            const defs = component.scope.gridOptions.columnDefs;
            applyState(mergeColumnLayout(getBaseColumns(), getHeaders(), state), defs);
            component.refreshLayoutNodes();
            if (component.grid.api) {
              redrawColumns(component.grid.api, defs);
              Utilities.timeout(() => component.reportChangedColumns());
            }
          };

          const currentState = () => mergeColumnLayout(getBaseColumns(), getHeaders(), component.columnLayoutState);

          /**
           * Show or hide a column
           * @param {string} name Column
           * @param {boolean} visible Show or hide
           */
          component.setColumnVisible = function (name, visible) {
            component.changeColumnLayout(setColumnVisible(getBaseColumns(), getHeaders(), currentState(), name, visible));
          };

          /**
           * Move a column among its siblings
           * @param {string} name Column
           * @param {number} delta Positions to move
           */
          component.moveColumn = function (name, delta) {
            component.changeColumnLayout(moveColumn(getBaseColumns(), getHeaders(), currentState(), name, delta));
          };

          /**
           * Put a column where one of its siblings is
           * @param {string} name Column
           * @param {string} target Sibling
           */
          component.moveColumnTo = function (name, target) {
            component.changeColumnLayout(moveColumnTo(getBaseColumns(), getHeaders(), currentState(), name, target));
          };

          /**
           * Show all the columns
           */
          component.showAllColumns = function () {
            component.changeColumnLayout({order: currentState().order, hidden: []});
          };

          /**
           * Go back to the definition of the screen and remove the stored layout
           */
          component.resetColumnLayout = function () {
            component.changeColumnLayout(null);
            component.controller.columnLayout = null;
            launchLayoutService("resetGridLayout", {
              gridLayoutScreen: component.controller.screenId,
              gridLayoutGrid: component.address.component
            });
          };

          /**
           * Store the current layout as the default of the user
           */
          component.saveColumnLayout = function () {
            const state = currentState();
            component.controller.columnLayout = state;
            launchLayoutService("saveGridLayout", {
              gridLayoutScreen: component.controller.screenId,
              gridLayoutGrid: component.address.component,
              gridLayoutJson: JSON.stringify(state)
            });
          };

          // Visible columns of a layout (used by tests and printing helpers)
          component.getLayoutColumns = () => applyColumnLayout(getBaseColumns(), getHeaders(), component.columnLayoutState);
        }
      };
    }
  ]);
