import {aweApplication} from "../awe";

// Translate multiple directive
aweApplication.directive('translateMultiple',
  ['$translate', '$compile', function ($translate, $compile) {
      return {
        restrict: 'A',
        link: function (scope, element, attrs) {
          /**
           * Update the translation
           */
          var updateTranslation = function () {
            var value = attrs.translateMultiple;
            if (value && typeof value === "string") {
              // Split value into multiple elements
              let translatedValue = value.split(" ").map($translate.instant).join(" ");

              // Put the translated value in the element
              element.html(translatedValue);
              if ($translate.isPostCompilingEnabled()) {
                $compile(element.contents())(scope);
              }
            }
          };
          // Update translation on value change
          attrs.$observe('translateMultiple', updateTranslation);
          // Ensures the text will be refreshed after the current language was changed
          // w/ $translate.use(...)
          var unbind = scope.$root.$on('$translateChangeSuccess', updateTranslation);
          scope.$on('$destroy', unbind);
        }
      };
    }
  ]);