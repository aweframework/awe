import {aweApplication} from "./../../awe";
import {textColumnTemplate} from "../../services/text";

// Column text directive
aweApplication.directive('aweColumnText',
  ['ServerData', 'Column', 'Criterion',
    function (serverData, Column, Criterion) {
      return {
        restrict: 'E',
        replace: true,
        template: textColumnTemplate,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new Criterion(scope, column.id, elem);

          // Initialize criterion and column
          if (column.init(component).asCriterion()) {
            // Update visible value on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
