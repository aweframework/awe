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

  it("shows the icon of the panel before its title, as a decorative icon (#793)", () => {
    renderPanel({ label: "User", icon: "user" });

    const title = panel().querySelector(".side-panel-title");
    const icon = title.querySelector(".panel-title-icon");
    expect(icon).not.toBeNull();
    expect(icon).toBe(title.firstElementChild);
    expect(icon.getAttribute("aria-hidden")).toBe("true");
    expect(icon.querySelector("svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#user");
    expect(title.textContent).toBe("User");
  });

  it("shows only the title without an icon", () => {
    renderPanel({ label: "Details" });

    expect(panel().querySelector(".side-panel-title").innerHTML).toBe("Details");
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

  // Identity header (#793 S18): while exactly one row of header-grid is selected, the header shows that row
  describe("identity header", () => {
    const HEADER = { headerGrid: "GrdUsr", headerTitleField: "FulNam", headerSubtitleField: "UsrPro", headerAvatarField: "Ini",
      headerAvatarToneField: "IniTon", headerTags: [
        { field: "StaTxt", toneField: "StaTon" },
        { field: "2faHdr", toneField: "2faTon", iconField: "2faIcn" }
      ] };
    const rows = (selected) => [
      { id: "1", FulNam: "Javier García", UsrPro: "jgarcia · Operator", Ini: "Javier García", IniTon: "warning",
        StaTxt: "Locked", StaTon: "danger", "2faHdr": "Two-factor on", "2faTon": "neutral", "2faIcn": "lucide:shield-check" },
      { id: "2", FulNam: { value: "ana", label: "Ana Ruiz" }, UsrPro: "ana", Ini: "Ana Ruiz", IniTon: "unknown",
        StaTxt: "Active", StaTon: "success", "2faHdr": "", "2faTon": "neutral", "2faIcn": "" }
    ].map(row => ({ ...row, selected: selected.includes(row.id) }));
    const gridAddress = { component: "GrdUsr", view: "report" };
    const renderIdentity = (selected, attributes = HEADER) => {
      const state = stateWith({ label: "User", icon: "user", ...attributes });
      state.components.GrdUsr = {
        address: gridAddress,
        model: { values: rows(selected) },
        attributes: { columnModel: [{ name: "FulNam" }] },
        specificAttributes: {},
        actions: []
      };
      return renderWithProviders(
        <AweSidePanel id="panel" elementList={[{ elementType: "Tag", type: "div", label: "inside", elementList: [] }]}/>,
        { preloadedState: state }
      );
    };
    const identity = () => panel().querySelector(".panel-identity");

    it("shows the avatar, the title, the subtitle and the tags of the selected row", () => {
      renderIdentity(["1"]);

      expect(identity()).not.toBeNull();
      expect(panel().querySelector(".side-panel-title")).toBeNull();
      const avatar = identity().querySelector(".panel-identity-avatar");
      expect(avatar.textContent).toBe("JG");
      expect(avatar.classList.contains("awe-cell-avatar")).toBe(true);
      expect(avatar.classList.contains("awe-cell-avatar-warning")).toBe(true);
      expect(avatar.getAttribute("aria-hidden")).toBe("true");
      const title = identity().querySelector(".panel-identity-title");
      expect(title.textContent).toBe("Javier García");
      expect(title.id).toBe("panel-title");
      expect(panel().getAttribute("aria-labelledby")).toBe("panel-title");
      expect(identity().querySelector(".panel-identity-subtitle").textContent).toBe("jgarcia · Operator");
      const tags = identity().querySelectorAll(".panel-identity-tags > .awe-cell-status");
      expect(tags).toHaveLength(2);
      expect(tags[0].textContent).toBe("Locked");
      expect(tags[0].classList.contains("awe-cell-status-danger")).toBe(true);
      expect(tags[0].classList.contains("awe-cell-status-plain")).toBe(false);
      expect(tags[1].textContent).toBe("Two-factor on");
      expect(tags[1].classList.contains("awe-cell-status-plain")).toBe(true);
      expect(tags[1].querySelector(".awe-cell-icon[aria-hidden='true'] use").getAttribute("href")).toContain("shield-check");
    });

    it("marks the title, the subtitle and each tag of the identity header for the tests", () => {
      renderIdentity(["1"]);

      expect(panel().querySelector("[data-testid='side-panel-title']").textContent).toBe("Javier García");
      expect(panel().querySelector("[data-testid='side-panel-subtitle']").textContent).toBe("jgarcia · Operator");
      const tags = panel().querySelectorAll("[data-testid='side-panel-tag']");
      expect(tags).toHaveLength(2);
      expect(tags[0].textContent).toBe("Locked");
      expect(tags[1].textContent).toBe("Two-factor on");
    });

    it("marks the title of the label for the tests without a selected row", () => {
      renderIdentity([]);

      expect(panel().querySelector("[data-testid='side-panel-title']").textContent).toBe("User");
      expect(panel().querySelector("[data-testid='side-panel-subtitle']")).toBeNull();
      expect(panel().querySelector("[data-testid='side-panel-tag']")).toBeNull();
    });

    it.each([[[]], [["1", "2"]]])("shows the label and the icon of the panel with the selected rows %j", (selected) => {
      renderIdentity(selected);

      expect(identity()).toBeNull();
      const title = panel().querySelector(".side-panel-title");
      expect(title.textContent).toBe("User");
      expect(title.querySelector(".panel-title-icon")).not.toBeNull();
      expect(title.id).toBe("panel-title");
      expect(panel().getAttribute("aria-labelledby")).toBe("panel-title");
    });

    it("follows the selected row while it is open, hiding the tags with an empty value", () => {
      const { store } = renderIdentity(["1"]);

      act(() => {
        store.dispatch({ type: "UPDATE_MODEL", address: gridAddress, data: { selected: ["2"] } });
      });

      expect(identity().querySelector(".panel-identity-title").textContent).toBe("Ana Ruiz");
      expect(identity().querySelector(".panel-identity-avatar").textContent).toBe("AR");
      expect(identity().querySelector(".panel-identity-avatar").classList.contains("awe-cell-avatar-neutral")).toBe(true);
      const tags = identity().querySelectorAll(".panel-identity-tags > .awe-cell-status");
      expect(tags).toHaveLength(1);
      expect(tags[0].textContent).toBe("Active");
      expect(tags[0].classList.contains("awe-cell-status-success")).toBe(true);

      act(() => {
        store.dispatch({ type: "UPDATE_MODEL", address: gridAddress, data: { selected: [] } });
      });
      expect(identity()).toBeNull();
      expect(panel().querySelector(".side-panel-title").textContent).toBe("User");
    });

    it("does not change without header-grid, even with a selected row in the screen", () => {
      renderIdentity(["1"], {});

      expect(identity()).toBeNull();
      expect(panel().querySelector(".side-panel-title").textContent).toBe("User");
      expect(panel().querySelector(".side-panel-title").id).toBe("");
      expect(panel().hasAttribute("aria-labelledby")).toBe(false);
    });
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
