const fs = require("fs");
const path = require("path");

const PAGES = path.resolve(__dirname, "../../../main/resources/less/awe/pages");
const REACT_LOGOS = path.resolve(__dirname, "../../../../../awe-client-react/src/assets/images/logos");

// The brand slot of the AngularJS home (#885), on par with the React shell: the mark with its own colours and the
// wordmark as a mask filled with the emphasis text colour, so the logo reads in light and dark mode
describe("Home logo (#885)", () => {
  const less = fs.readFileSync(path.join(PAGES, "home.less"), "utf8");
  const rule = selector => {
    const start = less.indexOf(`${selector} {`);
    return start < 0 ? "" : less.slice(start, less.indexOf("}", start));
  };

  it("draws the mark as the background of the logo, at the header logo height", () => {
    const logo = rule(".navbar-header h1");
    expect(logo).toMatch(/background: url\('images\/logo\/awe-logo-mark\.svg'\) no-repeat left center \/ auto 100%;/);
    expect(logo).toMatch(/height: var\(--awe-header-logo-height\);/);
    expect(logo).toMatch(/aspect-ratio: 1160\.2 \/ 360;/);
    expect(logo).not.toMatch(/drop-shadow/);
  });

  it("fills the wordmark with the emphasis text colour", () => {
    const wordmark = rule(".navbar-header h1::after");
    expect(wordmark).toMatch(/background-color: var\(--awe-color-text-emphasis\);/);
    expect(wordmark).toMatch(/mask: url\('images\/logo\/awe-logo-wordmark\.svg'\) no-repeat left center \/ auto 100%;/);
  });

  it("hides the wordmark when the application configures its own navigation bar logo", () => {
    // styles.css is a Thymeleaf text template: the block only renders when the logo is not the AWE mark
    expect(less).toMatch(/\/\*\[# th:unless="\$\{#strings\.endsWith\(navbarLogo, 'awe-logo-mark\.svg'\)\}"\]\*\/\s*\.navbar-header h1::after \{\s*display: none;\s*\}\s*\/\*\[\/\]\*\//);
  });

  it("publishes the mark where the default navigation bar logo property points", () => {
    const images = path.resolve(__dirname, "../../../main/resources/images/logo/awe-logo-mark.svg");
    expect(fs.readFileSync(images, "utf8")).toBe(fs.readFileSync(path.join(REACT_LOGOS, "awe-logo-mark.svg"), "utf8"));
  });

  it("uses the same artwork as the React shell", () => {
    for (const file of ["awe-logo-mark.svg", "awe-logo-wordmark.svg"]) {
      expect(fs.readFileSync(path.join(PAGES, "images/logo", file), "utf8"))
        .toBe(fs.readFileSync(path.join(REACT_LOGOS, file), "utf8"));
    }
  });
});
