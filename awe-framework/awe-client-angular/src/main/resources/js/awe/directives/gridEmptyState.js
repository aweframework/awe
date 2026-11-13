import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {getIconTemplate} from "../services/component";

const GRID_EMPTY_STATE_TEMPLATE = `<div class="grid-empty-state" data-testid="${TestIds.gridEmpty}" ng-cloak>
  ${getIconTemplate("grid-empty-icon", "span", "emptyState.icon")}
  <div class="grid-empty-title" data-testid="${TestIds.gridEmptyTitle}" translate-multiple="{{emptyState.title}}"></div>
  <div class="grid-empty-text" data-testid="${TestIds.gridEmptyText}" translate-multiple="{{emptyState.text}}"></div>
  <button ng-if="emptyState.button" type="button" class="btn-awe btn-default grid-empty-action" data-testid="${TestIds.gridEmptyAction}" ng-click="onEmptyAction()">
    ${getIconTemplate("", "i", "emptyState.buttonIcon")}
    <span class="button-text" translate-multiple="{{emptyState.button.label}}"></span>
  </button>
</div>`;

/**
 * Define the empty state of a grid or a tree grid (empty-icon, empty-title, empty-text and empty-action) in its scope
 * @param {object} scope Grid scope
 * @param {object} component Grid component
 * @param {object} $utilities Awe utilities
 */
export function initEmptyState(scope, component, $utilities) {
  scope.emptyState = {
    icon: $utilities.extractIcon(component.controller.emptyIcon || "inbox"),
    title: component.controller.emptyTitle || "SCREEN_TEXT_GRID_EMPTY_TITLE",
    text: component.controller.emptyText || "SCREEN_TEXT_GRID_EMPTY_TEXT",
    button: (component.controller.buttonModel || []).find(button => button.id === component.controller.emptyAction)
  };
  scope.emptyState.buttonIcon = $utilities.extractIcon(scope.emptyState.button?.icon);
  scope.onEmptyAction = function () {
    // Runs the actions of the grid button like any other click on it, once the digest of this click is over
    // (the button starts a digest of its own)
    $utilities.timeout(() => document.getElementById(component.controller.emptyAction)?.click());
  };
}

/**
 * Empty state of a grid without rows: the icon, the title, the text and the action defined by initEmptyState
 * @ngdoc directive
 * @name aweGridEmptyState
 */
aweApplication.directive('aweGridEmptyState', [() => ({
  restrict: 'E',
  replace: true,
  template: GRID_EMPTY_STATE_TEMPLATE
})]);
