import React from "react";
import { renderWithProviders } from "../test-utils";
import { ComponentList } from "../../../src/components/AweComponents";
import AwePopover from "../../../src/components/AwePopover";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { act, fireEvent } from "@testing-library/react";
import fs from "fs";
import path from "path";

describe("awe-react-client/test/js/components/AwePopoverTest.jsx", () => {
  const address = { component: "pop", view: "report" };
  const stateWith = (attributes) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      pop: {
        address,
        model: { values: [] },
        attributes: { isShowing: true, anchor: "ButOpen", ...attributes },
        specificAttributes: {},
        actions: []
      }
    }
  });
  const ANCHOR_RECT = { top: 100, left: 50, bottom: 130, right: 150, width: 100, height: 30, x: 50, y: 100 };
  const addAnchor = (attribute = "id") => {
    const anchor = document.createElement("button");
    anchor.setAttribute(attribute, "ButOpen");
    anchor.className = "anchor";
    anchor.textContent = "open";
    anchor.getBoundingClientRect = () => ANCHOR_RECT;
    document.body.appendChild(anchor);
    return anchor;
  };
  const elementList = [
    { elementType: "Tag", type: "div", label: "inside", elementList: [] },
    { elementType: "Tag", type: "div", style: "modal-footer", label: "footer", elementList: [] }
  ];
  const renderPopover = (attributes = {}, options = {}) => renderWithProviders(
    <AwePopover id="pop" elementList={elementList}/>,
    { preloadedState: stateWith(attributes), ...options }
  );
  const popover = () => document.querySelector("[data-testid='popover']");
  const closeButton = () => document.querySelector("[data-testid='popover-close']");
  const closeTypes = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action?.actions)
    .flatMap(action => action.actions.map(a => a.type));

  afterEach(() => {
    document.querySelectorAll(".anchor, .outside").forEach(element => element.remove());
  });

  it("is the renderer of the Popover element", () => {
    expect(ComponentList.Popover).toBe(AwePopover);
  });

  it("shows the header, the body, the footer and the open test state", () => {
    addAnchor();
    renderPopover({ label: "Details" });

    expect(popover()).not.toBeNull();
    expect(popover().getAttribute("data-testid-owner")).toBe("pop");
    expect(popover().getAttribute("data-open")).toBe("true");
    expect(popover().getAttribute("role")).toBe("dialog");
    expect(popover().querySelector(".popover-title").textContent).toBe("Details");
    expect(popover().textContent).toContain("inside");
    expect(popover().querySelector(".modal-footer").textContent).toContain("footer");
    expect(closeButton()).not.toBeNull();
  });

  it("does not render while it is hidden", () => {
    addAnchor();
    renderPopover({ isShowing: false });

    expect(popover()).toBeNull();
  });

  describe("anchor", () => {
    it("opens below the anchor by default", () => {
      addAnchor();
      renderPopover();

      expect(popover().classList).toContain("popover-bottom");
      expect(popover().style.position).toBe("absolute");
      expect(popover().style.top).toBe(`${130 + window.scrollY}px`);
      expect(popover().style.left).toBe(`${50 + window.scrollX}px`);
    });

    it("opens above the anchor with position top", () => {
      addAnchor();
      renderPopover({ position: "top" });

      expect(popover().classList).toContain("popover-top");
      expect(popover().style.top).toBe(`${100 + window.scrollY}px`);
      expect(popover().style.left).toBe(`${50 + window.scrollX}px`);
      expect(popover().style.transform).toBe("translateY(-100%)");
    });

    it("opens at the left of the anchor with position left", () => {
      addAnchor();
      renderPopover({ position: "left" });

      expect(popover().classList).toContain("popover-left");
      expect(popover().style.top).toBe(`${100 + window.scrollY}px`);
      expect(popover().style.left).toBe(`${50 + window.scrollX}px`);
      expect(popover().style.transform).toBe("translateX(-100%)");
    });

    it("opens at the right of the anchor with position right", () => {
      addAnchor();
      renderPopover({ position: "right" });

      expect(popover().classList).toContain("popover-right");
      expect(popover().style.top).toBe(`${100 + window.scrollY}px`);
      expect(popover().style.left).toBe(`${150 + window.scrollX}px`);
    });

    it("is anchored to a criterion through its criterion-id", () => {
      addAnchor("criterion-id");
      renderPopover();

      expect(popover().style.top).toBe(`${130 + window.scrollY}px`);
      expect(popover().classList).not.toContain("popover-unanchored");
    });

    it("is shown unanchored when the anchor does not exist", () => {
      renderPopover();

      expect(popover().classList).toContain("popover-unanchored");
    });
  });

  describe("max width", () => {
    it("keeps a default maximum width", () => {
      addAnchor();
      renderPopover();

      expect(popover().style.maxWidth).toBe("20rem");
    });

    it("uses the maximum width of the popover", () => {
      addAnchor();
      renderPopover({ maxWidth: "35rem" });

      expect(popover().style.maxWidth).toBe("35rem");
    });
  });

  describe("styles", () => {
    const SRC = path.join(__dirname, "../../../src");
    // Value of a property in the first rule of a stylesheet whose selector is exactly the given one
    const property = (file, selector, name) => {
      const escaped = selector.replace(/[.>*]/g, "\\$&");
      const stylesheet = fs.readFileSync(path.join(SRC, file), "utf8");
      const rule = stylesheet.match(new RegExp(`(?:^|\\s)${escaped}\\s*\\{([^}]*)\\}`));
      return rule?.[1].match(new RegExp(`(?:^|[\\s;])${name}:\\s*([^;]+);`))?.[1].trim();
    };

    it("pads its body as much as a panel body goes out of its parent, so the body does not scroll sideways", () => {
      const panelMargin = property("main.css", ".panel-body", "margin");

      expect(panelMargin).toMatch(/^-/);
      expect(property("components/AwePopover.less", ".popover-body", "padding")).toBe(panelMargin.slice(1));
    });

    it("takes the footer to the edges of the body", () => {
      const padding = property("components/AwePopover.less", ".popover-body", "padding");

      expect(property("components/AwePopover.less", ".modal-footer", "bottom")).toBe(`-${padding}`);
      expect(property("components/AwePopover.less", ".modal-footer", "margin")).toBe(`auto -${padding} -${padding}`);
    });
  });

  describe("close", () => {
    it("closes with the close action from its close button", () => {
      addAnchor();
      const { dispatchSpy } = renderPopover({}, { spyDispatch: true });

      fireEvent.click(closeButton());

      expect(closeTypes(dispatchSpy)).toEqual(["close"]);
    });

    it("closes with the close-cancel action when on-close is reject", () => {
      addAnchor();
      const { dispatchSpy } = renderPopover({ acceptOnClose: false }, { spyDispatch: true });

      fireEvent.click(closeButton());

      expect(closeTypes(dispatchSpy)).toEqual(["close-cancel"]);
    });

    it("closes with Esc", () => {
      addAnchor();
      const { dispatchSpy } = renderPopover({}, { spyDispatch: true });

      fireEvent.keyDown(popover(), { key: "Escape", code: "Escape" });

      expect(closeTypes(dispatchSpy)).toEqual(["close"]);
    });

    it("closes with a click outside the popover", () => {
      addAnchor();
      const outside = document.createElement("div");
      outside.className = "outside";
      document.body.appendChild(outside);
      const { dispatchSpy } = renderPopover({}, { spyDispatch: true });

      fireEvent.mouseDown(outside);

      expect(closeTypes(dispatchSpy)).toEqual(["close"]);
    });

    it("stays open with a click inside the popover", () => {
      addAnchor();
      const { dispatchSpy } = renderPopover({}, { spyDispatch: true });

      fireEvent.mouseDown(popover().querySelector(".popover-body"));

      expect(closeTypes(dispatchSpy)).toEqual([]);
    });

    it("closes with a click on its anchor, which does not open it again", () => {
      const anchor = addAnchor();
      const action = jest.fn();
      anchor.addEventListener("click", action);
      const { dispatchSpy } = renderPopover({}, { spyDispatch: true });

      fireEvent.mouseDown(anchor);
      fireEvent.click(anchor);

      expect(closeTypes(dispatchSpy)).toEqual(["close"]);
      expect(action).not.toHaveBeenCalled();
    });

    it("lets the next click on its anchor run once it is closed", () => {
      const anchor = addAnchor();
      const action = jest.fn();
      anchor.addEventListener("click", action);
      renderPopover();

      fireEvent.mouseDown(anchor);
      fireEvent.click(anchor);
      fireEvent.click(anchor);

      expect(action).toHaveBeenCalledTimes(1);
    });

    it("does not listen to the closing events while it is hidden", () => {
      addAnchor();
      const { dispatchSpy } = renderPopover({ isShowing: false }, { spyDispatch: true });

      fireEvent.keyDown(document.body, { key: "Escape", code: "Escape" });
      fireEvent.mouseDown(document.body);

      expect(closeTypes(dispatchSpy)).toEqual([]);
    });
  });

  it("asks for the data of a deferred popover when it is shown", () => {
    addAnchor();
    const { store } = renderPopover({ deferred: true });

    expect(store.getState().components.pop.attributes.deferredLoaded).toBe(true);
  });

  describe("focus", () => {
    const renderWithOpener = () => {
      const opener = addAnchor();
      opener.focus();
      const result = renderWithProviders(
        <AwePopover id="pop" elementList={[]}/>,
        { preloadedState: stateWith({ isShowing: false }) }
      );
      const show = (isShowing) => {
        act(() => {
          result.store.dispatch({ type: "UPDATE_ATTRIBUTES", address, data: { isShowing } });
        });
      };
      return { opener, show };
    };

    it("moves the focus inside when it is shown", () => {
      const { show } = renderWithOpener();

      show(true);

      expect(popover().contains(document.activeElement)).toBe(true);
    });

    it("keeps Tab inside the popover", () => {
      const { show } = renderWithOpener();
      show(true);
      closeButton().focus();

      fireEvent.keyDown(closeButton(), { key: "Tab", code: "Tab" });
      expect(popover().contains(document.activeElement)).toBe(true);

      fireEvent.keyDown(document.activeElement, { key: "Tab", code: "Tab", shiftKey: true });
      expect(popover().contains(document.activeElement)).toBe(true);
    });

    it("gives the focus back to the control that opened it when it is closed", () => {
      const { opener, show } = renderWithOpener();
      show(true);

      show(false);

      expect(document.activeElement).toBe(opener);
    });
  });
});
