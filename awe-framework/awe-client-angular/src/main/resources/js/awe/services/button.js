import {aweApplication} from "../awe";
import {getIconTemplate} from "./component";
import {TestIds} from "../data/testIds";
import {busySpinnerTemplate, createBusyState} from "./busyState";
import {iconMarkup} from "./icon";

export const templateButton = `<div ng-show="controller.visible" class="{{::groupClass}}" ui-dependency="dependencies" ng-cloak>
  <button type="{{::component.buttonType}}" id="{{::buttonId}}" data-testid="${TestIds.button}" title="{{getTitle()| translateMultiple}}"
          class="{{classes}}" ng-class="{'is-loading': busy}" ng-attr-aria-busy="{{busy ? 'true' : undefined}}"
          ng-attr-aria-label="{{iconOnly ? ((controller.label | translateMultiple) || undefined) : undefined}}"
          ng-attr-aria-haspopup="{{hasMenu ? 'menu' : undefined}}"
          ng-disabled="isDisabled()" ng-click="onClick()" ng-mousedown="onMouseDown()" ng-transclude>
    ${busySpinnerTemplate("busy")}
    ${getIconTemplate("button-icon", "i")}
    <span ng-if="controller.label && !iconOnly" class="button-text" translate-multiple="{{busy && controller.loadingLabel ? controller.loadingLabel : controller.label}}"></span>
    <span ng-if="hasMenu" class="awe-button-menu-chevron" aria-hidden="true">${iconMarkup("lucide:chevron-down")}</span>
  </button>
  <awe-context-menu open-on-right-click ng-cloak></awe-context-menu>
  <awe-loader class="loader button-loader" skeleton="button" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

export const templateColumnButton = `<div ng-show="component.controller.visible" class="column text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-click="click($event)" ng-cloak>
  <button type="button" id="{{component.id}}" data-testid="${TestIds.button}" title="{{getCellTitle()| translateMultiple}}"
          ng-attr-aria-label="{{component.model.values[0].label ? undefined : ((getCellTitle() | translateMultiple) || undefined)}}"
          class="{{classes}} {{component.model.values[0].style}}" ng-disabled="isDisabled()" ng-click="onClick($event)" ng-mousedown="onMouseDown()" ng-cloak>
    ${getIconTemplate("button-icon", "i")}
    <span ng-if="component.model.values[0].label" class="button-text" translate-multiple="{{component.model.values[0].label}}"></span>
  </button>
</div>`;

// Link of a column (#993): with actions, the label of the cell drawn as a link that launches them (no address); without
// actions, the value of the cell is an external address opened in a new tab, announced by a decorative external-link
// icon and a hidden text (WCAG G201)
export const templateColumnLink = `<div ng-show="component.controller.visible" class="column column-link text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-click="click($event)" ng-cloak>
  <a ng-if="hasActions()" id="{{component.id}}" role="link" ng-attr-tabindex="{{isDisabled() ? -1 : 0}}" ng-attr-title="{{(getLinkTitle() | translateMultiple) || undefined}}"
     ng-attr-aria-disabled="{{isDisabled() ? 'true' : undefined}}"
     class="link column-link-action {{component.controller.style}} {{component.model.values[0].style}}"
     ng-click="onLinkClick($event)" ng-keydown="onLinkKeyDown($event)" ng-mousedown="onMouseDown()" translate-multiple="{{getLinkLabel()}}"></a>
  <a ng-if="!hasActions()" id="{{component.id}}" ng-href="{{component.model.values[0].value}}" target="_blank" rel="noreferrer"
     ng-attr-title="{{(getLinkTitle() | translateMultiple) || undefined}}"
     class="link column-link-external {{component.controller.style}} {{component.model.values[0].style}}"><span class="column-link-label" translate-multiple="{{getLinkLabel()}}"></span>${iconMarkup("lucide:external-link column-link-external-icon")}<span class="sr-only"> {{'LINK_OPENS_IN_NEW_TAB' | translateMultiple}}</span></a>
</div>`;

/**
 * Make a cell component launch the actions of its column on its row (button and link columns): the row of the cell is
 * selected (only it) before the actions run, as their dependencies and the parameters they send read the selected row.
 * The click launches them, so they are no longer pending: selecting the row may run actions that disable the buttons,
 * and a pending click would launch them again
 * @param {Object} scope Cell scope (with the onClick of the Button service)
 * @param {Object} component Cell component, initialized as button
 * @param {Object} Utilities Utilities service
 * @param {Object} ActionController Action controller service
 */
export function launchColumnActionsOnRow(scope, component, Utilities, ActionController) {
  const launchActions = scope.onClick;
  scope.onClick = function (event) {
    component.pendingActions = false;
    component.selectCellRow();
    launchActions(event);
  };

  /**
   * Click function of the component
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

/**
 * Check if a button style makes it icon-only (the no-label class, as for criteria)
 * @param {string} style Button style
 * @returns {boolean} The button shows only its icon
 */
export function isIconOnly(style) {
  return String(style || "").split(/\s+/).includes("no-label");
}

/**
 * Check if a button style makes it a menu button (the has-menu class, as the header buttons whose action is
 * show-context-menu): it shows a trailing chevron and tells assistive technologies that it opens a menu
 * @param {string} style Style of the button
 * @returns {boolean} The button opens a menu
 */
export function hasMenu(style) {
  return String(style || "").split(/\s+/).includes("has-menu");
}

// Button service
aweApplication.factory('Button',
  ['Component', 'Storage', 'ActionController', 'AweUtilities', '$timeout',
    /**
     * Button generic methods
     * @param {Object} Component component
     * @param {Object} Storage service
     * @param {Object} ActionController service
     * @param {Object} Utilities service
     * @param {Function} $timeout Timeout service
     */
    function (Component, Storage, ActionController, Utilities, $timeout) {

      /**
       * Button constructor
       * @param {Scope} scope Button scope
       * @param {String} id Button id
       * @param {String} element Button element
       */
      function Button(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Component(scope, id);
        // The context menu action hangs the menu from the button itself
        this.component.getContextMenuAnchor = function () {
          return element.is("button") ? element : element.find("button").first();
        };
        // Store mouse down
        this.component.pendingActions = false;
        const button = this;

        /**
         * Initialize as button
         */
        this.component.asButton = function () {
          return button.init();
        };
        return this.component;
      }

      Button.prototype = {

        /**
         * Initialization method
         */
        init: function () {
          // Init as component
          const button = this;
          const component = this.component;
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          // Generate button class
          if (component.controller) {
            // Submit draws as primary and reset as default (Bootstrap secondary), as in React
            component.scope.buttonClass = {submit: "primary", reset: "default"}[component.controller.buttonType] || null;
            // Add reset action if type is reset
            if (component.controller.actions && component.controller.actions.length === 0 && component.controller.buttonType === "reset") {
              let  resetAction = {type: "restore"};
              component.controller.actions.push(resetAction);
            }

            // Generate buttonType
            component.buttonType = component.controller.buttonType === "reset" ? "button" : component.controller.buttonType;
          }

          // Busy state of the button while the actions it launched run (unless the button sets show-loading="false")
          const busyState = createBusyState({
            scope: component.scope,
            property: "busy",
            isRunning: () => Storage.get("actions-running"),
            timeout: $timeout,
            threshold: component.controller.busyThreshold
          });

          //*****************************************************************************
          // SCOPE METHODS
          //****************************************************************************/

          /**
           * On click out event
           * @param {Object} e Event
           */
          component.scope.click = function (e) {
            // Cancel event propagation
            Utilities.stopPropagation(e, true);
          };

          /**
           * Click button function
           */
          component.scope.onClick = function () {
            component.pendingActions = false;
            Utilities.timeout.cancel(component.helpTimer);
            ActionController.addActionList(component.controller.actions, true, {
              address: component.address,
              context: component.context
            });
            if (component.controller && component.controller.showLoading !== false) {
              busyState.start();
            }
            component.storeEvent('click');
          };

          /**
           * Tooltip of the button: its title; an icon-only button (style no-label) without help shows its label
           * @returns {string} Tooltip
           */
          component.scope.getTitle = function () {
            const controller = component.controller || {};
            if (controller.title) {
              return controller.title;
            }
            return component.scope.iconOnly && !("help" in controller) ? controller.label : "";
          };

          /**
           * Check if button is disabled
           * @returns {boolean} Button is disabled
           */
          component.scope.isDisabled = function () {
            return Storage.get("actions-running") || component.scope.$root.status.loading ||
              (component.controller && component.controller.disabled);
          };

          /**
           * Mousedown button function
           * Store if button was disabled on mouse down to see if there are
           * pending actions that have not been launched due to button disablery
           */
          component.scope.onMouseDown = function () {
            component.pendingActions = !component.scope.isDisabled();
          };

          //*****************************************************************************
          // PRIVATE METHODS
          //****************************************************************************/

          /**
           * Retrieve button class
           * @return {String} button class
           */
          const getClass = function () {
            let buttonClass = [];
            let buttonStyle = component.controller && component.controller.style ? component.controller.style : "";
            let buttonSize = "btn-" + component.scope.size;
            buttonClass.push(buttonStyle);
            if (buttonStyle === "" || buttonStyle.indexOf('no-class') === -1) {
              buttonClass.push("btn");
              buttonClass.push("btn-awe");
              buttonClass.push(buttonSize);
              if (component.scope.buttonClass) {
                buttonClass.push("btn-" + component.scope.buttonClass);
              }
            }

            if (component.controller && "help" in component.controller) {
              buttonClass.push("help-target");
            }

            return buttonClass.join(" ");
          };

          /**
           * Retrieve button group class
           * @return {String} button group class
           */
          const getGroupClass = function () {
            let buttonClass = [];
            let buttonStyle = component.controller.style || "";
            if (buttonStyle === "" || buttonStyle.indexOf('no-class') === -1) {
              buttonClass.push("btn-group");
            }
            if (component.controller.hidden) {
              buttonClass.push("hidden");
            }
            if (component.controller.invisible) {
              buttonClass.push("invisible");
            }
            return buttonClass.join(" ");
          };

          /**
           * Check if there are pending actions, and if true, launch them
           */
          const checkPendingActions = function () {
            if (component.pendingActions) {
              component.scope.onClick();
            }
          };

          /**
           * Initialize help rules
           */
          const initHelp = function () {
            // Set context menu if existing
            if (component.controller && "help" in component.controller) {
              Utilities.timeout(function () {
                // Initialize help node
                let help = {
                  node: button.element.find(".help-target"),
                  text: component.controller.help,
                  image: component.controller.helpImage
                };
                component.initHelpNode(component.id, help);
              }, 100);
            }
          };

          // Initialize help
          initHelp();

          //****************************************************************************
          //  COMPONENT METHODS
          //****************************************************************************

          /**
           * Store button classes
           */
          component.updateClasses = function () {
            // An icon-only button (style no-label) shows only its icon: its label is its tooltip and accessible name
            component.scope.iconOnly = isIconOnly(component.controller && component.controller.style);
            // A button that opens a menu (style has-menu) shows a trailing chevron
            component.scope.hasMenu = hasMenu(component.controller && component.controller.style);
            component.scope.groupClass = getGroupClass();
            component.scope.classes = getClass();
          };

          // Initially update classes
          component.updateClasses();

          /**
           * Basic getSpecificFields function (To be overwritten on complex directives)
           * @returns {Object} Specific fields from component
           */
          component.getSpecificFields = function () {
            return {buttonValue: component.model.selected, buttonAddress: component.address};
          };

          //****************************************************************************
          // EVENT LISTENERS
          //****************************************************************************
          component.listeners = component.listeners || {};
          // On model change launch dependency
          component.listeners["controllerChange"] = component.scope.$on("controllerChange", function (_event, parameters) {
            if (_.isEqual(parameters.address, component.address)) {
              component.updateClasses();
            }
          });
          // On disable button check for pending actions
          component.listeners["disableButtons"] = component.scope.$on("disable-buttons", checkPendingActions);

          // Return initialization flag
          return true;
        }
      };
      return Button;
    }
  ]);