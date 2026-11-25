import {act, fireEvent, screen} from "@testing-library/react";
import i18n from "i18next";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";

describe('awe-react-client/test/js/columns/ColumnLinkTest.jsx', () => {

  beforeAll(() => {
    i18n.addResources("en-GB", "translation", {LINK_OPENS_IN_NEW_TAB: "(opens in a new tab)"});
  });

  it('renders Column Link component', () => {
    renderWithProviders(Columns({
      component: 'link',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'link', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("a")).not.toBeNull();
  });

  it('opens the value of a link column without actions as an external address in a new tab', () => {
    renderWithProviders(Columns({
      component: 'link', actions: [],
      address: {component: 'GrdDoc', view: 'report', column: 'Url', row: 1}
    }, {value: "https://www.almis.com", label: "Almis"}, {}, false));

    const link = screen.getByRole("link", {name: "Almis (opens in a new tab)"});
    expect(link.getAttribute("href")).toBe("https://www.almis.com");
    expect(link.getAttribute("target")).toBe("_blank");
    expect(link.getAttribute("rel")).toBe("noreferrer");
  });

  it('marks a link column without actions as opening a new tab with a decorative icon and a hidden text', () => {
    renderWithProviders(Columns({
      component: 'link', actions: [],
      address: {component: 'GrdDoc', view: 'report', column: 'Url', row: 1}
    }, {value: "https://www.almis.com", label: "Almis"}, {}, false));

    const link = screen.getByRole("link", {name: "Almis (opens in a new tab)"});
    expect(link.querySelector(".p-hidden-accessible").textContent.trim()).toBe("(opens in a new tab)");
    const icon = link.querySelector("svg use");
    expect(icon.getAttribute("href")).toMatch(/#external-link$/);
    expect(icon.closest("[aria-hidden='true']")).not.toBeNull();
  });

  describe('with actions', () => {
    const actions = [{type: "screen", target: "flow-run", parameters: {}}];
    const address = {view: "v", component: "GrdTsk", row: 2, column: "FlwLnk"};

    const state = (selected) => ({
      settings: DEFAULT_SETTINGS,
      actions: {running: false},
      components: {
        GrdTsk: {
          address: {view: "v", component: "GrdTsk"},
          attributes: {multiselect: true, columnModel: []},
          model: {values: [1, 2, 3].map(id => ({id, selected: selected.includes(id)})), selected}
        }
      }
    });

    const render = (selected, attrs = {}, column = {}) => renderWithProviders(Columns({
      component: "link", name: "FlwLnk", actions, address, ...column
    }, {value: "F12", label: "Nightly flow"}, attrs, false), {preloadedState: state(selected), spyDispatch: true});

    const selectedRows = store => store.getState().components.GrdTsk.model.values.filter(row => row.selected)
      .map(row => row.id);
    const launchedActions = dispatchSpy => dispatchSpy.mock.calls.map(([action]) => action)
      .filter(action => action && action.type === ADD_ACTIONS_TOP);

    it('shows the secondary field of the row in a line under the link, and only the link when it is empty (#993)', () => {
      const cell = (row) => renderWithProviders(Columns({component: "link", name: "TskNam", actions, address, secondaryField: "TskDes"},
        {value: "F12", label: "Nightly flow"}, {}, false, row), {preloadedState: state([])});

      const {unmount} = cell({TskDes: {value: "Imports the bank statements"}});
      const link = screen.getByRole("link", {name: "Nightly flow"});
      expect(link.parentElement.querySelector(".awe-cell-secondary").textContent).toBe("Imports the bank statements");
      expect(link.parentElement.querySelector(".awe-cell-secondary").getAttribute("title")).toBe("Imports the bank statements");
      unmount();

      cell({TskDes: ""});
      expect(document.querySelector(".awe-cell-secondary")).toBeNull();
      expect(screen.getByRole("link", {name: "Nightly flow"})).not.toBeNull();
    });

    it('renders the label as a focusable link without an external address', () => {
      render([]);

      const link = screen.getByRole("link", {name: "Nightly flow"});
      expect(link.hasAttribute("href")).toBe(false);
      expect(link.hasAttribute("target")).toBe(false);
      expect(link.tabIndex).toBe(0);
      expect(link.hasAttribute("title")).toBe(false);
    });

    it('has no new tab icon nor hidden new tab text', () => {
      render([]);

      const link = screen.getByRole("link", {name: "Nightly flow"});
      expect(link.querySelector("svg")).toBeNull();
      expect(link.querySelector(".p-hidden-accessible")).toBeNull();
      expect(link.textContent).toBe("Nightly flow");
    });

    it('shows the title of the column as its tooltip when the cell has none', () => {
      render([], {}, {title: "Open the run detail"});

      expect(screen.getByRole("link", {name: "Nightly flow"}).getAttribute("title")).toBe("Open the run detail");
    });

    it('selects only its row and launches the column actions with the cell address when clicked', () => {
      const {store, dispatchSpy} = render([1, 3]);

      act(() => screen.getByRole("link", {name: "Nightly flow"}).click());

      expect(selectedRows(store)).toEqual([2]);
      expect(launchedActions(dispatchSpy)).toHaveLength(1);
      expect(launchedActions(dispatchSpy)[0].actions).toEqual([{...actions[0], address}]);
    });

    it('launches the column actions when Enter is pressed on it', () => {
      const {store, dispatchSpy} = render([]);

      fireEvent.keyDown(screen.getByRole("link", {name: "Nightly flow"}), {key: "Enter"});

      expect(selectedRows(store)).toEqual([2]);
      expect(launchedActions(dispatchSpy)).toHaveLength(1);
    });

    it.each([{disabled: true}, {readonly: true}])('launches nothing and is marked disabled when the cell is %o', (attrs) => {
      const {store, dispatchSpy} = render([1], attrs);

      const link = screen.getByRole("link", {name: "Nightly flow"});
      act(() => link.click());
      fireEvent.keyDown(link, {key: "Enter"});

      expect(link.getAttribute("aria-disabled")).toBe("true");
      expect(selectedRows(store)).toEqual([1]);
      expect(launchedActions(dispatchSpy)).toHaveLength(0);
    });
  });
});
