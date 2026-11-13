import $ from "jquery";
import {aweApplication} from "../awe";
import {afterTransition, animates, reflow} from "./transition";

/**
 * Native modal behavior (it replaces the Bootstrap 3.4.1 modal plugin, which AWE no longer ships).
 *
 * It keeps what AWE and its products depend on: the DOM classes (`display: block` + `.in` on the `.modal`, a
 * `.modal-backdrop[.fade].in` per open modal appended to the body, `body.modal-open`), the scrollbar compensation, the
 * Esc / backdrop / `data-dismiss="modal"` dismissals (`data-keyboard="false"` and `data-backdrop="static"` honored),
 * the fade timing (300 ms for the dialog, 150 ms for the backdrop, ended by `transitionend` or by timeout) and the
 * jQuery events `show.bs.modal`, `shown.bs.modal`, `hide.bs.modal` and `hidden.bs.modal` triggered on the element.
 */

const DATA_KEY = "awe.modal";
const NAMESPACE = ".bs.modal";
const DISMISS_NAMESPACE = ".dismiss.bs.modal";
const TRANSITION_DURATION = 300;
const BACKDROP_TRANSITION_DURATION = 150;
const ESCAPE_KEY = 27;
const FIXED_CONTENT = ".navbar-fixed-top, .navbar-fixed-bottom";
const DEFAULTS = {backdrop: true, keyboard: true};

/**
 * Width of the browser scrollbar
 * @param {jQuery} body Page body
 * @returns {number} Width in pixels
 */
const measureScrollbar = body => {
  const probe = document.createElement("div");
  probe.className = "modal-scrollbar-measure";
  body.append(probe);
  const width = probe.offsetWidth - probe.clientWidth;
  probe.remove();
  return width;
};

/**
 * Modal behavior of one element
 */
export class Modal {
  /**
   * @param {HTMLElement|jQuery} element Modal element (".modal")
   * @param {object} options Options: backdrop (true, false or "static") and keyboard
   */
  constructor(element, options) {
    this.options = options;
    this.body = $(document.body);
    this.element = $(element);
    this.dialog = this.element.find(".modal-dialog");
    this.backdropElement = null;
    this.isShown = false;
    this.originalBodyPad = null;
    this.scrollbarWidth = 0;
    this.bodyIsOverflowing = false;
    this.ignoreBackdropClick = false;
    this.opener = null;
    this.pending = [];
  }

  /**
   * Wait for a transition of the modal; the wait is cancelled when the modal is disposed, hidden or shown again
   * @param {jQuery} target Element which transitions
   * @param {number} duration Maximum wait in milliseconds
   * @param {Function} callback Function to call when it ends
   */
  wait(target, duration, callback) {
    const cancel = afterTransition(target, duration, () => {
      this.pending = this.pending.filter(item => item !== cancel);
      callback();
    });
    this.pending.push(cancel);
  }

  /**
   * Cancel the transitions the modal waits for
   * @returns {boolean} There was something pending
   */
  cancelPending() {
    const had = this.pending.length > 0;
    this.pending.forEach(cancel => cancel());
    this.pending = [];
    return had;
  }

  /**
   * Show or hide the modal
   * @param {HTMLElement} relatedTarget Control which asks for it
   * @returns {void}
   */
  toggle(relatedTarget) {
    return this.isShown ? this.hide() : this.show(relatedTarget);
  }

  /**
   * Show the modal: it raises show.bs.modal (which can cancel it) and shown.bs.modal once it is displayed
   * @param {HTMLElement} relatedTarget Control which asks for it
   */
  show(relatedTarget) {
    const showEvent = $.Event("show.bs.modal", {relatedTarget});
    this.element.trigger(showEvent);

    if (this.isShown || showEvent.isDefaultPrevented()) {
      return;
    }

    // Shown again while it fades out: the pending hide is dropped, with its backdrop and the paddings it left
    if (this.cancelPending()) {
      this.removeBackdrop();
      this.resetAdjustments();
      this.resetScrollbar();
    }

    this.isShown = true;
    this.rememberOpener();
    this.checkScrollbar();
    this.setScrollbar();
    this.body.addClass("modal-open");

    this.escape();
    this.resize();

    this.element.on("click" + DISMISS_NAMESPACE, "[data-dismiss=\"modal\"]", event => this.hide(event));

    // A drag which starts inside the dialog and ends on the backdrop does not dismiss the modal
    this.dialog.on("mousedown" + DISMISS_NAMESPACE, () => {
      this.element.one("mouseup" + DISMISS_NAMESPACE, event => {
        if ($(event.target).is(this.element)) {
          this.ignoreBackdropClick = true;
        }
      });
    });

    this.backdrop(() => {
      const transition = this.element.hasClass("fade") && animates(this.dialog);

      if (!this.element.parent().length) {
        this.element.appendTo(this.body);
      }

      this.element[0].style.display = "block";
      this.element.scrollTop(0);
      this.adjustDialog();

      if (transition) {
        // Force a reflow so the fade starts from the hidden state
        reflow(this.element[0]);
      }

      this.element.addClass("in");
      this.enforceFocus();

      const shownEvent = $.Event("shown.bs.modal", {relatedTarget});
      const finish = () => this.element.trigger("focus").trigger(shownEvent);

      if (transition) {
        this.wait(this.dialog, TRANSITION_DURATION, finish);
      } else {
        finish();
      }
    });
  }

  /**
   * Hide the modal: it raises hide.bs.modal (which can cancel it) and hidden.bs.modal once the backdrop is gone
   * @param {jQuery.Event} [event] Event which dismisses the modal (its default action is prevented)
   */
  hide(event) {
    event?.preventDefault?.();

    const hideEvent = $.Event("hide.bs.modal");
    this.element.trigger(hideEvent);

    if (!this.isShown || hideEvent.isDefaultPrevented()) {
      return;
    }

    // Hidden while it was still showing: the pending show never happens
    this.cancelPending();

    this.isShown = false;
    this.escape();
    this.resize();
    $(document).off("focusin" + NAMESPACE);

    this.element
      .removeClass("in")
      .off("click" + DISMISS_NAMESPACE)
      .off("mouseup" + DISMISS_NAMESPACE);
    this.dialog.off("mousedown" + DISMISS_NAMESPACE);

    if (this.element.hasClass("fade") && animates(this.element)) {
      this.wait(this.element, TRANSITION_DURATION, () => this.hideModal());
    } else {
      this.hideModal();
    }
  }

  /**
   * Hide the modal at once, with its backdrop and without events (the owner is about to remove the element)
   */
  dispose() {
    this.cancelPending();
    $(document).off("focusin" + NAMESPACE);
    $(window).off("resize" + NAMESPACE);
    this.element
      .off("keydown" + DISMISS_NAMESPACE)
      .off("click" + DISMISS_NAMESPACE)
      .off("mouseup" + DISMISS_NAMESPACE);
    this.dialog.off("mousedown" + DISMISS_NAMESPACE);
    if (this.isShown) {
      this.isShown = false;
      this.element.removeClass("in");
      this.element[0].style.display = "none";
      this.removeBackdrop();
      this.body.removeClass("modal-open");
      this.resetAdjustments();
      this.resetScrollbar();
    }
  }

  /**
   * Remember the control which has the focus, to give it back when the modal is hidden
   */
  rememberOpener() {
    const active = document.activeElement;
    this.opener = active && active !== document.body && !this.element[0].contains(active) ? active : null;
  }

  /**
   * Give the focus back to the control which opened the modal, unless the user moved it to another control
   */
  restoreFocus() {
    const opener = this.opener;
    this.opener = null;
    const active = document.activeElement;
    const focusIsFree = !active || active === document.body || this.element[0].contains(active);
    if (opener && opener.isConnected && typeof opener.focus === "function" && focusIsFree) {
      opener.focus();
    }
  }

  /**
   * Keep the focus inside the modal
   */
  enforceFocus() {
    $(document)
      .off("focusin" + NAMESPACE)
      .on("focusin" + NAMESPACE, event => {
        if (document !== event.target && this.element[0] !== event.target && !this.element.has(event.target).length) {
          this.element.trigger("focus");
        }
      });
  }

  /**
   * Dismiss with the Esc key (data-keyboard="false" disables it)
   */
  escape() {
    if (this.isShown && this.options.keyboard) {
      this.element.on("keydown" + DISMISS_NAMESPACE, event => {
        if (event.which === ESCAPE_KEY) {
          this.hide();
        }
      });
    } else if (!this.isShown) {
      this.element.off("keydown" + DISMISS_NAMESPACE);
    }
  }

  /**
   * Re-adjust the dialog when the window changes its size
   */
  resize() {
    if (this.isShown) {
      $(window).on("resize" + NAMESPACE, () => this.adjustDialog());
    } else {
      $(window).off("resize" + NAMESPACE);
    }
  }

  /**
   * Finish hiding: hide the element, remove the backdrop and the body flag, and raise hidden.bs.modal
   */
  hideModal() {
    this.element[0].style.display = "none";
    this.backdrop(() => {
      this.body.removeClass("modal-open");
      this.resetAdjustments();
      this.resetScrollbar();
      this.restoreFocus();
      this.element.trigger("hidden.bs.modal");
    });
  }

  /**
   * Remove the backdrop of the modal
   */
  removeBackdrop() {
    this.backdropElement?.remove();
    this.backdropElement = null;
  }

  /**
   * Add the backdrop when the modal is shown, or remove it when it is hidden
   * @param {Function} [callback] Function to call once the backdrop transition ends
   */
  backdrop(callback) {
    if (this.isShown && this.options.backdrop) {
      this.showBackdrop(callback);
    } else if (!this.isShown && this.backdropElement) {
      this.hideBackdrop(callback);
    } else if (callback) {
      callback();
    }
  }

  /**
   * Handle a click on the modal: on the backdrop it dismisses the modal (or focuses it when the backdrop is static)
   * @param {jQuery.Event} event Click
   */
  onBackdropClick(event) {
    if (this.ignoreBackdropClick) {
      this.ignoreBackdropClick = false;
    } else if (event.target === event.currentTarget) {
      if (this.options.backdrop === "static") {
        this.element[0].focus();
      } else {
        this.hide();
      }
    }
  }

  /**
   * Add the backdrop of the shown modal
   * @param {Function} [callback] Function to call once the backdrop transition ends
   */
  showBackdrop(callback) {
    const animate = this.element.hasClass("fade") ? "fade" : "";
    this.backdropElement = $(document.createElement("div"))
      .addClass("modal-backdrop " + animate)
      .appendTo(this.body);
    const doAnimate = animate && animates(this.backdropElement);

    this.element.on("click" + DISMISS_NAMESPACE, event => this.onBackdropClick(event));

    if (doAnimate) {
      // Force a reflow so the fade starts from the hidden state
      reflow(this.backdropElement[0]);
    }

    this.backdropElement.addClass("in");

    if (!callback) {
      return;
    }
    if (doAnimate) {
      this.wait(this.backdropElement, BACKDROP_TRANSITION_DURATION, callback);
    } else {
      callback();
    }
  }

  /**
   * Remove the backdrop of the hidden modal
   * @param {Function} [callback] Function to call once the backdrop is removed
   */
  hideBackdrop(callback) {
    this.backdropElement.removeClass("in");
    const remove = () => {
      this.removeBackdrop();
      callback?.();
    };
    if (this.element.hasClass("fade") && animates(this.backdropElement)) {
      this.wait(this.backdropElement, BACKDROP_TRANSITION_DURATION, remove);
    } else {
      remove();
    }
  }

  /**
   * Compensate the scrollbar of the body in a modal taller than the window
   */
  adjustDialog() {
    const modalIsOverflowing = this.element[0].scrollHeight > document.documentElement.clientHeight;
    this.element.css({
      paddingLeft: !this.bodyIsOverflowing && modalIsOverflowing ? this.scrollbarWidth : "",
      paddingRight: this.bodyIsOverflowing && !modalIsOverflowing ? this.scrollbarWidth : ""
    });
  }

  /**
   * Remove the padding added by adjustDialog
   */
  resetAdjustments() {
    this.element.css({paddingLeft: "", paddingRight: ""});
  }

  /**
   * Check if the page has a scrollbar and how wide it is
   */
  checkScrollbar() {
    this.bodyIsOverflowing = document.body.clientWidth < window.innerWidth;
    this.scrollbarWidth = measureScrollbar(this.body);
  }

  /**
   * Pad the body (and the fixed navbars) with the width of the scrollbar the page loses while the modal is open
   */
  setScrollbar() {
    const bodyPad = Number.parseInt(this.body.css("padding-right") || 0, 10);
    this.originalBodyPad = document.body.style.paddingRight || "";
    const scrollbarWidth = this.scrollbarWidth;
    if (this.bodyIsOverflowing) {
      this.body.css("padding-right", bodyPad + scrollbarWidth);
      $(FIXED_CONTENT).each((index, fixed) => {
        const actualPadding = fixed.style.paddingRight;
        const calculatedPadding = $(fixed).css("padding-right");
        $(fixed)
          .data("padding-right", actualPadding)
          .css("padding-right", Number.parseFloat(calculatedPadding) + scrollbarWidth + "px");
      });
    }
  }

  /**
   * Restore the padding of the body (and the fixed navbars)
   */
  resetScrollbar() {
    this.body.css("padding-right", this.originalBodyPad);
    $(FIXED_CONTENT).each((index, fixed) => {
      const padding = $(fixed).data("padding-right");
      $(fixed).removeData("padding-right");
      fixed.style.paddingRight = padding || "";
    });
  }
}

/**
 * Get the modal behavior of an element (it is created the first time)
 * @param {HTMLElement|jQuery} element Modal element
 * @param {object} [options] Options (backdrop, keyboard); the data-backdrop and data-keyboard attributes are the default,
 *   and the ones given update the options of an existing modal
 * @returns {Modal} Modal
 */
export function getModal(element, options = {}) {
  const $element = $(element);
  let modal = $element.data(DATA_KEY);
  if (modal) {
    // The options of a call win over the ones it was created with (the owner may decide them on every show)
    Object.assign(modal.options, options);
  } else {
    modal = new Modal($element, {...DEFAULTS, ...pickOptions($element.data()), ...options});
    $element.data(DATA_KEY, modal);
  }
  return modal;
}

/**
 * Pick the modal options of the data attributes of an element
 * @param {object} data Data of the element
 * @returns {object} Options
 */
function pickOptions(data) {
  const options = {};
  ["backdrop", "keyboard"].forEach(name => {
    if (data && data[name] !== undefined) {
      options[name] = data[name];
    }
  });
  return options;
}

/**
 * Show a modal
 * @param {HTMLElement|jQuery} element Modal element
 * @param {object} [options] Options
 */
export function showModal(element, options) {
  getModal(element, options).show();
}

/**
 * Hide a modal
 * @param {HTMLElement|jQuery} element Modal element
 */
export function hideModal(element) {
  getModal(element).hide();
}

/**
 * Check if a modal is shown
 * @param {HTMLElement|jQuery} element Modal element
 * @returns {boolean} The modal is shown
 */
export function isModalShown(element) {
  const modal = $(element).data(DATA_KEY);
  return !!modal && modal.isShown;
}

/**
 * Get rid of a modal which is about to be removed: it hides at once, without events, and leaves no backdrop
 * @param {HTMLElement|jQuery} element Modal element
 */
export function disposeModal(element) {
  const modal = $(element).data(DATA_KEY);
  if (modal) {
    modal.dispose();
  }
}

// Modal service: the public API to open and close the modals of AWE (dialogs, side panels, confirm...)
aweApplication.factory("AweModal", () => ({
  show: showModal,
  hide: hideModal,
  isShown: isModalShown,
  dispose: disposeModal
}));
