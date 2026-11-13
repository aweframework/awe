import fs from "fs";
import path from "path";
import {LESS_DIR} from "./support/compileStyles";

// Slice 9 of #755: the Bootstrap 3 rules that stay in the build (variables, mixins, grid columns and the unlayered leftovers) live in AWE's own
// less/legacy folder, with the Bootstrap MIT notice. Nothing is left under libs/bootstrap-3.3.2 and no LESS import points at a Bootstrap path.
const LEGACY = path.join(LESS_DIR, "legacy");
const lessFiles = dir => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => (entry.isDirectory() ? lessFiles(path.join(dir, entry.name)) : [path.join(dir, entry.name)])).filter(file => file.endsWith(".less"));

describe("legacy Bootstrap 3 LESS", () => {
  it("is not under libs/bootstrap-3.3.2 any more", () => {
    expect(fs.existsSync(path.join(LESS_DIR, "libs", "bootstrap-3.3.2"))).toBe(false);
  });

  it("has the entry, the variables, the mixins, the grid columns and the leftovers", () => {
    ["legacy.less", "variables.less", "mixins.less", "grid-columns.less", "forms-leftovers.less", "type-leftovers.less"].forEach(file => expect({file, exists: fs.existsSync(path.join(LEGACY, file))}).toEqual({file, exists: true}));
    expect(fs.readdirSync(path.join(LEGACY, "mixins")).length).toBe(29);
    expect(fs.readdirSync(LEGACY).filter(file => file.endsWith("-leftovers.less"))).toHaveLength(28);
  });

  it("is imported by awe.less, and no LESS file imports a Bootstrap 3 path", () => {
    const awe = fs.readFileSync(path.join(LESS_DIR, "awe.less"), "utf8");
    expect(awe).toContain('@import "./legacy/legacy.less";');
    expect(awe).not.toMatch(/bootstrap-3\.3\.2|libs\/bootstrap["/]/);
    expect(lessFiles(LESS_DIR).filter(file => /@import[^;]*bootstrap-3\.3\.2/.test(fs.readFileSync(file, "utf8")))).toEqual([]);
  });

  it("keeps the Bootstrap MIT notice with the code", () => {
    expect(fs.readFileSync(path.join(LEGACY, "legacy.less"), "utf8")).toMatch(/Bootstrap v3\.3\.2[\s\S]*MIT/);
    expect(fs.readFileSync(path.join(LEGACY, "LICENSE-bootstrap.txt"), "utf8")).toMatch(/The MIT License \(MIT\)[\s\S]*Twitter, Inc\./);
  });
});
