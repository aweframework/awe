import {aweApplication} from "../../awe";
import "Select2";
import {attachSelectSummary, SUMMARY_CLASS} from "../../services/selectSummary";
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
          let summary = null;

          // Id of the selector, used as owner of the plugin DOM (the dropdown is appended to the body)
          const getOwner = () => scope.component && scope.component.id || attrs.id;

          // Helper of the criterion scope (the Criterion factory sets it there; this scope inherits it), if any
          const criterionHelper = (name) => typeof scope[name] === "function" ? scope[name]() : undefined;

          // Placeholder translated, with the required mark of a criterion without label
          const getPlaceholder = (placeholder) => $translate.instant(placeholder) + (criterionHelper("requiredMark") || "");

          /**
           * Tell assistive technology that the focusable inputs of the plugin (the focus catcher of a single select and the
           * search input of a multiple one) are required
           * @param {jQuery} container Plugin container
           */
          const markRequired = (container) => {
            const required = criterionHelper("ariaRequired");
            if (container && required) {
              container.find(".select2-focusser, .select2-input").attr("aria-required", required);
            }
          };

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
                opts.placeholder = getPlaceholder(opts.placeholder);
              }
              if (opts.summary) {
                opts.containerCssClass = SUMMARY_CLASS;
              }
              // Test hooks: wrap the formatters (default markup is preserved) and tag the plugin DOM
              opts.formatResult = wrapSelect2FormatResult(opts.formatResult, getOwner);
              opts.formatSelection = wrapSelect2FormatSelection(opts.formatSelection, getOwner);
              let plugin = elem.select2(opts);
              testIds = bindSelect2TestIds(elem, getOwner);
              markRequired(elem.data("select2") ? elem.select2("container") : undefined);
              if (opts.summary) {
                summary = startSummary();
              }

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
           * Show the values of a select multiple as one line: the chips that fit and a counter with the rest (#922)
           * @returns {{refresh: function, destroy: function}} Summary
           */
          function startSummary() {
            const container = elem.select2("container");
            const summarized = attachSelectSummary({
              container,
              translate: (key, params) => $translate.instant(key, params),
              // Through the data of the chips: a suggest cannot set its values by id, it has no options to find them in (#845)
              remove: (index) => {
                const values = elem.select2("data");
                values.splice(index, 1);
                elem.select2("data", values, true);
              }
            });
            // select2 draws the chips again after every change of the values; the size of the field may change too
            const observers = [new MutationObserver(summarized.refresh)];
            observers[0].observe(container.find(".select2-choices")[0], {childList: true});
            if (typeof window.ResizeObserver !== "undefined") {
              observers.push(new window.ResizeObserver(summarized.refresh));
              observers[1].observe(container[0]);
            }
            summarized.refresh();
            return {
              refresh: summarized.refresh,
              destroy: () => {
                observers.forEach(observer => observer.disconnect());
                summarized.destroy();
              }
            };
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
              select2.opts.placeholder = getPlaceholder(opts.placeholder);
              select2.setPlaceholder?.();
            }
            summary?.refresh();
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
              summary?.destroy();
              summary = null;
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
