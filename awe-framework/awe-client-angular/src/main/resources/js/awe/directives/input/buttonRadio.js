import {aweApplication} from "../../awe";
import {
  hasRadioOptions,
  isChips,
  isChoiceCards,
  templateButtonRadio,
  templateButtonRadioGroup,
  templateChips,
  templateChoiceCards
} from "../../services/checkboxRadio";
import {getIcon} from "../../services/icon";

// Button radio directive: a criterion with options is one group of buttons, one per option (as in the React engine),
// one card per option with style="awe-choice-cards" or one pill per option with style="awe-chips"; without options it is one button of its value, grouped with
// the others of its group
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
          let  controller = Control.getAddressController(address);
          let  withOptions = hasRadioOptions(controller, Control.getAddressModel(address));
          // The icon of the option of a card
          scope.getOptionIcon = option => getIcon(option?.icon);
          let  groupTemplate = isChoiceCards(controller) ? templateChoiceCards : templateButtonRadioGroup;
          // style="awe-chips": pills that wrap (#993, T37)
          if (isChips(controller)) {
            groupTemplate = templateChips;
          }
          let  node = $compile(withOptions ? groupTemplate : templateButtonRadio)(scope);
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
