import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {iconMarkup} from "../services/icon";

// Selection bar of a grid: between the toolbar and the header while there are selected rows, with their number and the
// selection buttons of the grid (selection="true"), which leave the footer. It is a polite live region, so the number
// is announced; hidden (display: none) its buttons are out of the tab order
export const GRID_SELECTION_BAR_TEMPLATE = `<div class="grid-selection-bar" ng-class="{'is-visible': selectionCount() > 0}" aria-live="polite" data-testid="${TestIds.gridSelectionBar}" ng-attr-data-testid-owner="{{::gridId}}" ng-attr-data-count="{{selectionCount()}}">
  <span class="grid-selection-bar-icon">${iconMarkup("check-circle-o")}</span>
  <span class="grid-selection-bar-count" data-testid="${TestIds.gridSelectionCount}">{{selectionText()}}</span>
  <span class="grid-selection-bar-spacer"></span>
  <div class="grid-selection-bar-actions"></div>
</div>`;

/**
 * Identifiers of the selection buttons of a grid, in the order of the screen
 * @param {object} controller Grid controller
 * @returns {string[]} Button identifiers
 */
export const getSelectionButtons = controller => (controller?.buttonModel || [])
  .filter(button => button.selection === true || button.selection === "true")
  .map(button => button.id);

// Grid selection bar directive: it shares the scope of its grid (model, controller, gridId)
aweApplication.directive('aweGridSelectionBar',
  ['$translate',
    function ($translate) {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_SELECTION_BAR_TEMPLATE,
        link: function (scope, elem) {
          const buttons = getSelectionButtons(scope.controller);
          const actions = elem.find(".grid-selection-bar-actions");

          /**
           * Number of selected rows
           * @returns {number} Selected rows
           */
          scope.selectionCount = () => (scope.model?.selected || []).length;

          /**
           * Text of the bar: "1 selected", "2 selected"
           * @returns {string} Text
           */
          scope.selectionText = () => {
            const count = scope.selectionCount();
            return count > 0 ? $translate.instant(count === 1 ? "SCREEN_TEXT_GRID_SELECTED_ROW" : "SCREEN_TEXT_GRID_SELECTED_ROWS", {count}) : "";
          };

          // The buttons of the grid are rendered in its footer (the tree grid loads its template later): each selection
          // button is moved into the bar, in the order of the screen, as soon as it is there. The button keeps its scope
          const footerButton = id => elem.parent().children(".table-footer").find(`[button-id='${id}']`);
          const moveButtons = () => {
            buttons.forEach(id => {
              const button = footerButton(id);
              if (button.length) {
                actions.append(button);
              }
            });
            // Back in the order of the screen when they came in different digests
            buttons.forEach(id => actions.append(actions.children(`[button-id='${id}']`)));
            return actions.children().length;
          };
          if (moveButtons() < buttons.length) {
            const unwatch = scope.$watch(moveButtons, moved => {
              if (moved >= buttons.length) {
                unwatch();
              }
            });
          }
        }
      };
    }
  ]);
