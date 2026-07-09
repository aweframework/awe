import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweMenuSearch from "../../../src/components/AweMenuSearch";
import AweMenu from "../../../src/components/AweMenu";
import MenuRegistry from "../../../src/redux/registry/MenuRegistry";

// Allowance predicate mirroring the menu filter (visible and not restricted)
const isAllowed = (option) => option.visible !== false && !option.restricted;

// Deep sample tree: Sales > Invoices > New invoice
const tree = [
  {
    name: "sales", label: "Sales", visible: true, options: [
      {
        name: "invoices", label: "Invoices", visible: true, options: [
          { name: "newInvoice", label: "New invoice", visible: true, actions: [{ type: "screen" }] }
        ]
      }
    ]
  }
];

function renderSearch(props = {}) {
  const onSelect = props.onSelect || jest.fn();
  const utils = render(
    <AweMenuSearch options={tree} menuType="vertical" isAllowed={isAllowed} onSelect={onSelect} {...props} />
  );
  return { ...utils, onSelect };
}

function openPanel() {
  fireEvent.click(screen.getByRole("button"));
  return screen.getByRole("textbox");
}

describe('components/AweMenuSearch.jsx', () => {

  it('renders the toggle and keeps the panel closed initially', () => {
    renderSearch();

    const toggle = screen.getByRole("button");
    expect(toggle).toHaveAttribute("title", "BUTTON_SEARCH");
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('toggles the panel open and closed', () => {
    renderSearch();
    const toggle = screen.getByRole("button");

    fireEvent.click(toggle);
    expect(screen.getByRole("textbox")).toBeInTheDocument();

    fireEvent.click(toggle);
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('resets the query and results when closed via the toggle', () => {
    renderSearch();
    const toggle = screen.getByRole("button");

    fireEvent.change(openPanel(), { target: { value: "invoice" } });
    expect(screen.getAllByRole("option")).toHaveLength(1);

    fireEvent.click(toggle); // close
    fireEvent.click(toggle); // reopen

    expect(screen.getByRole("textbox")).toHaveValue("");
    expect(screen.queryAllByRole("option")).toHaveLength(0);
  });

  it('applies the orientation class', () => {
    const { container } = renderSearch({ menuType: "horizontal" });
    expect(container.querySelector(".awe-menu-search-horizontal")).toBeInTheDocument();
  });

  it('filters nested options into flat breadcrumb results', () => {
    renderSearch();
    const input = openPanel();

    fireEvent.change(input, { target: { value: "invoice" } });

    const items = screen.getAllByRole("option");
    expect(items).toHaveLength(1);
    expect(within(items[0]).getByText("New invoice")).toBeInTheDocument();
    expect(within(items[0]).getByText("Sales › Invoices")).toBeInTheDocument();
    // First result is active by default
    expect(items[0]).toHaveClass("active");
  });

  it('shows the empty state when nothing matches', () => {
    renderSearch();
    const input = openPanel();

    fireEvent.change(input, { target: { value: "zzz-nothing" } });

    expect(screen.queryAllByRole("option")).toHaveLength(0);
    expect(screen.getByText("MENU_SEARCH_EMPTY")).toBeInTheDocument();
  });

  it('launches the selected option on click and closes the panel', () => {
    const { onSelect } = renderSearch();
    const input = openPanel();

    fireEvent.change(input, { target: { value: "invoice" } });
    fireEvent.click(screen.getAllByRole("option")[0]);

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ name: "newInvoice" }));
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('navigates results with the keyboard and launches the active one on Enter', () => {
    const options = [
      { name: "a", label: "Alpha", visible: true, actions: [{ type: "screen" }] },
      { name: "b", label: "Alfa", visible: true, actions: [{ type: "screen" }] }
    ];
    const onSelect = jest.fn();
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={onSelect} />);
    const input = openPanel();

    fireEvent.change(input, { target: { value: "a" } });
    expect(screen.getAllByRole("option")).toHaveLength(2);
    expect(screen.getAllByRole("option")[0]).toHaveClass("active");

    fireEvent.keyDown(input, { keyCode: 40 }); // down -> index 1
    expect(screen.getAllByRole("option")[1]).toHaveClass("active");
    fireEvent.keyDown(input, { keyCode: 40 }); // down wraps -> index 0
    expect(screen.getAllByRole("option")[0]).toHaveClass("active");
    fireEvent.keyDown(input, { keyCode: 38 }); // up wraps -> index 1
    expect(screen.getAllByRole("option")[1]).toHaveClass("active");

    fireEvent.keyDown(input, { keyCode: 13 }); // enter -> launch index 1
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ name: "b" }));
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('highlights a result on mouse enter', () => {
    const options = [
      { name: "a", label: "Alpha", visible: true, actions: [{ type: "screen" }] },
      { name: "b", label: "Alfa", visible: true, actions: [{ type: "screen" }] }
    ];
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={jest.fn()} />);
    const input = openPanel();
    fireEvent.change(input, { target: { value: "a" } });

    fireEvent.mouseEnter(screen.getAllByRole("option")[1]);
    expect(screen.getAllByRole("option")[1]).toHaveClass("active");
  });

  it('closes the search on Escape and resets its state', () => {
    renderSearch();
    const input = openPanel();
    fireEvent.change(input, { target: { value: "invoice" } });

    fireEvent.keyDown(input, { keyCode: 27 });
    expect(screen.queryByRole("textbox")).toBeNull();

    // Reopen: query has been reset
    expect(openPanel()).toHaveValue("");
  });

  it('ignores a selected result without actions but still closes the search', () => {
    const options = [{ name: "no-actions", label: "Orphan", visible: true }];
    const onSelect = jest.fn();
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={onSelect} />);
    const input = openPanel();

    fireEvent.change(input, { target: { value: "orphan" } });
    fireEvent.click(screen.getAllByRole("option")[0]);

    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('does nothing on Enter when there are no results', () => {
    const { onSelect } = renderSearch();
    const input = openPanel();

    fireEvent.keyDown(input, { keyCode: 13 });

    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
  });

  it('closes the open search when clicking outside the panel', () => {
    renderSearch();
    openPanel();
    expect(screen.getByRole("textbox")).toBeInTheDocument();

    fireEvent.click(document.body);

    expect(screen.queryByRole("textbox")).toBeNull();
  });
});

describe('components/AweMenu.jsx menu search gating', () => {
  const searchOptions = [
    {
      name: "sales", label: "Sales", visible: true, restricted: false, separator: false,
      menuScreen: false, actions: [], options: [
        {
          name: "newInvoice", label: "New invoice", visible: true, restricted: false, separator: false,
          menuScreen: false, actions: [{ type: "screen" }], options: []
        }
      ]
    }
  ];

  const baseState = (settings) => ({
    settings,
    screen: { view: "report", size: { width: 100, height: 100 }, report: { option: "sites", title: "Sites" } },
    menu: { options: searchOptions },
    components: {
      menu: {
        address: { component: 'grid', view: 'report' },
        screen: { breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
        model: { values: [] },
        attributes: {}
      }
    }
  });

  beforeEach(() => {
    MenuRegistry.setOptions(searchOptions);
  });

  it('renders the search toggle when menuSearchEnabled is not disabled', () => {
    renderWithProviders(<AweMenu id="menu" style="vertical" />, { preloadedState: baseState(DEFAULT_SETTINGS) });

    expect(document.querySelector(".awe-menu-search-toggle")).toBeInTheDocument();
  });

  it('hides the search toggle when menuSearchEnabled is false', () => {
    const settings = { ...DEFAULT_SETTINGS, menuSearchEnabled: false };
    renderWithProviders(<AweMenu id="menu" style="vertical" />, { preloadedState: baseState(settings) });

    expect(document.querySelector(".awe-menu-search-toggle")).toBeNull();
  });

  it('launches the selected option through the menu search', () => {
    const { dispatchSpy } = renderWithProviders(
      <AweMenu id="menu" style="vertical" />,
      { preloadedState: baseState(DEFAULT_SETTINGS), spyDispatch: true }
    );

    fireEvent.click(document.querySelector(".awe-menu-search-toggle"));
    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: "invoice" } });
    fireEvent.keyDown(input, { keyCode: 13 });

    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({ type: "DELETE_STACK" }));
    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({ type: "ADD_ACTIONS_TOP" }));
    // Panel closes after launching
    expect(screen.queryByRole("textbox")).toBeNull();
  });
});
