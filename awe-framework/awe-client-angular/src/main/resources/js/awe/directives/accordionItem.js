import {aweApplication} from "../awe";
import {getCollapse} from "../services/collapse";

// Accordion item
aweApplication.directive('aweAccordionItem',
  ['ServerData', 'Component', 'Panel',
    function (serverData, Component, Panel) {
      return {
        restrict: 'E',
        replace: true,
        transclude: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('accordionItem');
        },
        scope: {
          'itemId': '@accordionItemId'
        },
        link: function (scope, element) {
          // Init as component
          let  component = new Component(scope, scope.itemId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }
          // Read component data from screen
          let  parent = scope.$parent.$parent;

          // Get controller values and assign to scope
          scope.label = component.controller.label;
          scope.isSelected = parent.controller.itemsSelected.indexOf(scope.itemId) !== -1;
          scope.autocollapse = parent.controller.autocollapse;
          scope.accordionId = parent.accordionId;

          // The panel and its toggle: the item is the only one which opens and closes them (no Bootstrap data-api)
          const collapse = getCollapse(element.find(".panel-collapse").first(), element.find(".accordion-toggle").first());
          collapse.setOpen(scope.isSelected, {animate: false});

          /*
           * Whenever Accordion's "itemsSelected" changes it means we have to update
           * this AccordionItem to see if it's still selected or not and apply
           * the adequate classes. The accordion keeps one item open when it is autocollapse, so the item which was
           * open closes while the new one opens
           */
          scope.$watch("$parent.$parent.controller.itemsSelected", function(newValue){
              scope.isSelected = newValue.indexOf(scope.itemId) !== -1;
              collapse.setOpen(scope.isSelected);
          });

          // Reveal the item when a field inside it fails the validation, and mark it while it holds invalid fields
          component.listeners = component.listeners || {};
          component.listeners["revealItem"] = Panel.revealOn(scope, () => scope.isSelected, () => scope.onClick(scope.itemId));
          component.listeners["trackInvalid"] = Panel.trackInvalid(scope, function (invalid) {
            scope.hasInvalid = invalid;
          });

          // Click handler
          scope.onClick = function(itemId, event){
              // The toggle is a link to the item: do not navigate to it
              event?.preventDefault?.();
              /* Accordion has a $watch over it's "selected" property that handles
               * itemsSelected array correctly. Setting its value to this item's ID
               * fires it
              */
            scope.$parent.$parent.controller.selected = itemId;
          };
        }
      };
    }
  ]);
