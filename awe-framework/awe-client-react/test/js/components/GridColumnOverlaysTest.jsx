import React from "react";
import {fireEvent, render, screen} from "@testing-library/react";
import GridColumnOverlays from "../../../src/components/GridColumnOverlays";

const rect = (left, width, top = 0, height = 100) => ({left, width, top, height, right: left + width, bottom: top + height, x: left, y: top, toJSON: () => ({})});

describe("awe-react-client/test/js/components/GridColumnOverlaysTest.jsx", () => {
  const setup = (props = {}) => {
    const grid = document.createElement("div");
    grid.innerHTML = '<table><thead><tr><th column-id="a"> Name </th><th column-id="b">Profile</th></tr></thead></table>';
    document.body.appendChild(grid);
    grid.getBoundingClientRect = () => rect(10, 400);
    const [a, b] = grid.querySelectorAll("th");
    a.getBoundingClientRect = () => rect(10, 100);
    b.getBoundingClientRect = () => rect(110, 80);
    const sort = jest.fn();
    a.addEventListener("click", sort); // the header cell is what sorts the grid
    const onSelect = jest.fn();
    render(<GridColumnOverlays grid={grid} columns={new Set(["a", "b"])} hidden={new Set(["b"])} selected={null} onSelect={onSelect} {...props}/>);
    return {grid, sort, onSelect};
  };
  afterEach(() => document.body.replaceChildren());

  it("covers each column, from its header cell, with an overlay", () => {
    setup();
    const overlays = screen.getAllByRole("button");
    expect(overlays).toHaveLength(2);
    expect(overlays[0]).toHaveAccessibleName("Name"); // the header text, not the internal id
    expect(overlays[1]).toHaveStyle({left: "100px", width: "80px"});
  });

  it("selects the column on click and does not let the grid sort", () => {
    const {sort, onSelect} = setup();
    fireEvent.click(screen.getAllByRole("button")[0]);
    expect(onSelect).toHaveBeenCalledWith("a");
    expect(sort).not.toHaveBeenCalled();
  });

  it("dims a really hidden column and tags it hidden, and outlines the selected one", () => {
    setup({selected: "a"});
    const [visible, hidden] = screen.getAllByRole("button");
    expect(hidden).toHaveClass("awe-column-overlay-hidden");
    expect(hidden).toHaveTextContent("SCR_PER_HIDDEN");
    expect(visible).not.toHaveClass("awe-column-overlay-hidden");
    expect(visible).toHaveClass("awe-column-overlay-selected");
  });
});
