import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";

/**
 * Test hooks for DOM that AWE does not render itself (select2, bootstrap-datepicker, tabdrop, Bootstrap popovers,
 * the angular-loading-bar elements and the UI Bootstrap alert).
 *
 * Everything here uses the official extension points of the plugins (formatters, plugin events, plugin methods).
 * Vendor code is never patched, and no class or id is removed or renamed: hooks are additive data attributes.
 */
aweApplication
  .constant("TestIds", TestIds)
  .constant("TestAttributes", TestAttributes);

/** Template registered for the UI Bootstrap alert so its close button carries a hook */
export const ALERT_TEMPLATE_URL = "awe/template/alert.html";

const ALERT_TEMPLATE = `<button ng-show="closeable" type="button" class="close" ng-click="close({$event: $event})" data-testid="${TestIds.alertClose}">
  <span aria-hidden="true">&times;</span>
  <span class="sr-only">Close</span>
</button>
<div ng-transclude></div>
`;

const LOADING_BAR_TEMPLATE = `<div id="loading-bar" ${TestAttributes.testId}="${TestIds.loadingBar}"><div class="bar"><div class="peg"></div></div></div>`;
const LOADING_SPINNER_TEMPLATE = `<div id="loading-bar-spinner" ${TestAttributes.testId}="${TestIds.loadingSpinner}"><div class="spinner-icon"></div></div>`;

// The loading bar and the alert are rendered by third party directives: the templates they officially accept add the hooks
aweApplication
  .config(["cfpLoadingBarProvider", function (loadingBarProvider) {
    loadingBarProvider.loadingBarTemplate = LOADING_BAR_TEMPLATE;
    loadingBarProvider.spinnerTemplate = LOADING_SPINNER_TEMPLATE;
  }])
  .run(["$templateCache", function ($templateCache) {
    $templateCache.put(ALERT_TEMPLATE_URL, ALERT_TEMPLATE);
  }]);

const SELECT2_SEARCH = ".select2-input";
const SELECT2_CHOSEN = ".select2-chosen";
const SELECT2_CHOICE = ".select2-search-choice";
const SELECT2_CHOICE_CLOSE = ".select2-search-choice-close";
const DATEPICKER_EVENTS_NAMESPACE = "testid";

/**
 * Resolve an owner that can be given as a value or as a function evaluated when the hook is applied
 * @param {string|function} owner
 * @return {string|undefined} Owner id
 */
function resolveOwner(owner) {
  return typeof owner === "function" ? owner() : owner;
}

/**
 * Tag elements with a vocabulary value and, optionally, the id of the component that owns them
 * @param {jQuery} elements Elements to tag
 * @param {string} testId Vocabulary value
 * @param {string} [owner] Owner component id
 * @return {jQuery} Tagged elements
 */
function tag(elements, testId, owner) {
  if (elements && elements.length) {
    elements.attr(TestAttributes.testId, testId);
    if (owner) {
      elements.attr(TestAttributes.owner, owner);
    }
  }
  return elements;
}

/**
 * Remove the vocabulary value and the owner from elements
 * @param {jQuery} elements Elements to clean
 */
function untag(elements) {
  if (elements && elements.length) {
    elements.removeAttr(TestAttributes.testId).removeAttr(TestAttributes.owner);
  }
}

/**
 * Expose a boolean state as a data attribute
 * @param {jQuery} element Element
 * @param {string} attribute Attribute name
 * @param {boolean} value State
 */
function setState(element, attribute, value) {
  element.attr(attribute, value ? "true" : "false");
}

/**
 * Wrap a select2 "formatResult" so the option label carries the select-option hook.
 * The markup is produced by the original formatter (the select2 default when none is given).
 * @param {function} [original] Formatter to wrap
 * @param {string|function} owner Id of the selector that owns the options
 * @return {function} select2 formatResult function
 */
export function wrapSelect2FormatResult(original, owner) {
  return function formatResult(result, label, ...rest) {
    const delegate = original || $.fn.select2.defaults.formatResult;
    const markup = delegate.call(this, result, label, ...rest);
    tag($(label), TestIds.selectOption, resolveOwner(owner));
    return markup;
  };
}

/**
 * Wrap a select2 "formatSelection" so the chosen value (single) or the choice item (multiple) carries its hook.
 * The markup is produced by the original formatter (the select2 default when none is given).
 * @param {function} [original] Formatter to wrap
 * @param {string|function} owner Id of the selector that owns the selection
 * @return {function} select2 formatSelection function
 */
export function wrapSelect2FormatSelection(original, owner) {
  return function formatSelection(data, container, ...rest) {
    const delegate = original || $.fn.select2.defaults.formatSelection;
    const markup = delegate.call(this, data, container, ...rest);
    const target = $(container);
    if (target.is(SELECT2_CHOSEN)) {
      tag(target, TestIds.selectValue, resolveOwner(owner));
    } else {
      // select2 replaces the inner div of a multiple choice, so the hook goes on the choice item
      const choice = target.closest(SELECT2_CHOICE);
      tag(choice, TestIds.selectChoice, resolveOwner(owner));
      tag(choice.children(SELECT2_CHOICE_CLOSE), TestIds.selectChoiceClose, resolveOwner(owner));
    }
    return markup;
  };
}

/**
 * Tag the parts of a select2 instance: container, search input and the single global dropdown while it is open.
 * select2 3.x moves the dropdown to the body when it opens, so it is tagged with the owner on "select2-open"
 * and untagged on "select2-close": at any time only the open dropdown carries the select-dropdown hook.
 * @param {jQuery} elem Element select2 was initialized on
 * @param {string|function} owner Id of the selector that owns the plugin DOM
 * @return {{unbind: function}} Handle to remove the listeners
 */
export function bindSelect2TestIds(elem, owner) {
  // Official plugin methods are only available once the plugin instance exists
  const part = name => elem.data("select2") ? elem.select2(name) : undefined;
  const tagSearch = root => root && tag(root.find(SELECT2_SEARCH), TestIds.selectSearch, resolveOwner(owner));

  const container = part("container");
  if (container) {
    tag(container, TestIds.select, resolveOwner(owner));
    tagSearch(container);
  }

  elem.on("select2-open.testid", () => {
    const dropdown = part("dropdown");
    if (dropdown) {
      tag(dropdown, TestIds.selectDropdown, resolveOwner(owner));
      tagSearch(dropdown);
    }
  });
  elem.on("select2-close.testid", () => {
    const dropdown = part("dropdown");
    if (dropdown) {
      untag(dropdown.find(SELECT2_SEARCH));
      untag(dropdown);
    }
  });

  return {unbind: () => elem.off(".testid")};
}

/**
 * Tag a day, month or year cell of the datepicker with its state
 * @param {jQuery} cell Cell
 * @param {string} testId Vocabulary value
 * @param {string} owner Owner component id
 * @param {boolean} [withOutsideMonth] Expose whether the cell belongs to an adjacent month
 */
function tagDatepickerCell(cell, testId, owner, withOutsideMonth) {
  tag(cell, testId, owner);
  cell.each((index, node) => {
    const item = $(node);
    setState(item, TestAttributes.selected, item.hasClass("active") || item.hasClass("selected"));
    setState(item, TestAttributes.active, item.hasClass("focused"));
    setState(item, TestAttributes.disabled, item.hasClass("disabled"));
    if (withOutsideMonth) {
      setState(item, TestAttributes.outsideMonth, item.hasClass("old") || item.hasClass("new"));
    }
  });
}

/**
 * Tag the datepicker popup and its day, month and year cells. State is read from the plugin classes and exposed as
 * data attributes, so tests do not depend on them. It is idempotent: call it again after the plugin re-renders.
 * @param {jQuery} popup Datepicker popup
 * @param {string|function} owner Id of the criterion that owns the popup
 */
export function tagDatepickerPopup(popup, owner) {
  const ownerId = resolveOwner(owner);
  tag(popup, TestIds.datepicker, ownerId);
  tagDatepickerCell(popup.find(".datepicker-days td.day"), TestIds.datepickerDay, ownerId, true);
  tagDatepickerCell(popup.find(".datepicker-months span.month"), TestIds.datepickerMonth, ownerId);
  tagDatepickerCell(popup.find(".datepicker-years span.year"), TestIds.datepickerYear, ownerId);
}

/**
 * Find the popup of a datepicker instance. There is deliberately no fallback to a popup found in the body: with
 * several criteria it could belong to another one, and a missing hook is safer than a wrong owner.
 * @param {jQuery} elem Element the datepicker was initialized on
 * @return {jQuery|undefined} Popup
 */
function findDatepickerPopup(elem) {
  const instance = elem.data("datepicker");
  return instance && instance.picker;
}

/**
 * Tag the datepicker popup when the plugin shows it and keep the cells tagged while the popup is open.
 * The plugin re-renders its cells on navigation and view changes, so the popup is observed until it is hidden.
 * @param {jQuery} elem Element the datepicker was initialized on
 * @param {string|function} owner Id of the criterion that owns the popup
 * @return {{unbind: function}} Handle to remove the listeners
 */
export function bindDatepickerTestIds(elem, owner) {
  let observer = null;
  const disconnect = () => {
    if (observer) {
      observer.disconnect();
      observer = null;
    }
  };

  elem.on(`show.${DATEPICKER_EVENTS_NAMESPACE}`, () => {
    const popup = findDatepickerPopup(elem);
    if (!popup || !popup.length) {
      return;
    }
    tagDatepickerPopup(popup, owner);
    disconnect();
    if (typeof MutationObserver !== "undefined") {
      observer = new MutationObserver(() => tagDatepickerPopup(popup, owner));
      // Only class changes are observed: the hooks are data attributes, so tagging never retriggers the observer
      observer.observe(popup[0], {childList: true, subtree: true, attributes: true, attributeFilter: ["class"]});
    }
  });
  elem.on(`hide.${DATEPICKER_EVENTS_NAMESPACE}`, disconnect);

  return {
    unbind: () => {
      disconnect();
      elem.off(`.${DATEPICKER_EVENTS_NAMESPACE}`);
    }
  };
}

/**
 * Tag the dropdown that tabdrop creates ("more" toggle and the menu that receives the tabs that do not fit).
 * It uses the plugin instance, so the in-repo tabdrop source is not touched. The tabs moved into the menu keep their
 * own hooks, because they are AWE markup.
 * @param {jQuery} elem Tab list the plugin was initialized on
 */
export function tagTabdrop(elem) {
  const instance = elem.data("tabdrop");
  const dropdown = instance && instance.dropdown;
  if (dropdown && dropdown.length) {
    tag(dropdown, TestIds.tabdrop);
    tag(dropdown.children("a.dropdown-toggle"), TestIds.tabdropToggle);
    tag(dropdown.children("ul.dropdown-menu"), TestIds.tabdropMenu);
  }
}

/**
 * Escape a value for a double quoted HTML attribute
 * @param {string} value Value
 * @return {string} Escaped value
 */
function escapeAttribute(value) {
  return String(value).replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/**
 * Build the template of a Bootstrap popover. It is the default Bootstrap template plus the hooks, so the plugin keeps
 * working with the same classes.
 * @param {string} type Message type (success, info, warning, danger)
 * @param {string} [owner] Id of the component the message points at
 * @return {string} Popover template
 */
export function popoverTemplate(type, owner) {
  const ownerAttribute = owner ? ` ${TestAttributes.owner}="${escapeAttribute(owner)}"` : "";
  return `<div class="popover" role="tooltip" ${TestAttributes.testId}="${TestIds.popover}" ${TestAttributes.type}="${escapeAttribute(type)}"${ownerAttribute}>`
    + `<div class="arrow"></div>`
    + `<h3 class="popover-title" ${TestAttributes.testId}="${TestIds.popoverTitle}"></h3>`
    + `<div class="popover-content" ${TestAttributes.testId}="${TestIds.popoverContent}"></div></div>`;
}

/**
 * Options that make a Bootstrap popover render the test hooks, through the official "template" and "whiteList" options.
 * Bootstrap sanitizes the template and removes every attribute that is not in its allow list, so the hook attributes
 * are added to a copy of the list (the plugin defaults stay untouched).
 * @param {string} type Message type (success, info, warning, danger)
 * @param {string} [owner] Id of the component the message points at
 * @return {{template: string, whiteList: object|undefined}} Popover options
 */
export function popoverOptions(type, owner) {
  const defaults = $.fn.popover && $.fn.popover.Constructor && $.fn.popover.Constructor.DEFAULTS.whiteList;
  const hooks = [TestAttributes.testId, TestAttributes.owner, TestAttributes.type];
  return {
    template: popoverTemplate(type, owner),
    whiteList: defaults ? {...defaults, "*": [...(defaults["*"] || []), ...hooks]} : undefined
  };
}
