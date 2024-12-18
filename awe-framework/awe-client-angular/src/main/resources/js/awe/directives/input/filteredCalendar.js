import {aweApplication} from "../../awe";
import {calendarInputTemplate} from "../../services/dateTime";

// Filtered calendar directive
aweApplication.directive('aweInputFilteredCalendar',
  ['ServerData', 'DateTime',
    function (serverData, DateTime) {
      return {
        restrict: 'E',
        replace: true,
        template: calendarInputTemplate,
        scope: {
          'criterionId': '@inputFilteredCalendarId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new DateTime(scope, scope.criterionId, elem).asFilteredDate();
        }
      };
    }
  ]);
