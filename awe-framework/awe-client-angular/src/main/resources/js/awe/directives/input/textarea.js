import {aweApplication} from "../../awe";
import {textareaInputTemplate} from "../../services/text";

// Textarea directive
aweApplication.directive('aweInputTextarea',
  ['ServerData', 'Criterion', '$log',
    function (serverData, Criterion, $log) {
      return {
        restrict: 'E',
        replace: true,
        template: textareaInputTemplate,
        scope: {
          'criterionId': '@inputTextareaId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new Criterion(scope, scope.criterionId, elem).asCriterion();
        }
      };
    }
  ]);