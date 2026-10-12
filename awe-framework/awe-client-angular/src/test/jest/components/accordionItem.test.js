// Focused Jest spec for directives/accordionItem.js: revealing and marking an item that holds invalid fields.
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

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
});
