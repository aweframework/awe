import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";
import {PREVIEW_PALETTE_DELAY, PREVIEW_PALETTE_QUERY} from "awe-client-react/utilities/themePreview";

/**
 * Theme preview (#949): the values of the theme panel give the appearance of the document of the preview, never the
 * one of the application, as in the React engine
 */
describe("awe-framework/awe-client-angular/src/test/jest/components/themePreview.test.js", () => {
  let refs, $timeout, models, httpRequest;

  const compile = ({primary = "emerald", primaryHex, surface = "auto", surfaceHex, mode = "light"} = {}) => {
    models = {PriPal: primary, PriHex: primaryHex, SrfPal: surface, SrfHex: surfaceHex, Mde: mode};
    stubPanelComponent(refs, {values: []}, createPanelController("ThmPrv", "theme-preview", {
      label: "SCREEN_TEXT_THEME_PREVIEW_SAMPLE", style: "col-xs-12", primary: "PriPal", primaryHex: "PriHex",
      surface: "SrfPal", surfaceHex: "SrfHex", mode: "Mde"
    }));
    refs.$control.getAddressModel.mockImplementation(address => address.component in models
      ? {selected: models[address.component], values: []}
      : {values: []});
    const element = refs.$compile("<awe-theme-preview theme-preview-id='ThmPrv'></awe-theme-preview>")(
      refs.$rootScope.$new());
    document.body.appendChild(element[0]);
    refs.$rootScope.$digest();
    return element;
  };
  const change = values => {
    Object.assign(models, values);
    refs.$rootScope.$digest();
  };
  const previewRoot = element => element.find("iframe")[0].contentDocument.documentElement;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["$timeout", "$q", "Ajax", "AweSettings", (_$timeout_, $q, Ajax, $settings) => {
      $timeout = _$timeout_;
      httpRequest = jest.spyOn(Ajax, "httpRequest").mockReturnValue($q.resolve({data: [{type: "fill", parameters: {
        datalist: {rows: [{"--awe-primary-500": "#1A73E8", "--awe-primary-fill": "#1765CC"}]}}}, {type: "end-load"}]}));
      const get = $settings.get.bind($settings);
      jest.spyOn($settings, "get").mockImplementation(name => name === "density" ? "compact" : get(name));
    }]);
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
    document.documentElement.removeAttribute("data-awe-primary");
  });

  it("renders the palettes, mode and density of the panel on the preview, not on the application", () => {
    const element = compile({primary: "indigo", surface: "zinc", mode: "dark"});
    const root = previewRoot(element);

    expect(root.getAttribute("data-awe-primary")).toBe("indigo");
    expect(root.getAttribute("data-awe-surface")).toBe("zinc");
    expect(root.getAttribute("data-awe-density")).toBe("compact");
    expect(root.classList.contains("dark")).toBe(true);
    expect(element.find("iframe").attr("tabindex")).toBe("-1");
    expect(element.find("iframe").attr("title")).toBe("SCREEN_TEXT_THEME_PREVIEW_SAMPLE");
    expect(document.documentElement.hasAttribute("data-awe-primary")).toBe(false);
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    $timeout.flush(PREVIEW_PALETTE_DELAY * 2);
    expect(httpRequest).not.toHaveBeenCalled();
  });

  it("requests the palette of a custom hex and renders its properties on the preview", () => {
    const element = compile({primary: "custom", primaryHex: "#1a73e8"});
    expect(httpRequest).not.toHaveBeenCalled();

    $timeout.flush(PREVIEW_PALETTE_DELAY);

    expect(httpRequest).toHaveBeenCalledTimes(1);
    const [{method, url, data}] = httpRequest.mock.calls[0];
    expect(method).toBe("POST");
    expect(url).toMatch(new RegExp(`/action/data/${PREVIEW_PALETTE_QUERY}$`));
    expect(data).toEqual(expect.objectContaining({primaryHex: "#1A73E8", surfaceHex: null}));
    const root = previewRoot(element);
    expect(root.getAttribute("data-awe-primary")).toBe("custom");
    expect(root.style.getPropertyValue("--awe-primary-500")).toBe("#1A73E8");
    expect(root.style.getPropertyValue("--awe-primary-fill")).toBe("#1765CC");
    expect(document.documentElement.style.getPropertyValue("--awe-primary-500")).toBe("");
  });

  it("keeps the last valid preview without a request while the custom hex is invalid or empty", () => {
    const element = compile({primary: "violet"});

    for (const hex of ["#12", ""]) {
      change({PriPal: "custom", PriHex: hex});
      $timeout.flush(PREVIEW_PALETTE_DELAY * 2);

      expect(previewRoot(element).getAttribute("data-awe-primary")).toBe("violet");
    }
    expect(httpRequest).not.toHaveBeenCalled();
  });

  it("follows the device in the system mode and switches the preview alone to light or dark", () => {
    const element = compile({mode: "system"});
    const root = previewRoot(element);
    const option = mode => element.find(`[option-id='${mode}'] input`);
    expect(root.classList.contains("light")).toBe(false);
    expect(root.classList.contains("dark")).toBe(false);

    option("dark").triggerHandler("click");
    expect(root.classList.contains("dark")).toBe(true);
    expect(element.find("[option-id='dark']").hasClass("active")).toBe(true);

    option("light").triggerHandler("click");
    expect(root.classList.contains("light")).toBe(true);
    expect(root.classList.contains("dark")).toBe(false);
    expect(document.documentElement.classList.contains("light")).toBe(false);
    expect(document.documentElement.classList.contains("dark")).toBe(false);

    change({Mde: "dark"});
    expect(element.find(".awe-theme-preview-mode").length).toBe(0);
  });
});
