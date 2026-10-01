import React from "react";
import { act, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import MessageContainer from "../../../src/containers/MessageContainer";
import SubViewContainer from "../../../src/containers/SubViewContainer";
import ViewContainer from "../../../src/containers/ViewContainer";
import { MemoryRouter, Route, Routes } from "react-router";

const hook = (testId) => `[data-testid='${testId}']`;

describe("awe-react-client/test/js/containers/TestIdsContainersTest.jsx", () => {
  beforeEach(() => {
    jest.spyOn(console, "warn").mockImplementation(() => {});
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  const message = (severity, id) => ({
    severity, summary: `Title ${id}`, detail: `Detail ${id}`, sticky: true, life: 0, id, closable: true
  });

  describe("alerts", () => {
    it.each([
      ["success", "success"],
      ["info", "info"],
      ["warn", "warning"],
      ["error", "danger"]
    ])("exposes a %s message as a %s alert", async (severity, type) => {
      renderWithProviders(<MessageContainer />, {
        preloadedState: { settings: DEFAULT_SETTINGS, messages: { showing: [message(severity, 1)] } }
      });

      await waitFor(() => expect(document.querySelector(hook("alert"))).not.toBeNull());
      const alert = document.querySelector(hook("alert"));
      expect(alert.getAttribute("data-type")).toBe(type);
      expect(alert.querySelector(hook("alert-title")).textContent).toBe("Title 1");
      expect(alert.querySelector(hook("alert-message")).textContent).toBe("Detail 1");
      expect(alert.querySelector(hook("alert-close"))).not.toBeNull();
    });

    it("closes an alert with its close icon", async () => {
      renderWithProviders(<MessageContainer />, {
        preloadedState: { settings: DEFAULT_SETTINGS, messages: { showing: [message("error", 2)] } }
      });
      await waitFor(() => expect(document.querySelector(hook("alert-close"))).not.toBeNull());

      await act(async () => {
        fireEvent.click(document.querySelector(hook("alert-close")));
      });

      await waitFor(() => expect(document.querySelector(hook("alert"))).toBeNull());
    });
  });

  describe("confirm dialog", () => {
    it("exposes the dialog and its buttons", () => {
      renderWithProviders(<MessageContainer />, {
        preloadedState: {
          settings: DEFAULT_SETTINGS,
          messages: { showing: [], confirm: { title: "Confirm", message: "Sure?", action: {}, visible: true } }
        }
      });

      const dialog = document.querySelector(hook("confirm-dialog"));
      expect(dialog).not.toBeNull();
      expect(dialog.querySelector(hook("confirm-accept")).id).toBe("confirm-accept");
      expect(dialog.querySelector(hook("confirm-cancel")).id).toBe("confirm-cancel");
    });

    it("does not render the dialog without a confirmation", () => {
      renderWithProviders(<MessageContainer />, {
        preloadedState: { settings: DEFAULT_SETTINGS, messages: { showing: [] } }
      });

      expect(document.querySelector(hook("confirm-dialog"))).toBeNull();
    });
  });

  describe("loaders", () => {
    function renderInRouter(Container) {
      return renderWithProviders(
        <MemoryRouter initialEntries={["/screen"]}>
          <Routes>
            <Route path="/:screenId" element={<Container />} />
            <Route path="/:subScreenId" element={<Container />} />
          </Routes>
        </MemoryRouter>,
        { preloadedState: { settings: DEFAULT_SETTINGS } }
      );
    }

    beforeEach(() => {
      global.fetch = jest.fn(() => new Promise(() => {}));
      window.fetch = global.fetch;
    });

    afterEach(() => {
      delete global.fetch;
      delete window.fetch;
    });

    it.each([
      ["ViewContainer", ViewContainer],
      ["SubViewContainer", SubViewContainer]
    ])("exposes the spinner while %s is loading", (_name, Container) => {
      const { container } = renderInRouter(Container);

      const spinner = container.querySelector(hook("loading-spinner"));
      expect(spinner).not.toBeNull();
      expect(spinner.classList.contains("p-progress-spinner")).toBe(true);
    });
  });
});
