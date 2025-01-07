import {aweApplication} from "../../awe";
import {timeColumnTemplate} from "../../services/dateTime";

// Column timepicker directive
aweApplication.directive('aweColumnTime',
  ['ServerData', 'Column', 'DateTime',
    function (serverData, Column, DateTime) {

      return {
        restrict: 'E',
        replace: true,
        template: timeColumnTemplate,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new DateTime(scope, column.id, elem);

          // Initialize criterion and column
          if (column.init(component).asTime()) {
            // Update visible value on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
