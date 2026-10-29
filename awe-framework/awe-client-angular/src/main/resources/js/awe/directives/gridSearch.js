import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {iconMarkup} from "../services/icon";

// Pause after the last key before the grid is filtered
export const GRID_SEARCH_DELAY = 300;

// Search of a grid: a text field with the search icon (and a clear button when it has text) at the start of the toolbar
export const GRID_SEARCH_TEMPLATE = `<div ng-show="controller.visible" class="grid-search" role="search" ng-attr-criterion-id="{{::controller.id}}" data-testid="${TestIds.gridSearch}" ng-attr-data-testid-owner="{{::gridId}}" ng-cloak>
  <span class="grid-search-icon">${iconMarkup("search")}</span>
  <input type="search" ng-model="model.selected" ng-change="change()" ng-keydown="keyDown($event)" ng-disabled="controller.readonly" autocomplete="off" ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" placeholder="{{controller.placeholder | translateMultiple}}" aria-label="{{(controller.label || 'BUTTON_SEARCH') | translateMultiple}}" data-testid="${TestIds.criterionInput}"/>
  <button type="button" class="grid-search-clear" ng-if="hasText() && !controller.readonly" ng-click="clear()" aria-label="{{'BUTTON_CLEAR' | translateMultiple}}" title="{{'BUTTON_CLEAR' | translateMultiple}}" data-testid="${TestIds.gridSearchClear}">${iconMarkup("times")}</button>
</div>`;

/**
 * Text of the search: an empty value (null, undefined or an empty list) is the empty text
 * @param {*} value Value
 * @returns {string} Text
 */
const asText = value => {
  const first = Array.isArray(value) ? value[0] : value;
  return first === null || first === undefined ? "" : String(first);
};

// Grid search directive: a text criterion of the screen with no query of its own. Its value is sent with the requests
// of the grid; typing filters the grid after a pause, Enter at once, and the clear button or Escape empty it
aweApplication.directive('aweGridSearch',
  ['Criterion', 'ActionController', '$timeout',
    function (Criterion, $actionController, $timeout) {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_SEARCH_TEMPLATE,
        scope: {
          criterionId: '@searchId',
          gridId: '@'
        },
        link: function (scope, elem) {
          const component = new Criterion(scope, scope.criterionId, elem);
          if (!component.asCriterion()) {
            return;
          }
          component.model.selected = asText(component.model.selected);
          // Text the grid was last filtered with: the grid is not filtered again for the same text
          let filtered = component.model.selected;
          let pending = null;

          const cancelPending = () => {
            if (pending) {
              $timeout.cancel(pending);
              pending = null;
            }
          };

          // Filter the grid with the current text, unless it is already filtered with it
          const filter = () => {
            cancelPending();
            const text = asText(component.model.selected);
            if (text === filtered) {
              return;
            }
            filtered = text;
            $actionController.addActionList([{type: "filter", target: scope.gridId}], true,
              {address: component.address, context: component.context});
          };

          /**
           * Check if the field has text
           * @returns {boolean} The field has text
           */
          scope.hasText = () => asText(component.model.selected) !== "";

          // Typing: the value changes now, the grid is filtered after a pause
          scope.change = function () {
            component.modelChange();
            cancelPending();
            pending = $timeout(filter, GRID_SEARCH_DELAY);
          };

          /**
           * Empty the field and filter the grid at once
           */
          scope.clear = function () {
            component.model.selected = "";
            component.modelChange();
            filter();
            elem.find("input").trigger("focus");
          };

          /**
           * Enter filters the grid at once (and does not submit the screen); Escape empties the field
           * @param {Event} event Key down event
           */
          scope.keyDown = function (event) {
            if (event.key === "Enter") {
              event.preventDefault();
              event.stopPropagation();
              filter();
            } else if (event.key === "Escape" && scope.hasText()) {
              event.preventDefault();
              event.stopPropagation();
              scope.clear();
            }
          };

          // Any filter of the grid (a button, a reset of the screen...) sends the current text: it is the filtered one
          component.listeners['grid-filter'] = scope.$on("/action/filter", function (event, action) {
            const target = action.attr("callbackTarget") || {};
            if (target.component === scope.gridId && target.view === component.address.view) {
              filtered = asText(component.model.selected);
            }
          });

          scope.$on("$destroy", cancelPending);
        }
      };
    }
  ]);
