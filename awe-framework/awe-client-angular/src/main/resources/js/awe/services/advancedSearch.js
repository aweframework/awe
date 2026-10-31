import {TestAttributes, TestIds} from "../data/testIds";
import {submitForm} from "./criterion";
import {iconMarkup} from "./icon";
import {launchSilentService} from "./silentService";
import "../directives/plugins/uiModal";

// Advanced search of a window (#790): the advanced block of the window is headed by its row (the Advanced filters or
// Advanced filters button (inline or side panel) with the number of advanced criteria in use, and the filters in effect as removable chips) and
// holds the advanced criteria, always mounted (collapsed with CSS below the row, or moved into a side panel), so
// dependencies, validation, defaults, print and export do not change.

// Class of the region of the advanced criteria while it is collapsed (inline)
export const ADVANCED_COLLAPSED_CLASS = "window-advanced-collapsed";

// Width of the filter panel of the design, relative to the text size and not to the root font size, which differs
// between the engines (it takes the whole width of a small screen)
const PANEL_MAX_WIDTH = "39em";

// Criteria that are a flag: their chip is their label
const FLAGS = ["checkbox", "button-checkbox"];
const RADIOS = ["radio", "button-radio"];

const owner = `ng-attr-data-testid-owner="{{::advanced.windowId}}"`;

const toggleTemplate = `<button type="button" class="window-advanced-toggle" data-testid="${TestIds.windowAdvancedToggle}" ${owner}
          ng-click="advanced.toggle()" ng-attr-aria-expanded="{{advanced.ariaExpanded()}}"
          aria-controls="{{::advanced.controls}}" ng-attr-aria-haspopup="{{::advanced.inline ? undefined : 'dialog'}}">
    ${iconMarkup("sliders")}
    <span>{{advanced.toggleText() | translateMultiple}}</span>
    <span class="window-advanced-count" data-testid="${TestIds.windowAdvancedCount}" ${owner} ng-show="advanced.count > 0">{{advanced.count}}</span>
    ${iconMarkup("lucide:chevron-down window-advanced-chevron", 'ng-if="::advanced.inline"')}${iconMarkup("lucide:panel-right", 'ng-if="::!advanced.inline"')}
  </button>`;

const chipsTemplate = `<div class="window-filter-chips" role="group" aria-label="{{'SCREEN_TEXT_FILTERED_BY' | translateMultiple}}"
     data-testid="${TestIds.windowFilterChips}" ${owner} ng-show="advanced.chips.length > 0">
  <span class="window-filter-chips-title">{{'SCREEN_TEXT_FILTERED_BY' | translateMultiple}}</span>
  <span class="window-filter-chip" ng-repeat="chip in advanced.chips track by chip.id" ng-class="{'window-filter-chip-hidden': advanced.isHidden(chip)}"
        data-testid="${TestIds.windowFilterChip}" ${owner} ng-attr-${TestAttributes.filterId}="{{chip.id}}">
    ${iconMarkup("eye-slash", 'ng-if="advanced.isHidden(chip)"')}
    <span ng-if="chip.value === null"><b>{{chip.label | translateMultiple}}</b></span>
    <span ng-if="chip.value !== null">{{chip.label | translateMultiple}}: <b>{{chip.value | translateMultiple}}</b>{{chip.more}}</span>
    <span class="sr-only" ng-if="advanced.isHidden(chip)">({{'SCREEN_TEXT_HIDDEN_ADVANCED_FILTER' | translateMultiple}})</span>
    <button type="button" class="window-filter-chip-remove" data-testid="${TestIds.windowFilterChipRemove}" ${owner}
            aria-label="{{'SCREEN_TEXT_REMOVE_FILTER' | translateMultiple}} {{chip.label | translateMultiple}}" ng-click="advanced.remove(chip)">
      ${iconMarkup("times")}
    </button>
  </span>
  <button type="button" class="window-filter-clear-all" data-testid="${TestIds.windowFilterClearAll}" ${owner} ng-click="advanced.clearAll()">
    {{'SCREEN_TEXT_CLEAR_ALL_FILTERS' | translateMultiple}}
  </button>
</div>`;

// The block: its row first, then the region of its children (the advanced criteria and their layout tags)
export const advancedTemplate = `<div class="window-advanced">
  <div class="window-advanced-row" ng-if="::advanced">
    ${toggleTemplate}
    ${chipsTemplate}
  </div>
  <div class="window-advanced-region" ng-attr-id="{{::advanced.regionId}}" ng-transclude
       ng-class="{'${ADVANCED_COLLAPSED_CLASS}': advanced.inline && !advanced.expanded}"></div>
</div>`;

const panelTemplate = `<div class="window-advanced-panel-host">
  <div class="modal fade side-panel side-panel-right window-advanced-panel" ui-modal trap-tab on-close="advanced.panelClosed()"
       id="{{::advanced.panelId}}" role="dialog" aria-modal="true" aria-labelledby="{{::advanced.panelId}}-title"
       data-testid="${TestIds.windowAdvancedPanel}" ${owner}>
    <div class="modal-dialog" ng-style="::{'max-width': advanced.maxWidth}">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" aria-label="{{'BUTTON_CANCEL' | translateMultiple}}">&times;</button>
          <h4 class="modal-title" id="{{::advanced.panelId}}-title">{{::advanced.title | translateMultiple}}</h4>
        </div>
        <div class="side-panel-body window-advanced-panel-body"></div>
        <div class="modal-footer">
          <button type="button" class="btn btn-awe window-advanced-reset" data-testid="${TestIds.windowAdvancedReset}" ${owner} ng-click="advanced.reset()">
            ${iconMarkup("rotate-left")}<span>{{'SCREEN_TEXT_RESET_DEFAULTS' | translateMultiple}}</span>
          </button>
          <button type="button" class="btn btn-awe" data-dismiss="modal" data-testid="${TestIds.windowAdvancedCancel}" ${owner}>
            {{'BUTTON_CANCEL' | translateMultiple}}
          </button>
          <button type="button" class="btn btn-awe btn-primary" data-testid="${TestIds.windowAdvancedApply}" ${owner} ng-click="advanced.apply()">
            {{'SCREEN_TEXT_SHOW_RESULTS' | translateMultiple}}
          </button>
        </div>
      </div>
    </div>
  </div>
</div>`;

/**
 * Initialize the advanced block of a window. Out of a window with an advanced search it only holds its children
 * @param {Object} scope Block scope
 * @param {Object} elem Block element
 * @param {Object} windowController Controller of the window that holds the block
 * @param {{$compile: Function, Control: Object, Utilities: Object, $actionController: Object, $settings: Object, Storage: Object}} services Services
 */
export function initAdvancedBlock(scope, elem, windowController, {$compile, Control, Utilities, $actionController, $settings, Storage}) {
  const view = scope.$parent.view;
  const search = windowController ? windowController.getAdvancedSearch(view) : null;
  if (!search) {
    scope.advanced = null;
    return;
  }
  const {windowId, advancedIds, searchIds} = search;
  const inline = search.mode !== "side-panel";

  const addressOf = id => ({view, component: id});
  const controllerOf = id => Control.getAddressController(addressOf(id)) || {};
  // A radio button shares the model of its group
  const modelAddressOf = id => {
    const criterionController = controllerOf(id);
    return RADIOS.includes(criterionController.component) && criterionController.group ? addressOf(criterionController.group) : addressOf(id);
  };
  const modelOf = id => Control.getAddressModel(modelAddressOf(id)) || {};

  // Values of a criterion without blanks, as strings and sorted, so that equal values compare equal
  const valuesOf = value => Utilities.asArray(value).filter(item => !Utilities.isEmpty(item)).map(String)
    .sort((a, b) => a.localeCompare(b));
  const checked = value => valuesOf(value).some(item => item !== "0");
  const isDefault = id => {
    const model = modelOf(id);
    if (FLAGS.includes(controllerOf(id).component)) {
      return checked(model.selected) === checked(model.defaultValues);
    }
    return _.isEqual(valuesOf(model.selected), valuesOf(model.defaultValues));
  };

  /**
   * Chip of a criterion in use: its label and its value (the label of the option for a list, the first value and the
   * number of the other ones for a multiple one, no value for a flag)
   * @param {string} id Criterion id
   * @returns {{id: string, label: string, value: string|null, more: string, advanced: boolean}} Chip
   */
  const chipOf = id => {
    const criterionController = controllerOf(id);
    const model = modelOf(id);
    const chip = {id, label: criterionController.label || id, value: null, more: "", advanced: advancedIds.includes(id)};
    if (!FLAGS.includes(criterionController.component) && !RADIOS.includes(criterionController.component)) {
      const values = Utilities.asArray(model.selected).filter(item => !Utilities.isEmpty(item));
      if (values.length > 0) {
        const option = (model.values || []).find(item => String(item.value) === String(values[0]));
        chip.value = String(option && !Utilities.isEmpty(option.label) ? option.label : values[0]);
        chip.more = values.length > 1 ? ` +${values.length - 1}` : "";
      }
    }
    return chip;
  };

  // The radio button of a group that holds its selected value
  const isSelectedRadio = id => {
    const own = Control.getAddressModel(addressOf(id)) || {};
    const value = own.values && own.values.length > 0 ? own.values[0].value : null;
    return String(modelOf(id).selected) === String(value);
  };

  const chipsInUse = () => searchIds
    .filter(id => !isDefault(id))
    .filter(id => !RADIOS.includes(controllerOf(id).component) || isSelectedRadio(id))
    .map(chipOf);

  /**
   * Remember for the user whether the inline block is open, silently: a failed save changes nothing on the screen
   * @param {boolean} open The block is open
   */
  const saveState = open => {
    const screen = (Storage.get("screen") || {})[view] || {};
    launchSilentService({$actionController, $settings}, "saveWindowAdvancedState", {
      advancedStateScreen: screen.name,
      advancedStateWindow: windowId,
      advancedStateOpen: open ? "1" : "0"
    }, () => undefined);
  };

  // The region of the children of the block, in the window or moved into the panel
  const region = elem.children(".window-advanced-region");
  let panel = null;

  // Restore criteria of the window to their default values, as the restore action does (with their dependencies)
  const restore = ids => windowController.element.find("[criterion-id]").add(region.find("[criterion-id]"))
    .filter((index, node) => !ids || ids.includes(node.getAttribute("criterion-id")))
    .each((index, node) => {
      const criterionScope = angular.element(node).isolateScope();
      if (criterionScope) {
        criterionScope.$broadcast("restore-scope", view);
      }
    });

  const advanced = {
    windowId,
    inline,
    // Inline, it starts as the user left it; the side panel always starts closed
    expanded: inline && search.open,
    opened: false,
    applying: false,
    count: 0,
    chips: [],
    snapshot: {},
    title: search.label,
    maxWidth: PANEL_MAX_WIDTH,
    regionId: `${windowId}-advanced-region`,
    panelId: `${windowId}-advanced-panel`,
    controls: inline ? `${windowId}-advanced-region` : `${windowId}-advanced-panel`,
    toggleText() {
      // The side panel opens with the same Advanced filters button as the inline mode
      return inline && advanced.expanded ? "SCREEN_TEXT_HIDE_ADVANCED_FILTERS" : "SCREEN_TEXT_ADVANCED_FILTERS";
    },
    ariaExpanded() {
      return inline ? String(advanced.expanded) : undefined;
    },
    isHidden(chip) {
      return chip.advanced && !(inline ? advanced.expanded : advanced.opened);
    },
    toggle() {
      if (inline) {
        advanced.expanded = !advanced.expanded;
        saveState(advanced.expanded);
      } else {
        openPanel();
      }
    },
    remove(chip) {
      restore([chip.id]);
    },
    clearAll() {
      restore(null);
    },
    reset() {
      restore(advancedIds);
    },
    apply() {
      advanced.applying = true;
      showPanel(false);
    },
    // The panel is closed (Show results, Cancel, Escape or its close button): only Show results keeps the values
    panelClosed() {
      advanced.opened = false;
      if (advanced.applying) {
        advanced.applying = false;
        submitForm(elem);
      } else {
        Object.entries(advanced.snapshot).forEach(([id, selected]) => {
          if (!_.isEqual(modelOf(id).selected, selected)) {
            Control.changeModelAttribute(modelAddressOf(id), {selected: _.cloneDeep(selected)}, true);
          }
        });
      }
      advanced.snapshot = {};
      elem.find(`[data-testid="${TestIds.windowAdvancedToggle}"]`).focus();
    }
  };
  scope.advanced = advanced;
  // Enter in a criterion of the window (or of its panel) launches the submit button of the window
  windowController.advancedBlock = advanced;

  /**
   * Show or hide the panel
   * @param {boolean} show Show it
   */
  function showPanel(show) {
    const modalScope = panel.find(".modal").isolateScope();
    modalScope.$broadcast("modalChange", show);
  }

  /**
   * Open the panel, remembering the values of the advanced criteria for Cancel
   */
  function openPanel() {
    if (advanced.opened) {
      return;
    }
    advanced.opened = true;
    advanced.snapshot = {};
    advancedIds.forEach(id => {
      advanced.snapshot[id] = _.cloneDeep(modelOf(id).selected);
    });
    showPanel(true);
  }

  // The panel lives with the dialogs of the screen (in its form), out of the window, and holds the region (compiled)
  if (!inline) {
    panel = $compile(panelTemplate)(scope);
    panel.find(".window-advanced-panel-body").append(region);
    panel.data("$aweWindowController", windowController);
    const form = elem.closest("form");
    (form.length > 0 ? form : $(document.body)).append(panel);
  }

  scope.$on("$destroy", () => {
    if (panel) {
      panel.remove();
    }
    if (windowController.advancedBlock === advanced) {
      windowController.advancedBlock = null;
    }
  });

  // The count and the chips follow the values of the criteria
  scope.$watch(() => searchIds.map(id => {
    const model = modelOf(id);
    return [model.selected, model.defaultValues, (model.values || []).length];
  }), () => {
    advanced.count = advancedIds.filter(id => !isDefault(id)).length;
    advanced.chips = chipsInUse();
  }, true);

  // An invalid criterion of the block is shown, so its error is visible
  scope.$on("criterion-validity", (event, {invalid}) => {
    if (invalid) {
      if (inline) {
        advanced.expanded = true;
      } else {
        openPanel();
      }
      scope.$applyAsync();
    }
  });
}
