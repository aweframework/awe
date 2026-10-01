import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";

/**
 * Test hooks for DOM that AWE does not render itself (select2 and bootstrap-datepicker).
 *
 * Everything here uses the official extension points of the plugins (formatters, plugin events, plugin methods).
 * Vendor code is never patched, and no class or id is removed or renamed: hooks are additive data attributes.
 */
aweApplication
  .constant("TestIds", TestIds)
  .constant("TestAttributes", TestAttributes);

const SELECT2_SEARCH = ".select2-input";
const SELECT2_CHOSEN = ".select2-chosen";
const SELECT2_CHOICE = ".select2-search-choice";
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
      tag(target.closest(SELECT2_CHOICE), TestIds.selectChoice, resolveOwner(owner));
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
