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

  it.each([true, "true"])('maximizes and restores the window with the maximize button (maximize=%s)', (maximize) => {
    renderWithProviders(<AweWindow id="w4" maximize={maximize} elementList={[]}/>, {preloadedState});

    const panel = document.querySelector("div.p-panel");
    expect(panel.classList).not.toContain("maximized");

    fireEvent.click(document.querySelector("button.maximize-button"));
    expect(panel.classList).toContain("maximized");
    expect(document.querySelector("button.maximize-button i").className).toContain("pi-window-minimize");

    fireEvent.click(document.querySelector("button.maximize-button"));
    expect(panel.classList).not.toContain("maximized");
    expect(document.querySelector("button.maximize-button i").className).toContain("pi-window-maximize");
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

    fireEvent.click(document.querySelector("button.maximize-button"));

    const panel = document.querySelector("div.p-panel");
    expect(panel.style.position).toBe("fixed");
    expect([panel.style.top, panel.style.left, panel.style.width, panel.style.height])
      .toEqual(["40px", "10px", "800px", "500px"]);

    fireEvent.click(document.querySelector("button.maximize-button"));
    expect(panel.style.position).toBe("");
  });

  it('follows the size of the maximize target while it is maximized', () => {
    renderWithProviders(<div className="maximize-target" id="target"><AweWindow id="w6" maximize={true}/></div>,
      {preloadedState});
    const rect = jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
      .mockReturnValue({top: 0, left: 0, width: 800, height: 500});
    fireEvent.click(document.querySelector("button.maximize-button"));

    rect.mockReturnValue({top: 0, left: 0, width: 600, height: 400});
    act(() => {
      window.dispatchEvent(new Event("resize"));
    });

    expect(document.querySelector("div.p-panel").style.width).toBe("600px");
  });

  it('fills the viewport when there is no maximize target', () => {
    renderWithProviders(<AweWindow id="w7" maximize={true}/>, {preloadedState});

    fireEvent.click(document.querySelector("button.maximize-button"));

    const panel = document.querySelector("div.p-panel");
    expect([panel.style.position, panel.style.top, panel.style.left]).toEqual(["fixed", "0px", "0px"]);
  });

});
