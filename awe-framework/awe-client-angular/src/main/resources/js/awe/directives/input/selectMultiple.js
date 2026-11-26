import {aweApplication} from "../../awe";
import {templateSelector} from "../../services/selector";
import {isToggles, templateToggles} from "../../services/checkboxRadio";

// Select multiple directive: a select of several values, or one toggle pill per option with display="toggles" (#993,
// T37), whose template replaces the one of the select (its select2 is never started: it has no options)
aweApplication.directive('aweInputSelectMultiple',
  ['$compile', 'Control', 'Selector', 'CheckboxRadio',
    function ($compile, Control, Selector, CheckboxRadio) {
      return {
        restrict: 'E',
        replace: true,
        template: templateSelector,
        scope: {
          'criterionId': '@inputSelectMultipleId'
        },
        link: function (scope, elem) {
          let  view = scope.view || scope.$parent.view || scope.$parent.$parent.view;
          let  controller = Control.getAddressController({view: view, component: scope.criterionId});
          if (isToggles(controller)) {
            let  node = $compile(templateToggles)(scope);
            elem.replaceWith(node);
            scope.initialized = new CheckboxRadio(scope, scope.criterionId, node).asToggleGroup();
            return;
          }
          // Initialize criterion
          scope.initialized = new Selector(scope, scope.criterionId, elem).asSelectMultiple(true);
        }
      };
    }
  ]);
