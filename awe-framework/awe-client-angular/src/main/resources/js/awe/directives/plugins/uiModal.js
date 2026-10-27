import {aweApplication} from "../../awe";
import {TestAttributes} from "../../data/testIds";
import {createFocusTrap} from "../../services/focusTrap";

// Modal plugin
// Bootstrap closes a modal with Escape only when the keyboard focus is inside it: the focus moves into the modal when
// it is shown and goes back to the control which opened it when it is hidden. Escape closes only a modal the user can
// close (it has its own close button, data-dismiss="modal"). With the trap-tab attribute Tab stays inside the modal.
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
          const trap = createFocusTrap(() => elem[0], {trapTab: "trapTab" in attrs});

          // Expose whether the dialog is open. Bootstrap fires "hidden.bs.modal" after removing the backdrop,
          // so "false" means the screen is interactive again
          const setOpen = open => $(elem).attr(TestAttributes.open, open ? "true" : "false");
          setOpen(false);

          // The close buttons of this modal (not the ones of a modal nested in it)
          const isClosable = () => $(elem).find('[data-dismiss="modal"]')
            .filter((index, button) => $(button).closest(".modal")[0] === elem[0]).length > 0;

          // Bootstrap reads its options once: the keyboard option is updated on every show (the close button may come
          // with the dialog title)
          const showModal = () => {
            const keyboard = isClosable();
            const modal = $(elem).data("bs.modal");
            if (modal) {
              modal.options.keyboard = keyboard;
              $(elem).modal('show');
            } else {
              $(elem).modal({keyboard, show: true});
            }
          };

          scope.$on("modalChange", function (event, show) {
            if (show && show !== opened) {
              showModal();
              opened = true;
            } else {
              $(elem).modal('hide');
            }
          });

          // Escape pressed in a nested modal closes that modal only
          $(elem).on('keydown', function (event) {
            if (event.which === 27 && $(event.target).closest(".modal")[0] !== elem[0]) {
              event.stopImmediatePropagation();
            }
          });

          // The events of a nested modal bubble up to this one: the focus follows only the ones of this modal
          const own = event => event.target === elem[0];

          // Remember the control which opened the modal
          $(elem).on('show.bs.modal', function (event) {
            if (own(event)) {
              trap.activate();
            }
          });

          // Call on finish showing
          $(elem).on('shown.bs.modal', function (event) {
            setOpen(true);
            if (own(event)) {
              trap.focusFirst();
            }
            if (attrs.onOpen) {
              Utilities.timeout(function () {
                scope.onOpen();
              });
            }
          });

          // Call on finish hiding
          $(elem).on('hidden.bs.modal', function (event) {
            setOpen(false);
            if (own(event)) {
              trap.deactivate();
            }
            if (attrs.onClose) {
              Utilities.timeout(function () {
                scope.onClose();
              });
            }
            opened = false;
          });

          scope.$on("$destroy", () => trap.deactivate());
        }
      };
    }
  ]);
