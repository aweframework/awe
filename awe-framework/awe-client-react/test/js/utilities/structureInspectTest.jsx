import React from "react";
import {fireEvent, screen} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import {Components, setInspectWrapper} from "../../../src/utilities/structure";
import {ComponentList} from "../../../src/components/AweComponents";
import {InspectContext, InspectNode, inspectWrapper} from "../../../src/components/ScreenInspect";

describe("awe-react-client/test/js/utilities/structureInspectTest.jsx", () => {
  afterEach(() => setInspectWrapper(null));

  const context = (overrides = {}) => ({configurable: new Set(["btn"]), selected: null, personalized: new Set(), onSelect: jest.fn(), ...overrides});
  const renderNode = (ctx, id = "btn", state = {}) => renderWithProviders(
    <InspectContext.Provider value={ctx}><InspectNode node={{id}}><button onClick={state.onClick}>Save</button></InspectNode></InspectContext.Provider>,
    {preloadedState: {settings: DEFAULT_SETTINGS, components: state.components || {}}});

  it("leaves the normal rendering untouched when no inspect wrapper is installed", () => {
    expect(Components({elementType: "Button", id: "btn"}, 0).type).toBe(ComponentList.Button);
  });

  it("wraps the component only while the inspect wrapper is installed", () => {
    setInspectWrapper(inspectWrapper);
    expect(Components({elementType: "Button", id: "btn"}, 0).type).toBe(InspectNode);
    expect(Components({elementType: "ButtonAction"}, 0)).toBeNull();
  });

  it("selects a configurable element on click instead of letting it act", () => {
    const ctx = context(), onClick = jest.fn();
    renderNode(ctx, "btn", {onClick});
    fireEvent.click(screen.getByText("Save"));
    expect(ctx.onSelect).toHaveBeenCalledWith("btn");
    expect(onClick).not.toHaveBeenCalled();
  });

  it("selects the innermost element of nested ones (a grid in a window), and the outer one around it", () => {
    const ctx = context({configurable: new Set(["win", "grid"])});
    renderWithProviders(
      <InspectContext.Provider value={ctx}>
        <InspectNode node={{id: "win"}}><section>Window<InspectNode node={{id: "grid"}}><div>Grid</div></InspectNode></section></InspectNode>
      </InspectContext.Provider>,
      {preloadedState: {settings: DEFAULT_SETTINGS, components: {}}});
    fireEvent.click(screen.getByText("Grid"));
    fireEvent.click(screen.getByText("Window"));
    expect(ctx.onSelect.mock.calls).toEqual([["grid"], ["win"]]);
  });

  it("does not touch an element that is not configurable", () => {
    const ctx = context(), onClick = jest.fn();
    renderNode(ctx, "other", {onClick});
    fireEvent.click(screen.getByText("Save"));
    expect(onClick).toHaveBeenCalled();
    expect(ctx.onSelect).not.toHaveBeenCalled();
  });

  it("shows a hidden element dimmed with a hidden tag so it can be selected", () => {
    const ctx = context();
    renderNode(ctx, "btn", {components: {btn: {address: {component: "btn", view: "report"}, model: {}, attributes: {visible: false, label: "Save"}}}});
    fireEvent.click(screen.getByText("SCR_PER_HIDDEN"));
    expect(ctx.onSelect).toHaveBeenCalledWith("btn");
  });
});
