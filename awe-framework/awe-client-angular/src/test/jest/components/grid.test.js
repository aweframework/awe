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

  describe("empty state", () => {
    const EMPTY = "[data-testid='grid-empty']";
    const baseController = {
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
      columnModel: [{name: "date", field: "date"}],
      buttonModel: [],
      headerModel: []
    };

    const compileGrid = (controllerOverrides = {}, values = []) => {
      const controller = {...baseController, ...controllerOverrides};
      const model = {page: 1, records: values.length, total: 1, selected: [], values};
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(model);
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(controller);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();
      const element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();
      return {element, scope: element.isolateScope() || element.scope()};
    };

    it("shows the default empty state in a grid without rows", () => {
      const {element, scope} = compileGrid();

      expect(element.find(EMPTY).length).toBe(1);
      expect(scope.emptyState.title).toBe("SCREEN_TEXT_GRID_EMPTY_TITLE");
      expect(scope.emptyState.text).toBe("SCREEN_TEXT_GRID_EMPTY_TEXT");
      expect(scope.emptyState.icon).toEqual({name: "inbox", family: "fa"});
      expect(scope.emptyState.button).toBeUndefined();
    });

    it("shows the configured icon, title and text", () => {
      const {scope} = compileGrid({emptyIcon: "mdi:filter_alt", emptyTitle: "MY_TITLE", emptyText: "MY_TEXT"});

      expect(scope.emptyState.icon).toEqual({name: "filter_alt", family: "mdi"});
      expect(scope.emptyState.title).toBe("MY_TITLE");
      expect(scope.emptyState.text).toBe("MY_TEXT");
    });

    it("does not show the empty state when the grid has rows", () => {
      const {element} = compileGrid({}, [{id: 1, date: "23/10/1978"}]);

      expect(element.find(EMPTY).length).toBe(0);
    });

    it("clicks the button of the grid that the empty action refers to", () => {
      const clearButton = {id: "ClearFilters", label: "BUTTON_CLEAR", icon: "eraser"};
      const {scope} = compileGrid({emptyAction: "ClearFilters", buttonModel: [clearButton]});
      const onClick = jest.fn();
      const domButton = document.createElement("button");
      domButton.id = "ClearFilters";
      domButton.addEventListener("click", onClick);
      document.body.appendChild(domButton);

      scope.onEmptyAction();
      document.body.removeChild(domButton);

      expect(scope.emptyState.button).toEqual(clearButton);
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it("waits for the data of a grid with initial load before showing the empty state", () => {
      const {element, scope} = compileGrid({initialLoad: "query"});

      expect(element.find(EMPTY).length).toBe(0);

      scope.component.api.updateModelValues({values: [], records: 0});
      refs.$rootScope.$digest();

      expect(element.find(EMPTY).length).toBe(1);
    });

    it("shows the empty state of a grid with initial load whose data came with the screen and has no rows", () => {
      // The server sends the model of the grid inside the screen data: the query ran and returned no rows
      let serverModel, serverController, serverApi = {};
      angular.mock.inject(["ServerData", ServerData => {
        const views = {model: {}, controller: {}, api: {}};
        ServerData.storeComponent({
          id: "Grid",
          controller: {...baseController, initialLoad: "query"},
          model: {total: 1, page: 1, records: 0, selected: null, values: []}
        }, views.model, views.controller, views.api, "option");
        serverModel = views.model.Grid;
        serverController = views.controller.Grid;
        serverApi = views.api.Grid;
      }]);
      jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
      jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
      jest.spyOn(refs.$control, "getAddressModel").mockReturnValue(serverModel);
      jest.spyOn(refs.$control, "getAddressController").mockReturnValue(serverController);
      jest.spyOn(refs.$control, "getAddressApi").mockReturnValue(serverApi);
      jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
      refs.$utilities.timeout.cancel = jest.fn();

      const element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(refs.$rootScope.$new());
      refs.$rootScope.$digest();

      expect(element.find(EMPTY).length).toBe(1);
    });

    it("clicks the button of the grid outside of the digest of the click on the empty action", () => {
      const {scope} = compileGrid({emptyAction: "ClearFilters", buttonModel: [{id: "ClearFilters"}]});
      refs.$utilities.timeout.mockRestore();
      const errors = [];
      let clicks = 0;
      const domButton = document.createElement("button");
      domButton.id = "ClearFilters";
      // The button of the grid runs its actions through ng-click, which starts a digest of its own
      domButton.addEventListener("click", () => {
        clicks++;
        try {
          refs.$rootScope.$apply();
        } catch (error) {
          errors.push(error.message);
        }
      });
      document.body.appendChild(domButton);

      // The empty action is itself an ng-click, so it runs inside a digest
      refs.$rootScope.$apply(() => scope.onEmptyAction());
      angular.mock.inject(["$timeout", $timeout => $timeout.flush()]);
      document.body.removeChild(domButton);

      expect(errors).toEqual([]);
      expect(clicks).toBe(1);
    });

    it("has no action for a button that is not in the grid", () => {
      const {scope} = compileGrid({emptyAction: "Unknown", buttonModel: [{id: "Other"}]});

      expect(scope.emptyState.button).toBeUndefined();
      expect(() => scope.onEmptyAction()).not.toThrow();
    });
  });
});
