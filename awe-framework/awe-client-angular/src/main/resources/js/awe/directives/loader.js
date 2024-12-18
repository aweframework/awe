import {aweApplication} from "../awe";

/**
 * Preload angular template
 * @param templateName
 * @param Utilities
 * @param ServerData
 * @param compileTemplate
 */
function preloadTemplate(templateName, Utilities, ServerData, compileTemplate) {
  ServerData.preloadAngularTemplate({path: templateName}, (data) => {
    Utilities.publishDelayed('template-' + templateName, data);
    compileTemplate(data);
  });
}

/**
 *
 * @param templateName
 * @param scope
 * @param compileTemplate
 */
function watchTemplateLoaded(templateName, scope, compileTemplate) {
  let endWatchTemplateLoaded = scope.$on('template-' + templateName, (event, template) => {
    endWatchTemplateLoaded();
    compileTemplate(template);
  });
}

// Loader directive
aweApplication.directive('aweLoader',
  ['ServerData', '$compile', '$templateCache', 'AweUtilities',
    /**
     * Help directive
     * @param {object} ServerData Server call service
     * @param {function} $compile compilation service
     * @param {object} $templateCache Template cache
     * @param {object} Utilities Awe utilities
     */
    function (ServerData, $compile, $templateCache, Utilities) {
      // Action Controller methods
      let TEMPLATE_LOADING = "--LOADING--";
      return {
        restrict: 'E',
        compile: function () {
          return function (scope, elem, attrs) {
            /**
             * Compile the template
             * @param {type} template
             */
            function compileTemplate(template) {
              // Compile the received data
              let newElement = $compile(template)(scope);
              // Which we can then append to our DOM element.
              elem.append(newElement);
            }

            // Observe select2 attributes
            let initWatch = attrs.$observe('iconLoader', initLoader);

            /**
             * Loader initialization
             * @param {String} iconLoader
             */
            function initLoader(iconLoader) {
              // Add action
              let templateName = "loader/" + (iconLoader || "spinner");

              // Check parameters
              let template = $templateCache.get(templateName);
              if (!template) {
                $templateCache.put(templateName, TEMPLATE_LOADING);
                preloadTemplate(templateName, Utilities, ServerData, compileTemplate);
              } else if (template === TEMPLATE_LOADING) {
                watchTemplateLoaded(templateName, scope, compileTemplate);
              } else {
                compileTemplate(template);
              }
              // Remove watch
              initWatch();
            }
          };
        }
      };
    }
  ]);
