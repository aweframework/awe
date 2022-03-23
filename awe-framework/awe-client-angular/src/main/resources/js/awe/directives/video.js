import {aweApplication} from "../awe";

// Video directive
aweApplication.directive('aweVideo',
  ['ServerData', 'Component',
    /**
     * Video directive
     * @param {Object} serverData Server call service
     * @param {Object} Component Component class
     */
    function (serverData, Component) {
      return {
        restrict: 'E',
        replace: true,
        transclude: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('video');
        },
        scope: {
          'videoId': '@'
        },
        link: function (scope) {
          // Init as component
          var component = new Component(scope, scope.videoId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }
        }
      };
    }
  ]);
