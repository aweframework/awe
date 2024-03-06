import {aweApplication} from "./../../awe";
import "../../services/checkboxRadio";
import {templateInputCheckbox} from "../../services/checkboxRadio";

// Checkbox directive
aweApplication.directive('aweInputCheckbox',
  ['ServerData', 'CheckboxRadio',
    function (serverData, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateInputCheckbox,
        scope: {
          'criterionId': '@inputCheckboxId'
        },
        link: function (scope, elem, attrs) {
          // Initialize checkbox
          scope.initialized = new CheckboxRadio(scope, scope.criterionId, elem).asCheckbox();
        }
      };
    }
  ]);
