import fs from "fs";
import path from "path";

const SRC = path.join(__dirname, "../../../src");
const read = (...parts) => fs.readFileSync(path.join(SRC, ...parts), "utf8");

// Body of the first rule whose selector is exactly the given one
const ruleBody = (source, selector) => {
  const escaped = selector.replace(/[>*.:()]/g, "\\$&").replace(/\s+/g, "\\s+");
  const match = source.match(new RegExp(`(?:^|[}\\n])\\s*${escaped}\\s*\\{([^}]*)\\}`));
  return match ? match[1] : null;
};

describe("awe-react-client/test/js/styles/gridHeightStylesTest.js", () => {
  it("keeps a minimum rows area when the container of the grid gives it no height", () => {
    const body = ruleBody(read("components", "AweGrid.less"), ".grid-container .p-datatable-wrapper");

    // A grid in a dialog or a non-expandible panel has no height to fill: its rows area would measure 0px
    expect(body).not.toBeNull();
    expect(body).toMatch(/min-height:\s*var\(--awe-grid-min-body-height\);/);
  });

  // A panel body that is also expandible is a flex column: these rules would narrow its lines and fight the expand rules
  const PANEL_BODY = ".panel-body:not(.expandible-vertical):not(.expandible-horizontal)";

  it("lays out the lines of a panel body at their natural height", () => {
    const body = ruleBody(read("main.css"), PANEL_BODY);

    expect(body).not.toBeNull();
    expect(body).toMatch(/align-content:\s*flex-start;/);
  });

  it("gives an expanding child of a panel body a full line", () => {
    const body = ruleBody(read("main.css"), `${PANEL_BODY} > .expand`);

    expect(body).not.toBeNull();
    expect(body).toMatch(/flex:\s*1 1 100%;/);
  });

  it("leaves the layout of an expandible panel body to the expandible rules", () => {
    const body = ruleBody(read("main.css"), ".panel-body");

    expect(body).not.toBeNull();
    expect(body).not.toMatch(/align-content/);
    expect(ruleBody(read("main.css"), ".panel-body > .expand")).toBeNull();
  });
});
