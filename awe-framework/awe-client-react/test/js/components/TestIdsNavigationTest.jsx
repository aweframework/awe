import React from "react";
import { act, cleanup, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweTabs from "../../../src/components/AweTabs";
import AweSteps from "../../../src/components/AweSteps";
import AweMenu from "../../../src/components/AweMenu";
import AweAvatar from "../../../src/components/AweAvatar";
import MenuRegistry from "../../../src/redux/registry/MenuRegistry";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";

const hook = (testId) => `[data-testid='${testId}']`;

function panelable(id, values, attributes = {}, extra = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    components: {
      [id]: {
        address: { component: id, view: "report" },
        model: { values },
        elementList: [],
        attributes,
        specificAttributes: { sort: [] },
        ...extra
      }
    }
  };
}

const tabs = [
  { label: "First", value: "tab1" },
  { label: "Second", value: "tab2", selected: true },
  { label: "Third", value: "tab3" }
];

const option = (name, label, extra = {}) => ({
  name, label, visible: true, restricted: false, options: [], actions: [], ...extra
});

describe("awe-react-client/test/js/components/TestIdsNavigationTest.jsx", () => {
  afterEach(() => {
    cleanup();
    MenuRegistry.clear();
    ViewRegistry.clearView("report");
  });

  describe("tabs", () => {
    it("exposes the tab list, the tabs and their labels", () => {
      const { container } = renderWithProviders(<AweTabs id="Tab" />, { preloadedState: panelable("Tab", tabs) });

      const root = container.querySelector("[criterion-id='Tab']");
      expect(root).not.toBeNull();
      expect(root.querySelector(hook("tab-list")).tagName).toBe("UL");
      const items = Array.from(root.querySelectorAll(hook("tab")));
      expect(items.map(tab => tab.getAttribute("option-id"))).toEqual(["tab1", "tab2", "tab3"]);
      expect(items.map(tab => tab.getAttribute("data-active"))).toEqual(["false", "true", "false"]);
      expect(items.map(tab => tab.querySelector(hook("tab-label")).textContent)).toEqual(["First", "Second", "Third"]);
      expect(items[0].querySelector(hook("tab-link")).tagName).toBe("BUTTON");
    });

    it("reports a disabled tab list", () => {
      const { container } = renderWithProviders(<AweTabs id="Tab" />,
        { preloadedState: panelable("Tab", tabs, { disabled: true }) });

      expect(container.querySelector(hook("tab-list")).getAttribute("data-disabled")).toBe("true");
    });
  });

  describe("wizard", () => {
    it("exposes the steps with their state and number", () => {
      const { container } = renderWithProviders(<AweSteps id="Wiz" />, { preloadedState: panelable("Wiz", tabs) });

      const root = container.querySelector("[criterion-id='Wiz']");
      const steps = Array.from(root.querySelectorAll(hook("wizard-step")));
      expect(steps.map(step => step.getAttribute("option-id"))).toEqual(["tab1", "tab2", "tab3"]);
      expect(steps.map(step => step.getAttribute("data-active"))).toEqual(["false", "true", "false"]);
      expect(steps.map(step => step.getAttribute("data-completed"))).toEqual(["true", "false", "false"]);
      expect(steps[1].querySelector(hook("wizard-step-number")).textContent).toBe("2");
    });
  });

  describe("application menu", () => {
    const options = [
      option("home", "Home", { actions: [{ type: "screen" }] }),
      option("tools", "Tools", {
        options: [
          option("sites", "Sites", { options: [option("new-site", "New site", { actions: [{ type: "screen" }] })] }),
          option("users", "Users", { actions: [{ type: "screen" }] })
        ]
      })
    ];

    function renderMenu(style, current = "users") {
      MenuRegistry.setOptions(options);
      ViewRegistry.setView("report", { option: current, title: "Report" });
      return renderWithProviders(<AweMenu id="menu" style={style} />, {
        preloadedState: { settings: { ...DEFAULT_SETTINGS, menuSearchEnabled: false } }
      });
    }

    it("exposes the options and links of the vertical menu", () => {
      const { container } = renderMenu("vertical");

      const menu = container.querySelector(hook("menu"));
      expect(menu).not.toBeNull();
      const names = Array.from(menu.querySelectorAll(`${hook("menu-link")}[name]`)).map(link => link.getAttribute("name"));
      expect(names).toEqual(expect.arrayContaining(["home", "tools"]));
      const home = menu.querySelector(`${hook("menu-link")}[name='home']`);
      expect(home.textContent).toContain("Home");
      expect(menu.querySelector(`${hook("menu-option")}[option-name='home']`)).not.toBeNull();
    });

    it("exposes the open branches, the nested options and the active option", () => {
      const { container } = renderMenu("vertical");
      fireEvent.click(container.querySelector(`${hook("menu-link")}[name='tools']`));

      const tools = container.querySelector(`${hook("menu-option")}[option-name='tools']`);
      expect(tools.getAttribute("data-open")).toBe("true");
      const users = container.querySelector(`${hook("menu-option")}[option-name='users']`);
      expect(users).not.toBeNull();
      expect(users.getAttribute("data-active")).toBe("true");
      expect(container.querySelector(`${hook("menu-option")}[option-name='sites']`).getAttribute("data-active")).toBe("false");
      expect(container.querySelector(hook("menu-submenu"))).not.toBeNull();
    });

    it("exposes the horizontal menu", () => {
      const { container } = renderMenu("horizontal");

      expect(container.querySelector(hook("menu"))).not.toBeNull();
      expect(container.querySelector(`${hook("menu-option")}[option-name='home'] ${hook("menu-link")}[name='home']`)).not.toBeNull();
    });
  });

  describe("avatar", () => {
    const state = (attributes = {}) => panelable("Usr", [{ label: "Manager (test)", value: "test", selected: true }], attributes);

    it("exposes the avatar and the user name", () => {
      const { container } = renderWithProviders(<AweAvatar id="Usr" elementList={[]} />, { preloadedState: state() });

      const avatar = container.querySelector(hook("avatar"));
      expect(avatar.id).toBe("Usr");
      expect(avatar.getAttribute("title")).toBe("Manager (test)");
      expect(container.querySelector(hook("avatar-name")).textContent).toBe("Manager (test)");
    });

    it("exposes the dropdown of the avatar with its owner", async () => {
      const { container } = renderWithProviders(
        <AweAvatar id="Usr" elementList={[{ elementType: "Tag", type: "div", label: "inside", elementList: [] }]} />,
        { preloadedState: state() });

      await act(async () => {
        fireEvent.click(container.querySelector(".avatar-component"));
      });

      const menu = document.querySelector(hook("info-dropdown-menu"));
      expect(menu).not.toBeNull();
      expect(menu.getAttribute("data-testid-owner")).toBe("Usr");
    });
  });
});
