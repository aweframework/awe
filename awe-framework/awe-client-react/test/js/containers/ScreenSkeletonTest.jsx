import React from "react";
import {cleanup} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router";
import ViewContainer from "../../../src/containers/ViewContainer";
import SubViewContainer from "../../../src/containers/SubViewContainer";
import {endInitialLoad} from "../../../src/components/AweScreenSkeleton";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";

const hook = (testId) => `[data-testid='${testId}']`;

describe("awe-react-client/test/js/containers/ScreenSkeletonTest.jsx", () => {
  function renderInRouter(Container) {
    return renderWithProviders(
      <MemoryRouter initialEntries={["/screen"]}>
        <Routes>
          <Route path="/:screenId" element={<Container/>}/>
          <Route path="/:subScreenId" element={<Container/>}/>
        </Routes>
      </MemoryRouter>,
      {preloadedState: {settings: DEFAULT_SETTINGS}}
    );
  }

  // The first screen of the page shows the splash animation instead (InitialLoadSplashTest)
  beforeAll(endInitialLoad);

  beforeEach(() => {
    // The screen never arrives: the view stays loading
    global.fetch = jest.fn(() => new Promise(() => {}));
    window.fetch = global.fetch;
  });

  afterEach(() => {
    cleanup();
    delete global.fetch;
    delete window.fetch;
  });

  it.each([
    ["ViewContainer", ViewContainer],
    ["SubViewContainer", SubViewContainer]
  ])("shows a busy screen skeleton instead of the spinner while %s is loading", (_name, Container) => {
    const {container} = renderInRouter(Container);

    const skeleton = container.querySelector(hook("loading-spinner"));
    expect(skeleton).not.toBeNull();
    expect(skeleton.getAttribute("aria-busy")).toBe("true");
    expect(container.querySelector(".p-progress-spinner")).toBeNull();
    // Title bar, criteria row and grid block
    expect(skeleton.querySelector(".screen-skeleton-title .p-skeleton")).not.toBeNull();
    expect(skeleton.querySelectorAll(".screen-skeleton-criteria .p-skeleton").length).toBeGreaterThan(1);
    expect(skeleton.querySelector(".screen-skeleton-grid .p-skeleton")).not.toBeNull();
    // The placeholders are decoration only
    [...skeleton.querySelectorAll(".p-skeleton")].forEach(block =>
      expect(block.closest("[aria-hidden='true']")).not.toBeNull());
  });
});
