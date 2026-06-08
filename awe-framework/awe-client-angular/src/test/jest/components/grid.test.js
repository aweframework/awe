// Source-traceable focused Jest parity for components/grid.js.
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";

describe("components/grid.js", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("migrates grid option defaults, paging behavior, and row template wiring", () => {
    const controller = {
      id: "Grid",
      style: "no-label",
      visible: true,
      loading: false,
      targetAction: "getRows",
      loadAll: false,
      multiselect: false,
      editable: false,
      enableFilters: true,
      showTotals: false,
      disablePagination: false,
      max: 10,
      pagerValues: [10, 20],
      columnModel: [{name: "date", field: "date"}],
      buttonModel: [],
      headerModel: []
    };
    const model = {page: 1, records: 25, total: 3, selected: [], values: [{id: 1, date: "23/10/1978"}]};
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();

    const element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();

    const scope = element.isolateScope() || element.scope();
    expect(element.attr("id")).toBe("Grid");
    expect(element.find("#grid-Grid").length).toBe(1);
    expect(scope.gridOptions.columnDefs).toEqual(controller.columnModel);
    expect(scope.gridOptions.rowTemplate).toBe("grid/row");
    expect(scope.gridOptions.paginationTemplate).toBe("grid/pagination");
    expect(scope.gridOptions.enableFiltering).toBe(true);
    expect(scope.gridOptions.useExternalPagination).toBe(true);

    const event = {
      type: "keypress",
      keyCode: 13,
      currentTarget: {value: "2"},
      stopPropagation: jest.fn(),
      preventDefault: jest.fn()
    };
    jest.spyOn(scope.component, "setPage").mockImplementation(page => {
      scope.component.currentPage = page;
    });

    scope.onGoToPageChanged(event);

    expect(scope.component.setPage).toHaveBeenCalledWith(2);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(event.preventDefault).toHaveBeenCalled();
    expect(event.currentTarget.value).toBe("");
  });
});
