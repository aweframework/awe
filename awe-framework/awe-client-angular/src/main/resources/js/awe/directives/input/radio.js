import {aweApplication} from "../../awe";
import {templateRadio} from "../../services/checkboxRadio";

// Radio directive
aweApplication.directive('aweInputRadio',
  ['ServerData', 'CheckboxRadio',
    function (serverData, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateRadio,
        scope: {
          'criterionId': '@inputRadioId'
        },
        link: function (scope, elem, attrs) {
          // Initialize criterion
          scope.initialized = new CheckboxRadio(scope, scope.criterionId, elem).asRadio();
        }
      };
    }
  ]);

