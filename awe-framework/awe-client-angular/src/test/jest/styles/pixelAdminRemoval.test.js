import fs from "fs";
import path from "path";
import {LESS_DIR} from "./support/compileStyles";

// The last step of #755: pixel-admin is no longer a vendored library. The LESS files of it that the AWE build reaches live in AWE's own
// less/legacy/pixel-admin folder; the demo, pages, rtl, the widgets and the images nobody reaches are deleted.
const FOLDER = path.join(LESS_DIR, "legacy", "pixel-admin");
const lessFiles = dir => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => (entry.isDirectory() ? lessFiles(path.join(dir, entry.name)) : [path.join(dir, entry.name)])).filter(file => file.endsWith(".less"));
const imports = file => [...fs.readFileSync(file, "utf8").matchAll(/^\s*@import\s*(?:\([^)]*\)\s*)?["']([^"']+)["']/gm)].map(match => match[1]);

describe("pixel-admin LESS", () => {
  it("is not under libs/pixel-admin-less any more", () => {
    expect(fs.existsSync(path.join(LESS_DIR, "libs", "pixel-admin-less"))).toBe(false);
  });

  it("is imported by awe.less from less/legacy/pixel-admin, and no LESS file imports a pixel-admin library path", () => {
    const awe = fs.readFileSync(path.join(LESS_DIR, "awe.less"), "utf8");
    expect(awe).toContain('@import "./legacy/pixel-admin/pixel-admin";');
    expect(awe).toContain('@import "./legacy/pixel-admin/widgets/notifications";');
    expect(awe).not.toMatch(/pixel-admin-less/);
    expect(lessFiles(LESS_DIR).filter(file => imports(file).some(target => /pixel-admin-less/.test(target)))).toEqual([]);
  });

  it("keeps only the files that the build imports: no demo, pages, rtl nor unused widgets, and every import resolves", () => {
    const reached = new Set();
    const walk = file => {
      if (reached.has(file)) return;
      reached.add(file);
      imports(file).filter(target => target.startsWith(".")).forEach(target => walk(path.resolve(path.dirname(file), /\.less$/.test(target) ? target : `${target}.less`)));
    };
    ["pixel-admin.less", path.join("widgets", "notifications.less")].forEach(entry => walk(path.join(FOLDER, entry)));
    reached.forEach(file => expect({file: path.relative(FOLDER, file), exists: fs.existsSync(file)}).toEqual({file: path.relative(FOLDER, file), exists: true}));
    expect(lessFiles(FOLDER).filter(file => !reached.has(file)).map(file => path.relative(FOLDER, file))).toEqual([]);
    ["demo.less", "pages", "rtl"].forEach(name => expect({name, exists: fs.existsSync(path.join(FOLDER, name))}).toEqual({name, exists: false}));
  });

  it("keeps only the images that a compiled rule points at (the IE blank image of the mixin and the editable spinner)", () => {
    const images = dir => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => (entry.isDirectory() ? images(path.join(dir, entry.name)) : [path.relative(FOLDER, path.join(dir, entry.name)).split(path.sep).join("/")]));
    expect(images(path.join(FOLDER, "images")).sort()).toEqual(["images/pixel-admin/blank.png", "images/plugins/bootstrap-editable/loading.gif"]);
  });

  it("keeps a note about where the files come from", () => {
    expect(fs.readFileSync(path.join(FOLDER, "pixel-admin.less"), "utf8")).toMatch(/Pixel Admin/);
    expect(fs.readFileSync(path.join(FOLDER, "NOTICE-pixel-admin.txt"), "utf8")).toMatch(/Pixel Admin/);
  });
});
