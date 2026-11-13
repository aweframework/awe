import {aweApplication} from "../../awe";
import {templateColumnRadio} from "../../services/checkboxRadio";

// Column radio directive
aweApplication.directive('aweColumnRadio',
  ['Column', 'CheckboxRadio',
    function (Column, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateColumnRadio,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new CheckboxRadio(scope, column.id, elem);

          // Initialize criterion and column
          column.init(component).asColumnRadio();
        }
      };
    }
  ]);
