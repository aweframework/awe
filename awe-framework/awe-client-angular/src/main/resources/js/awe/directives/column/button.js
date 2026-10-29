import {aweApplication} from "../../awe";
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

          // The show-context-menu action anchors the menu on the button
          component.element = elem;

          // Initialize criterion and column
          column.init(component).asButton();

          /**
           * Tooltip of the cell button: the title of the cell, else the one of the column. A cell without label shows
           * only its icon and the tooltip is its accessible name too
           * @returns {string} Tooltip
           */
          scope.getCellTitle = function () {
            const cell = (component.model && component.model.values && component.model.values[0]) || {};
            return cell.title || (component.controller && component.controller.title) || "";
          };

          // The button acts on its row: it is selected (only it) before the actions run, as their dependencies and the
          // parameters they send read the selected row
          const launchActions = scope.onClick;
          scope.onClick = function (event) {
            component.selectCellRow();
            launchActions(event);
          };

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