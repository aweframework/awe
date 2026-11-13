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

// A first screen whose load is rejected never leaves the splash animation over the page
describe("awe-react-client/test/js/containers/InitialLoadSplashRejectTest.jsx", () => {
  afterEach(cleanup);

  it("removes the splash animation when ViewContainer fails to load the first screen", async () => {
    let rejectLoad;
    mockLoadScreen.mockReturnValue(new Promise((_resolve, reject) => {
      rejectLoad = reject;
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

    await act(async () => rejectLoad(new Error("screen not retrieved")));

    expect(container.querySelector("#splash")).toBeNull();
    expect(container.querySelector(".screen-skeleton")).not.toBeNull();
  });
});
