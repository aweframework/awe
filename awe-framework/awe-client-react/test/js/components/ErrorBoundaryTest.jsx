import React from "react";
import {fireEvent, render} from "@testing-library/react";
import i18n from "i18next";
import ErrorBoundary from "../../../src/components/ErrorBoundary";

const hook = (testId) => `[data-testid='${testId}']`;

/**
 * Component that throws while rendering when the shared flag is on
 */
const flag = {broken: true};
const Fragile = () => {
  if (flag.broken) {
    throw new Error("Cannot read properties of undefined (reading '$attrs')");
  }
  return <span id="fragile">fine</span>;
};

describe("awe-react-client/test/js/components/ErrorBoundaryTest.jsx", () => {
  let errorSpy;

  beforeEach(() => {
    flag.broken = true;
    // React logs every error caught by a boundary: keep the output clean and check what the boundary logs
    errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => jest.restoreAllMocks());

  it("renders its children when nothing fails", () => {
    flag.broken = false;
    const {container} = render(<ErrorBoundary scope="app"><Fragile/></ErrorBoundary>);

    expect(container.querySelector("#fragile")).not.toBeNull();
    expect(container.querySelector(hook("error-boundary"))).toBeNull();
  });

  it("shows a panel with the message of the error instead of an empty tree", () => {
    const {container} = render(<ErrorBoundary scope="app"><Fragile/></ErrorBoundary>);

    const panel = container.querySelector(hook("error-boundary"));
    expect(panel).not.toBeNull();
    expect(panel.getAttribute("role")).toBe("alert");
    expect(panel.getAttribute("data-scope")).toBe("app");
    expect(container.querySelector(hook("error-boundary-message")).textContent)
      .toBe("Cannot read properties of undefined (reading '$attrs')");
    expect(container.querySelector("#fragile")).toBeNull();
  });

  it("keeps the component stack in the DOM", () => {
    const {container} = render(<ErrorBoundary scope="app"><Fragile/></ErrorBoundary>);

    const details = container.querySelector(hook("error-boundary-details"));
    expect(details).not.toBeNull();
    expect(details.textContent).toContain("Fragile");
  });

  it("logs the error with its component stack", () => {
    render(<ErrorBoundary scope="view"><Fragile/></ErrorBoundary>);

    const call = errorSpy.mock.calls.find(args => String(args[0]).includes("[ErrorBoundary]"));
    expect(call).toBeDefined();
    expect(call[0]).toContain("view");
    expect(call.some(arg => arg instanceof Error && arg.message.includes("$attrs"))).toBe(true);
    expect(call.some(arg => typeof arg === "string" && arg.includes("Fragile"))).toBe(true);
  });

  it("translates the title and the buttons", () => {
    i18n.addResourceBundle("en-GB", "translation", {
      SCREEN_TEXT_ERROR_TITLE: "Oops something went wrong",
      BUTTON_RETRY: "Retry",
      BUTTON_RELOAD: "Reload page"
    }, true, true);
    const {container} = render(<ErrorBoundary scope="app"><Fragile/></ErrorBoundary>);

    expect(container.querySelector(hook("error-boundary-title")).textContent).toBe("Oops something went wrong");
    expect(container.querySelector(hook("error-boundary-retry")).textContent).toBe("Retry");
    expect(container.querySelector(hook("error-boundary-reload")).textContent).toBe("Reload page");
  });

  it("renders the children again when the user retries and the cause is gone", () => {
    const {container} = render(<ErrorBoundary scope="view"><Fragile/></ErrorBoundary>);
    flag.broken = false;

    fireEvent.click(container.querySelector(hook("error-boundary-retry")));

    expect(container.querySelector("#fragile")).not.toBeNull();
    expect(container.querySelector(hook("error-boundary"))).toBeNull();
  });

  it("keeps the panel when the retry fails again", () => {
    const {container} = render(<ErrorBoundary scope="view"><Fragile/></ErrorBoundary>);

    fireEvent.click(container.querySelector(hook("error-boundary-retry")));

    expect(container.querySelector(hook("error-boundary"))).not.toBeNull();
  });

  it("recovers by itself when the reset key changes", () => {
    const {container, rerender} = render(<ErrorBoundary scope="view" resetKey="a"><Fragile/></ErrorBoundary>);
    expect(container.querySelector(hook("error-boundary"))).not.toBeNull();
    flag.broken = false;

    rerender(<ErrorBoundary scope="view" resetKey="a"><Fragile/></ErrorBoundary>);
    expect(container.querySelector(hook("error-boundary"))).not.toBeNull();

    rerender(<ErrorBoundary scope="view" resetKey="b"><Fragile/></ErrorBoundary>);
    expect(container.querySelector("#fragile")).not.toBeNull();
  });

  it("reloads the page from the reload button", () => {
    const reload = jest.fn();
    const {container} = render(<ErrorBoundary scope="app" onReload={reload}><Fragile/></ErrorBoundary>);

    fireEvent.click(container.querySelector(hook("error-boundary-reload")));

    expect(reload).toHaveBeenCalledTimes(1);
  });
});
