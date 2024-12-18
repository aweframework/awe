import {aweApplication} from "../awe";

// Screen service
aweApplication.factory("Screen",
  ['$location', 'AweSettings', 'AweUtilities', 'ActionController', 'DependencyController', 'Control', 'ServerData', 'Storage', '$state', '$window',
    /**
     * Control screen data
     *
     * @param {object} $location
     * @param {object} $settings
     * @param {object} $utilities
     * @param {object} $actionController
     * @param {object} $dependencyController
     * @param {object} $control
     * @param {object} $serverData
     * @param {object} $storage Storage service
     * @param {object} $state $state service
     * @param {object} $window Window service
     */
    function ($location, $settings, $utilities, $actionController, $dependencyController, $control, $serverData, $storage, $state, $window) {
      const $screen = {
        /**
         * Retrieve parameters and send them to the server
         * @param {object} action Action received
         */
        screen: function (action) {

          // Retrieve action parameters
          let parameters = action.attr("parameters");
          let context = action.attr("context");
          let reload = parameters.reload || false;

          // If token received
          if ("token" in parameters) {
            $settings.setToken(parameters.token, true);
          }

          // Define target screen
          let  target = context ? "/" + context + "/" : "";
          if ("screen" in parameters) {
            target += parameters.screen;
          } else if ("target" in parameters) {
            target += parameters.target;
          } else {
            target += action.attr("target");
          }

          // Location is not the same
          if (!$utilities.sameUrl(target, $location.url()) || reload) {
            // Redirect to the screen
            let state = $utilities.getState(target, reload);
            $state.transitionTo(state.to, state.parameters, {reload: false, inherit: true, notify: true, location: true});

            // Finish screen action
            $actionController.acceptAction(action);
          } else if ($settings.get("reloadCurrentScreen")) {
            // Location is the same: reload
            $screen.reload(action);
          } else {
            // Finish action
            $actionController.acceptAction(action);
          }
        },
        /**
         * Reload the current state
         * @param {object} action Action received
         */
        reload: function (action) {
          // Retrieve action parameters
          $state.go($state.current, {}, {reload: false, inherit: true, notify: true, location: false});

          // Finish screen action
          $actionController.acceptAction(action);
        },
        /**
         * Return to the previous screen
         * @param {object} action Action received
         */
        back: function (action) {
          // Finish screen action
          $actionController.acceptAction(action);

          // Go to the previous screen
          $window.history.back();
        },
        /**
         * Change the language of the interface
         * @param {object} action Action received
         */
        changeLanguage: function (action) {
          // Retrieve action parameters
          let parameters = action.attr("parameters") || {};
          let target = action.attr("target");
          let view = action.attr("view");
          let model = $storage.get("model");
          let language = parameters.language;
          if (view in model && target in model[view]) {
            language = model[view][target].selected;
          }

          // If language has been received, update it
          if (language) {
            $settings.changeLanguage(language);
          }

          // Finish screen action
          $actionController.acceptAction(action);
        },
        /**
         * Wait x milliseconds
         * @param {object} action Action received
         */
        wait: function (action) {
          // Retrieve action parameters
          let  parameters = action.attr("parameters");
          let  time = parameters.target || 1;

          $utilities.timeout(function () {
            // Finish action
            $actionController.acceptAction(action);
          }, time);
        },
        /**
         * Change the theme of the interface
         * @param {object} action Action received
         */
        changeTheme: function (action) {
          // Retrieve action parameters
          let parameters = action.attr("parameters") || {};
          let target = action.attr("target");
          let view = action.attr("view");
          let model = $storage.get("model");
          let theme = parameters.theme;
          if (view in model && target in model[view]) {
            theme = model[view][target].selected;
          }

          // If language has been received, update it
          if (theme) {
            $settings.update({theme: theme});
          }

          // Finish screen action
          $actionController.acceptAction(action);
        },

        /**
         * Reload the theme variable css class
         * @param {object} action Action received
         */
        updateTheme: function (action) {
          // Reload themeVariable css
          let queryString = '?reload=' + new Date().getTime();
          $('link[rel="stylesheet"]#themeVariables').each(function () {
            this.href = this.href.replace(/\?.*|$/, queryString);
          });

          // Finish screen action
          $actionController.acceptAction(action);
        },
        /**
         * Load screen data
         * @param {object} action Action received
         */
        screenData: function (action) {
          // Get parameters
          let  parameters = action.attr("parameters");

          // Store parameters in view scope
          $storage.get("screenData")[parameters.view] = parameters.screenData;

          // Send messages
          let  actions = parameters.screenData.actions;
          if (actions.length > 0) {
            $actionController.addActionList(actions, false, {});
          }

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Open screen dialog
         * @param {object} action Action received
         */
        openDialog: function (action) {
          let  address = action.attr("callbackTarget");

          // Change controller
          let  attributes = {opened: true, openAction: action};
          $control.changeControllerAttribute(address, attributes);
        },
        /**
         * Close screen dialog
         * @param {object} action Action received
         */
        closeDialog: function (action) {
          // Close action
          $actionController.acceptAction(action);

          // Close dialog
          $utilities.timeout(function () {
            $control.changeControllerAttribute(action.attr("callbackTarget"), {opened: false, accept: true});
          });
        },
        /**
         * Close screen dialog and cancel action
         * @param {object} action Action received
         */
        closeDialogAndCancel: function (action) {
          // Close action
          $actionController.acceptAction(action);

          // Close dialog
          $utilities.timeout(function () {
            $control.changeControllerAttribute(action.attr("callbackTarget"), {opened: false, accept: false});
          });
        },
        /**
         * Finish loading
         * @param {object} action Action received
         */
        endLoad: function (action) {
          // Retrieve action address
          let  address = action.attr("callbackTarget");
          let  api = $control.getAddressApi(address);
          if (api.endLoad) {
            api.endLoad();
          }

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Get file from server
         * @param {object} action get file from server
         */
        getFile: function (action) {
          // Variable definition
          let parameters = {
            ...action.attr("parameters"),
            [$settings.get("serverActionKey")]: "get-file"
          };

          // Generate url parameter
          let  fileData = $serverData.getFileData("download", parameters);
          fileData.action = action;

          // Download file
          $utilities.downloadFile(fileData);
        },
        /**
         * Enable dependencies
         * @param {object} action Action received
         */
        enableDependencies: function (action) {
          $screen.toggleDependencies(action, true);
        },
        /**
         * Disable dependencies
         * @param {object} action Action received
         */
        disableDependencies: function (action) {
          $screen.toggleDependencies(action, false);
        },
        /**
         * Enable/Disable dependencies
         * @param {object} action Action received
         * @param {Boolean} enabled Enable/disable dependencies
         */
        toggleDependencies: function (action, enabled) {
          // Retrieve action address
          $dependencyController.toggleDependencies(enabled);

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Add Class
         * @param {object} action Action received
         */
        addClass: function (action) {
          $screen.changeClass(action, true);
        },
        /**
         * Remove Class
         * @param {object} action Action received
         */
        removeClass: function (action) {
          $screen.changeClass(action, false);
        },
        /**
         * Toggle Class
         * @param {object} action Action received
         */
        toggleClass: function (action) {
          // Variable definition
          let  tagSelector = action.attr("target");
          let  parameters = action.attr("parameters");
          let  targetClass = parameters[$settings.get("targetActionKey")];

          // Add/remove the class/classes
          targetClass.split(" ").forEach(cssClass => $(tagSelector).toggleClass(cssClass));

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Add/Remove a class to a tag
         * @param {object} action Action received
         * @param {object} add Add/Remove a class
         */
        changeClass: function (action, add) {
          // Variable definition
          let  tagSelector = action.attr("target");
          let  parameters = action.attr("parameters");
          let  targetClass = parameters[$settings.get("targetActionKey")];
          let  method = add ? "addClass" : "removeClass";

          // Add/remove the class/classes
          $(tagSelector)[method](targetClass);

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Print the current screen
         * @param {object} action Action received
         */
        screenPrint: function (action) {
          $window.print();

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Redirect to another URL
         * @param {object} action Action received
         */
        redirect: function (action) {
          let url = action.attr("target");
          let newWindow = action.attr("parameters")?.newWindow || false;

          if (newWindow) {
            // Open url in new window
            $window.open(url, "_blank");
          } else {
            // Redirect browser
            $window.location.href = url;
          }

          // Close action
          $actionController.acceptAction(action);
        },
        /**
         * Redirect screen to another URL
         * @param {object} action Action received
         */
        redirectScreen: function (action) {
          let screen = action.attr("parameters")?.screen || null;
          let view = "report" in $storage.get("screen") ? $storage.get("screen")["report"] : $storage.get("screen")["base"];
          if (screen === view.name) {
            $screen.redirect(action);
          } else {
            // Close action
            $actionController.acceptAction(action);
          }
        },
        /**
         * Close the current window
         * @param {object} action Action received
         */
        closeWindow: function (action) {
          // Close action
          $actionController.acceptAction(action);

          // Call window close
          $window.close();
        },
        /**
         * Finish a dependency
         * @param {object} action Action received
         */
        endDependency: function (action) {
          let  dependency = action.attr("parameters").dependency;
          $dependencyController.finishDependency(dependency, action);
        }
      };
      return $screen;
    }]);