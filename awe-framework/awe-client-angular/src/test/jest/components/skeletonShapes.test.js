// Shaped loading skeletons for criteria, buttons and charts, and the dimmed reload of the grids
import fs from "fs";
import path from "path";
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

const SERVICES = path.join(__dirname, "../../../main/resources/js/awe/services");
const TEMPLATES = path.join(__dirname, "../../../main/resources/templates/angular");

describe("awe-framework/awe-client-angular/src/test/jest/components/skeletonShapes", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $storage;
  let ServerData;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "Storage", "ServerData", (_$rootScope_, _$compile_, _$httpBackend_, _$storage_, _ServerData_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $storage = _$storage_;
      ServerData = _ServerData_;
      $rootScope.view = "base";
      $rootScope.context = "screen";
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
    jest.spyOn($storage, "get").mockReturnValue({base: {}});
    jest.spyOn(ServerData, "preloadAngularTemplate").mockImplementation((a, fn) => fn("<i class='custom-icon'></i>"));
  });

  const compile = (html, scopeValues = {}) => {
    const scope = Object.assign($rootScope.$new(), scopeValues);
    const element = $compile(html)(scope);
    $rootScope.$digest();
    return {element, scope};
  };

  describe("criteria", () => {
    const CRITERION = icon => `<div class="criterion"><label class="control-label">Name</label>
      <div class="input"><input class="form-control"/><awe-loader class="loader" skeleton="field" icon-loader="${icon}"></awe-loader></div></div>`;

    it.each(["spinner", "", "skeleton"])("draws the field skeleton with the loader hook when the icon is '%s'", icon => {
      const {element} = compile(CRITERION(icon));

      const loader = element.find("awe-loader");
      expect(ServerData.preloadAngularTemplate).not.toHaveBeenCalled();
      expect(loader.attr("data-testid")).toBe("loader");
      expect(loader.hasClass("skeleton-loader")).toBe(true);
      expect(loader.find(".skeleton-block .skeleton-bar").length).toBe(1);
    });

    it("masks the label of the criterion while the skeleton shows and restores it when the loader goes away", () => {
      const {element, scope} = compile(CRITERION("spinner"));
      expect(element.hasClass("skeleton-pending")).toBe(true);

      element.find("awe-loader").scope().$destroy();
      scope.$destroy();

      expect(element.hasClass("skeleton-pending")).toBe(false);
    });

    it("keeps the criterion masked until the last of its loaders goes away", () => {
      const {element} = compile(`<div class="criterion"><label class="control-label">Range</label>
        <div class="input"><span class="from"><awe-loader skeleton="field" icon-loader="spinner"></awe-loader></span>
        <span class="to"><awe-loader skeleton="field" icon-loader="spinner"></awe-loader></span></div></div>`);
      const loaders = element.find("awe-loader");

      angular.element(loaders[0]).remove();
      expect(element.hasClass("skeleton-pending")).toBe(true);

      angular.element(loaders[1]).remove();
      expect(element.hasClass("skeleton-pending")).toBe(false);
    });

    it("keeps a custom loading icon: no skeleton and the criterion is not masked", () => {
      const {element} = compile(CRITERION("circle"));

      expect(ServerData.preloadAngularTemplate).toHaveBeenCalled();
      expect(element.find("awe-loader").hasClass("skeleton-loader")).toBe(false);
      expect(element.hasClass("skeleton-pending")).toBe(false);
    });

    it("draws a box and a label for checkboxes and a round box for radios", () => {
      const checkbox = compile(`<label class="criterion"><awe-loader class="loader" skeleton="checkbox" icon-loader="spinner"></awe-loader></label>`).element;
      const radio = compile(`<label class="criterion"><awe-loader class="loader" skeleton="radio" icon-loader="spinner"></awe-loader></label>`).element;

      expect(checkbox.find(".skeleton-check").length).toBe(1);
      expect(checkbox.find(".skeleton-check").hasClass("skeleton-radio")).toBe(false);
      expect(radio.find(".skeleton-check.skeleton-radio").length).toBe(1);
      expect(radio.find(".skeleton-bar").length).toBe(2);
    });

    it("declares the skeleton in every criterion loader of the templates", () => {
      const sources = ["text", "numeric", "selector", "dateTime", "dateRange", "criterion", "checkboxRadio"]
        .map(name => fs.readFileSync(path.join(SERVICES, `${name}.js`), "utf8"))
        .concat(["input/checkbox", "input/radio", "input/markdown-editor"].map(name => fs.readFileSync(path.join(TEMPLATES, `${name}.html`), "utf8")));

      const loaders = sources.join("\n").match(/<awe-loader class="loader"[^>]*controller\.loading[^>]*>/g)
        .filter(loader => !loader.includes("component.controller.loading"));

      expect(loaders.length).toBeGreaterThan(15);
      loaders.forEach(loader => expect(loader).toMatch(/skeleton="(field|checkbox|radio)"/));
    });
  });

  describe("buttons", () => {
    const BUTTON = icon => `<div class="button-group"><button class="btn"><span class="button-text">Accept</span></button>
      <awe-loader class="loader button-loader" skeleton="button" icon-loader="${icon}"></awe-loader></div>`;

    it("masks the button at the width of its label and keeps the loader hook", () => {
      const {element} = compile(BUTTON("spinner"));

      expect(element.hasClass("skeleton-pending")).toBe(true);
      expect(element.find("button .button-text").text()).toBe("Accept");
      expect(element.find("awe-loader").attr("data-testid")).toBe("loader");
      expect(element.find("awe-loader").hasClass("skeleton-hook")).toBe(true);
    });

    it("restores the button when the loader goes away", () => {
      const {element, scope} = compile(BUTTON("spinner"));

      scope.$destroy();

      expect(element.hasClass("skeleton-pending")).toBe(false);
    });

    it("keeps a custom loading icon", () => {
      const {element} = compile(BUTTON("circle"));

      expect(element.hasClass("skeleton-pending")).toBe(false);
      expect(element.find("awe-loader").hasClass("skeleton-hook")).toBe(false);
    });

    it("declares the loader in the button template", () => {
      expect(fs.readFileSync(path.join(SERVICES, "button.js"), "utf8")).toMatch(/<awe-loader[^>]*skeleton="button"[^>]*controller\.loading/);
    });
  });

  describe("charts", () => {
    const CHART = (type, icon = "spinner") => compile(`<div><awe-loader class="loader chart-loader" skeleton="chart" icon-loader="${icon}"></awe-loader></div>`,
      {controller: {chartModel: type ? {chart: {type}} : {}}}).element;

    it.each(["column", "bar", undefined])("hints a bar chart for %s charts", type => {
      const element = CHART(type);

      expect(element.find(".skeleton-chart.skeleton-chart-bars .skeleton-bar").length).toBe(7);
      expect(element.find("awe-loader").attr("data-testid")).toBe("loader");
    });

    it.each(["line", "area", "spline", "areaspline"])("hints a trend for %s charts", type => {
      expect(CHART(type).find(".skeleton-chart.skeleton-chart-trend .skeleton-bar").length).toBe(1);
    });

    it("hints a pie for pie charts", () => {
      expect(CHART("pie").find(".skeleton-chart.skeleton-chart-pie .skeleton-bar").length).toBe(1);
    });

    it("reads the type from the first series when the chart has none", () => {
      const element = compile(`<div><awe-loader class="loader chart-loader" skeleton="chart" icon-loader="spinner"></awe-loader></div>`,
        {controller: {chartModel: {series: [{type: "pie"}]}}}).element;

      expect(element.find(".skeleton-chart-pie").length).toBe(1);
    });

    it("keeps a custom loading icon", () => {
      const element = CHART("pie", "circle");

      expect(element.find(".skeleton-chart").length).toBe(0);
    });

    it("declares the loader in the chart template", () => {
      expect(fs.readFileSync(path.join(TEMPLATES, "chart.html"), "utf8")).toMatch(/<awe-loader[^>]*skeleton="chart"/);
    });
  });

  describe("grid reload", () => {
    const RELOAD = icon => `<div class="grid" aria-busy="true"><awe-loader class="loader grid-loader" skeleton="reload" data-testid="grid-loader" icon-loader="${icon}"></awe-loader></div>`;

    it("dims the rows of the grid instead of drawing a spinner and keeps the grid-loader hook", () => {
      const {element} = compile(RELOAD("skeleton"));

      const loader = element.find("awe-loader");
      expect(ServerData.preloadAngularTemplate).not.toHaveBeenCalled();
      expect(element.hasClass("grid-reloading")).toBe(true);
      expect(element.attr("aria-busy")).toBe("true");
      expect(loader.attr("data-testid")).toBe("grid-loader");
      expect(loader.hasClass("grid-reload")).toBe(true);
      expect(loader.find(".skeleton-bar").length).toBe(0);
    });

    it("restores the rows when the reload ends", () => {
      const {element, scope} = compile(RELOAD("skeleton"));

      scope.$destroy();

      expect(element.hasClass("grid-reloading")).toBe(false);
    });

    it("keeps the icon of a grid with a custom icon-loading", () => {
      const {element} = compile(RELOAD("circle"));

      expect(ServerData.preloadAngularTemplate).toHaveBeenCalled();
      expect(element.hasClass("grid-reloading")).toBe(false);
      expect(element.find("awe-loader").hasClass("grid-reload")).toBe(false);
    });

    it.each(["grid.html", "treegrid.html"])("selects the loader of the %s template by the rows of the grid", template => {
      const html = fs.readFileSync(path.join(TEMPLATES, template), "utf8");

      expect(html).toMatch(/<awe-grid-skeleton[^>]*ng-if="[^"]*iconLoader === 'skeleton' && !model\.values\.length"/);
      expect(html).toMatch(/<awe-loader[^>]*skeleton="reload"[^>]*ng-if="[^"]*iconLoader === 'skeleton' && model\.values\.length"/);
      expect(html).toMatch(/<awe-loader[^>]*ng-if="[^"]*iconLoader !== 'skeleton'"/);
    });
  });
});
