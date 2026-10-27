import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweMenuSearch from "../../../src/components/AweMenuSearch";
import AweMenu from "../../../src/components/AweMenu";
import MenuRegistry from "../../../src/redux/registry/MenuRegistry";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";

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
    expect(toggle).toHaveAttribute("title", "MENU_SEARCH_PLACEHOLDER");
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('invites to find an option in the vertical toggle and in the input, as the AWE 5 prototype', () => {
    renderSearch();

    const toggle = screen.getByRole("button", { name: "MENU_SEARCH_PLACEHOLDER" });
    expect(toggle).toHaveTextContent("MENU_SEARCH_PLACEHOLDER");
    expect(toggle).not.toHaveTextContent("BUTTON_SEARCH");

    const input = openPanel();
    expect(input).toHaveAttribute("placeholder", "MENU_SEARCH_PLACEHOLDER");
    expect(input).toHaveAttribute("aria-label", "MENU_SEARCH_PLACEHOLDER");
  });

  it('keeps the search title on the icon toggle of the horizontal menu', () => {
    renderSearch({ menuType: "horizontal" });

    const toggle = screen.getByRole("button");
    expect(toggle).toHaveAttribute("title", "BUTTON_SEARCH");
    expect(toggle).toHaveAttribute("aria-label", "BUTTON_SEARCH");
    expect(toggle).toHaveTextContent("");
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

    fireEvent.keyDown(input, { key: "ArrowDown" }); // down -> index 1
    expect(screen.getAllByRole("option")[1]).toHaveClass("active");
    fireEvent.keyDown(input, { key: "ArrowDown" }); // down wraps -> index 0
    expect(screen.getAllByRole("option")[0]).toHaveClass("active");
    fireEvent.keyDown(input, { key: "ArrowUp" }); // up wraps -> index 1
    expect(screen.getAllByRole("option")[1]).toHaveClass("active");

    fireEvent.keyDown(input, { key: "Enter" }); // enter -> launch index 1
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ name: "b" }));
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('prevents the default of the keys it handles only when it handles them', () => {
    const options = [{ name: "a", label: "Alpha", visible: true, actions: [{ type: "screen" }] }];
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={jest.fn()} />);
    const input = openPanel();

    // Without results the arrows and Enter keep their default behavior
    expect(fireEvent.keyDown(input, { key: "ArrowDown" })).toBe(true);
    expect(fireEvent.keyDown(input, { key: "ArrowUp" })).toBe(true);
    expect(fireEvent.keyDown(input, { key: "Enter" })).toBe(true);
    expect(screen.getByRole("textbox")).toBeInTheDocument();

    // With results they are handled
    fireEvent.change(input, { target: { value: "a" } });
    expect(fireEvent.keyDown(input, { key: "ArrowDown" })).toBe(false);
    expect(fireEvent.keyDown(input, { key: "ArrowUp" })).toBe(false);

    // Other keys are left alone
    expect(fireEvent.keyDown(input, { key: "a" })).toBe(true);
  });

  it('handles the keys whatever modifiers are pressed', () => {
    const options = [{ name: "a", label: "Alpha", visible: true, actions: [{ type: "screen" }] }];
    const onSelect = jest.fn();
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={onSelect} />);
    const input = openPanel();
    fireEvent.change(input, { target: { value: "a" } });

    expect(fireEvent.keyDown(input, { key: "Enter", shiftKey: true })).toBe(false);
    expect(onSelect).toHaveBeenCalledTimes(1);

    openPanel();
    expect(fireEvent.keyDown(screen.getByRole("textbox"), { key: "Escape", ctrlKey: true })).toBe(false);
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it('ignores the keys of an input method composition, as the old keyCode switch did', () => {
    const options = [{ name: "a", label: "Alpha", visible: true, actions: [{ type: "screen" }] }];
    const onSelect = jest.fn();
    render(<AweMenuSearch options={options} menuType="vertical" isAllowed={isAllowed} onSelect={onSelect} />);
    const input = openPanel();
    fireEvent.change(input, { target: { value: "a" } });

    // Safari ends a composition with key "Enter" and keyCode 229; other browsers flag it with isComposing
    expect(fireEvent.keyDown(input, { key: "Enter", keyCode: 229 })).toBe(true);
    expect(fireEvent.keyDown(input, { key: "Enter", isComposing: true })).toBe(true);
    expect(fireEvent.keyDown(input, { key: "Escape", keyCode: 229 })).toBe(true);
    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
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

    fireEvent.keyDown(input, { key: "Escape" });
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

    fireEvent.keyDown(input, { key: "Enter" });

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
    fireEvent.keyDown(input, { key: "Enter" });

    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({ type: "DELETE_STACK" }));
    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({ type: "ADD_ACTIONS_TOP" }));
    // Panel closes after launching
    expect(screen.queryByRole("textbox")).toBeNull();
  });
});

const leafOption = (name, label, description) => ({
  name, label, ...(description ? { description } : {}), visible: true, restricted: false, separator: false,
  menuScreen: false, actions: [{ type: "screen" }], options: []
});

// Menu with a described option, a plain option and a launcher nested in another launcher (3 levels)
const describedMenuOptions = [
  {
    name: "launcher", label: "Launcher", description: "LAUNCHER_DESCRIPTION", visible: true, restricted: false,
    separator: false, menuScreen: true, actions: [{ type: "screen" }], options: [
      {
        name: "nested", label: "Nested", description: "NESTED_DESCRIPTION", visible: true, restricted: false,
        separator: false, menuScreen: true, actions: [{ type: "screen" }], options: [
          leafOption("deep", "Deep", "DEEP_DESCRIPTION")
        ]
      }
    ]
  },
  leafOption("described", "Described", "DESCRIBED_DESCRIPTION"),
  leafOption("plain", "Plain")
];

describe('components/AweMenu.jsx option descriptions', () => {

  // The view registry is global: leave it as it was after every test
  afterEach(() => ViewRegistry.setView("report", { loading: true }));

  const renderMenu = (style) => {
    MenuRegistry.setOptions(describedMenuOptions);
    renderWithProviders(<AweMenu id="menu" style={style} />, {
      preloadedState: {
        settings: DEFAULT_SETTINGS,
        screen: { view: "report", size: { width: 100, height: 100 }, report: { option: "plain", title: "Plain" } },
        menu: { options: describedMenuOptions },
        components: {
          menu: {
            address: { component: 'grid', view: 'report' },
            screen: { breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
            model: { values: [] },
            attributes: {}
          }
        }
      }
    });
  };

  it.each(["vertical", "horizontal"])('shows the description of an option as the hint of its link (%s menu)', (style) => {
    renderMenu(style);

    expect(document.querySelector("a[name='described']")).toHaveAttribute("title", "DESCRIBED_DESCRIPTION");
  });

  it('shows the description as the hint of a launcher option', () => {
    renderMenu("vertical");

    expect(document.querySelector("a[name='launcher']")).toHaveAttribute("title", "LAUNCHER_DESCRIPTION");
  });

  it('generates the breadcrumbs of an option inside nested launchers (3 levels)', () => {
    ViewRegistry.setView("report", { option: "deep", title: "Deep" });
    renderMenu("vertical");

    expect(MenuRegistry.getBreadcrumbs().items.map(item => item.name)).toEqual(["launcher", "nested", "deep"]);
  });

  it('adds no hint to an option without description', () => {
    renderMenu("vertical");

    expect(document.querySelector("a[name='plain']")).not.toHaveAttribute("title");
  });
});

// Menu with a section, a parent, a nested parent and the current option (3 levels), all open
const trailMenuOptions = [
  { name: "general", label: "General", separator: true, visible: true, restricted: false, actions: [], options: [] },
  {
    id: "tools", name: "tools", label: "Tools", expanded: true, visible: true, restricted: false, separator: false,
    menuScreen: false, actions: [], options: [
      {
        id: "sites", name: "sites", label: "Sites", expanded: true, visible: true, restricted: false, separator: false,
        menuScreen: false, actions: [], options: [leafOption("site-list", "Site list"), leafOption("site-map", "Site map")]
      },
      leafOption("users", "Users")
    ]
  },
  leafOption("plain", "Plain")
];

describe('components/AweMenu.jsx active trail (#885)', () => {

  // The view registry is global: leave it as it was after every test
  afterEach(() => ViewRegistry.setView("report", { loading: true }));

  const renderMenu = (option) => {
    ViewRegistry.setView("report", { option, title: option });
    MenuRegistry.setOptions(trailMenuOptions);
    renderWithProviders(<AweMenu id="menu" style="vertical" />, {
      preloadedState: {
        settings: DEFAULT_SETTINGS,
        screen: { view: "report", size: { width: 100, height: 100 }, report: { option, title: option } },
        menu: { options: trailMenuOptions },
        components: {
          menu: {
            address: { component: 'grid', view: 'report' },
            screen: { breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
            model: { values: [] },
            attributes: {}
          }
        }
      }
    });
  };
  const option = (name) => document.querySelector(`[data-testid='menu-option'][option-name='${name}']`);

  it('marks every ancestor of the current option as its trail and the option itself as active', () => {
    renderMenu("site-list");

    expect(option("tools")).toHaveClass("p-menuitem-active-path");
    expect(option("tools")).not.toHaveClass("p-menuitem-active");
    expect(option("sites")).toHaveClass("p-menuitem-active-path");
    expect(option("site-list")).toHaveClass("p-menuitem-active");
    expect(option("site-list")).not.toHaveClass("p-menuitem-active-path");
    // The active hook of the browser tests stays on the current option only
    expect(option("site-list")).toHaveAttribute("data-active", "true");
    expect(option("sites")).not.toHaveAttribute("data-active", "true");
  });

  it('leaves the options outside the trail unmarked', () => {
    renderMenu("site-list");

    ["site-map", "users", "plain"].forEach(name => {
      expect(option(name)).not.toHaveClass("p-menuitem-active-path");
      expect(option(name)).not.toHaveClass("p-menuitem-active");
    });
  });

  it('marks no trail for a first level option', () => {
    renderMenu("plain");

    expect(option("plain")).toHaveClass("p-menuitem-active");
    expect(document.querySelectorAll(".p-menuitem-active-path")).toHaveLength(0);
  });
});
