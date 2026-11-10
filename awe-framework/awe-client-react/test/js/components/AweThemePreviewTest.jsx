import React from "react";
import {act, fireEvent, screen, waitFor} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {updateModel} from "../../../src/redux/actions/components";
import {renderWithProviders} from "../test-utils";
import AweThemePreview from "../../../src/components/AweThemePreview";
import {PREVIEW_PALETTE_DELAY, PREVIEW_PALETTE_QUERY} from "../../../src/utilities/themePreview";

/**
 * Theme preview (#949): the values of the theme panel give the appearance of the document of the preview, never the
 * one of the application
 */
describe("awe-client-react/test/js/components/AweThemePreviewTest.jsx", () => {

  const criterion = (id, value) => ({
    address: {view: "base", component: id},
    model: {values: value === undefined ? [] : [{value, label: value, selected: true}]},
    attributes: {id, component: "text"}
  });

  const renderPreview = ({primary = "emerald", primaryHex, surface = "auto", surfaceHex, mode = "light"} = {}) =>
    renderWithProviders(<AweThemePreview id="ThmPrv" label="SCREEN_TEXT_THEME_PREVIEW_SAMPLE" primary="PriPal"
                                         primaryHex="PriHex" surface="SrfPal" surfaceHex="SrfHex" mode="Mde"/>, {
      preloadedState: {
        settings: {...DEFAULT_SETTINGS, token: "token", density: "compact"},
        components: {
          ThmPrv: {address: {view: "base", component: "ThmPrv"}, attributes: {}},
          PriPal: criterion("PriPal", primary),
          PriHex: criterion("PriHex", primaryHex),
          SrfPal: criterion("SrfPal", surface),
          SrfHex: criterion("SrfHex", surfaceHex),
          Mde: criterion("Mde", mode)
        }
      }
    });

  const previewRoot = () => screen.getByTitle("SCREEN_TEXT_THEME_PREVIEW_SAMPLE").contentDocument.documentElement;

  const answer = (rows) => Promise.resolve({ok: true, json: () => Promise.resolve([
    {type: "fill", parameters: {datalist: {rows}}}, {type: "end-load"}])});

  let consoleError;

  beforeEach(() => {
    global.fetch = jest.fn();
    window.fetch = global.fetch;
    consoleError = jest.spyOn(console, "error");
  });

  afterEach(() => {
    expect(consoleError).not.toHaveBeenCalled();
    consoleError.mockRestore();
    delete global.fetch;
    delete window.fetch;
  });

  it("renders the palettes, mode and density of the panel on the preview, not on the application", async () => {
    renderPreview({primary: "indigo", surface: "zinc", mode: "dark"});

    await waitFor(() => expect(previewRoot().getAttribute("data-awe-primary")).toBe("indigo"));
    expect(previewRoot().getAttribute("data-awe-surface")).toBe("zinc");
    expect(previewRoot().getAttribute("data-awe-density")).toBe("compact");
    expect(previewRoot().classList.contains("dark")).toBe(true);
    expect(document.documentElement.hasAttribute("data-awe-primary")).toBe(false);
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("requests the palette of a custom hex and renders its properties on the preview", async () => {
    global.fetch.mockReturnValue(answer([{"--awe-primary-500": "#1A73E8", "--awe-primary-fill": "#1765CC"}]));
    renderPreview({primary: "custom", primaryHex: "#1a73e8"});

    await waitFor(() => expect(previewRoot().getAttribute("data-awe-primary")).toBe("custom"));
    expect(previewRoot().style.getPropertyValue("--awe-primary-500")).toBe("#1A73E8");
    expect(previewRoot().style.getPropertyValue("--awe-primary-fill")).toBe("#1765CC");
    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [url, request] = global.fetch.mock.calls[0];
    expect(url).toContain(`/action/data/${PREVIEW_PALETTE_QUERY}`);
    expect(JSON.parse(request.body)).toEqual({primaryHex: "#1A73E8", surfaceHex: null});
    expect(document.documentElement.style.getPropertyValue("--awe-primary-500")).toBe("");
  });

  it("keeps the last valid preview without a request while the custom hex is invalid or empty", async () => {
    jest.useFakeTimers();
    const {store} = renderPreview({primary: "violet"});
    expect(previewRoot().getAttribute("data-awe-primary")).toBe("violet");

    for (const hex of ["#12", ""]) {
      act(() => {
        store.dispatch(updateModel({view: "base", component: "PriPal"}, {values: [{value: "custom", selected: true}]}));
        store.dispatch(updateModel({view: "base", component: "PriHex"}, {values: [{value: hex, selected: true}]}));
      });
      act(() => jest.advanceTimersByTime(PREVIEW_PALETTE_DELAY * 2));

      expect(previewRoot().getAttribute("data-awe-primary")).toBe("violet");
    }
    expect(global.fetch).not.toHaveBeenCalled();
    jest.useRealTimers();
  });

  it("follows the device in the system mode and switches the preview alone to light or dark", async () => {
    renderPreview({mode: "system"});
    await waitFor(() => expect(previewRoot().getAttribute("data-awe-primary")).toBe("emerald"));
    expect(previewRoot().classList.contains("light")).toBe(false);
    expect(previewRoot().classList.contains("dark")).toBe(false);

    fireEvent.click(screen.getByText("ENUM_THEME_MODE_DARK"));
    expect(previewRoot().classList.contains("dark")).toBe(true);

    fireEvent.click(screen.getByText("ENUM_THEME_MODE_LIGHT"));
    expect(previewRoot().classList.contains("light")).toBe(true);
    expect(previewRoot().classList.contains("dark")).toBe(false);
    expect(document.documentElement.classList.contains("light")).toBe(false);
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });
});
