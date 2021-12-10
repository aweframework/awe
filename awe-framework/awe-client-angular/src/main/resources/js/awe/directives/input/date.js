import {aweApplication} from "../../awe";
import {calendarTemplate} from "../../services/dateTime";

// Datepicker directive
aweApplication.directive('aweInputDate',
  ['ServerData', 'DateTime',
    function (serverData, DateTime) {
      return {
        restrict: 'E',
        replace: true,
        template: calendarTemplate,
        scope: {
          'criterionId': '@inputDateId'
        },
        link: function (scope, elem, attrs) {
          // Initialize criterion
          scope.initialized = new DateTime(scope, scope.criterionId, elem).asDate();
        }
      };
    }
  ]);
