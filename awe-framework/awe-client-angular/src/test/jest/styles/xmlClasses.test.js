import fs from "fs";
import path from "path";
import {compileStyles} from "./support/compileStyles";
import {compileTailwind} from "./support/compileTailwind";
import {usedXmlClasses, xmlScreens} from "./support/xmlScreens";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

// The screens of the React client (application/awe-react) are not read: the React client has its own styles.
// Classes that the XML screens write in a `style` attribute and that have no CSS rule at all: names the JavaScript reads,
// markers for the tests and for the products, and classes a product stylesheet provides. Not a place to hide a class
// that Bootstrap serves: the test fails when a listed class is served again or no longer used
const MARKERS = JSON.parse(fs.readFileSync(path.join(__dirname, "support", "xmlClassMarkers.json"), "utf8"));
const GRID_CLASS = /^col-(xs|sm|md|lg)-(?:(?:offset|push|pull)-)?\d+$/;

const classesOf = css => new Set([...css.replace(/\/\*[\s\S]*?\*\//g, "").matchAll(/\.(-?[_a-zA-Z][\w-]*)/g)].map(match => match[1]));

describe("awe-framework/awe-client-angular/src/test/jest/styles/xmlClasses", () => {
  let used;
  let vendored;
  let compat;

  beforeAll(async () => {
    vendored = classesOf(await compileStyles());
    compat = classesOf(await compileTailwind());
    used = usedXmlClasses();
  });

  it("reads the XML screens of the framework, the modules and the test applications", () => {
    expect(xmlScreens().length).toBeGreaterThan(100);
    expect(used.size).toBeGreaterThan(200);
    expect(used.get("col-xs-12")).toBeGreaterThan(300);
    expect(used.get("panel-body")).toBeGreaterThan(80);
  });

  it("serves every class of the XML screens, from the compat layer or from the vendored CSS (so removing a vendored rule cannot silently drop one)", () => {
    const unserved = [...used.keys()].filter(token => !vendored.has(token) && !compat.has(token) && !MARKERS.includes(token));
    expect(unserved).toEqual([]);
  });

  it("keeps the marker list honest: every marker is used and has no CSS rule", () => {
    const stale = MARKERS.filter(token => !used.has(token) || vendored.has(token) || compat.has(token));
    expect(stale).toEqual([]);
  });

  it("implements in the compat layer every grid class that the XML screens use", () => {
    const missing = [...used.keys()].filter(token => GRID_CLASS.test(token) && !compat.has(token));
    expect(missing).toEqual([]);
  });
});
