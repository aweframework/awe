import {aweApplication} from "./../../awe";
import {timeInputTemplate} from "../../services/dateTime";

// Time directive
aweApplication.directive('aweInputTime',
  ['ServerData', 'DateTime',
    function (serverData, DateTime) {
      return {
        restrict: 'E',
        replace: true,
        template: timeInputTemplate,
        scope: {
          'criterionId': '@inputTimeId'
        },
        link: function (scope, elem, attrs) {
          // Initialize criterion
          scope.initialized = new DateTime(scope, scope.criterionId, elem).asTime();
        }
      };
    }
  ]);