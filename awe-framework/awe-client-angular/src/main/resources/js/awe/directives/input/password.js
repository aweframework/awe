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

          // Out of the sign-in page a password is never a login (new-password): the browser does not fill a saved
          // credential in it or in the text field before it. The sign-in page keeps the credential autofill
          if (elem[0].closest(".awe-signin")) {
            elem[0].querySelector(".validator > input")?.removeAttribute("autocomplete");
          }

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
