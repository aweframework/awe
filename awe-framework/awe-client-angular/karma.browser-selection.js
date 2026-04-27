const IGNORED_BROWSERS = ["IE", "PhantomJS"];
const PREFERRED_BROWSERS = [
  "ChromeHeadless",
  "Chrome",
  "FirefoxHeadless",
  "Firefox",
  "SafariNative",
  "Safari",
  "Edge",
  "Opera"
];

function normalizeBrowser(browser) {
  return browser === "Safari" ? "SafariNative" : browser;
}

function filterSupportedBrowsers(availableBrowsers = []) {
  return availableBrowsers
    .map(normalizeBrowser)
    .filter((browser) => !IGNORED_BROWSERS.includes(browser));
}

function selectBrowsers(availableBrowsers = []) {
  const supportedBrowsers = filterSupportedBrowsers(availableBrowsers);

  if (!supportedBrowsers.length) {
    return supportedBrowsers;
  }

  const preferredBrowser = PREFERRED_BROWSERS.find((browser) => supportedBrowsers.includes(browser));

  return preferredBrowser ? [preferredBrowser] : [supportedBrowsers[0]];
}

module.exports = {
  filterSupportedBrowsers,
  selectBrowsers
};
