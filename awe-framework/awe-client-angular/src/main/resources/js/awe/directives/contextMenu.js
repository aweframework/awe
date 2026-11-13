import {aweApplication} from "../awe";
import "../services/contextMenu";
import {TestIds} from "../data/testIds";

const template =
`<ul ng-if="::contextMenu" ng-show="contextMenu.isVisible()" class="context-menu dropdown-menu ng-hide" role="menu" data-testid="${TestIds.contextMenu}" ng-cloak>
  <awe-context-option ng-repeat="option in contextMenuData track by option.id" option-id="{{::option.id}}" option="option"></awe-context-option>
</ul>`;

// Context menu directive
aweApplication.directive('aweContextMenu',
  ['ServerData', 'ContextMenu', 'AweUtilities',
    function (serverData, ContextMenu, Utilities) {
      return {
        restrict: 'E',
        replace: true,
        template: template,
        link: function (scope, elem, attrs) {
          // Components open their menu with the right click on their root (grids and charts have their own triggers)
          if (attrs.openOnRightClick !== undefined) {
            bindRightClick(scope, elem);
          }

          // Watch for context menu
          let removeComponentWatch = scope.$watch("contextMenuData", initContextMenu);

          /**
           * Open the context menu with the right click on the component root
           * @param {Scope} scope Component scope
           * @param {jQuery} elem Context menu element, placed inside the component root
           */
          function bindRightClick(scope, elem) {
            const root = elem.parent();

            const onContextMenu = function (event) {
              if (!scope.contextMenu || !scope.contextMenu.hasVisibleOptions() || $(event.target).closest(".context-menu").length > 0) {
                return;
              }
              Utilities.stopPropagation(event, true);
              scope.$apply(function () {
                scope.contextMenu.show(event);
              });
            };
            root.on("contextmenu", onContextMenu);
            scope.$on("$destroy", function () {
              root.off("contextmenu", onContextMenu);
            });
          }

          /**
           * Initialize context menu
           * @param contextMenuList Context menu data
           */
          function initContextMenu(contextMenuList) {
            if (contextMenuList && contextMenuList.length > 0) {
              scope.contextMenu = new ContextMenu(scope, elem, contextMenuList);
              scope.contextMenu.init();
            }
            removeComponentWatch();
          }
        }
      };
    }
  ]);
