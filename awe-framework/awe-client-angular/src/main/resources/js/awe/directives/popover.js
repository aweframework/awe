import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {createFocusTrap} from "../services/focusTrap";
import {findComponentElement} from "../services/validator";
import {TestAttributes, TestIds} from "../data/testIds";
import {initDialogComponent} from "./dialog";

const DEFAULT_MAX_WIDTH = "20rem";
const POSITIONS = ["bottom", "top", "left", "right"];
// Space between the anchor and the popover, and between the popover and the edges of the window
const GAP = 8;
const MARGIN = 8;
// Time to wait for the click which follows a mouse down on the anchor (it would open the popover again)
const CLICK_TIMEOUT = 1000;
// Overlays opened from the content of the popover (select dropdowns, datepickers, confirm dialogs, other popovers):
// a click inside them is not a click outside the popover
const OVERLAYS = ".modal, .select2-drop, .datepicker, .dropdown-menu, .awe-popover";
// Dialogs of its content (the Columns of a grid): while one is open it holds the keyboard
const CONTENT_DIALOGS = "[role='dialog']";

const template = `<div ng-attr-id="{{::controller.id}}" class="awe-popover-host" ng-cloak>
  <div class="awe-popover awe-popover-{{::position}}" role="dialog" aria-modal="true" tabindex="-1" hidden data-testid="${TestIds.popover}" ng-attr-data-testid-owner="{{::controller.id}}" data-position="{{::position}}" ng-style="::{'max-width': maxWidth}">
    <div class="awe-popover-header">
      <button type="button" class="close" data-testid="${TestIds.popoverClose}" ng-click="close()">&times;</button>
      <h4 class="awe-popover-title">
        ${getIconTemplate("panel-title-icon")}
        <span translate-multiple="{{::controller.label}}"></span>
      </h4>
    </div>
    <div class="awe-popover-body" ng-transclude></div>
  </div>
</div>`;

// Popover directive: a dialog anchored to a control of the screen (#843). The Bootstrap popover only holds text, so it
// is its own container, placed next to the anchor with fixed coordinates
aweApplication.directive('awePopover',
  ['Component', 'ActionController', 'AweUtilities',
    function (Component, $actionController, Utilities) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          popoverId: '@'
        },
        link: function (scope, element) {
          const component = initDialogComponent(scope, scope.popoverId, {Component, $actionController, Utilities});
          if (!component) {
            return false;
          }

          const controller = component.controller || {};
          scope.position = POSITIONS.includes(controller.position) ? controller.position : "bottom";
          scope.maxWidth = controller.maxWidth || DEFAULT_MAX_WIDTH;

          const popover = element.find(".awe-popover");
          let shown = false;

          // The layer which holds the keyboard: a dialog open in the content (the Columns of a grid), the popover, or
          // none while a modal opened from it (the Columns side panel) is shown
          const getLayer = () => {
            const modalAbove = $(".modal.in").toArray().some(modal => !modal.contains(popover[0]));
            if (modalAbove) {
              return null;
            }
            const dialogs = popover.find(CONTENT_DIALOGS).toArray().filter(dialog => !dialog.closest("[hidden]") &&
              (!dialog.classList.contains("modal") || dialog.classList.contains("in")));
            return dialogs[dialogs.length - 1] || popover[0];
          };
          const trap = createFocusTrap(getLayer);

          // The element of the control the popover is anchored to (the visible one if there are more)
          const getAnchor = () => {
            const nodes = findComponentElement($(document.body), controller.anchor);
            return nodes.filter(":visible")[0] || nodes[0] || null;
          };

          // Place the popover on its side of the anchor, inside the window (centered in the window without anchor)
          const place = () => {
            // A grid of the content is maximized over the screen: the popover is part of the screen until it is restored
            if (popover.hasClass("maximizeParent")) {
              return;
            }
            const anchor = getAnchor();
            const size = popover[0].getBoundingClientRect();
            const maxLeft = window.innerWidth - size.width - MARGIN;
            const maxTop = window.innerHeight - size.height - MARGIN;
            const clamp = (value, max) => Math.max(MARGIN, Math.min(value, max));
            let top = (window.innerHeight - size.height) / 2;
            let left = (window.innerWidth - size.width) / 2;
            if (anchor) {
              const rect = anchor.getBoundingClientRect();
              const vertical = scope.position === "top" || scope.position === "bottom";
              top = vertical ? 0 : clamp(rect.top + (rect.height - size.height) / 2, maxTop);
              left = vertical ? clamp(rect.left + (rect.width - size.width) / 2, maxLeft) : 0;
              switch (scope.position) {
                case "top":
                  top = rect.top - GAP - size.height;
                  break;
                case "left":
                  left = rect.left - GAP - size.width;
                  break;
                case "right":
                  left = rect.right + GAP;
                  break;
                default:
                  top = rect.bottom + GAP;
              }
            }
            popover.css({top: `${Math.round(top)}px`, left: `${Math.round(left)}px`});
          };

          const close = () => component.closeDialog();
          scope.close = close;

          // A mouse down on the anchor closes the popover: its click must not open it again
          const swallowAnchorClick = anchor => {
            const onClick = event => {
              document.removeEventListener("click", onClick, true);
              if (anchor.contains(event.target)) {
                event.preventDefault();
                event.stopPropagation();
              }
            };
            document.addEventListener("click", onClick, true);
            setTimeout(() => document.removeEventListener("click", onClick, true), CLICK_TIMEOUT);
          };

          const onMouseDown = event => {
            const target = event.target;
            const overlay = target.closest ? target.closest(OVERLAYS) : null;
            if (popover[0].contains(target) || (overlay && !overlay.contains(popover[0]))) {
              return;
            }
            const anchor = getAnchor();
            if (anchor && anchor.contains(target)) {
              swallowAnchorClick(anchor);
            }
            close();
          };

          // Esc inside the popover closes it, and not the modal which holds it (a dialog of its content keeps it open)
          const onKeyDown = event => {
            if (shown && event.key === "Escape") {
              event.stopPropagation();
              if (getLayer() === popover[0]) {
                close();
              }
            }
          };
          // Esc with the focus nowhere (on the page body) closes it too
          const onDocumentKeyDown = event => {
            if (event.target === document.body) {
              onKeyDown(event);
            }
          };
          popover.on("keydown", onKeyDown);

          const listen = on => {
            const method = on ? "addEventListener" : "removeEventListener";
            document[method]("mousedown", onMouseDown, true);
            document[method]("keydown", onDocumentKeyDown);
            window[method]("resize", place);
            window[method]("scroll", place, true);
          };

          const show = () => {
            if (shown) {
              return;
            }
            shown = true;
            // Placed in the modal which holds it (Bootstrap keeps the focus inside an open modal), in the maximize target
            // of the screen (its content area, where a grid of the popover is maximized as any other) or in the page
            // body, so no container of the screen clips it
            const host = element.closest(".modal")[0] || element.closest(".maximize-target")[0] || document.body;
            if (popover[0].parentNode !== host) {
              host.appendChild(popover[0]);
            }
            trap.activate();
            popover[0].hidden = false;
            popover.attr(TestAttributes.open, "true");
            place();
            trap.focusFirst();
            listen(true);
          };

          const hide = () => {
            if (!shown) {
              return;
            }
            shown = false;
            listen(false);
            // A grid of the content maximized over the screen gives the screen back
            scope.$broadcast("restore");
            // Give the focus back to the control which opened it while the focus is still inside
            trap.deactivate();
            popover[0].hidden = true;
            popover.attr(TestAttributes.open, "false");
          };
          popover.attr(TestAttributes.open, "false");

          // Show and hide the popover with the dialog: open action, close actions, close button, outside click and Esc
          const openDialog = component.openDialog;
          component.openDialog = function () {
            if (!component.isOpened) {
              openDialog();
              show();
            }
          };
          // Closed at once, not after the 200 ms of the dialog fade: the popover has no fade, and a reopen within those
          // 200 ms (the anchor clicked right after Esc) would be dropped and its new opening action ended instead
          component.closeDialog = function () {
            if (!component.isOpened) {
              return;
            }
            hide();
            component.storeEvent('close');
            Utilities.publishDelayedFromScope("modalChange", false, scope);
            $actionController.removeStack();
            $actionController.finishAction(component.controller.openAction, component.controller.accept);
            component.controller.accept = component.controller.acceptOnClose;
            component.isOpened = false;
          };

          scope.$on("$destroy", () => {
            hide();
            popover.off("keydown", onKeyDown);
            popover.remove();
          });
        }
      };
    }
  ]);
