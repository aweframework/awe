describe("Jest service-filter bucket templateSelectorColumn export", () => {
  it("publishes the selector column template with editable input and loader bindings", () => {
    const selectorModule = require("../../../main/resources/js/awe/services/selector.js");
    const templateSelectorColumn = selectorModule.templateSelectorColumn;

    expect(templateSelectorColumn).toContain("class=\"validator column-input");
    expect(templateSelectorColumn).toContain("ui-select2=\"aweSelectOptions\"");
    expect(templateSelectorColumn).toContain("ng-disabled=\"component.controller.readonly\"");
    expect(templateSelectorColumn).toContain("awe-loader");
  });

  it("preserves visible-value, icon, and visibility bindings used by column selectors", () => {
    const selectorModule = require("../../../main/resources/js/awe/services/selector.js");
    const templateSelectorColumn = selectorModule.templateSelectorColumn;

    expect(templateSelectorColumn).toContain("ng-show=\"component.controller.visible\"");
    expect(templateSelectorColumn).toContain("{{component.visibleValue}}");
    expect(templateSelectorColumn).toContain("{{::iconClass}}");
    expect(templateSelectorColumn).toContain("{{component.model.selected}}");
  });
});
