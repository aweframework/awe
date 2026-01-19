describe('templateSelectorColumn export', function () {
  it('should be a string with expected layout markers', function () {
    const selectorModule = require('./../../../main/resources/js/awe/services/selector.js');
    const templateSelectorColumn = selectorModule.templateSelectorColumn;

    expect(typeof templateSelectorColumn).toBe('string');
    // Basic checks to ensure template content is present
    expect(templateSelectorColumn).toContain('class="validator column-input');
    expect(templateSelectorColumn).toContain('ui-select2="aweSelectOptions"');
    expect(templateSelectorColumn).toContain('awe-loader');
    expect(templateSelectorColumn).toContain('{{::iconClass}}');
    expect(templateSelectorColumn).toContain('component.controller.visible');
  });
});
