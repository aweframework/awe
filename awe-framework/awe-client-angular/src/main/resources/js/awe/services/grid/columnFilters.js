import {aweApplication} from "../../awe";
import {
  BLANKS_KEY,
  RELATIVE_DATES,
  SUBMENU_OPERATORS,
  buildDateTree,
  getColumnFilter,
  getColumnFilterType,
  getFilterFields,
  getLeafKeys,
  getOperators,
  getValueKey,
  setColumnFilter,
  summarizeColumnFilter
} from "awe-client-react/utilities/columnFilters";

// Grid column autofilter (#912) in the AngularJS client: the column filters of a grid, the request of the values of a
// column and the helpers of its menu. The filter entries, their checks and the chip summaries are the ones of the React
// client (awe-client-react/utilities/columnFilters), so both clients send the same request.

/** Server action that lists the distinct values of a grid column (#912, S8) */
export const COLUMN_VALUES_ACTION = "column-values";

/** Pause after the last key typed in the search of the values before they are asked again */
export const SEARCH_DELAY = 300;

// Actions of the answer of the values that the menu reads itself or that have no component to work on
const OWN_ACTIONS = new Set([COLUMN_VALUES_ACTION, "end-load"]);

// Key of the "(Select all)" row of the values list
const SELECT_ALL = "\u0000all";

// A condition of the custom filter without operator (the second one is optional)
const NO_CONDITION = Object.freeze({operator: "", value: "", to: ""});

/**
 * Options of the type submenu of a column: its operators, the relative dates of a date column and the custom filter,
 * with separators between the groups
 * @param {string} type Filter type
 * @returns {object[]} Options ({operator}, {relative}, {custom} or {separator}), each with its key
 */
export function getSubmenuOptions(type) {
  const options = (SUBMENU_OPERATORS[type] || []).map(operator => ({key: operator, operator}));
  if (type === "date") {
    options.push({key: "separator-relative", separator: true}, ...RELATIVE_DATES.map(relative => ({key: relative, relative})));
  }
  return [...options, {key: "separator-custom", separator: true}, {key: "custom", custom: true}];
}

/**
 * Conditions the custom filter starts with: the operator chosen in the type submenu, or the conditions of the filter of
 * the column (the first operator of the type without filter), and an empty second one
 * @param {object} filter Filter of the column
 * @param {string} operator Operator chosen in the submenu (none for the custom filter)
 * @param {string} type Filter type
 * @returns {object[]} Two conditions
 */
export function getInitialConditions(filter, operator, type) {
  if (operator) {
    return [{...NO_CONDITION, operator}, {...NO_CONDITION}];
  }
  const current = ((filter && filter.conditions) || []).map(condition => ({...NO_CONDITION, ...condition}));
  return [current[0] || {...NO_CONDITION, operator: getOperators(type)[0]}, current[1] || {...NO_CONDITION}];
}

/**
 * Rows of the values list, in order: (Select all), the values (or the years, months and days of a date column, as far
 * as they are expanded) and (Blanks)
 * @param {object[]} values Values of the column (value, label and count)
 * @param {boolean} tree The values are dates shown as a tree
 * @param {object} expanded Expanded nodes by key
 * @param {string} language Language of the month names
 * @param {function} translate Translation function
 * @returns {object[]} Rows: key, label, count, level, the keys of the values they check and, in the tree, expandable
 */
export function getValueRows(values, tree, expanded, language, translate) {
  const isBlank = item => item.value === null || item.value === undefined;
  const rows = [];
  if (tree) {
    const addNode = node => {
      const expandable = node.children.length > 0;
      rows.push({key: node.key, label: node.label, count: node.count, level: node.level, leafKeys: getLeafKeys(node),
        expandable, expanded: expandable && !!expanded[node.key], value: node.value});
      if (expandable && expanded[node.key]) {
        node.children.forEach(addNode);
      }
    };
    buildDateTree(values, language).forEach(addNode);
  } else {
    values.filter(item => !isBlank(item)).forEach(item => rows.push({key: getValueKey(item.value),
      label: String(item.label ?? item.value), count: item.count, level: 0, leafKeys: [getValueKey(item.value)],
      value: item.value}));
  }
  values.filter(isBlank).forEach(item => rows.push({key: BLANKS_KEY, label: translate("GRID_FILTER_BLANKS"),
    count: item.count, level: 0, leafKeys: [BLANKS_KEY], blanks: true}));
  const allKeys = values.map(item => getValueKey(item.value));
  return [{key: SELECT_ALL, label: translate("GRID_FILTER_SELECT_ALL"), level: 0, leafKeys: allKeys, selectAll: true}, ...rows];
}

/**
 * Read a flag the server sends as a boolean or as its text
 * @param {*} value Value
 * @returns {boolean} True for true and "true"
 */
const isTrue = value => value === true || String(value).toLowerCase() === "true";

/**
 * Read the answer of the column-values action: the values (value, label and count), whether the column has more values
 * than the listed ones, and the other actions of the answer (an error message), which are launched as usual
 * @param {*} actions Actions of the answer
 * @returns {{values: object[], more: boolean, others: object[]}} Values, more values, other actions
 */
export function readColumnValues(actions) {
  if (!Array.isArray(actions)) {
    return {values: [], more: false, others: []};
  }
  const list = actions.filter(action => action && typeof action === "object");
  const answer = list.find(action => action.type === COLUMN_VALUES_ACTION);
  const parameters = (answer && answer.parameters) || {};
  return {
    values: (parameters.datalist && parameters.datalist.rows) || [],
    more: isTrue(parameters.more),
    others: list.filter(action => !OWN_ACTIONS.has(action.type))
  };
}

/**
 * @ngdoc service
 * @name GridColumnFilters
 * @description Column autofilter of the grids (#912): it gives a grid component its column filters, which travel in
 * every request of the grid as "columnFilters" (only while there are some), the functions that change them (each change
 * reloads the grid from the server from its first page, also a grid that loads all its rows), the sort of a column from
 * its menu, the chips of the filters in effect and the request of the values of a column (column-values action).
 */
aweApplication.factory('GridColumnFilters',
  ['ServerData', 'Ajax', 'AweSettings', 'AweUtilities', 'ActionController', 'Storage', '$q', '$translate', '$filter', 'uiGridConstants',
    /**
     * Grid column filters
     * @param {service} ServerData Server data service
     * @param {service} Ajax Ajax service
     * @param {service} $settings AWE settings
     * @param {service} Utilities AWE utilities
     * @param {service} $actionController Action controller
     * @param {service} Storage Storage service
     * @param {service} $q Promise service
     * @param {service} $translate Translation service
     * @param {service} $filter Filter service
     * @param {service} uiGridConstants ui-grid constants
     */
    function (ServerData, Ajax, $settings, Utilities, $actionController, Storage, $q, $translate, $filter, uiGridConstants) {
      const translate = (key, values) => $translate.instant(key, values);

      /**
       * Label of a column, translated
       * @param {object} column Column
       * @returns {string} Label
       */
      const getColumnLabel = column => $filter("translateMultiple")((column && column.label) || "");

      return {
        /**
         * Give a grid component its column filters
         * @param {object} component Grid component
         */
        attach(component) {
          // Column filters in effect, and the labels of their values by column (the chips show them; not sent)
          component.columnFilters = [];
          component.columnFilterLabels = {};

          const getColumnModel = () => (component.controller && component.controller.columnModel) || [];

          /**
           * Filter in effect of a column
           * @param {string} name Column name
           * @returns {object|undefined} Filter of the column
           */
          component.getColumnFilter = name => getColumnFilter(component.columnFilters, name);

          /**
           * Parameters of the column filters for the requests of the grid: none while no column is filtered, so the
           * request of a grid without filters does not change
           * @returns {object} {columnFilters} or nothing
           */
          component.getColumnFiltersParameters = () => component.columnFilters.length > 0 ?
            {columnFilters: component.columnFilters.map(filter => ({...filter}))} : {};

          /**
           * Change the column filters and reload the grid from the server, from its first page
           * @param {object[]} filters New column filters
           * @param {object} labels Labels of their values by column
           */
          const reload = (filters, labels) => {
            component.columnFilters = filters;
            component.columnFilterLabels = labels;
            component.currentPage = 1;
            if (component.model) {
              component.model.page = 1;
            }
            if (component.scope && component.scope.gridOptions) {
              component.scope.gridOptions.paginationCurrentPage = 1;
            }
            component.reload();
          };

          /**
           * Apply the filter of a column (null removes it), with the labels of its values, and reload the grid
           * @param {string} name Column name
           * @param {object|null} filter Filter of the column
           * @param {object} [valueLabels] Label of each value key, as the values list showed them
           */
          component.applyColumnFilter = (name, filter, valueLabels = null) => {
            const labels = Object.fromEntries(Object.entries(component.columnFilterLabels).filter(([column]) => column !== name));
            if (filter && filter.values && valueLabels) {
              labels[name] = valueLabels;
            }
            reload(setColumnFilter(component.columnFilters, name, filter), labels);
          };

          /**
           * Remove the filter of a column and reload the grid
           * @param {string} name Column name
           */
          component.removeColumnFilter = name => component.applyColumnFilter(name, null);

          /**
           * Remove every column filter and reload the grid. While the default search of the grid is in effect (#945) the
           * grid leaves it instead: it returns to the state of the XML
           */
          component.clearColumnFilters = () => {
            if (!component.leaveDefaultSearch?.()) {
              reload([], {});
            }
          };

          /**
           * Set the sort of the columns without the sort event, which would reload the grid: on the columns of ui-grid, or
           * on the column definitions while ui-grid has not built them (it takes their sort when it creates them)
           * @param {{id: string, direction: string}[]} sort Sort, by column name or sort field
           */
          const setSort = sort => {
            const grid = component.grid && component.grid.api && component.grid.api.grid;
            const built = grid && grid.columns.length > 0;
            const columns = built ? grid.columns.map(column => ({target: column, definition: column.colDef || {}})) :
              getColumnModel().map(column => ({target: column, definition: column}));
            const sorting = [];
            columns.forEach(({target}) => {
              target.sort = {};
            });
            sort.forEach(item => {
              const column = columns.find(({definition}) => definition.sortField === item.id || definition.name === item.id);
              if (column) {
                column.target.sort = {direction: item.direction === "desc" ? uiGridConstants.DESC : uiGridConstants.ASC, priority: sorting.length};
                sorting.push({id: column.definition.sortField, direction: column.target.sort.direction});
              }
            });
            component.grid.sorting = sorting;
          };

          /**
           * Restore the column filters, their labels and the sort of a saved search (#912) and reload the grid once: the
           * sort is set on the columns without the sort event, which would reload the grid again
           * @param {object[]} filters Column filters
           * @param {object} labels Labels of their values by column
           * @param {{id: string, direction: string}[]} sort Sort, by column name or sort field
           */
          component.restoreColumnFilters = (filters, labels, sort) => {
            if (component.grid?.api?.grid) {
              setSort(sort);
            }
            reload(filters, labels);
          };

          /**
           * Take the column filters, their labels and the sort of the default search the server applied before the initial
           * load (#945), without a reload: the rows already came filtered and sorted
           * @param {object[]} filters Column filters
           * @param {object} labels Labels of their values by column
           * @param {{id: string, direction: string}[]} sort Sort, by column name or sort field
           */
          component.seedColumnFilters = (filters, labels, sort) => {
            component.columnFilters = filters;
            component.columnFilterLabels = labels;
            setSort(sort);
          };

          /**
           * Chips of the column filters in effect: the label of the column (also of a hidden one) and the summary of its
           * filter ("Status: Inactive")
           * @returns {object[]} Chips (id, label and summary)
           */
          component.getColumnFilterChips = () => component.columnFilters.map(filter => {
            const column = getColumnModel().find(item => item.name === filter.id);
            return {
              id: filter.id,
              label: column ? getColumnLabel(column) : filter.id,
              summary: summarizeColumnFilter(filter, translate, component.columnFilterLabels[filter.id])
            };
          });

          /**
           * Sort a grid column from its autofilter menu, as a click on its header: the column alone, in a direction
           * @param {string} name Column name
           * @param {string} direction asc or desc
           */
          component.sortColumnFromMenu = (name, direction) => {
            const grid = component.grid && component.grid.api && component.grid.api.grid;
            const column = grid && grid.getColumn(name);
            if (column) {
              grid.sortColumn(column, direction === "desc" ? uiGridConstants.DESC : uiGridConstants.ASC, false)
                .then(() => grid.refresh());
            }
          };

          /**
           * Load the distinct values of a column (S15): the column-values action of the query of the grid with the
           * values of the screen, the parameters of the grid (its column filters included; the server leaves out the
           * filter of the column itself) and the column, its fields, its type and the search. The other actions of the
           * answer (an error message) are launched as usual
           * @param {object} column Column
           * @param {string} [search] Text the labels contain
           * @returns {{promise: Promise, cancel: function}} Values (value, label and count) and whether more exist, and
           * the function that cancels the request (a newer search, the menu closed)
           */
          component.loadColumnValues = (column, search) => {
            const canceller = $q.defer();
            const text = String(search ?? "").trim();
            const targetAction = component.controller[$settings.get("targetActionKey")];
            const screen = (Storage.get("screen") || {})[component.address.view] || {};
            const parameters = {
              ...ServerData.getFormValues(component.address),
              screen: screen.name,
              address: component.address,
              component: component.address.component,
              ...component.getSpecificFields(),
              columnValues: {
                id: column.name,
                fields: getFilterFields(column),
                type: getColumnFilterType(column),
                ...(text ? {search: text} : {})
              }
            };
            parameters[$settings.get("serverActionKey")] = COLUMN_VALUES_ACTION;
            parameters[$settings.get("targetActionKey")] = targetAction;

            const promise = Ajax.httpRequest({
              method: "POST",
              url: Ajax.getActionUrl(COLUMN_VALUES_ACTION, targetAction),
              data: parameters,
              timeout: canceller.promise
            }).then(response => {
              const {values, more, others} = readColumnValues(Utilities.decodeData(response.data, $settings.get("encodeTransmission")));
              if (others.length > 0) {
                $actionController.addActionList(others, false, {address: component.address, context: component.context});
              }
              return {values, more};
            });
            return {promise, cancel: () => canceller.resolve()};
          };
        }
      };
    }
  ]);
