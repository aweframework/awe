import {aweApplication} from "../awe";
import {templateButton} from "../services/button";

// Button directive
aweApplication.directive('aweButton',
  ['ServerData', 'Button',
    function ($serverData, Button) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template: templateButton,
        scope: {
          'buttonId': '@'
        },
        link: function (scope, element) {
          // Initialize button
          scope.initialized = new Button(scope, scope.buttonId, element).asButton();
        }
      };
    }]);
