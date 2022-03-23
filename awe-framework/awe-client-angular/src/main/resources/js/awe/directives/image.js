import {aweApplication} from "../awe";

// Video directive
aweApplication.directive('aweImage',
  ['ServerData', 'Component', '$sce',
    /**
     * Image directive
     * @param {Object} serverData Server call service
     * @param {Object} Component Component class
     * @param {Object} $sce Strict Contextual Escaping
     */
    function (serverData, Component, $sce) {
      return {
        restrict: 'E',
        replace: true,
        transclude: true,
        template:
        `<img class="{{::controller.style}}" title="{{::controller.title | translate}}" ng-src="{{trustSrc(controller.url)}}" 
          ng-attr-alt="{{trustSrc(controller.alternateUrl)}}"/>`,
        scope: {
          'imageId': '@'
        },
        link: function (scope) {
          scope.trustSrc = function (src) {
            return $sce.trustAsResourceUrl(src);
          }

          // Init as component
          var component = new Component(scope, scope.imageId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }
        }
      };
    }
  ]);
