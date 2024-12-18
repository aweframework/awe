import {aweApplication} from "../awe";

// Loading bar service
aweApplication.factory('LoadingBar',
  ['cfpLoadingBar', 'Control',
    function (loadingBarPlugin, Control) {
      // Call number
      let  calls = 0;
      let  showing = false;
      let  $window = $(window);

      let  LoadingBar = {
        /**
         * Starts a server task
         */
        startTask: function () {
          // If not showing, start loading bar
          LoadingBar.startTasks(1);
        },
        /**
         * Starts some server tasks
         * @param {integer} tasks
         */
        startTasks: function (tasks) {
          // If not showing, start loading bar
          if (!showing) {
            $window.scrollTop(0);
            Control.publish('hideHelp');
            loadingBarPlugin.start();
            showing = true;
            calls = tasks;
            // Else add a call
          } else {
            calls += tasks;
            loadingBarPlugin.inc();
          }
        },
        /**
         * Finishes a server task
         */
        endTask: function () {
          // Remove a call
          calls--;
          loadingBarPlugin.inc();

          // If calls are finished, complete the loading bar
          if (showing && calls === 0) {
            loadingBarPlugin.complete();
            showing = false;
          }
        },
        /**
         * Finishes all server tasks
         */
        end: function () {
          // Remove all calls
          calls = 1;

          // If calls are finished, complete the loading bar
          LoadingBar.endTask();
        }
      };
      return LoadingBar;
    }
  ]);