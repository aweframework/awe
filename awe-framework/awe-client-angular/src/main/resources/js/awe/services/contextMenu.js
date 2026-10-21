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

          // Show the context menu and, once displayed, keep it inside the viewport
          contextMenu.visible = true;
          Utilities.timeout(function () {
            if (contextMenu.visible) {
              keepInsideViewport(contextMenu.element);
            }
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

            // Remove the mask and put the menu back in its place, with no position left behind
            this.mask.remove();
            this.element.css({position: "", top: "", left: ""});
            if (destroyed !== true && this.placeholder.parent().length > 0) {
              this.placeholder.replaceWith(this.element);
            } else {
              // Nothing to return to (the component DOM is gone). Never remove() the linked menu (that strips its
              // Angular data and a later show would find a dead node): detach it so it can be shown again
              this.placeholder.remove();
              this.element.detach();
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
