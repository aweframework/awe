/* eslint-disable */
import {components} from '../../../../src/redux/reducers/componentsReducer';
import {getComponentId} from '../../../../src/utilities/components';
import {
  GENERATE_CELL_COMPONENTS,
  UPDATE_MODEL,
  UPDATE_ROW_MODEL,
  KEEP_ROW_MODEL,
  KEEP_MODEL,
  RESET_MODEL,
  RESTORE_ATTRIBUTE
} from '../../../../src/redux/actions/components';

/**
 * Additional grid-focused coverage for componentsReducer
 */
describe('awe-react-client/test/js/redux/reducers/componentsReducerGridTest.jsx', () => {
  let baseGrid;

  beforeEach(() => {
    baseGrid = {
      grid: {
        address: {component: 'grid', view: 'base'},
        attributes: {
          id: 'id',
          columnModel: [
            // include id property equal to column name to satisfy resetCellModel implementation
            {name: 'colA', id: 'colA', component: true, model: {values: [{value: 'A', label: 'A'}]}},
            {name: 'colB', id: 'colB', component: true, model: {values: [{value: 'B', label: 'B'}]}}
          ]
        },
        storedAttributes: {
          // Stored column attrs used by restoreAttributeCell
          columnModel: [
            {placeholder: 'FromStoredA'},
            {placeholder: 'FromStoredB'}
          ]
        },
        model: {
          values: [
            {id: 1, colA: 'A', colB: 'B', $attrs: {colA: {placeholder: 'FromStoredA'}, colB: {placeholder: 'FromStoredB'}}},
            {id: 2, colA: 'A', colB: 'B', $attrs: {colA: {placeholder: 'FromStoredA'}, colB: {placeholder: 'FromStoredB'}}}
          ],
          page: 3,
          total: 10,
          records: 20
        }
      }
    };
  });

  it('generates cell components and updates them when grid model changes (updateCellsModel + getGridModelUpdate)', () => {
    // 1) Generate cell components for both rows
    const withCells = components(baseGrid, {
      type: GENERATE_CELL_COMPONENTS,
      address: {component: 'grid', view: 'base'},
      data: [1, 2]
    });

    // Sanity: cell components exist
    const idA1 = getComponentId({component: 'grid', view: 'base', row: 1, column: 'colA'});
    const idB2 = getComponentId({component: 'grid', view: 'base', row: 2, column: 'colB'});
    expect(withCells[idA1]).toBeDefined();
    expect(withCells[idB2]).toBeDefined();

    // 2) Update grid values to trigger updateCellsModel via getGridModelUpdate
    const newValues = [
      {id: 1, colA: 'A', colB: 'B'},
      {id: 2, colA: 'A', colB: 'B'}
    ];

    const updated = components(withCells, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base'},
      data: {values: newValues}
    });

    // Grid changed flag set by getModelUpdate
    expect(updated.grid.model.changed).toBe(true);
    // Cells should also reflect the new model
    expect(updated[idA1].model.values.find(v => v.value === 'A').selected).toBe(true);
    expect(updated[idB2].model.values.find(v => v.value === 'B').selected).toBe(true);
  });

  it('updates a single cell model via UPDATE_MODEL with cell address (updateCellModel)', () => {
    // Construct a state with a string value for colA row 1 so getCellModel adapts
    const state = {
      ...baseGrid,
      grid: {
        ...baseGrid.grid,
        model: {
          ...baseGrid.grid.model,
          values: [
            {id: 1, colA: 'A', colB: 'B'},
            {id: 2, colA: 'A', colB: 'B'}
          ]
        }
      }
    };

    const newState = components(state, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base', row: 1, column: 'colA'},
      data: {values: [{value: 'A', selected: true}]}
    });

    const row1 = newState.grid.model.values.find(r => String(r.id) === '1');
    expect(row1.colA).toBeDefined();
    expect(row1.colA.value).toBe('A');
  });

  it('updates selected rows on grid (updateSelectedGrid path)', () => {
    const res = components(baseGrid, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base'},
      data: {selected: [2], event: 'sel'}
    });

    expect(res.grid.model.changed).toBe(true);
    expect(res.grid.model.event).toBe('sel');
    const selectedRows = res.grid.model.values.filter(v => v.selected).map(v => v.id);
    expect(selectedRows).toEqual([2]);
  });

  it('updates a full row via UPDATE_ROW_MODEL', () => {
    const rowUpdate = {id: 2, colA: {values: [{value: 'A', selected: true}]}, colB: {values: [{value: 'B', selected: true}]},
      $attrs: {colA: {placeholder: 'FromStoredA'}, colB: {placeholder: 'FromStoredB'}}};
    const updated = components(baseGrid, {
      type: UPDATE_ROW_MODEL,
      address: {component: 'grid', view: 'base', row: 2},
      data: rowUpdate
    });

    const row2 = updated.grid.model.values.find(r => r.id === 2);
    expect(row2).toEqual(rowUpdate);
  });

  it('keeps a row model (KEEP_ROW_MODEL) and stores snapshot', () => {
    // Also ensure that when a cell component exists, it gets storedModel
    const withCells = components(baseGrid, {
      type: GENERATE_CELL_COMPONENTS,
      address: {component: 'grid', view: 'base'},
      data: [1]
    });

    // Ensure storedModel exists on grid
    const withStored = components(withCells, {
      type: KEEP_MODEL,
      address: {component: 'grid', view: 'base'}
    });

    const kept = components(withStored, {
      type: KEEP_ROW_MODEL,
      address: {component: 'grid', view: 'base', row: 1}
    });

    const idA1 = getComponentId({component: 'grid', view: 'base', row: 1, column: 'colA'});
    expect(kept[idA1]).toBeDefined();
    expect(kept[idA1].storedModel).toBeDefined();
  });

  it('resets grid model via RESET_MODEL (grid branch)', () => {
    const reset = components(baseGrid, {
      type: RESET_MODEL,
      address: {component: 'grid', view: 'base'}
    });

    expect(reset.grid.model.values).toEqual([]);
    expect(reset.grid.model.page).toBe(1);
    expect(reset.grid.model.total).toBe(1);
    expect(reset.grid.model.records).toBe(0);
  });

  it('resets a cell model via RESET_MODEL at cell address (resetCellModel)', () => {
    // Choose colA on row 1; default values come from column model
    const reset = components(baseGrid, {
      type: RESET_MODEL,
      address: {component: 'grid', view: 'base', row: 1, column: 'colA'}
    });

    const row1 = reset.grid.model.values.find(r => r.id === 1);
    // After resetCellModel, cell becomes a simple object model with selected value
    expect(row1.colA).toBeDefined();
    expect(row1.colA.value).toBe('A');
  });

  it('restores a specific cell attribute from storedAttributes (restoreAttributeCell)', () => {
    // First, mutate the cell to hold a custom placeholder
    const mutated = {
      ...baseGrid,
      grid: {
        ...baseGrid.grid,
        model: {
          ...baseGrid.grid.model,
          values: baseGrid.grid.model.values.map(v => v.id === 1 ? {
            ...v,
            $attrs: {...v.$attrs, colA: {...v.$attrs.colA, placeholder: 'Temporary'}}
          } : v)
        }
      }
    };

    const restored = components(mutated, {
      type: RESTORE_ATTRIBUTE,
      address: {component: 'grid', view: 'base', row: 1, column: 'colA'},
      data: 'placeholder'
    });

    const row1 = restored.grid.model.values.find(r => r.id === 1);
    expect(row1.$attrs.colA.placeholder).toBe('FromStoredA');
  });
});
