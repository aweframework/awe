import {aweApplication} from "../awe";

// Control service
aweApplication.factory('Control',
  ['AweUtilities', 'Storage', '$log',
    /**
     * General control methods
     * @param {AweUtilities} Utilities
     * @param {Storage} Storage
     * @param {$log} $log Log service
     */
    function (Utilities, Storage, $log) {

      // Storage constants
      let  INITIAL = "initial-";
      let  MODEL = "model";
      let  CONTROLLER = "controller";
      let  API = "api";

      // Model constants
      let  SELECTED = "selected";
      let  PREVIOUS = "previous";

      // Address constants
      let  VIEW = "view";
      let  COMPONENT = "component";
      let  COLUMN = "column";
      let  ROW = "row";

      function getCellTarget(action, address, view, component, target) {
        if (Storage.has(action)) {
          let storedAction = Storage.get(action);
          // Retrieve cell id
          let cellId = Utilities.getCellId(address);
          if (view in storedAction && component in storedAction[view] && cellId in storedAction[view][component].cells) {
            target = storedAction[view][component].cells[cellId];
          }
        }
        return target;
      }

      function getViewAndComponentTarget(action, view, component, target) {
        // Normal component
        if (Storage.has(action)) {
          let storedAction = Storage.get(action);
          if (view in storedAction && component in storedAction[view]) {
            target = storedAction[view][component];
          }
        }
        return target;
      }

      function getComponentTarget(action, component, target) {
        if (Storage.has(action)) {
          let storedAction = Storage.get(action);
          // Normal component (no view)
          _.each(storedAction, function (actionView) {
            if (component in actionView) {
              target = actionView[component];
            }
          });
        }
        return target;
      }

      let  Control = {
        /**
         * Retrieve an address target
         * @param {Object} address
         * @return {Object} Address type
         */
        getAddressType: function (address) {
          let  addressType;
          if (address && VIEW in address && COMPONENT in address && COLUMN in address && ROW in address) {
            addressType = "cell";
          } else if (address && VIEW in address && COMPONENT in address) {
            addressType = "viewAndComponent";
          } else if (address && COMPONENT in address) {
            addressType = "component";
          } else {
            addressType = "invalid";
          }
          return addressType;
        },
        /**
         * Retrieve an address target
         * @param {Object} address
         * @param {string} action Action to operate
         * @return {Object} controller/model
         */
        getTarget: function (address, action) {
          let  target = null;
          // Check if address, action, view and component exists in both checks
          let view = address && VIEW in address ? address[VIEW] : null;
          let component = address && COMPONENT in address ? address[COMPONENT] : null;
          switch (Control.getAddressType(address)) {
            case "cell":
              target = getCellTarget(action, address, view, component, target);
              break;
            case "viewAndComponent":
              target = getViewAndComponentTarget(action, view, component, target);
              break;
            case "component":
              target = getComponentTarget(action, component, target);
              break;
            default:
              break;
          }
          return target;
        },
        /**
         * Store an address target
         * @param {Object} address
         * @param {string} action Action to operate
         * @param {string} value Value to set
         * @return {Object} controller/model
         */
        setTarget: function (address, action, value) {
          let target = null;
          const {view, component} = address || {};

          // If storage is not in action, return null;
          if (!Storage.has(action)) return null;
          let storedAction = Storage.get(action);

          // Check if address, action, view and component exists in both checks
          switch (Control.getAddressType(address)) {
            case "cell":
              // Retrieve cell id
              let cellId = Utilities.getCellId(address);
              let cells = storedAction[view][component].cells || {};
              cells[cellId] = value;
              target = cells[cellId];
              break;
            case "viewAndComponent":
              // Normal component
              storedAction[view] = storedAction[view] || {};
              storedAction[view][component] = value;
              target = storedAction[view][component];
              break;
            default:
              break;
          }
          return target;
        },

        /**
         * Check if model has changed compare to the initial model
         * @param {String} view
         * @return {boolean} true if have changed | false
         */
        checkModelChanged: function (view) {
          let  changes = false;
          let  model = Storage.get(MODEL);
          let  initialValue = INITIAL + SELECTED;
          // Get model of view
          if (view in model) {
            let  modelView = model[view];
            // Compare each selected values of element
            _.each(modelView, function (modelValue) {
              if (initialValue in modelValue && !_.isEqual(modelValue[SELECTED], modelValue[initialValue])) {
                changes = true;
              }
            });
          }
          return changes;
        },
        /**
         * Check if selected values in model are null
         * @param {String} view
         * @return {boolean} true if all null | false
         */
        checkModelEmpty: function (view) {
          let  empty = true;
          let  model = Storage.get(MODEL);
          let  controller = Storage.get(CONTROLLER);

          // Get model of view
          if (view in model) {
            let  modelView = model[view];
            let  controllerView = controller[view];
            // Compare each selected values of element
            _.each(modelView, function (modelValue, componentId) {
              if (componentId in controller &&
                controllerView[componentId].criterion &&
                !angular.equals(modelView[componentId].selected, null)) {
                empty = false;
              }
            });
          }
          return empty;
        },
        /**
         * Add message to scope
         * @param {string} view
         * @param {string} messageId (Message Id)
         * @param {object} message Message to add
         */
        addMessageToScope: function (view, messageId, message) {
          let  messages = Storage.get("messages");
          messages[view][messageId] = message;
        },
        /**
         * Get message from scope
         * @param {string} view
         * @param {string} messageId (Message Id)
         */
        getMessageFromScope: function (view, messageId) {
          let  messages = Storage.get("messages");
          return messages[view][messageId];
        },
        /**
         * Publish model changed for the scope
         * @param {Object} address Target address
         * @param {Object} changes
         */
        publishModelChanged: function (address, changes) {
          let  launchers = {};
          let  launcherId = Utilities.getAddressId(address);
          launchers[launcherId] = changes;
          Control.publish("modelChanged", launchers);
        },
        /**
         * Publish model changed for the scope
         * @param {Object} address Target address
         * @param {Object} changes
         */
        publishControllerChanged: function (address, changes) {
          Control.publish("controllerChange", {address: address, controller: changes});
        },
        /**
         * Check if component has definition
         * @param {Object} address
         * @return {boolean} Component has definition
         */
        checkComponent: function (address) {
          return Control.getTarget(address, CONTROLLER) !== null;
        },
        /**
         * Check if component has definition
         * @param {Object} address
         * @return {boolean} Component has definition
         */
        checkOnlyComponent: function (address) {
          return Control.getTarget({view: address.view, component: address.component}, CONTROLLER) !== null;
        },
        /**
         * Fix the selected attribute so that it allways return an array
         * @param {Object} scope
         */
        fixMultipleSelectedValue: function (scope) {
          scope.model.selected = Utilities.asArray(scope.model.selected);
        },
        /**
         * Retrieve an address controller
         * @param {Object} address
         * @return {Object} controller
         */
        getAddressController: function (address) {
          return Control.getTarget(address, CONTROLLER) || {actions: []};
        },
        /**
         * Store an address controller
         * @param {Object} address
         * @param {Object} value
         */
        setAddressController: function (address, value) {
          return Control.setTarget(address, CONTROLLER, value);
        },
        /**
         * Retrieve an address controller
         * @param {Object} address
         * @return {Object} controller
         */
        getAddressViewController: function (address) {
          let  controller = Storage.get(CONTROLLER);
          return controller[address[VIEW]];
        },
        /**
         * Retrieve an address model
         * @param {Object} address
         * @return {Object} model
         */
        getAddressViewModel: function (address) {
          let  model = Storage.get(MODEL);
          return model[address[VIEW]];
        },
        /**
         * Retrieve an address api
         * @param {Object} address
         * @return {Object} api
         */
        getAddressViewApi: function (address) {
          let  api = Storage.get(API);
          return api[address[VIEW]];
        },
        /**
         * Retrieve an address for api
         * @param {Object} address
         * @return {Object} api
         */
        getAddressApi: function (address) {
          return Control.getTarget(address, API) || {};
        },
        /**
         * Store an address controller
         * @param {Object} address
         * @param {Object} value
         */
        setAddressApi: function (address, value) {
          return Control.setTarget(address, API, value);
        },
        /**
         * Retrieve an address model
         * @param {Object} address
         * @return {Object} model
         */
        getAddressModel: function (address) {
          return Control.getTarget(address, MODEL) || {selected: null, values: []};
        },
        /**
         * Store an address model
         * @param {Object} address
         * @param {Object} value
         * @return {Object} model
         */
        setAddressModel: function (address, value) {
          return Control.setTarget(address, MODEL, value);
        },
        /**
         * Format an array of data into readable data for the server
         * @param {object} data Data array
         * @return {mixed} formatted data
         */
        formatDataList: function (data) {
          let  formattedData;
          let  dataList = Utilities.asArray(data);
          switch (dataList.length) {
            case 0:
              formattedData = null;
              break;
            case 1:
              formattedData = Utilities.isNull(dataList[0]) ? null : dataList[0];
              break;
            default:
              formattedData = dataList;
          }
          return formattedData;
        },
        /**
         * Change a controller attribute
         * @param {Object} address Component address
         * @param {Object} attributes Attributes to set
         */
        changeControllerAttribute: function (address, attributes) {
          let  controller = Control.getAddressController(address);
          if (controller) {
            _.each(attributes, function (attribute, attributeId) {
              let  initialAttribute = INITIAL + attributeId;
              if (!(initialAttribute in controller)) {
                controller[initialAttribute] = _.cloneDeep(controller[attributeId]);
              }
              controller[attributeId] = attribute;
            });

            // Publish controller change
            Control.publishControllerChanged(address, attributes);
          }
        },
        /**
         * Restore a controller attribute
         * @param {Object} address Component address
         * @param {String} attribute Attribute to restore
         */
        restoreControllerAttribute: function (address, attribute) {
          let  controller = Control.getAddressController(address);
          let  initialAttribute = INITIAL + attribute;
          if (controller) {
            if (initialAttribute in controller) {
              controller[attribute] = _.cloneDeep(controller[initialAttribute]);
            }
            // Publish controller change
            let  changes = {};
            changes[attribute] = controller[attribute];
            Control.publishControllerChanged(address, changes);
          }
        },
        /**
         * Change model value
         * @param {Object} address Component address
         * @param {Object} attributes Model attributes
         * @param {Boolean} publish Publish model changed
         */
        changeModelAttribute: function (address, attributes, publish) {
          let  model = Control.getAddressModel(address);
          if (model) {
            _.each(attributes, function (attribute, attributeId) {
              let  initialAttribute = INITIAL + attributeId;
              if (!(initialAttribute in model) && Storage.get("status")[address.view] === "loaded") {
                if (attributeId === SELECTED) {
                  model[initialAttribute] = _.cloneDeep(model[PREVIOUS]);
                } else {
                  model[initialAttribute] = _.cloneDeep(attribute);
                }
              }

              // Store previous value if selected has changed
              if (attributeId === SELECTED) {
                model[PREVIOUS] = _.cloneDeep(attribute);
              }
            });

            // Copy attributes to model
            Control.launchApiMethod(address, "updateModelValues", [attributes]);

            // Publish model change
            if (publish) {
              Control.publishModelChanged(address, attributes);
            }
          }
        },
        /**
         * Restore model value
         * @param {Object} address Component address
         * @param {String} attribute Model attribute
         */
        restoreModelAttribute: function (address, attribute) {
          let  model = Control.getAddressModel(address);
          // Publish model change
          if (model) {
            let  attributes = {};
            attributes[attribute] = model.defaultValues;
            Control.changeModelAttribute(address, attributes, true);
          }
        },
        /**
         * Restore radio model value
         * @param {Object} address Component address
         * @param {String} attribute Model attribute
         */
        restoreInitialModelAttribute: function (address, attribute) {
          let  model = Control.getAddressModel(address);
          let  initialAttribute = INITIAL + attribute;
          if (model) {
            if (initialAttribute in model) {
              model[attribute] = _.cloneDeep(model[initialAttribute]);
            }
            // Publish controller change
            let  changes = {};
            changes[attribute] = model[attribute];
            Control.changeModelAttribute(address, changes, true);
          }
        },
        /**
         * Reset model attribute
         * @param {Object} address Component address
         * @param {String} attribute Model attribute
         */
        resetModelAttribute: function (address, attribute) {
          let  attributes = {};
          attributes[attribute] = null;
          Control.changeModelAttribute(address, attributes, true);
        },
        /**
         * Change view model
         * @param {String} view Scope view
         * @param {object} modelView New model
         * @param {object} publish Publish the change
         */
        changeViewModel: function (view, modelView, publish) {
          let  model = Storage.get(MODEL);
          model[view] = modelView;
          if (publish) {
            Control.publish("modelChanged", model[view]);
          }
        },
        /**
         * Destroy all views
         */
        destroyAllViews: function () {
          let  controls = [MODEL, CONTROLLER, API];
          _.each(controls, function (control) {
            Storage.put(control, {});
          });
        },
        /**
         * Retrieve a controller attribute value
         * @param {Object} address Component address
         * @param {String} attribute Attribute to get
         */
        getControllerAttribute: function (address, attribute) {
          let  controller = Control.getAddressController(address);
          let  value = null;
          if (controller) {
            value = controller[attribute];
          }
          return value;
        },
        /**
         * Retrieve a model attribute value
         * @param {Object} address Component address
         * @param {String} attribute Attribute to get
         */
        getModelAttribute: function (address, attribute) {
          let  model = Control.getAddressModel(address);
          let  value = null;
          if (model) {
            value = model[attribute];
          }
          return value;
        },
        /**
         * Launch a method in api
         * @param {Object} address Component address
         * @param {String} method Method to call
         * @param {Array} parameters Method parameters (array)
         */
        launchApiMethod: function (address, method, parameters) {
          let  api = Control.getAddressApi(address);
          if (api && method in api) {
            api[method].apply(api, parameters);
          } else {
            $log.warn("[WARNING] Method '" + method + "' not found in api", {'address': address});
          }
        },
        /**
         * Broadcast a change
         * @param {Object} channel Channel to publish
         * @param {Object} parameters Parameters to send
         */
        publish: function (channel, parameters) {
          Utilities.publish(channel, parameters);
        }
      };
      return Control;
    }
  ]);