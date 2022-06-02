import {aweApplication} from "../awe";

// Video directive
aweApplication.directive('aweLink',
  ['ServerData', 'Component', '$sce',
    /**
     * Link directive
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
          `<a class="{{::controller.style}}" title="{{::controller.title | translate}}" ng-href="{{trustSrc(controller.url)}}" target="_blank">
            <i ng-if="::controller.icon" class="fa fa-{{::controller.icon}}"></i>
            <span ng-if="::controller.label" class="link-text" translate-multiple="{{controller.label}}"></span>
          </a>`,
        scope: {
          'linkId': '@'
        },
        link: function (scope) {
          scope.trustSrc = function (src) {
            return $sce.trustAsUrl(src);
          }

          // Init as component
          let  component = new Component(scope, scope.linkId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }
        }
      };
    }
  ]);
