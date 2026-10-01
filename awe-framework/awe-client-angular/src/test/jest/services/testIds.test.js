import "../components/lightComponentTestUtils";

import {TestAttributes, TestIds} from "../../../main/resources/js/awe/data/testIds";
import {
  bindDatepickerTestIds,
  bindSelect2TestIds,
  tagDatepickerPopup,
  wrapSelect2FormatResult,
  wrapSelect2FormatSelection
} from "../../../main/resources/js/awe/services/testIds";

const flushObservers = () => new Promise(resolve => setTimeout(resolve, 0));

describe("test id vocabulary", () => {
  it("exposes a small frozen kebab-case vocabulary of component parts", () => {
    expect(Object.isFrozen(TestIds)).toBe(true);
    expect(TestIds).toEqual({
      criterionInput: "criterion-input",
      select: "select",
      selectValue: "select-value",
      selectSearch: "select-search",
      selectChoice: "select-choice",
      selectDropdown: "select-dropdown",
      selectOption: "select-option",
      datepicker: "datepicker",
      datepickerDay: "datepicker-day",
      datepickerMonth: "datepicker-month",
      datepickerYear: "datepicker-year",
      uploadFilename: "upload-filename",
      uploadClear: "upload-clear"
    });
    Object.keys(TestIds).map(name => TestIds[name]).forEach(value => expect(value).toMatch(/^[a-z]+(-[a-z]+)*$/));
  });

  it("exposes the attribute names used to carry test hooks", () => {
    expect(Object.isFrozen(TestAttributes)).toBe(true);
    expect(TestAttributes).toEqual({
      testId: "data-testid",
      owner: "data-testid-owner",
      selected: "data-selected",
      active: "data-active",
      disabled: "data-disabled",
      outsideMonth: "data-outside-month"
    });
  });

  it("is registered as an injectable constant", () => {
    angular.mock.module("aweApplication");
    inject(["TestIds", "TestAttributes", (injectedIds, injectedAttributes) => {
      expect(injectedIds).toBe(TestIds);
      expect(injectedAttributes).toBe(TestAttributes);
    }]);
  });
});

describe("select2 test hooks", () => {
  const escapeMarkup = text => text.replace(/&/g, "&amp;").replace(/</g, "&lt;");
  let originalSelect2;

  beforeEach(() => {
    originalSelect2 = $.fn.select2;
    $.fn.select2 = jest.fn();
    $.fn.select2.defaults = {
      formatResult(result, container, query, escape) {
        return escape(this.text(result));
      },
      formatSelection(data, container, escape) {
        return data ? escape(this.text(data)) : undefined;
      }
    };
  });

  afterEach(() => {
    $.fn.select2 = originalSelect2;
  });

  it("keeps the default option markup and tags the option label with the owner", () => {
    const wrapped = wrapSelect2FormatResult(undefined, "country");
    const label = $("<div class='select2-result-label'></div>");
    const options = {text: item => item.text};

    const markup = wrapped.call(options, {id: "1", text: "<Spain>"}, label, {term: ""}, escapeMarkup);

    expect(markup).toBe("&lt;Spain>");
    expect(label.attr("data-testid")).toBe("select-option");
    expect(label.attr("data-testid-owner")).toBe("country");
  });

  it("delegates to a custom formatResult when one is configured", () => {
    const custom = jest.fn(function custom(result) {
      return `<b>${result.text}</b>`;
    });
    const wrapped = wrapSelect2FormatResult(custom, "country");
    const label = $("<div></div>");

    const markup = wrapped.call({}, {text: "Spain"}, label, {term: ""}, escapeMarkup);

    expect(markup).toBe("<b>Spain</b>");
    expect(custom).toHaveBeenCalled();
    expect(label.attr("data-testid")).toBe("select-option");
  });

  it("tags the chosen value of a single select and keeps the default text", () => {
    const wrapped = wrapSelect2FormatSelection(undefined, "country");
    const container = $("<span class='select2-chosen'></span>");

    const markup = wrapped.call({text: item => item.text}, {id: "1", text: "Spain"}, container, escapeMarkup);

    expect(markup).toBe("Spain");
    expect(container.attr("data-testid")).toBe("select-value");
    expect(container.attr("data-testid-owner")).toBe("country");
  });

  it("tags the choice item of a multiple select instead of its inner div", () => {
    const wrapped = wrapSelect2FormatSelection(undefined, "countries");
    const choice = $("<li class='select2-search-choice'><div></div></li>");

    const markup = wrapped.call({text: item => item.text}, {id: "1", text: "Spain"}, choice.find("div"), escapeMarkup);

    expect(markup).toBe("Spain");
    expect(choice.attr("data-testid")).toBe("select-choice");
    expect(choice.attr("data-testid-owner")).toBe("countries");
    expect(choice.find("div").attr("data-testid")).toBeUndefined();
  });

  it("returns undefined for an empty selection without failing", () => {
    const wrapped = wrapSelect2FormatSelection(undefined, "country");
    const container = $("<span class='select2-chosen'></span>");

    expect(wrapped.call({text: () => ""}, null, container, escapeMarkup)).toBeUndefined();
  });

  describe("bindSelect2TestIds", () => {
    let element;
    let container;
    let dropdown;

    beforeEach(() => {
      container = $("<div class='select2-container'><input class='select2-input'/></div>");
      dropdown = $("<div class='select2-drop'><div class='select2-search'><input class='select2-input'/></div></div>");
      element = $("<input type='hidden'/>");
      element.data("select2", {});
      $.fn.select2.mockImplementation(function select2(method) {
        if (method === "container") {
          return container;
        }
        if (method === "dropdown") {
          return dropdown;
        }
        return this;
      });
    });

    it("tags the container and its search input as soon as it is bound", () => {
      bindSelect2TestIds(element, () => "country");

      expect(container.attr("data-testid")).toBe("select");
      expect(container.attr("data-testid-owner")).toBe("country");
      expect(container.find(".select2-input").attr("data-testid")).toBe("select-search");
    });

    it("marks the global dropdown with its owner on open and clears the mark on close", () => {
      bindSelect2TestIds(element, () => "country");

      element.trigger($.Event("select2-open"));

      expect(dropdown.attr("data-testid")).toBe("select-dropdown");
      expect(dropdown.attr("data-testid-owner")).toBe("country");
      expect(dropdown.find(".select2-input").attr("data-testid")).toBe("select-search");
      expect(dropdown.find(".select2-input").attr("data-testid-owner")).toBe("country");

      element.trigger($.Event("select2-close"));

      expect(dropdown.attr("data-testid")).toBeUndefined();
      expect(dropdown.attr("data-testid-owner")).toBeUndefined();
      expect(dropdown.find(".select2-input").attr("data-testid")).toBeUndefined();
      expect(dropdown.find(".select2-input").attr("data-testid-owner")).toBeUndefined();
    });

    it("resolves the owner lazily when the component id is not known at bind time", () => {
      let owner;
      bindSelect2TestIds(element, () => owner);
      owner = "late";

      element.trigger($.Event("select2-open"));

      expect(dropdown.attr("data-testid-owner")).toBe("late");
    });

    it("stops reacting once unbound", () => {
      const binding = bindSelect2TestIds(element, () => "country");

      binding.unbind();
      element.trigger($.Event("select2-open"));

      expect(dropdown.attr("data-testid")).toBeUndefined();
    });

    it("does nothing when the plugin instance is not available", () => {
      const bare = $("<input type='hidden'/>");

      expect(() => bindSelect2TestIds(bare, () => "x").unbind()).not.toThrow();
      expect($.fn.select2).not.toHaveBeenCalledWith("container");
    });
  });
});

describe("datepicker test hooks", () => {
  function buildPopup() {
    return $(`
      <div class="datepicker datepicker-dropdown">
        <div class="datepicker-days"><table><tbody><tr>
          <td class="day old">30</td>
          <td class="day">1</td>
          <td class="day active selected">2</td>
          <td class="day disabled">3</td>
          <td class="day focused">4</td>
          <td class="day new">5</td>
        </tr></tbody></table></div>
        <div class="datepicker-months"><table><tbody><tr><td colspan="7">
          <span class="month">Jan</span><span class="month active">Feb</span><span class="month disabled">Mar</span>
        </td></tr></tbody></table></div>
        <div class="datepicker-years"><table><tbody><tr><td colspan="7">
          <span class="year old">2019</span><span class="year active">2020</span><span class="year disabled">2021</span>
        </td></tr></tbody></table></div>
      </div>`);
  }

  it("tags the popup and every day, month and year cell with its state", () => {
    const popup = buildPopup();

    tagDatepickerPopup(popup, "birthDate");

    expect(popup.attr("data-testid")).toBe("datepicker");
    expect(popup.attr("data-testid-owner")).toBe("birthDate");

    const days = popup.find("[data-testid='datepicker-day']");
    expect(days.length).toBe(6);
    expect(days.eq(0).attr("data-outside-month")).toBe("true");
    expect(days.eq(1).attr("data-outside-month")).toBe("false");
    expect(days.eq(1).attr("data-selected")).toBe("false");
    expect(days.eq(2).attr("data-selected")).toBe("true");
    expect(days.eq(3).attr("data-disabled")).toBe("true");
    expect(days.eq(4).attr("data-active")).toBe("true");
    expect(days.eq(5).attr("data-outside-month")).toBe("true");

    const months = popup.find("[data-testid='datepicker-month']");
    expect(months.length).toBe(3);
    expect(months.eq(1).attr("data-selected")).toBe("true");
    expect(months.eq(2).attr("data-disabled")).toBe("true");

    const years = popup.find("[data-testid='datepicker-year']");
    expect(years.length).toBe(3);
    expect(years.eq(1).attr("data-selected")).toBe("true");
    expect(years.eq(2).attr("data-disabled")).toBe("true");
  });

  it("keeps the library classes untouched", () => {
    const popup = buildPopup();

    tagDatepickerPopup(popup, "birthDate");

    expect(popup.find("td.day.active.selected").length).toBe(1);
    expect(popup.find("span.month.disabled").length).toBe(1);
  });

  describe("bindDatepickerTestIds", () => {
    let element;
    let popup;

    beforeEach(() => {
      popup = buildPopup();
      element = $("<div class='date'></div>");
      element.data("datepicker", {picker: popup});
    });

    it("tags the popup when the plugin shows it", () => {
      bindDatepickerTestIds(element, () => "birthDate");

      element.trigger('show');

      expect(popup.attr("data-testid")).toBe("datepicker");
      expect(popup.find("td.day").first().attr("data-testid")).toBe("datepicker-day");
    });

    it("re-tags cells and state when the plugin re-renders the popup", async () => {
      bindDatepickerTestIds(element, () => "birthDate");
      element.trigger('show');

      popup.find(".datepicker-days tbody").html("<tr><td class='day active'>9</td><td class='day'>10</td></tr>");
      await flushObservers();

      const days = popup.find(".datepicker-days [data-testid='datepicker-day']");
      expect(days.length).toBe(2);
      expect(days.eq(0).attr("data-selected")).toBe("true");

      popup.find(".datepicker-months .month").eq(0).addClass("active");
      await flushObservers();

      expect(popup.find(".datepicker-months .month").eq(0).attr("data-selected")).toBe("true");
    });

    it("does not tag a popup of another criterion when the instance has no picker", () => {
      const foreign = buildPopup().appendTo(document.body);
      element.data("datepicker", {});
      bindDatepickerTestIds(element, () => "birthDate");

      element.trigger("show");

      expect(foreign.attr("data-testid")).toBeUndefined();
      expect(foreign.attr("data-testid-owner")).toBeUndefined();
    });

    it("ignores namespaced show events bubbling from other widgets", () => {
      bindDatepickerTestIds(element, () => "birthDate");

      element.trigger("show.bs.tooltip");

      expect(popup.attr("data-testid")).toBeUndefined();
    });

    it("stops observing the popup when the plugin hides it", async () => {
      bindDatepickerTestIds(element, () => "birthDate");
      element.trigger('show');
      element.trigger('hide');

      popup.find(".datepicker-days tbody").html("<tr><td class='day'>11</td></tr>");
      await flushObservers();

      expect(popup.find(".datepicker-days td.day").attr("data-testid")).toBeUndefined();
    });
  });
});
