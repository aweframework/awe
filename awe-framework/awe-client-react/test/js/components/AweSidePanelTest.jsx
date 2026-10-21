import React from "react";
import { renderWithProviders } from "../test-utils";
import { ComponentList } from "../../../src/components/AweComponents";
import AweSidePanel from "../../../src/components/AweSidePanel";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { act, fireEvent } from "@testing-library/react";

describe("awe-react-client/test/js/components/AweSidePanelTest.jsx", () => {
  const address = { component: "panel", view: "report" };
  const stateWith = (attributes) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      panel: {
        address,
        model: { values: [] },
        attributes: { isShowing: true, ...attributes },
        specificAttributes: {},
        actions: []
      }
    }
  });
  const renderPanel = (attributes = {}, options = {}) => renderWithProviders(
    <AweSidePanel id="panel" elementList={[{ elementType: "Tag", type: "div", label: "inside", elementList: [] }]}/>,
    { preloadedState: stateWith(attributes), ...options }
  );
  const panel = () => document.querySelector("[data-testid='side-panel']");
  const closeTypes = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action?.actions)
    .flatMap(action => action.actions.map(a => a.type));

  afterEach(() => {
    document.querySelectorAll(".opener").forEach(opener => opener.remove());
  });

  it("is the renderer of the SidePanel element", () => {
    expect(ComponentList.SidePanel).toBe(AweSidePanel);
  });

  it("shows the header, the children and the open test state", () => {
    renderPanel({ label: "Details" });

    expect(panel()).not.toBeNull();
    expect(panel().getAttribute("data-testid-owner")).toBe("panel");
    expect(panel().getAttribute("data-open")).toBe("true");
    expect(panel().querySelector(".side-panel-title").textContent).toBe("Details");
    expect(panel().textContent).toContain("inside");
  });

  it("is anchored to the right edge by default", () => {
    renderPanel();

    expect(document.querySelector(".p-sidebar-right")).not.toBeNull();
  });

  it("is anchored to the left edge with position left", () => {
    renderPanel({ position: "left" });

    expect(document.querySelector(".p-sidebar-left")).not.toBeNull();
  });

  it("is as wide as the screen up to its maximum width", () => {
    renderPanel({ maxWidth: "30rem" });

    expect(panel().style.width).toBe("100%");
    expect(panel().style.maxWidth).toBe("30rem");
  });

  it("keeps a default maximum width", () => {
    renderPanel();

    expect(panel().style.maxWidth).toBe("40rem");
  });

  it("does not render while it is hidden", () => {
    renderPanel({ isShowing: false });

    expect(panel()).toBeNull();
  });

  it("closes with the close action from its close button", () => {
    const { dispatchSpy } = renderPanel({}, { spyDispatch: true });

    fireEvent.click(document.querySelector("[data-testid='side-panel-close']"));

    expect(closeTypes(dispatchSpy)).toEqual(["close"]);
  });

  it("closes with the close-cancel action when on-close is reject", () => {
    const { dispatchSpy } = renderPanel({ acceptOnClose: false }, { spyDispatch: true });

    fireEvent.click(document.querySelector("[data-testid='side-panel-close']"));

    expect(closeTypes(dispatchSpy)).toEqual(["close-cancel"]);
  });

  it("closes with Esc", () => {
    const { dispatchSpy } = renderPanel({}, { spyDispatch: true });

    fireEvent.keyDown(panel(), { key: "Escape", code: "Escape" });

    expect(closeTypes(dispatchSpy)).toEqual(["close"]);
  });

  it("closes with a click outside the panel", () => {
    const { dispatchSpy } = renderPanel({}, { spyDispatch: true });

    // PrimeReact closes the panel on the mouse down on its mask
    fireEvent.mouseDown(document.querySelector(".p-sidebar-mask"));

    expect(closeTypes(dispatchSpy)).toEqual(["close"]);
  });

  it("asks for the data of a deferred panel when it is shown", () => {
    const { store } = renderPanel({ deferred: true });

    expect(store.getState().components.panel.attributes.deferredLoaded).toBe(true);
  });

  describe("focus", () => {
    // The panel takes the focus once its opening transition ends
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    const renderWithOpener = (attributes) => {
      const opener = document.createElement("button");
      opener.textContent = "open";
      opener.className = "opener";
      document.body.appendChild(opener);
      opener.focus();
      const result = renderWithProviders(
        <AweSidePanel id="panel" elementList={[]}/>,
        { preloadedState: stateWith({ isShowing: false, ...attributes }) }
      );
      const show = (isShowing) => {
        act(() => {
          result.store.dispatch({ type: "UPDATE_ATTRIBUTES", address, data: { isShowing } });
        });
        act(() => jest.runAllTimers());
      };
      return { opener, show };
    };

    it("moves the focus inside when it is shown", () => {
      const { show } = renderWithOpener();

      show(true);

      expect(panel().contains(document.activeElement)).toBe(true);
    });

    it("keeps Tab inside the panel", () => {
      const { show } = renderWithOpener();
      show(true);
      const close = document.querySelector("[data-testid='side-panel-close']");
      close.focus();

      fireEvent.keyDown(close, { key: "Tab", code: "Tab" });
      expect(panel().contains(document.activeElement)).toBe(true);

      fireEvent.keyDown(document.activeElement, { key: "Tab", code: "Tab", shiftKey: true });
      expect(panel().contains(document.activeElement)).toBe(true);
    });

    it("gives the focus back to the control that opened it when it is closed", () => {
      const { opener, show } = renderWithOpener();
      show(true);

      show(false);

      expect(document.activeElement).toBe(opener);
    });
  });
});
