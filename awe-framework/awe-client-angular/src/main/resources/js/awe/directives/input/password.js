import {aweApplication} from "./../../awe";
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
        }
      };
    }
  ]);
