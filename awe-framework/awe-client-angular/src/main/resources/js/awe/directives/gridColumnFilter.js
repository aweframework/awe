import {aweApplication} from "../awe";
import "bootstrap-datepicker";
import {TestAttributes, TestIds} from "../data/testIds";
import {iconMarkup} from "../services/icon";
import {createFocusTrap} from "../services/focusTrap";
import {
  RELATIVE_DATE_LABELS,
  buildConditionsFilter,
  buildDateTree,
  buildRelativeDateFilter,
  buildValuesFilter,
  getCheckState,
  getInitialChecked,
  getOperatorLabel,
  getOperators,
  getValueKey,
  operatorNeedsValue
} from "awe-client-react/utilities/columnFilters";
import {SEARCH_DELAY, getInitialConditions, getSubmenuOptions, getValueRows} from "../services/grid/columnFilters";

// Sort icons and labels of each type
const SORT_TEXTS = {
  values: {asc: ["lucide:arrow-up-a-z", "GRID_FILTER_SORT_ASC_TEXT"], desc: ["lucide:arrow-down-z-a", "GRID_FILTER_SORT_DESC_TEXT"]},
  text: {asc: ["lucide:arrow-up-a-z", "GRID_FILTER_SORT_ASC_TEXT"], desc: ["lucide:arrow-down-z-a", "GRID_FILTER_SORT_DESC_TEXT"]},
  number: {asc: ["lucide:arrow-up-0-1", "GRID_FILTER_SORT_ASC_NUMBER"], desc: ["lucide:arrow-down-1-0", "GRID_FILTER_SORT_DESC_NUMBER"]},
  date: {asc: ["lucide:calendar-arrow-up", "GRID_FILTER_SORT_ASC_DATE"], desc: ["lucide:calendar-arrow-down", "GRID_FILTER_SORT_DESC_DATE"]}
};

// Label of the type submenu of each type
const SUBMENU_LABELS = {text: "GRID_FILTER_TEXT_FILTERS", number: "GRID_FILTER_NUMBER_FILTERS", date: "GRID_FILTER_DATE_FILTERS"};

// Overlays opened from the controls of the menu (the operator lists, the date pickers): a click inside them is not a
// click outside the menu
const OVERLAYS = ".datepicker, .dropdown-menu";

// Room under the funnel below which the menu opens above it, when there is more room there
const MIN_ROOM_BELOW = 0.4;

// Hooks of the parts of the menu
const owner = `ng-attr-${TestAttributes.owner}="{{::menu.gridId}}"`;
const optionHook = key => `data-testid="${TestIds.gridColumnFilterOption}" ${owner} option-id="${key}"`;

// Funnel of a column header: outlined while the column is not filtered, filled while it is
export const GRID_COLUMN_FILTER_TEMPLATE = `<button type="button" class="grid-column-filter-button" ng-class="{'grid-column-filter-active': isFiltered()}"
  title="{{title()}}" aria-label="{{title()}}" aria-haspopup="dialog" aria-expanded="{{open}}" data-testid="${TestIds.gridColumnFilter}"
  ng-attr-${TestAttributes.owner}="{{::gridId}}" ng-attr-column-id="{{::columnName}}" ng-attr-${TestAttributes.active}="{{isFiltered()}}"
  ng-click="toggle($event)" ng-keydown="$event.stopPropagation()" ng-mousedown="$event.stopPropagation()">${iconMarkup("lucide:funnel")}</button>`;

/**
 * An option of the menu
 * @param {string} key Option key (its "option-id")
 * @param {string} icon Icon
 * @param {string} text Expression of its text
 * @param {string} click Expression of its click
 * @param {string} [disabled] Expression that disables it
 * @returns {string} Template
 */
const optionTemplate = (key, icon, text, click, disabled = "") => `<button type="button" class="grid-column-filter-option" ${optionHook(key)}
    ng-click="${click}"${disabled ? ` ng-disabled="${disabled}"` : ""}>${iconMarkup(icon)}<span class="grid-column-filter-option-label">{{${text}}}</span></button>`;

/**
 * Type submenu of the menu (text, number or date filters): its trigger and the menu at its side
 * @param {string} type Filter type
 * @returns {string} Template
 */
const submenuTemplate = type => {
  const options = getSubmenuOptions(type).map(option => option.separator ?
    `<div role="separator" class="grid-column-filter-separator"></div>` :
    `<button type="button" role="menuitem" tabindex="-1" class="grid-column-filter-option" ${optionHook(option.key)}
       ng-click="menu.chooseOption('${option.key}')">{{::menu.optionLabels['${option.key}']}}</button>`).join("");
  return `<div class="grid-column-filter-submenu">
    <button type="button" class="grid-column-filter-option grid-column-filter-submenu-trigger" aria-haspopup="menu" aria-expanded="{{menu.submenuOpen}}"
      ng-click="menu.toggleSubmenu()" ng-keydown="menu.onSubmenuTriggerKeyDown($event)" ${optionHook("conditions")}>${iconMarkup(type === "date" ? "lucide:calendar" : "lucide:funnel")}<span class="grid-column-filter-option-label">{{::menu.submenuLabel}}</span>${iconMarkup("lucide:chevron-right grid-column-filter-option-end")}</button>
    <div ng-if="menu.submenuOpen" role="menu" aria-label="{{::menu.submenuLabel}}" class="grid-column-filter-flyout" ng-class="{'grid-column-filter-flyout-start': menu.flip}"
      ng-keydown="menu.onSubmenuKeyDown($event)">${options}</div>
  </div>`;
};

// Values list: (Select all), the values (or the date tree) and (Blanks), one stop of the Tab order
const valuesTemplate = `<div class="grid-column-filter-list" role="{{::menu.tree ? 'tree' : 'group'}}" aria-label="{{::menu.valuesLabel}}" ng-keydown="menu.onListKeyDown($event)">
  <div ng-repeat="row in menu.rows track by row.key" class="grid-column-filter-value grid-column-filter-level-{{row.level}}" role="{{::menu.tree ? 'treeitem' : 'checkbox'}}"
       data-row-index="{{$index}}" tabindex="{{$index === menu.focusIndex ? 0 : -1}}" aria-checked="{{row.state === 'mixed' ? 'mixed' : row.state === 'checked'}}"
       ng-attr-aria-expanded="{{menu.tree && row.expandable ? row.expanded : undefined}}" ng-attr-aria-level="{{menu.tree ? row.level + 1 : undefined}}"
       data-testid="{{row.selectAll ? '${TestIds.gridColumnFilterSelectAll}' : '${TestIds.gridColumnFilterValue}'}}" ${owner}
       ng-attr-${TestAttributes.selected}="{{row.selectAll ? undefined : row.state === 'checked'}}" ng-attr-${TestAttributes.type}="{{row.blanks ? 'blanks' : undefined}}"
       ng-attr-option-id="{{row.selectAll ? undefined : row.optionId}}" ng-click="menu.clickRow($index, row)">
    <span ng-if="::menu.tree" class="grid-column-filter-toggle" aria-hidden="true" ng-click="menu.clickToggle($event, $index, row)">${iconMarkup("lucide:chevron-right", 'ng-if="row.expandable && !row.expanded"')}${iconMarkup("lucide:chevron-down", 'ng-if="row.expandable && row.expanded"')}</span>
    <span class="grid-column-filter-check grid-column-filter-check-{{row.state}}" aria-hidden="true">${iconMarkup("lucide:check", "ng-if=\"row.state === 'checked'\"")}${iconMarkup("lucide:minus", "ng-if=\"row.state === 'mixed'\"")}</span>
    <span class="grid-column-filter-value-label">{{row.label}}</span>
    <span ng-if="!row.selectAll && row.count !== undefined && row.count !== null" class="grid-column-filter-value-count">{{row.count}}</span>
  </div>
</div>`;

/**
 * Main view of the menu: sort, clear the filter of the column, the type submenu, the values with their search, and
 * Accept and Cancel
 * @param {string} type Filter type
 * @param {boolean} sortable The column sorts from its menu
 * @returns {string} Template
 */
const mainTemplate = (type, sortable) => `<div class="grid-column-filter-main">
  <div class="grid-column-filter-actions">
    ${sortable ? optionTemplate("sort-asc", SORT_TEXTS[type].asc[0], "::menu.sortAscLabel", "menu.sort('asc')") +
      optionTemplate("sort-desc", SORT_TEXTS[type].desc[0], "::menu.sortDescLabel", "menu.sort('desc')") +
      `<div role="separator" class="grid-column-filter-separator"></div>` : ""}
    ${optionTemplate("clear", "lucide:funnel-x", "::menu.clearLabel", "menu.apply(null)", "!menu.filter")}
    ${type === "values" ? "" : submenuTemplate(type)}
  </div>
  <div class="grid-column-filter-values">
    <span class="grid-column-filter-search">${iconMarkup("lucide:search")}<input type="search" ng-model="menu.search" ng-change="menu.searchChanged()"
      placeholder="{{::'GRID_FILTER_SEARCH' | translateMultiple}}" aria-label="{{::'GRID_FILTER_SEARCH' | translateMultiple}}" autocomplete="off"
      data-testid="${TestIds.gridColumnFilterSearch}" ${owner}/></span>
    <p ng-if="menu.loading && !menu.values.length" class="grid-column-filter-note" role="status">{{::'GRID_FILTER_LOADING' | translateMultiple}}</p>
    <p ng-if="!menu.loading && !menu.values.length" class="grid-column-filter-note" role="status">{{::'GRID_FILTER_NO_VALUES' | translateMultiple}}</p>
    <div ng-if="menu.values.length">${valuesTemplate}</div>
    <p ng-if="menu.more && menu.values.length" class="grid-column-filter-note" role="status">{{menu.moreText()}}</p>
  </div>
  <div class="grid-column-filter-footer">
    <button type="button" class="btn btn-awe btn-sm" ng-click="menu.close()" data-testid="${TestIds.gridColumnFilterCancel}" ${owner}>{{::'BUTTON_CANCEL' | translateMultiple}}</button>
    <button type="button" class="btn btn-awe btn-primary btn-sm" ng-click="menu.accept()" ng-disabled="!menu.canAccept()" data-testid="${TestIds.gridColumnFilterAccept}" ${owner}>{{::'BUTTON_ACCEPT' | translateMultiple}}</button>
  </div>
</div>`;

/**
 * A condition of the custom filter: its operator (a list of the operators of the type; the second one may be none) and
 * its value, two for between (date pickers on a date column)
 * @param {number} index Condition index
 * @param {string} type Filter type
 * @returns {string} Template
 */
const conditionTemplate = (index, type) => {
  const condition = `menu.conditions[${index}]`;
  const input = (field, label) => `<input type="text" class="grid-column-filter-input" ng-model="${condition}.${field}" autocomplete="off"
      aria-label="{{::menu.conditionLabel('${label}', ${index})}}"${type === "number" ? ' inputmode="decimal"' : ""}${type === "date" ? " awe-grid-column-filter-date" : ""}/>`;
  return `<div class="grid-column-filter-condition">
    <div class="dropdown grid-column-filter-operator">
      <button type="button" class="grid-column-filter-select" data-toggle="dropdown" aria-haspopup="listbox" aria-expanded="false"
        aria-label="{{::menu.conditionLabel('GRID_FILTER_CONDITION_OPERATOR', ${index})}}"><span class="grid-column-filter-select-text">{{menu.operatorText(${index})}}</span>${iconMarkup("lucide:chevron-down")}</button>
      <ul class="dropdown-menu grid-column-filter-operator-list" role="listbox" aria-label="{{::menu.conditionLabel('GRID_FILTER_CONDITION_OPERATOR', ${index})}}">
        <li ng-repeat="option in ::menu.operatorOptions[${index}] track by option.value" role="presentation" ng-class="{'active': ${condition}.operator === option.value}">
          <a href="" role="option" aria-selected="{{${condition}.operator === option.value}}" ng-click="menu.setOperator(${index}, option.value)">{{::option.label}}</a>
        </li>
      </ul>
    </div>
    <span ng-if="menu.needsValue(${index})" class="grid-column-filter-input-wrapper">${input("value", "GRID_FILTER_CONDITION_VALUE")}</span>
    <span ng-if="${condition}.operator === 'between'" class="grid-column-filter-input-wrapper">${input("to", "GRID_FILTER_CONDITION_TO")}</span>
  </div>`;
};

/**
 * Custom filter of the menu: two conditions joined by And or Or, and Accept and Cancel
 * @param {string} type Filter type
 * @returns {string} Template
 */
const customTemplate = type => `<div class="grid-column-filter-custom" role="group" aria-labelledby="{{::menu.headId}}">
  <div class="grid-column-filter-head" id="{{::menu.headId}}">
    <span>{{::'GRID_FILTER_CUSTOM_TITLE' | translateMultiple}}</span><span class="grid-column-filter-head-column">{{::menu.label}}</span>
  </div>
  <div class="grid-column-filter-form">
    ${conditionTemplate(0, type)}
    <div class="grid-column-filter-union" role="radiogroup" aria-label="{{::'GRID_FILTER_UNION' | translateMultiple}}" ng-keydown="menu.onUnionKeyDown($event)">
      <button type="button" role="radio" class="grid-column-filter-union-option" aria-checked="{{menu.union === 'and'}}" tabindex="{{menu.union === 'and' ? 0 : -1}}"
        ng-click="menu.union = 'and'">{{::'GRID_FILTER_AND' | translateMultiple}}</button><button type="button" role="radio" class="grid-column-filter-union-option"
        aria-checked="{{menu.union === 'or'}}" tabindex="{{menu.union === 'or' ? 0 : -1}}" ng-click="menu.union = 'or'">{{::'GRID_FILTER_OR' | translateMultiple}}</button>
    </div>
    ${conditionTemplate(1, type)}
  </div>
  <div class="grid-column-filter-footer">
    <button type="button" class="btn btn-awe btn-sm" ng-click="menu.close()" data-testid="${TestIds.gridColumnFilterCancel}" ${owner}>{{::'BUTTON_CANCEL' | translateMultiple}}</button>
    <button type="button" class="btn btn-awe btn-primary btn-sm" ng-click="menu.acceptCustom()" ng-disabled="!menu.customResult()" data-testid="${TestIds.gridColumnFilterAccept}" ${owner}>{{::'BUTTON_ACCEPT' | translateMultiple}}</button>
  </div>
</div>`;

/**
 * Menu of the autofilter of a column, a dialog under its funnel
 * @param {string} type Filter type
 * @param {boolean} sortable The column sorts from its menu
 * @returns {string} Template
 */
export const getColumnFilterMenuTemplate = (type, sortable) => `<div class="grid-column-filter-menu" role="dialog" aria-label="{{::menu.title}}" tabindex="-1"
     data-testid="${TestIds.gridColumnFilterMenu}" ${owner} ng-attr-column-id="{{::menu.column.name}}" ng-class="{'grid-column-filter-menu-above': menu.above}">
  <div ng-if="menu.view.name !== 'custom'">${mainTemplate(type, sortable)}</div>
  <div ng-if="menu.view.name === 'custom'">${customTemplate(type)}</div>
</div>`;

/**
 * Place of the menu under the funnel, inside the window (fixed coordinates): it goes left when it would go out at the
 * right, and above the funnel when there is little room under it. Its height is the room it has, so its list scrolls
 * @param {HTMLElement} anchor Funnel
 * @param {HTMLElement} menu Menu
 * @returns {{top: number, left: number, room: number, above: boolean}} Place of the menu
 */
export function getMenuPlacement(anchor, menu) {
  const rect = anchor.getBoundingClientRect();
  const width = (menu && menu.offsetWidth) || 0;
  const right = document.documentElement.clientWidth;
  const left = Math.max(0, Math.min(rect.left, right - width));
  const below = window.innerHeight - rect.bottom;
  if (below < window.innerHeight * MIN_ROOM_BELOW && rect.top > below) {
    return {top: rect.top, left, room: rect.top, above: true};
  }
  return {top: rect.bottom, left, room: below, above: false};
}

/**
 * @ngdoc directive
 * @name aweGridColumnFilter
 * @description Funnel of a grid column that declares a filter (#912, S18), next to the label of its header: it opens
 * the autofilter menu of the column, anchored under it (sort, clear, the filters of its type with the custom filter, and
 * the values of the column with their search). Accept applies the filter; Cancel, Escape and a click outside close it
 * without changes, and the focus goes back to the funnel.
 */
aweApplication.directive('aweGridColumnFilter',
  ['$compile', '$timeout', '$translate', '$filter', 'AweSettings',
    function ($compile, $timeout, $translate, $filter, $settings) {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_COLUMN_FILTER_TEMPLATE,
        scope: {
          col: '<'
        },
        link: function (scope, elem) {
          const column = scope.col.colDef;
          const component = scope.col.grid.appScope.component;
          const type = column.columnFilterType;
          const translate = (key, values) => $translate.instant(key, values);
          const label = () => $filter("translateMultiple")(column.label || "");
          let menu = null;

          scope.gridId = component.address.component;
          scope.columnName = column.name;
          scope.open = false;
          scope.title = () => translate("GRID_FILTER_BUTTON", {label: label()});
          scope.isFiltered = () => !!component.getColumnFilter(column.name);

          /**
           * Open the menu under the funnel
           */
          const openMenu = () => {
            const sortable = !!column.enableSorting;
            const menuScope = scope.$new(true);
            const state = createMenuState(menuScope);
            menuScope.menu = state;
            const element = $compile(getColumnFilterMenuTemplate(type, sortable))(menuScope);
            const host = elem.closest(".modal")[0] || elem.closest(".maximize-target")[0] || document.body;
            // Hidden until it is placed: shown where it is created first, it would jump to its place on the next digest
            element[0].style.visibility = "hidden";
            host.appendChild(element[0]);
            const trap = createFocusTrap(() => element[0]);
            trap.activate();

            const place = () => {
              const placement = getMenuPlacement(elem[0], element[0]);
              element.css({top: `${Math.round(placement.top)}px`, left: `${Math.round(placement.left)}px`});
              element[0].style.setProperty("--grid-column-filter-room", `${Math.round(placement.room)}px`);
              // The side is set at once, with the position, so the menu is never painted on the wrong side
              element.toggleClass("grid-column-filter-menu-above", placement.above);
              if (state.above !== placement.above) {
                state.above = placement.above;
                menuScope.$evalAsync();
              }
              element[0].style.visibility = "";
            };

            // A mouse down outside closes it; on the funnel it is left to its click, which closes it
            const onMouseDown = event => {
              const target = event.target;
              const overlay = target.closest ? target.closest(OVERLAYS) : null;
              if (element[0].contains(target) || elem[0].contains(target) || (overlay && !overlay.contains(element[0]))) {
                return;
              }
              scope.$apply(closeMenu);
            };

            // Escape closes it unless a date picker of its controls is open (it closes itself) or one of its lists is
            // open (the menu closes the list and keeps the key: a modal behind would close on it) or the type submenu
            // is open (it closes and the focus goes back to its trigger). Listened while the event goes down, before
            // the date picker, which keeps Escape for itself
            const onKeyDown = event => {
              if (event.key !== "Escape") {
                return;
              }
              if ($(".datepicker").filter(":visible").length > 0) {
                return;
              }
              const openList = element.find(".dropdown.open");
              if (openList.length > 0) {
                event.preventDefault();
                event.stopPropagation();
                const toggle = openList.removeClass("open").find("[data-toggle='dropdown']").attr("aria-expanded", "false");
                toggle.trigger("focus");
                return;
              }
              event.preventDefault();
              event.stopPropagation();
              scope.$apply(() => (state.submenuOpen ? state.closeSubmenu() : closeMenu()));
            };

            menu = {element, menuScope, state, trap, place, onMouseDown, onKeyDown};
            document.addEventListener("mousedown", onMouseDown, true);
            element[0].addEventListener("keydown", onKeyDown, true);
            window.addEventListener("resize", place);
            window.addEventListener("scroll", place, true);
            scope.open = true;
            state.load();
            menuScope.$$postDigest(() => {
              place();
              state.focusFirst();
            });
          };

          /**
           * Close the menu, without changes, and give the focus back to the funnel
           */
          const closeMenu = () => {
            if (!menu) {
              return;
            }
            const {element, menuScope, state, trap, place, onMouseDown, onKeyDown} = menu;
            menu = null;
            document.removeEventListener("mousedown", onMouseDown, true);
            element[0].removeEventListener("keydown", onKeyDown, true);
            window.removeEventListener("resize", place);
            window.removeEventListener("scroll", place, true);
            state.destroy();
            trap.deactivate();
            menuScope.$destroy();
            element.remove();
            scope.open = false;
          };

          /**
           * State of the menu of the column and its functions
           * @param {object} menuScope Scope of the menu
           * @returns {object} Menu state
           */
          function createMenuState(menuScope) {
            const columnLabel = label();
            const decimalSeparator = ($settings.get("numericOptions") || {}).aDec || ".";
            const language = $settings.getLanguage();
            const seen = new Set();
            let request = null;
            let searchTimer = null;
            let expanded = {};
            let touched = false;

            const operatorOptions = index => {
              const options = getOperators(type).map(value => ({value, label: translate(getOperatorLabel(value, type))}));
              return index === 0 ? options : [{value: "", label: translate("GRID_FILTER_NO_CONDITION")}, ...options];
            };

            const state = {
              gridId: component.address.component,
              column,
              label: columnLabel,
              title: translate("GRID_FILTER_BUTTON", {label: columnLabel}),
              headId: `${component.address.component}-${column.name}-column-filter-custom`,
              filter: component.getColumnFilter(column.name),
              tree: type === "date",
              view: {name: "main"},
              search: "",
              query: "",
              values: [],
              more: false,
              loading: true,
              checked: new Set(),
              rows: [],
              focusIndex: 0,
              submenuOpen: false,
              flip: false,
              above: false,
              conditions: [],
              union: "and",
              sortAscLabel: translate(SORT_TEXTS[type].asc[1]),
              sortDescLabel: translate(SORT_TEXTS[type].desc[1]),
              clearLabel: translate("GRID_FILTER_CLEAR", {label: columnLabel}),
              submenuLabel: translate(SUBMENU_LABELS[type] || ""),
              valuesLabel: translate("GRID_FILTER_VALUES", {label: columnLabel}),
              optionLabels: Object.fromEntries(getSubmenuOptions(type).filter(option => !option.separator).map(option => {
                if (option.operator) {
                  return [option.key, `${translate(getOperatorLabel(option.operator, type))}…`];
                }
                return [option.key, translate(option.relative ? RELATIVE_DATE_LABELS[option.relative] : "GRID_FILTER_CUSTOM")];
              })),
              operatorOptions: [operatorOptions(0), operatorOptions(1)]
            };

            const element = () => (menu ? menu.element : $());
            const stateOf = row => getCheckState(row.leafKeys.filter(key => state.checked.has(key)).length, row.leafKeys.length);
            const refreshRows = () => {
              state.rows = getValueRows(state.values, state.tree, expanded, language, translate).map(row => ({
                ...row,
                state: stateOf(row),
                optionId: row.blanks ? "" : String(row.value ?? row.key)
              }));
              state.focusIndex = Math.max(0, Math.min(state.focusIndex, state.rows.length - 1));
            };
            const focusRow = index => {
              state.focusIndex = Math.max(0, Math.min(index, state.rows.length - 1));
              menuScope.$$postDigest(() => {
                const node = element().find(".grid-column-filter-value")[state.focusIndex];
                if (node) {
                  node.focus();
                }
              });
            };
            const checkedShown = () => state.values.filter(item => state.checked.has(getValueKey(item.value)));
            const apply = (filter, labels) => {
              closeMenu();
              component.applyColumnFilter(column.name, filter, labels);
            };

            /**
             * Check the values loaded for the first time that the filter of the column holds
             * @param {object[]} values Loaded values
             */
            const checkNewValues = values => {
              const initial = getInitialChecked(state.filter, values);
              values.map(item => getValueKey(item.value)).filter(key => !seen.has(key)).forEach(key => {
                seen.add(key);
                if (initial.has(key)) {
                  state.checked.add(key);
                }
              });
            };

            /**
             * Load the values of the column with the other filters of the grid; a newer search or closing the menu
             * cancels the request
             */
            state.load = () => {
              if (request) {
                request.cancel();
              }
              const current = component.loadColumnValues(column, state.query);
              request = current;
              state.loading = true;
              current.promise.then(({values, more}) => {
                if (request !== current) {
                  return;
                }
                checkNewValues(values);
                // A list starts with the focus on its first row and, in the tree, a single year open
                const years = state.tree ? buildDateTree(values, language) : [];
                expanded = years.length === 1 ? {[years[0].key]: true} : {};
                state.values = values;
                state.more = more;
                state.loading = false;
                state.focusIndex = 0;
                refreshRows();
              }).catch(() => {
                if (request === current) {
                  state.values = [];
                  state.more = false;
                  state.loading = false;
                  refreshRows();
                }
              });
            };

            /**
             * The search asks the values again after a short pause
             */
            state.searchChanged = () => {
              $timeout.cancel(searchTimer);
              searchTimer = $timeout(() => {
                const text = String(state.search || "");
                if (text !== state.query) {
                  state.query = text;
                  state.load();
                }
              }, SEARCH_DELAY);
            };

            /**
             * Move the focus to the search, or to the first control of the custom filter
             */
            state.focusFirst = () => {
              const node = state.view.name === "custom" ? element().find(".grid-column-filter-select")[0] :
                element().find(`[data-testid='${TestIds.gridColumnFilterSearch}']`)[0];
              if (node) {
                node.focus();
              }
            };

            state.close = closeMenu;
            state.apply = filter => apply(filter);
            state.sort = direction => {
              closeMenu();
              component.sortColumnFromMenu(column.name, direction);
            };
            state.moreText = () => translate("GRID_FILTER_MORE_VALUES", {count: state.values.length});
            state.canAccept = () => !state.loading && checkedShown().length > 0;

            /**
             * Accept the values list: the checked values (among the shown ones while searching); without changes and
             * without search the menu only closes
             */
            state.accept = () => {
              const searching = state.query.length > 0;
              if (!touched && !searching) {
                closeMenu();
                return;
              }
              const labels = Object.fromEntries(checkedShown()
                .filter(item => item.value !== null && item.value !== undefined)
                .map(item => [getValueKey(item.value), String(item.label ?? item.value)]));
              apply(buildValuesFilter(column, state.values, state.checked, !searching), labels);
            };

            // Values list
            const toggle = row => {
              touched = true;
              const check = row.state !== "checked";
              row.leafKeys.forEach(key => (check ? state.checked.add(key) : state.checked.delete(key)));
              refreshRows();
            };
            const setExpanded = (row, open) => {
              expanded = {...expanded, [row.key]: open};
              refreshRows();
            };
            state.clickRow = (index, row) => {
              state.focusIndex = index;
              toggle(row);
            };
            state.clickToggle = (event, index, row) => {
              event.stopPropagation();
              if (row.expandable) {
                state.focusIndex = index;
                setExpanded(row, !row.expanded);
              }
            };
            state.onListKeyDown = event => {
              const current = state.focusIndex;
              const row = state.rows[current];
              switch (event.key) {
                case "ArrowDown":
                  focusRow(current + 1);
                  break;
                case "ArrowUp":
                  focusRow(current - 1);
                  break;
                case "Home":
                  focusRow(0);
                  break;
                case "End":
                  focusRow(state.rows.length - 1);
                  break;
                case " ":
                case "Enter":
                  toggle(row);
                  break;
                case "ArrowRight":
                  if (row.expandable && !row.expanded) {
                    setExpanded(row, true);
                  } else if (row.expandable) {
                    focusRow(current + 1);
                  }
                  break;
                case "ArrowLeft":
                  if (row.expandable && row.expanded) {
                    setExpanded(row, false);
                  } else if (row.level > 0) {
                    focusRow(state.rows.slice(0, current).map(item => item.level).lastIndexOf(row.level - 1));
                  }
                  break;
                default:
                  return;
              }
              event.preventDefault();
              event.stopPropagation();
            };

            // Type submenu
            const submenuItems = () => element().find(".grid-column-filter-flyout [role='menuitem']").toArray();
            state.toggleSubmenu = () => {
              state.submenuOpen = !state.submenuOpen;
              if (state.submenuOpen) {
                menuScope.$$postDigest(() => {
                  const flyout = element().find(".grid-column-filter-flyout")[0];
                  if (flyout) {
                    state.flip = flyout.getBoundingClientRect().right > document.documentElement.clientWidth;
                    menuScope.$evalAsync();
                  }
                  const [first] = submenuItems();
                  if (first) {
                    first.focus();
                  }
                });
              }
            };
            state.closeSubmenu = () => {
              state.submenuOpen = false;
              const trigger = element().find(".grid-column-filter-submenu-trigger")[0];
              if (trigger) {
                trigger.focus();
              }
            };
            state.onSubmenuTriggerKeyDown = event => {
              if (event.key === "ArrowRight" && !state.submenuOpen) {
                event.preventDefault();
                state.toggleSubmenu();
              }
            };
            state.onSubmenuKeyDown = event => {
              const list = submenuItems();
              const index = list.indexOf(document.activeElement);
              switch (event.key) {
                case "ArrowDown":
                  list[(index + 1) % list.length].focus();
                  break;
                case "ArrowUp":
                  list[(index - 1 + list.length) % list.length].focus();
                  break;
                case "Home":
                  list[0].focus();
                  break;
                case "End":
                  list[list.length - 1].focus();
                  break;
                case "ArrowLeft":
                  state.closeSubmenu();
                  break;
                default:
                  return;
              }
              event.preventDefault();
              event.stopPropagation();
            };
            state.chooseOption = key => {
              state.submenuOpen = false;
              const option = getSubmenuOptions(type).find(item => item.key === key);
              if (option.relative) {
                apply(buildRelativeDateFilter(column, option.relative));
                return;
              }
              state.conditions = getInitialConditions(state.filter, option.operator, type);
              state.union = state.filter && state.filter.union === "or" ? "or" : "and";
              state.view = {name: "custom", operator: option.operator};
              menuScope.$$postDigest(() => {
                if (menu) {
                  menu.place();
                }
                state.focusFirst();
              });
            };

            // Custom filter
            state.conditionLabel = (key, index) => translate(key, {number: index + 1});
            state.operatorText = index => {
              const operator = (state.conditions[index] || {}).operator;
              const option = state.operatorOptions[index].find(item => item.value === (operator || ""));
              return option ? option.label : translate("GRID_FILTER_NO_CONDITION");
            };
            state.setOperator = (index, operator) => {
              state.conditions[index] = {...state.conditions[index], operator};
            };
            state.needsValue = index => operatorNeedsValue((state.conditions[index] || {}).operator);
            state.customResult = () => buildConditionsFilter(column, state.conditions, state.union, decimalSeparator);
            state.acceptCustom = () => {
              const result = state.customResult();
              if (result) {
                apply(result);
              }
            };
            state.onUnionKeyDown = event => {
              if (["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) {
                event.preventDefault();
                state.union = state.union === "and" ? "or" : "and";
                menuScope.$$postDigest(() => {
                  const checked = element().find(".grid-column-filter-union [aria-checked='true']")[0];
                  if (checked) {
                    checked.focus();
                  }
                });
              }
            };

            state.destroy = () => {
              $timeout.cancel(searchTimer);
              if (request) {
                request.cancel();
                request = null;
              }
            };

            return state;
          }

          /**
           * Open or close the menu. The header of the column sorts and drags it: the funnel keeps its own clicks
           * @param {Event} event Click
           */
          scope.toggle = function (event) {
            event.stopPropagation();
            elem[0].focus();
            if (menu) {
              closeMenu();
            } else {
              openMenu();
            }
          };

          scope.$on("$destroy", closeMenu);
        }
      };
    }
  ]);

/**
 * @ngdoc directive
 * @name aweGridColumnFilterDate
 * @description Date input of a condition of the custom filter of a date column: a date picker (dd/mm/yyyy) whose chosen
 * date is the value of the condition
 */
aweApplication.directive('aweGridColumnFilterDate',
  ['AweSettings',
    function ($settings) {
      return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, elem, attrs, ngModel) {
          elem.datepicker({
            container: "body",
            format: "dd/mm/yyyy",
            todayHighlight: true,
            autoclose: true,
            language: $settings.getLanguage()
          }).on("changeDate", event => {
            const value = event.format();
            scope.$evalAsync(() => ngModel.$setViewValue(value));
          });
          scope.$on("$destroy", () => elem.datepicker("destroy"));
        }
      };
    }
  ]);
