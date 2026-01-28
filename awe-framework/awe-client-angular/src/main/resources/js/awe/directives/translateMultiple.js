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
          let  updateTranslation = function () {
            let  value = attrs.translateMultiple;
            if (value && typeof value === "string") {
              // Split value into multiple elements
              let translatedValue = value.split(" ").map(v => $translate.instant(v)).join(" ");

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
          let  unbind = scope.$root.$on('$translateChangeSuccess', updateTranslation);
          scope.$on('$destroy', unbind);
        }
      };
    }
  ]);