import {aweApplication} from "../awe";
import {advancedTemplate, initAdvancedBlock} from "../services/advancedSearch";

// Advanced block of a window (#790): its row (Advanced filters and the filters in effect) and its
// children, the advanced criteria, below the row or in a side panel
aweApplication.directive('aweAdvanced',
  ['$compile', 'Control', 'AweUtilities', 'Component', 'ActionController', 'AweSettings', 'Storage', 'Connection',
    function ($compile, Control, Utilities, Component, $actionController, $settings, Storage, Connection) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template: advancedTemplate,
        require: '?^^aweWindow',
        scope: {
          advancedId: '@'
        },
        link: function (scope, elem, attrs, windowController) {
          initAdvancedBlock(scope, elem, windowController, {$compile, Control, Utilities, $actionController, $settings, Storage, Connection});
          // The screen sends the block as a component: its view waits for it to be initialized before it starts the
          // dependencies of the screen
          new Component(scope, scope.advancedId).asComponent();
        }
      };
    }
  ]);
