import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";
import {TestAttributes, TestIds} from "../../../main/resources/js/awe/data/testIds";

const hook = testId => `[data-testid='${testId}']`;

describe("awe-framework/awe-client-angular/src/test/jest/components/card.test.js", () => {
  let refs, model, settings;

  const compile = (attributes = {}, rows = [], children = "") => {
    model = {values: rows};
    stubPanelComponent(refs, model, createPanelController("Crd", "card", {
      icon: undefined, label: undefined, style: "card-style", ...attributes
    }));
    const element = refs.$compile(`<awe-card card-id='Crd'>${children}</awe-card>`)(refs.$rootScope.$new());
    document.body.appendChild(element[0]);
    refs.$rootScope.$digest();
    return element;
  };
  const getCard = () => $(document.body).find(hook(TestIds.card));

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["AweSettings", (_AweSettings_) => {
      settings = _AweSettings_;
    }]);
    jest.spyOn(settings, "getLanguage").mockReturnValue("en-GB");
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  describe("markup", () => {
    it("is a section labelled by its title, marked with its owner and its style", () => {
      compile({title: "Active customers", value: 1284});
      const card = getCard();

      expect(card.length).toBe(1);
      expect(card[0].tagName).toBe("SECTION");
      expect(card.hasClass("awe-card")).toBe(true);
      expect(card.hasClass("card-style")).toBe(true);
      expect(card.attr("id")).toBe("Crd");
      expect(card.attr(TestAttributes.owner)).toBe("Crd");
      expect(card.attr("aria-labelledby")).toBe("Crd-title");
      expect(card.find("header.awe-card-header .awe-card-heading h3.awe-card-title#Crd-title").text()).toBe("Active customers");
    });

    it("titles the card with its label when it has no title", () => {
      compile({label: "Sales"});

      expect(getCard().find(".awe-card-title").text()).toBe("Sales");
    });

    it("shows the formatted value and its unit", () => {
      compile({value: 1284.5, unit: "€"});
      const value = getCard().find(hook(TestIds.cardValue));

      expect(value.hasClass("awe-card-value")).toBe(true);
      expect(value.find(".awe-card-number").text()).toBe("1,284.5");
      expect(value.find(".awe-card-unit").text()).toBe("€");
    });

    it("formats the value with numberDecimals and with the user language", () => {
      settings.getLanguage.mockReturnValue("es-ES");
      compile({value: 12840, numberDecimals: 2});

      expect(getCard().find(".awe-card-number").text()).toBe("12.840,00");
    });

    it("renders no empty parts", () => {
      compile({title: "Only title"});
      const card = getCard();

      expect(card.find(".awe-card-subtitle").length).toBe(0);
      expect(card.find(".awe-card-icon").length).toBe(0);
      expect(card.find(hook(TestIds.cardValue)).length).toBe(0);
      expect(card.find(".awe-card-unit").length).toBe(0);
      expect(card.find(".awe-card-body").length).toBe(0);
      expect(card.find(hook(TestIds.cardProgress)).length).toBe(0);
      expect(card.find(".awe-card-progress-text").length).toBe(0);
      expect(card.find(".awe-card-description").length).toBe(0);
      expect(card.find(".awe-card-footer").length).toBe(0);
      expect(card.find(hook(TestIds.cardDelta)).length).toBe(0);
    });

    it("shows the subtitle, the description and the footer", () => {
      compile({title: "T", subtitle: "Sub", description: "Desc", footer: "Foot"});
      const card = getCard();

      expect(card.find(".awe-card-heading p.awe-card-subtitle").text()).toBe("Sub");
      expect(card.find("p.awe-card-description").text()).toBe("Desc");
      expect(card.find("footer.awe-card-footer .awe-card-footer-text").text()).toBe("Foot");
      expect(card.find(".awe-card-footer-end").length).toBe(0);
    });

    it("is hidden when visible is false", () => {
      compile({title: "T", visible: false});

      expect(getCard().hasClass("ng-hide")).toBe(true);
    });
  });

  describe("data", () => {
    it("takes the fields of the first row over the attributes", () => {
      compile({title: "Static", value: 1, unit: "€"}, [{title: "From query", value: 2500, unit: ""}, {value: 9}]);
      const card = getCard();

      expect(card.find(".awe-card-title").text()).toBe("From query");
      expect(card.find(".awe-card-number").text()).toBe("2,500");
      expect(card.find(".awe-card-unit").text()).toBe("€");
    });

    it("renders again when the model values change (fill, reload)", () => {
      const element = compile({title: "T", delta: 2}, [{value: 10, caption: "old"}]);
      const component = element.isolateScope().component;
      // The generic API of the criteria would keep the selected row and merge the new one into it
      model.selected = ["10"];

      component.api.updateModelValues({values: [{value: 20, delta: -3}], page: 1, records: 1, total: 1});
      refs.$rootScope.$digest();

      expect(model.values).toEqual([{value: 20, delta: -3}]);
      expect(getCard().find(".awe-card-number").text()).toBe("20");
      expect(getCard().find(".awe-card-caption").length).toBe(0);
      expect(getCard().find(hook(TestIds.cardDelta)).attr("data-trend")).toBe("down");
    });

    it("renders the rows of the fill action", () => {
      compile({title: "T"}, [{value: 10, delta: 5}]);
      model.selected = ["10"];

      refs.$control.changeModelAttribute({view: "base", component: "Crd"}, {values: [{value: 30}], page: 1}, true);
      refs.$rootScope.$digest();

      expect(model.values).toEqual([{value: 30}]);
      expect(getCard().find(".awe-card-number").text()).toBe("30");
      expect(getCard().find(hook(TestIds.cardDelta)).length).toBe(0);
    });

    it("renders again when the controller changes (dependencies)", () => {
      const element = compile({title: "T"});

      element.isolateScope().component.controller.title = "Changed";
      refs.$rootScope.$digest();

      expect(getCard().find(".awe-card-title").text()).toBe("Changed");
    });

    it("renders again when the language changes", () => {
      const element = compile({value: 1284.5});

      settings.getLanguage.mockReturnValue("es-ES");
      element.isolateScope().$broadcast("languageChanged", "es-ES");
      refs.$rootScope.$digest();

      expect(getCard().find(".awe-card-number").text()).toBe("1284,5");
    });
  });

  describe("delta", () => {
    it("shows the arrow, the delta text, the hidden trend label, the trend and the tone", () => {
      compile({title: "T", delta: 4.2});
      const delta = getCard().find(hook(TestIds.cardDelta));

      expect(delta.hasClass("awe-card-delta")).toBe(true);
      expect(delta.hasClass("awe-card-delta-success")).toBe(true);
      expect(delta.attr("data-trend")).toBe("up");
      expect(delta.attr("data-tone")).toBe("success");
      expect(delta.find(".awe-card-delta-arrow").attr("aria-hidden")).toBe("true");
      expect(delta.find(".awe-card-delta-arrow").text()).toBe("↗");
      expect(delta.find(".awe-card-delta-text").text()).toBe("+4.2%");
      expect(delta.find(".sr-only").text()).toBe("SCREEN_TEXT_CARD_TREND_UP");
    });

    it("is unsigned with an explicit trend and takes the tone of good-trend", () => {
      compile({delta: -4.2, trend: "down", goodTrend: "down"});
      const delta = getCard().find(hook(TestIds.cardDelta));

      expect(delta.find(".awe-card-delta-text").text()).toBe("4.2%");
      expect(delta.attr("data-tone")).toBe("success");
    });

    it("is neutral when flat and takes the tone field over everything", () => {
      compile({delta: 0, deltaUnit: "pt"});
      expect(getCard().find(hook(TestIds.cardDelta)).attr("data-tone")).toBe("neutral");
      expect(getCard().find(".awe-card-delta-text").text()).toBe("0 pt");
      document.body.innerHTML = "";

      compile({delta: -3, tone: "info"});
      expect(getCard().find(hook(TestIds.cardDelta)).attr("data-tone")).toBe("info");
    });

    it("goes to the header right without icon, with the caption in the footer", () => {
      compile({title: "T", delta: 4.2, caption: "vs. last month"});
      const card = getCard();

      expect(card.find(`.awe-card-header > ${hook(TestIds.cardDelta)}`).length).toBe(1);
      expect(card.find(`.awe-card-footer ${hook(TestIds.cardDelta)}`).length).toBe(0);
      expect(card.find(".awe-card-footer .awe-card-footer-end .awe-card-caption").text()).toBe("vs. last month");
      expect(card.find(".awe-card-footer-text").length).toBe(0);
    });

    it("goes to the footer right, before the caption, with an icon in the header right", () => {
      compile({title: "T", icon: "user", delta: 4.2, caption: "vs. last month"});
      const card = getCard();
      const icon = card.find(".awe-card-header > .awe-card-icon");
      const end = card.find(".awe-card-footer > .awe-card-footer-end");

      expect(icon.length).toBe(1);
      expect(icon.hasClass("awe-card-icon-primary")).toBe(true);
      expect(icon.attr("aria-hidden")).toBe("true");
      expect(icon.find("use").attr("href")).toBe("icons/lucide-sprite.svg#user");
      expect(card.find(`.awe-card-header ${hook(TestIds.cardDelta)}`).length).toBe(0);
      expect(end.children().first().is(hook(TestIds.cardDelta))).toBe(true);
      expect(end.children().last().hasClass("awe-card-caption")).toBe(true);
    });

    it("colours the icon tile with icon-tone", () => {
      compile({icon: "users", iconTone: "warning"});

      expect(getCard().find(".awe-card-icon").hasClass("awe-card-icon-warning")).toBe(true);
    });
  });

  describe("progress", () => {
    it("colours the progress with the icon tone, the primary colour by default", () => {
      compile({value: 410000, target: 500000, icon: "euro", iconTone: "success"});
      expect(getCard().find(hook(TestIds.cardProgress)).hasClass("awe-card-progress-success")).toBe(true);

      compile({value: 410000, target: 500000});
      expect(getCard().find(hook(TestIds.cardProgress)).hasClass("awe-card-progress-primary")).toBe(true);
    });

    it("shows the percent of the target as a progress bar and a text", () => {
      compile({value: 410000, target: 500000, unit: "€"});
      const progress = getCard().find(hook(TestIds.cardProgress));

      expect(progress.hasClass("awe-card-progress")).toBe(true);
      expect(progress.attr("role")).toBe("progressbar");
      expect(progress.attr("aria-valuemin")).toBe("0");
      expect(progress.attr("aria-valuemax")).toBe("100");
      expect(progress.attr("aria-valuenow")).toBe("82");
      expect(progress.find(".awe-card-progress-bar")[0].style.width).toBe("82%");
      expect(getCard().find("p.awe-card-progress-text").text()).toBe("82 % SCREEN_TEXT_CARD_TARGET_OF 500,000 €");
    });

    it("clamps the bar to 100 %", () => {
      compile({value: 600, target: 500});

      expect(getCard().find(".awe-card-progress-bar")[0].style.width).toBe("100%");
      expect(getCard().find(".awe-card-progress-text").text()).toContain("120 %");
    });

    it("is absent without target or with a zero target", () => {
      compile({value: 10, target: 0});

      expect(getCard().find(hook(TestIds.cardProgress)).length).toBe(0);
    });
  });

  describe("sparkline", () => {
    const getSparkline = () => getCard().find(hook(TestIds.cardSparkline));

    it("draws the series of the attribute text as an area SVG that fills the card, in the primary tone", () => {
      compile({value: 93.1, sparkline: "[93.1,93.8,92.6]"});
      const sparkline = getSparkline();
      const svg = sparkline.children("svg");

      expect(sparkline.length).toBe(1);
      expect(sparkline[0].tagName).toBe("DIV");
      expect(sparkline.hasClass("awe-card-sparkline")).toBe(true);
      expect(sparkline.hasClass("awe-card-sparkline-primary")).toBe(true);
      expect(svg.length).toBe(1);
      expect(svg.attr("class")).toBe("awe-sparkline");
      expect(svg.attr("data-type")).toBe("area");
      expect(svg[0].getAttribute("viewBox")).toBe("0 0 100 24");
      expect(svg[0].getAttribute("preserveAspectRatio")).toBe("none");
      expect(svg.attr("role")).toBe("img");
      expect(svg.attr("aria-label")).toBe("93.1, 93.8, 92.6");
      expect(svg.attr("focusable")).toBe("false");
      expect(svg.attr("width")).toBeUndefined();
      expect(svg.attr("height")).toBeUndefined();
      expect(svg.find("path").length).toBeGreaterThan(0);
    });

    it("draws the series of a row array over the attribute, with the sparkline type", () => {
      compile({sparkline: "[9,8]", sparklineType: "line"}, [{sparkline: [1, 2, 3], sparklineType: "bar"}]);
      const svg = getSparkline().children("svg");

      expect(svg.attr("aria-label")).toBe("1, 2, 3");
      expect(svg.attr("data-type")).toBe("bar");
      expect(svg.find("rect").length).toBeGreaterThan(0);
    });

    it("draws a line when asked", () => {
      compile({sparkline: "[1,2,3]", sparklineType: "line"});

      expect(getSparkline().children("svg").attr("data-type")).toBe("line");
    });

    it("takes the icon tone", () => {
      compile({icon: "users", iconTone: "success", sparkline: "[1,2,3]"});

      expect(getSparkline().hasClass("awe-card-sparkline-success")).toBe(true);
    });

    it("renders nothing with an invalid or empty series", () => {
      compile({value: 1, sparkline: "[]"});
      expect(getSparkline().length).toBe(0);
      expect(getCard().find(".awe-card-sparkline").length).toBe(0);
      document.body.innerHTML = "";

      compile({value: 1, sparkline: "oops"});
      expect(getSparkline().length).toBe(0);
    });

    it("goes right after the value and before the body", () => {
      compile({value: 5, target: 10, sparkline: "[1,2,3]"}, [], "<p class='child'>child</p>");
      const parts = getCard().children().toArray().map(node => node.className.split(" ")[0]);

      expect(parts.indexOf("awe-card-sparkline")).toBe(parts.indexOf("awe-card-value") + 1);
      expect(parts.indexOf("awe-card-body")).toBe(parts.indexOf("awe-card-sparkline") + 1);
    });

    it("draws again when the row changes", () => {
      const element = compile({}, [{sparkline: [1, 2]}]);

      element.isolateScope().component.api.updateModelValues({values: [{sparkline: [4, 5, 6]}]});
      refs.$rootScope.$digest();

      expect(getSparkline().children("svg").attr("aria-label")).toBe("4, 5, 6");
    });
  });

  describe("gauge", () => {
    it("replaces the bar with a gauge of the clamped width, its percent and the progress text below", () => {
      compile({value: 600, target: 500, unit: "€", progressStyle: "gauge", icon: "gauge", iconTone: "warning"});
      const card = getCard();
      const gauge = card.find(hook(TestIds.cardProgress));
      const arc = gauge.children("svg");
      const paths = arc.children("path");

      expect(gauge.length).toBe(1);
      expect(gauge[0].tagName).toBe("DIV");
      expect(gauge.hasClass("awe-card-progress")).toBe(true);
      expect(gauge.hasClass("awe-card-gauge")).toBe(true);
      expect(gauge.hasClass("awe-card-progress-warning")).toBe(true);
      expect(gauge.attr("role")).toBe("progressbar");
      expect(gauge.attr("aria-valuemin")).toBe("0");
      expect(gauge.attr("aria-valuemax")).toBe("100");
      expect(gauge.attr("aria-valuenow")).toBe("100");
      expect(arc.attr("class")).toBe("awe-card-gauge-arc");
      expect(arc[0].getAttribute("viewBox")).toBe("0 0 100 56");
      expect(arc.attr("aria-hidden")).toBe("true");
      expect(arc.attr("focusable")).toBe("false");
      expect(paths.length).toBe(2);
      expect(paths.eq(0).attr("class")).toBe("awe-card-gauge-track");
      expect(paths.eq(1).attr("class")).toBe("awe-card-gauge-bar");
      paths.toArray().forEach(path => {
        expect(path.getAttribute("d")).toBe("M 8 50 A 42 42 0 0 1 92 50");
        expect(path.getAttribute("pathLength")).toBe("100");
      });
      expect(paths.eq(1).attr("stroke-dasharray")).toBe("100 100");
      expect(gauge.children(".awe-card-gauge-value").text()).toBe("120 %");
      expect(card.find(".awe-card-progress-bar").length).toBe(0);
      expect(gauge.next().is("p.awe-card-progress-text")).toBe(true);
      // The percent is already in the middle of the arc: the text below only tells the target
      expect(card.find(".awe-card-progress-text").text()).toBe("SCREEN_TEXT_CARD_TARGET_OF 500 €");
    });

    it("draws the arc of the percent in the primary tone by default", () => {
      compile({value: 410000, target: 500000, progressStyle: "gauge"});
      const gauge = getCard().find(hook(TestIds.cardProgress));

      expect(gauge.hasClass("awe-card-progress-primary")).toBe(true);
      expect(gauge.attr("aria-valuenow")).toBe("82");
      expect(gauge.find(".awe-card-gauge-bar").attr("stroke-dasharray")).toBe("82 100");
      expect(gauge.find(".awe-card-gauge-value").text()).toBe("82 %");
    });

    it("keeps the bar by default and with progress style bar", () => {
      compile({value: 1, target: 2, progressStyle: "bar"});
      expect(getCard().find(".awe-card-progress-bar").length).toBe(1);
      expect(getCard().find(".awe-card-gauge").length).toBe(0);
      document.body.innerHTML = "";

      compile({value: 1, target: 2});
      expect(getCard().find(".awe-card-progress-bar").length).toBe(1);
      expect(getCard().find(".awe-card-gauge").length).toBe(0);
    });

    it("renders neither the gauge nor the bar without target", () => {
      compile({value: 1, progressStyle: "gauge"});

      expect(getCard().find(hook(TestIds.cardProgress)).length).toBe(0);
      expect(getCard().find(".awe-card-gauge").length).toBe(0);
      expect(getCard().find(".awe-card-progress-text").length).toBe(0);
    });

    it("turns into a gauge when the row asks for it", () => {
      compile({value: 1, target: 2}, [{progressStyle: "gauge"}]);

      expect(getCard().find(".awe-card-gauge").length).toBe(1);
      expect(getCard().find(".awe-card-progress-bar").length).toBe(0);
    });
  });

  describe("body", () => {
    it("holds the children below the value and above the progress", () => {
      compile({value: 5, target: 10}, [], "<p class='child'>child</p>");
      const card = getCard();
      const parts = card.children().toArray().map(node => node.className.split(" ")[0]);

      expect(card.find(".awe-card-body > p.child").text()).toBe("child");
      expect(parts.indexOf("awe-card-value")).toBeLessThan(parts.indexOf("awe-card-body"));
      expect(parts.indexOf("awe-card-body")).toBeLessThan(parts.indexOf("awe-card-progress"));
    });

    it("shows only the body without value", () => {
      compile({title: "Sales"}, [], "<p class='child'>child</p>");

      expect(getCard().find(hook(TestIds.cardValue)).length).toBe(0);
      expect(getCard().find(".awe-card-body > p.child").length).toBe(1);
    });
  });
});
