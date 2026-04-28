const { filterSupportedBrowsers, selectBrowsers } = require("../../../../karma.browser-selection");

describe("karma browser selection", () => {
  it("should normalize Safari and ignore unsupported browsers", () => {
    expect(filterSupportedBrowsers(["Safari", "IE", "ChromeHeadless", "PhantomJS"]))
      .toEqual(["SafariNative", "ChromeHeadless"]);
  });

  it("should keep the first preferred supported browser only", () => {
    expect(selectBrowsers(["Safari", "ChromeHeadless", "FirefoxHeadless"]))
      .toEqual(["ChromeHeadless"]);
  });

  it("should fall back to SafariNative when it is the only supported browser", () => {
    expect(selectBrowsers(["Safari", "IE"]))
      .toEqual(["SafariNative"]);
  });
});
