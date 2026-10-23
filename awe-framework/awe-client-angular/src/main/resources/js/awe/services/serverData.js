import {aweApplication} from "../awe";
import _ from "lodash";
import {getRowDetailInstanceData} from "./grid/rowDetail";

// Server data service
aweApplication.factory('ServerData',
  ['Connection', '$log', 'Storage', 'AweSettings', 'ActionController', 'Control', '$templateCache', 'AweUtilities',
    /**
     *
     * @param {object} Connection
     * @param {object} $log
     * @param {object} Storage
     * @param {object} $settings
     * @param {object} ActionController
     * @param {object} Control
     * @param {object} $templateCache
     * @param {object} $utilities
     */
    function (Connection, $log, Storage, $settings, ActionController, Control, $templateCache, $utilities) {
      let ServerData = {
        /**
         * Retrieve a screen template code
         * @param {String} screen Screen name
         * @param {String} view Screen view
         * @returns {String} Screen template
         */
        getScreenData: function (screen, view) {
          let parameters = {
            ...ServerData.getFormValues(),
            view
          };
          let screenToRetrieve = "initial screen";
          if (screen !== null) {
            parameters[$settings.get("optionKey")] = screen;
            screenToRetrieve = screen;
          }
          $log.info(`Retrieving screen data for ${screenToRetrieve}`);

          // Send ajax post
          return Connection.post(ServerData.getScreenDataUrl(screen), {...parameters, template: true}, "application/json")
              .then(screenData => {
                Storage.put("screenData-" + view, screenData.data);
                return screenData.data.template;
              })
              .catch(function (response) {
                const actions = $utilities.manageRestError({status: response?.status, title: response?.data?.title, message: response?.data?.message});
                ActionController.addActionList(actions, false, {address: {view: "base"}});
              });
        },
        /**
         * Store the loaded screen data
         * @param {Object} data Screen data
         * @param {String} view Screen view
         */
        storeScreenData: function (data, view) {
          let controller = Storage.get("controller");
          let messages = Storage.get("messages");
          let api = Storage.get("api");
          let screen = Storage.get("screen");

          // Store data
          if (data !== null) {
            // Generate screen data
            screen[view] = data.screen;

            // Store screen messages in scope
            messages[view] = data.messages;

            // Generate component api
            api[view] = {};
            controller[view] = {};
            let modelView = {};
            _.each(data.components, function (component) {
              ServerData.storeComponent(component, modelView, controller[view], api[view], screen[view].option);
            });

            // Change model
            Control.changeViewModel(view, modelView, false);

            // Store specific model (currentOption) for favourites
            if ("currentOption" in controller["base"]) {
              const option = data.screen.option;
              Control.changeModelAttribute({view: "base", component: "currentOption"}, {selected: option}, true);
            }

            // Launch actions if defined
            if ("actions" in data) {
              return ActionController.addActionList(data.actions, true, {address: {}, context: ""});
            }
          }
        },
        /**
         * Store a component in view
         * @param component Component
         * @param model View model
         * @param controller View controller
         * @param api View API
         * @param option Option
         */
        storeComponent: function (component, model, controller, api, option) {
          // Define model architecture
          let fixedModel = {values: [], selected: null, defaultValues: null};
          // Add model values
          if (component.model !== null) {
            _.each(component.model, function (value, attribute) {
              fixedModel[attribute] = value;
            });

            // A list sent with the screen (query already run, even with no rows) is a list whose data has arrived
            if ("records" in component.model) {
              fixedModel.loaded = true;
            }

            // Get selected values as list or single value
            if ("selected" in component.model) {
              fixedModel.selected = Control.formatDataList(component.model.selected);
              fixedModel.previous = _.cloneDeep(fixedModel.selected);
            }

            // Get selected values as list or single value
            if ("defaultValues" in component.model) {
              fixedModel.defaultValues = Control.formatDataList(component.model.defaultValues);
            }
          }

          // Store controller in view
          component.controller.optionId = option;
          controller[component.id] = component.controller;

          // Store model in view
          model[component.id] = fixedModel;

          // Pregenerate api for the component
          api[component.id] = {};
        },
        /**
         * Retrieve a screen data url
         * @param {String} screen Screen name
         * @returns {String} Screen template
         */
        getScreenDataUrl: function (screen) {
          let option = screen ? `/${screen}` : "";

          // Retrieve url
          return Connection.getRawUrl() + "/screen-data" + option;
        },
        /**
         * Retrieve a screen template code
         * @param {String} screen Screen name
         * @param {String} view Screen view
         * @param {String} random Random number to avoid cache
         * @returns {String} Screen template
         */
        getTemplateUrl: function (screen, view, random) {
          let template = "";
          // Add option
          if (!$utilities.isEmpty(screen)) {
            template = $utilities.generateEndpointUrl("template", "screen", view, screen);
          } else {
            template = $utilities.generateEndpointUrl("template", "screen");
          }

          // Retrieve url
          return Connection.getRawUrl() + template + (random ? "?r=" + random : "");
        },
        /**
         * Retrieve the help url for a screen
         * @param {String} option Option identifier
         * @returns {String} Help template url
         */
        getHelpUrl: function (option) {
          // Retrieve url
          return Connection.getRawUrl() + $utilities.generateEndpointUrl("template", "help", option);
        },
        /**
         * Retrieves a generic file url
         * @param {String} action Action to launch
         * @param {String} target Action target
         * @returns {String} Help template url
         */
        getGenericFileUrl: function (action, target) {
          // Retrieve url
          return Connection.getRawUrl() + $utilities.generateEndpointUrl(action, target);
        },
        /**
         * Compose url and parameters to retrieve a file from server
         * @param {String} action Action name
         * @param {Object} parameters Parameter list
         * @returns {Object} File data
         */
        getFileData: function (action, parameters) {
          // Retrieve url
          return {url: ServerData.getFileUrl(action), data: parameters};
        },
        /**
         * Retrieve URL for one server file
         * @param {String} action
         * @returns {String} Server file URL
         */
        getFileUrl: function (action) {
          return Connection.getRawUrl() + $utilities.generateEndpointUrl("file", action);
        },
        /**
         * Retrieve an angular template code
         * @param {String} template Template to load
         * @returns {String} Angular template url
         */
        getAngularTemplateUrl: function (template) {
          return Connection.getRawUrl() + $utilities.generateEndpointUrl("template", "angular", template);
        },
        /**
         * Preload an angular template code
         * @param {Object} template Template to load
         * @param {Function} onLoad on loaded method
         */
        preloadAngularTemplate: function (template, onLoad) {
          let  templateUrl = ServerData.getAngularTemplateUrl(template.path);
          ServerData.get(templateUrl).success(function (data, status) {
            if (data && status === 200) {
              $templateCache.put(template.name || template.path, data);
              if (onLoad) {
                onLoad(data);
              }
            }
          });
        },
        /**
         * Retrieve the updated model
         * @param {Object} component Component to check
         * @param {Object} model model to update
         * @param {String} method Method to call
         * @param {String} args Method arguments
         * @returns {Object} model updated
         */
        getComponentData: function (component, model, method, args) {
          // Get data for component
          if (method in component) {
            let componentData = component[method](args);
            // Check and warn if value is overwritten
            _.each(componentData, function (parameter, parameterId) {
              if (parameterId in model) {
                $log.warn("[WARNING] Overwriting '" + parameterId + "' duplicated parameter", {'old': model[parameterId], 'new': parameter});
              }
            });
            // Update model sent
            _.merge(model, componentData);
          }

          // Return data
          return model;
        },
        /**
         * Check whether a component belongs to a grid row detail instance (the instance or one of its buttons). They are
         * not screen components: they are not part of the form values of the screen
         * @param {String} view Component view
         * @param {String} componentId Component identifier
         * @returns {Boolean} The component belongs to a row detail instance
         */
        isRowDetailComponent: function (view, componentId) {
          return Boolean(((Storage.get("controller") || {})[view] || {})[componentId]?.rowDetailInstance);
        },
        /**
         * Values of the grid row detail instance an address belongs to (the instance, one of its cells or one of its
         * buttons): the data of the instance under the keys of its template and the values of its parent row. Other
         * addresses have none
         * @param {Object} address Address of the component that sends a request
         * @param {String} method Method of the api that retrieves the data
         * @param {*} args Method arguments
         * @returns {Object} Values of the row detail instance
         */
        getRowDetailValues: function (address, method, args) {
          const controllers = Storage.get("controller") || {};
          const componentId = address?.component;
          const view = address?.view in controllers ? address.view : _.findKey(controllers, viewControllers => componentId in viewControllers);
          const marker = ((controllers[view] || {})[componentId] || {}).rowDetailInstance;
          if (!marker) {
            return {};
          }
          const api = ((Storage.get("api") || {})[view] || {})[marker.instance] || {};
          const data = method in api ? api[method](args) : {};
          return {...getRowDetailInstanceData(data, marker.instance, marker.template), ...marker.parameters};
        },
        /**
         * Retrieve the data of every screen component with an api method
         * @param {String} method Method to call
         * @param {*} args Method arguments
         * @returns {Object} Data of the screen
         */
        getScreenValues: function (method, args) {
          let model = {};
          let api = Storage.get("api");

          // Retrieve components from each view (the row detail instances are not screen components)
          _.each(api, function (view, viewId) {
            _.each(view, function (component, componentId) {
              if (!ServerData.isRowDetailComponent(viewId, componentId)) {
                model = ServerData.getComponentData(component, model, method, args);
              }
            });
          });

          // Return data
          return model;
        },
        /**
         * Retrieve the updated model
         * @param {Object} address Address of the component that sends the values: a grid row detail instance adds its own
         */
        getFormValues: function (address = null) {
          return {...ServerData.getScreenValues("getData"), ...ServerData.getRowDetailValues(address, "getData")};
        },
        /**
         * Retrieve the updated model for printing
         * @param {Object} address Address of the component that sends the values: a grid row detail instance adds its own
         */
        getFormValuesForPrinting: function (address = null) {
          let fullModel = Storage.get("model");
          let fullModelReport = fullModel["report"] || {};
          let orientationModel = fullModelReport["reportOrientation"] || {};
          let orientation = orientationModel.selected || "PORTRAIT";

          return {
            ...ServerData.getScreenValues("getPrintData", orientation),
            ...ServerData.getRowDetailValues(address, "getPrintData", orientation)
          };
        },
        /**
         * Retrieve parameters and send them to the server
         * @param {object} action Action received
         * @param {Object} parameters Server action parameters
         */
        launchServerAction: function (action, parameters) {
          // Retrieve form values
          let  message = {
            action: action,
            target: action.attr("callbackTarget"),
            values: action.attr("parameters") || {}
          };

          // Set callback target as loading
          Control.changeControllerAttribute(message.target, {loading: true});

          // Add form values
          _.merge(message.values, parameters);

          // Retrieve target specific attributes for the server call
          if (message.target) {
            let  target = Control.getAddressApi(message.target);
            if (target && target.getSpecificFields) {
              // Add form values
              _.merge(message.values, target.getSpecificFields());
            }
          }

          // Launch server action
          let  request = Connection.sendMessage(message);
          action.onCancel = function () {
            if (request && request.reject) {
              request.reject();
            }
          };
        },
        /**
         * Generate a server action
         * @param {String} address Target address
         * @param {Object} custom Custom values
         * @param {Boolean} async Async action
         * @param {Boolean} silent Silent action
         */
        getServerAction: function (address, custom, async, silent) {
          let  screenData = Storage.get("screen")[address.view];

          // Generate action
          let  action = {
            type: 'server',
            async: async,
            silent: silent,
            parameters: {
              screen: screenData.name,
              address: address,
              component: address.component
            }
          };

          // Add form and custom values
          _.merge(action.parameters, custom);

          // Add server action
          action.parameters[$settings.get("serverActionKey")] = custom.type || "data";

          // Return server action
          return action;
        },
        /**
         * Send a query to the server
         * @param {Object} custom Custom values
         * @param {Boolean} async Async action
         * @param {Boolean} silent Silent action
         */
        sendMaintain: function (custom, async, silent) {
          // Generate server action
          // Generate action
          let  action = {
            type: 'server',
            async: async,
            silent: silent,
            parameters: {}
          };

          // Add server action
          action.parameters[$settings.get("serverActionKey")] = custom.type;
          action.parameters[$settings.get("targetActionKey")] = custom.maintain;

          // Add action to actions stack
          return ActionController.addActionList([action], true, {address: {}, context: ""});
        },
        /**
         * Send a message to the server
         * @param {Object} message Message to send
         * @return {Object} Connection promise
         */
        send: function (message) {
          return Connection.send(message);
        },
        /**
         * Launch a get message to the server
         * @param {Object} url Message to send
         * @return {Object} Connection promise
         */
        get: function (url) {
          return Connection.get(url);
        }
      };
      return ServerData;
    }
  ]);
