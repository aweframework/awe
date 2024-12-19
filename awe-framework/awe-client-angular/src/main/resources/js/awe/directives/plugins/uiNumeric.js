import {aweApplication} from "../../awe";
import "autonumeric";

// Numeric plugin
aweApplication.directive('uiNumeric',
  ['AweSettings', 'AweUtilities', 'Control',
    /**
     * jquery Autonumeric angular wrapper
     * @param {object} $settings
     * @param {object} Utilities
     * @param {object} Control
     */
    function ($settings, Utilities, Control) {
      // Declare a empty options object

      let options = _.cloneDeep($settings.get("numericOptions"));
      return {
        // This directive only works when used in element's attribute (e.g: ui-numeric)
        restrict: 'A',
        priority: 1,
        link: function (scope, elem, attrs) {

          /**
           * Helper method to update autoNumeric with new value.
           * @param {string} newVal New value
           * @param {{initialized: boolean, opts: *, elem, scope}} params
           */
          function updateNumericElement(newVal, params) {
            const {initialized, elem, opts} = params;
            // Only set value if value is numeric
            if (initialized) {
              if (newVal === null) {
                $(elem).val("");
              } else if ((opts.lZero === undefined || opts.lZero !== 'keep') && $.isNumeric(newVal)) {
                elem.autoNumeric('set', parseFloat(newVal));
              } else if ($.isNumeric(newVal)) {
                elem.autoNumeric('set', newVal);
              }
            }
          }

          /**
           * Update model values
           * @param {{initialized: boolean, opts: *, elem, scope}} params
           */
          function updateNumericModelValues(params) {
            const {initialized, scope, elem} = params;
            if (initialized) {
              let model = Control.getAddressModel(scope.component.address);

              // Update the model values
              model.values[0] = {
                value: model.selected,
                label: model.selected === null ? "" : elem.val()
              };
            }
          }

          /**
           * Update view value
           * @param {{initialized: boolean, opts: *, elem, scope}} params
           */
          function updateNumericModel(params) {
            const {initialized, scope} = params;
            if (initialized) {
              let model = Control.getAddressModel(scope.component.address);

              // Update the element
              updateNumericElement(Array.isArray(model.selected) && model.selected.length > 0 ? model.selected[0] : model.selected, params);

              // Update the model values
              updateNumericModelValues(params);
            }
          }

          /**
           * Process numeric options
           * @param options Object
           * @return Options processed
           */
          function processNumericOptions(options) {
            let optionsProcess = options;

            // Change attribute names
            if ("min" in options) {
              optionsProcess.vMin = options.min;
            }
            if ("max" in options) {
              optionsProcess.vMax = options.max;
            }
            if ("precision" in options) {
              optionsProcess.mDec = Math.floor(options.precision);
            }
            return optionsProcess;
          }

          /**
           * Destroy plugin
           */
          function destroyNumeric(listeners, params) {
            const {elem} = params;
            elem.off('change');
            params.initialized = false;

            // Clear listeners
            Utilities.clearListeners(listeners);
          }

          function onNumericChange(params) {
            const {scope, elem} = params;
            Utilities.timeout(() => {
              let model = Control.getAddressModel(scope.component.address);
              model.selected = parseFloat(elem.autoNumeric('get'));
              updateNumericModelValues(params);
              scope.component.modelChange();
            });
          }


          // Read options
          const params = {initialized: false, opts: options, elem, scope};

          // Watch for numeric options changes
          let initWatch = scope.$watch(attrs.uiNumeric, initPlugin);

          /**
           * Plugin initialization
           * @param {object} newValues plugin parameters
           */
          function initPlugin(newValues) {
            if (newValues) {
              // Initialize element as autoNumeric with options
              params.opts = _.merge({}, options, newValues);

              // Process numeric options
              params.opts = processNumericOptions(params.opts);

              if (params.initialized) {
                params.elem.autoNumeric('update', params.opts);
              } else {
                // Set autonumeric
                params.elem.autoNumeric(params.opts);
                params.initialized = true;

                // Update the model
                updateNumericModel(params);

                // Bind change event
                elem.on("change", () => onNumericChange(params));

                /**
                 * Component method links
                 */
                scope.component.updateModel = () => updateNumericModel(params);

                /**
                 * API Links
                 */
                if (scope.component.api) {
                  /**
                   * API link to update the model values
                   * @param {object} data New model data attributes
                   */
                  scope.component.api.updateModelValues = function (data) {
                    let model = Control.getAddressModel(scope.component.address);
                    if (model) {
                      _.merge(model, data);
                      updateNumericModel(params);
                    }
                  };
                }

                // Unwatch initialization
                initWatch();
              }
            }
          }

          /**
           * Event listeners
           */
          let listeners = {};

          // On number format change launch dependency
          listeners["updateNumberFormat"] = params.scope.$on("updateNumberFormat", function (event, parameters) {
            if (params.initialized) {
              initPlugin(parameters);
              updateNumericModelValues(params);
              params.scope.$emit("visibleValue");
            }
          });

          // Observe destroy event
          elem.on("$destroy", () => destroyNumeric(listeners, params));
        }
      };
    }
  ]);
