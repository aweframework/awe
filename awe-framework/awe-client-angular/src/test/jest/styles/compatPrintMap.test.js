import fs from "fs";
import path from "path";
import {compileTailwind, getRules, TAILWIND_DIR} from "./support/compileTailwind";

// Compiling Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

const MAP_IMAGES = [".leaflet-container .leaflet-marker-pane img", ".leaflet-container .leaflet-shadow-pane img", ".leaflet-container .leaflet-tile-pane img"];

describe("awe-framework/awe-client-angular/src/test/jest/styles/compat print map", () => {
  let compat;

  beforeAll(async () => {
    compat = getRules(await compileTailwind());
  });

  // The compat print rule `img { max-width: 100% !important }` is layered, and a layered !important beats the unlayered one of leaflet.css
  // (`max-width: none !important`): the printed map images would take the width of their empty container. The restore has to be layered too,
  // where its specificity decides
  it("keeps the Leaflet images of the map at max-width none when printing, in the layer of the print rule that would shrink them", () => {
    MAP_IMAGES.forEach(selector => {
      const rules = compat.filter(rule => rule.selector === selector && rule.media.includes("print"));
      expect({selector, declarations: rules.map(rule => rule.declarations["max-width"])}).toEqual({selector, declarations: ["none !important"]});
    });
  });

  it("is imported right after print.css, where the print rule it restores is compiled", () => {
    const imports = [...fs.readFileSync(path.join(TAILWIND_DIR, "compat", "index.css"), "utf8").matchAll(/^@import\s+"\.\/([\w.-]+\.css)";/gm)].map(match => match[1]);
    expect(imports.indexOf("print-map.css")).toBe(imports.indexOf("print.css") + 1);
  });
});
