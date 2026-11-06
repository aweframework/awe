import React from 'react';
import { act, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweMenu from "../../../src/components/AweMenu";
import MenuRegistry from "../../../src/redux/registry/MenuRegistry";

const leaf = (name, description) => ({
  name, label: name.toUpperCase(), ...(description ? { description } : {}), visible: true, restricted: false,
  separator: false, menuScreen: false, actions: [{ type: "screen" }], options: []
});
const group = (name, description, options) => ({
  name, label: name.toUpperCase(), ...(description ? { description } : {}), visible: true, restricted: false,
  separator: false, menuScreen: false, actions: [], options
});

// Tools describes itself, Settings does not; one option of each has no description
const menuOptions = [
  group("tools", "TOOLS_DESCRIPTION", [leaf("sites", "SITES_DESCRIPTION"), leaf("users", "USERS_DESCRIPTION"), leaf("plain")]),
  group("settings", null, [leaf("theme", "THEME_DESCRIPTION"), leaf("language")]),
  group("empty", null, [leaf("first"), leaf("second")])
];

describe('components/AweMenu.jsx option description in the flyout of the rail (#924)', () => {
  let matchMedia;

  beforeEach(() => {
    matchMedia = window.matchMedia;
    window.matchMedia = jest.fn(query => ({ matches: query === "(min-width: 1025px)", media: query }));
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    window.matchMedia = matchMedia;
    MenuRegistry.setOptions([]);
  });

  const renderRail = (settings = {}, collapsed = true) => {
    MenuRegistry.setOptions(menuOptions);
    renderWithProviders(
      <div className={collapsed ? "layout-container layout-static-inactive" : "layout-container"}>
        <AweMenu id="menu" style="vertical" />
      </div>, {
        preloadedState: {
          settings: { ...DEFAULT_SETTINGS, menuSearchEnabled: false, ...settings },
          screen: { size: { width: 100, height: 100 } },
          components: { menu: { address: { component: 'menu', view: 'base' }, model: { values: [] }, attributes: {} } }
        }
      });
  };
  const panel = name => document.querySelector(`.p-panelmenu-panel.${name}`);
  const header = name => panel(name).querySelector(".p-panelmenu-header");
  const link = name => document.querySelector(`a[name='${name}']`);
  const area = () => document.querySelector(".awe-menu-description");

  it('keeps the description as the hint of the option and shows no area by default (tooltip)', () => {
    renderRail();
    fireEvent.mouseOver(header("tools"));

    expect(link("sites")).toHaveAttribute("title", "SITES_DESCRIPTION");
    expect(area()).toBeNull();
  });

  it('keeps the hint of the options in the expanded menu in panel mode', () => {
    renderRail({ menuDescription: "panel" }, false);
    fireEvent.mouseOver(header("tools"));
    fireEvent.click(header("tools"));

    expect(link("sites")).toHaveAttribute("title", "SITES_DESCRIPTION");
    expect(area()).toBeNull();
  });

  it('describes the option under the pointer or with the focus, in an area linked to its link (panel)', () => {
    renderRail({ menuDescription: "panel" });
    fireEvent.mouseOver(header("tools"));
    // The option of the flyout describes itself until the pointer goes to one of its options
    expect(area()).toHaveTextContent("TOOLS_DESCRIPTION");

    fireEvent.mouseOver(link("sites"));
    expect(area()).toHaveTextContent("SITES_DESCRIPTION");
    expect(link("sites")).toHaveAttribute("aria-describedby", area().id);
    // The area replaces the hint: it is not announced twice
    expect(link("sites")).not.toHaveAttribute("title");

    fireEvent.focus(link("users"));
    expect(area()).toHaveTextContent("USERS_DESCRIPTION");
    expect(link("sites")).not.toHaveAttribute("aria-describedby");
    expect(link("users")).toHaveAttribute("aria-describedby", area().id);

    // An option without description falls back to the description of the option of the flyout
    fireEvent.mouseOver(link("plain"));
    expect(area()).toHaveTextContent("TOOLS_DESCRIPTION");

    // The area goes with the flyout
    fireEvent.mouseLeave(document.querySelector(".awe-menu-container-vertical"));
    act(() => jest.advanceTimersByTime(300));
    expect(area()).toBeNull();
  });

  it('renders no area when neither the option nor its flyout have a description (panel)', () => {
    renderRail({ menuDescription: "panel" });
    fireEvent.mouseOver(header("empty"));
    fireEvent.mouseOver(link("first"));

    expect(area()).toBeNull();
    expect(link("first")).not.toHaveAttribute("aria-describedby");
  });

  it('starts the flyout at the top of the menu in full-height placement', () => {
    renderRail({ menuFlyoutPlacement: "full-height" });
    const container = document.querySelector(".awe-menu-container-vertical");
    jest.spyOn(container, "getBoundingClientRect").mockReturnValue({ top: 56, bottom: 700, left: 0, right: 68, height: 644 });
    jest.spyOn(header("settings"), "getBoundingClientRect").mockReturnValue({ top: 120, bottom: 156, left: 0, right: 68, height: 36 });

    fireEvent.mouseOver(header("settings"));

    expect(container.style.getPropertyValue("--_flyout-top")).toBe("56px");
  });
});
