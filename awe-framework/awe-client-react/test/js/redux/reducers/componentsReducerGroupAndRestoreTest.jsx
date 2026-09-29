import { components } from '../../../../src/redux/reducers/componentsReducer';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';
import { getComponentId } from '../../../../src/utilities/components';
import {
  KEEP_ROW_MODEL,
  RESTORE_ATTRIBUTE,
  UPDATE_MODEL
} from '../../../../src/redux/actions/components';

describe('awe-react-client/test/js/redux/reducers/componentsReducerGroupAndRestoreTest.jsx', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  it('updates selected group across components without registry', () => {
    const state = {
      compA: {
        address: { component: 'compA', view: 'base' },
        attributes: { group: 'g1', component: 'radio' },
        model: { values: [{ value: '1', selected: false }, { value: '2', selected: true }] }
      },
      compB: {
        address: { component: 'compB', view: 'base' },
        attributes: { group: 'g1', component: 'radio' },
        model: { values: [{ value: '1', selected: false }, { value: '2', selected: false }] }
      }
    };

    const next = components(state, {
      type: UPDATE_MODEL,
      address: { component: 'compA', view: 'base' },
      data: { selected: ['1'] }
    });

    expect(next.compA.model.values.map(v => v.selected)).toEqual([true, false]);
    expect(next.compB.model.values.map(v => v.selected)).toEqual([true, false]);
  });

  it('updates selected group across components with registry', () => {
    ComponentRegistry.register('compA', {
      address: { component: 'compA', view: 'base' },
      attributes: { group: 'g1', component: 'radio' },
      model: { values: [{ value: '1', selected: false }, { value: '2', selected: true }] }
    });
    ComponentRegistry.register('compB', {
      address: { component: 'compB', view: 'base' },
      attributes: { group: 'g1', component: 'radio' },
      model: { values: [{ value: '1', selected: false }, { value: '2', selected: false }] }
    });

    const next = components({}, {
      type: UPDATE_MODEL,
      address: { component: 'compA', view: 'base' },
      data: { selected: ['1'] },
      settings: { useComponentRegistry: true }
    });

    expect(next.compA.model.values.map(v => v.selected)).toEqual([true, false]);
    expect(next.compB.model.values.map(v => v.selected)).toEqual([true, false]);
  });

  it('keeps row model for grid and cell components without registry', () => {
    const gridAddress = { component: 'grid', view: 'base' };
    const cellAddress = { ...gridAddress, row: 1, column: 'colA' };
    const cellId = getComponentId(cellAddress);

    const state = {
      grid: {
        address: gridAddress,
        attributes: { id: 'id' },
        model: { values: [{ id: 1, colA: 'A' }] },
        storedModel: { storedRows: {} }
      },
      [cellId]: {
        address: cellAddress,
        model: { values: [{ value: 'A' }] }
      }
    };

    const next = components(state, {
      type: KEEP_ROW_MODEL,
      address: { ...gridAddress, row: 1 }
    });

    expect(next.grid.storedModel.storedRows['1']).toEqual({ id: 1, colA: 'A' });
    expect(next[cellId].storedModel.values).toEqual([{ value: 'A' }]);
  });

  it('keeps row model for grid and cell components with registry', () => {
    const gridAddress = { component: 'grid', view: 'base' };
    const cellAddress = { ...gridAddress, row: 1, column: 'colA' };
    const cellId = getComponentId(cellAddress);

    ComponentRegistry.register('grid', {
      address: gridAddress,
      attributes: { id: 'id' },
      model: { values: [{ id: 1, colA: 'A' }] }
    });
    ComponentRegistry.register(cellId, {
      address: cellAddress,
      model: { values: [{ value: 'A' }] }
    });

    const next = components({}, {
      type: KEEP_ROW_MODEL,
      address: { ...gridAddress, row: 1 },
      settings: { useComponentRegistry: true }
    });

    expect(next.grid.storedModel.storedRows['1']).toEqual({ id: 1, colA: 'A' });
    expect(next[cellId].storedModel.values).toEqual([{ value: 'A' }]);
  });

  it('restores component attributes without registry', () => {
    const state = {
      comp: {
        address: { component: 'comp', view: 'base' },
        attributes: { placeholder: 'new' },
        storedAttributes: { placeholder: 'old' }
      }
    };

    const next = components(state, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'comp', view: 'base' },
      data: 'placeholder'
    });

    expect(next.comp.attributes.placeholder).toBe('old');
  });

  it('restores component attributes with registry', () => {
    ComponentRegistry.register('comp', {
      address: { component: 'comp', view: 'base' },
      attributes: { placeholder: 'new' },
      storedAttributes: { placeholder: 'old' }
    });

    const next = components({}, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'comp', view: 'base' },
      data: 'placeholder',
      settings: { useComponentRegistry: true }
    });

    expect(next.comp.attributes.placeholder).toBe('old');
  });

  it('restores column attributes without registry', () => {
    const state = {
      grid: {
        address: { component: 'grid', view: 'base' },
        attributes: {
          columnModel: [{ name: 'colA', width: 200 }]
        },
        storedAttributes: {
          columnModel: [{ width: 100 }]
        }
      }
    };

    const next = components(state, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'grid', view: 'base', column: 'colA' },
      data: 'width'
    });

    expect(next.grid.attributes.columnModel[0].width).toBe(100);
  });

  it('restores column attributes with registry', () => {
    ComponentRegistry.register('grid', {
      address: { component: 'grid', view: 'base' },
      attributes: {
        columnModel: [{ name: 'colA', width: 200 }]
      },
      storedAttributes: {
        columnModel: [{ width: 100 }]
      }
    });

    const next = components({}, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'grid', view: 'base', column: 'colA' },
      data: 'width',
      settings: { useComponentRegistry: true }
    });

    expect(next.grid.attributes.columnModel[0].width).toBe(100);
  });

  it('restores cell attributes without registry', () => {
    const state = {
      grid: {
        address: { component: 'grid', view: 'base' },
        attributes: {
          id: 'id',
          columnModel: [{ name: 'colA' }]
        },
        storedAttributes: {
          columnModel: [{ placeholder: 'old' }]
        },
        model: {
          values: [
            { id: 1, colA: 'A', $attrs: { colA: { placeholder: 'new' } } }
          ]
        }
      }
    };

    const next = components(state, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: 'placeholder'
    });

    expect(next.grid.model.values[0].$attrs.colA.placeholder).toBe('old');
  });

  it('restores cell attributes with registry', () => {
    ComponentRegistry.register('grid', {
      address: { component: 'grid', view: 'base' },
      attributes: {
        id: 'id',
        columnModel: [{ name: 'colA' }]
      },
      storedAttributes: {
        columnModel: [{ placeholder: 'old' }]
      },
      model: {
        values: [
          { id: 1, colA: 'A', $attrs: { colA: { placeholder: 'new' } } }
        ]
      }
    });

    const next = components({}, {
      type: RESTORE_ATTRIBUTE,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: 'placeholder',
      settings: { useComponentRegistry: true }
    });

    expect(next.grid.model.values[0].$attrs.colA.placeholder).toBe('old');
  });
});
