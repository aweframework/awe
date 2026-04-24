const contractCases = require("./gridContractCases.js");
const {createGridHarness} = require("./gridContractTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/gridFamily/grid.contract.js", function () {
  let $injector;
  let $rootScope;
  let GridCommons;
  let currentModels;
  let currentControllers;
  let currentApis;

  beforeEach(function () {
    currentModels = {};
    currentControllers = {};
    currentApis = {};

    angular.mock.module("aweApplication");

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get("$rootScope");
      GridCommons = $injector.get("GridCommons");
    }]);
  });

  function createGrid(options = {}) {
    return createGridHarness({
      $rootScope,
      GridCommons,
      currentModels,
      currentControllers,
      currentApis,
      options
    });
  }

  it("serializes a basic grid payload with selected ids, selected cell data and metadata", function () {
    const grid = createGrid(contractCases.basicPayload);

    expect(grid.component.getData()).toEqual(contractCases.basicPayload.expectedData);
  });

  it("serializes multi-selection as arrays and omits the single selected row address", function () {
    const grid = createGrid(contractCases.multipleSelection);

    expect(grid.component.getData()).toEqual(contractCases.multipleSelection.expectedData);
  });

  it("normalizes sanitized and empty selections before serializing the grid contract", function () {
    const sanitizedGrid = createGrid(contractCases.sanitizedSelection);
    const emptyGrid = createGrid(contractCases.emptySelection);

    sanitizedGrid.component.setSelection(contractCases.sanitizedSelection.selection);
    emptyGrid.component.selectRows([]);

    expect(sanitizedGrid.model.selected).toEqual(contractCases.sanitizedSelection.expectedSelection);
    expect(sanitizedGrid.component.getData()).toEqual(contractCases.sanitizedSelection.expectedData);
    expect(emptyGrid.model.selected).toBeNull();
    expect(emptyGrid.component.getData()).toEqual(contractCases.emptySelection.expectedData);
  });
});
