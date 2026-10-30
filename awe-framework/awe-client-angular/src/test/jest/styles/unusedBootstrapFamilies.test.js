import fs from "fs";
import path from "path";
import {sourceFiles, usedXmlClasses, walk} from "./support/xmlScreens";

// Reading every template, directive and screen of the framework takes a while on a loaded CI runner
jest.setTimeout(30000);

const FRAMEWORK = path.resolve(__dirname, "..", "..", "..", "..", "..");
const COMPAT = path.join(FRAMEWORK, "awe-client-angular", "src", "main", "resources", "tailwind", "compat");

// The vendored Bootstrap 3 families that slice 7 of #755 drops with no compat CSS, because no AWE source writes any of their classes
const DROPPED = {
  pager: ["pager"],
  media: ["media", "media-body", "media-object", "media-heading", "media-list", "media-left", "media-right", "media-middle", "media-bottom"],
  jumbotron: ["jumbotron"],
  carousel: ["carousel", "carousel-inner", "carousel-control", "carousel-indicators", "carousel-caption", "icon-prev", "icon-next"],
  "code helpers": ["pre-scrollable"]
};
// Families that something in the tree still writes: they are served by the compat layer (list-group.css, thumbnails.css, responsive-embed.css) instead of being dropped
const SERVED = {
  "list-group": {classes: ["list-group", "list-group-item"], file: "list-group.css", writtenIn: "awe-modules/awe-notifier/src/main/resources/application/awe-notifier/screen/notification-settings.xml"},
  "responsive-embed": {classes: ["embed-responsive"], file: "responsive-embed.css", writtenIn: "awe-modules/awe-tools/src/main/resources/templates/angular/fileManager.html"},
  thumbnails: {classes: ["thumbnail"], file: "thumbnails.css", writtenIn: "awe-modules/awe-tools/src/main/resources/less/filemanager/angular-filemanager.less"}
};
const GLYPHICON = /^glyphicon(-[a-z0-9-]+)?$/;

const read = file => fs.readFileSync(file, "utf8");
// Every word written inside quotes (a class attribute, an ng-class expression, a template string, an addClass call): a class name is always there
const quotedWords = text => [...text.matchAll(/(["'`])((?:(?!\1)[^\n])*)\1/g)].flatMap(match => match[2].split(/[\s,;:{}()[\]?!&|=+<>]+/)).filter(Boolean);

/**
 * Where the AngularJS client can write a class: the templates (html and StringTemplate groups) and directives that the Tailwind build scans,
 * and the file manager of awe-tools (its own bundle, with the same classes). The XML screens are read apart (usedXmlClasses); Java builders
 * only emit screens.
 * @return {Map<string, string[]>} Files by class token (quoted words of html, js and stg)
 */
const classUses = () => {
  const uses = new Map();
  const add = (token, file) => uses.set(token, [...(uses.get(token) || []), path.relative(FRAMEWORK, file)]);
  const templates = path.join(FRAMEWORK, "awe-client-angular", "src", "main", "resources", "templates");
  const tools = path.join(FRAMEWORK, "awe-modules", "awe-tools", "src", "main", "resources");
  const files = [
    ...sourceFiles().filter(file => /\.(html|js)$/.test(file)),
    ...walk(templates).filter(file => file.endsWith(".stg")),
    ...["templates", "js"].flatMap(folder => walk(path.join(tools, folder))).filter(file => /\.(html|js|stg)$/.test(file))
  ];
  files.forEach(file => quotedWords(read(file)).forEach(token => add(token, file)));
  return uses;
};

describe("awe-framework/awe-client-angular/src/test/jest/styles/unusedBootstrapFamilies (slice 7 of #755)", () => {
  let uses;
  let xmlUses;

  beforeAll(() => {
    uses = classUses();
    xmlUses = usedXmlClasses();
  });

  it("reads the templates, the directives, the file manager and the XML screens", () => {
    expect(uses.get("col-xs-12").length).toBeGreaterThan(5);
    expect(uses.has("panel-body")).toBe(true);
    expect(xmlUses.get("panel-body")).toBeGreaterThan(80);
  });

  Object.entries(DROPPED).forEach(([family, classes]) => {
    it(`is written by no template, directive, tool or XML screen: ${family}`, () => {
      expect(classes.filter(name => uses.has(name) || xmlUses.has(name)).map(name => ({name, files: uses.get(name)}))).toEqual([]);
    });
  });

  it("is written by no template, directive, tool or XML screen: the glyphicon icon font", () => {
    expect([...uses.keys(), ...xmlUses.keys()].filter(name => GLYPHICON.test(name))).toEqual([]);
  });

  it("does not load the Bootstrap LESS in the React client, which has its own styles (its `carousel` class is the PrimeReact widget, styled by AweCarousel.less)", () => {
    const client = path.join(FRAMEWORK, "awe-client-react");
    const sources = [...walk(path.join(client, "src")), ...fs.readdirSync(client).filter(file => /^webpack.*\.js$/.test(file)).map(file => path.join(client, file))]
      .filter(file => /\.(jsx?|tsx?|less|css)$/.test(file));
    expect(sources.length).toBeGreaterThan(50);
    expect(sources.filter(file => /(import|require|@import)[^\n]*(bootstrap-3\.3\.2|pixel-admin|awe-client-angular)/.test(read(file))).map(file => path.relative(client, file))).toEqual([]);
  });

  describe("the families that something still writes (the day that use goes, their compat file can go too)", () => {
    Object.entries(SERVED).forEach(([family, {classes, file, writtenIn}]) => {
      it(`${family}: ${writtenIn} writes ${classes.join(", ")}, so the compat layer serves them (${file})`, () => {
        expect(read(path.join(COMPAT, file))).toMatch(new RegExp(`(^|[^\\w-])\\.${classes[0]}([^\\w-]|$)`));
        expect(read(path.join(FRAMEWORK, writtenIn))).toMatch(new RegExp(`(^|[^\\w-])${classes[0]}([^\\w-]|$)`));
      });
    });
  });
});
