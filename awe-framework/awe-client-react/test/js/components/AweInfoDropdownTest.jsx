import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInfoDropdown from "../../../src/components/AweInfoDropdown";
import {fireEvent, waitFor} from "@testing-library/react";
import {PrimeReactProvider} from "primereact/api";

describe('awe-react-client/test/js/criteria/AweInfoDropdownTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      infodropdown: {
        address: {component: 'infodropdown', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          help: "help",
          helpImage: "helpImage"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Info dropdown component', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown"></AweInfoDropdown>, {preloadedState});

    // check
    expect(document.querySelector("button#infodropdown")).not.toBeNull();
  });

  it('is not disabled while actions run, as in the AngularJS engine', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown"></AweInfoDropdown>, {
      preloadedState: {...preloadedState, actions: {running: true, sync: [[]], async: []}}
    });

    expect(document.querySelector("button#infodropdown").hasAttribute("disabled")).toBe(false);
  });

  // The notification bell (#768): an info with a filter action, children and a unit set by the server
  const bellState = (unit) => ({
    ...preloadedState,
    components: {
      infodropdown: {
        ...preloadedState.components.infodropdown,
        attributes: {icon: "bell", visible: true, unit},
        actions: [{type: "filter", target: "notification-bulletin"}]
      }
    }
  });
  const children = [{elementType: "Tag", id: "bell-child", type: "div", label: "Bell child", style: "bell-child"}];

  it('opens its dropdown when clicked even if it has actions', async () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>,
      {preloadedState: bellState("3")});

    fireEvent.click(document.querySelector("button#infodropdown"));

    await waitFor(() => expect(document.querySelector(".info-dropdown")).not.toBeNull());
  });

  it('renders the unit as a PrimeReact badge over the icon trigger', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>,
      {preloadedState: bellState("3")});

    const badge = document.querySelector(".p-overlay-badge .p-badge");
    expect(badge.textContent).toBe("3");
    // Rendered by the Badge component, which registers the style that positions it over the trigger
    expect(badge.getAttribute("data-pc-name")).toBe("badge");
  });

  it('puts the dropdown-style of the info on its panel', async () => {
    const state = bellState("3");
    state.components.infodropdown.attributes.dropdownStyle = "dropdown-xl widget-notifications";
    renderWithProviders(<AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>,
      {preloadedState: state});

    fireEvent.click(document.querySelector("button#infodropdown"));

    await waitFor(() => expect(document.querySelector(".info-dropdown.dropdown-xl.widget-notifications")).not.toBeNull());
  });

  it('mounts its children, hidden, before it is opened so they load (the unread count of the bell)', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>,
      {preloadedState: bellState("3")});

    const child = document.querySelector(".bell-child");
    expect(child).not.toBeNull();
    expect(child.closest("[hidden]")).not.toBeNull();
    expect(document.querySelector(".info-dropdown")).toBeNull();
  });

  it('shows its children once, in the dropdown, without remounting them when it is opened', async () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>,
      {preloadedState: bellState("3")});
    const child = document.querySelector(".bell-child");

    fireEvent.click(document.querySelector("button#infodropdown"));

    await waitFor(() => expect(document.querySelector(".info-dropdown .bell-child")).not.toBeNull());
    expect(document.querySelectorAll(".bell-child")).toHaveLength(1);
    expect(document.querySelector("[hidden] .bell-child")).toBeNull();
    // The same node: a remount would create another one and launch the autoload of its children again
    expect(document.querySelector(".bell-child")).toBe(child);
  });

  it('keeps its children mounted, hidden and once, when the dropdown is dismissed', async () => {
    // Without transitions the panel unmounts as soon as it is dismissed
    renderWithProviders(<PrimeReactProvider value={{cssTransition: false}}>
      <AweInfoDropdown id="infodropdown" elementList={children}></AweInfoDropdown>
    </PrimeReactProvider>, {preloadedState: bellState("3")});
    const child = document.querySelector(".bell-child");
    fireEvent.click(document.querySelector("button#infodropdown"));
    await waitFor(() => expect(document.querySelector(".info-dropdown .bell-child")).not.toBeNull());

    // Dismiss it by clicking outside of it
    fireEvent.mouseDown(document.body);
    fireEvent.click(document.body);

    await waitFor(() => expect(document.querySelector(".info-dropdown")).toBeNull());
    expect(document.querySelectorAll(".bell-child")).toHaveLength(1);
    expect(document.querySelector("[hidden] .bell-child")).toBe(child);
    expect(child.isConnected).toBe(true);
  });

  it('shows the unit as a PrimeReact info badge over the button', () => {
    const state = {
      ...preloadedState,
      components: {
        infodropdown: {
          ...preloadedState.components.infodropdown,
          attributes: {...preloadedState.components.infodropdown.attributes, unit: "3"}
        }
      }
    };

    renderWithProviders(<AweInfoDropdown id="infodropdown"/>, {preloadedState: state});

    const badge = document.querySelector('[data-pc-name="badge"]');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toBe("3");
    expect(badge.className).toContain("p-badge-info");
  });
});
