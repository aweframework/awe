import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import {useInvalidValues} from "../../../src/hooks/usePanelable";
import {addStack} from "../../../src/redux/actions/actions";
import {updateAttributes} from "../../../src/redux/actions/components";
import * as reveal from "../../../src/utilities/reveal";

/**
 * The tabs, steps and accordions mark the options that hold invalid fields. The components are scanned once per change
 * of the components, for all the panels of the screen, and not for every panel on every dispatch (#962)
 */
describe("awe-react-client/test/js/performance/InvalidValuesScanTest.jsx", () => {
  const address = (component) => ({component, view: "report"});
  const field = (id, source, attributes = {}) => ({address: address(id), attributes, model: {values: []}, context: {source}});
  const preloadedState = {
    actions: {running: false, sync: [[]], async: []},
    components: {
      name: field("name", ["tabs", "one", "name"], {error: "Required"}),
      code: field("code", ["tabs", "two", "accordion", "first", "code"]),
      other: field("other", ["steps", "start", "other"])
    }
  };

  const seen = {};
  const Panel = ({id}) => {
    seen[id] = useInvalidValues(id);
    return null;
  };

  const scans = () => {
    const calls = (name) => (jest.isMockFunction(reveal[name]) ? reveal[name].mock.calls.length : 0);
    return calls("getInvalidPanelValues") + calls("getInvalidValuesByPanel");
  };

  const renderPanels = () => {
    jest.spyOn(reveal, "getInvalidPanelValues");
    if (reveal.getInvalidValuesByPanel) {
      jest.spyOn(reveal, "getInvalidValuesByPanel");
    }
    return renderWithProviders(<>
      <Panel id="tabs"/>
      <Panel id="accordion"/>
      <Panel id="steps"/>
    </>, {preloadedState});
  };

  afterEach(() => jest.restoreAllMocks());

  it("marks the options that hold invalid fields", () => {
    const {store} = renderPanels();
    expect(seen.tabs).toEqual(["one"]);
    expect(seen.accordion).toEqual([]);
    expect(seen.steps).toEqual([]);

    act(() => {
      store.dispatch(updateAttributes(address("code"), {error: "Too long"}));
    });

    expect(seen.tabs).toEqual(["one", "two"]);
    expect(seen.accordion).toEqual(["first"]);
    expect(seen.steps).toEqual([]);
  });

  it("does not scan the components on a dispatch that does not change them", () => {
    const {store} = renderPanels();
    const before = scans();

    act(() => {
      store.dispatch(addStack());
    });

    expect(scans()).toBe(before);
  });

  it("scans the components once for all the panels when they change", () => {
    const {store} = renderPanels();
    const before = scans();

    act(() => {
      store.dispatch(updateAttributes(address("other"), {visible: false}));
    });

    expect(scans() - before).toBe(1);
  });

  it("keeps the same values of a panel when its marked options do not change", () => {
    const {store} = renderPanels();
    const tabs = seen.tabs;

    act(() => {
      store.dispatch(updateAttributes(address("other"), {error: "Required"}));
    });

    expect(seen.steps).toEqual(["start"]);
    expect(seen.tabs).toBe(tabs);
  });
});
