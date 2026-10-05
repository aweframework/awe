import React from "react";
import { act, cleanup, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../test-utils";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import AweInputText from "../../../src/criteria/AweInputText";
import AweInputPassword from "../../../src/criteria/AweInputPassword";
import AweInputTextarea from "../../../src/criteria/AweInputTextarea";
import AweInputNumeric from "../../../src/criteria/AweInputNumeric";
import AweInputCheckbox from "../../../src/criteria/AweInputCheckbox";
import AweInputRadio from "../../../src/criteria/AweInputRadio";
import AweInputDate from "../../../src/criteria/AweInputDate";
import AweInputFilteredDate from "../../../src/criteria/AweInputFilteredDate";
import AweInputTime from "../../../src/criteria/AweInputTime";
import AweInputUploader from "../../../src/criteria/AweInputUploader";
import AweInputTextView from "../../../src/criteria/AweInputTextView";
import AweInputHidden from "../../../src/criteria/AweInputHidden";
import AweSelect from "../../../src/criteria/AweSelect";
import AweSelectMultiple from "../../../src/criteria/AweSelectMultiple";
import AweSuggest from "../../../src/criteria/AweSuggest";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";
import AweButtonCheckbox from "../../../src/criteria/AweButtonCheckbox";
import AweButtonRadio from "../../../src/criteria/AweButtonRadio";

const hook = (testId) => `[data-testid='${testId}']`;

function criterion(id, values, attributes = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    components: {
      [id]: {
        address: { component: id, view: "report" },
        model: { values },
        attributes: { readonly: false, ...attributes },
        validationRules: { required: false },
        specificAttributes: { sort: [] }
      }
    }
  };
}

describe("awe-react-client/test/js/criteria/TestIdsCriteriaTest.jsx", () => {
  afterEach(cleanup);

  describe("text criteria", () => {
    it.each([
      ["AweInputText", AweInputText, "INPUT"],
      ["AweInputPassword", AweInputPassword, "INPUT"],
      ["AweInputTextarea", AweInputTextarea, "TEXTAREA"],
      ["AweInputNumeric", AweInputNumeric, "INPUT"]
    ])("%s exposes criterion-input on the real control", (_name, Component, tag) => {
      const { container } = renderWithProviders(<Component id="Txt" />,
        { preloadedState: criterion("Txt", [{ value: "5", label: "5", selected: true }]) });

      const input = container.querySelector(`[criterion-id='Txt'] ${hook("criterion-input")}`);
      expect(input).not.toBeNull();
      expect(input.tagName).toBe(tag);
    });

    it("exposes the unit of the criterion", () => {
      const { container } = renderWithProviders(<AweInputText id="Unt" />,
        { preloadedState: criterion("Unt", [{ value: "a", label: "a", selected: true }], { unit: "EUR" }) });

      const unit = container.querySelector(`[criterion-id='Unt'] ${hook("criterion-unit")}`);
      expect(unit).not.toBeNull();
      expect(unit.textContent).toBe("EUR");
    });

    it("does not render a unit hook when there is no unit", () => {
      const { container } = renderWithProviders(<AweInputText id="Unt" />,
        { preloadedState: criterion("Unt", [{ value: "a", label: "a", selected: true }]) });

      expect(container.querySelector(hook("criterion-unit"))).toBeNull();
    });

    it("exposes the validation error of the criterion", () => {
      const { container } = renderWithProviders(<AweInputText id="Err" />,
        { preloadedState: criterion("Err", [{ value: "", label: "", selected: true }], { error: { message: "Required field" } }) });

      const error = container.querySelector(`[criterion-id='Err'] ${hook("criterion-error")}`);
      expect(error).not.toBeNull();
      expect(error.textContent).toBe("Required field");
    });

    it("does not render an error hook when the criterion is valid", () => {
      const { container } = renderWithProviders(<AweInputText id="Err" />,
        { preloadedState: criterion("Err", [{ value: "a", label: "a", selected: true }]) });

      expect(container.querySelector(hook("criterion-error"))).toBeNull();
    });

    it("exposes the value of a hidden criterion without showing anything", () => {
      const { container } = renderWithProviders(<AweInputHidden id="Hid" />,
        { preloadedState: criterion("Hid", [{ value: "RstTst", label: "RstTst", selected: true }]) });

      const input = container.querySelector(`[criterion-id='Hid'] ${hook("criterion-input")}`);
      expect(input).not.toBeNull();
      expect(input.getAttribute("type")).toBe("hidden");
      expect(input.value).toBe("RstTst");
      expect(container.querySelector("[criterion-id='Hid']").classList.contains("hidden")).toBe(true);
    });

    it("exposes the shown text of a text view", () => {
      const { container } = renderWithProviders(<AweInputTextView id="Txv" />,
        { preloadedState: criterion("Txv", [{ value: "shown", label: "shown", selected: true }]) });

      const input = container.querySelector(`[criterion-id='Txv'] ${hook("criterion-input")}`);
      expect(input).not.toBeNull();
      expect(input.textContent).toBe("shown");
    });
  });

  describe("checkbox and radio", () => {
    it("exposes the checkbox with its state", () => {
      const { container } = renderWithProviders(<AweInputCheckbox id="Chk" />,
        { preloadedState: criterion("Chk", [{ value: "1", label: "Yes", selected: true }], { label: "Check" }) });

      const input = container.querySelector(`[criterion-id='Chk'] ${hook("criterion-input")}`);
      expect(input.classList.contains("p-checkbox")).toBe(true);
      expect(input.getAttribute("data-selected")).toBe("true");
    });

    it("reports an unchecked checkbox", () => {
      const { container } = renderWithProviders(<AweInputCheckbox id="Chk" />,
        { preloadedState: criterion("Chk", [{ value: "1", label: "Yes", selected: false }], { label: "Check" }) });

      expect(container.querySelector(hook("criterion-input")).getAttribute("data-selected")).toBe("false");
    });

    it("exposes the switch with its state", () => {
      const { container } = renderWithProviders(<AweInputCheckbox id="Chk" />,
        { preloadedState: criterion("Chk", [{ value: "1", label: "Yes", selected: true }], { label: "Check", style: "switch" }) });

      const input = container.querySelector(`[criterion-id='Chk'] ${hook("criterion-input")}`);
      expect(input.classList.contains("p-inputswitch")).toBe(true);
      expect(input.getAttribute("data-selected")).toBe("true");
    });

    it("exposes the radio with its state", () => {
      const { container } = renderWithProviders(<AweInputRadio id="Rad" />,
        { preloadedState: criterion("Rad", [{ value: "1", label: "One", selected: true }], { label: "Radio", group: "g" }) });

      const input = container.querySelector(`[criterion-id='Rad'] ${hook("criterion-input")}`);
      expect(input.classList.contains("p-radiobutton")).toBe(true);
      expect(input.getAttribute("data-selected")).toBe("true");
    });

    it.each([
      ["AweButtonCheckbox", AweButtonCheckbox],
      ["AweButtonRadio", AweButtonRadio]
    ])("%s exposes every option of the button group with its value and state", (_name, Component) => {
      const { container } = renderWithProviders(<Component id="Grp" />,
        {
          preloadedState: criterion("Grp", [
            { value: "Opt1", label: "One", selected: false },
            { value: "Opt2", label: "Two", selected: true }
          ])
        });

      const options = container.querySelectorAll(`[criterion-id='Grp'] ${hook("criterion-input")}`);
      expect(options).toHaveLength(2);
      expect(options[0].getAttribute("option-id")).toBe("Opt1");
      expect(options[0].getAttribute("data-selected")).toBe("false");
      expect(options[1].getAttribute("option-id")).toBe("Opt2");
      expect(options[1].getAttribute("data-selected")).toBe("true");
    });
  });

  describe("date criteria", () => {
    it.each([
      ["AweInputDate", AweInputDate],
      ["AweInputFilteredDate", AweInputFilteredDate],
      ["AweInputTime", AweInputTime]
    ])("%s exposes criterion-input on the input", (_name, Component) => {
      const { container } = renderWithProviders(<Component id="Dat" />,
        { preloadedState: criterion("Dat", [{ value: "01/02/2024", label: "01/02/2024", selected: true }]) });

      const input = container.querySelector(`[criterion-id='Dat'] ${hook("criterion-input")}`);
      expect(input.tagName).toBe("INPUT");
    });

    it("exposes the calendar overlay with its owner and its cells", () => {
      const { container } = renderWithProviders(<AweInputDate id="Dat" />,
        { preloadedState: criterion("Dat", [{ value: "15/01/2024", label: "15/01/2024", selected: true }]) });

      fireEvent.focus(container.querySelector(hook("criterion-input")));
      fireEvent.click(container.querySelector(hook("criterion-input")));

      const picker = document.querySelector(hook("datepicker"));
      expect(picker).not.toBeNull();
      expect(picker.getAttribute("data-testid-owner")).toBe("Dat");

      const days = Array.from(picker.querySelectorAll(hook("datepicker-day")));
      expect(days.length).toBeGreaterThanOrEqual(28);
      days.forEach(day => expect(day.getAttribute("data-testid-owner")).toBe("Dat"));
      const selected = days.filter(day => day.getAttribute("data-selected") === "true");
      expect(selected.map(day => day.textContent)).toEqual(["15"]);
      const inside = days.filter(day => day.getAttribute("data-outside-month") === "false");
      expect(inside.length).toBe(31);
      expect(inside.every(day => day.getAttribute("data-disabled") === "false")).toBe(true);
      // The hook is on the cell, which is also the element the user clicks
      expect(days[0].tagName).toBe("TD");
      expect(days[0].querySelector("span")).not.toBeNull();
    });

    it("exposes the month and year cells of the overlay", () => {
      const { container } = renderWithProviders(<AweInputDate id="Dat" />,
        { preloadedState: criterion("Dat", [{ value: "15/01/2024", label: "15/01/2024", selected: true }]) });
      fireEvent.focus(container.querySelector(hook("criterion-input")));
      fireEvent.click(container.querySelector(hook("criterion-input")));

      // Go to the month view and then to the year view
      fireEvent.click(document.querySelector(".p-datepicker-month"));
      const months = Array.from(document.querySelectorAll(hook("datepicker-month")));
      expect(months.length).toBe(12);
      expect(months[0].getAttribute("data-testid-owner")).toBe("Dat");
      expect(months.filter(month => month.getAttribute("data-selected") === "true").length).toBe(1);

      fireEvent.click(document.querySelector(".p-datepicker-year"));
      const years = Array.from(document.querySelectorAll(hook("datepicker-year")));
      expect(years.length).toBe(10);
      expect(years[0].getAttribute("data-testid-owner")).toBe("Dat");
    });
  });

  describe("selects", () => {
    const options = [{ label: "One", value: "1", selected: true }, { label: "Two", value: "2" }];

    it("exposes the container, the chosen value and the dropdown", () => {
      const { container } = renderWithProviders(<AweSelect id="Sta" />,
        { preloadedState: criterion("Sta", options, { placeholder: "pick" }) });

      const select = container.querySelector(`[criterion-id='Sta'] ${hook("select")}`);
      expect(select).not.toBeNull();
      expect(select.classList.contains("p-dropdown")).toBe(true);
      expect(select.querySelector(hook("select-value")).textContent).toBe("One");
      // Without dropdown nothing is rendered outside the component
      expect(document.querySelector(hook("select-dropdown"))).toBeNull();

      fireEvent.click(select);
      const dropdown = document.querySelector(hook("select-dropdown"));
      expect(dropdown.getAttribute("data-testid-owner")).toBe("Sta");
      const items = Array.from(dropdown.querySelectorAll(hook("select-option")));
      expect(items.map(item => item.textContent)).toEqual(["One", "Two"]);
      expect(items.map(item => item.getAttribute("data-selected"))).toEqual(["true", "false"]);
      items.forEach(item => expect(item.getAttribute("data-testid-owner")).toBe("Sta"));
    });

    it("exposes the trigger of a select and opens its panel from it", () => {
      const { container } = renderWithProviders(<AweSelect id="Sta" />,
        { preloadedState: criterion("Sta", options, { placeholder: "pick" }) });

      const trigger = container.querySelector(`[criterion-id='Sta'] ${hook("select-trigger")}`);
      expect(trigger).not.toBeNull();
      fireEvent.click(trigger);
      expect(document.querySelector(hook("select-dropdown"))).not.toBeNull();
    });

    it("shows the placeholder as value when nothing is selected", () => {
      const { container } = renderWithProviders(<AweSelect id="Sta" />,
        { preloadedState: criterion("Sta", [{ label: "One", value: "1" }], { placeholder: "pick", optional: true }) });

      expect(container.querySelector(hook("select-value")).textContent).toBe("pick");
    });

    it("exposes the search input when the select is filterable", () => {
      const many = Array.from({ length: 8 }, (_, i) => ({ label: `L${i}`, value: `${i}`, selected: i === 0 }));
      const { container } = renderWithProviders(<AweSelect id="Sta" />, { preloadedState: criterion("Sta", many) });

      fireEvent.click(container.querySelector(hook("select")));
      const search = document.querySelector(`${hook("select-dropdown")} ${hook("select-search")}`);
      expect(search.tagName).toBe("INPUT");
    });

    it("exposes the chosen items of a multiple select", () => {
      const multiple = [
        { label: "One", value: "1", selected: true },
        { label: "Two", value: "2", selected: true },
        { label: "Three", value: "3" }
      ];
      const { container } = renderWithProviders(<AweSelectMultiple id="Mul" />,
        { preloadedState: criterion("Mul", multiple) });

      expect(container.querySelector(`[criterion-id='Mul'] ${hook("select")}`)).not.toBeNull();
      const choices = Array.from(container.querySelectorAll(hook("select-choice")));
      expect(choices.map(choice => choice.textContent)).toEqual(["One", "Two"]);
      expect(container.querySelectorAll(`${hook("select-choice")} ${hook("select-choice-close")}`).length).toBe(2);

      fireEvent.click(container.querySelector(hook("select")));
      const dropdown = document.querySelector(hook("select-dropdown"));
      expect(dropdown.getAttribute("data-testid-owner")).toBe("Mul");
      const items = Array.from(dropdown.querySelectorAll(hook("select-option")));
      expect(items.map(item => item.getAttribute("data-selected"))).toEqual(["true", "true", "false"]);
      expect(dropdown.querySelector(hook("select-search"))).not.toBeNull();
    });

    it("exposes the trigger of a multiple select and opens its panel from it", () => {
      const multiple = [{ label: "One", value: "1", selected: true }, { label: "Two", value: "2" }];
      const { container } = renderWithProviders(<AweSelectMultiple id="Mul" />,
        { preloadedState: criterion("Mul", multiple, { optional: true }) });

      // A short multiple select shows its clear icon in the middle: clicking the trigger never clears its values
      const trigger = container.querySelector(`[criterion-id='Mul'] ${hook("select-trigger")}`);
      expect(trigger).not.toBeNull();
      expect(document.querySelector(hook("select-dropdown"))).toBeNull();
      fireEvent.click(trigger);
      expect(document.querySelector(hook("select-dropdown"))).not.toBeNull();
    });
  });

  describe("suggests", () => {
    it("exposes the container and the typing input of a suggest", async () => {
      let container;
      await act(async () => {
        ({ container } = renderWithProviders(<AweSuggest id="Sug" />,
          { preloadedState: criterion("Sug", [{ label: "test", value: "test", selected: true }]) }));
      });

      expect(container.querySelector(`[criterion-id='Sug'] ${hook("select")}`)).not.toBeNull();
      const search = container.querySelector(hook("select-search"));
      expect(search.tagName).toBe("INPUT");
      expect(search.value).toBe("test");
    });

    it("exposes the panel and the options of a suggest", async () => {
      await act(async () => {
        renderWithProviders(<AweSuggest id="Sug" />, {
          preloadedState: criterion("Sug", [{ label: "test", value: "test", selected: true }, { label: "other", value: "other" }],
            { openPanelOnMount: true })
        });
      });

      const panel = document.querySelector(hook("select-dropdown"));
      expect(panel.getAttribute("data-testid-owner")).toBe("Sug");
      const items = Array.from(panel.querySelectorAll(hook("select-option")));
      expect(items.map(item => item.textContent)).toEqual(["test", "other"]);
      items.forEach(item => expect(item.getAttribute("data-testid-owner")).toBe("Sug"));
    });

    it("exposes the chosen items of a multiple suggest", () => {
      const { container } = renderWithProviders(<AweSuggestMultiple id="Sum" />, {
        preloadedState: criterion("Sum", [{ label: "test", value: "test", selected: true }, { label: "tutu", value: "tutu", selected: true }])
      });

      expect(container.querySelector(`[criterion-id='Sum'] ${hook("select")}`)).not.toBeNull();
      const choices = Array.from(container.querySelectorAll(hook("select-choice")));
      expect(choices.map(choice => choice.textContent)).toEqual(["test", "tutu"]);
      expect(container.querySelectorAll(hook("select-choice-close")).length).toBe(2);
      expect(container.querySelector(hook("select-search")).tagName).toBe("INPUT");
    });
  });

  describe("uploader", () => {
    it("exposes the file name and the clear button", () => {
      const { container } = renderWithProviders(<AweInputUploader id="Upl" />, {
        preloadedState: criterion("Upl", [{ value: "file.txt", label: "file.txt", size: 10, selected: true }])
      });

      const name = container.querySelector(`[criterion-id='Upl'] ${hook("upload-filename")}`);
      expect(name.tagName).toBe("INPUT");
      expect(name.value).toContain("file.txt");
      expect(container.querySelector(hook("upload-clear")).tagName).toBe("BUTTON");
      expect(container.querySelector(`[criterion-id='Upl'] ${hook("criterion-input")}`)).not.toBeNull();
    });
  });
});
