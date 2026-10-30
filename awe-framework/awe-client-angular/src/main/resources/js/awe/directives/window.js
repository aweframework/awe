import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {iconMarkup} from "../services/icon";
import {TestIds} from "../data/testIds";

const template = `<div data-testid="${TestIds.window}" ng-attr-data-testid-owner="{{::windowId}}" ng-attr-data-maximized="{{iconMaximized === true}}" class="panel panel-awe {{::panelClass + ' panel-' + size}} expandible-vertical" ng-class="{'maximized': maximized, 'maximizing': maximizing, 'resizing resizeTarget': panelResizing, 'expand': isExpandible || maximized}" ng-cloak>
  <div ng-show="::panelTitle" class="awe-panel-heading panel-heading" ng-cloak>
    ${getIconTemplate("panel-title-icon")}
    <span translate-multiple="{{::panelTitle}}"></span>
    <div ng-if="::maximize" class="btn-group pull-right">
      <button role="button" type="button" class="maximize-button" data-testid="${TestIds.windowMaximizeButton}" aria-hidden="true" ng-click="togglePanel()" title="{{togglePanelText| translateMultiple}}">
        <i>${iconMarkup("compress", 'ng-if="iconMaximized"')}${iconMarkup("expand", 'ng-if="!iconMaximized"')}</i>
      </button>
    </div>
  </div>
  <div ng-transclude class="awe-panel-content panel-content maximize-content expandible-{{::expandDirection}}" ng-class="{'expand': isExpandible || maximized}" ng-cloak></div>
</div>`;

/**
 * What the advanced block of a window needs from it (#790): its advanced search and its submit button
 * @param {Object} $element Window element
 * @param {Object} $attrs Window attributes
 * @param {Object} Control Control service
 * @constructor
 */
function WindowController($element, $attrs, Control) {
  this.element = $element;
  // The advanced block of the window, when it holds one
  this.advancedBlock = null;

  /**
   * Advanced search of the window: null without an advanced block
   * @param {string} view View of the window
   * @returns {{windowId: string, label: string, mode: string, advancedIds: string[], searchIds: string[]}|null} Search
   */
  this.getAdvancedSearch = view => {
    const controller = Control.getAddressController({view, component: $attrs.windowId}) || {};
    if (!controller.advancedCriteria) {
      return null;
    }
    return {
      windowId: $attrs.windowId,
      label: controller.label || "",
      mode: controller.advancedMode,
      advancedIds: controller.advancedCriteria,
      searchIds: controller.searchCriteria || []
    };
  };

  /**
   * Launch the submit button of the window
   * @returns {boolean} The window has a submit button
   */
  this.submit = () => {
    const own = $element.find(":submit:visible");
    if (own.length > 0) {
      $(own[0]).scope().onClick();
    }
    return own.length > 0;
  };
}

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
        controller: ['$element', '$attrs', 'Control', WindowController],
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
