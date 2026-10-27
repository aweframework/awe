import React from "react";
import {act, cleanup, fireEvent, render} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import {Columns} from "../../../src/utilities/structure";
import ColumnRowEditor from "../../../src/columns/ColumnRowEditor";
import {AweGridRowDetailToggle} from "../../../src/components/AweGridRowDetail";
import AweInputPassword from "../../../src/criteria/AweInputPassword";
import AweInputDateRange from "../../../src/criteria/AweInputDateRange";
import AweInputUploader from "../../../src/criteria/AweInputUploader";
import AweInputCheckbox from "../../../src/criteria/AweInputCheckbox";
import AweInputText from "../../../src/criteria/AweInputText";
import MessageContainer from "../../../src/containers/MessageContainer";

const fs = require("fs");
const path = require("path");

const SOURCES = path.join(__dirname, "../../../src");

// The Lucide icon an element holds: the name of its sprite symbol
const lucideOf = element => element?.querySelector(":scope > svg.awe-icon > use")?.getAttribute("href")?.split("#")[1];
const isDecorative = element => {
  const svg = element.querySelector(":scope > svg.awe-icon");
  return svg.getAttribute("aria-hidden") === "true" && svg.getAttribute("focusable") === "false";
};

const criterionState = (id, component, attributes = {}, values = []) => ({
  settings: DEFAULT_SETTINGS,
  components: {
    [id]: {
      address: {component: id, view: "report"},
      model: {values},
      attributes: {id, component, label: "Label", readonly: false, visible: true, ...attributes},
      validationRules: {required: false},
      specificAttributes: {sort: []}
    }
  }
});

// The icons the AWE components draw (#885): the Lucide icon inside the element that held the glyph, which keeps its
// classes, attributes and test hooks
describe("awe-react-client/test/js/styles/aweIconComponentsTest.jsx", () => {
  afterEach(cleanup);

  it("draws the icon of an icon column from the data, and keeps the data value on data-icon", () => {
    const {container} = renderWithProviders(Columns({
      component: "icon",
      model: {values: []},
      numberFormat: {},
      address: {component: "Grd", view: "report", column: "col", row: "row"},
      settings: {language: "en"}
    }, {value: "fa-check", icon: "check", style: "text-success"}, {}, true));

    const hook = container.querySelector("[data-testid='column-icon']");
    expect(hook.getAttribute("data-icon")).toBe("check");
    const icon = hook.querySelector(":scope > i[role='icon']");
    expect(icon.classList).toContain("text-success");
    expect(lucideOf(icon)).toBe("check");
    expect(isDecorative(icon)).toBe(true);
    // The success locator of the browser tests: an element with the style of the data under the hook
    expect(hook.querySelector(".text-success")).not.toBeNull();
  });

  it("draws the row editor icons in the elements PrimeReact styles", () => {
    const actions = {editRow: jest.fn(), saveRow: jest.fn(), cancelRow: jest.fn()};
    const {container, rerender} = render(<ColumnRowEditor rowData={{id: "1", $row: {}}} {...actions}/>);
    const edit = container.querySelector(".p-row-editor-init-icon");
    expect(edit.className).toBe("p-row-editor-init-icon p-clickable");
    expect(lucideOf(edit)).toBe("pencil");

    rerender(<ColumnRowEditor rowData={{id: "1", $row: {editing: true}}} {...actions}/>);
    expect(lucideOf(container.querySelector(".p-row-editor-save-icon"))).toBe("check");
    expect(lucideOf(container.querySelector(".p-row-editor-cancel-icon"))).toBe("x");
  });

  it("draws the row detail toggle chevron", () => {
    const {container, rerender} = render(<AweGridRowDetailToggle gridId="Grd" rowId="1" onToggle={jest.fn()}/>);
    const toggle = container.querySelector("[data-testid='grid-row-detail-toggle']");
    expect(lucideOf(toggle.querySelector("i[aria-hidden='true']"))).toBe("chevron-right");
    rerender(<AweGridRowDetailToggle gridId="Grd" rowId="1" expanded onToggle={jest.fn()}/>);
    expect(lucideOf(toggle.querySelector("i[aria-hidden='true']"))).toBe("chevron-down");
  });

  it("draws the caps lock warning of the password criterion", () => {
    renderWithProviders(<AweInputPassword id="password"/>, {preloadedState: criterionState("password", "password")});
    fireEvent.keyDown(document.querySelector("input#password"), {key: "A", modifierCapsLock: true});

    const warning = document.querySelector(".caps-lock-icon");
    expect(warning.getAttribute("aria-hidden")).toBe("true");
    expect(lucideOf(warning)).toBe("circle-arrow-up");
    act(() => {
      fireEvent.keyUp(document, {key: "CapsLock", modifierCapsLock: false});
    });
  });

  it("draws the separator of the date range criterion", () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: criterionState("period", "date-range", {},
      [{value: "01/01/2026", label: "01/01/2026", selected: true}, {value: "31/01/2026", label: "31/01/2026", selected: true}])});

    expect(lucideOf(document.querySelector(".date-range-separator > i"))).toBe("arrow-right");
  });

  it("draws the help icon of the criteria", () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: criterionState("text", "text", {help: "HELP"})});
    const help = document.querySelector("i.help-icon[role='icon']");
    expect(lucideOf(help)).toBe("circle-question-mark");

    cleanup();
    renderWithProviders(<AweInputCheckbox id="check"/>, {preloadedState: criterionState("check", "checkbox", {help: "HELP"},
      [{label: "test", value: "1", selected: true}])});
    expect(lucideOf(document.querySelector("label i.help-icon[role='icon']"))).toBe("circle-question-mark");
  });

  it("draws the capture and clear buttons of the uploader", () => {
    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: criterionState("uploader", "uploader",
      {capture: "user"})});

    const capture = document.querySelector("[data-testid='upload-capture']");
    expect(lucideOf(capture.querySelector("i.p-button-icon"))).toBe("camera");
    const clear = document.querySelector("[data-testid='upload-clear']");
    expect(lucideOf(clear.querySelector("i.p-button-icon"))).toBe("x");
  });

  it("draws the warning and the buttons of the confirm dialog", () => {
    const action = {type: "confirm", parameters: {target: "Confirm"}};
    renderWithProviders(<MessageContainer/>, {preloadedState: {settings: DEFAULT_SETTINGS,
      messages: {showing: [], confirm: {title: "Confirm title", message: "Confirm message", action}}}});

    const warning = document.querySelector(".confirm-icon");
    // The badge of the warning takes its size and colour from the overlay styles (#885), not from utility classes
    expect(warning.className).toBe("confirm-icon");
    expect(warning.style.fontSize).toBe("");
    expect(warning.closest(".p-dialog").classList).toContain("awe-confirm-dialog");
    expect(lucideOf(warning)).toBe("triangle-alert");
    expect(lucideOf(document.querySelector("#confirm-accept i.p-button-icon"))).toBe("check");
    expect(lucideOf(document.querySelector("#confirm-cancel i.p-button-icon"))).toBe("x");
  });

  it("leaves no PrimeIcons class written in the AWE components", () => {
    const listFiles = folder => fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => entry.isDirectory()
      ? listFiles(path.join(folder, entry.name))
      : [path.join(folder, entry.name)]).filter(file => /\.jsx?$/.test(file));
    const primeClasses = listFiles(SOURCES).flatMap(file => [...fs.readFileSync(file, "utf8")
      .matchAll(/\bpi pi-[a-z-]+|`pi \$\{/g)]
      .map(match => `${path.relative(SOURCES, file)}: ${match[0]}`));
    expect(primeClasses).toEqual([]);
  });
});
