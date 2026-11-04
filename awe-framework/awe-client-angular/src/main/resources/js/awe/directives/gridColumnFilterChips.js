import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";
import {iconMarkup} from "../services/icon";

const owner = `ng-attr-${TestAttributes.owner}="{{::gridId}}"`;

// Column filters in effect of a grid (#912), under its toolbar: "Filtered by", a removable chip per filtered column and
// Clear all, with the look of the filters in effect of a window (#790)
export const GRID_COLUMN_FILTER_CHIPS_TEMPLATE = `<div class="grid-column-filter-chips" role="group" aria-label="{{'SCREEN_TEXT_FILTERED_BY' | translateMultiple}}"
     data-testid="${TestIds.gridColumnFilterChips}" ${owner}>
  <span class="window-filter-chips-title">{{'SCREEN_TEXT_FILTERED_BY' | translateMultiple}}</span>
  <span class="window-filter-chip" ng-repeat="chip in chips track by chip.id" data-testid="${TestIds.gridColumnFilterChip}" ${owner} ng-attr-column-id="{{chip.id}}"><span class="window-filter-chip-text">{{chip.label}}: <b>{{chip.summary}}</b></span><button type="button" class="window-filter-chip-remove"
      data-testid="${TestIds.gridColumnFilterChipRemove}" ${owner} aria-label="{{'SCREEN_TEXT_REMOVE_FILTER' | translateMultiple}} {{chip.label}}: {{chip.summary}}"
      ng-click="remove(chip)">${iconMarkup("lucide:x")}</button></span>
  <button type="button" class="window-filter-clear-all" data-testid="${TestIds.gridColumnFilterClearAll}" ${owner} ng-click="clearAll()">{{'SCREEN_TEXT_CLEAR_ALL_FILTERS' | translateMultiple}}</button>
</div>`;

/**
 * @ngdoc directive
 * @name aweGridColumnFilterChips
 * @description Column filters in effect of a grid (#912, S18), under its toolbar: a removable chip per filtered column
 * ("<label>: <summary>", also for a hidden column) and Clear all. Removing a chip or Clear all reloads the grid and gives
 * the focus to the funnel of the column (the first funnel for Clear all). It shares the scope of its grid and is only
 * rendered while some column is filtered.
 */
aweApplication.directive('aweGridColumnFilterChips',
  [
    function () {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_COLUMN_FILTER_CHIPS_TEMPLATE,
        link: function (scope) {
          const component = scope.component;

          // The chips follow the column filters (a new list on every change) and the language
          const update = () => {
            scope.chips = component.getColumnFilterChips();
          };
          scope.$watch(() => component.columnFilters, update);
          scope.$on("languageChanged", update);

          /**
           * Give the focus to the funnel of a column (the first one without column), as its chip is gone
           * @param {string} [column] Column name
           */
          const focusFunnel = column => {
            const funnels = component.element.find(`[data-testid='${TestIds.gridColumnFilter}']`).toArray();
            const funnel = column ? funnels.find(node => node.getAttribute("column-id") === column) : funnels[0];
            if (funnel) {
              funnel.focus();
            }
          };

          /**
           * Remove the filter of a chip
           * @param {object} chip Chip
           */
          scope.remove = function (chip) {
            component.removeColumnFilter(chip.id);
            focusFunnel(chip.id);
          };

          /**
           * Remove every column filter
           */
          scope.clearAll = function () {
            component.clearColumnFilters();
            focusFunnel();
          };
        }
      };
    }
  ]);
