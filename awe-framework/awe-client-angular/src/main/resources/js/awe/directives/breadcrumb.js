import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {collapseLevels} from "awe-client-react/utilities/breadcrumbs";

const template = `<nav aria-label="{{'SCREEN_TEXT_BREADCRUMB' | translateMultiple}}">
  <ul class="breadcrumb breadcrumb-page" data-testid="${TestIds.breadcrumb}">
    <div class="breadcrumb-label">
      <button type="button" class="window-crumb" data-testid="${TestIds.breadcrumbHome}" aria-label="{{'BUTTON_HOME' | translateMultiple}}"
              ng-click="goHome()" ng-transclude></button>
    </div>
    <li ng-repeat="level in items track by $index" ng-class="{active: $last}">
      <span ng-if="$last" aria-current="page" data-testid="${TestIds.breadcrumbLevel}" data-active="true" translate-multiple="{{level.label}}"></span>
      <button ng-if="level.collapsed" type="button" class="window-crumb window-crumb-ellipsis" data-testid="${TestIds.breadcrumbEllipsis}"
              aria-expanded="{{expanded}}" aria-label="{{'SCREEN_TEXT_BREADCRUMB_EXPAND' | translateMultiple}}" ng-click="toggle()">…</button>
      <button ng-if="!$last && !level.collapsed && isLink(level)" type="button" class="window-crumb" data-testid="${TestIds.breadcrumbLevel}"
              ng-attr-name="{{level.name}}" ng-attr-path="{{level.path}}" ng-click="follow(level)" translate-multiple="{{level.label}}"></button>
      <span ng-if="!$last && !level.collapsed && !isLink(level)" translate-multiple="{{level.label}}"></span>
    </li>
  </ul>
</nav>`;

/**
 * Levels that the server wrote in the attribute: none when they are not a JSON list, so the header still renders
 * @param {string} value Attribute value
 * @returns {object[]} Levels
 */
function parseLevels(value) {
  try {
    const levels = JSON.parse(value || "[]");
    return Array.isArray(levels) ? levels : [];
  } catch {
    return [];
  }
}

// Breadcrumb of the window header: the screens the user came from, the levels of the menu above the screen (the server
// sends them) and the screen. A level that goes somewhere is a button: a screen goes back to it, an option launches as
// the menu does, a group opens the menu at it. More than four levels collapse into an ellipsis that expands them
aweApplication.directive('aweBreadcrumb',
  ['Breadcrumb',
    function ($breadcrumb) {
      return {
        restrict: 'E',
        // The home icon comes from the template of the window
        transclude: true,
        template,
        scope: {},
        // The levels, the option and the title are read as the template wrote them: Angular would interpolate the braces of a label
        compile: function (tElement, tAttrs) {
          const own = parseLevels(tAttrs.levels);
          const title = tAttrs.pageTitle;
          // The option of the screen: the screens opened from it know whether they are on its menu path
          const option = tAttrs.pageOption;
          return function link(scope) {
            const levels = $breadcrumb.getLevels(own, title);
            $breadcrumb.show(levels, option);

            // The hidden levels show until the screen changes (the directive is created again with it)
            scope.expanded = false;
            scope.items = collapseLevels(levels, scope.expanded);
            scope.toggle = () => {
              scope.expanded = !scope.expanded;
              scope.items = collapseLevels(levels, scope.expanded);
            };
            scope.isLink = $breadcrumb.isLink;
            scope.follow = $breadcrumb.follow;
            scope.goHome = $breadcrumb.goHome;
          };
        }
      };
    }]);
