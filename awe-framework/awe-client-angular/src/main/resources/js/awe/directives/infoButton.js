import {aweApplication} from "../awe";
import "../services/button";
import {getIconTemplate} from "../services/component";
import {TestIds} from "../data/testIds";

const template = `<li ng-show="controller.visible" ng-attr-id="{{::controller.id}}" title="{{controller.title| translateMultiple}}" class="info nav-icon-btn {{::controller.infoStyle}}" data-testid="${TestIds.infoButton}" ui-dependency="dependencies" ng-cloak>
  <a class="info-button info-button-{{::size}} {{::controller.style}}" data-testid="${TestIds.infoButtonLink}" ng-click="onClick($event)">
    ${getIconTemplate("nav-icon")}
    <span ng-if="controller.unit" class="label" translate-multiple="{{controller.unit}}"></span>
    <span ng-if="controller.text" class="info-text" translate-multiple="{{controller.text}}"></span>
    <span ng-if="controller.label" class="info-text" translate-multiple="{{controller.label}}"></span>
    <span ng-if="controller.title" class="small-screen-text" translate-multiple="{{controller.title}}"></span>
  </a>
</li>`;

// Info button directive
aweApplication.directive('aweInfoButton',
  ['ServerData', 'Button',
    /**
     * Info directive
     * @param {Service} ServerData Server call service
     * @param {Service} Button
     */
    function (ServerData, Button) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          'infoId': '@infoButtonId'
        },
        link: function (scope, element) {
          // Initialize button
          scope.initialized = new Button(scope, scope.infoId, element).asButton();
        }
      };
    }]);
