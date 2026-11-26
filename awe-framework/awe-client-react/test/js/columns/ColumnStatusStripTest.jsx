import {act, fireEvent} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

const column = (extra = {}) => ({
  component: "status-strip",
  address: {component: "grid", view: "report", column: "history", row: "1"},
  ...extra
});

const ITEMS = [
  {tone: "success", label: "OK"},
  {tone: "warning", label: "Aviso"},
  {tone: "success", label: "OK"},
  {tone: "danger", label: "Error"}
];

const renderCell = value => renderWithProviders(Columns(column(), value, {}, false));

const drawnItems = () => Array.from(document.querySelectorAll(".awe-status-strip-item"))
  .map(item => ({tone: item.getAttribute("data-tone"), tooltip: item.getAttribute("data-pr-tooltip")}));

/**
 * Hover a bar and let its tooltip show
 * @param {number} index Bar position
 * @return {string|undefined} Text of the visible tooltip
 */
const hoverBar = index => {
  fireEvent.mouseOver(document.querySelectorAll(".awe-status-strip-item")[index]);
  act(() => jest.runOnlyPendingTimers());
  return document.querySelector(".p-tooltip .p-tooltip-text")?.textContent;
};

describe("awe-react-client/test/js/columns/ColumnStatusStripTest.jsx", () => {
  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  it("draws one item per status with its tone and its label as the tooltip, and names the strip with the summary", () => {
    renderCell({value: JSON.stringify(ITEMS)});

    const strip = document.querySelector(".awe-status-strip");
    expect(strip.getAttribute("role")).toBe("img");
    expect(strip.getAttribute("aria-label")).toBe("2 OK, 1 Aviso, 1 Error");
    expect(drawnItems()).toEqual(ITEMS.map(item => ({tone: item.tone, tooltip: item.label})));
    expect(strip.querySelector("[title]")).toBeNull();
  });

  it("shows the label and the title of the hovered bar in the tooltip of the engine, and the summary keeps the labels", () => {
    renderCell({value: JSON.stringify([
      {tone: "success", label: "OK", title: "#4807 · 17/11/2026 08:05 · 1 min 12 s"},
      {tone: "danger", label: "Error"}
    ])});

    expect(hoverBar(0)).toBe("OK\n#4807 · 17/11/2026 08:05 · 1 min 12 s");
    expect(hoverBar(1)).toBe("Error");
    expect(document.querySelector(".awe-status-strip").getAttribute("aria-label")).toBe("1 OK, 1 Error");
  });

  it("draws an already parsed list sent as the cell value", () => {
    renderCell(ITEMS);

    expect(drawnItems()).toHaveLength(4);
    expect(document.querySelector(".awe-status-strip").getAttribute("aria-label")).toBe("2 OK, 1 Aviso, 1 Error");
  });

  it.each([[null], [""], ["not json"], ["[]"], ["[{\"tone\":\"success\"}]"], [{}]])("leaves the cell empty for %p", value => {
    renderCell({value});

    expect(document.querySelector(".column-status-strip")).not.toBeNull();
    expect(document.querySelector(".awe-status-strip")).toBeNull();
  });
});
