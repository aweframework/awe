// AWE no longer ships the JavaScript of Bootstrap 3 (#755, slice 4): its modals, popovers, dropdowns and collapses are
// native. This guards that nothing brings it back, and that nothing relies on its data-api without a native handler.
import "../../main/resources/js/awe/app";
import "../../main/resources/webpack/locals-en-GB.config";
import {DefaultSettings} from "../../main/resources/js/awe/data/options";

const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..", "..", "..");
const JS_ROOT = path.join(ROOT, "src", "main", "resources", "js", "awe");
const ALL_JS_ROOT = path.join(ROOT, "src", "main", "resources", "js");
const TEMPLATES_ROOT = path.join(ROOT, "src", "main", "resources", "templates", "angular");

const listFiles = (directory, extension) => fs.readdirSync(directory, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(directory, entry.name);
  if (entry.isDirectory()) {
    return listFiles(file, extension);
  }
  return file.endsWith(extension) ? [file] : [];
});

const read = file => fs.readFileSync(file, "utf8");
const relative = file => path.relative(ROOT, file);
const sources = () => [...listFiles(JS_ROOT, ".js"), ...listFiles(TEMPLATES_ROOT, ".html")];

describe("awe-framework/awe-client-angular/src/test/jest/bootstrapJsRemoval.spec.js", () => {
  it("does not import the JavaScript of Bootstrap anywhere under js/ (import, require, dynamic import, deep specifiers)", () => {
    // "bootstrap" and "bootstrap/js/modal", but not "bootstrap-tabdrop" or "bootstrap-datepicker"
    const specifier = /(?:\bfrom\s*|\bimport\s*\(?\s*|\brequire\s*\(\s*)["']bootstrap(?:\/[^"']*)?["']/;

    const offenders = listFiles(ALL_JS_ROOT, ".js").filter(file => specifier.test(read(file))).map(relative);

    expect(offenders).toEqual([]);
  });

  it("recognizes the specifiers it forbids", () => {
    const specifier = /(?:\bfrom\s*|\bimport\s*\(?\s*|\brequire\s*\(\s*)["']bootstrap(?:\/[^"']*)?["']/;

    ['import "bootstrap";', 'import "bootstrap/js/modal";', 'import $ from "bootstrap"', 'const b = require("bootstrap/js/dropdown")',
      'await import("bootstrap")', "import 'bootstrap/dist/js/bootstrap'"].forEach(code => expect(specifier.test(code)).toBe(true));
    ['import "bootstrap-tabdrop";', 'import "bootstrap-datepicker";', 'import Slider from "bootstrap-slider"',
      'import "bootstrap-markdown/js/bootstrap-markdown"'].forEach(code => expect(specifier.test(code)).toBe(false));
  });

  it("does not depend on the bootstrap package", () => {
    const manifest = JSON.parse(read(path.join(ROOT, "package.json")));

    expect(Object.keys({...manifest.dependencies, ...manifest.devDependencies})).not.toContain("bootstrap");
  });

  it("does not call a Bootstrap plugin from the sources of AWE", () => {
    const calls = /\.(modal|popover|dropdown|collapse|tab|tooltip|button|alert|scrollspy|affix|carousel)\(\s*['"{]|\$\.fn\.(modal|popover|dropdown|collapse|tab|tooltip)\b/;

    const offenders = sources().filter(file => calls.test(read(file))).map(relative);

    expect(offenders).toEqual([]);
  });

  it("does not listen to the events of the Bootstrap plugins which are gone (popover, collapse, dropdown, tab, tooltip)", () => {
    const events = /\.bs\.(popover|collapse|tab|tooltip|button|alert)\b/;

    const offenders = sources().filter(file => events.test(read(file))).map(relative);

    expect(offenders).toEqual([]);
  });

  it("only relies on the data-api attributes which have a native handler", () => {
    // data-toggle="dropdown" (services/dropdown.js) and data-dismiss="modal" (services/modal.js)
    const toggles = /data-toggle\s*=\s*["']?([^"'\s>]*)|ng-attr-data-toggle\s*=\s*"([^"]*)"/g;
    const dismisses = /data-dismiss\s*=\s*["']([^"']*)["']/g;
    const unsupported = /\bdata-(target|parent|ride|spy|slide|slide-to|content|placement|trigger|original-title)\s*=/;
    const offenders = [];

    // The handlers themselves describe the attributes in their documentation
    sources().filter(file => !/services[\\/](dropdown|modal)\.js$/.test(file)).forEach(file => {
      const text = read(file);
      [...text.matchAll(toggles)]
        .map(match => match[1] || match[2] || "")
        .filter(value => !/dropdown/.test(value))
        .forEach(value => offenders.push(`${relative(file)}: data-toggle ${value}`));
      [...text.matchAll(dismisses)]
        .filter(match => match[1] !== "modal")
        .forEach(match => offenders.push(`${relative(file)}: data-dismiss ${match[1]}`));
      if (unsupported.test(text)) {
        offenders.push(`${relative(file)}: ${unsupported.exec(text)[0]}`);
      }
    });

    expect(offenders).toEqual([]);
  });

  it("handles the attributes it relies on: data-toggle=dropdown and data-dismiss=modal", () => {
    expect(read(path.join(JS_ROOT, "services", "dropdown.js"))).toContain("[data-toggle=\\\"dropdown\\\"]");
    expect(read(path.join(JS_ROOT, "services", "modal.js"))).toContain("[data-dismiss=\\\"modal\\\"]");
  });

  describe("behavior of the native handlers in the application", () => {
    let $modal;

    beforeEach(() => {
      angular.mock.module("aweApplication");
      inject(["AweModal", "$httpBackend", (AweModal, $httpBackend) => {
        $modal = AweModal;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
      }]);
    });

    afterEach(() => {
      document.body.innerHTML = "";
      document.body.className = "";
    });

    it("opens a [data-toggle=dropdown] exactly once per click (no second handler toggles it back)", () => {
      document.body.insertAdjacentHTML("beforeend",
        "<ul><li class='dropdown'><a data-toggle='dropdown'>menu</a><ul class='dropdown-menu'><li><a>item</a></li></ul></li></ul>");
      const item = $("li.dropdown");

      $("[data-toggle='dropdown']").trigger("click");
      expect(item.hasClass("open")).toBe(true);

      $("[data-toggle='dropdown']").trigger("click");
      expect(item.hasClass("open")).toBe(false);
    });

    it("closes the modal with a [data-dismiss=modal] click", () => {
      document.body.insertAdjacentHTML("beforeend",
        "<div class='modal'><div class='modal-dialog'><button class='close' data-dismiss='modal'></button></div></div>");
      const modal = $(".modal");
      $modal.show(modal);
      expect($modal.isShown(modal)).toBe(true);

      modal.find("[data-dismiss='modal']").trigger("click");

      expect($modal.isShown(modal)).toBe(false);
      expect(modal.hasClass("in")).toBe(false);
      expect($(".modal-backdrop").length).toBe(0);
    });

    it("has no Bootstrap plugin loaded", () => {
      ["modal", "popover", "collapse", "tab", "alert", "carousel", "affix", "scrollspy"].forEach(name => expect($.fn[name]).toBeUndefined());
    });
  });
});
