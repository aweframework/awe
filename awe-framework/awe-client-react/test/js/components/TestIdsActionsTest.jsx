import React from "react";
import { act, cleanup, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweButton from "../../../src/components/AweButton";
import AweInfoButton from "../../../src/components/AweInfoButton";
import AweInfoDropdown from "../../../src/components/AweInfoDropdown";
import AweDialog from "../../../src/components/AweDialog";
import AweGrid from "../../../src/components/AweGrid";
import { Columns } from "../../../src/utilities/structure";

const hook = (testId) => `[data-testid='${testId}']`;

function component(id, attributes = {}, extra = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    actions: { running: false },
    components: {
      [id]: {
        address: { component: id, view: "report" },
        model: { values: [] },
        attributes: { label: "Label", visible: true, ...attributes },
        actions: [],
        ...extra
      }
    }
  };
}

describe("awe-react-client/test/js/components/TestIdsActionsTest.jsx", () => {
  afterEach(cleanup);

  it("exposes a button with its id", () => {
    const { container } = renderWithProviders(<AweButton id="ButAdd" />, { preloadedState: component("ButAdd") });

    const button = container.querySelector(hook("button"));
    expect(button.tagName).toBe("BUTTON");
    expect(button.id).toBe("ButAdd");
  });

  it("exposes the button of a grid cell", () => {
    const { container } = renderWithProviders(Columns({
      component: "button",
      model: { values: [] },
      numberFormat: {},
      address: { component: "ButRow", view: "report", column: "column", row: "row" },
      t: jest.fn(),
      settings: {}
    }, { value: "test" }, {}, true));

    expect(container.querySelector(hook("button")).id).toBe("ButRow");
  });

  it("exposes an info button", () => {
    const { container } = renderWithProviders(<AweInfoButton id="Inf" />, { preloadedState: component("Inf") });

    expect(container.querySelector(hook("info-button")).id).toBe("Inf");
  });

  describe("info dropdown", () => {
    it("exposes the dropdown and, once opened, its menu with its owner", async () => {
      const { container } = renderWithProviders(
        <AweInfoDropdown id="Dro" elementList={[{ elementType: "Tag", type: "div", label: "inside", elementList: [] }]} />,
        { preloadedState: component("Dro") });

      const dropdown = container.querySelector(hook("info-dropdown"));
      expect(dropdown.id).toBe("Dro");
      await act(async () => {
        fireEvent.click(dropdown);
      });

      const menu = document.querySelector(hook("info-dropdown-menu"));
      expect(menu.getAttribute("data-testid-owner")).toBe("Dro");
    });
  });

  describe("dialog", () => {
    const state = (isShowing) => component("Dlg", { isShowing }, { elementList: [] });

    it("exposes the dialog with its owner and its close button", () => {
      renderWithProviders(<AweDialog id="Dlg" elementList={[]} />, { preloadedState: state(true) });

      const dialog = document.querySelector(hook("dialog"));
      expect(dialog.getAttribute("data-testid-owner")).toBe("Dlg");
      expect(dialog.querySelector(hook("dialog-close")).tagName).toBe("BUTTON");
    });

    it("does not render the dialog while it is hidden", () => {
      renderWithProviders(<AweDialog id="Dlg" elementList={[]} />, { preloadedState: state(false) });

      expect(document.querySelector(hook("dialog"))).toBeNull();
    });
  });

  describe("context menu", () => {
    it("exposes the menu, its options and its links", () => {
      const preloadedState = {
        settings: DEFAULT_SETTINGS,
        screen: { size: { width: 100, height: 100 } },
        components: {
          grid: {
            address: { component: "grid", view: "report" },
            model: { values: [{ id: 1, test: "test" }] },
            attributes: {
              loadAll: true,
              max: 30,
              columnModel: [{ name: "test", label: "test", hidden: false }],
              headerModel: [],
              buttonModel: [],
              contextMenu: [{ id: "OptAdd" }, { id: "OptDel" }]
            },
            specificAttributes: { sort: [] }
          },
          OptAdd: {
            address: { component: "OptAdd", view: "report" },
            model: { values: [] },
            attributes: { label: "Add", visible: true }
          },
          OptDel: {
            address: { component: "OptDel", view: "report" },
            model: { values: [] },
            attributes: { label: "Delete", visible: true, disabled: true }
          }
        }
      };
      renderWithProviders(<AweGrid id="grid" />, { preloadedState });

      fireEvent.contextMenu(document.querySelector("[data-testid='grid-cell']"));

      const menu = document.querySelector(hook("context-menu"));
      expect(menu).not.toBeNull();
      const options = Array.from(menu.querySelectorAll(hook("context-menu-option")));
      expect(options.length).toBe(2);
      const links = Array.from(menu.querySelectorAll(`${hook("context-menu-option")} ${hook("context-menu-link")}`));
      expect(links.map(link => link.getAttribute("option-id"))).toEqual(["OptAdd", "OptDel"]);
      expect(links.map(link => link.textContent)).toEqual(["Add", "Delete"]);
      expect(links.map(link => link.getAttribute("data-disabled"))).toEqual(["false", "true"]);
    });
  });
});
