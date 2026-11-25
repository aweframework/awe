import {aweApplication} from "../../awe";
import {launchColumnActionsOnRow, templateColumnLink} from "../../services/button";

// Column link directive (#993): without actions the value of the cell is an external address opened in a new tab; with
// actions the link launches them on its row, as the column button does (click or Enter)
aweApplication.directive('aweColumnLink',
  ['Column', 'Button', 'AweUtilities', 'ActionController',
    function (Column, Button, Utilities, ActionController) {

      return {
        restrict: 'E',
        replace: true,
        template: templateColumnLink,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          const column = new Column(attrs);
          const component = new Button(scope, column.id, elem);
          component.element = elem;

          // Initialize criterion and column
          column.init(component).asButton();

          /**
           * Whether the column has actions to launch
           * @returns {boolean} The link launches actions
           */
          scope.hasActions = function () {
            const actions = component.controller && component.controller.actions;
            return Array.isArray(actions) && actions.length > 0;
          };

          /**
           * Text of the link: the label of the cell, else its value
           * @returns {string} Label
           */
          scope.getLinkLabel = function () {
            const cell = (component.model && component.model.values && component.model.values[0]) || {};
            return cell.label || cell.value || "";
          };

          /**
           * Tooltip of the link, as in the button column: the title of the cell, else the one of the column
           * @returns {string} Tooltip
           */
          scope.getLinkTitle = function () {
            const cell = (component.model && component.model.values && component.model.values[0]) || {};
            return cell.title || (component.controller && component.controller.title) || "";
          };

          // A click on the cell does not reach the grid, and the external address is still opened
          scope.click = function (event) {
            Utilities.stopPropagation(event);
          };

          // The link acts on its row: it is selected (only it) before its actions
          launchColumnActionsOnRow(scope, component, Utilities, ActionController);

          /**
           * Launch the actions of the column, unless the actions are disabled (as the button column)
           * @param {Object} event Event
           */
          scope.onLinkClick = function (event) {
            Utilities.stopPropagation(event, true);
            if (!scope.isDisabled()) {
              scope.onClick(event);
            }
          };

          /**
           * Enter launches the actions of the column, as a click
           * @param {Object} event Keyboard event
           */
          scope.onLinkKeyDown = function (event) {
            if (event.key === "Enter") {
              scope.onLinkClick(event);
            }
          };
        }
      };
    }
  ]);
