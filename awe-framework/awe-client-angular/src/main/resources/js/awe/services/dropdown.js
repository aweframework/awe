import $ from "jquery";

/**
 * Native dropdown behavior (it replaces the data-api of the Bootstrap 3.4.1 dropdown plugin, which AWE no longer ships).
 *
 * An element with `data-toggle="dropdown"` opens and closes its parent (or the element of `data-target`) by toggling
 * the `open` class, and keeps `aria-expanded` on the toggle. A click anywhere else (also on an item of the menu) or
 * Esc closes it, and opening a menu closes the others. The arrows move the focus between the items and the space bar
 * opens the menu. The jQuery events `show.bs.dropdown`, `shown.bs.dropdown`, `hide.bs.dropdown` and
 * `hidden.bs.dropdown` are triggered on the dropdown with the toggle as `relatedTarget`.
 *
 * The handlers are delegated to the document: they work for the toggles which are created later (Angular templates,
 * the overflow menu of the tab strip).
 */

const TOGGLE = "[data-toggle=\"dropdown\"]";
const BACKDROP = ".dropdown-backdrop";
const NAMESPACE = ".awe.dropdown";
const KEYS = /(38|40|27|32)/;
const ESCAPE_KEY = 27;
const UP_KEY = 38;
const DOWN_KEY = 40;
const RIGHT_BUTTON = 3;
const FIELD = /input|textarea/i;

/**
 * Get the anchor of an href ("page.html#holder" gives "#holder"): from the last "#", when no space follows it
 * @param {string} [href] Link
 * @returns {string|boolean} Selector of the anchor, or false when the link does not point at an element of the page
 */
const anchorOf = href => {
  if (!href || !/#[A-Za-z]/.test(href)) {
    return false;
  }
  const anchor = href.slice(href.lastIndexOf("#"));
  return /\s/.test(anchor) ? href : anchor;
};

/**
 * Get the element which opens and closes with a toggle: the data-target, the anchor of the href, or the parent
 * @param {jQuery} $toggle Toggle
 * @returns {jQuery} Dropdown
 */
const getParent = $toggle => {
  const selector = $toggle.attr("data-target") || anchorOf($toggle.attr("href"));
  const $parent = selector !== "#" ? $(document).find(selector) : null;
  return $parent?.length ? $parent : $toggle.parent();
};

/**
 * Close the open menus
 * @param {jQuery.Event} [event] Event which closes them
 */
const clearMenus = event => {
  if (event?.which === RIGHT_BUTTON) {
    return;
  }
  $(BACKDROP).remove();
  $(TOGGLE).each(function () {
    const $toggle = $(this);
    const $parent = getParent($toggle);
    const relatedTarget = {relatedTarget: this};

    if (!$parent.hasClass("open")) {
      return;
    }

    // A click in a field of the menu does not close it
    if (event?.type === "click" && FIELD.test(event.target.tagName) && $.contains($parent[0], event.target)) {
      return;
    }

    const hideEvent = $.Event("hide.bs.dropdown", relatedTarget);
    $parent.trigger(hideEvent);

    if (hideEvent.isDefaultPrevented()) {
      return;
    }

    $toggle.attr("aria-expanded", "false");
    $parent.removeClass("open").trigger($.Event("hidden.bs.dropdown", relatedTarget));
  });
};

/**
 * Open or close the menu of a toggle (the handler of its click)
 * @param {jQuery.Event} event Click
 * @returns {boolean|undefined} False to cancel the default action and the propagation
 */
function toggleMenu() {
  const $toggle = $(this);

  if ($toggle.is(".disabled, :disabled")) {
    return;
  }

  const $parent = getParent($toggle);
  const isActive = $parent.hasClass("open");

  clearMenus();

  if (!isActive) {
    const relatedTarget = {relatedTarget: this};
    const showEvent = $.Event("show.bs.dropdown", relatedTarget);
    $parent.trigger(showEvent);

    // A listener cancelled the opening: nothing opens, there is no backdrop, and the toggle (a link) does not navigate
    if (showEvent.isDefaultPrevented()) {
      return false;
    }

    if ("ontouchstart" in document.documentElement && !$parent.closest(".navbar-nav").length) {
      // On a touch screen the clicks outside do not reach the document: a backdrop catches them
      $(document.createElement("div"))
        .addClass("dropdown-backdrop")
        .insertAfter($toggle)
        .on("click", clearMenus);
    }

    $toggle
      .trigger("focus")
      .attr("aria-expanded", "true");

    $parent
      .toggleClass("open")
      .trigger($.Event("shown.bs.dropdown", relatedTarget));
  }

  return false;
}

/**
 * Handle the keys of a toggle or of its menu: the arrows move between the items, Esc closes and the space bar opens
 * @param {jQuery.Event} event Keydown
 * @returns {*} The result of the click it triggers
 */
function keydown(event) {
  if (!KEYS.test(event.which) || FIELD.test(event.target.tagName)) {
    return;
  }

  const $this = $(this);

  event.preventDefault();
  event.stopPropagation();

  if ($this.is(".disabled, :disabled")) {
    return;
  }

  const $parent = getParent($this);
  const isActive = $parent.hasClass("open");

  if ((!isActive && event.which !== ESCAPE_KEY) || (isActive && event.which === ESCAPE_KEY)) {
    if (event.which === ESCAPE_KEY) {
      $parent.find(TOGGLE).trigger("focus");
    }
    return $this.trigger("click");
  }

  const $items = $parent.find(".dropdown-menu li:not(.disabled):visible a");

  if (!$items.length) {
    return;
  }

  let index = $items.index(event.target);

  if (event.which === UP_KEY && index > 0) {
    index--;
  }
  if (event.which === DOWN_KEY && index < $items.length - 1) {
    index++;
  }
  if (index < 0) {
    index = 0;
  }

  $items.eq(index).trigger("focus");
}

$(document)
  .off(NAMESPACE)
  .on("click" + NAMESPACE, clearMenus)
  .on("click" + NAMESPACE, ".dropdown form", event => event.stopPropagation())
  .on("click" + NAMESPACE, TOGGLE, toggleMenu)
  .on("keydown" + NAMESPACE, TOGGLE, keydown)
  .on("keydown" + NAMESPACE, ".dropdown-menu", keydown);
