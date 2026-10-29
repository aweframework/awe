import {aweApplication} from "../../awe";
import {passwordInputTemplate} from "../../services/text";

// Password directive
aweApplication.directive('aweInputPassword',
  ['ServerData', 'Criterion',
    function ($serverData, Criterion) {
      return {
        restrict: 'E',
        replace: true,
        template: passwordInputTemplate,
        scope: {
          'criterionId': '@inputPasswordId'
        },
        link: function (scope, elem) {
          // Initialize criterion
          scope.initialized = new Criterion(scope, scope.criterionId, elem).asCriterion();

          // Show / hide button: only the type of the input changes, the value and the model stay as they are
          scope.passwordShown = false;
          scope.togglePassword = function () {
            scope.passwordShown = !scope.passwordShown;
            const input = elem[0].querySelector(".validator > input");
            if (input) {
              input.type = scope.passwordShown ? "text" : "password";
            }
          };
        }
      };
    }
  ]);
