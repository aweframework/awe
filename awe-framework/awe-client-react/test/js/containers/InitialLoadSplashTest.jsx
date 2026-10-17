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

jest.mock("../../../src/templates", () => ({
  __esModule: true,
  default: () => <div id="screen-contents"/>
}));

const hook = (testId) => `[data-testid='${testId}']`;

// The first screen of the page session loads under the splash animation, the later ones under the screen skeleton
describe("awe-react-client/test/js/containers/InitialLoadSplashTest.jsx", () => {
  const render = Container => renderWithProviders(
    <MemoryRouter initialEntries={["/screen"]}>
      <Routes>
        <Route path="/:screenId" element={<Container/>}/>
      </Routes>
    </MemoryRouter>,
    {preloadedState: {settings: DEFAULT_SETTINGS}}
  );

  beforeEach(() => {
    ViewRegistry.clearAll();
  });

  afterEach(() => {
    cleanup();
    mockLoadScreen.mockReset();
  });

  it.each([
    ["ViewContainer", ViewContainer],
    ["SubViewContainer", SubViewContainer]
  ])("shows only the splash animation, with the loading hook, while %s loads the first screen", (_name, Container) => {
    mockLoadScreen.mockReturnValue(new Promise(() => {}));

    const {container} = render(Container);

    const splash = container.querySelector(hook("loading-spinner"));
    expect(splash).not.toBeNull();
    expect(splash.id).toBe("splash");
    expect(splash.getAttribute("aria-busy")).toBe("true");
    expect(splash.querySelector("#splashBackground")).not.toBeNull();
    expect(splash.querySelector(".spinner .dot1")).not.toBeNull();
    expect(splash.querySelector(".spinner .dot2")).not.toBeNull();
    expect(container.querySelector(".screen-skeleton")).toBeNull();
    expect(container.querySelector(".p-skeleton")).toBeNull();
  });

  it("shows the screen skeleton, and no splash, when a screen loads after the first one", async () => {
    let finishLoad;
    mockLoadScreen.mockReturnValue(new Promise(resolve => {
      finishLoad = resolve;
    }));
    const {container} = render(ViewContainer);

    await act(async () => {
      ViewRegistry.setView("base", {loading: false, structure: []});
      finishLoad();
    });
    expect(container.querySelector("#screen-contents")).not.toBeNull();
    expect(container.querySelector("#splash")).toBeNull();

    // A later screen starts loading
    act(() => ViewRegistry.updateView("base", {loading: true}));

    const skeleton = container.querySelector(hook("loading-spinner"));
    expect(skeleton).not.toBeNull();
    expect(skeleton.classList.contains("screen-skeleton")).toBe(true);
    expect(skeleton.getAttribute("aria-busy")).toBe("true");
    expect(container.querySelector("#splash")).toBeNull();
  });
});
