import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";

const template = `<div class="panel panel-awe {{::panelClass + ' panel-' + size}} expandible-vertical" ng-class="{'maximized': maximized, 'maximizing': maximizing, 'resizing resizeTarget': panelResizing, 'expand': isExpandible || maximized}" ng-cloak>
  <div ng-show="::panelTitle" class="awe-panel-heading panel-heading" ng-cloak>
    ${getIconTemplate("panel-title-icon")}
    <span translate-multiple="{{::panelTitle}}"></span>
    <div ng-if="::maximize" class="btn-group pull-right">
      <button role="button" type="button" class="maximize-button" aria-hidden="true" ng-click="togglePanel()" title="{{togglePanelText| translateMultiple}}">
        <i class="fa {{iconMaximized ? 'fa-compress' : 'fa-expand'}}"></i>
      </button>
    </div>
  </div>
  <div ng-transclude class="awe-panel-content panel-content maximize-content expandible-{{::expandDirection}}" ng-class="{'expand': isExpandible || maximized}" ng-cloak></div>
</div>`;


// Window directive
aweApplication.directive('aweWindow',
  ['ServerData', 'Maximize', 'Component',
    function (ServerData, Maximize, Component) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          windowId: '@'
        },
        link: function (scope, elem) {
          // Init as component
          let  component = new Component(scope, scope.windowId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          if (component.controller.maximize) {
            Maximize.initMaximize(component.scope, elem);
          }

          // Controller variables
          component.scope.isExpandible = component.controller?.style ? component.controller.style.indexOf("expand") !== -1 : false;
          component.scope.expandDirection = component.controller?.expand || "vertical";
          component.scope.panelClass = component.controller?.style || "";
          component.scope.panelTitle = component.controller?.label || null;
        }
      };
    }
  ]);
