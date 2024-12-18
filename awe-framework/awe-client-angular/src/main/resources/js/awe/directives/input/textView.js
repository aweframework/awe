import {aweApplication} from "../../awe";
import {textViewInputTemplate} from "../../services/text";

// Text view directive
aweApplication.directive('aweInputTextView',
  ['ServerData', 'Text',
    function (serverData, Text) {
      return {
        restrict: 'E',
        replace: true,
        template: textViewInputTemplate,
        scope: {
          'criterionId': '@inputTextViewId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new Text(scope, scope.criterionId, elem).asText();
        }
      };
    }
  ]);
