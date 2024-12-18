import {aweApplication} from "../awe";
import {ClientActions} from "../data/actions";
import _ from "lodash";

// Form directive
aweApplication.directive('aweForm',
  ['ServerData', 'Control', 'ActionController', 'AweSettings', 'AweUtilities', 'Validator', 'Connection',
    /**
     * Form directive
     * @param {object} ServerData Server data
     * @param {object} Control Control service
     * @param {object} Control Control service
     * @param {object} $actionController ActionController service
     * @param {object} $settings AWE $settings
     * @param {object} Utilities AWE Utilities
     * @param {object} Validator Validator service
     * @param {object} $connection Connection service
     */
    function (ServerData, Control, $actionController, $settings, Utilities, Validator, $connection) {

      /**
       * Retrieve reseteable scopes
       * @param {type} target
       * @param {type} scope
       * @returns {Array}
       */
      const getReseteableScopes = function (target, scope) {
        let  reseteableScopes = [];
        if (target) {
          // Reset target model
          let  reseteableComponents = [".criterion", ".grid", ".chart"];
          let  targetId = "#" + target;
          let  reseteableTarget = "";
          _.each(reseteableComponents, function (reseteableComponent) {
            reseteableTarget += reseteableTarget === "" ? "" : ",";
            reseteableTarget += targetId + " " + reseteableComponent;
          });
          let  $target = $(reseteableTarget);
          if ($target.length) {
            // If target has children, reset all children
            _.each($target, function (reseteable) {
              let  $reseteable = $(reseteable);
              if ($reseteable.children().length > 0) {
                _.each($reseteable.children(), function(child) {
                  if($(child).scope) {
                    reseteableScopes.push($(child).scope());
                  }
                });
                // Else reset only the target
              } else if ($reseteable.scope()) {
                reseteableScopes.push($reseteable.scope());
              }
            });
          } else {
            // No children. Try to reset scope
            $target = $(targetId);
            if ($target.children().length > 0 && $target.children().scope()) {
              reseteableScopes.push($target.children().scope());
              // Else reset only the target
            } else if ($target.scope()) {
              reseteableScopes.push($target.scope());
            }
          }
        } else {
          // Reset view model
          reseteableScopes.push(scope);
        }
        return reseteableScopes;
      };

      const FormActions = {
        /**
         * Validate the form
         * @param {object} action Action received
         * @param {Object} scope Scope
         */
        validate: function (action, scope) {

          // Reset validator errors
          scope.showValidation = false;

          // Check if action is for a specific target
          let  target = action.attr("target");
          let  $base = $(document.body);
          if (target) {
            // Validate an element and children
            let  $target = $("#" + target);
            if ($target.is(".form-control")) {
              $base = $target.closest(".criterion");
            } else {
              $base = $target;
            }
          }

          // Launch validation
          let  errorList = Validator.validateNode($base);

          // Check if validation has been sucessful
          $actionController.finishAction(action, errorList.length === 0);
        },
        /**
         * Show validation error
         * @param {Object} scope Scope
         * @param {Object} error Error to show
         */
        showValidationError: function (scope, error) {
          Validator.showValidationError(scope, error);
        },
        /**
         * Set a criterion as valid
         * @param {object} action Action received
         */
        setValid: function (action) {
          let  target = action.attr("callbackTarget");
          Control.launchApiMethod(target, "changeValidation", ["invalid", false]);
          $actionController.acceptAction(action);
        },
        /**
         * Set a criterion as invalid
         * @param {object} action Action received
         */
        setInvalid: function (action) {
          let  parameters = action.attr("parameters");
          let  target = action.attr("callbackTarget");
          Control.launchApiMethod(target, "changeValidation", [{
              invalid: {
                message: parameters.message
              }
            }, true]);
          Control.launchApiMethod(target, "validate", []);
          // Accept the current action
          $actionController.acceptAction(action);
        },
        /**
         * Retrieve parameters and send them to the server
         * @param {object} action Action received
         */
        server: function (action) {
          // Launch server action with form values
          ServerData.launchServerAction(action, ServerData.getFormValues(), false);
        },
        /**
         * Retrieve parameters and send them to the server for printing actions (send images and text)
         * @param {object} action Action received
         */
        serverPrint: function (action) {
          // Launch server action for printing
          ServerData.launchServerAction(action, ServerData.getFormValuesForPrinting(), true);
        },
        /**
         * Retrieve parameters and send them to the server for printing actions (send images and text)
         * @param {object} action Action received
         */
        serverDownload: function (action) {
          // Launch server action for printing
          let  parameters = {};
          let  target = action.attr("callbackTarget");

          // Store parameters
          _.merge(parameters, action.attr("parameters"), ServerData.getFormValues());
          let  targetAction = parameters[$settings.get("targetActionKey")];

          // Retrieve target specific attributes for the server call
          if (target) {
            const api = Control.getAddressApi(target);
            if (api?.getSpecificFields) {
              // Add form values
              _.merge(parameters, api.getSpecificFields());
            }
          }

          // Generate url parameter
          let  fileData = ServerData.getFileData("download/maintain/" + targetAction, parameters);
          fileData.action = action;

          // Download file
          Utilities.downloadFile(fileData);
        },
        /**
         * Copy a criterion value to the clipboard
         * @param {object} action Action received
         */
        copyCriterionValueClipboard: function (action) {
          // Launch server action for printing
          let address = action.attr("callbackTarget");
          let model = Control.getAddressModel(address);

          // Copy the lines into the clipboard
          document.body.focus();
          navigator.clipboard.writeText(Utilities.isEmpty(model.selected) ? "" : model.selected);

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Update model with action values
         * @param {object} action Action received
         */
        fill: function (action) {
          // Retrieve parameters
          let  parameters = _.cloneDeep(action.attr("parameters"));
          let  data = parameters.datalist;
          let  address = action.attr("callbackTarget");

          // Generate model
          let  model = data;
          model.values = model.rows;
          delete model.rows;

          // Publish model change
          Control.changeModelAttribute(address, model, true);

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Fill suggest model with action values
         * @param {object} action Action received
         */
        fillSuggest: function (action) {
          // Retrieve parameters
          let parameters = _.cloneDeep(action.attr("parameters"));
          let values = parameters.values;
          let address = action.attr("callbackTarget");

          // Call the method update seleted value from API
          Control.changeModelAttribute(address, {selected: values, model: values}, true);

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Update controller with action values
         * @param {object} action Action received
         */
        updateController: function (action) {
          // Retrieve parameters
          const parameters = _.cloneDeep(action.attr("parameters"));
          const data = parameters.datalist || {};
          const values = data.rows || [{}];
          const address = action.attr("callbackTarget");
          delete data.rows;

          // Change controller
          const attributes = {[parameters.attribute]: parameters.value || values[0].value};
          Control.changeControllerAttribute(address, attributes);

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Update model with action values
         * @param {object} action Action received
         */
        select: function (action) {
          // Retrieve parameters
          let  parameters = _.cloneDeep(action.attr("parameters"));
          let  values = parameters.values;
          let  address = action.attr("callbackTarget");

          // Call the method update seleted value from API
          Control.changeModelAttribute(address, {selected: values}, true);

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Reset view selected values
         * @param {object} action
         * @param {Object} scope
         */
        reset: function (action, scope) {
          // Get parameters
          let  view = action.attr("view");

          // Check reset target
          let  reseteableScopes = getReseteableScopes(action.attr("target"), scope);
          _.each(reseteableScopes, function (reseteableScope) {
            reseteableScope.$broadcast("reset-scope", view);
          });

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Restore view selected values
         * @param {object} action
         * @param {object} scope
         */
        restore: function (action, scope) {
          // Get parameters
          let  view = action.attr("view");

          // Check restore target
          let  reseteableScopes = getReseteableScopes(action.attr("target"), scope);
          _.each(reseteableScopes, function (reseteableScope) {
            reseteableScope.$broadcast("restore-scope", view);
          });

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Restore view selected values with target
         * @param {Service} action
         * @param {Object} scope
         */
        restoreTarget: function (action, scope) {
          // Get parameters
          let  view = action.attr("view");

          // Check restore target
          let  reseteableScopes = getReseteableScopes(action.attr("target"), scope);
          _.each(reseteableScopes, function (reseteableScope) {
            reseteableScope.$broadcast("restore-scope-target", view);
          });

          // Finish action
          $actionController.acceptAction(action);
        },
        /**
         * Destroy all views
         * @param {object} action
         */
        logout: function (action) {
          // Close following actions
          $actionController.deleteStack();

          // Close connection
          $connection.disconnect();

          // Zombie action (to accept server actions)
          action.attr("alive", true);

          // Launch a logout server action
          let  parameters = {};
          parameters[$settings.get("serverActionKey")] = "logout";
          action.attr("parameters", parameters);
          FormActions.server(action);

          // Destroy all views
          Control.destroyAllViews();
        },
        /**
         * Check if model has been modified
         * @param {object} action
         */
        checkModelUpdated: function (action) {
          // Get target
          let  target = action.attr("callbackTarget");
          // Get view
          let  view = action.attr("view");
          let  context = action.attr("context");
          // Check if model is different than initial model
          let  changes = Control.checkModelChanged(view);

          if (changes) {
            // Create message to show in confirm action
            let  targetMessage = 'CONFIRM_UPDATE_DATA';
            let  message = {
              title: 'CONFIRM_TITLE_UPDATED_DATA',
              message: 'CONFIRM_MESSAGE_UPDATED_DATA'
            };
            // Add targetAction message to scope
            Control.addMessageToScope(view, targetMessage, message);

            // Create confirm action
            let  confirmAction = {type: 'confirm'};

            // Add parameters
            confirmAction.parameters = {'target': targetMessage};

            // Send action confirm
            $actionController.addActionList([confirmAction], false, {address: target, context: context});
          }

          // Accept action
          $actionController.acceptAction(action);
        },
        /**
         * Check if model hasn't been modified
         * @param {object} action
         */
        checkModelNoUpdated: function (action) {
          // Get target
          let  target = action.attr("callbackTarget");
          // Get view
          let  view = action.attr("view");
          let  context = action.attr("context");
          // Check if model is equal than initial model
          let  changes = Control.checkModelChanged(view);

          if (!changes) {
            // Create message to show in confirm action
            let  targetMessage = 'CONFIRM_NOT_UPDATE_DATA';
            let  message = {
              title: 'CONFIRM_TITLE_NOT_UPDATED_DATA',
              message: 'CONFIRM_MESSAGE_NOT_UPDATED_DATA'
            };
            // Add targetAction message to scope
            Control.addMessageToScope(view, targetMessage, message);

            // Create confirm action
            let  confirmAction = {type: 'confirm'};

            // Add parameters
            confirmAction.parameters = {'target': targetMessage};

            // Send action confirm
            $actionController.addActionList([confirmAction], false, {address: target, context: context});
          }

          // Accept action
          $actionController.acceptAction(action);
        },
        /**
         * Check if model has empty data
         * @param {object} action
         */
        checkModelEmpty: function (action) {
          // Get target
          let  target = action.attr("callbackTarget");
          // Get view
          let  view = action.attr("view");
          let  context = action.attr("context");
          // Check if model is different than initial model
          let  empty = Control.checkModelEmpty(view);

          if (empty) {
            // Create message to show in confirm action
            let  targetMessage = 'CONFIRM_EMPTY_DATA';
            let  message = {
              title: 'CONFIRM_TITLE_EMPTY_DATA',
              message: 'CONFIRM_MESSAGE_EMPTY_DATA'
            };
            // Add targetAction message to scope
            Control.addMessageToScope(view, targetMessage, message);

            // Create confirm action
            let  confirmAction = {type: 'confirm'};

            // Add parameters
            confirmAction.parameters = {'target': targetMessage};

            // Send action confirm
            $actionController.addActionList([confirmAction], false, {address: target, context: context});
          }
          // Accept action
          $actionController.acceptAction(action);
        },
        /**
         * Set a static value for an element
         * @param {object} action
         */
        value: function (action) {
          // Retrieve parameters
          action.attr("parameters").values = [action.attributes.value];
          FormActions.select(action);
        },
        /**
         * Cancel all actions of the current stack
         */
        cancel: function () {
          $actionController.deleteStack();
        }
      };
      const Form = {
        restrict: 'A',
        link: function (scope, elem) {
          // Store element in scope
          scope.element = elem;

          // Define listeners
          let  listeners = {};
          _.each(ClientActions.form, function (actionOptions, actionId) {
            listeners[actionId] = scope.$on("/action/" + actionId, function (event, action) {
              return FormActions[actionOptions.method](action, scope);
            });
          });

          // Show validation error
          listeners["showValidationError"] = scope.$on("show-validation-error", function (event, error) {
            FormActions.showValidationError(scope, error);
          });

          // Destroy listeners
          listeners["destroy"] = scope.$on("$destroy", function () {
            Utilities.clearListeners(listeners);
          });
        }
      };
      return Form;
    }
  ]);
