import {aweApplication} from "./../../awe";
import {templateColumnButton} from "../../services/button";

// Column button directive
aweApplication.directive('aweColumnButton',
  ['ServerData', 'Column', 'Button', 'AweUtilities', 'AweSettings', 'ActionController',
    function (serverData, Column, Button, Utilities, $settings, ActionController) {

      return {
        restrict: 'E',
        replace: true,
        template: templateColumnButton,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          const column = new Column(attrs);
          const component = new Button(scope, column.id, elem);

          // Initialize criterion and column
          column.init(component).asButton();

          /**
           * Click button function
           * @param {Object} event Event
           */
          component.onClick = function (event) {
            // Cancel event propagation
            Utilities.stopPropagation(event);

            // Launch action list
            component.pendingActions = false;
            ActionController.addActionList(component.controller.actions, true, scope);
            component.storeEvent('click');
          };
        }
      };
    }
  ]);