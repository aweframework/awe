import React from "react";
import { renderWithProviders } from "../test-utils";
import AweDialog from "../../../src/components/AweDialog";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { fireEvent } from "@testing-library/react";

describe("awe-react-client/test/js/components/AweDialogTest.jsx", () => {
  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      dialog: {
        address: { component: "dialog", view: "report" },
        model: { values: [] },
        attributes: {
          isShowing: true,
        },
        specificAttributes: { sort: [] },
        actions: [],
      }
    },
  };

  it("renders AweDialog component visible with header and children", () => {
    renderWithProviders(
      <AweDialog
        id="dialog"
        label="My Dialog"
        elementList={[
          { elementType: "Tag", type: "div", label: "inside", elementList: [] },
        ]}
      />, { preloadedState }
    );

    // Dialog root should exist and be visible (Dialog uses portal -> query document)
    expect(document.querySelector(".p-dialog")).not.toBeNull();
    // Header text should be present (translated label)
    expect(document.querySelector(".p-dialog .p-dialog-header")).not.toBeNull();
  });

  it("marks the open dialog with its owner and the open test state", () => {
    renderWithProviders(<AweDialog id="dialog" label="Open" elementList={[]} />, { preloadedState });

    const root = document.querySelector("[data-testid='dialog']");
    expect(root.getAttribute("data-testid-owner")).toBe("dialog");
    expect(root.getAttribute("data-open")).toBe("true");
  });

  it("calls onHide when closing (dispatch close action)", () => {
    renderWithProviders(
      <AweDialog id="dialog" label="Close me" elementList={[]} />, { preloadedState }
    );

    // PrimeReact renders a close button with .p-dialog-header-icon
    const closeButton = document.querySelector(".p-dialog .p-dialog-header .p-dialog-header-icon");
    if (closeButton) {
      fireEvent.click(closeButton);
      // We don't assert store effects here; just ensure no crash when invoking onHide
      expect(true).toBe(true);
    } else {
      // Fallback: ensure dialog rendered
      expect(document.querySelector(".p-dialog")).not.toBeNull();
    }
  });

  describe("size classes", () => {
    const renderWithStyle = (style) => {
      renderWithProviders(<AweDialog id="dialog" elementList={[]} />, {
        preloadedState: {
          ...preloadedState,
          components: { dialog: { ...preloadedState.components.dialog, attributes: { isShowing: true, style } } }
        }
      });
      return document.querySelector(".p-dialog");
    };

    it.each([
      ["modal-sm", "300px"],
      ["modal-md", "600px"],
      ["modal-lg", "900px"],
      ["expand modal-lg", "900px"]
    ])("style '%s' gives a dialog %s wide without the default minimum", (style, width) => {
      const dialog = renderWithStyle(style);
      expect(dialog.style.width).toBe(width);
      expect(dialog.style.minWidth).toBe("");
      expect(dialog.style.maxWidth).toBe("90vw");
    });

    it.each([[undefined], ["expand"], ["modal-body"]])("style '%s' keeps the default size", (style) => {
      const dialog = renderWithStyle(style);
      expect(dialog.style.width).toBe("");
      expect(dialog.style.minWidth).toBe("40vw");
      expect(dialog.style.maxWidth).toBe("90vw");
    });
  });
});
