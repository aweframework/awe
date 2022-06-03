import {aweApplication} from "./../awe";

// Application controller
aweApplication.controller('AppController',
  ['$scope', '$log', 'LoadingBar', 'ServerData', 'Storage', 'AweUtilities', 'AweSettings', 'ActionController', '$rootScope',
    /**
     * Control the base application behaviour
     * @param {object} $scope
     * @param {object} $log
     * @param {object} $loadingBar
     * @param {object} $serverData
     * @param {object} $storage
     * @param {object} $utilities
     * @param {object} $settings
     */
    function ($scope, $log, $loadingBar, $serverData, $storage, $utilities, $settings, $actionController, $root) {
      // Define controller
      let $ctrl = this;

      $root.status = {loading: false};

      $ctrl.updateStatus = function(id, value) {
        $root.status = {
          ...$root.status,
          [id]:value
        }
      }

      /**
       * Manage keydown event
       * @param {Object} $event jQuery Event
       */
      $ctrl.onKeydown = function($event) {
        // On Alt + Shift + 0-9, toggle actions to 0-9 secs
        let DIGITS = [48, 49, 50, 51, 52, 53, 54, 55, 56, 57];
        if ($event.altKey && $event.shiftKey && DIGITS.includes($event.which)) {
          // Toggle stack
          $settings.update({actionsStack: DIGITS.indexOf($event.which) * 1000});
        }

        // Check caps lock
        $ctrl.updateStatus("isCapsLockOn", $event.originalEvent.getModifierState('CapsLock'));
      };

      /**
       * Manage keyup event
       * @param {Object} $event jQuery Event
       */
      $ctrl.onKeyup = function($event) {
        // Check caps lock
        $ctrl.updateStatus("isCapsLockOn", $event.originalEvent.getModifierState('CapsLock'));
      };

      /**
       * Check if application is IE or not
       * @returns {string} Application ie detector
       */
      $ctrl.isIE = function() {
        let browser = $utilities.getBrowser();
        return browser.includes("ie") ? browser : `not-ie ${browser}`;
      };

      // Initialize controller, model, messages and api
      $storage.put("controller", {});
      $storage.put("model", {});
      $storage.put("messages", {});
      $storage.put("screen", {});
      $storage.put("screenData", {});
      $storage.put("api", {});
      $storage.put("status", {});

      let  $window = $(window);
      let  ON_UNLOAD = "onunload";

      // Show loading message
      $ctrl.updateStatus("loading", true);

      // View is resizing
      $scope.resizing = true;

      /**
       * Broadcasts a resize event
       */
      let  resize = function () {
        $scope.$apply(function () {
          $utilities.publish('resize');
        });
      };

      /**
       * Broadcasts a resize action
       */
      let  resizeAction = function () {
        $utilities.publish('resize-action');
      };

      /**
       * Initialise loading
       */
      let initLoading = function () {
        $ctrl.updateStatus("loading", true);
      };

      /**
       * Finish loading
       */
      let endLoading = function () {
        $ctrl.updateStatus("loading", false);
      };

      /**
       * Check on unload
       * @param {type} screenParameters
       * @param {type} closingTab       */
      let  checkUnload = function (screenParameters, closingTab) {
        let onUnload = ON_UNLOAD in screenParameters ? screenParameters[ON_UNLOAD] : false;
        if (onUnload) {
          if (closingTab) {
            let  message = {};
            message[$root.settings.serverActionKey] = 'maintain-async';
            message[$root.settings.targetActionKey] = onUnload;

            $serverData.send(message);

            // Time for browser to send ajax
            for (let i = 0; i < 10000000; i++) {
              // Wait actively
            }
          } else {
            $serverData.sendMaintain({type: "maintain-silent", maintain: onUnload}, false, true);
          }
        }
      };

      // Route change start (show loading message)
      $scope.$on('$stateChangeStart', function (event, toState, _toParams, fromState/*, fromParams*/) {
        // Prevent start if settings are still not defined
        if (!$storage.getRoot("cometUID")) {
          return event.preventDefault();
        }

        let views = _.merge({}, fromState.views, toState.views);
        _.each(views, function (_view, viewName) {
          $scope.$broadcast("unload", viewName);
        });
        //$log.debug("$stateChangeStart", {evento: event, to: {state: toState, params: toParams}, from: {state: fromState, params: fromParams}})
        $scope.resizing = true;
        $loadingBar.startTask();

        let unloadViews = {};
        // Retrieve view from target state
        for (let view in toState.views) {
          unloadViews[view] = true;
        }
        // Retrieve view from source state
        for (let view in fromState.views) {
          unloadViews[view] = true;
        }

        // For each model retrieve unload and launch
        let screen = $storage.get("screen");
        _.each(screen, function (screenView, viewId) {
          if (viewId in unloadViews) {
            checkUnload(screenView, false);
          }
        });
      });

      // Route change start (hide loading message)
      $scope.$on('$stateChangeSuccess', function (/*event, toState, toParams, fromState, fromParams*/) {
        // $log.debug("$stateChangeSuccess", {evento: event, to: {state: toState,params: toParams}, from: {state: fromState,params: fromParams}})
        $scope.resizing = false;
      });

      // Route change start (hide loading message)
      $scope.$on('$stateChangeError', function (_event, toState, _toParams, _fromState, _fromParams, error) {
        $ctrl.updateStatus("loading", false);
        $scope.resizing = false;
        $loadingBar.end();
        $log.warn("State '" + toState.name + "' rejected: " + error);
      });

      // Route state not found
      $scope.$on('$stateNotFound', function (_event, current/*, previous, reject*/) {
        //do you work here
        $ctrl.updateStatus("loading", false);
        $loadingBar.end();
        $log.warn("State not found: " + current);
      });

      // Window resize event
      $window.on('resize', resize);
      // Launch message action
      $scope.$on('/action/resize', function (_event, action) {
        let  parameters = action.attr("parameters");
        let  delay = parameters ? parameters.delay || 0 : 0;
        $utilities.timeout(function () {
          resizeAction();
          // Finish screen action
          $actionController.acceptAction(action);
        }, delay);
      });

      // Manage init loading and end loading
      $scope.$on('cfpLoadingBar:started', initLoading);
      $scope.$on('cfpLoadingBar:completed', endLoading);

      // On loaded screen
      $scope.$on('initialised', function () {
        $utilities.timeout(function () {
          $loadingBar.endTask();
        }, 100);
      });

      // Remove splash screen & friends
      $("#splash").fadeOut(250, () => $(this).remove());

      /**
       * Check for unload maintain actions in opened views
       */
      $ctrl.beforeUnload = function() {
        let screen = $storage.get("screen");
        _.each(screen, function (screenView) {
          checkUnload(screenView, true);
        });
      };

      $window.onbeforeunload = $ctrl.beforeUnload;
    }
  ]);