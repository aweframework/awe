import {aweApplication} from "./../../awe";
import "angular-bootstrap-colorpicker";
import {templateInputColor} from "../../services/criterion";

// Add requirements
aweApplication.requires.push("colorpicker.module");

// Colorpicker directive
aweApplication.directive('aweInputColor',
  ['ServerData', 'Criterion',
    function (serverData, Criterion) {
      return {
        restrict: 'E',
        replace: true,
        template: templateInputColor,
        scope: {
          'criterionId': '@inputColorId'
        },
        link: function (scope, elem, attrs) {
          // Initialize criterion
          scope.initialized = new Criterion(scope, scope.criterionId, elem).asCriterion();
        }
      };
    }
  ]);
