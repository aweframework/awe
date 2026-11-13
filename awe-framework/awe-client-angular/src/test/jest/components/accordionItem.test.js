// Focused Jest spec for directives/accordionItem.js: revealing and marking an item that holds invalid fields.
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

const fs = require("fs");
const path = require("path");

const ITEM_TEMPLATE = fs.readFileSync(
  path.join(__dirname, "..", "..", "..", "main", "resources", "templates", "angular", "accordionItem.html"), "utf8");

describe("components/accordionItem.js", () => {
  const TEMPLATE_URL = "accordionItem-template";
  let refs;
  let accordionScope;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  function compileItem(itemsSelected) {
    stubPanelComponent(refs, createPanelModel("1"), createPanelController("item1", "accordion-item"));
    inject(["ServerData", "$templateCache", (serverData, $templateCache) => {
      jest.spyOn(serverData, "getAngularTemplateUrl").mockReturnValue(TEMPLATE_URL);
      $templateCache.put(TEMPLATE_URL,
        "<div class='panel' ng-class=\"{'panel-invalid': hasInvalid}\"><div ng-transclude></div></div>");
    }]);

    accordionScope = refs.$rootScope.$new();
    accordionScope.controller = {itemsSelected, autocollapse: true, selected: null};
    accordionScope.accordionId = "acc1";
    const element = refs.$compile("<awe-accordion-item accordion-item-id='item1'></awe-accordion-item>")(accordionScope.$new());
    refs.$rootScope.$digest();
    return {element, scope: element.isolateScope()};
  }

  it("opens a collapsed item when a field inside it has to be revealed", () => {
    const {scope} = compileItem([]);

    scope.$emit("select-current-tab");

    expect(accordionScope.controller.selected).toBe("item1");
  });

  it("keeps an open item as it is when a field inside it has to be revealed", () => {
    const {scope} = compileItem(["item1"]);

    scope.$emit("select-current-tab");

    expect(accordionScope.controller.selected).toBeNull();
  });

  it("marks the item while it holds invalid fields", () => {
    const {element, scope} = compileItem([]);

    scope.$emit("criterion-validity", {id: "field", invalid: true});
    refs.$rootScope.$digest();
    expect(element.hasClass("panel-invalid")).toBe(true);

    scope.$emit("criterion-validity", {id: "field", invalid: false});
    refs.$rootScope.$digest();
    expect(element.hasClass("panel-invalid")).toBe(false);
  });

  describe("collapse (no Bootstrap data-api: the item is the single driver)", () => {
    const compileReal = (selected, id = "item1") => {
      stubPanelComponent(refs, createPanelModel("1"), createPanelController(id, "accordion-item", {label: "ITEM_LABEL"}));
      inject(["ServerData", "$templateCache", (serverData, $templateCache) => {
        jest.spyOn(serverData, "getAngularTemplateUrl").mockReturnValue(TEMPLATE_URL);
        $templateCache.put(TEMPLATE_URL, ITEM_TEMPLATE);
      }]);
      accordionScope = accordionScope || refs.$rootScope.$new();
      accordionScope.controller = accordionScope.controller || {itemsSelected: selected, autocollapse: true, selected: null};
      accordionScope.accordionId = "acc1";
      const element = refs.$compile(`<awe-accordion-item accordion-item-id='${id}'></awe-accordion-item>`)(accordionScope.$new());
      refs.$rootScope.$digest();
      document.body.appendChild(element[0]);
      return {element, scope: element.isolateScope(), panel: element.find(".panel-collapse"), toggle: element.find(".accordion-toggle")};
    };
    const select = ids => {
      accordionScope.controller.itemsSelected = ids;
      refs.$rootScope.$digest();
    };

    beforeEach(() => {
      accordionScope = null;
    });

    afterEach(() => {
      document.body.innerHTML = "";
    });

    it("does not rely on the Bootstrap data-api attributes", () => {
      expect(ITEM_TEMPLATE).not.toMatch(/data-toggle|data-parent|data-target/);
    });

    it("renders an open item as collapse in, with its toggle expanded", () => {
      const {panel, toggle} = compileReal(["item1"]);

      expect(panel.hasClass("collapse")).toBe(true);
      expect(panel.hasClass("in")).toBe(true);
      expect(toggle.hasClass("collapsed")).toBe(false);
      expect(toggle.attr("aria-expanded")).toBe("true");
      expect(panel.attr("aria-expanded")).toBe("true");
    });

    it("renders a closed item as collapse, with its toggle collapsed", () => {
      const {panel, toggle} = compileReal([]);

      expect(panel.hasClass("collapse")).toBe(true);
      expect(panel.hasClass("in")).toBe(false);
      expect(toggle.hasClass("collapsed")).toBe(true);
      expect(toggle.attr("aria-expanded")).toBe("false");
      expect(panel[0].style.height).toBe("");
    });

    it("opens when the accordion selects it and closes when it does not", () => {
      const {panel, toggle} = compileReal([]);

      select(["item1"]);
      expect(panel.hasClass("in")).toBe(true);
      expect(toggle.hasClass("collapsed")).toBe(false);
      expect(toggle.attr("aria-expanded")).toBe("true");

      select([]);
      expect(panel.hasClass("in")).toBe(false);
      expect(toggle.hasClass("collapsed")).toBe(true);
      expect(panel[0].style.height).toBe("");
    });

    it("opens again after it was closed, without a leftover height (no workaround needed)", () => {
      const {panel} = compileReal(["item1"]);
      select([]);

      select(["item1"]);

      expect(panel.hasClass("in")).toBe(true);
      expect(panel[0].style.height).toBe("");
    });

    it("keeps one item open: selecting another one closes this one", () => {
      const first = compileReal(["item1"], "item1");
      const second = compileReal(["item1"], "item2");

      select(["item2"]);

      expect(first.panel.hasClass("in")).toBe(false);
      expect(second.panel.hasClass("in")).toBe(true);
    });

    it("asks the accordion to select the item with a click on its toggle", () => {
      const {toggle} = compileReal([]);

      toggle.trigger("click");

      expect(accordionScope.controller.selected).toBe("item1");
    });

    it("does not navigate to the anchor of the toggle", () => {
      const {toggle} = compileReal([]);
      const click = $.Event("click");

      toggle.trigger(click);

      expect(click.isDefaultPrevented()).toBe(true);
    });

    it("does not toggle by itself: only the accordion state opens and closes it", () => {
      const {panel, toggle} = compileReal(["item1"]);
      accordionScope.controller.selected = null;

      // The accordion directive is not compiled in this spec, so nothing consumes "selected"
      toggle.trigger("click");

      expect(panel.hasClass("in")).toBe(true);
    });
  });
});
