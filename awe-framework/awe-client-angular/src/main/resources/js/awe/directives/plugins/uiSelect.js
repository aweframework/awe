import {aweApplication} from "../../awe";
import "Select2";
import {bindSelect2TestIds, wrapSelect2FormatResult, wrapSelect2FormatSelection} from "../../services/testIds";

// Selector plugin
aweApplication.directive('uiSelect2',
  ['$translate', 'AweUtilities',
    /**
     * select2 Wrapper
     * @param {object} $translate
     * @param {object} Utilities
     */
    function ($translate, Utilities) {
      return {
        // This directive only works when used in element's attribute (e.g: ui-select2)
        restrict: 'A',
        priority: 1,
        link: function (scope, elem, attrs) {
          let initialized = false;
          let options = {data: null};
          let testIds = null;

          // Id of the selector, used as owner of the plugin DOM (the dropdown is appended to the body)
          const getOwner = () => scope.component && scope.component.id || attrs.id;

          // Observe select2 attributes
          let initWatch = scope.$watch(attrs.initialized, initPlugin);

          /**
           * Plugin initialization
           * @param {object} startPlugin flag that indicates whether to start plugin or not
           */
          function initPlugin(startPlugin) {
            let controller = scope[attrs.uiSelect2];
            // Report parent of plugin generated
            if (!initialized && controller && startPlugin) {

              // Create plugin
              let opts = _.merge({}, options, controller);
              if ("placeholder" in opts) {
                opts.placeholder = $translate.instant(opts.placeholder);
              }
              // Test hooks: wrap the formatters (default markup is preserved) and tag the plugin DOM
              opts.formatResult = wrapSelect2FormatResult(opts.formatResult, getOwner);
              opts.formatSelection = wrapSelect2FormatSelection(opts.formatSelection, getOwner);
              let plugin = elem.select2(opts);
              testIds = bindSelect2TestIds(elem, getOwner);

              // Define management methods

              /**
               * Fill the selector
               * @param {Object} data
               */
              scope.component.fill = (data) => {
                plugin.select2("data", data);
              };

              /**
               * Select a value
               * @param {Object} value
               */
              scope.component.select = (value) => {
                plugin.select2("val", value);
              };

              // Notify initialization
              initialized = true;
              scope.component.onPluginInit();

              // Unwatch initialization
              initWatch();
            }
          }

          /**
           * Plugin update
           */
          function updatePlugin() {
            // Get options
            let opts = _.merge({}, options, scope[attrs.uiSelect2]);

            // Update plugin
            let select2 = elem.data("select2");
            if ("placeholder" in opts) {
              select2.opts.placeholder = $translate.instant(opts.placeholder);
              select2.setPlaceholder?.();
            }
          }

          /**
           * Event listeners
           */
          let listeners = {};

          // Observe change event
          elem.on("change", function (event) {
            scope.component.onPluginChange(event);
            scope.$apply();
          });

          // Observe focus event
          elem.on("select2-focus", function () {
            scope.component.changeFocus(true);
            scope.$apply();
          });

          // Observe blur event
          elem.on("select2-blur", function () {
            scope.component.changeFocus(false);
            scope.$apply();
          });

          // Observe destroy event
          elem.on("$destroy", destroy);

          // Watch for language change
          listeners["languageChanged"] = scope.$on('languageChanged', function () {
            updatePlugin();
          });

          /**
           * Kill plugin
           */
          function killPlugin() {
            if (initialized) {
              // Destroy plugin
              //elem.select2('destroy');
              // Remove event listeners
              elem.off("change select2-focus select2-blur");
              if (testIds) {
                testIds.unbind();
                testIds = null;
              }
            }
          }

          /**
           * Destroy plugin
           */
          function destroy() {
            killPlugin();
            initialized = false;
            // Clear listeners
            Utilities.clearListeners(listeners);
          }
        }
      };
    }
  ]);
