import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act, fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweWindow from "../../../src/components/AweWindow";

describe('awe-react-client/test/js/criteria/AweWindowTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      window: {
        address: {component: 'window', view: 'report'},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "window",
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders window component', () => {
    renderWithProviders(<AweWindow id="window"/>, {preloadedState});

    // check
    expect(document.querySelector("div.p-panel")).not.toBeNull();
  });

  it('exposes its identifier as the window-id attribute, the one that dependency actions target', () => {
    renderWithProviders(<AweWindow id="ExecutionsWindow" style="expand hidden"/>, {preloadedState});

    expect(document.querySelector("[window-id='ExecutionsWindow']")).toBe(document.querySelector("div.p-panel"));
  });


  it('lays its content out vertically by default', () => {
    renderWithProviders(<AweWindow id="w1"/>, {preloadedState});

    expect(document.querySelector("div.p-panel").classList).toContain("expandible-vertical");
  });

  it('lays its content out in the direction of its expandible attribute', () => {
    renderWithProviders(<AweWindow id="w2" expand="horizontal"/>, {preloadedState});

    const panel = document.querySelector("div.p-panel");
    expect(panel.classList).toContain("expandible-horizontal");
    expect(panel.classList).not.toContain("expandible-vertical");
  });

  it('shows no maximize button unless the window is maximizable', () => {
    renderWithProviders(<AweWindow id="w3"/>, {preloadedState});

    expect(document.querySelector("button.maximize-button")).toBeNull();
  });

  describe('maximize', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    const settle = (time = 600) => act(() => {
      jest.advanceTimersByTime(time);
    });
    const button = () => document.querySelector("button.maximize-button");
    const panel = () => document.querySelector("div.p-panel");

    it.each([true, "true"])('maximizes and restores the window with the maximize button (maximize=%s)', (maximize) => {
      renderWithProviders(<AweWindow id="w4" maximize={maximize} elementList={[]}/>, {preloadedState});
      expect(panel().classList).not.toContain("maximized");

      fireEvent.click(button());
      settle();
      expect(panel().classList).toContain("maximized");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#minimize");

      fireEvent.click(button());
      settle();
      expect(panel().classList).not.toContain("maximized");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#maximize");
    });

    it('maximizes over the maximize target of the template, not over a resizable that contains the window', () => {
      renderWithProviders(
        <div className="maximize-target" id="target">
          <div className="resizable" id="box"><AweWindow id="w5" maximize={true} elementList={[]}/></div>
        </div>, {preloadedState});
      jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
        .mockReturnValue({top: 40, left: 10, width: 800, height: 500});
      jest.spyOn(document.getElementById("box"), "getBoundingClientRect")
        .mockReturnValue({top: 100, left: 300, width: 200, height: 200});

      fireEvent.click(button());
      settle();

      expect(panel().style.position).toBe("fixed");
      expect([panel().style.top, panel().style.left, panel().style.width, panel().style.height])
        .toEqual(["40px", "10px", "800px", "500px"]);

      fireEvent.click(button());
      settle();
      expect(panel().style.position).toBe("");
    });

    it('follows the size of the maximize target while it is maximized', () => {
      renderWithProviders(<div className="maximize-target" id="target"><AweWindow id="w6" maximize={true}/></div>,
        {preloadedState});
      const rect = jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
        .mockReturnValue({top: 0, left: 0, width: 800, height: 500});
      fireEvent.click(button());
      settle();

      rect.mockReturnValue({top: 0, left: 0, width: 600, height: 400});
      act(() => {
        window.dispatchEvent(new Event("resize"));
      });

      expect(panel().style.width).toBe("600px");
    });

    it('fills the viewport when there is no maximize target', () => {
      renderWithProviders(<AweWindow id="w7" maximize={true}/>, {preloadedState});

      fireEvent.click(button());
      settle();

      expect([panel().style.position, panel().style.top, panel().style.left]).toEqual(["fixed", "0px", "0px"]);
    });

    it('minimizes its siblings while it is maximized and restores them afterwards', () => {
      renderWithProviders(
        <div className="maximize-target">
          <AweWindow id="w8" maximize={true}/>
          <div id="sibling">other</div>
        </div>, {preloadedState});
      const sibling = document.getElementById("sibling");

      fireEvent.click(button());
      settle();
      expect(sibling.classList).toContain("minimized");

      fireEvent.click(button());
      settle();
      expect(sibling.classList).not.toContain("minimized");
    });

    it('grows a clone over the target while it animates', () => {
      renderWithProviders(<AweWindow id="w9" maximize={true}/>, {preloadedState});

      fireEvent.click(button());

      expect(document.querySelector(".maximize-clone")).not.toBeNull();
      settle();
      expect(document.querySelector(".maximize-clone")).toBeNull();
    });

    it('exposes the window and its maximize button as test hooks that follow the state', () => {
      renderWithProviders(<AweWindow id="w10" maximize={true}/>, {preloadedState});
      const window10 = document.querySelector("[data-testid='window']");
      expect(window10).toBe(panel());
      expect(window10.getAttribute("data-testid-owner")).toBe("w10");
      expect(window10.getAttribute("data-maximized")).toBe("false");
      expect(button().getAttribute("data-testid")).toBe("window-maximize-button");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_MAXIMIZE");

      fireEvent.click(button());
      settle();
      expect(window10.getAttribute("data-maximized")).toBe("true");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_RESTORE");

      fireEvent.click(button());
      settle();
      expect(window10.getAttribute("data-maximized")).toBe("false");
    });

    it('renders the window hook without a maximize button when it is not maximizable', () => {
      renderWithProviders(<AweWindow id="w11"/>, {preloadedState});

      expect(document.querySelector("[data-testid='window']")).toBe(panel());
      expect(document.querySelector("[data-testid='window-maximize-button']")).toBeNull();
    });
  });

});
