import {aweApplication} from "./../awe";
import {getIconTemplate} from "../services/component";

const template = `<li ng-show="controller.visible" ng-attr-id="{{::controller.id}}" title="{{controller.title| translateMultiple}}" class="info nav-icon-btn {{::controller.style}}" ng-class="::{'dropdown': controller.hasChildren}" ui-dependency="dependencies" ng-cloak>
  <a ng-click="infoClick()" ng-class="::{'dropdown-toggle': controller.hasChildren}" ng-attr-data-toggle="{{controller.hasChildren ? 'dropdown' : ''}}">
    ${getIconTemplate("nav-icon")}
    <span ng-if="controller.unit" class="label" translate-multiple="{{controller.unit}}"></span>
    <span ng-if="model.values[0].label" class="info-text" translate-multiple="{{model.values[0].label}}"></span>
    <span ng-if="controller.text" class="info-text" translate-multiple="{{controller.text}}"></span>
    <span ng-if="controller.label" class="info-text" translate-multiple="{{controller.label}}"></span>
    <span ng-if="controller.title" class="small-screen-text" translate-multiple="{{controller.title}}"></span>
  </a>
  <ul ng-if="::controller.hasChildren" class="dropdown-menu {{::controller.dropdownStyle}}" ng-transclude></ul>
</li>`;

// Info dropdown directive
aweApplication.directive('aweInfoDropdown', ['ServerData', 'Component', 'ActionController',
  /**
   * Info directive
   * @param {Service} ServerData Server call service
   * @param {Service} Component
   * @param {Service} ActionController
   */
  function (ServerData, Component, ActionController) {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      template,
      scope: {
        'infoId': '@infoDropdownId'
      },
      link: function (scope) {
        // Init as component
        let  component = new Component(scope, scope.infoId);
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
        component.scope.infoClick = function () {
          if (component.controller && component.controller.actions && component.controller.actions.length > 0) {
            ActionController.addActionList(component.controller.actions, true, {address: component.address, context: component.context});
          }
        };
      }
    };
  }]);
