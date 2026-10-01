import "./lightComponentTestUtils";

import fs from "fs";
import path from "path";
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";

const angularTemplates = path.join(__dirname, "../../../main/resources/templates/angular");
const readTemplate = name => fs.readFileSync(path.join(angularTemplates, name), "utf8");

/**
 * The templates under test are the ones AWE hands to ui-grid. Rendering them with the real library proves the hooks
 * survive its compilation: the selection directive rewrites the row repeater and the viewport is replaced in place.
 */
describe("ui-grid rendered with the AWE templates", () => {
  let refs;
  let scope;
  let element;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    // jsdom cannot parse the style block that ui-grid renders with unresolved bindings and reports it on the console
    jest.spyOn(console, "error").mockImplementation(() => undefined);

    inject(["$templateCache", $templateCache => {
      $templateCache.put("grid/row", readTemplate("grid/row.html"));
      $templateCache.put("grid/cell", readTemplate("grid/cell.html"));
      $templateCache.put("grid/header", readTemplate("grid/header.html"));
      $templateCache.put("grid/headerFilter", readTemplate("grid/headerFilter.html"));
      $templateCache.put("ui-grid/uiGridViewport", readTemplate("grid/viewport.html"));
      $templateCache.put("ui-grid/uiGridRenderContainer", readTemplate("grid/renderContainer.html"));
      $templateCache.put("ui-grid/ui-grid-header", readTemplate("grid/headerCell.html"));
    }]);

    scope = refs.$rootScope.$new();
    scope.component = {
      constants: {ROW_IDENTIFIER: "id", ROW_CLASS_FIELD: "style"},
      hasFrozen: false,
      getCellValue: value => value,
      getCellStyle: () => "",
      viewportClick: jest.fn()
    };
    scope.gridOptions = {
      data: [{id: "r1", name: "A", age: 30}, {id: "r2", name: "B", age: 40}],
      columnDefs: [
        {name: "name", field: "name", headerCellTemplate: "grid/header", cellTemplate: "grid/cell"},
        {name: "age", field: "age", headerCellTemplate: "grid/header", cellTemplate: "grid/cell"}
      ],
      rowTemplate: "grid/row",
      enableRowSelection: true,
      enableFullRowSelection: true,
      enableRowHeaderSelection: false,
      multiSelect: true,
      virtualizationThreshold: 50,
      enableMinHeightCheck: false,
      appScopeProvider: scope,
      onRegisterApi: api => {
        scope.api = api;
      }
    };
    element = refs.$compile("<div style='height: 300px; width: 400px' ui-grid='gridOptions' ui-grid-selection></div>")(scope);
    $(document.body).append(element);
    scope.$digest();
  });

  afterEach(() => {
    element.remove();
    cleanupHeavyComponentTest();
  });

  it("renders one header cell, one row and one cell per column with the instance attributes", () => {
    expect(element.find("[data-testid='grid-header-cell']").map((i, node) => node.getAttribute("column-id")).get()).toEqual(["name", "age"]);
    expect(element.find("[data-testid='grid-row']").map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r1", "r2"]);
    expect(element.find("[data-testid='grid-row'][row-id='r2'] [data-testid='grid-cell'][column-id='age']").text()).toBe("40");
  });

  it("renders the viewport of the body container", () => {
    const viewport = element.find("[data-testid='grid-viewport']");

    expect(viewport.length).toBe(1);
    expect(viewport.attr("data-container")).toBe("body");
  });

  it("follows the row selection of the library", () => {
    const states = () => element.find("[data-testid='grid-row']").map((i, node) => node.getAttribute("data-selected")).get();
    expect(states()).toEqual(["false", "false"]);

    scope.api.selection.selectRow(scope.gridOptions.data[1]);
    scope.$digest();

    expect(states()).toEqual(["false", "true"]);
    expect(element.find("[data-testid='grid-row'][data-selected='true'] [data-testid='grid-cell'][column-id='name']").text()).toBe("B");

    scope.api.selection.clearSelectedRows();
    scope.$digest();

    expect(states()).toEqual(["false", "false"]);
  });
});
