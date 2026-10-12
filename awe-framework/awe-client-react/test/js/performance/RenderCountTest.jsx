import React, {Profiler, Suspense} from "react";
import {act} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweCriterion from "../../../src/criteria/AweCriterion";
import AweGrid from "../../../src/components/AweGrid";
import MessageContainer from "../../../src/containers/MessageContainer";
import {useMultipleComponents} from "../../../src/hooks/useComponentState";
import {updateAttributes, updateModel} from "../../../src/redux/actions/components";

/**
 * Render count tests: a component only renders again when the state it reads changes, not on every dispatch
 */
describe("awe-react-client/test/js/performance/RenderCountTest.jsx", () => {
  const address = (component) => ({component, view: "report"});
  const component = (name, attributes = {}) => ({
    address: address(name),
    model: {values: []},
    attributes,
    specificAttributes: {sort: []}
  });

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    screen: {size: {width: 100, height: 100}},
    messages: {showing: []},
    components: {
      grid: component("grid", {
        columnModel: [{name: "test", label: "test"}],
        headerModel: [],
        buttonModel: [],
        contextMenu: [{id: "menuOption"}]
      }),
      menuOption: component("menuOption", {label: "Option", visible: true}),
      criterion: component("criterion", {label: "Criterion"}),
      other: component("other", {label: "Other"})
    }
  };

  /**
   * Render the element inside a profiler that counts the commits that touch it
   */
  const renderCounting = (element) => {
    const commits = {count: 0};
    const rendered = renderWithProviders(
      <Profiler id="counter" onRender={() => commits.count++}>
        <Suspense fallback={null}>{element}</Suspense>
      </Profiler>, {preloadedState});
    // Only count what happens after the first render
    commits.count = 0;
    return {...rendered, commits};
  };

  const dispatchOnOtherComponent = (store) => act(() => {
    store.dispatch(updateAttributes(address("other"), {label: "Changed"}));
    store.dispatch(updateModel(address("other"), {values: [{value: "1", label: "One"}]}));
  });

  it("does not render a criterion again when another component changes", () => {
    const {store, commits} = renderCounting(
      <AweCriterion address={address("criterion")} attributes={{label: "Criterion"}}><input/></AweCriterion>);

    dispatchOnOtherComponent(store);

    expect(commits.count).toBe(0);
  });

  it("does not render a grid again when another component changes", () => {
    const {store, commits} = renderCounting(<AweGrid id="grid"/>);

    dispatchOnOtherComponent(store);

    expect(commits.count).toBe(0);
  });

  it("renders a grid again when the model of the grid changes", () => {
    const {store, commits} = renderCounting(<AweGrid id="grid"/>);

    act(() => {
      store.dispatch(updateModel(address("grid"), {values: [{id: 1, test: "value"}]}));
    });

    expect(commits.count).toBeGreaterThan(0);
  });

  it("renders a grid again when the attributes of one of its context menu options change", () => {
    const {store, commits} = renderCounting(<AweGrid id="grid"/>);

    act(() => {
      store.dispatch(updateAttributes(address("menuOption"), {label: "Changed option"}));
    });

    expect(commits.count).toBeGreaterThan(0);
  });

  it("does not render the message container again when another component changes", () => {
    const {store, commits} = renderCounting(<MessageContainer/>);

    dispatchOnOtherComponent(store);

    expect(commits.count).toBe(0);
  });

  it("does not render the users of several components again when another component changes", () => {
    const Reader = () => <span>{Object.keys(useMultipleComponents(["criterion", "other"])).join()}</span>;
    const {store, commits} = renderCounting(<Reader/>);

    act(() => {
      store.dispatch(updateAttributes(address("grid"), {label: "Changed"}));
    });

    expect(commits.count).toBe(0);
  });

  it("renders the users of several components again when one of them changes", () => {
    const Reader = () => <span>{Object.keys(useMultipleComponents(["criterion", "other"])).join()}</span>;
    const {store, commits} = renderCounting(<Reader/>);

    dispatchOnOtherComponent(store);

    expect(commits.count).toBeGreaterThan(0);
  });
});
