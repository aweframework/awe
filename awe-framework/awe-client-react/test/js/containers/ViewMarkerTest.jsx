import React from "react";
import {cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import ViewContainer from "../../../src/containers/ViewContainer";
import SubViewContainer from "../../../src/containers/SubViewContainer";

jest.mock("../../../src/templates", () => ({
  __esModule: true,
  default: () => <div id="content"/>
}));

jest.mock("../../../src/hooks/useViewRegistry", () => ({
  useView: () => ({loading: false, title: "Title", structure: []})
}));

jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: () => () => Promise.resolve()
}));

describe("awe-react-client/test/js/containers/ViewMarkerTest.jsx", () => {
  afterEach(cleanup);

  // A target message looks for its component in the view of its address
  it.each([
    ["ViewContainer", ViewContainer, "base"],
    ["SubViewContainer", SubViewContainer, "report"]
  ])("marks the components rendered by %s with their view", (_name, Container, view) => {
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={["/screen"]}>
        <Routes>
          <Route path="/:screenId" element={<Container/>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );

    expect(container.querySelector("#content").closest("[view-id]").getAttribute("view-id")).toBe(view);
  });
});
