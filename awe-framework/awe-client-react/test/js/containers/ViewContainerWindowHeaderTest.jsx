import React from "react";
import {cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import ViewContainer from "../../../src/containers/ViewContainer";

// The server serializes the whole screen as the view structure: the window template receives its header props from it
jest.mock("../../../src/hooks/useViewRegistry", () => ({
  useView: () => ({
    loading: false,
    title: "Title",
    structure: {screenTemplate: "window", windowHeader: "none", label: "SCREEN_TITLE", elementList: []}
  })
}));

jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: () => () => Promise.resolve()
}));

describe("awe-react-client/test/js/containers/ViewContainerWindowHeaderTest.jsx", () => {
  afterEach(cleanup);

  it("renders no breadcrumb for a window screen whose structure asks for the none header", () => {
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={["/screen"]}>
        <Routes>
          <Route path="/:screenId" element={<ViewContainer/>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: {...DEFAULT_SETTINGS, windowHeader: "breadcrumb"}}}
    );

    expect(container.querySelector('[data-testid="breadcrumb"]')).toBeNull();
  });
});
