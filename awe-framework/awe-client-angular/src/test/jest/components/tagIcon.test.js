// Icon of a tag (#793): the server template of a tag with a label and an icon writes the decorative icon before the label
import "./lightComponentTestUtils";
import fs from "fs";
import path from "path";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const STG = path.join(__dirname, "../../../main/resources/templates/awe/elements.stg");

/**
 * The markup the tag template writes for the icon of a tag (the $if(e.icon)$ block), with the icon of the tag
 * @param {string} icon Icon of the tag
 * @returns {string} Markup
 */
const iconMarkupOf = icon => {
  const tag = fs.readFileSync(STG, "utf8").split("\n").find(line => line.startsWith("tag("));
  const block = tag.match(/\$if\(e\.icon\)\$(.*?)\$endif\$/)[1];
  return block.replace(/\\"/g, "\"").replace(/\$e\.icon\$/g, icon);
};

describe("tag icon", () => {
  let $compile, $rootScope;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$compile", "$rootScope", "$httpBackend", (_$compile_, _$rootScope_, $httpBackend) => {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
  });

  const compile = icon => {
    const element = $compile(`<h3 class="awe-form-section-title">${iconMarkupOf(icon)}<span>Identity</span></h3>`)($rootScope.$new());
    $rootScope.$digest();
    return element;
  };

  it("is written only for a tag with a label", () => {
    const tag = fs.readFileSync(STG, "utf8").split("\n").find(line => line.startsWith("tag("));
    expect(tag).toMatch(/\$if\(e\.label\)\$\$if\(e\.icon\)\$/);
  });

  it("draws a Lucide icon of the sprite, hidden from assistive technologies, before the label", () => {
    const element = compile("user");

    const icon = element.children().first();
    expect(icon.hasClass("tag-icon")).toBe(true);
    expect(icon.attr("aria-hidden")).toBe("true");
    expect(icon.find("svg.awe-icon use").attr("href")).toMatch(/#user$/);
    expect(icon.find("i.fa").length).toBe(0);
    expect(icon.next().text()).toBe("Identity");
  });

  it("resolves the Font Awesome names and the lucide: prefix to Lucide", () => {
    expect(compile("lucide:key-round").find("svg.awe-icon use").attr("href")).toMatch(/#key-round$/);
    expect(compile("lucide:sliders-horizontal").find("svg.awe-icon use").attr("href")).toMatch(/#sliders-horizontal$/);
    expect(compile("shield").find("svg.awe-icon use").attr("href")).toMatch(/#shield$/);
  });
});
