import {aweApplication} from "../../awe";
import {uploaderColumnTemplate} from "../../services/uploader";

// Column uploader directive
aweApplication.directive('aweColumnUploader',
  ['ServerData', 'Column', 'Uploader',
    function (serverData, Column, Uploader) {
      return {
        restrict: 'E',
        replace: true,
        template: uploaderColumnTemplate,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let  column = new Column(attrs);
          let  component = new Uploader(scope, column.id, elem);

          // Initialize criterion and column
          if (column.init(component).asUploader()) {
            // Update visible value on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
