import {aweApplication} from "../awe";

/**
 * Find the element of a component by its identifier (criterion-id attribute or id). The identifier is compared as a
 * value, never written into a selector, so CSS special characters (a dot, a colon...) are safe.
 * @param {Object} $parent Parent node to search in
 * @param {string} id Component identifier
 * @return {Object} Matching nodes
 */
export function findComponentElement($parent, id) {
  return $parent.find("[criterion-id], [id]").filter(function () {
    return this.getAttribute("criterion-id") === id || this.id === id;
  });
}

// Validator service
aweApplication.factory('Validator',
  ['Storage', 'AweUtilities', 'AweSettings', 'ValidationRules', '$log', '$window',
    /**
     *
     * @param {object} Storage
     * @param {object} $utilities
     * @param {object} $settings
     * @param {object} ValidationRules
     * @param {object} $log
     */
    function (Storage, $utilities, $settings, ValidationRules, $log, $window) {

      // CONSTANTS
      const ERROR_CONTAINER = '.error-container';
      const FORM_GROUP = '.form-group';
      const FOCUSABLE = "input:not([type='hidden']), textarea, select, button";
      // Milliseconds to wait before focusing a control that was not displayed yet
      const REVEAL_FOCUS_DELAY = 100;

      /**
       * Validate each view
       * @param view
       * @param errorList
       * @param baseNode
       */
      function validateView(view, errorList, baseNode) {
        _.each(view, function (component) {
          Validator.validateComponent(component, errorList, baseNode);
          if ("cells" in component) {
            _.each(component.cells, function (cell) {
              Validator.validateComponent(cell, errorList, baseNode);
            });
          }
        });
      }

      let Validator = {
        /**
         * Validate a component
         * @param {Object} controller
         * @param {Object} address
         */
        initController: function (controller, address) {
          if (!("validationRules" in controller)) {
            // Parse validation rules
            controller.validationRules = Validator.parseValidationRules(controller.validation, address);
          }
        },
        /**
         * Validate a component
         * @param {Object} component
         * @param {Array} errorList
         * @param {Object} baseNode
         */
        validateComponent: function (component, errorList, baseNode) {
          let method = "validate";
          // The content of a deferred dialog or tab which was never opened is not validated
          if (Validator.isInPendingDeferredContainer(component)) {
            return;
          }
          // Get data for component
          if (method in component) {
            let validationData = component[method](baseNode, errorList.length > 0);
            if (validationData !== null) {
              errorList.push(validationData);
            }
          }
        },
        /**
         * Check if a component is inside a deferred dialog or tab which was not opened yet (its content was never
         * loaded, and the user may not even be able to open it)
         * @param {Object} component Component api
         * @return {boolean} The component is inside a pending deferred container
         */
        isInPendingDeferredContainer: function (component) {
          const address = component.address;
          if (!address) {
            return false;
          }
          // The API of a component has no element: look for it by its identifier
          const element = component.element || findComponentElement($(document), address.component);
          if (!element || $(element).length === 0) {
            return false;
          }
          const controllers = Storage.get("controller");
          return _.some(controllers && controllers[address.view], function (controller, containerId) {
            return containerId !== address.component && controller && controller.deferred && !controller.deferredLoaded
              && $(document.getElementById(containerId)).find(element).length > 0;
          });
        },
        /**
         * Retrieve the validation nodes
         * @param {Object} baseNode base node
         */
        validateNode: function (baseNode) {
          // Retrieve action data
          let errorList = [];
          let api = Storage.get("api");

          // Retrieve components from each view
          _.each(api, function (view) {
            validateView(view, errorList, baseNode);
          });
          // Return data
          return errorList;
        },
        /**
         * Validate a value with
         * @param {Mixed} value
         * @param {object} rules
         * @param {object} element
         * @param {object} address
         * @returns {Array}
         */
        validate: function (value, rules, element, address) {
          let errorMessage = null;
          _.each(rules, function (rule, ruleMethod) {
            if (errorMessage === null) {
              errorMessage = ValidationRules.validate(ruleMethod, value, rule, address);
              if (angular.isObject(errorMessage)) {
                errorMessage.element = element;
                errorMessage.id = $utilities.getAddressId(address);
              }
            }
          });
          return errorMessage;
        },
        /**
         * Retrieve the validation nodes
         * @param {type} scope
         * @param {type} error
         * @returns {undefined}
         */
        showValidationError: function (scope, error) {
          let $errorContainer = $(ERROR_CONTAINER);
          let $element = $(error.element);

          // Retrieve the element data
          $utilities.timeout.cancel(scope.errorTimer);
          $utilities.timeout(() => {
            let target = $element;
            let classTarget = $element.find(FORM_GROUP);
            let targetPosition = target.offset();
            targetPosition.top += target.height();

            // Show validation message
            $errorContainer.css(targetPosition);
            scope.validationStyle = classTarget.length > 0 ? classTarget.attr("class") : "";
            scope.validationMessage = error.message;
            $log.info("[VALIDATION] '" + error.id + "' - " + scope.validationMessage);
            scope.showValidation = true;

            // Focus and scroll to element
            if (!Validator.revealElement($element)) {
              let scrollable = $window;
              let scrollableNodes = $element.parentsUntil(".scrollable");
              if (scrollableNodes.length > 0) {
                scrollable = scrollableNodes[0];
              }
              scrollable.scrollTo(targetPosition.left, targetPosition.top - target.height());
            }

            // Hide validation after timeout seconds (if not equals to 0)
            if ($settings.get("messageTimeout").validate > 0) {
              scope.errorTimer = $utilities.timeout(function () {
                scope.showValidation = false;
              }, $settings.get("messageTimeout").validate);
            }
          });
        },
        /**
         * Focus the control of a criterion and scroll it into view
         * @param {Object} $element Criterion element (jQuery)
         * @return {boolean} The element has been scrolled into view
         */
        revealElement: function ($element) {
          let node = $element[0];
          if (!node || typeof node.scrollIntoView !== "function") {
            return false;
          }
          const control = $element.find(FOCUSABLE).first();
          control.trigger("focus");
          if (control.length > 0 && document.activeElement !== control[0]) {
            // The tab, accordion item or step that holds the control may still be opening: focus it once it is displayed
            $utilities.timeout(() => control.trigger("focus"), REVEAL_FOCUS_DELAY);
          }
          node.scrollIntoView({block: "center", behavior: "smooth"});
          return true;
        },
        /**
         * Retrieve the validation nodes
         * @param {type} validationRules
         * @param {type} address
         * @returns {undefined}
         */
        parseValidationRules: function (validationRules, address) {
          let rulesParsed = {};
          if (validationRules) {
            if (angular.isObject(validationRules)) {
              rulesParsed = validationRules;
            } else {
              let rulesList = validationRules.indexOf("{") > -1 ? [validationRules] : validationRules.split(" ");
              rulesParsed = rulesList.reduce((parsed, rule) => ({...parsed, ...Validator.parseRule(rule, address)}), {});
            }
          }
          return rulesParsed;
        },
        /**
         * Retrieve the validation nodes
         * @param {string|object} rule
         * @param {object} address
         * @returns {object} Rule as object
         */
        parseRule: function (rule, address) {
          let newRule;
          if (angular.isObject(rule)) {
            newRule = rule;
          } else if (rule.indexOf("{") > -1) {
            try {
              newRule = $utilities.evalJSON(rule);
            } catch (exc) {
              $log.error("[ERROR] Parsing validation rule to JSON", {rule: rule, address: address, exception: exc});
            }
          } else {
            newRule = {[rule]: true};
          }
          return {...newRule};
        }
      };
      return Validator;
    }
  ]);
