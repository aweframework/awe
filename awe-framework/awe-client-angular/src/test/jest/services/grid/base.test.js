import "./gridTestUtils";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/base.js', function () {
  let $injector, GridBase, GridEvents, $rootScope;
  const currentModel = {};
  const currentController = {columnModel: []};

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      GridBase = $injector.get('GridBase');
      GridEvents = $injector.get('GridEvents');
      $rootScope = $injector.get('$rootScope');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function () {
  });

  it('should initialize a grid', function () {
    // Mock
    let $scope = $rootScope.$new();
    let grid = new GridBase($scope, "id", {});
    jest.spyOn(grid, "asComponent").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapCommonActions").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapTreeActions").mockReturnValue(true);
    grid.api = {};
    grid.model = currentModel;
    grid.controller = currentController;

    // Assert
    expect(grid.asGrid()).toBe(true);
  });

  it('should delete a specific row not existent', function () {
    // Mock
    let $scope = $rootScope.$new();
    $scope.gridOptions = {icons: {}};
    let grid = new GridBase($scope, "id", {});
    jest.spyOn(grid, "asComponent").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapCommonActions").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapTreeActions").mockReturnValue(true);
    grid.api = {};
    grid.model = {
      records: 6,
      total: 1,
      page: 1,
      values: [{id: 1, value: "tutu", RowTyp: "INSERT"}, {id: 2, value: "lala"}, {
        id: 4,
        value: "lele",
        other: "asda",
        RowTyp: "UPDATE"
      }, {id: 5, value: "lili"}, {id: 6, value: "lolo", RowTyp: "DELETE"}, {id: 7, value: "lulu"}],
      selected: [4]
    };
    grid.controller = currentController;
    grid.asGrid();

    grid.deleteRowSpecific("3");

    // Assert
    expect(grid.model.values.length).toBe(6);
  });

  it('should delete a specific row', function () {
    // Mock
    let $scope = $rootScope.$new();
    $scope.gridOptions = {icons: {}};
    let grid = new GridBase($scope, "id", {});
    jest.spyOn(grid, "asComponent").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapCommonActions").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapTreeActions").mockReturnValue(true);
    grid.api = {};
    grid.model = {
      records: 6,
      total: 1,
      page: 1,
      values: [{id: 1, value: "tutu", RowTyp: "INSERT"}, {id: 2, value: "lala"}, {
        id: 4,
        value: "lele",
        other: "asda",
        RowTyp: "UPDATE"
      }, {id: 5, value: "lili"}, {id: 6, value: "lolo", RowTyp: "DELETE"}, {id: 7, value: "lulu"}],
      selected: [4]
    };
    grid.controller = currentController;
    grid.asGrid();
    grid.grid.measures = {};

    grid.deleteRowSpecific("4");

    // Assert
    expect(grid.model.values.length).toBe(5);
  });

  function buildGridToAddRows() {
    let $scope = $rootScope.$new();
    $scope.gridOptions = {icons: {}};
    let grid = new GridBase($scope, "id", {});
    jest.spyOn(grid, "asComponent").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapCommonActions").mockReturnValue(true);
    jest.spyOn(GridEvents, "mapTreeActions").mockReturnValue(true);
    grid.api = {};
    grid.model = {records: 3, total: 1, page: 1, values: [{id: 1}, {id: 2}, {id: 3}], selected: []};
    grid.controller = currentController;
    grid.asGrid();
    grid.grid.measures = {};
    jest.spyOn(grid, "unselectRows").mockReturnValue(true);
    jest.spyOn(grid, "updateModelSpecific").mockReturnValue(true);
    jest.spyOn(grid, "deferRowsRendered").mockReturnValue(true);
    return grid;
  }

  it('should add a row after the selected row when it exists', function () {
    let grid = buildGridToAddRows();

    grid.addRowSpecific("2", "after", {id: "new"});

    // Assert
    expect(grid.model.values.map((value) => value.id)).toEqual([1, 2, "new", 3]);
  });

  it('should add a row at the end when the selected row does not exist', function () {
    let grid = buildGridToAddRows();

    grid.addRowSpecific("9", "before", {id: "new"});

    // Assert
    expect(grid.model.values.map((value) => value.id)).toEqual([1, 2, 3, "new"]);
  });

});
