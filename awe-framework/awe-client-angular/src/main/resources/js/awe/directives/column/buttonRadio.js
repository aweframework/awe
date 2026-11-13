import {aweApplication} from "../../awe";
import {templateColumnButtonRadio} from "../../services/checkboxRadio";

// Column button radio directive: the options of the list of the column drawn as one joined bar of buttons
aweApplication.directive('aweColumnButtonRadio',
  ['Column', 'CheckboxRadio',
    function (Column, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateColumnButtonRadio,
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
