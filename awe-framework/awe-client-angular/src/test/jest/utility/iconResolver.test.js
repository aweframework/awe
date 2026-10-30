import fs from "fs";
import path from "path";
import {resolveIcon} from "awe-client-react/utilities/iconResolver";

const ROOT = path.join(__dirname, "..", "..", "..", "..");
const RESOURCES = path.join(ROOT, "src", "main", "resources");
const equivalences = JSON.parse(fs.readFileSync(
  path.join(ROOT, "..", "awe-design-tokens", "icons", "icon-equivalences.json"), "utf8"));
const lucideNames = JSON.parse(fs.readFileSync(
  path.join(ROOT, "..", "awe-design-tokens", "icons", "lucide-names.json"), "utf8"));

const listFiles = (folder, extensions) => fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(folder, entry.name);
  if (entry.isDirectory()) {
    return listFiles(file, extensions);
  }
  return extensions.some(extension => entry.name.endsWith(extension)) ? [file] : [];
});

// Modifier classes of Font Awesome, not icons
const MODIFIERS = new Set(["fa-fw", "fa-spin", "fa-pulse", "fa-lg", "fa-2x", "fa-3x", "fa-4x", "fa-5x", "fa-li", "fa-ul",
  "fa-border", "fa-inverse", "fa-stack", "fa-stack-1x", "fa-stack-2x"]);

// The defaults of the tree grid icons (icon-expand, icon-collapse, icon-leaf), written without the fa- prefix
const TREE_GRID_DEFAULTS = ["fa-plus-square", "fa-minus-square", "fa-dot-circle-o"];

// The Font Awesome icons the AngularJS client writes in its code and templates: as classes, or by name in the icon
// helpers (iconTemplate("name"), iconMarkup("name"), 'name' | aweIcon)
const hardCodedIcons = () => [...new Set([
  ...listFiles(path.join(RESOURCES, "js", "awe"), [".js"]),
  ...listFiles(path.join(RESOURCES, "templates"), [".html", ".stg"])
].flatMap(file => {
  const source = fs.readFileSync(file, "utf8");
  return [
    ...[...source.matchAll(/\bfa-[a-z0-9-]+/g)].map(match => match[0]),
    // A Lucide icon written by its own name ("lucide:name") is kept as it is
    ...[...source.matchAll(/\bicon(?:Template|Markup)\("((?:lucide:)?[a-z0-9-]+)/g)]
      .map(match => (match[1].startsWith("lucide:") ? match[1] : `fa-${match[1]}`)),
    ...[...source.matchAll(/'([a-z0-9-]+)(?: fa-spin)?' \| aweIcon/g)].map(match => `fa-${match[1]}`)
  ];
}))]
  .filter(name => !MODIFIERS.has(name) && !name.startsWith("fa-rotate-") && !name.startsWith("fa-flip-"));

describe("awe-framework/awe-client-angular/src/test/jest/utility/iconResolver", () => {
  it("is shared with the React client", () => {
    expect(resolveIcon("fa-question-circle")).toEqual({family: "lucide", name: "circle-question-mark", classes: ""});
    expect(resolveIcon("mdi:dark_mode")).toEqual({family: "lucide", name: "moon", classes: ""});
    expect(resolveIcon("rebel")).toEqual({family: "fa", name: "rebel", classes: ""});
  });

  it("maps every icon written in the AngularJS client to a Lucide icon or lists it as a fallback", () => {
    const icons = [...hardCodedIcons(), ...TREE_GRID_DEFAULTS];
    expect(icons).toEqual(expect.arrayContaining(["fa-question-circle", "fa-calendar", "fa-clock-o", "fa-eyedropper"]));
    const fallback = new Set(equivalences.fallback.fa);
    const unresolved = icons.filter(icon => {
      const resolved = resolveIcon(icon);
      return resolved?.family !== "lucide" && !(resolved?.family === "fa" && fallback.has(resolved.name));
    });
    expect(unresolved).toEqual([]);
  });

  it("references only Lucide icons of the sprite in the server templates", () => {
    const references = listFiles(path.join(RESOURCES, "templates", "awe"), [".stg"])
      .flatMap(file => [...fs.readFileSync(file, "utf8").matchAll(/lucide-sprite\.svg#([a-z0-9-]+)/g)])
      .map(match => match[1]);
    expect(references).toEqual(expect.arrayContaining(["house", "triangle-alert", "square-pen", "check", "x"]));
    expect(references.filter(name => !lucideNames.icons.includes(name))).toEqual([]);
  });
});
