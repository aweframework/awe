import {attachSelectSummary, visibleChipCount} from "../../../main/resources/js/awe/services/selectSummary";

describe("visibleChipCount", () => {
  it("shows every chip when they all fit", () => {
    expect(visibleChipCount([50, 50], 104, 24, 4)).toBe(2);
  });

  it("leaves room for the counter of the chips that do not fit", () => {
    expect(visibleChipCount([60, 60, 60], 130, 24, 0)).toBe(1);
    expect(visibleChipCount([60, 60, 60], 150, 24, 0)).toBe(2);
  });

  it("keeps the first chip even when it does not fit alone", () => {
    expect(visibleChipCount([400, 60], 100, 24, 4)).toBe(1);
  });
});

describe("attachSelectSummary (select2 multiple field of one line, #922)", () => {
  const translate = (key, params) => `${key}:${JSON.stringify(params)}`;
  let container, summary, remove;

  // select2 3.x markup of a multiple selector, with the size of the layout (jsdom has none) given by the test
  const build = (labels, {available, widths, counter = 24}) => {
    document.body.innerHTML = `<div class="select2-container select2-container-multi"><ul class="select2-choices">
      ${labels.map(label => `<li class="select2-search-choice"><div>${label}</div><a href="#" class="select2-search-choice-close" tabindex="-1"></a></li>`).join("")}
      <li class="select2-search-field"><input type="text" class="select2-input"/></li></ul></div>`;
    container = $(document.body).find(".select2-container");
    Object.defineProperty(container.find(".select2-choices")[0], "clientWidth", {value: available});
    container.find(".select2-search-choice").each((index, chip) => Object.defineProperty(chip, "scrollWidth", {configurable: true, value: widths[index]}));
    Object.defineProperty(window.HTMLElement.prototype, "offsetWidth", {configurable: true, get: () => counter});
    remove = jest.fn();
    summary = attachSelectSummary({container, translate, remove});
    summary.refresh();
  };
  const counterButton = () => container.find("button.awe-select-summary-more");
  const panel = () => $(document.body).find(".awe-select-summary-panel");

  afterEach(() => {
    summary.destroy();
    delete window.HTMLElement.prototype.offsetWidth;
    document.body.innerHTML = "";
  });

  it("shows every chip and no counter when they fit", () => {
    build(["A", "B"], {available: 200, widths: [50, 50]});

    expect(container.find(".awe-select-summary-hidden").length).toBe(0);
    expect(counterButton().closest("li").prop("hidden")).toBe(true);
  });

  it("measures the chips at their natural width, not shrunk by the row (#922)", () => {
    build(["A", "B", "C"], {available: 130, widths: [0, 0, 0]});
    // In the browser the chips of the row shrink to fit: only while measuring do they keep their own width
    container.find(".select2-search-choice").each((index, chip) => Object.defineProperty(chip, "scrollWidth",
      {configurable: true, get: () => container.hasClass("awe-select-summary-measuring") ? 60 : 30}));
    summary.refresh();

    expect(container.hasClass("awe-select-summary-measuring")).toBe(false);
    expect(counterButton().text()).toBe("+2");
  });

  it("keeps the counter after the chips that select2 adds later (#922)", () => {
    build(["A"], {available: 200, widths: [50]});
    // select2 puts a new chip just before its search field, so after the counter
    $('<li class="select2-search-choice"><div>B</div></li>').insertBefore(container.find(".select2-search-field"));
    summary.refresh();

    expect(container.find(".select2-choices > li").last().prev().hasClass("awe-select-summary-more-item")).toBe(true);
  });

  it("hides the chips that do not fit and counts them in a button that names its popup", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});

    expect(container.find(".select2-search-choice.awe-select-summary-hidden").length).toBe(2);
    expect(counterButton().closest("li").prop("hidden")).toBe(false);
    expect(counterButton().text()).toBe("+2");
    expect(counterButton().attr("aria-haspopup")).toBe("dialog");
    expect(counterButton().attr("aria-expanded")).toBe("false");
    expect(counterButton().attr("aria-label")).toBe('SCREEN_TEXT_SELECTED_VALUES_SHOW_ALL:{"count":2}');
  });

  it("gives every chip its full label as title", () => {
    build(["Alpha", "Beta"], {available: 200, widths: [50, 50]});

    expect(container.find(".select2-search-choice").map((index, chip) => chip.title).get()).toEqual(["Alpha", "Beta"]);
  });

  it("opens a popover under the field with every value and a remove button each", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});

    counterButton().trigger("click");

    expect(panel().attr("role")).toBe("dialog");
    expect(panel().find("li").map((index, row) => $(row).find("span").first().text()).get()).toEqual(["A", "B", "C"]);
    expect(panel().find("button").map((index, button) => button.getAttribute("aria-label")).get())
      .toEqual(["A", "B", "C"].map(label => `SCREEN_TEXT_REMOVE_VALUE:{"label":"${label}"}`));
    expect(counterButton().attr("aria-expanded")).toBe("true");
  });

  it("asks to remove the value of the row", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    counterButton().trigger("click");

    panel().find("li:nth-child(2) button").trigger("click");

    expect(remove).toHaveBeenCalledWith(1);
  });

  it("does not let the click on the counter reach the field, which would open its options", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    const reachesField = jest.fn();
    container.on("mousedown click", reachesField);

    counterButton().trigger("mousedown").trigger("click");

    expect(reachesField).not.toHaveBeenCalled();
  });

  it("closes with Escape and gives the focus back to the counter", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    counterButton().trigger("click");

    document.dispatchEvent(new window.KeyboardEvent("keydown", {key: "Escape", bubbles: true}));

    expect(panel().length).toBe(0);
    expect(counterButton().attr("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(counterButton()[0]);
  });

  it("closes with a click outside, but not with a click inside", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    counterButton().trigger("click");

    panel().find("span")[0].dispatchEvent(new MouseEvent("mousedown", {bubbles: true}));
    expect(panel().length).toBe(1);
    document.body.dispatchEvent(new MouseEvent("mousedown", {bubbles: true}));

    expect(panel().length).toBe(0);
  });

  it.each(["scroll", "resize"])("closes on %s", eventName => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    counterButton().trigger("click");

    (eventName === "scroll" ? document : window).dispatchEvent(new Event(eventName));

    expect(panel().length).toBe(0);
  });

  it("updates the popover and the counter when a value is removed", () => {
    build(["A", "B", "C"], {available: 100, widths: [60, 60, 60]});
    counterButton().trigger("click");

    container.find(".select2-search-choice").eq(1).remove();
    summary.refresh();

    expect(panel().find("li").length).toBe(2);
    expect(counterButton().text()).toBe("+1");
  });

  it("closes the popover when no chip is hidden any more", () => {
    build(["A", "B", "C"], {available: 130, widths: [60, 60, 60]});
    counterButton().trigger("click");

    container.find(".select2-search-choice").slice(1).remove();
    summary.refresh();

    expect(panel().length).toBe(0);
  });
});
