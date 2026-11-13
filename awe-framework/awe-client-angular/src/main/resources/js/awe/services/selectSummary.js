// Select multiple of one line (#922): the chips that fit in the field and a "+K" counter that opens the list of all of them
const CHIP = ".select2-search-choice";
const HIDDEN = "awe-select-summary-hidden";
// While measuring, the chips keep their own width instead of shrinking to the row
const MEASURING = "awe-select-summary-measuring";
// Class of the container the layout hangs on. select2 drops the classes it does not know on every sync of its element,
// so the plugin also gets it as containerCssClass
export const SUMMARY_CLASS = "awe-select-summary";
// Space between the panel and the field
const PANEL_GAP = 4;

/**
 * How many chips of a row are shown. They go in order and all of them when they fit; when some do not, the counter of
 * the hidden ones keeps its room. At least one chip stays: the layout cuts its text with an ellipsis.
 * (The same arithmetic as the React client.)
 *
 * @param {number[]} widths Natural width of every chip
 * @param {number} available Width of the row
 * @param {number} counterWidth Width of the "+K" counter
 * @param {number} gap Space between two items of the row
 * @returns {number} Number of chips to show
 */
export function visibleChipCount(widths, available, counterWidth, gap) {
  const rowWidth = count => widths.slice(0, count).reduce((sum, width) => sum + width, 0) + gap * Math.max(count - 1, 0);
  if (rowWidth(widths.length) <= available) {
    return widths.length;
  }
  let count = widths.length - 1;
  while (count > 1 && rowWidth(count) + gap + counterWidth > available) {
    count--;
  }
  return Math.max(count, 1);
}

/**
 * Show the values of a select2 multiple as one line: the chips that fit and a counter with a popover of all of them
 *
 * @param {jQuery} container select2 container of the multiple selector
 * @param {function} translate Translation of a key with its parameters
 * @param {function} remove Called with the position of the value to remove
 * @returns {{refresh: function, destroy: function}} Refresh (after a change of the values or the size) and destroy
 */
export function attachSelectSummary({container, translate, remove}) {
  const choices = container.find(".select2-choices");
  const search = choices.find(".select2-search-field");
  const item = $(`<li class="awe-select-summary-more-item" hidden>
    <button type="button" class="awe-cell-chip awe-cell-chip-more awe-select-summary-more" aria-haspopup="dialog" aria-expanded="false"></button>
  </li>`).insertBefore(search);
  const counter = item.find("button");
  let panel = null;
  let removeListeners = () => {};

  const chips = () => choices.find(CHIP);
  const labels = () => chips().map((index, chip) => $(chip).text().trim()).get();
  const px = (element, property) => parseFloat(element.css(property)) || 0;

  // Clicks on the counter must not reach the field: select2 would open its options
  counter.on("mousedown mouseup touchstart click", event => event.stopPropagation());

  const close = giveFocusBack => {
    if (panel) {
      panel.remove();
      panel = null;
      removeListeners();
      counter.attr("aria-expanded", "false");
      if (giveFocusBack) {
        counter.trigger("focus");
      }
    }
  };

  const renderRows = () => {
    const values = labels();
    panel.attr("aria-label", counter.attr("aria-label"));
    panel.empty().append($("<ul class='awe-select-summary-list'></ul>").append(values.map((label, index) => {
      const row = $("<li><span></span><button type='button'><span aria-hidden='true'>&times;</span></button></li>");
      row.find("span").first().text(label).attr("title", label);
      row.find("button").attr("aria-label", translate("SCREEN_TEXT_REMOVE_VALUE", {label})).on("click", () => remove(index));
      return row;
    })));
    if (document.activeElement === document.body) {
      panel.find("button").first().trigger("focus");
    }
  };

  const open = () => {
    panel = $("<div class='awe-select-summary-panel' role='dialog' tabindex='-1'></div>").appendTo(document.body);
    const field = choices[0].getBoundingClientRect();
    panel.css({top: field.bottom + PANEL_GAP, left: field.left, minWidth: field.width});
    renderRows();
    counter.attr("aria-expanded", "true");
    const closeOutside = event => {
      if (!panel[0].contains(event.target) && !counter[0].contains(event.target)) {
        close(false);
      }
    };
    const closeOnEscape = event => {
      if (event.key === "Escape") {
        close(true);
      }
    };
    const closeOnScroll = event => {
      // The scroll of the page is reported on the document, and the one of a container on that container
      if (!(event.target instanceof window.Node) || !panel[0].contains(event.target)) {
        close(false);
      }
    };
    const closeOnResize = () => close(false);
    document.addEventListener("mousedown", closeOutside, true);
    document.addEventListener("keydown", closeOnEscape, true);
    window.addEventListener("scroll", closeOnScroll, true);
    window.addEventListener("resize", closeOnResize);
    removeListeners = () => {
      document.removeEventListener("mousedown", closeOutside, true);
      document.removeEventListener("keydown", closeOnEscape, true);
      window.removeEventListener("scroll", closeOnScroll, true);
      window.removeEventListener("resize", closeOnResize);
    };
  };

  counter.on("click", () => panel ? close(false) : open());

  const refresh = () => {
    // select2 adds a new chip just before its search field: the counter goes back after the chips (moved only when
    // needed, as every move is a change of the row that calls this again)
    if (item.next()[0] !== search[0]) {
      item.insertBefore(search);
    }
    const all = chips().removeClass(HIDDEN);
    all.each((index, chip) => chip.setAttribute("title", $(chip).text().trim()));
    // The counter is measured with its widest text, the number of all the chips
    item.prop("hidden", !all.length);
    counter.text(`+${all.length}`);
    const gap = px(choices, "column-gap");
    const room = choices[0].clientWidth - px(choices, "padding-left") - px(choices, "padding-right")
      - px(search, "min-width") - gap;
    container.addClass(MEASURING);
    const widths = all.map((index, chip) => chip.scrollWidth).get();
    container.removeClass(MEASURING);
    const hidden = all.length - visibleChipCount(widths, room, item[0].offsetWidth, gap);
    all.slice(all.length - hidden).addClass(HIDDEN);
    item.prop("hidden", hidden === 0);
    if (hidden > 0) {
      counter.text(`+${hidden}`).attr("aria-label", translate("SCREEN_TEXT_SELECTED_VALUES_SHOW_ALL", {count: hidden}));
    }
    if (panel) {
      hidden > 0 ? renderRows() : close(false);
    }
  };

  container.addClass(SUMMARY_CLASS);
  return {
    refresh,
    destroy: () => {
      close(false);
      item.remove();
      container.removeClass(SUMMARY_CLASS);
    }
  };
}
