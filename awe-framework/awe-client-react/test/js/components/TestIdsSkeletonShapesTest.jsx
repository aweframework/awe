import React from "react";
import {act, cleanup} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";
import {updateAttributes} from "../../../src/redux/actions/components";

const hook = (testId) => `[data-testid='${testId}']`;

const rows = [
  {id: "r1", parent: null, name: "Ann", age: 30},
  {id: "r2", parent: null, name: "Bob", age: 41}
];

function gridState(attributes = {}, {values = [], tree = false, settings = {}} = {}) {
  return {
    settings: {...DEFAULT_SETTINGS, ...settings},
    screen: {size: {width: 100, height: 100}},
    components: {
      Grd: {
        address: {component: "Grd", view: "report"},
        model: {values},
        attributes: {
          columnModel: [{name: "name", label: "Name"}, {name: "age", label: "Age"}],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          loadAll: true,
          ...(tree ? {treegrid: true} : {}),
          ...attributes
        },
        specificAttributes: {sort: []}
      }
    }
  };
}

describe("awe-react-client/test/js/components/TestIdsSkeletonShapesTest.jsx", () => {
  const KNOWN_WARNINGS = [
    /does not recognize the[\s\S]*__TYPE/, /does not recognize the[\s\S]*ptOptions/, /non-boolean attribute[\s\S]*unstyled/
  ];
  beforeEach(() => {
    jest.spyOn(console, "error").mockImplementation((...args) => {
      if (!KNOWN_WARNINGS.some(known => known.test(String(args.join(" "))))) {
        process.stderr.write(`${args.join(" ")}\n`);
      }
    });
  });
  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  describe.each([
    ["grid", AweGrid, false],
    ["tree grid", AweTreeGrid, true]
  ])("%s", (_name, Component, tree) => {
    const renderGrid = (attributes, options = {}) =>
      renderWithProviders(<Component id="Grd"/>, {preloadedState: gridState(attributes, {tree, ...options})});

    describe("reload with rows and the default loader", () => {
      it.each([
        ["the application default", {}, {}],
        ["an explicit skeleton icon", {iconLoading: "skeleton"}, {settings: {gridIconLoading: "spinner"}}]
      ])("dims the rows instead of drawing the skeleton with %s", (_case, attributes, options) => {
        const {container} = renderGrid({loading: true, ...attributes}, {values: rows, ...options});

        const dimmed = container.querySelector(".grid-container");
        expect(dimmed.classList.contains("grid-reloading")).toBe(true);
        expect(dimmed.getAttribute("aria-busy")).toBe("true");
        expect(container.querySelector(".p-progress-spinner")).toBeNull();
        expect(container.querySelector(".grid-skeleton")).toBeNull();
        expect(container.querySelector(".p-skeleton")).toBeNull();
        // The rows stay where they are
        expect(container.querySelectorAll(hook("grid-cell")).length).toBeGreaterThan(0);
      });

      it("keeps the loading hook while the rows are dimmed", () => {
        const {container} = renderGrid({loading: true}, {values: rows});

        const loader = container.querySelector(hook("grid-loader"));
        expect(loader).not.toBeNull();
        expect(loader.getAttribute("aria-hidden")).toBe("true");
        expect(loader.classList.contains("grid-reload")).toBe(true);
      });

      it("stops dimming when the data arrives", () => {
        const {container, store} = renderGrid({loading: true}, {values: rows});
        expect(container.querySelector(".grid-reloading")).not.toBeNull();

        act(() => {
          store.dispatch(updateAttributes({component: "Grd", view: "report"}, {loading: false}));
        });

        expect(container.querySelector(".grid-reloading")).toBeNull();
        expect(container.querySelector(hook("grid-loader"))).toBeNull();
        expect(container.querySelector(".grid-container").getAttribute("aria-busy")).toBeNull();
      });

      it("does not dim a grid that is not loading", () => {
        const {container} = renderGrid({}, {values: rows});

        expect(container.querySelector(".grid-reloading")).toBeNull();
      });

      it("keeps the skeleton for the first load, when there are no rows to dim", () => {
        const {container} = renderGrid({loading: true});

        expect(container.querySelector(".grid-reloading")).toBeNull();
        expect(container.querySelector(`${hook("grid-loader")} .grid-skeleton-row`)).not.toBeNull();
      });
    });

    describe("custom icon-loading", () => {
      it.each([
        ["first load", []],
        ["reload with rows", rows]
      ])("keeps the icon of the grid on %s", (_case, values) => {
        const {container} = renderGrid({loading: true, iconLoading: "carpet"}, {values});

        expect(container.querySelector(`${hook("grid-loader")} .carpet-loader`)).not.toBeNull();
        expect(container.querySelector(".grid-reloading")).toBeNull();
        expect(container.querySelector(".grid-skeleton")).toBeNull();
        expect(container.querySelector(".grid-container").getAttribute("aria-busy")).toBe("true");
      });

      it("keeps a spinner chosen by the grid over the dimmed reload", () => {
        const {container} = renderGrid({loading: true, iconLoading: "spinner"}, {values: rows});

        expect(container.querySelector(`.p-progress-spinner${hook("grid-loader")}`)).not.toBeNull();
        expect(container.querySelector(".grid-reloading")).toBeNull();
      });

      it("keeps the spinner of a spinner application default on reload", () => {
        const {container} = renderGrid({loading: true}, {values: rows, settings: {gridIconLoading: "spinner"}});

        expect(container.querySelector(`.p-progress-spinner${hook("grid-loader")}`)).not.toBeNull();
        expect(container.querySelector(".grid-reloading")).toBeNull();
      });
    });
  });
});
