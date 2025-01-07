import {aweApplication} from "../../awe";
import {templateColumnCheckbox} from "../../services/checkboxRadio";

// Column checkbox directive
aweApplication.directive('aweColumnCheckbox',
  ['ServerData', 'Column', 'CheckboxRadio',
    function (serverData, Column, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateColumnCheckbox,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new CheckboxRadio(scope, column.id, elem);

          // Initialize criterion and column
          column.init(component).asCheckbox();
        }
      };
    }
  ]);
