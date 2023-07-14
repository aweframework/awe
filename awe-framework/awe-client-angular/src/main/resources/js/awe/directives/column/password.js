import {aweApplication} from "./../../awe";
import {passwordColumnTemplate} from "../../services/text";

// Column password directive
aweApplication.directive('aweColumnPassword',
  ['ServerData', 'Column', 'Criterion',
    function (serverData, Column, Criterion) {
      return {
        restrict: 'E',
        replace: true,
        template: passwordColumnTemplate,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new Criterion(scope, column.id, elem);

          // Initialize criterion and column
          if (column.init(component).asCriterion()) {
            // Set visible value
            component.updateVisibleValue = function () {
              component.visibleValue = "******";
            };

            // Update visible value on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
