import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {moveChoice} from "../services/checkboxRadio";
import {isDateRangeValid, DATE_RANGE_FROM_SUFFIX, DATE_RANGE_TO_SUFFIX} from "../services/dateRange";
import {iconMarkup} from "../services/icon";
import {filterGrid} from "./gridSearch";
import "bootstrap-datepicker";

// Quick filter shown as a dropdown button, and the date range quick filter (presets and a custom range)
export const QUICK_FILTER_DROPDOWN = "dropdown";
export const QUICK_FILTER_DATE_RANGE = "date-range";

/**
 * Check if a quick filter is shown as a menu button (a dropdown or a date range) instead of a segmented control
 * @param {string} display Display of the quick filter
 * @returns {boolean} The quick filter is a menu button
 */
export const isMenuQuickFilter = display => display === QUICK_FILTER_DROPDOWN || display === QUICK_FILTER_DATE_RANGE;

/**
 * Text of a custom date range: both dates (dd/MM/yyyy) joined by a dash, an ellipsis for the open end
 * @param {string} from Start date
 * @param {string} to End date
 * @returns {string} Text of the range
 */
export const formatDateRange = (from, to) => `${from || "…"} – ${to || "…"}`;

const owner = `ng-attr-data-testid-owner="{{::gridId}}"`;
const countTemplate = `<span ng-if="hasCount(option)" class="grid-quick-filter-count" ng-attr-data-tone="{{option.tone || undefined}}">{{option.count}}</span>`;

// Menu button of a dropdown or date range quick filter: «Label: option» and its menu (with the counts); a date range adds
// «Custom range…» and its panel with the two dates, Clear and Apply
const MENU_TEMPLATE = `<button type="button" class="grid-quick-filter-toggle" ng-if="::isMenu" aria-haspopup="menu" ng-attr-aria-expanded="{{menu.open}}"
          ng-attr-aria-controls="{{menu.open ? menuId : undefined}}" ng-disabled="controller.readonly" ng-click="menu.toggle()"
          ng-keydown="menu.toggleKeydown($event)" data-testid="${TestIds.gridQuickFilterToggle}" ${owner}>
    <span class="grid-quick-filter-toggle-icon" ng-if="::isDateRange" aria-hidden="true">${iconMarkup("lucide:calendar")}</span>
    <span ng-if="controller.label" ng-class="::isDateRange ? 'grid-quick-filter-toggle-sr' : 'grid-quick-filter-toggle-label'">{{controller.label | translateMultiple}}:</span>
    <span class="grid-quick-filter-toggle-value">{{currentText()}}</span>
    <span class="grid-quick-filter-toggle-chevron" aria-hidden="true">${iconMarkup("lucide:chevron-down")}</span>
  </button>
  <div class="grid-quick-filter-popup" ng-if="::isMenu" ng-attr-hidden="{{menu.open ? undefined : 'hidden'}}">
    <div role="menu" ng-if="!menu.editing" ng-attr-id="{{::menuId}}" class="grid-quick-filter-menu"
         aria-label="{{(controller.label || 'SCREEN_TEXT_FILTERS') | translateMultiple}}" ng-keydown="menu.keydown($event)"
         data-testid="${TestIds.gridQuickFilterMenu}" ${owner}>
      <button type="button" role="menuitemradio" tabindex="-1" class="grid-quick-filter-menu-option" ng-repeat="option in model.values track by $index"
              ng-attr-aria-checked="{{isChecked(option)}}" ng-attr-option-id="{{option.value === null || option.value === undefined ? '' : option.value}}"
              ng-click="menu.choose(option)" data-testid="${TestIds.gridQuickFilterOption}">
        <span class="grid-quick-filter-menu-check" aria-hidden="true"><span ng-if="isChecked(option)">${iconMarkup("lucide:check")}</span></span>
        <span class="grid-quick-filter-menu-text">{{option.label | translateMultiple}}</span> ${countTemplate}
      </button>
      <div class="grid-quick-filter-menu-separator" role="separator" ng-if="::isDateRange"></div>
      <button type="button" role="menuitemradio" tabindex="-1" class="grid-quick-filter-menu-option" ng-if="::isDateRange"
              ng-attr-aria-checked="{{isCustom()}}" ng-click="menu.editRange()" data-testid="${TestIds.gridQuickFilterCustom}">
        <span class="grid-quick-filter-menu-check" aria-hidden="true"><span ng-if="isCustom()">${iconMarkup("lucide:check")}</span></span>
        <span class="grid-quick-filter-menu-text">{{'DATE_RANGE_CUSTOM' | translateMultiple}}</span>
      </button>
    </div>
    <div role="group" ng-if="menu.editing" class="grid-quick-filter-range" aria-label="{{'DATE_RANGE_CUSTOM' | translateMultiple}}"
         ng-keydown="menu.rangeKeydown($event)" data-testid="${TestIds.gridQuickFilterRange}" ${owner}>
      <label class="grid-quick-filter-range-date"><span class="grid-quick-filter-range-label">{{'DATE_RANGE_FROM' | translateMultiple}}</span>
        <input type="text" class="form-control" autocomplete="off" placeholder="dd/mm/yyyy" ng-model="menu.draft.from"
               data-testid="${TestIds.dateRangeFrom}" ${owner}/></label>
      <label class="grid-quick-filter-range-date"><span class="grid-quick-filter-range-label">{{'DATE_RANGE_TO' | translateMultiple}}</span>
        <input type="text" class="form-control" autocomplete="off" placeholder="dd/mm/yyyy" ng-model="menu.draft.to"
               ng-attr-aria-invalid="{{menu.isValid() ? undefined : 'true'}}" data-testid="${TestIds.dateRangeTo}" ${owner}/></label>
      <div class="grid-quick-filter-range-actions">
        <button type="button" class="btn btn-awe btn-sm btn-default" ng-click="menu.clear()" data-testid="${TestIds.gridQuickFilterRangeClear}" ${owner}>{{'BUTTON_CLEAR' | translateMultiple}}</button>
        <button type="button" class="btn btn-awe btn-sm btn-primary" ng-click="menu.apply()" ng-disabled="!menu.isValid()"
                data-testid="${TestIds.gridQuickFilterRangeApply}" ${owner}>{{'DATE_RANGE_APPLY' | translateMultiple}}</button>
      </div>
    </div>
  </div>`;

// Quick filters of a grid: a segmented control (one radio per option, "Label N") at the start of the grid toolbar, or a
// menu button with display="dropdown" or display="date-range"
export const GRID_QUICK_FILTER_TEMPLATE = `<div ng-show="controller.visible" class="{{::isMenu ? 'grid-quick-filter-dropdown' : 'grid-quick-filter'}}" ng-attr-role="{{::isMenu ? undefined : 'radiogroup'}}" ng-attr-aria-label="{{isMenu ? undefined : ((controller.label || 'SCREEN_TEXT_FILTERS') | translateMultiple)}}" ng-attr-criterion-id="{{::controller.id}}" data-testid="${TestIds.gridQuickFilter}" ${owner} ng-attr-aria-busy="{{controller.loading ? 'true' : undefined}}" ng-cloak>
  <label class="grid-quick-filter-option" ng-if="::!isMenu" ng-repeat="option in model.values track by $index" ng-attr-option-id="{{option.value === null || option.value === undefined ? '' : option.value}}" data-testid="${TestIds.gridQuickFilterOption}">
    <input type="radio" ng-attr-name="{{::radioName}}" ng-attr-value="{{option.value}}" ng-checked="isChecked(option)" ng-click="choose(option)" ng-disabled="controller.readonly" data-testid="${TestIds.criterionInput}"/>
    <span class="grid-quick-filter-segment"><span class="grid-quick-filter-label">{{option.label | translateMultiple}}</span> ${countTemplate}</span>
  </label>
  ${MENU_TEMPLATE}
</div>`;

/**
 * Value of an option or of the criterion as text: an empty value (null or undefined) is the empty text, so the option
 * with an empty value (All) is the one of an empty criterion
 * @param {*} value Value
 * @returns {string} Value as text
 */
const asText = value => (value === null || value === undefined ? "" : String(value));

/**
 * Menu of a dropdown or date range quick filter (menu button pattern): the button opens it with a click, Enter, Space or
 * the arrows; the arrows, Home and End move between its options, Escape closes it and gives the focus back to the
 * button. The menu is fixed under its button (the grid clips its content) and closes with a click out of it, a scroll or a
 * resize. «Custom range…» shows two dates (bootstrap datepickers) with Clear and Apply
 * @param {Object} scope Scope of the quick filter
 * @param {Object} elem Element of the quick filter
 * @param {Object} component Quick filter
 * @param {Object} services Control, ActionController, settings, translate and the reader of the custom range
 */
function initMenu(scope, elem, component, {Control, $actionController, $settings, $translate, getRange}) {
  const applyScope = fn => (scope.$root.$$phase ? fn() : scope.$apply(fn));
  const afterDigest = fn => scope.$$postDigest(fn);
  const toggleButton = () => elem.find(".grid-quick-filter-toggle");
  const popup = () => elem.find(".grid-quick-filter-popup");
  const menuItems = () => elem.find("[role=menu] [role^=menuitem]");
  const dateInputs = () => elem.find(".grid-quick-filter-range input");
  let listening = false;

  const filter = (selected, range) => {
    component.model.selected = selected;
    component.model.range = range;
    component.modelChange();
    filterGrid(Control, $actionController, component, scope.gridId);
  };

  // Place the popup under its button, in the coordinates of the viewport, flipped up when there is no room below
  const position = () => afterDigest(() => {
    const button = toggleButton()[0];
    const element = popup()[0];
    if (!button || !element || !menu.open) {
      return;
    }
    const gap = parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--awe-space-1")) || 4;
    const buttonBox = button.getBoundingClientRect();
    const box = element.getBoundingClientRect();
    const fitsBelow = buttonBox.bottom + gap + box.height <= window.innerHeight;
    const top = fitsBelow ? buttonBox.bottom + gap : Math.max(gap, buttonBox.top - box.height - gap);
    const left = Math.max(gap, Math.min(buttonBox.left, window.innerWidth - box.width - gap));
    element.style.top = `${Math.round(top)}px`;
    element.style.left = `${Math.round(left)}px`;
  });

  // The chosen option (or the first one) takes the focus
  const focusChosen = () => afterDigest(() => {
    const items = menuItems();
    const checked = items.filter("[aria-checked=true]");
    (checked.length ? checked : items).first()[0]?.focus({preventScroll: true});
  });

  const destroyPickers = () => {
    const inputs = dateInputs();
    if (inputs.data("datepicker")) {
      inputs.datepicker("destroy");
    }
  };

  const onViewportChange = () => applyScope(() => close(false));
  // A click out of the quick filter closes it; the calendar of a date hangs from the body, so a click on it does not
  const onDocumentMouseDown = event => {
    if ($(event.target).closest(elem).length === 0 && $(event.target).closest(".datepicker").length === 0) {
      applyScope(() => close(false));
    }
  };

  const close = focus => {
    destroyPickers();
    menu.open = false;
    menu.editing = false;
    if (listening) {
      listening = false;
      $(document).off("mousedown", onDocumentMouseDown);
      $(window).off("resize scroll", onViewportChange);
    }
    if (focus) {
      toggleButton()[0]?.focus({preventScroll: true});
    }
  };

  const open = () => {
    menu.open = true;
    if (!listening) {
      listening = true;
      $(document).on("mousedown", onDocumentMouseDown);
      $(window).on("resize scroll", onViewportChange);
    }
    focusChosen();
    position();
  };

  const menu = {
    open: false,
    editing: false,
    draft: {from: "", to: ""},
    toggle() {
      if (menu.open) {
        close(true);
      } else {
        open();
      }
    },
    toggleKeydown(event) {
      if ((event.key === "ArrowDown" || event.key === "ArrowUp") && !menu.open) {
        event.preventDefault();
        open();
      }
    },
    keydown(event) {
      const items = menuItems().toArray();
      const index = items.indexOf(document.activeElement);
      const moves = {ArrowDown: index + 1, ArrowUp: index < 0 ? items.length - 1 : index - 1, Home: 0, End: items.length - 1};
      if (event.key in moves) {
        event.preventDefault();
        items[(moves[event.key] + items.length) % items.length]?.focus();
      } else if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        close(true);
      } else if (event.key === "Tab") {
        close(false);
      }
    },
    rangeKeydown(event) {
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        close(true);
      }
    },
    // Choose an option: the grid is filtered with it (and with no custom range), unless it is the chosen one
    choose(option) {
      close(true);
      if (scope.isChecked(option)) {
        return;
      }
      filter(asText(option.value), ["", ""]);
    },
    // Show the two dates of the custom range, with the range in effect
    editRange() {
      menu.draft = getRange();
      menu.editing = true;
      afterDigest(() => {
        const language = $settings.getLanguage();
        dateInputs().datepicker({format: "dd/mm/yyyy", autoclose: true, todayHighlight: true, todayBtn: "linked",
          ...(language ? {language} : {})});
        dateInputs().first()[0]?.focus({preventScroll: true});
        position();
      });
    },
    isValid() {
      return isDateRangeValid(menu.draft.from, menu.draft.to);
    },
    // Apply the custom range: no preset is sent with it; with no dates it clears it
    apply() {
      const {from = "", to = ""} = menu.draft;
      if (!from && !to) {
        menu.clear();
        return;
      }
      close(true);
      filter(null, [from, to]);
    },
    // Clear: back to the option of an empty value (any date), or the first one
    clear() {
      close(true);
      const values = component.model.values || [];
      const empty = values.find(option => asText(option.value) === "") || values[0];
      filter(empty ? asText(empty.value) : "", ["", ""]);
    }
  };
  scope.menu = menu;

  /**
   * Text of the button: the custom range or the label of the chosen option
   * @returns {string} Text
   */
  scope.currentText = function () {
    if (scope.isCustom()) {
      return formatDateRange(getRange().from, getRange().to);
    }
    const option = (component.model.values || []).find(item => scope.isChecked(item));
    return option ? $translate.instant(String(option.label)) : "";
  };

  scope.$on("$destroy", () => close(false));
}

// Quick filter directive: a criterion of the screen whose options come from its initial load (value, label, count
// and tone). Its value is sent with the requests of the grid; choosing an option filters the grid, and every filter of
// the grid reloads the options, so the counts stay current. With display="dropdown" it is a menu button «Label: option»;
// with display="date-range" too, plus «Custom range…»: it sends its preset (the chosen option) as its value and the
// custom range as <id>.from and <id>.to (dd/MM/yyyy), as the date range criterion does
aweApplication.directive('aweGridQuickFilter',
  ['Criterion', 'Control', 'ActionController', 'AweUtilities', 'AweSettings', '$translate',
    function (Criterion, Control, $actionController, Utilities, $settings, $translate) {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_QUICK_FILTER_TEMPLATE,
        scope: {
          criterionId: '@quickFilterId',
          gridId: '@'
        },
        link: function (scope, elem) {
          const component = new Criterion(scope, scope.criterionId, elem);
          if (!component.asCriterion()) {
            return;
          }
          scope.radioName = `${component.address.view}-${component.address.component}`;
          scope.isMenu = isMenuQuickFilter(component.controller.display);
          scope.isDateRange = component.controller.display === QUICK_FILTER_DATE_RANGE;
          scope.menuId = `${scope.radioName}-menu`;

          // The chosen value: the first one of the model, as text
          const getSelected = () => asText(Utilities.asArray(component.model.selected)[0]);

          // The custom range of a date range quick filter: its start and its end (dd/MM/yyyy)
          const getRange = () => {
            const [from, to] = Utilities.asArray(component.model.range);
            return {from: from || "", to: to || ""};
          };

          /**
           * Check if a date range quick filter has a custom range
           * @returns {boolean} A custom range is set
           */
          scope.isCustom = function () {
            const range = getRange();
            return scope.isDateRange && Boolean(range.from || range.to);
          };

          /**
           * Check if an option is the chosen one. When no option has the value of the criterion, the first one is; none
           * while a custom range is set
           * @param {Object} option Option
           * @returns {boolean} The option is the chosen one
           */
          scope.isChecked = function (option) {
            if (scope.isCustom()) {
              return false;
            }
            const values = component.model.values || [];
            const selected = getSelected();
            const current = values.find(item => asText(item.value) === selected) || values[0];
            return current === option;
          };

          /**
           * Check if an option shows its count
           * @param {Object} option Option
           * @returns {boolean} The option has a count
           */
          scope.hasCount = option => option.count !== null && option.count !== undefined && option.count !== "";

          /**
           * Choose an option: its value is the value of the criterion, and the grid is filtered with it
           * @param {Object} option Option
           */
          scope.choose = function (option) {
            if (component.controller.readonly || (asText(option.value) === getSelected() && scope.isChecked(option))) {
              return;
            }
            component.model.selected = asText(option.value);
            component.modelChange();
            filterGrid(Control, $actionController, component, scope.gridId);
          };

          // The arrow keys move the choice. Listened outside Angular: the click on the next option starts its own digest
          elem.on("keydown", "input[type=radio]", moveChoice);
          scope.$on("$destroy", () => elem.off("keydown"));

          if (scope.isMenu) {
            initMenu(scope, elem, component, {Control, $actionController, $settings, $translate, getRange});
          }

          // The visible value is the label of the chosen option, or the custom range
          component.getVisibleValue = function () {
            if (scope.isCustom()) {
              return formatDateRange(getRange().from, getRange().to);
            }
            const option = (component.model.values || []).find(item => scope.isChecked(item));
            return option ? String(option.label) : "";
          };

          // A date range quick filter sends its preset as its value and its custom range as two parameters, null when empty
          if (scope.isDateRange) {
            component.getData = function () {
              const range = getRange();
              return {
                [component.address.component]: component.model.selected,
                [component.address.component + DATE_RANGE_FROM_SUFFIX]: range.from || null,
                [component.address.component + DATE_RANGE_TO_SUFFIX]: range.to || null
              };
            };
          }

          // The reload (fill) replaces the options and keeps the chosen value
          component.api.updateModelValues = function (data) {
            if ("values" in data) {
              component.model.values = data.values;
            }
            if ("selected" in data) {
              component.model.selected = Control.formatDataList(data.selected);
            }
          };

          // Every filter of the grid reloads the options, so the counts follow the data (after a maintain, a search...)
          component.listeners['grid-filter'] = scope.$on("/action/filter", function (event, action) {
            const target = action.attr("callbackTarget") || {};
            if (target.component === scope.gridId && target.view === component.address.view) {
              component.reload({silent: true, async: true});
            }
          });
        }
      };
    }
  ]);
