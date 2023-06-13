import {aweApplication} from "./../../awe";
import {templateButtonCheckbox} from "../../services/checkboxRadio";

// Button checkbox directive
aweApplication.directive('aweInputButtonCheckbox',
  ['ServerData', 'CheckboxRadio',
    function (serverData, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateButtonCheckbox,
        scope: {
          'criterionId': '@inputButtonCheckboxId'
        },
        link: function (scope, elem, attrs) {
          // Initialize checkbox
          let  component = new CheckboxRadio(scope, scope.criterionId, elem);
          component.specialClass = "btn-" + scope.size;
          scope.initialized = component.asCheckbox();
        }
      };
    }
  ]);

