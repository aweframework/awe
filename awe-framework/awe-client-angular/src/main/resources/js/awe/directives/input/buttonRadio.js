import {aweApplication} from "./../../awe";
import {templateButtonRadio} from "../../services/checkboxRadio";

// Button radio directive
aweApplication.directive('aweInputButtonRadio',
  ['ServerData', 'CheckboxRadio',
    function (serverData, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateButtonRadio,
        scope: {
          'criterionId': '@inputButtonRadioId'
        },
        link: function (scope, elem, attrs) {
          // Initialize checkbox
          let  component = new CheckboxRadio(scope, scope.criterionId, elem);
          component.specialClass = "btn-" + scope.size;
          scope.initialized = component.asRadio();
        }
      };
    }
  ]);

