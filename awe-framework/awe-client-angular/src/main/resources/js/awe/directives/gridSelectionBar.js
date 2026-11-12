import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {iconMarkup} from "../services/icon";

// Selection bar of a grid: while there are selected rows it takes the band of the toolbar (rendered by the toolbar, over
// its search, quick filters, saved searches and group by, with its height) or, in a grid without toolbar, the row of the
// column headers (placement="header", leaving the select-all checkbox out of it), so the rows never move. It shows the
// clear button, the number of selected rows (a polite live region), the filter the selection was made with and the
// selection buttons of the grid (selection="true"), which leave the footer. Hidden (visibility), its buttons are out of
// the tab order. The visibility follows an attribute, not a class: ngAnimate would apply a class a frame later, and the
// focus goes back to the toolbar as soon as the selection is cleared
export const GRID_SELECTION_BAR_TEMPLATE = `<div class="grid-selection-bar" role="region" aria-label="{{'SCREEN_TEXT_SELECTED' | translateMultiple}}" ng-attr-data-selected="{{selectionCount() > 0}}" data-testid="${TestIds.gridSelectionBar}" ng-attr-data-testid-owner="{{::gridId}}" ng-attr-data-count="{{selectionCount()}}">
  <button type="button" class="grid-selection-bar-clear" data-testid="${TestIds.gridSelectionClear}" aria-label="{{clearText()}}" title="{{clearText()}}" ng-click="clearSelection()">${iconMarkup("lucide:x")}</button>
  <span class="grid-selection-bar-icon">${iconMarkup("check-circle-o")}</span>
  <span class="grid-selection-bar-count" aria-live="polite" data-testid="${TestIds.gridSelectionCount}">{{selectionText()}}</span>
  <span class="grid-selection-bar-context" ng-if="selectionCount() > 0 && (contextLabel() || contextSearch())">${iconMarkup("lucide:funnel")}<span class="sr-only">{{'SCREEN_TEXT_FILTERED_BY' | translateMultiple}} </span><span class="grid-selection-bar-context-label" ng-if="contextLabel()">{{contextLabel()}}</span><span class="grid-selection-bar-context-search" ng-if="contextSearch()">{{contextSearch()}}</span></span>
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

/**
 * First value of a criterion as text: an empty value (null, undefined or an empty list) is the empty text
 * @param {*} value Value
 * @returns {string} Text
 */
const asText = value => {
  const first = Array.isArray(value) ? value[0] : value;
  return first === null || first === undefined ? "" : String(first);
};

/**
 * Box of the row of the column headers of a grid, relative to the grid, without its select-all checkbox
 * @param {HTMLElement} grid Grid element
 * @returns {{top: number, left: number, width: number, height: number}|null} Box, null while the header is not drawn
 */
const getHeaderBox = grid => {
  const rects = [...grid.querySelectorAll(".grid-node .ui-grid-header-canvas")]
    .map(node => node.getBoundingClientRect())
    .filter(rect => rect.width > 0 && rect.height > 0);
  if (!rects.length) {
    return null;
  }
  const box = grid.getBoundingClientRect();
  const checkbox = grid.querySelector(".grid-node .ui-grid-header-checkbox");
  const top = Math.min(...rects.map(rect => rect.top));
  const bottom = Math.max(...rects.map(rect => rect.bottom));
  const right = Math.max(...rects.map(rect => rect.right));
  const left = Math.max(Math.min(...rects.map(rect => rect.left)), checkbox ? checkbox.getBoundingClientRect().right : -Infinity);
  return {top: top - box.top, left: left - box.left, width: Math.max(right - left, 0), height: bottom - top};
};

// Grid selection bar directive: it shares the scope of its grid (model, controller, component, gridId)
aweApplication.directive('aweGridSelectionBar',
  ['$translate', '$filter', 'Control', 'AweUtilities',
    function ($translate, $filter, Control, Utilities) {
      return {
        restrict: 'E',
        replace: true,
        template: GRID_SELECTION_BAR_TEMPLATE,
        link: function (scope, elem, attrs) {
          const buttons = getSelectionButtons(scope.controller);
          const actions = elem.find(".grid-selection-bar-actions");
          // The grid holds the bar in its toolbar or over its header
          const gridElement = () => elem.closest(".grid");
          const translate = $filter("translateMultiple");
          const criterionModel = id => Control.getAddressModel({view: scope.component.address.view, component: id});

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

          /**
           * Name and tooltip of the clear button: "Clear selection (Esc)"
           * @returns {string} Text
           */
          scope.clearText = () => `${$translate.instant("SCREEN_TEXT_GRID_CLEAR_SELECTION")} (Esc)`;

          /**
           * Label of the active quick filter of the grid, none for the option with an empty value (all)
           * @returns {string} Label
           */
          scope.contextLabel = () => {
            const id = scope.controller?.quickFilter;
            if (!id) {
              return "";
            }
            const model = criterionModel(id);
            const selected = asText(model.selected);
            const option = selected === "" ? null : (model.values || []).find(item => asText(item.value) === selected);
            return option ? translate(String(option.label)) : "";
          };

          /**
           * Text of the search of the grid
           * @returns {string} Search text
           */
          scope.contextSearch = () => {
            const id = scope.controller?.search;
            return id ? asText(criterionModel(id).selected).trim() : "";
          };

          // After clearing, the focus goes to the search of the grid, or to its select-all checkbox. The toolbar shows
          // its search again once the digest has run
          const focusAfterClear = () => {
            const grid = gridElement();
            const target = grid.find(`[data-testid='${TestIds.gridSearch}'] input`).first();
            const fallback = grid.find(".grid-node .ui-grid-header-checkbox input").first();
            (target.length ? target : fallback).trigger("focus");
          };

          /**
           * Unselect every row and give the focus back
           */
          scope.clearSelection = () => {
            scope.component.unselectRows();
            Utilities.timeout(focusAfterClear, 0, false);
          };

          // Esc in the grid clears the selection, one row or many; the row being edited keeps Esc for its edition, and
          // the toolbar popovers and the search stop it for themselves. The key does not reach the window of the grid
          const onKeyDown = event => {
            const editing = scope.component?.editable && scope.component?.isEditing;
            if (event.key !== "Escape" || event.defaultPrevented || editing || scope.selectionCount() === 0) {
              return;
            }
            event.preventDefault();
            event.stopPropagation();
            scope.$apply(scope.clearSelection);
          };
          const grid = gridElement()[0];
          grid?.addEventListener("keydown", onKeyDown);

          // The buttons of the grid are rendered in its footer (the tree grid loads its template later): each selection
          // button is moved into the bar, in the order of the screen, as soon as it is there. The button keeps its scope
          const footerButton = id => gridElement().children(".table-footer").find(`[button-id='${id}']`);
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

          // Over the header (grid without toolbar): the bar takes the box of the row of the column headers
          let observer = null;
          const place = () => {
            const box = grid && scope.selectionCount() > 0 ? getHeaderBox(grid) : null;
            if (box) {
              elem.css({top: box.top, left: box.left, width: box.width, height: box.height});
            }
          };
          if (attrs.placement === "header") {
            elem.addClass("grid-selection-bar-header");
            scope.$watch(() => scope.selectionCount() > 0, place);
            window.addEventListener("resize", place);
            if (typeof window.ResizeObserver === "function" && grid) {
              observer = new window.ResizeObserver(place);
              observer.observe(grid);
            }
          }

          // When the bar leaves (the toolbar is shown or hidden), its buttons go back to the footer, so the other bar
          // takes them
          scope.$on("$destroy", () => {
            grid?.removeEventListener("keydown", onKeyDown);
            window.removeEventListener("resize", place);
            observer?.disconnect();
            const footer = gridElement().children(".table-footer").find("[ng-transclude]").first();
            if (footer.length) {
              footer.append(actions.children());
            }
          });
        }
      };
    }
  ]);
