import fs from "fs";
import path from "path";

const angularTemplates = path.join(__dirname, "../../../main/resources/templates/angular");

/**
 * Read an AWE angular template
 * @param {string} name Template path (relative to templates/angular)
 * @returns {string} Template
 */
export const readTemplate = name => fs.readFileSync(path.join(angularTemplates, name), "utf8");

/**
 * Put the grid templates that the settings service preloads in the template cache, so ui-grid renders the grid with
 * the AWE templates instead of requesting them
 * @param {object} $templateCache Template cache
 */
export function cacheGridTemplates($templateCache) {
  [
    ["grid/header", "grid/header.html"],
    ["grid/cell", "grid/cell.html"],
    ["grid/footer", "grid/footer.html"],
    ["grid/row", "grid/row.html"],
    ["grid/pagination", "grid/pagination.html"],
    ["ui-grid/uiGridViewport", "grid/viewport.html"],
    ["ui-grid/uiGridRenderContainer", "grid/renderContainer.html"],
    ["grid/headerFilter", "grid/headerFilter.html"],
    ["grid/cellRowNumber", "grid/cellRowNumber.html"],
    ["grid/cellCheckbox", "grid/cellCheckbox.html"],
    ["ui-grid/selectionHeaderCell", "grid/selectionHeaderCell.html"],
    ["ui-grid/ui-grid-header", "grid/headerCell.html"],
    ["grid/headerCheckbox", "grid/headerCheckbox.html"]
  ].forEach(([name, file]) => $templateCache.put(name, readTemplate(file)));
}

/**
 * Mount an AWE grid (awe-grid directive) rendered by ui-grid with the AWE templates
 * @param {object} refs References of the heavy component test (initHeavyComponentTest)
 * @param {object} controller Grid controller (merged over a default one)
 * @param {Array} values Rows of the grid
 * @returns {{element: object, scope: object, component: object, model: object, controller: object}} Mounted grid
 */
export function mountGrid(refs, controller = {}, values = []) {
  const gridController = {
    id: "Grid",
    visible: true,
    loading: false,
    targetAction: "getRows",
    loadAll: false,
    multiselect: false,
    editable: false,
    enableFilters: false,
    showTotals: false,
    disablePagination: false,
    max: 10,
    pagerValues: [10, 20],
    columnModel: [{name: "name", label: "Name", sendable: true}],
    buttonModel: [],
    headerModel: [],
    ...controller
  };
  const model = {page: 1, records: values.length, total: 1, selected: [], values};
  inject(["$templateCache", cacheGridTemplates]);
  jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
  jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
  jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
  jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
  jest.spyOn(refs.$control, "getAddressController").mockReturnValue(gridController);
  jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
  refs.$utilities.timeout.cancel = jest.fn();
  // jsdom cannot parse the style block that ui-grid renders with unresolved bindings and reports it on the console
  jest.spyOn(console, "error").mockImplementation(() => undefined);

  const element = refs.$compile("<awe-grid grid-id='Grid' style='height: 300px; width: 400px'></awe-grid>")(refs.$rootScope.$new());
  $(document.body).append(element);
  refs.$rootScope.$digest();
  const scope = element.isolateScope() || element.scope();
  return {element, scope, component: scope.component, model, controller: gridController};
}
