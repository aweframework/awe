import {aweApplication} from "../../awe";
import "bootstrap-datepicker";
import {bindDatepickerTestIds} from "../../services/testIds";

/**
 * Set the date of one input if it is not the one it shows
 * @param {jQuery} input Input
 * @param {string} date Date
 */
function setInputDate(input, date) {
  if (input.val() !== (date || "")) {
    input.datepicker('setDate', date || null);
  }
}

// Date range plugin: two bootstrap datepickers (start and end of a range) where the end cannot be before the start
aweApplication.directive('uiDateRange',
  ['AweUtilities',
    /**
     * Bootstrap datepicker wrapper for a pair of inputs
     * @param {object} Utilities
     */
    function (Utilities) {
      return {
        // This directive only works when used in element's attribute (e.g: ui-date-range)
        restrict: 'A',
        priority: 1,
        compile: function () {
          return function (scope, elem, attrs) {
            let initialized = false;
            let options = null;
            let inputs = null;
            let testIds = [];

            // Watch for controller changes
            let initWatch = scope.$watch(attrs.initialized, initPlugin);

            /**
             * Plugin initialization
             * @param {object} startPlugin flag that indicates whether to start plugin or not
             */
            function initPlugin(startPlugin) {
              let controller = options || scope[attrs.uiDateRange];
              if (!initialized && controller && startPlugin) {
                // Initialize both inputs as dates with options
                inputs = elem.find("input");
                inputs.datepicker(controller);
                options = controller;
                initialized = true;

                // Unwatch initialization
                initWatch();

                // Init events
                initEvents();

                // Add methods to component
                addMethodsToComponent();
              }
            }

            /**
             * The start cannot be after the end and the end cannot be before the start
             * @param {string} from Start date
             * @param {string} to End date
             */
            function updateLimits(from, to) {
              inputs.eq(0).datepicker('setEndDate', to || null);
              inputs.eq(1).datepicker('setStartDate', from || null);
            }

            /**
             * Update the limits with the dates the inputs show
             */
            function onChangeDate() {
              updateLimits(inputs.eq(0).val(), inputs.eq(1).val());
            }

            /**
             * Update both datepickers with the selected values of the model
             */
            function updateModelSelected() {
              const [from, to] = Utilities.asArray(scope.component.model.selected);
              setInputDate(inputs.eq(0), from);
              setInputDate(inputs.eq(1), to);
              updateLimits(from, to);
            }

            /**
             * Add methods to component
             */
            function addMethodsToComponent() {
              let component = scope.component;

              /**
               * Update selected values
               */
              component.updateModelSelected = updateModelSelected;

              // Update initialization flag
              component.pluginInitialized = true;

              // Show the dates of the model the first time
              updateModelSelected();
            }

            /**
             * Event listeners
             */
            let listeners;

            /**
             * Tag the popup of one input with test hooks
             * @param {Element} input Input
             * @returns {object} Test ids binding
             */
            function bindTestIds(input) {
              return bindDatepickerTestIds($(input), getComponentId);
            }

            /**
             * Get the id of the component
             * @returns {string} Component id
             */
            function getComponentId() {
              return scope.component?.id || attrs.id;
            }

            /**
             * Event initialization
             */
            function initEvents() {
              listeners = {};

              // Watch for language change
              listeners["languageChanged"] = scope.$on('languageChanged', updatePlugin);

              // Keep the limits of both dates when one changes
              inputs.on("changeDate", onChangeDate);

              // Tag the popups (appended to the body) and their cells with test hooks
              testIds = inputs.toArray().map(bindTestIds);
            }

            /**
             * Destroy plugin
             */
            function destroy() {
              if (initialized) {
                testIds.forEach(testId => testId.unbind());
                testIds = [];
                inputs.off("changeDate", onChangeDate);
                inputs.datepicker('destroy');
                initialized = false;

                // Clear listeners
                Utilities.clearListeners(listeners);
              }
            }

            /**
             * Update plugin
             */
            function updatePlugin(event, language) {
              // Update elements as dates with options
              destroy();
              options.language = language;
              initPlugin(options);
            }

            // Observe destroy event
            elem.on("$destroy", destroy);
          };
        }
      };
    }
  ]);
