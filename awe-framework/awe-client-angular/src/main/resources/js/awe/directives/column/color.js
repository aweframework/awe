import {aweApplication} from "../../awe";
import "angular-bootstrap-colorpicker";
import {templateColumnColor} from "../../services/criterion";

// Add requirements
aweApplication.requires.push("colorpicker.module");

// Column colorpicker directive
aweApplication.directive('aweColumnColor', [
  'ServerData', 'Criterion', 'Column',
  function (serverData, Criterion, Column) {
    return {
      restrict: 'E',
      replace: true,
      template: templateColumnColor,
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
