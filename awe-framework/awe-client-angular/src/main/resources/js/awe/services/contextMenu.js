import {aweApplication} from "../awe";

// Context menu service
aweApplication.factory('ContextMenu',
  ['AweUtilities',
    /**
     * Context menu generic methods
     * @param {service} Utilities
     */
    function (Utilities) {
      // The pointer stays inside the menu, so the mouseup of the right button lands on it
      const POINTER_OFFSET = 16;

      /**
       * Move the menu so it stays fully inside the viewport. It is measured once displayed
       * @param {jQuery} element Menu element
       */
      function keepInsideViewport(element) {
        const node = element[0];
        if (!node || !node.getBoundingClientRect) {
          return;
        }
        const rect = node.getBoundingClientRect();
        const overflowX = Math.max(rect.right - window.innerWidth, 0);
        const overflowY = Math.max(rect.bottom - window.innerHeight, 0);
        // Never push it past the top left corner of the viewport
        const shiftX = Math.min(overflowX, Math.max(rect.left, 0));
        const shiftY = Math.min(overflowY, Math.max(rect.top, 0));
        if (shiftX > 0) {
          element.css("left", (parseFloat(node.style.left) || 0) - shiftX);
        }
        if (shiftY > 0) {
          element.css("top", (parseFloat(node.style.top) || 0) - shiftY);
        }
      }

      /**
       * Align a menu opened under an element to the right edge of the element when the element sits in the window
       * header (its buttons are on the right of the page) or when the menu, aligned to the left edge of the element,
       * would overflow the right edge of the viewport. It never starts before the left edge of the viewport. Same rule
       * as the React engine (useContextMenu alignMenuToAnchor)
       * @param {jQuery} element Menu element, displayed under the anchor
       * @param {jQuery} anchor Element the menu hangs from
       */
      function alignToAnchor(element, anchor) {
        const node = element[0];
        const anchorNode = anchor && anchor[0];
        if (!node || !anchorNode || !anchorNode.getBoundingClientRect) {
          return;
        }
        const rect = anchorNode.getBoundingClientRect();
        const width = node.offsetWidth;
        const inHeader = anchor.closest(".window-buttons").length > 0;
        if (inHeader || rect.left + width > window.innerWidth) {
          element.css("left", Math.max(rect.right - width, 0));
        }
      }

      /**
       * Option links of a menu the keyboard can reach: the visible and enabled options (no separators)
       * @param {jQuery} element Menu element
       * @return {HTMLElement[]} Option links, in order
       */
      function keyboardOptions(element) {
        return element.children("li.context-option").not(".ng-hide, .divider").children("a")
          .filter(function () {
            return $(this).attr("data-disabled") !== "true";
          }).toArray();
      }

      /**
       * Context menu creator
       * @param {type} scope
       * @param {type} elem
       * @param {type} data
       */
      function ContextMenu(scope, elem, data) {
        this.scope = scope;
        this.node = elem;
        this.visible = false;
        this.data = data;
      }
      ContextMenu.prototype = {
        /**
         * Initialize ContextMenu
         */
        init: function () {
          // Add hide context menu event
          let _self = this;
          let hide = function () {
            _self.hide();
          };
          this.scope.$on("hideContextMenu", hide);
          this.scope.$on("$destroy", function () {
            _self.hide(true);
          });
        },
        /**
         * Show the context menu
         * @param {event} event
         */
        show: function (event) {
          // Grids resolve their option dependencies after the right click selects the row, so show() never gates on
          // the visible options: the right click entry points of the other components do (see hasVisibleOptions)

          // Opening it again before hiding it must not leave the previous mask or placement behind
          this.hide();

          // Render the menu at body level, fixed at the pointer: it stacks above the mask (z-index 1050, also at body
          // level) without raising any ancestor, so a click on a sibling of the component still reaches the mask.
          // Moving a linked element keeps its scope and watchers
          let contextMenu = this;
          const menu = this.getMenuElement();
          if (menu.length === 0) {
            return;
          }
          this.placeholder = $(document.createComment("context-menu"));
          this.element.before(this.placeholder);
          $("body").append(this.element);
          this.element.css({
            position: "fixed",
            top: event.clientY - POINTER_OFFSET,
            left: event.clientX - POINTER_OFFSET
          });

          // Create a mask panel to hide the context menu
          this.mask = $("<div class='component-mask'></div>");
          $("body").append(this.mask);
          this.mask.on("click contextmenu mouseup", function () {
            // Cancel default event
            event.preventDefault();
            Utilities.timeout(function () {
              contextMenu.hide();
            });
            return false;
          });

          // Show the context menu and, once displayed, align it to the element it hangs from (if any) and keep it
          // inside the viewport. A menu opened under a button (from the keyboard too) takes the focus on its first option
          contextMenu.visible = true;
          const anchor = event.anchor;
          contextMenu.anchor = anchor;
          contextMenu.element.on("keydown.contextMenu", keyEvent => contextMenu.onKeyDown(keyEvent));
          Utilities.timeout(function () {
            if (contextMenu.visible) {
              alignToAnchor(contextMenu.element, anchor);
              keepInsideViewport(contextMenu.element);
              const [first] = keyboardOptions(contextMenu.element);
              if (anchor && first) {
                first.focus();
              }
            }
          });
        },
        /**
         * Keyboard of the open menu: the arrows move between the options, Enter or Space chooses the focused one, Escape
         * closes the menu (the focus goes back to the button it hangs from) and Tab closes it
         * @param {KeyboardEvent} event Key event
         */
        onKeyDown: function (event) {
          const options = keyboardOptions(this.element);
          const index = options.indexOf(document.activeElement);
          switch (event.key) {
            case "ArrowDown":
            case "ArrowUp": {
              if (options.length > 0) {
                const step = event.key === "ArrowDown" ? 1 : -1;
                const next = index === -1 ? (step > 0 ? 0 : options.length - 1) : (index + step + options.length) % options.length;
                options[next].focus();
              }
              event.preventDefault();
              break;
            }
            case "Enter":
            case " ":
              if (index > -1) {
                options[index].click();
              }
              event.preventDefault();
              break;
            case "Escape":
              // Only the menu closes, not the panel or dialog under it
              event.preventDefault();
              event.stopPropagation();
              this.hide();
              break;
            case "Tab":
              this.hide();
              break;
            default:
              break;
          }
        },
        /**
         * Show the context menu under an element: its top left corner lands on the bottom left corner of the
         * anchor, or its top right corner on the bottom right corner of an anchor of the window header or one whose
         * menu would overflow the viewport. It serves actions, which carry no mouse event
         * @param {jQuery} anchor Element the menu hangs from
         */
        showUnder: function (anchor) {
          const node = anchor && anchor[0];
          if (!node || !node.getBoundingClientRect) {
            return;
          }
          const rect = node.getBoundingClientRect();
          // show() moves the menu up and left by the pointer offset, which is added back here
          this.show({
            clientX: rect.left + POINTER_OFFSET,
            clientY: rect.bottom + POINTER_OFFSET,
            preventDefault: Utilities.noop,
            anchor: anchor
          });
        },
        /**
         * Get the menu list. It is rendered next to the directive anchor (ng-if) and remembered once found, as it
         * leaves its place while it is open. An empty lookup (menu not rendered yet) or a node that ng-if has
         * since replaced is never remembered
         * @return {jQuery} Menu element
         */
        getMenuElement: function () {
          const known = this.element;
          // While open the menu lives in the body, away from its place: it is still the live one
          if (known && known.length > 0 && (this.visible || known.parent().length > 0)) {
            return known;
          }
          const found = this.node.is(".context-menu") ? this.node : $(this.node.siblings(".context-menu"));
          if (found.length > 0) {
            this.element = found;
            return found;
          }
          // Nothing rendered next to the anchor: an empty result is never remembered. A menu detached on hide
          // (component DOM gone) is still the linked one
          return known && known.length > 0 ? known : found;
        },
        /**
         * Check if the menu has any option the user can see: hidden options and separators do not count
         * @return {boolean} At least one visible option
         */
        hasVisibleOptions: function () {
          const menu = this.getMenuElement();
          return menu.children("li.context-option").filter(function () {
            const option = $(this);
            return !option.hasClass("ng-hide") && !option.hasClass("divider");
          }).length > 0;
        },
        /**
         * Hide the context menu
         */
        hide: function (destroyed) {
          if (this.visible) {
            // Hide the context menu
            this.visible = false;
            this.element.off("keydown.contextMenu");
            const hadFocus = $.contains(this.element[0], document.activeElement);

            // Remove the mask and put the menu back in its place, with no position left behind
            this.mask.remove();
            this.element.css({position: "", top: "", left: ""});
            if (destroyed !== true && this.placeholder.parent().length > 0) {
              this.placeholder.replaceWith(this.element);
            } else {
              // Nothing to return to (the component DOM is gone). While the scope lives, never remove() the linked
              // menu (that strips its Angular data and a later show would find a dead node): detach it so it can be
              // shown again. With the scope destroyed nothing shows it again, so it leaves the page for good
              this.placeholder.remove();
              if (destroyed === true) {
                this.element.remove();
              } else {
                this.element.detach();
              }
            }

            // A menu opened under a button gives the focus back to it when it closes with the focus inside it (Escape,
            // an option chosen), as the React engine does
            const anchor = this.anchor;
            this.anchor = null;
            const active = document.activeElement;
            if (anchor && anchor[0] && document.body.contains(anchor[0]) && (hadFocus || !active || active === document.body)) {
              anchor[0].focus();
            }
          }
        },
        /**
         * Retrieve the visibility of the context menu
         * @return {boolean} Context menu is visible or not
         */
        isVisible: function () {
          return this.visible;
        }
      };
      return ContextMenu;
    }]);
