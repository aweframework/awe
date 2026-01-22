import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";

const template = `<li ng-show="controller.visible" ng-attr-id="{{::controller.id}}" title="{{(model.values[0].title || controller.title) | translateMultiple}}" class="avatar nav-icon-btn {{::controller.style}}" ng-class="::{'dropdown': controller.hasChildren}" ui-dependency="dependencies" ng-cloak>
  <a ng-click="onClick()" ng-class="::{'dropdown-toggle': controller.hasChildren}" ng-attr-data-toggle="{{controller.hasChildren ? 'dropdown' : ''}}">
    <img  ng-if="model.values[0].image || controller.image" class="avatar-image" ng-src="{{getContextPath()}}{{model.values[0].image || controller.image}}" ng-alt="{{(model.values[0].label || controller.text || controller.title || controller.label) | translateMultiple}}"/>
    <span ng-if="!(model.values[0].image || controller.image) && (model.values[0].icon || controller.icon)" class="avatar-icon">${getIconTemplate("nav-icon")}</span>
    <span ng-if="model.values[0].unit || controller.unit" class="label" translate-multiple="{{model.values[0].unit || controller.unit}}"></span>
    <span ng-if="controller.showLabel && (model.values[0].label || controller.text || controller.title || controller.label)" class="avatar-text" translate-multiple="{{model.values[0].label || controller.text || controller.title || controller.label}}"></span>
  </a>
  <ul ng-if="::controller.hasChildren" class="dropdown-menu {{::controller.dropdownStyle}}" ng-transclude></ul>
</li>`;

// Avatar directive
aweApplication.directive('aweAvatar', ['ServerData', 'Component', 'ActionController', 'AweUtilities',
  /**
   * Info directive
   * @param {object} ServerData Server call service
   * @param {function} Component
   * @param {object} ActionController
   * @param {object} Utilities
   */
  function (ServerData, Component, ActionController, Utilities) {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      template,
      scope: {
        'avatarId': '@avatarId'
      },
      link: function (scope) {
        // Exponer getContextPath() al scope
        scope.getContextPath = Utilities.getContextPath;

        // Init as component
        let  component = new Component(scope, scope.avatarId);
        if (!component.asComponent()) {
          // If component initialization is wrong, cancel initialization
          return false;
        }
        // Define extra controls
        let  controller = {
          hasChildren: false
        };
        // Define controller
        if (component.controller) {
          _.merge(component.controller, controller);
        } else {
          component.controller = controller;
        }

        // Has children
        component.controller.hasChildren = component.controller.children > 0;

        /**
         * Click button function
         */
        component.scope.onClick = function () {
          if (component.controller?.actions?.length > 0) {
            ActionController.addActionList(component.controller.actions, true, {address: component.address, context: component.context});
          }
        };
      }
    };
  }]);
