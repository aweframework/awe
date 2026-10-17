import React from "react";
import {act, cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";
import {renderWithProviders} from "../test-utils";
import ViewContainer from "../../../src/containers/ViewContainer";
import SubViewContainer from "../../../src/containers/SubViewContainer";

const mockLoadScreen = jest.fn();

jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: (...args) => () => mockLoadScreen(...args)
}));

// The base screen renders the sub screen in its outlet
jest.mock("../../../src/templates", () => ({
  __esModule: true,
  default: () => {
    const {Outlet: MockOutlet} = jest.requireActual("react-router");
    return <div id="screen-contents"><MockOutlet/></div>;
  }
}));

// Reloading the page on a screen with a sub screen: the splash stays until the sub screen is loaded too
describe("awe-react-client/test/js/containers/InitialLoadSplashSubViewTest.jsx", () => {
  afterEach(cleanup);

  it("keeps the splash animation, and no skeleton, until the sub screen of the first screen is loaded", async () => {
    const loads = {};
    mockLoadScreen.mockImplementation(view => new Promise(resolve => {
      loads[view] = resolve;
    }));
    ViewRegistry.clearAll();
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={["/main/sub"]}>
        <Routes>
          <Route path="/:screenId" element={<ViewContainer/>}>
            <Route path=":subScreenId" element={<SubViewContainer/>}/>
          </Route>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );

    await act(async () => {
      ViewRegistry.setView("base", {loading: false, structure: []});
      loads.base();
    });
    expect(container.querySelector("#screen-contents")).not.toBeNull();
    expect(container.querySelector("#splash")).not.toBeNull();
    expect(container.querySelector(".screen-skeleton")).toBeNull();

    await act(async () => {
      ViewRegistry.setView("report", {loading: false, structure: []});
      loads.report();
    });
    expect(container.querySelector("#splash")).toBeNull();

    // A later sub screen starts loading
    act(() => ViewRegistry.updateView("report", {loading: true}));
    expect(container.querySelector(".screen-skeleton")).not.toBeNull();
    expect(container.querySelector("#splash")).toBeNull();
  });
});
