import { cleanup, fireEvent } from "@testing-library/react";
import { Columns } from "../../../src/utilities/structure";
import { renderWithProviders } from "../test-utils";

const hook = (testId) => `[data-testid='${testId}']`;

function editor(component, data, extra = {}) {
  return renderWithProviders(Columns({
    component,
    model: { values: [{ label: "One", value: "1" }, { label: "Two", value: "2" }] },
    numberFormat: {},
    address: { component: "Grd", view: "report", column: "col", row: "row" },
    settings: { language: "en" },
    placeholder: "placeholder",
    ...extra
  }, data, {}, true));
}

describe("awe-react-client/test/js/columns/TestIdsColumnsTest.jsx", () => {
  afterEach(cleanup);

  it.each([
    ["text", "INPUT"],
    ["password", "INPUT"],
    ["textarea", "INPUT"],
    ["numeric", "INPUT"]
  ])("exposes criterion-input on the %s editor of a cell", (component, tag) => {
    const { container } = editor(component, { value: "5" });

    const input = container.querySelector(hook("criterion-input"));
    expect(input.tagName).toBe(tag);
  });

  it("exposes criterion-input and the state of the checkbox editor", () => {
    const { container } = editor("checkbox", { value: 1 });

    const input = container.querySelector(hook("criterion-input"));
    expect(input.classList.contains("p-inputswitch")).toBe(true);
    expect(input.getAttribute("data-selected")).toBe("true");
  });

  it.each([
    [1, "true"], [true, "true"], ["1", "true"], ["0", "true"],
    [0, "false"], [false, "false"], [null, "false"], ["", "false"]
  ])("reports the checkbox editor with value %p as selected=%s, as PrimeReact renders it", (value, selected) => {
    const { container } = editor("checkbox", { value });

    const input = container.querySelector(hook("criterion-input"));
    expect(input.getAttribute("data-selected")).toBe(selected);
    expect(input.classList.contains("p-highlight")).toBe(selected === "true");
  });

  it.each(["date", "time", "filtered-calendar"])("exposes criterion-input on the %s editor", (component) => {
    const { container } = editor(component, { value: "15/01/2024" });

    expect(container.querySelector(hook("criterion-input")).tagName).toBe("INPUT");
  });

  it("exposes the calendar of a date editor with its owner", () => {
    const { container } = editor("date", { value: "15/01/2024" });
    fireEvent.focus(container.querySelector(hook("criterion-input")));
    fireEvent.click(container.querySelector(hook("criterion-input")));

    expect(document.querySelector(hook("datepicker")).getAttribute("data-testid-owner")).toBe("Grd");
    expect(document.querySelectorAll(hook("datepicker-day")).length).toBeGreaterThanOrEqual(28);
  });

  it("exposes the select editor, its value and its dropdown", () => {
    const { container } = editor("select", { value: "1" });

    const select = container.querySelector(hook("select"));
    expect(select.querySelector(hook("select-value")).textContent).toBe("One");
    fireEvent.click(select);
    const dropdown = document.querySelector(hook("select-dropdown"));
    expect(dropdown.getAttribute("data-testid-owner")).toBe("Grd");
    expect(Array.from(dropdown.querySelectorAll(hook("select-option"))).map(o => o.textContent)).toEqual(["One", "Two"]);
  });

  it("exposes the chosen items of the select multiple editor", () => {
    const { container } = editor("select-multiple", [
      { label: "One", value: "1", selected: true },
      { label: "Two", value: "2" }
    ]);

    expect(container.querySelector(hook("select"))).not.toBeNull();
    expect(Array.from(container.querySelectorAll(hook("select-choice"))).map(c => c.textContent)).toEqual(["One"]);
    expect(container.querySelector(hook("select-choice-close"))).not.toBeNull();
  });

  it("exposes the suggest editor", () => {
    const { container } = editor("suggest", { value: "test" }, { t: jest.fn() });

    expect(container.querySelector(hook("select"))).not.toBeNull();
    expect(container.querySelector(hook("select-search")).tagName).toBe("INPUT");
  });

  it("exposes the suggest multiple editor", () => {
    const { container } = editor("suggest-multiple", [{ label: "One", value: "1", selected: true }], { t: jest.fn() });

    expect(container.querySelector(hook("select"))).not.toBeNull();
    expect(container.querySelector(hook("select-choice")).textContent).toBe("One");
    expect(container.querySelector(hook("select-search")).tagName).toBe("INPUT");
  });
});
