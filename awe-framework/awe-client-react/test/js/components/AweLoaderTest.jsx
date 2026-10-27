import React, {Suspense} from "react";
import fs from "fs";
import path from "path";
import {render, screen} from "@testing-library/react";
import AweLoader, {LOADER_ICONS} from "../../../src/components/AweLoader";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

describe("awe-react-client/test/js/components/AweLoaderTest.jsx", () => {
  const cssClassOf = {
    carpet: "carpet-loader",
    circlebar: "circlebar-loader",
    circles: "circles-loader",
    cubes: "cubes-loader",
    dots: "dots-loader",
    folding: "folding-loader",
    square: "square-loader",
    squarebar: "squarebar-loader",
    custom: "custom-loader"
  };

  it("offers the same 14 loading icons as the AngularJS engine", () => {
    expect([...LOADER_ICONS].sort()).toEqual([
      "carpet", "circlebar", "circles", "cubes", "custom", "dots", "folding", "icon", "none", "skeleton",
      "skeleton-dim", "spinner", "square", "squarebar"
    ]);
  });

  it.each(Object.entries(cssClassOf))("renders the %s icon", (icon, cssClass) => {
    const {container} = render(<AweLoader icon={icon}/>);

    expect(container.querySelector(`.${cssClass}`)).not.toBeNull();
  });

  it("renders the folding cubes as direct children of the folding loader, as the AngularJS markup does", () => {
    const {container} = render(<AweLoader icon="folding"/>);

    const cubes = container.querySelectorAll(".folding-loader > .sk-cube");
    expect([...cubes].map(cube => cube.className)).toEqual(
      ["sk-cube sk-cube1", "sk-cube sk-cube2", "sk-cube sk-cube4", "sk-cube sk-cube3"]);
  });

  it("sizes the folding cubes with a rule that matches that markup", () => {
    const less = fs.readFileSync(path.join(__dirname, "..", "..", "..", "src", "components", "AweLoader.less"), "utf8");

    expect(less).not.toContain("sk-folding-cube");
    expect(less).toMatch(/\.folding-loader \{[\s\S]*?> \.sk-cube \{\s*float: left;\s*width: 50%;/);
  });

  it.each([undefined, "spinner", "unknown-icon"])("renders the spinner for %s", (icon) => {
    const {container} = render(<AweLoader icon={icon}/>);

    expect(container.querySelector(".p-progress-spinner")).not.toBeNull();
  });

  it("renders nothing visible for none", () => {
    const {container} = render(<AweLoader icon="none"/>);

    expect(container.querySelector(".p-progress-spinner")).toBeNull();
    expect(container.querySelector(".awe-loader")?.children).toHaveLength(0);
  });

  it("renders a spinning icon for icon", () => {
    const {container} = render(<AweLoader icon="icon"/>);

    const svg = container.querySelector("i > svg.awe-icon.awe-icon-spin");
    expect(svg.querySelector("use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#loader-circle");
  });

  it("applies the pass through attributes to the root", () => {
    const {container} = render(<AweLoader icon="dots" rootProps={{"data-testid": "loader"}}/>);

    expect(container.querySelector("[data-testid='loader']")).not.toBeNull();
  });

  it("shows the grid loading icon selected by icon-loading", () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: "grid", view: "report"},
          model: {values: []},
          attributes: {columnModel: [], headerModel: [], buttonModel: [], loading: true, iconLoading: "carpet"},
          specificAttributes: {sort: []}
        }
      }
    };

    renderWithProviders(<Suspense fallback={<div>Loading...</div>}><AweGrid id="grid"/></Suspense>, {preloadedState});

    expect(document.querySelector(".carpet-loader")).not.toBeNull();
    expect(screen.getByRole("table")).toBeDefined();
  });
});
