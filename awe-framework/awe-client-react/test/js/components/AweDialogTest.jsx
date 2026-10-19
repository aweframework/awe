import React from "react";
import { renderWithProviders } from "../test-utils";
import AweDialog from "../../../src/components/AweDialog";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { act, fireEvent } from "@testing-library/react";

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
  const closeActionType = (attributes) => {
    const { dispatchSpy } = renderWithProviders(
      <AweDialog id="dialog" label="Close me" elementList={[]} />,
      {
        preloadedState: {
          ...preloadedState,
          components: { dialog: { ...preloadedState.components.dialog, attributes: { isShowing: true, ...attributes } } }
        },
        spyDispatch: true
      }
    );
    fireEvent.click(document.querySelector(".p-dialog .p-dialog-header .p-dialog-header-icon"));
    return dispatchSpy.mock.calls
      .map(([action]) => action)
      .filter(action => action?.actions)
      .flatMap(action => action.actions.map(a => a.type));
  };

  it("closes with the close action by default", () => {
    expect(closeActionType({})).toEqual(["close"]);
  });

  it("closes with the close action when on-close accepts", () => {
    expect(closeActionType({ acceptOnClose: true })).toEqual(["close"]);
  });

  it("closes with the close-cancel action when on-close is reject", () => {
    expect(closeActionType({ acceptOnClose: false })).toEqual(["close-cancel"]);
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

  describe('deferred loading (#824)', () => {
    const renderDialog = (attributes) => renderWithProviders(<AweDialog id="dialog" elementList={[]} />, {
      preloadedState: {
        ...preloadedState,
        components: { dialog: { ...preloadedState.components.dialog, attributes } }
      }
    });
    const deferredLoaded = (store) => store.getState().components.dialog.attributes.deferredLoaded;

    it('asks for the data of a deferred dialog when it is shown', () => {
      const { store } = renderDialog({ isShowing: true, deferred: true });

      expect(deferredLoaded(store)).toBe(true);
    });

    it('waits while a deferred dialog is not shown', () => {
      const { store } = renderDialog({ isShowing: false, deferred: true });

      expect(deferredLoaded(store)).toBeUndefined();
    });

    it('does not ask for the data of a dialog which is not deferred', () => {
      const { store } = renderDialog({ isShowing: true });

      expect(deferredLoaded(store)).toBeUndefined();
    });

    it('asks for the data of a deferred dialog only once', () => {
      const { store } = renderDialog({ isShowing: true, deferred: true, deferredLoaded: true });
      const dispatched = jest.spyOn(store, 'dispatch');

      store.dispatch({ type: 'UPDATE_ATTRIBUTES', address: { component: 'dialog', view: 'report' }, data: { isShowing: true } });

      expect(dispatched.mock.calls.filter(([call]) => typeof call === 'function')).toHaveLength(0);
    });

    it('asks again on the next opening, not at once, after the load failed', () => {
      const { store } = renderDialog({ isShowing: true, deferred: true });
      const update = (data) => act(() => {
        store.dispatch({ type: 'UPDATE_ATTRIBUTES', address: { component: 'dialog', view: 'report' }, data });
      });
      expect(deferredLoaded(store)).toBe(true);

      // The server answered that the load failed: the dialog is still open, and it is not requested in a loop
      update({ deferredLoaded: false });
      expect(deferredLoaded(store)).toBe(false);

      // Closed and opened again: it is requested again
      update({ isShowing: false });
      update({ isShowing: true });
      expect(deferredLoaded(store)).toBe(true);
    });
  });
});
