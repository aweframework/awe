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
    // The AWE 5 splash: the logo fills with a liquid, and the logo is the one of the page (css/splash.css)
    expect(splash.getAttribute("role")).toBe("status");
    expect(splash.querySelector(".splash-logo .splash-fill .splash-level .splash-cover")).not.toBeNull();
    expect(splash.querySelector(".splash-logo").getAttribute("aria-hidden")).toBe("true");
    expect(splash.querySelector(".splash-label").textContent).toBe("Loading");
    expect(splash.querySelector(".spinner")).toBeNull();
    expect(container.querySelector(".screen-skeleton")).toBeNull();
    expect(container.querySelector(".p-skeleton")).toBeNull();
  });

  it("fills the logo up and fades the splash out when the first screen is ready", async () => {
    // The outro is queued as a microtask: only its fade timer is faked
    jest.useFakeTimers({doNotFake: ["queueMicrotask"]});
    try {
      // The splash of the earlier tests may still be fading out
      const earlier = new Set(document.body.querySelectorAll("#splash"));
      let finishLoad;
      mockLoadScreen.mockReturnValue(new Promise(resolve => {
        finishLoad = resolve;
      }));
      const {container} = render(ViewContainer);

      await act(async () => {
        ViewRegistry.setView("base", {loading: false, structure: []});
        finishLoad();
      });

      // The screen is shown under a copy of the splash that completes the fill and fades out
      expect(container.querySelector("#splash")).toBeNull();
      const outro = [...document.body.querySelectorAll("#splash.splash-done.splash-leaving")]
        .find(splash => !earlier.has(splash));
      expect(outro).toBeDefined();
      expect(outro.getAttribute("aria-hidden")).toBe("true");
      expect(outro.getAttribute("data-testid")).toBeNull();
      expect(outro.getAttribute("aria-busy")).toBeNull();

      act(() => jest.advanceTimersByTime(400));
      expect(outro.isConnected).toBe(false);
    } finally {
      jest.useRealTimers();
    }
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
