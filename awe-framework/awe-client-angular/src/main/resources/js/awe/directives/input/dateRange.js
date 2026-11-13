import {aweApplication} from "../../awe";
import {dateRangeInputTemplate} from "../../services/dateRange";

// Date range directive
aweApplication.directive('aweInputDateRange',
  ['DateRange',
    function (DateRange) {
      return {
        restrict: 'E',
        replace: true,
        template: dateRangeInputTemplate,
        scope: {
          'criterionId': '@inputDateRangeId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new DateRange(scope, scope.criterionId, elem).asDateRange();
        }
      };
    }
  ]);
