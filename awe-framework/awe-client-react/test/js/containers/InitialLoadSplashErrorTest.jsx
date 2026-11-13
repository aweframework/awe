import React from "react";
import {act, cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";
import {renderWithProviders} from "../test-utils";
import ViewContainer from "../../../src/containers/ViewContainer";

const mockLoadScreen = jest.fn();

jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: (...args) => () => mockLoadScreen(...args)
}));

// A first screen that fails to load never leaves the splash animation over the page
describe("awe-react-client/test/js/containers/InitialLoadSplashErrorTest.jsx", () => {
  afterEach(cleanup);

  it("removes the splash animation when the first screen fails to load", async () => {
    let finishLoad;
    mockLoadScreen.mockReturnValue(new Promise(resolve => {
      finishLoad = resolve;
    }));
    ViewRegistry.clearAll();
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={["/screen"]}>
        <Routes>
          <Route path="/:screenId" element={<ViewContainer/>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );
    expect(container.querySelector("#splash")).not.toBeNull();

    // The load ends with the view still loading: the screen could not be retrieved
    await act(async () => finishLoad());

    expect(container.querySelector("#splash")).toBeNull();
    expect(container.querySelector(".screen-skeleton")).not.toBeNull();
  });
});
