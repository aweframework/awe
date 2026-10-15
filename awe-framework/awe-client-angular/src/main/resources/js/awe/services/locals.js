import {aweApplication} from "../awe";

// Locals service
aweApplication.factory('AweLocals',
  ['AweSettings', 'ActionController', '$rootScope', 'AweUtilities', '$translate', 'Connection',
    function ($settings, $actionController, $rootScope, Utilities, $translate, Connection) {

      // Locals repository

      let  locals = {};
      let  deferred = null;

      /**
       * Launch local functions for jquery widgets
       * @returns {undefined}
       */
      let  launchLocalFunctions = function () {
        // Retrieve language
        let  language = $settings.getLanguage();

        // Create script tag and add it to HTML. The page publishes the versioned URL of each language script
        // (content hash when static resource versioning is on), so a deploy never serves a stale one
        // The language is compared, never built into the selector: any value is safe
        let  versionedUrl = $("meta[name^='awe-locals-']").filter((_index, meta) => meta.name === "awe-locals-" + language).attr("content");
        let  url = versionedUrl || Connection.getRawUrl() + "/js/locals-" + language + ".js";
        let  script = $("<script>", {type: "text/javascript", id: "localeFunctions", src: url});

        // Append localeFunctions (and replace old)
        let  oldLocaleFunctions = $("#localeFunctions");
        if (oldLocaleFunctions.length > 0) {
          oldLocaleFunctions.replaceWith(script);
        } else {
          $("body").append(script);
        }
        /*Connection.get(url, "text/javascript").then(function (response) {
          if (response.data && response.status === 200) {
            // assign it into the current DOM
            let  script = $("<script type='text/javascript' id='localeFunctions'>");
            script.text(response.data);

            // Append localeFunctions (and replace old)
            let  oldLocaleFunctions = $("#localeFunctions");
            if (oldLocaleFunctions.length > 0) {
              oldLocaleFunctions.replaceWith(script);
            } else {
              $("body").append(script);
            }
          }
        });*/
      };

      // Store locals retrieved
      $rootScope.$on('/action/locals-retrieved', function (event, action) {
        // Retrieve action parameters
        let  parameters = action.attr("parameters");
        let  language = parameters.language;

        if (language !== null) {
          // Finish locals action
          $actionController.acceptAction(action);

          // Store data
          locals[language] = parameters;

          // Set language
          deferred.resolve(locals[language].translations);
        }
      });

      // Store locals retrieved
      $rootScope.$on('/action/reload-language', function (event, action) {
        // Get language
        let  language = $settings.getLanguage();

        // If language in locals, retrieve it
        if (language !== null) {
          if (language in locals) {
            delete locals[language];
            $translate.refresh();
          }
        }
        $actionController.acceptAction(action);
      });

      // Launch local functions
      $rootScope.$on('launchLocalFunctions', launchLocalFunctions);
      return function () {
        // Generate deferred
        deferred = Utilities.q.defer();

        // Get language
        let  language = $settings.getLanguage();

        // If language in locals, retrieve it
        if (language !== null) {
          if (language in locals) {
            deferred.resolve(locals[language].translations);
          } else {
            let  parameters = {language: language};
            parameters[$settings.get("serverActionKey")] = "get-locals";
            let  getLocals = {type: 'server', parameters: parameters};
            $actionController.addActionList([getLocals], false, {address: {}, context: ""});
          }
        }

        return deferred.promise;
      };
    }
  ]);