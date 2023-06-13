import {aweApplication} from "./../../awe";
import {textInputTemplate} from "../../services/text";

// Text directive
aweApplication.directive('aweInputText',
  ['ServerData', 'Criterion',
    function ($serverData, Criterion) {
      return {
        restrict: 'E',
        replace: true,
        template: textInputTemplate,
        scope: {
          'criterionId': '@inputTextId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new Criterion(scope, scope.criterionId, elem).asCriterion();
        }
      };
    }
  ]);
