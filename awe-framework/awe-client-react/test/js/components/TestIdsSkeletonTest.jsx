import React from "react";
import {act, cleanup, render} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";
import AweLoader, {LOADER_ICONS} from "../../../src/components/AweLoader";
import {updateAttributes} from "../../../src/redux/actions/components";

const hook = (testId) => `[data-testid='${testId}']`;

function gridState(attributes = {}, {values = [], tree = false, settings = {}} = {}) {
  return {
    settings: {...DEFAULT_SETTINGS, ...settings},
    screen: {size: {width: 100, height: 100}},
    components: {
      Grd: {
        address: {component: "Grd", view: "report"},
        model: {values},
        attributes: {
          columnModel: [{name: "name", label: "Name"}, {name: "age", label: "Age"}, {name: "secret", hidden: true}],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          ...(tree ? {treegrid: true} : {}),
          ...attributes
        },
        specificAttributes: {sort: []}
      }
    }
  };
}

const rows = [
  {id: "r1", name: "Ann", age: 30},
  {id: "r2", name: "Bob", age: 41}
];

describe("awe-react-client/test/js/components/TestIdsSkeletonTest.jsx", () => {
  // Known warnings PrimeReact raises for the Row/ColumnGroup of the header (they exist without these tests too)
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

  it("uses the skeleton as the default grid loading icon before the server settings arrive", () => {
    expect(DEFAULT_SETTINGS.gridIconLoading).toBe("skeleton");
  });

  describe.each([
    ["grid", AweGrid, false],
    ["tree grid", AweTreeGrid, true]
  ])("%s", (_name, Component, tree) => {
    const renderGrid = (attributes, options = {}) =>
      renderWithProviders(<Component id="Grd"/>, {preloadedState: gridState(attributes, {tree, ...options})});

    it("shows skeleton rows with the column model while loading without icon-loading", () => {
      const {container} = renderGrid({loading: true, loadAll: true, max: 3}, {values: rows});

      const loader = container.querySelector(hook("grid-loader"));
      expect(loader).not.toBeNull();
      expect(loader.getAttribute("aria-hidden")).toBe("true");
      expect(container.querySelector(".p-progress-spinner")).toBeNull();
      const skeletonRows = loader.querySelectorAll(".grid-skeleton-row");
      expect(skeletonRows).toHaveLength(3);
      // One placeholder per visible column
      [...skeletonRows].forEach(row => expect(row.querySelectorAll(".p-skeleton")).toHaveLength(2));
      // The header stays visible and the old rows are replaced by the placeholders
      expect(container.querySelectorAll(hook("grid-header-cell"))).not.toHaveLength(0);
      expect(container.querySelector(hook("grid-cell"))).toBeNull();
    });

    it("caps the skeleton rows to ten", () => {
      const {container} = renderGrid({loading: true, loadAll: true, max: 50});

      expect(container.querySelectorAll(`${hook("grid-loader")} .grid-skeleton-row`)).toHaveLength(10);
    });

    it("marks the grid container as busy only while loading", () => {
      const loading = renderGrid({loading: true, loadAll: true});
      expect(loading.container.querySelector(".grid-container").getAttribute("aria-busy")).toBe("true");
      cleanup();

      const idle = renderGrid({loadAll: true}, {values: rows});
      expect(idle.container.querySelector(".grid-container").getAttribute("aria-busy")).toBeNull();
      expect(idle.container.querySelector(hook("grid-loader"))).toBeNull();
      expect(idle.container.querySelector(".p-skeleton")).toBeNull();
    });

    it("keeps the spinner when icon-loading is spinner", () => {
      const {container} = renderGrid({loading: true, loadAll: true, iconLoading: "spinner"});

      expect(container.querySelector(`.p-progress-spinner${hook("grid-loader")}`)).not.toBeNull();
      expect(container.querySelector(".p-skeleton")).toBeNull();
    });

    it("uses the application default when the grid has no icon-loading", () => {
      const {container} = renderGrid({loading: true, loadAll: true}, {settings: {gridIconLoading: "spinner"}});

      expect(container.querySelector(`.p-progress-spinner${hook("grid-loader")}`)).not.toBeNull();
      expect(container.querySelector(".p-skeleton")).toBeNull();
    });

    it("lets an explicit skeleton win over a spinner application default", () => {
      const {container} = renderGrid({loading: true, loadAll: true, iconLoading: "skeleton"},
        {settings: {gridIconLoading: "spinner"}});

      expect(container.querySelector(`${hook("grid-loader")} .p-skeleton`)).not.toBeNull();
      expect(container.querySelector(".p-progress-spinner")).toBeNull();
    });
  });

  it("keeps the page of a client-side paginated grid while the skeleton replaces its rows", () => {
    const many = Array.from({length: 25}, (_, index) => ({id: `r${index}`, name: `Row ${index}`, age: index}));
    const state = gridState({loadAll: true, max: 10}, {values: many});
    state.components.Grd.model.records = 25;
    state.components.Grd.specificAttributes = {sort: [], first: 10, rows: 10};
    const {container, store, dispatchSpy} = renderWithProviders(<AweGrid id="Grd"/>, {preloadedState: state, spyDispatch: true});

    act(() => {
      store.dispatch(updateAttributes({component: "Grd", view: "report"}, {loading: true}));
    });

    expect(container.querySelector(`${hook("grid-loader")} .grid-skeleton-row`)).not.toBeNull();
    expect(container.querySelector(".p-paginator-page.p-highlight").textContent).toBe("2");
    expect(JSON.stringify(dispatchSpy.mock.calls)).not.toContain("change-page");
  });

  describe("loader", () => {
    it("offers the skeleton loading icon", () => {
      expect(LOADER_ICONS).toContain("skeleton");
    });

    it("renders a block skeleton hidden from assistive technologies", () => {
      const {container} = render(<AweLoader icon="skeleton" rootProps={{"data-testid": "loader"}}/>);

      const loader = container.querySelector(hook("loader"));
      expect(loader).not.toBeNull();
      expect(loader.getAttribute("aria-hidden")).toBe("true");
      expect(loader.querySelector(".p-skeleton")).not.toBeNull();
      expect(container.querySelector(".p-progress-spinner")).toBeNull();
    });
  });
});
