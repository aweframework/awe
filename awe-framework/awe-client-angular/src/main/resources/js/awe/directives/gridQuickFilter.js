import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {moveChoice} from "../services/checkboxRadio";

// Quick filters of a grid: a segmented control (one radio per option, "Label N") at the start of the grid toolbar
export const GRID_QUICK_FILTER_TEMPLATE = `<div ng-show="controller.visible" class="grid-quick-filter" role="radiogroup" aria-label="{{(controller.label || 'SCREEN_TEXT_FILTERS') | translateMultiple}}" ng-attr-criterion-id="{{::controller.id}}" data-testid="${TestIds.gridQuickFilter}" ng-attr-data-testid-owner="{{::gridId}}" ng-attr-aria-busy="{{controller.loading ? 'true' : undefined}}" ng-cloak>
  <label class="grid-quick-filter-option" ng-repeat="option in model.values track by $index" ng-attr-option-id="{{option.value === null || option.value === undefined ? '' : option.value}}" data-testid="${TestIds.gridQuickFilterOption}">
    <input type="radio" ng-attr-name="{{::radioName}}" ng-attr-value="{{option.value}}" ng-checked="isChecked(option)" ng-click="choose(option)" ng-disabled="controller.readonly" data-testid="${TestIds.criterionInput}"/>
    <span class="grid-quick-filter-segment"><span class="grid-quick-filter-label">{{option.label | translateMultiple}}</span> <span ng-if="hasCount(option)" class="grid-quick-filter-count" ng-attr-data-tone="{{option.tone || undefined}}">{{option.count}}</span></span>
  </label>
</div>`;

/**
 * Value of an option or of the criterion as text: an empty value (null or undefined) is the empty text, so the option
 * with an empty value (All) is the one of an empty criterion
 * @param {*} value Value
 * @returns {string} Value as text
 */
const asText = value => (value === null || value === undefined ? "" : String(value));

// Quick filter directive: a criterion of the screen whose options come from its initial load (value, label, count
// and tone). Its value is sent with the requests of the grid; choosing an option filters the grid, and every filter of
// the grid reloads the options, so the counts stay current
aweApplication.directive('aweGridQuickFilter',
  ['Criterion', 'Control', 'ActionController', 'AweUtilities',
    function (Criterion, Control, $actionController, Utilities) {
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

          // The chosen value: the first one of the model, as text
          const getSelected = () => asText(Utilities.asArray(component.model.selected)[0]);

          /**
           * Check if an option is the chosen one. When no option has the value of the criterion, the first one is
           * @param {Object} option Option
           * @returns {boolean} The option is the chosen one
           */
          scope.isChecked = function (option) {
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
            $actionController.addActionList([{type: "filter", target: scope.gridId}], true,
              {address: component.address, context: component.context});
          };

          // The arrow keys move the choice. Listened outside Angular: the click on the next option starts its own digest
          elem.on("keydown", "input[type=radio]", moveChoice);
          scope.$on("$destroy", () => elem.off("keydown"));

          // The visible value is the label of the chosen option
          component.getVisibleValue = function () {
            const option = (component.model.values || []).find(item => scope.isChecked(item));
            return option ? String(option.label) : "";
          };

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
