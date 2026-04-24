import {aweApplication} from "../awe";
import "../directives/plugins/uiSelect";
import {getIconTemplate} from "./component";
import {
  ensureSuggestVisibleOptions,
  filterSuggestModel as normalizeSuggestSelection,
  normalizeSelectorLegacyModel,
  normalizeSuggestLegacyModel,
} from "./selector/selectorNormalizer";
import {
  getSelectorOptionKey,
  mergeUniqueSelectorOptions,
  selectorValueEquals,
} from "./selector/selectorIdentity";
import {
  asSelectedArray,
  formatSelectedValues,
  getSelectedKeyList,
} from "./selector/selectorSelection";
import {
  applySelectorModelSlice,
  syncSelectorOptionState,
} from "./selector/selectorModelState";

// Selector template
export const templateSelector =
`<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" ng-cloak title="{{controller.title| translateMultiple}}">
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="validator input {{::validatorGroup}} focus-target" ng-class="{'input-group': controller.unit}">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="hidden" ui-select2="aweSelectOptions" class="form-control {{classes}}" initialized="initialized" autocomplete="off" ng-click="click($event)"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" ng-disabled="controller.readonly"/>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak/>
      <span ng-if="controller.unit" class="input-group-addon add-on unit" translate-multiple="{{controller.unit}}" ng-cloak></span>
    </div>
  </div>
</div>`;

// Selector template for columns
export const templateSelectorColumn =
`<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate {{component.model.values[0].style}}" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition" title="{{component.model.values[0].title| translateMultiple}}">
    <div class="input input-group-{{::size}} focus-target">
      <input type="hidden" ui-select2="aweSelectOptions" class="form-control col-xs-12 {{classes}}" value="{{component.model.selected}}"
             ng-disabled="component.controller.readonly" initialized="initialized" autocomplete="off"/>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak/>
</div>`;

/**
 * Normalize selected values to plain value list
 * @param {Array|Object|String|Number} selected Selected values
 * @return {Array} selected values as strings
 */
function normalizeSelectedValues(selected) {
  return getSelectedKeyList(selected);
}

/**
 * Ensure multiple criteria data is always an array
 * @param {object} component
 */
function ensureMultipleGetData(component) {
  if (!component._multipleGetData) {
    let baseGetData = component.getData;
    component.getData = function () {
      let data = baseGetData();
      data[component.address.component] = formatSelectedValues(component.model.selected);
      return data;
    };
    component._multipleGetData = true;
  }
}

/**
 * Normalize suggest model state
 * @param {object} model
 */
function filterSuggestModel(model) {
  let normalizedModel = normalizeSuggestSelection(model);
  syncSelectorOptionState(model, normalizedModel);
}

// Selector service
aweApplication.factory('Selector',
  ['Control', 'Criterion', '$translate', 'AweUtilities', 'AweSettings', 'ActionController',
    /**
     * @constructor Criterion generic methods
     * @param {object} Control
     * @param {function} Criterion
     * @param {object} $translate
     * @param {object} Utilities Awe Utilities
     * @param {object} $settings Awe $settings
     * @param {object} $actionController Action controller
     */
    function (Control, Criterion, $translate, Utilities, $settings, $actionController) {
      /**
       * Format select data
       * @param {Array} values
       * @param {Array} term Term to filter
       * @return {Array} formatted data
       */
      function formatSelectData(values, term) {
        let data = [];
        _.each(values, function (element) {

          // Search for term if defined
          let search = String(term || "").toUpperCase();
          let value = String(element.value || "");
          let label = String(element.label || "");
          let translatedLabel = $translate.instant(label.toString());

          // Add element to select data
          if (value.toUpperCase().indexOf(search) !== -1 || translatedLabel.toUpperCase().indexOf(search) !== -1) {
            data.push({
              id: element.value,
              text: translatedLabel
            });
          }
        });
        return data;
      }

      /**
       * Format select data
       * @param {Array} values Model values
       * @param {Array} selected Model selected values
       * @return {Array} formatted data
       */
      function filterSelectData(values, selected) {
        let data = [];
        if (selected !== null) {
          let selectedList = normalizeSelectedValues(selected);
          _.each(values, function (element) {
            if ($.inArray(getSelectorOptionKey(element), selectedList) > -1) {
              data.push({
                id: element.value,
                text: $translate.instant(String(element.label))
              });
            }
          });
        }
        return data;
      }

      /**
       * Clear selected only when current values prove it is invalid
       * @param {object} model Component model
       * @param {Array} data Retrieved selected option data
       */
      function clearSelectedIfResolvedAndMissing(model, data) {
        if (data.length === 0 && model.values.length > 0) {
          model.selected = null;
        }
      }
      /**
       * Fill a callback object (in select2 format) with a model value list
       * @param {Array} values
       * @param {Object} callback Callback function
       * @param {String} term Search term
       */
      function fillCallback(values, callback, term) {
        let results = formatSelectData(values, term);
        let data = {
          results: results
        };
        callback(data);
      }

      /**
       * Fill a callback object (in select2 format) with a model value list
       * filtered by selected data
       * @param {Array} values All values
       * @param {Array} selected Selected values
       * @param {Object} callback Callback function
       * @return {Object} Data retrieved
       */
      function filterSelectedCallback(values, selected, callback, multiple) {
        let data = filterSelectData(values, selected);
        if (multiple) {
          callback(data);
        } else if (data.length === 1) {
          callback(data[0]);
        } else {
          callback(data);
        }
        return data;
      }

      /**
       * Check if selected is in value list
       * @param {object} component
       * @return {boolean} selected is in value list
       */
      function checkSelectedValues(component) {
        let check = false;
        let model = Control.getAddressModel(component.address);
        let selected = normalizeSelectedValues(model.selected)[0];
        if (!Utilities.isNull(selected)) {
          _.each(model.values, function (value) {
            if (selectorValueEquals(selected, value)) {
              check = true;
            }
          });
        }
        return check;
      }

      /**
       * Select data in select2 format
       * @param {Array} selected Model
       * @param {Array} valueList Model
       * @return {Array} label list
       */
      function getSelectedLabel(selected, valueList) {
        let selectedList = normalizeSelectedValues(selected);
        let labelList = [];
        _.each(selectedList, function (selectedValue) {
          _.each(valueList, function (value) {
            if (selectorValueEquals(selectedValue, value)) {
              labelList.push($translate.instant(String(value.label)));
            }
          });
        });
        return labelList;
      }

      /**
       * Normalize multiple plugin values to the selector contract
       * @param {object} model Component model
       * @param {Array|String|Number|Object} value Raw plugin value
       * @return {Array} normalized selected value list
       */
      function normalizeMultiplePluginValue(model, value) {
        if (Array.isArray(value)) {
          return value.filter(item => item !== "");
        }

        if (typeof value !== "string") {
          return Utilities.asArray(value).filter(item => item !== "");
        }

        const selectableValues = _.uniq(
          [...Utilities.asArray(model.storedValues), ...Utilities.asArray(model.values)]
            .filter(item => _.isPlainObject(item) && Object.prototype.hasOwnProperty.call(item, "value"))
            .map(getSelectorOptionKey)
        );

        if (selectableValues.includes(value)) {
          return [value];
        }

        const splitValues = value.split(",").filter(item => item !== "");
        if (splitValues.length > 0 && splitValues.every(item => selectableValues.includes(item))) {
          return splitValues;
        }

        return [value];
      }

      /**
       * Process change event
       *
       * @param {Object} component
       * @param {Object} item item to add/remove
       */
      function processChangeEvent(component, item) {
        // Check value length (of string / array)
        let model = Control.getAddressModel(component.address);
        if (item.val && item.val.length) {
          // Always return array for multiple components
          if (component.multiple) {
            model.selected = normalizeMultiplePluginValue(model, item.val);
          } else {
            model.selected = item.val;
          }
        } else {
          model.selected = null;
        }
        component.modelChange();
      }

      /**
       * Check if selected is in value list
       * @param {Object} component
       * @param {boolean} multiple
       */
      function fixRequiredValue(component, multiple) {
        // If not optional set first value as selected
        let model = Control.getAddressModel(component.address);
        if (!multiple && !component.controller.optional && model.values.length) {
          if (model.selected === null || !checkSelectedValues(component)) {
            model.selected = model.values[0].value;
            model["initial-selected"] = _.cloneDeep(model.selected);
            Utilities.timeout(function () {
              component.modelChange();
            });
          }
        }
      }

      /**
       * Selector constructor
       * @param {Scope} scope Selector scope
       * @param {String} id Selector id
       * @param {String} element Selector element
       */
      function Selector(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Criterion(this.scope, this.id, this.element);
        this.initialized = false;
        let selector = this;
        this.component.asSelect = function () {
          selector.multiple = false;
          return selector.asSelect();
        };
        this.component.asSuggest = function (initializable) {
          selector.multiple = false;
          selector.initializable = initializable;
          return selector.asSuggest();
        };
        this.component.asSelectMultiple = function () {
          selector.multiple = true;
          return selector.asSelect();
        };
        this.component.asSuggestMultiple = function (initializable) {
          selector.multiple = true;
          selector.initializable = initializable;
          return selector.asSuggest();
        };
        return this.component;
      }

      Selector.prototype = {
        /**
         * Initialize selector
         */
        asSelect: function () {
          // Initialize criterion
          let selector = this;
          let component = this.component;
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }
          if (selector.multiple) {
            component.multiple = selector.multiple;
            ensureMultipleGetData(component);
          }

          /*
           * Set select2 options
           */
          let options = {
            allowClear: component.controller && component.controller.optional,
            minimumResultsForSearch: 5,
            multiple: selector.multiple,
            query: function (query) {
              let model = Control.getAddressModel(component.address);
              fillCallback(model.values, query.callback, query.term);
            },
            initSelection: function (node, callback) {
              let model = Control.getAddressModel(component.address);
               let data = filterSelectedCallback(model.values, model.selected, callback, selector.multiple);
              clearSelectedIfResolvedAndMissing(model, data);
            }
          };
          /**
           * Retrieves visible value for the selector
           * @returns {string} visible value
           */
          component.getVisibleValue = function () {
            let model = Control.getAddressModel(component.address);
            return getSelectedLabel(model.selected, model.values).join(", ");
          };
          /**
           * Update the model on model changed
           */
          component.onModelChanged = function () {
            // Fix required value
            fixRequiredValue(component, selector.multiple);
            // Fill data
            selector.updateSelectValues(component);
          };
          /**
           * On plugin initialization
           */
          component.onPluginInit = function () {
            // Get plugin
            selector.initialized = true;
            // Fix required value
            component.onStart();
          };
          /**
           * On plugin initialization
           */
          component.onStart = function () {
            // Fix required value
            fixRequiredValue(component, selector.multiple);
            selector.updateSelectValues(component);
          };
          /**
           * On plugin change
           * @param {object} item
           */
          component.onPluginChange = function (item) {
            processChangeEvent(component, item);
          };
          /**********************************************************************/
          /* API METHODS                                                        */
          /**********************************************************************/
          /**
           * Update model values
           * @param {type} data
           */
          component.api.updateModelValues = function (data) {
            let model = Control.getAddressModel(component.address);
            if (model) {
              let normalizedModel = normalizeSelectorLegacyModel(model, data, {
                multiple: selector.multiple,
                stringifySingle: !selector.multiple && !component.controller.serverAction,
              });

              applySelectorModelSlice(model, normalizedModel);

              if ("selected" in data) {
                selector.selectData(model.selected);
              }

              // Store updated model
              Control.setAddressModel(component.address, model);
            }
          };
          /******************************************************************************
           * EVENT LISTENERS
           *****************************************************************************/
          component.listeners = component.listeners || {};
          // Action listener definition
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, service: component, method: "onModelChanged"});
          // Watch for model values change
          component.listeners['editingCell'] = component.scope.$on("editing-cell", function (event, address) {
            if (_.isEqual(address, component.address)) {
              component.onStart();
            }
          });
          // Watch for language change
          component.listeners['languageChanged'] = component.scope.$on('languageChanged', function () {
            selector.updateSelectValues(component);
            // Update visible value on language change (if defined)
            if (component.updateVisibleValue) {
              component.updateVisibleValue();
            }
          });
          // Placeholder
          component.controller.placeholder = component.controller.placeholder || 'SELECT2_SELECT_VALUE';
          options.placeholder = component.controller.placeholder;
          // Define first option as placeholder place
          options.placeholderOption = 'first';
          // Store options
          selector.scope.aweSelectOptions = options;
          // Finish initialization
          return true;
        },
        /**
         * Initialize suggest
         */
        asSuggest: function () {
          // Define searchQuery for model changes
          let searchCallback = null, initCallback = null;
          let timer = null;
          // Initialize criterion
          let selector = this;
          let component = this.component;
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }
          if (selector.multiple) {
            component.multiple = selector.multiple;
            ensureMultipleGetData(component);
          }

          // Retrieve attributes from component
          selector.term = component.model.selected || "";
          // Generate target and initial target
          selector.target = component.controller[$settings.get("targetActionKey")];
          selector.initialTarget = component.controller.checkTarget || selector.target;
          /*
           * Set select2 options
           */
          let options = {
            allowClear: true,
            multiple: selector.multiple,
            minimumInputLength: 1,
            query: function (query) {
              // Remove timer
              searchCallback = null;
              Utilities.timeout.cancel(timer);
              // Abort last action if alive
              if (selector.lastAction) {
                $actionController.abortAction(selector.lastAction);
              }
              timer = Utilities.timeout(function () {
                searchCallback = query.callback;
                selector.term = Utilities.trim(query.term);
                component.suggest(selector.target);
              }, $settings.get("suggestTimeout"));
            },
            initSelection: function (node, callback) {
              let model = Control.getAddressModel(component.address);
              let data = filterSelectedCallback(model.values, model.selected, callback, selector.multiple);
              clearSelectedIfResolvedAndMissing(model, data);
            }
          };
          // Fix the selected value so that it always returns an array
          if (selector.multiple) {
            component.model.selected = Utilities.asArray(component.model.selected);
          }
          // Strict value method
          if (component.controller && !component.controller.strict) {
            options.createSearchChoice = function (term) {
              let trimmedTerm = $.trim(term);
              let output = null;
              if (trimmedTerm.length > 0) {
                let newValue = {value: trimmedTerm, label: trimmedTerm, __adHoc: true};
                component.model.values.push(newValue);
                component.model.values = mergeUniqueSelectorOptions(component.model.values);
                output = {id: trimmedTerm, text: trimmedTerm};
              }
              return output;
            };
            options.createSearchChoicePosition = "bottom";
          }

          /**
           * Launch a suggest
           * @param {type} target
           * @returns {undefined}
           */
          component.suggest = function (target) {
            component.controller[$settings.get("targetActionKey")] = target;
            selector.lastAction = component.reload();
          };
          /**
           * Retrieves visible value for the selector
           * @returns {string} visible value
           */
          component.getVisibleValue = function () {
            let model = Control.getAddressModel(component.address);
            return getSelectedLabel(model.selected, model.values).join(", ");
          };
          /**
           * Basic getSpecificFields function (To be overwritten on complex directives)
           * @returns {Object} Specific fields from component
           */
          component.getSpecificFields = function () {
            // Initialize data
            return {
              max: component.getMax(0),
              suggest: Control.formatDataList(selector.term)
            };
          };
          /**
           * Update the model when model and selected have changed
           */
          component.onModelChangedValuesSelected = function () {
            // Fill data
            let model = Control.getAddressModel(component.address);
            selector.selectData(asSelectedArray(model.selected));
            filterSuggestModel(model);
          };
          /**
           * Update the model on model changed
           */
          component.onModelChangedValues = function () {
            // Fill suggest
            let model = Control.getAddressModel(component.address);
            if (searchCallback !== null) {
              fillCallback(model.values, searchCallback, selector.term);
              searchCallback = null;
              // Fill data
            } else if (initCallback !== null) {
               let data = filterSelectedCallback(model.values, model.selected, initCallback, selector.multiple);
              clearSelectedIfResolvedAndMissing(model, data);
              initCallback = null;
            } else {
              filterSuggestModel(model);
              let visibleModel = ensureSuggestVisibleOptions(model, {strict: component.controller.strict});
              syncSelectorOptionState(model, visibleModel);

              selector.selectData(asSelectedArray(model.selected));
            }
          };
          /**
           * Check if selected value has a value list to fill the selector
           * @param {object} model Model
           * @returns {Boolean}
           */
          function checkSelectedValue(model) {
            let check = false;
            if (model.values.length === 0 && !Utilities.isEmpty(model.selected)) {
              selector.term = model.selected;
              component.suggest(selector.initialTarget);
              check = true;
            }
            return check;
          }

          /**
           * Update the model on selected changed
           */
          component.onModelChangedSelected = function () {
            let model = Control.getAddressModel(component.address);
            // Important: Filter first to avoid non useful values, and after that,
            // select the data to launch callback if needed
            filterSuggestModel(model);
            if (!checkSelectedValue(model)) {
              selector.selectData(asSelectedArray(model.selected));
            }
          };
          /**
           * On plugin initialization
           */
          component.onPluginInit = function () {
            // Get plugin
            selector.initialized = true;
            // Start
            component.onStart();
          };
          /**
           * On plugin start
           */
          component.onStart = function () {
            // Fill data
            let model = Control.getAddressModel(component.address);
            if (!checkSelectedValue(model)) {
              selector.selectData(asSelectedArray(model.selected));
              filterSuggestModel(model);
            }
          };
          /**
           * On plugin change
           * @param {object} item
           */
          component.onPluginChange = function (item) {
            processChangeEvent(component, item);
          };
          /**
           * Update model values
           * @param {type} data
           */
          component.api.updateModelValues = function (data) {
            let model = Control.getAddressModel(component.address);
            if (model) {
              let normalizedModel = normalizeSuggestLegacyModel(model, data, {
                multiple: selector.multiple,
                strict: component.controller.strict,
              });

              applySelectorModelSlice(model, normalizedModel);

              if ("selected" in data) {
                if (model.shouldReload) {
                  checkSelectedValue(model);
                } else {
                  selector.selectData(model.selected);
                }
              }

              Control.setAddressModel(component.address, model);
            }
          };

          // Initialization (for suggest on columns)
          let defaultModel = Control.getAddressModel(component.address);
          if (checkSelectedValue(defaultModel)) {
            // Store options (on
            let initialModelChanged = component.scope.$on("modelChanged", function (event, launchers) {
              let changes = Utilities.modelChanged(component, launchers);
              if (changes) {
                if (selector.initializable) {
                  component.scope.initialized = true;
                }
                // Plugin initialization (retarded)
                initialModelChanged();
              }
            });
          } else {
            if (selector.multiple) {
              filterSuggestModel(defaultModel);
            }
            if (selector.initializable) {
              component.scope.initialized = true;
            }
          }

          /******************************************************************************
           * EVENT LISTENERS
           *****************************************************************************/
          component.listeners = component.listeners || {};
          // Model changed listeners
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, check: ["values", "selected"], service: component, method: "onModelChangedValuesSelected"});
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, check: ["values"], service: component, method: "onModelChangedValues"});
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, check: ["selected"], service: component, method: "onModelChangedSelected"});
          // Placeholder
          component.controller.placeholder = component.controller.placeholder || 'SELECT2_SEARCH_VALUE';
          options.placeholder = component.controller.placeholder;
          // Store options
          selector.scope.aweSelectOptions = options;
          // Finish initialization
          return true;
        },
        /**
         * Update select values
         * @param {type} component
         */
        updateSelectValues: function (component) {
          // Update select
          let model = Control.getAddressModel(component.address);
          this.fillData(model.values);
          this.selectData(model.selected);
        },
        /**
         * Fill a select2 element (in select2 format) with a model value list
         * @param {Array} values Model
         */
        fillData: function (values) {
          if (this.initialized) {
            let data = formatSelectData(values, null);
            this.component.fill(data);
          }
        },
        /**
         * Select data in select2 format
         * @param {Array} selected Model
         */
        selectData: function (selected) {
          if (this.initialized) {
            this.component.select(selected);
          }
        }
      };
      return Selector;
    }
  ]);
