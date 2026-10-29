import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {createFocusTrap} from "../services/focusTrap";
import {iconTemplate} from "../services/icon";
import {getFixedOffset} from "./gridToolbar";

// Space between the Group by button and its panel, and between the panel and the edges of the window
const GAP = 8;
const MARGIN = 8;

// Grouping controls of the grid toolbar (#833): the Group by button with its panel (group-selector) and Expand all /
// Collapse all while the rows are grouped. The panel is a popover anchored to its button, as the one of the Columns:
// "No grouping" and a checkbox per column, with the level of the checked ones. Every change applies at once, on the
// client (no server call)
export const GRID_GROUP_BY_TEMPLATE = `<button ng-if="component.groupSelector" type="button" class="btn-awe btn-default grid-toolbar-button grid-group-by-button" data-testid="${TestIds.gridGroupBy}" title="{{'GRID_GROUP_BY' | translateMultiple}}" aria-haspopup="dialog" ng-attr-aria-expanded="{{groupByOpen === true}}" ng-click="toggleGroupBy()">
    ${iconTemplate("object-group")}
    <span class="button-text" translate-multiple="GRID_GROUP_BY"></span>
  </button>
  <button ng-if="component.grouped" type="button" class="btn-awe btn-default grid-toolbar-button grid-group-expand-all" data-testid="${TestIds.gridGroupExpandAll}" title="{{'GRID_GROUP_EXPAND_ALL' | translateMultiple}}" aria-label="{{'GRID_GROUP_EXPAND_ALL' | translateMultiple}}" ng-click="component.setAllGroupsExpanded(true)">
    ${iconTemplate("plus-square")}
  </button>
  <button ng-if="component.grouped" type="button" class="btn-awe btn-default grid-toolbar-button grid-group-collapse-all" data-testid="${TestIds.gridGroupCollapseAll}" title="{{'GRID_GROUP_COLLAPSE_ALL' | translateMultiple}}" aria-label="{{'GRID_GROUP_COLLAPSE_ALL' | translateMultiple}}" ng-click="component.setAllGroupsExpanded(false)">
    ${iconTemplate("minus-square")}
  </button>
  <div ng-if="groupByOpen" class="grid-group-by-popover" role="dialog" aria-label="{{'GRID_GROUP_BY' | translateMultiple}}" data-testid="${TestIds.gridGroupByPanel}" ng-attr-data-testid-owner="{{component.address.component}}">
    <h4 class="grid-group-by-title" translate-multiple="GRID_GROUP_BY"></h4>
    <button type="button" class="btn-awe btn-text grid-group-by-none" data-testid="${TestIds.gridGroupNone}" aria-pressed="{{!component.grouped}}" ng-click="clearLevels()" translate-multiple="GRID_GROUP_NONE"></button>
    <ul class="grid-group-by-list">
      <li ng-repeat="option in groupByOptions track by option.name">
        <label class="grid-group-by-option">
          <input type="checkbox" class="grid-group-by-checkbox" data-testid="${TestIds.gridGroupOption}" data-column="{{option.name}}" ng-checked="option.level > 0" ng-click="toggleLevel(option.name)"/>
          <span class="grid-group-by-label" translate-multiple="{{option.label}}"></span>
          <span ng-if="option.level" class="grid-group-by-level" ng-bind="option.level"></span>
        </label>
      </li>
    </ul>
  </div>`;

// Grouping controls of the grid toolbar
aweApplication.directive('aweGridGroupBy',
  [function () {
    return {
      restrict: 'E',
      scope: true,
      template: GRID_GROUP_BY_TEMPLATE,
      link: function (scope, element) {
        let listening = false;
        scope.groupByOpen = false;

        // Run a scope change from a DOM event, which may come inside or outside a digest
        const applyScope = fn => (scope.$root.$$phase ? fn() : scope.$apply(fn));
        const getButton = () => element.find(".grid-group-by-button")[0] || null;
        const getPanel = () => element.find(".grid-group-by-popover")[0] || null;
        const trap = createFocusTrap(getPanel);

        // Place the panel below its button, aligned to its right edge, inside the window (above the button when it does
        // not fit below and there is more room above)
        const place = () => {
          const panel = getPanel();
          const button = getButton();
          if (!panel || !button) {
            return;
          }
          const rect = button.getBoundingClientRect();
          const size = panel.getBoundingClientRect();
          const clamp = (value, max) => Math.max(MARGIN, Math.min(value, max));
          const below = rect.bottom + GAP;
          const roomBelow = window.innerHeight - MARGIN - below;
          const roomAbove = rect.top - GAP - MARGIN;
          const above = size.height > roomBelow && roomAbove > roomBelow;
          const room = Math.max(0, above ? roomAbove : roomBelow);
          panel.style.maxHeight = `${Math.round(room)}px`;
          const height = Math.min(size.height, room);
          const top = clamp(above ? rect.top - GAP - height : below, window.innerHeight - height - MARGIN);
          const left = clamp(rect.right - size.width, window.innerWidth - size.width - MARGIN);
          const offset = getFixedOffset(panel);
          panel.style.top = `${Math.round(top - offset.top)}px`;
          panel.style.left = `${Math.round(left - offset.left)}px`;
        };

        // A mouse down outside the panel (and its button, which toggles it) closes it
        const onMouseDown = event => {
          const panel = getPanel();
          const button = getButton();
          if (!panel || panel.contains(event.target) || (button && button.contains(event.target))) {
            return;
          }
          applyScope(() => scope.closeGroupBy());
        };

        // Esc closes the panel, and not the window which holds the grid
        const onKeyDown = event => {
          const panel = getPanel();
          if (event.key !== "Escape" || !panel || !(panel.contains(event.target) || event.target === document.body)) {
            return;
          }
          event.stopPropagation();
          applyScope(() => scope.closeGroupBy());
        };

        const listen = on => {
          if (listening === on) {
            return;
          }
          listening = on;
          const method = on ? "addEventListener" : "removeEventListener";
          document[method]("mousedown", onMouseDown, true);
          document[method]("keydown", onKeyDown);
          element[0][method]("keydown", onKeyDown);
          window[method]("resize", place);
          window[method]("scroll", place, true);
        };

        // Columns of the panel with their level, read again after every change
        const updateOptions = () => {
          scope.groupByOptions = scope.component.getGroupByOptions();
        };

        scope.toggleLevel = function (name) {
          scope.component.toggleGroupLevel(name);
          updateOptions();
        };

        scope.clearLevels = function () {
          scope.component.setGroupLevels([]);
          updateOptions();
        };

        scope.openGroupBy = function () {
          if (scope.groupByOpen) {
            return;
          }
          updateOptions();
          scope.groupByOpen = true;
          trap.activate();
          listen(true);
          // The panel is shown once the digest renders it
          scope.$$postDigest(() => {
            if (scope.groupByOpen && getPanel()) {
              place();
              trap.focusFirst();
            }
          });
        };

        // Close the panel and give the focus back to its button
        scope.closeGroupBy = function () {
          listen(false);
          trap.deactivate();
          scope.groupByOpen = false;
        };

        scope.toggleGroupBy = function () {
          if (scope.groupByOpen) {
            scope.closeGroupBy();
          } else {
            scope.openGroupBy();
          }
        };

        scope.$on("$destroy", () => {
          listen(false);
          trap.deactivate();
        });
      }
    };
  }]);
