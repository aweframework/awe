import React from "react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {buildSparkline} from "../../../src/utilities/sparkline";

const column = (extra = {}) => ({
  component: "sparkline",
  address: {component: "grid", view: "report", column: "trend", row: "1"},
  ...extra
});

const renderCell = (value, extra = {}, editing = false) => renderWithProviders(Columns(column(extra), value, {}, editing));

describe("awe-react-client/test/js/columns/ColumnSparklineTest.jsx", () => {

  it("draws the series of a JSON string as an SVG with a point per value", () => {
    renderCell({value: "[3, 5, -1, 8]"});

    const svg = document.querySelector("svg.awe-sparkline");
    expect(svg).not.toBeNull();
    expect(svg.getAttribute("viewBox")).toBe("0 0 100 24");
    expect(svg.getAttribute("role")).toBe("img");
    expect(svg.getAttribute("data-type")).toBe("line");
    expect(svg.getAttribute("aria-label")).toBe("3, 5, -1, 8");
    expect(Array.from(svg.querySelectorAll("title")).map(title => title.textContent)).toEqual(["3", "5", "-1", "8"]);
    expect(svg.querySelector("path")).not.toBeNull();
  });

  it("draws an already parsed list sent as the cell value", () => {
    renderCell([3, 5, -1, 8]);

    expect(document.querySelector("svg.awe-sparkline")).not.toBeNull();
    expect(document.querySelectorAll("svg.awe-sparkline title")).toHaveLength(4);
  });

  it("draws exactly what the shared renderer builds for the column options", () => {
    const options = {type: "bar", color: "#112233", negativeColor: "#aa0000", lastPoint: true, minMax: true};
    renderCell({value: "[3, 5, -1, 8]"}, {
      sparklineType: "bar", sparklineColor: "#112233", sparklineNegativeColor: "#aa0000",
      sparklineLastPoint: true, sparklineMinMax: true
    });

    const model = buildSparkline("[3, 5, -1, 8]", options);
    const svg = document.querySelector("svg.awe-sparkline");
    expect(svg.children).toHaveLength(model.shapes.length);
    expect(svg.querySelectorAll("rect")).toHaveLength(model.shapes.filter(shape => shape.tag === "rect").length);
    expect(svg.querySelector("rect[fill='#aa0000']")).not.toBeNull();
    expect(svg.querySelectorAll("circle")).toHaveLength(2);
  });

  it.each([[null], [""], ["not json"], ["[]"], ["[1, \"a\"]"], [{}], [[]]])("leaves the cell empty for %p", value => {
    renderCell({value});
    expect(document.querySelector("svg")).toBeNull();
    expect(document.querySelector(".column-sparkline")).not.toBeNull();
  });

  it("draws a single value as one dot", () => {
    renderCell({value: "[7]"});
    expect(document.querySelectorAll("svg.awe-sparkline circle")).toHaveLength(1);
  });

  it("is not a tab stop and does not take the focus, so the grid keyboard navigation is untouched", () => {
    renderCell({value: "[1, 2, 3]"});
    const svg = document.querySelector("svg.awe-sparkline");
    expect(svg.getAttribute("focusable")).toBe("false");
    expect(svg.hasAttribute("tabindex")).toBe(false);
    expect(svg.querySelectorAll("[tabindex]")).toHaveLength(0);
  });

  it("is a display component: it draws the same while the row is being edited", () => {
    renderCell({value: "[1, 2, 3]"}, {}, true);
    expect(document.querySelector("svg.awe-sparkline")).not.toBeNull();
  });

  it("applies the style of the column and aligns the drawing like the other cells", () => {
    renderCell({value: "[1, 2, 3]"}, {style: "my-style", align: "right"});
    const cell = document.querySelector(".column-sparkline");
    expect(cell.classList.contains("my-style")).toBe(true);
    expect(cell.style.textAlign).toBe("right");
  });

  it("draws a page of 100 rows with 12 points each", () => {
    const series = Array.from({length: 12}, (_, index) => Math.round(Math.sin(index) * 50));
    renderWithProviders(<div>{Array.from({length: 100}, (_, row) => Columns(
      column({address: {component: "grid", view: "report", column: "trend", row: String(row)}}),
      {value: JSON.stringify(series)}))}</div>);

    expect(document.querySelectorAll("svg.awe-sparkline")).toHaveLength(100);
  });
});
