import React from "react";
import {cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import ViewContainer from "../../../src/containers/ViewContainer";
import SubViewContainer from "../../../src/containers/SubViewContainer";

const mockTemplates = jest.fn();

jest.mock("../../../src/templates", () => ({
  __esModule: true,
  default: (...args) => mockTemplates(...args)
}));

jest.mock("../../../src/hooks/useViewRegistry", () => ({
  useView: () => ({loading: false, title: "Title", structure: []})
}));

jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: () => () => Promise.resolve()
}));

const hook = (testId) => `[data-testid='${testId}']`;

describe("awe-react-client/test/js/containers/ErrorBoundaryContainersTest.jsx", () => {
  beforeEach(() => {
    jest.spyOn(console, "error").mockImplementation(() => {});
    jest.spyOn(console, "warn").mockImplementation(() => {});
    // Templates returns elements: a template fails when React renders it
    const Broken = () => {
      throw new Error("Cannot read properties of undefined (reading 'attributes')");
    };
    mockTemplates.mockImplementation(() => <Broken/>);
  });

  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
    mockTemplates.mockReset();
  });

  it.each([
    ["ViewContainer", ViewContainer, "/screen"],
    ["SubViewContainer", SubViewContainer, "/screen"]
  ])("shows the error panel of the view when %s fails to render a template", (_name, Container, route) => {
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/:screenId" element={<><header id="header"/><Container/></>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );

    const panel = container.querySelector(hook("error-boundary"));
    expect(panel).not.toBeNull();
    expect(panel.getAttribute("data-scope")).toBe("view");
    expect(container.querySelector(hook("error-boundary-message")).textContent).toContain("reading 'attributes'");
    // The rest of the application stays on screen
    expect(container.querySelector("#header")).not.toBeNull();
  });

  it.each([
    ["ViewContainer", ViewContainer, "/screen"],
    ["SubViewContainer", SubViewContainer, "/screen"]
  ])("shows the error panel of the view when %s fails while building the templates", (_name, Container, route) => {
    // The structure itself is wrong: Templates throws before React renders any template
    mockTemplates.mockImplementation(() => {
      throw new Error("Cannot read properties of undefined (reading 'elementList')");
    });
    const {container} = renderWithProviders(
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/:screenId" element={<><header id="header"/><Container/></>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );

    const panel = container.querySelector(hook("error-boundary"));
    expect(panel).not.toBeNull();
    expect(panel.getAttribute("data-scope")).toBe("view");
    expect(container.querySelector(hook("error-boundary-message")).textContent).toContain("reading 'elementList'");
    expect(container.querySelector("#header")).not.toBeNull();
  });
});
