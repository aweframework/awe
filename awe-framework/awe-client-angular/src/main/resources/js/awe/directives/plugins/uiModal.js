import {aweApplication} from "../../awe";
import {TestAttributes} from "../../data/testIds";

// Modal plugin
aweApplication.directive('uiModal',
  ['AweUtilities',
    function (Utilities) {
      return {
        restrict: 'A',
        priority: 1,
        scope: {
          onOpen: '&',
          onClose: '&'
        },
        link: function (scope, elem, attrs) {
          let  opened = false;

          // Expose whether the dialog is open. Bootstrap fires "hidden.bs.modal" after removing the backdrop,
          // so "false" means the screen is interactive again
          const setOpen = open => $(elem).attr(TestAttributes.open, open ? "true" : "false");
          setOpen(false);

          scope.$on("modalChange", function (event, show) {
            if (show && show !== opened) {
              $(elem).modal('show');
              opened = true;
            } else {
              $(elem).modal('hide');
            }
          });

          // Call on finish showing
          $(elem).on('shown.bs.modal', function () {
            setOpen(true);
            if (attrs.onOpen) {
              Utilities.timeout(function () {
                scope.onOpen();
              });
            }
          });

          // Call on finish hiding
          $(elem).on('hidden.bs.modal', function () {
            setOpen(false);
            if (attrs.onClose) {
              Utilities.timeout(function () {
                scope.onClose();
              });
            }
            opened = false;
          });
        }
      };
    }
  ]);
