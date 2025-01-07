import {aweApplication} from "../../awe";
import {uploaderInputTemplate} from "../../services/uploader";

// Upload directive
aweApplication.directive('aweInputUploader',
  ['ServerData', 'Uploader',
    function (serverData, Uploader) {
      return {
        restrict: 'E',
        replace: true,
        template: uploaderInputTemplate,
        scope: {
          'criterionId': '@inputUploaderId'
        },
        link: function (scope, elem, attrs) {
          // Initialize criterion
          scope.initialized = new Uploader(scope, scope.criterionId, elem).asUploader();
        }
      };
    }
  ]);
