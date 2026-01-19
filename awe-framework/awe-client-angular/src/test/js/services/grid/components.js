describe('awe-framework/awe-client-angular/src/test/js/services/grid/components.js', function () {
  let $injector, GridComponents, AweUtilities, Control;

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      GridComponents = $injector.get('GridComponents');
      AweUtilities = $injector.get('AweUtilities');
      Control = $injector.get('Control');
    }]);
  });

  it('should update cell model and publish model changed', function () {
    // Prepare a fake grid component with two rows
    let component = {
      id: 'gridId',
      address: {view: 'view1', component: 'gridId'},
      constants: { ROW_IDENTIFIER: 'id' },
      controller: {},
      model: { values: [ {id: 1, colA: 'foo'}, {id: 2, colA: 'bar'} ], footer: {} },
      api: {},
      columnModelStringified: {},
      getCellObject: (v) => v,
      getColumnValueList: () => [],
      getColumn: () => ({})
    };

    let grid = new GridComponents(component);
    // initialize to create methods such as updateCellModel
    grid.init();

    // Spy publishModelChanged
    spyOn(Control, 'publishModelChanged').and.callFake(function(){});

    // Build a fake cell located in row with id 2 and column colA
    let cell = {
      address: {component: 'gridId', row: 2, column: 'colA'},
      model: {selected: 'changed'}
    };

    // Sanity: ensure AweUtilities returns correct index
    let idx = AweUtilities.getRowIndex(component.model.values, cell.address.row, component.constants.ROW_IDENTIFIER);
    expect(idx).toBe(1);

    // Exercise the line under test: component.model.values[rowIndex][address.column] = cell.model.selected;
    component.updateCellModel(cell);

    // Assert model updated
    expect(component.model.values[1].colA).toBe('changed');
    // Assert publish was called with values
    expect(Control.publishModelChanged).toHaveBeenCalled();
    const args = Control.publishModelChanged.calls.mostRecent().args;
    expect(args[0]).toEqual(component.address);
    expect(args[1]).toEqual({values: component.model.values});
  });

  it('should not update when rowIndex is -1 (footer row)', function () {
    // Prepare
    let component = {
      id: 'gridId',
      address: {view: 'view1', component: 'gridId'},
      constants: { ROW_IDENTIFIER: 'id' },
      controller: {},
      model: { values: [ {id: 1, colA: 'foo'} ], footer: { colA: 'sum' } },
      api: {},
      columnModelStringified: {},
      getCellObject: (v) => v,
      getColumnValueList: () => [],
      getColumn: () => ({})
    };

    let grid = new GridComponents(component);
    grid.init();

    spyOn(Control, 'publishModelChanged').and.callFake(function(){});

    // address.row that is not present -> getRowIndex should be -1
    let cell = {
      address: {component: 'gridId', row: 'footer-row-id', column: 'colA'},
      model: {selected: 'x'}
    };

    // Force getRowIndex to -1 to emulate footer behavior
    spyOn(AweUtilities, 'getRowIndex').and.returnValue(-1);

    component.updateCellModel(cell);

    // Ensure values did not change and no publish
    expect(component.model.values[0].colA).toBe('foo');
    expect(Control.publishModelChanged).not.toHaveBeenCalled();
  });
});
