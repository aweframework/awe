import {aweApplication} from "../../awe";
import {hasRadioOptions, templateButtonRadio, templateButtonRadioGroup} from "../../services/checkboxRadio";

// Button radio directive: a criterion with options is one group of buttons, one per option (as in the React engine);
// without options it is one button of its value, grouped with the others of its group
aweApplication.directive('aweInputButtonRadio',
  ['$compile', 'Control', 'CheckboxRadio',
    function ($compile, Control, CheckboxRadio) {
      return {
        restrict: 'E',
        scope: {
          'criterionId': '@inputButtonRadioId'
        },
        link: function (scope, elem) {
          // The template depends on the model, so it is chosen here and replaces the element, as replace does
          let  view = scope.view || scope.$parent.view || scope.$parent.$parent.view;
          let  address = {view: view, component: scope.criterionId};
          let  withOptions = hasRadioOptions(Control.getAddressController(address), Control.getAddressModel(address));
          let  node = $compile(withOptions ? templateButtonRadioGroup : templateButtonRadio)(scope);
          Array.from(elem[0].attributes).forEach((attribute) => node.attr(attribute.name, attribute.value));
          elem.replaceWith(node);

          // Initialize the criterion
          let  component = new CheckboxRadio(scope, scope.criterionId, node);
          if (withOptions) {
            scope.initialized = component.asRadioGroup();
          } else {
            component.specialClass = "btn-" + scope.size;
            scope.initialized = component.asRadio();
          }
        }
      };
    }
  ]);
