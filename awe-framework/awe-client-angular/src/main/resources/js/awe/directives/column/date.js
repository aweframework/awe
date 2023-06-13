import {aweApplication} from "./../../awe";
import {calendarColumnTemplate} from "../../services/dateTime";

// Column datepicker directive
aweApplication.directive('aweColumnDate',
  ['ServerData', 'Column', 'DateTime',
    function (serverData, Column, DateTime) {
      return {
        restrict: 'E',
        replace: true,
        template: calendarColumnTemplate,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new DateTime(scope, column.id, elem);

          // Initialize criterion and column
          if (column.init(component).asDate()) {
            // Update visible value on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
