import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act, fireEvent, waitFor, within} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweWindow from "../../../src/components/AweWindow";
import AweAdvanced from "../../../src/components/AweAdvanced";
import {ComponentList} from "../../../src/components/AweComponents";
import {updateAttributes, updateModel} from "../../../src/redux/actions/components";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";

describe('awe-react-client/test/js/criteria/AweWindowTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      window: {
        address: {component: 'window', view: 'report'},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "window",
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders window component', () => {
    renderWithProviders(<AweWindow id="window"/>, {preloadedState});

    // check
    expect(document.querySelector("div.p-panel")).not.toBeNull();
  });

  it('exposes its identifier as the window-id attribute, the one that dependency actions target', () => {
    renderWithProviders(<AweWindow id="ExecutionsWindow" style="expand hidden"/>, {preloadedState});

    expect(document.querySelector("[window-id='ExecutionsWindow']")).toBe(document.querySelector("div.p-panel"));
  });


  it('lays its content out vertically by default', () => {
    renderWithProviders(<AweWindow id="w1"/>, {preloadedState});

    expect(document.querySelector("div.p-panel").classList).toContain("expandible-vertical");
  });

  it('lays its content out in the direction of its expandible attribute', () => {
    renderWithProviders(<AweWindow id="w2" expand="horizontal"/>, {preloadedState});

    const panel = document.querySelector("div.p-panel");
    expect(panel.classList).toContain("expandible-horizontal");
    expect(panel.classList).not.toContain("expandible-vertical");
  });

  it('shows no maximize button unless the window is maximizable', () => {
    renderWithProviders(<AweWindow id="w3"/>, {preloadedState});

    expect(document.querySelector("button.maximize-button")).toBeNull();
  });

  describe('maximize', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    const settle = (time = 600) => act(() => {
      jest.advanceTimersByTime(time);
    });
    const button = () => document.querySelector("button.maximize-button");
    const panel = () => document.querySelector("div.p-panel");

    it.each([true, "true"])('maximizes and restores the window with the maximize button (maximize=%s)', (maximize) => {
      renderWithProviders(<AweWindow id="w4" maximize={maximize} elementList={[]}/>, {preloadedState});
      expect(panel().classList).not.toContain("maximized");

      fireEvent.click(button());
      settle();
      expect(panel().classList).toContain("maximized");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#minimize");

      fireEvent.click(button());
      settle();
      expect(panel().classList).not.toContain("maximized");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#maximize");
    });

    it('maximizes over the maximize target of the template, not over a resizable that contains the window', () => {
      renderWithProviders(
        <div className="maximize-target" id="target">
          <div className="resizable" id="box"><AweWindow id="w5" maximize={true} elementList={[]}/></div>
        </div>, {preloadedState});
      jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
        .mockReturnValue({top: 40, left: 10, width: 800, height: 500});
      jest.spyOn(document.getElementById("box"), "getBoundingClientRect")
        .mockReturnValue({top: 100, left: 300, width: 200, height: 200});

      fireEvent.click(button());
      settle();

      expect(panel().style.position).toBe("fixed");
      expect([panel().style.top, panel().style.left, panel().style.width, panel().style.height])
        .toEqual(["40px", "10px", "800px", "500px"]);

      fireEvent.click(button());
      settle();
      expect(panel().style.position).toBe("");
    });

    it('follows the size of the maximize target while it is maximized', () => {
      renderWithProviders(<div className="maximize-target" id="target"><AweWindow id="w6" maximize={true}/></div>,
        {preloadedState});
      const rect = jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
        .mockReturnValue({top: 0, left: 0, width: 800, height: 500});
      fireEvent.click(button());
      settle();

      rect.mockReturnValue({top: 0, left: 0, width: 600, height: 400});
      act(() => {
        window.dispatchEvent(new Event("resize"));
      });

      expect(panel().style.width).toBe("600px");
    });

    it('fills the viewport when there is no maximize target', () => {
      renderWithProviders(<AweWindow id="w7" maximize={true}/>, {preloadedState});

      fireEvent.click(button());
      settle();

      expect([panel().style.position, panel().style.top, panel().style.left]).toEqual(["fixed", "0px", "0px"]);
    });

    it('minimizes its siblings while it is maximized and restores them afterwards', () => {
      renderWithProviders(
        <div className="maximize-target">
          <AweWindow id="w8" maximize={true}/>
          <div id="sibling">other</div>
        </div>, {preloadedState});
      const sibling = document.getElementById("sibling");

      fireEvent.click(button());
      settle();
      expect(sibling.classList).toContain("minimized");

      fireEvent.click(button());
      settle();
      expect(sibling.classList).not.toContain("minimized");
    });

    it('grows a clone over the target while it animates', () => {
      renderWithProviders(<AweWindow id="w9" maximize={true}/>, {preloadedState});

      fireEvent.click(button());

      expect(document.querySelector(".maximize-clone")).not.toBeNull();
      settle();
      expect(document.querySelector(".maximize-clone")).toBeNull();
    });

    it('exposes the window and its maximize button as test hooks that follow the state', () => {
      renderWithProviders(<AweWindow id="w10" maximize={true}/>, {preloadedState});
      const window10 = document.querySelector("[data-testid='window']");
      expect(window10).toBe(panel());
      expect(window10.getAttribute("data-testid-owner")).toBe("w10");
      expect(window10.getAttribute("data-maximized")).toBe("false");
      expect(button().getAttribute("data-testid")).toBe("window-maximize-button");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_MAXIMIZE");

      fireEvent.click(button());
      settle();
      expect(window10.getAttribute("data-maximized")).toBe("true");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_RESTORE");

      fireEvent.click(button());
      settle();
      expect(window10.getAttribute("data-maximized")).toBe("false");
    });

    it('renders the window hook without a maximize button when it is not maximizable', () => {
      renderWithProviders(<AweWindow id="w11"/>, {preloadedState});

      expect(document.querySelector("[data-testid='window']")).toBe(panel());
      expect(document.querySelector("[data-testid='window-maximize-button']")).toBeNull();
    });
  });

});

// Advanced search (#790): a window that holds an <advanced> block draws the block where it is: the disclosure row
// (Advanced filters, the count badge and the filters in effect) and below it the advanced criteria, shown
// on demand (inline) or in a side panel
describe("awe-react-client/test/js/components/AweWindowTest.jsx: advanced search", () => {
  const VIEW = "report";
  const THEMES = [{value: "1", label: "Light"}, {value: "2", label: "Dark"}];
  const select = (selected) => THEMES.map(option => ({...option, selected: option.value === selected}));

  const criterion = (id, component, label, values, defaults, source = ["SearchWindow", "SearchBody", id]) => ({
    address: {component: id, view: VIEW},
    model: {values},
    storedModel: {values},
    defaultModel: {values: defaults},
    attributes: {id, component, label, visible: true},
    validationRules: {},
    specificAttributes: {},
    context: {view: VIEW, source},
    actions: [],
    dependencies: []
  });

  const advancedSource = (id) => ["SearchWindow", "SearchBody", "AdvCrt", id];

  // User (essential) is in use, Theme (advanced) differs from its default Light and Blocked (advanced) is checked
  const preloadedState = () => ({
    settings: DEFAULT_SETTINGS,
    components: {
      CrtUsr: criterion("CrtUsr", "text", "User", [{value: "ana", label: "ana", selected: true}], []),
      CrtThm: criterion("CrtThm", "select", "Theme", select("2"), select("1"), advancedSource("CrtThm")),
      CrtBlk: criterion("CrtBlk", "checkbox", "Blocked", [{value: 1, selected: true}], [{value: 1, selected: false}],
        advancedSource("CrtBlk"))
    }
  });

  // As the server sends it: the essential criteria, then the advanced block with the advanced ones, in the body tag
  const advancedBlock = {elementType: "Advanced", id: "AdvCrt", elementList: [
    {elementType: "Criteria", component: "select", id: "CrtThm"},
    {elementType: "Criteria", component: "checkbox", id: "CrtBlk"}
  ]};
  const elementList = [
    {elementType: "Tag", type: "div", id: "SearchBody", style: "panel-body", elementList: [
      {elementType: "Criteria", component: "text", id: "CrtUsr"},
      advancedBlock
    ]}
  ];

  const renderWindow = (props = {}, options = {}) => renderWithProviders(
    <AweWindow id="SearchWindow" label="Search" advancedMode="inline" advancedCriteria={["CrtThm", "CrtBlk"]}
               searchCriteria={["CrtUsr", "CrtThm", "CrtBlk"]} elementList={elementList} {...props}/>,
    {preloadedState: preloadedState(), ...options});

  const byTestId = (id, container = document) => container.querySelector(`[data-testid='${id}']`);
  const allByTestId = (id, container = document) => [...container.querySelectorAll(`[data-testid='${id}']`)];
  const field = (id) => document.querySelector(`[criterion-id='${id}']`);
  const toggle = () => byTestId("window-advanced-toggle");
  const badge = () => byTestId("window-advanced-count");
  const chips = () => allByTestId("window-filter-chip");
  const chip = (text) => chips().find(element => element.textContent.includes(text));
  const block = () => document.querySelector(".window-advanced");
  const row = () => document.querySelector(".window-advanced-row");
  const region = () => document.querySelector(".window-advanced-region");
  const selected = (store, id) => store.getState().components[id].model.values
    .filter(value => value.selected).map(value => value.value);
  const update = (store, id, values) => act(() => {
    store.dispatch(updateModel({component: id, view: VIEW}, {values}));
  });
  const submits = (dispatchSpy) => dispatchSpy.mock.calls.map(([action]) => action)
    .filter(action => action?.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");

  it("registers the advanced block in the components of the structure", () => {
    expect(ComponentList.Advanced).toBe(AweAdvanced);
  });

  describe("a window without advanced block (S9)", () => {
    it("renders no toggle, no chips, no panel and nothing around its criteria", () => {
      renderWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined,
        elementList: [{elementType: "Tag", type: "div", id: "SearchBody", style: "panel-body", elementList: [
          {elementType: "Criteria", component: "text", id: "CrtUsr"},
          {elementType: "Criteria", component: "select", id: "CrtThm"},
          {elementType: "Criteria", component: "checkbox", id: "CrtBlk"}
        ]}]});

      expect(toggle()).toBeNull();
      expect(badge()).toBeNull();
      expect(byTestId("window-filter-chips")).toBeNull();
      expect(byTestId("window-advanced-panel")).toBeNull();
      expect(document.querySelector(".window-advanced, .window-advanced-bar, .window-advanced-holder")).toBeNull();
      const body = document.getElementById("SearchBody");
      expect([...document.querySelector(".p-panel-content").children]).toEqual([body]);
      expect([...body.children].map(child => child.getAttribute("criterion-id"))).toEqual(["CrtUsr", "CrtThm", "CrtBlk"]);
      ["CrtUsr", "CrtThm", "CrtBlk"].forEach(id => expect(field(id).classList).not.toContain("hidden"));
    });

    it("renders the children of an advanced block outside a window with advanced search as they are", () => {
      renderWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined});

      expect(toggle()).toBeNull();
      expect(block()).toBeNull();
      expect(field("CrtThm").parentElement).toBe(document.getElementById("SearchBody"));
      expect(field("CrtThm").classList).not.toContain("hidden");
    });
  });

  describe("the block in the window", () => {
    it("is drawn where the block is, after the essential criteria, and the window draws nothing else (S17)", () => {
      renderWindow();

      const body = document.getElementById("SearchBody");
      expect([...document.querySelector(".p-panel-content").children]).toEqual([body]);
      expect([...body.children]).toEqual([field("CrtUsr"), block()]);
      expect(block().id).toBe("AdvCrt");
      // The disclosure row first (the toggle, then the filters in effect), the region of the advanced criteria below it
      expect([...block().children]).toEqual([row(), region()]);
      expect(row().firstElementChild).toBe(toggle());
      expect(row().children[1]).toBe(byTestId("window-filter-chips"));
      expect(region().contains(field("CrtThm"))).toBe(true);
      expect(region().contains(field("CrtBlk"))).toBe(true);
      expect(document.querySelector(".window-advanced-bar")).toBeNull();
      // The title bar keeps its own content
      expect(document.querySelector(".p-panel-header").contains(toggle())).toBe(false);
    });
  });

  describe("inline mode", () => {
    it("starts collapsed: the region is hidden and the advanced criteria stay mounted in it", () => {
      renderWindow();

      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(toggle().textContent).toContain("SCREEN_TEXT_ADVANCED_FILTERS");
      expect(toggle().getAttribute("aria-controls")).toBe(region().id);
      expect(region().id).toBe("SearchWindow-advanced-region");
      expect(region().hidden).toBe(true);
      expect(toggle().querySelector(".window-advanced-chevron svg.awe-icon")).not.toBeNull();
      expect(field("CrtThm")).not.toBeNull();
      // The block hides them, not the criteria
      ["CrtUsr", "CrtThm", "CrtBlk"].forEach(id => expect(field(id).classList).not.toContain("hidden"));
    });

    it("opens the region below the row with the toggle, and closes it again", () => {
      renderWindow({maximize: true});
      const icons = document.querySelector(".p-panel-icons");
      expect([...icons.querySelectorAll("button")].map(button => button.getAttribute("data-testid")))
        .toEqual(["window-maximize-button"]);

      fireEvent.click(toggle());
      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      expect(toggle().textContent).toContain("SCREEN_TEXT_HIDE_ADVANCED_FILTERS");
      expect(region().hidden).toBe(false);
      expect(row().nextElementSibling).toBe(region());

      fireEvent.click(toggle());
      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(region().hidden).toBe(true);
    });

    it("keeps the row with the toggle when no filter is in effect, without the chips", () => {
      const {store} = renderWindow();
      update(store, "CrtUsr", []);
      update(store, "CrtThm", select("1"));
      update(store, "CrtBlk", [{value: 1, selected: false}]);

      expect([...row().children]).toEqual([toggle()]);
      expect(byTestId("window-filter-chips")).toBeNull();
      expect(byTestId("window-filter-clear-all")).toBeNull();
    });

    it("reveals a hidden advanced criterion when its validation fails", () => {
      const {store} = renderWindow();
      expect(region().hidden).toBe(true);

      act(() => {
        store.dispatch(updateAttributes({component: "CrtThm", view: VIEW}, {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}}));
      });

      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      expect(region().hidden).toBe(false);
    });

    it("submits from the window with Enter in a criterion of the block, so its own submit button runs", () => {
      const state = preloadedState();
      state.components.CrtCod = criterion("CrtCod", "text", "Code", [], [], advancedSource("CrtCod"));
      const {dispatchSpy} = renderWindow({advancedCriteria: ["CrtThm", "CrtBlk", "CrtCod"],
        searchCriteria: ["CrtUsr", "CrtThm", "CrtBlk", "CrtCod"],
        elementList: [{...elementList[0], elementList: [elementList[0].elementList[0],
          {...advancedBlock, elementList: [...advancedBlock.elementList, {elementType: "Criteria", component: "text", id: "CrtCod"}]}]}]},
      {preloadedState: state, spyDispatch: true});

      fireEvent.keyDown(field("CrtCod").querySelector("input"), {key: "Enter", code: "Enter"});

      expect(submits(dispatchSpy)).toEqual([{type: "submit", target: "SearchWindow", address: {component: "CrtCod", view: VIEW}}]);
    });
  });

  describe("count badge (S4)", () => {
    it("counts only the advanced criteria whose value differs from its default, and follows the values", () => {
      const {store} = renderWindow();
      expect(badge().textContent).toBe("2");
      expect(toggle().querySelector(".p-hidden-accessible").textContent).toBe("(2)");

      update(store, "CrtBlk", [{value: 1, selected: false}]);
      expect(badge().textContent).toBe("1");

      // An essential criterion back to its default does not change the count
      update(store, "CrtUsr", []);
      expect(badge().textContent).toBe("1");

      update(store, "CrtThm", select("1"));
      expect(badge()).toBeNull();
    });
  });

  describe("filter chips (S5, S6, S7)", () => {
    it("lists every criterion of the window whose value differs from its default, marking hidden advanced ones", () => {
      renderWindow();

      const group = byTestId("window-filter-chips");
      expect(group.getAttribute("role")).toBe("group");
      expect(group.textContent).toContain("SCREEN_TEXT_FILTERED_BY");
      expect(chips().map(element => element.querySelector(".window-filter-chip-text").textContent))
        .toEqual(["User: ana", "Theme: Dark", "Blocked"]);
      expect(chip("Theme").querySelector("b").textContent).toBe("Dark");
      expect(chip("Theme").getAttribute("data-filter-id")).toBe("CrtThm");
      expect(chip("Theme").getAttribute("data-testid-owner")).toBe("SearchWindow");
      // Hidden advanced filters carry an eye-off icon and a text for the screen readers
      expect(chip("User").classList).not.toContain("window-filter-chip-hidden");
      expect(chip("User").querySelector(".window-filter-chip-marker")).toBeNull();
      expect(chip("Theme").classList).toContain("window-filter-chip-hidden");
      expect(chip("Theme").querySelector(".window-filter-chip-marker")).not.toBeNull();
      expect(chip("Theme").querySelector(".p-hidden-accessible").textContent).toBe("(SCREEN_TEXT_HIDDEN_ADVANCED_FILTER)");
      expect(chip("Blocked").classList).toContain("window-filter-chip-hidden");
      // Clear all closes the group
      expect([...group.querySelectorAll("button")].pop()).toBe(byTestId("window-filter-clear-all"));

      fireEvent.click(toggle());
      expect(chip("Theme").classList).not.toContain("window-filter-chip-hidden");
      expect(chip("Theme").querySelector(".window-filter-chip-marker")).toBeNull();
    });

    it("shows the first value and the number of the others for a multiple criterion", () => {
      const state = preloadedState();
      state.components.CrtUsr = criterion("CrtUsr", "select-multiple", "Profiles",
        [{value: "a", label: "Admin", selected: true}, {value: "b", label: "Basic", selected: true},
          {value: "c", label: "Guest", selected: true}], []);
      renderWindow({}, {preloadedState: state});

      expect(chip("Profiles").querySelector(".window-filter-chip-text").textContent).toBe("Profiles: Admin +2");
    });

    it("shows both dates of a date range", () => {
      const state = preloadedState();
      state.components.CrtUsr = criterion("CrtUsr", "date-range", "Period",
        [{value: "01/01/2026", selected: true}, {value: "31/01/2026", selected: true}], []);
      renderWindow({}, {preloadedState: state});

      expect(chip("Period").querySelector(".window-filter-chip-text").textContent).toBe("Period: 01/01/2026 - 31/01/2026");
    });

    it("restores the criterion of a removed chip to its default", () => {
      const {store} = renderWindow();
      const remove = byTestId("window-filter-chip-remove", chip("Theme"));
      expect(remove.getAttribute("aria-label")).toBe("SCREEN_TEXT_REMOVE_FILTER Theme");

      fireEvent.click(remove);

      expect(selected(store, "CrtThm")).toEqual(["1"]);
      expect(chip("Theme")).toBeUndefined();
      expect(selected(store, "CrtUsr")).toEqual(["ana"]);
      expect(badge().textContent).toBe("1");
    });

    it("restores every criterion of the window with Clear all", () => {
      const {store} = renderWindow();

      fireEvent.click(byTestId("window-filter-clear-all"));

      expect(selected(store, "CrtUsr")).toEqual([]);
      expect(selected(store, "CrtThm")).toEqual(["1"]);
      expect(selected(store, "CrtBlk")).toEqual([]);
      expect(byTestId("window-filter-chips")).toBeNull();
      expect(badge()).toBeNull();
    });
  });

  describe("side-panel mode", () => {
    const renderPanelWindow = (options) => renderWindow({advancedMode: "side-panel"}, options);
    const panel = () => byTestId("window-advanced-panel");
    // The panel plays its exit animation once closed
    const isOpen = () => panel()?.getAttribute("data-open") === "true";
    const open = () => fireEvent.click(toggle());

    it("keeps the children of the block mounted in a hidden holder of the block while the panel is closed", () => {
      renderPanelWindow();

      expect(toggle().textContent).toContain("SCREEN_TEXT_ADVANCED_FILTERS");
      expect(toggle().textContent).not.toContain("SCREEN_TEXT_ALL_FILTERS");
      expect(toggle().getAttribute("aria-haspopup")).toBe("dialog");
      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(toggle().getAttribute("aria-controls")).toBe("SearchWindow-advanced-panel");
      expect(toggle().querySelector(".window-advanced-chevron")).toBeNull();
      expect(region()).toBeNull();
      expect(panel()).toBeNull();
      expect(row().parentElement).toBe(block());
      expect(field("CrtThm").closest(".window-advanced-holder")?.parentElement).toBe(block());
      expect(field("CrtBlk").closest(".window-advanced-holder")).not.toBeNull();
      expect(field("CrtUsr").closest(".window-advanced-holder")).toBeNull();
    });

    it("opens a side panel titled with the window label that holds the children of the block", () => {
      renderPanelWindow();

      open();

      expect(panel()).not.toBeNull();
      expect(panel().getAttribute("data-testid-owner")).toBe("SearchWindow");
      expect(panel().querySelector(".side-panel-title").textContent).toBe("Search");
      // The width of the filter panel of the design, relative to the text size and not to the root font size, which
      // differs between the engines (it takes the whole width of a small screen)
      expect(panel().style.maxWidth).toBe("39em");
      expect(panel().contains(field("CrtThm"))).toBe(true);
      expect(panel().contains(field("CrtBlk"))).toBe(true);
      expect(panel().contains(field("CrtUsr"))).toBe(false);
      expect(toggle().getAttribute("aria-expanded")).toBe("true");
    });

    it("restores the values the advanced criteria had when it opened with Cancel", async () => {
      const {store} = renderPanelWindow();
      open();
      update(store, "CrtBlk", [{value: 1, selected: false}]);
      update(store, "CrtThm", select("1"));

      fireEvent.click(byTestId("window-advanced-cancel"));

      expect(isOpen()).toBe(false);
      expect(selected(store, "CrtBlk")).toEqual([1]);
      expect(selected(store, "CrtThm")).toEqual(["2"]);
      // Back in the holder of the block once the panel is gone
      await waitFor(() => expect(panel()).toBeNull());
      expect(field("CrtThm").closest(".window-advanced-holder")).not.toBeNull();
    });

    it("cancels with Escape", () => {
      const {store} = renderPanelWindow();
      open();
      update(store, "CrtBlk", [{value: 1, selected: false}]);

      fireEvent.keyDown(panel(), {key: "Escape", code: "Escape"});

      expect(isOpen()).toBe(false);
      expect(selected(store, "CrtBlk")).toEqual([1]);
    });

    it("restores the advanced criteria to their defaults with Reset, and stays open", () => {
      const {store} = renderPanelWindow();
      open();

      fireEvent.click(byTestId("window-advanced-reset"));

      expect(isOpen()).toBe(true);
      expect(selected(store, "CrtThm")).toEqual(["1"]);
      expect(selected(store, "CrtBlk")).toEqual([]);
      expect(selected(store, "CrtUsr")).toEqual(["ana"]);
      expect(badge()).toBeNull();
    });

    it("closes and submits from the window with Show results, so its own submit button runs", () => {
      const {store, dispatchSpy} = renderPanelWindow({spyDispatch: true});
      open();
      update(store, "CrtBlk", [{value: 1, selected: false}]);

      fireEvent.click(byTestId("window-advanced-apply"));

      expect(submits(dispatchSpy)).toEqual([{type: "submit", target: "SearchWindow", address: {component: "CrtThm", view: VIEW}}]);
      expect(isOpen()).toBe(false);
      // The values chosen in the panel are kept
      expect(selected(store, "CrtBlk")).toEqual([]);
    });

    it("is a modal dialog that gives the focus back to the Advanced filters button when it closes", () => {
      renderPanelWindow();
      toggle().focus();
      open();
      expect(panel().getAttribute("role")).toBe("dialog");
      expect(panel().getAttribute("aria-modal")).toBe("true");
      expect(panel().getAttribute("aria-label")).toBe("Search");
      byTestId("window-advanced-cancel").focus();

      fireEvent.click(byTestId("window-advanced-cancel"));

      expect(document.activeElement).toBe(toggle());
    });

    it("labels its footer buttons", () => {
      renderPanelWindow();
      open();

      const footer = within(panel());
      expect(footer.getByText("SCREEN_TEXT_RESET_DEFAULTS")).not.toBeNull();
      expect(footer.getByText("BUTTON_CANCEL")).not.toBeNull();
      expect(footer.getByText("SCREEN_TEXT_SHOW_RESULTS")).not.toBeNull();
      // Reset to defaults is a text button with its icon, as in the design
      expect(byTestId("window-advanced-reset").querySelector("svg.awe-icon")).not.toBeNull();
    });

    it("opens to reveal a hidden advanced criterion whose validation fails", () => {
      const {store} = renderPanelWindow();

      act(() => {
        store.dispatch(updateAttributes({component: "CrtBlk", view: VIEW}, {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}}));
      });

      expect(isOpen()).toBe(true);
      expect(panel().contains(field("CrtBlk"))).toBe(true);
    });
  });

  // Delivery 2 of #790: the open state of the inline block is remembered per user, screen and window. The window
  // controller carries advancedOpen only when a state is stored, and every user toggle saves it silently
  describe("remembered open state", () => {
    const originalFetch = global.fetch;
    const saves = () => global.fetch.mock.calls
      .filter(([url]) => url.includes("/action/maintain-silent/saveWindowAdvancedState"))
      .map(([, options]) => JSON.parse(options.body));
    // Messages queued to be launched (the thunk dispatches them inside the store, so a dispatch spy does not see them)
    const messages = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async.flat()]
      .filter(action => action?.type === "message");
    const flush = () => act(async () => {
      await Promise.resolve();
    });

    beforeEach(() => {
      global.fetch = jest.fn(() => Promise.resolve({ok: true, status: 200, json: () => Promise.resolve([]),
        text: () => Promise.resolve("[]")}));
      // The screen of the view of the window, as the screen data sets it
      ViewRegistry.setView(VIEW, {name: "UsrLst", loading: false});
    });

    afterEach(() => {
      global.fetch = originalFetch;
      ViewRegistry.clearAll();
    });

    it("starts the inline block open when the window component of the screen carries advancedOpen true", () => {
      // The server puts the stored state in the controller of the window (the component in the store), not in the
      // screen structure the window is rendered from
      const state = preloadedState();
      state.components.SearchWindow = {address: {component: "SearchWindow", view: VIEW}, attributes: {advancedOpen: true},
        validationRules: {}, specificAttributes: {}};
      renderWithProviders(
        <AweWindow id="SearchWindow" label="Search" advancedMode="inline" advancedCriteria={["CrtThm", "CrtBlk"]}
                   searchCriteria={["CrtUsr", "CrtThm", "CrtBlk"]} elementList={elementList}/>,
        {preloadedState: state});

      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      expect(region().hidden).toBe(false);
    });

    it("starts the inline block open when the window has advancedOpen true", () => {
      renderWindow({advancedOpen: true});

      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      expect(region().hidden).toBe(false);
      expect(saves()).toEqual([]);
    });

    it("starts the inline block collapsed when the window has advancedOpen false", () => {
      renderWindow({advancedOpen: false});

      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(region().hidden).toBe(true);
    });

    it("saves the state with every toggle of the user, with the screen, the window and the new state", async () => {
      renderWindow();

      fireEvent.click(toggle());
      await flush();
      expect(saves()).toEqual([{advancedStateScreen: "UsrLst", advancedStateWindow: "SearchWindow", advancedStateOpen: "1"}]);

      fireEvent.click(toggle());
      await flush();
      expect(saves()).toEqual([
        {advancedStateScreen: "UsrLst", advancedStateWindow: "SearchWindow", advancedStateOpen: "1"},
        {advancedStateScreen: "UsrLst", advancedStateWindow: "SearchWindow", advancedStateOpen: "0"}
      ]);
      expect(global.fetch).toHaveBeenCalledTimes(2);
    });

    it("saves the closed state when the user closes a block that started open", async () => {
      renderWindow({advancedOpen: true});

      fireEvent.click(toggle());
      await flush();

      expect(region().hidden).toBe(true);
      expect(saves()).toEqual([{advancedStateScreen: "UsrLst", advancedStateWindow: "SearchWindow", advancedStateOpen: "0"}]);
    });

    it("does not save when the block opens to reveal a criterion whose validation fails", async () => {
      const {store} = renderWindow();

      act(() => {
        store.dispatch(updateAttributes({component: "CrtThm", view: VIEW}, {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}}));
      });
      await flush();

      expect(region().hidden).toBe(false);
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("keeps the state and shows an error message when the save fails", async () => {
      global.fetch = jest.fn(() => Promise.reject(new Error("down")));
      const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      const {store} = renderWindow({});

      fireEvent.click(toggle());
      await flush();
      await flush();

      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      expect(region().hidden).toBe(false);
      expect(messages(store)).toEqual([expect.objectContaining({parameters: expect.objectContaining({
        type: "error", title: "SCREEN_TEXT_ADVANCED_STATE_ERROR_TITLE", message: "SCREEN_TEXT_ADVANCED_STATE_ERROR_MESSAGE"})})]);
      consoleSpy.mockRestore();
    });

    it("keeps the state and shows the message of the server when it answers the save with an error", async () => {
      const answer = [{type: "message", parameters: {type: "error", title: "Error", message: "Failed"}}];
      global.fetch = jest.fn(() => Promise.resolve({ok: true, status: 200, json: () => Promise.resolve(answer),
        text: () => Promise.resolve(JSON.stringify(answer))}));
      const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      const {store} = renderWindow({});

      fireEvent.click(toggle());
      await flush();
      await flush();

      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(region().hidden).toBe(false);
      expect(messages(store)).toEqual([expect.objectContaining({parameters: expect.objectContaining({
        type: "error", title: "Error", message: "Failed"})})]);
      consoleSpy.mockRestore();
    });

    it("starts the side panel closed whatever advancedOpen says, and saves nothing when it opens or closes", async () => {
      renderWindow({advancedMode: "side-panel", advancedOpen: true});
      const panel = () => byTestId("window-advanced-panel");

      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(panel()?.getAttribute("data-open") === "true").toBe(false);

      fireEvent.click(toggle());
      await flush();
      expect(toggle().getAttribute("aria-expanded")).toBe("true");
      fireEvent.click(byTestId("window-advanced-cancel"));
      await flush();

      expect(toggle().getAttribute("aria-expanded")).toBe("false");
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("ignores advancedOpen in a window without advanced block", () => {
      renderWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined, advancedOpen: true});

      expect(toggle()).toBeNull();
      expect(block()).toBeNull();
      expect(field("CrtThm").parentElement).toBe(document.getElementById("SearchBody"));
      expect(global.fetch).not.toHaveBeenCalled();
    });
  });
});
