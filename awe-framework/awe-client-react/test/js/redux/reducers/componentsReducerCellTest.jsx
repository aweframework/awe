import { components } from '../../../../src/redux/reducers/componentsReducer';
import {
  UPDATE_ATTRIBUTES,
  UPDATE_MODEL,
  UPDATE_ROW_MODEL,
  UPDATE_VALIDATION
} from '../../../../src/redux/actions/components';

describe('awe-react-client/test/js/redux/reducers/componentsReducerCellTest.jsx', () => {
  let baseState;

  beforeEach(() => {
    baseState = {
      grid: {
        address: { component: 'grid', view: 'base' },
        attributes: {
          id: 'id',
          showTotals: true,
          columnModel: [
            { name: 'colA', summaryType: 'sum' }
          ]
        },
        model: {
          values: [
            { id: 1, colA: 5, $attrs: { colA: { placeholder: 'old', validationRules: { required: true } } } },
            { id: 2, colA: 10, $attrs: { colA: { placeholder: 'old2' } } }
          ]
        }
      }
    };
  });

  it('updates cell attributes when address includes row and column', () => {
    const next = components(baseState, {
      type: UPDATE_ATTRIBUTES,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: { placeholder: 'new', disabled: true }
    });

    const row1 = next.grid.model.values.find(row => row.id === 1);
    expect(row1.$attrs.colA.placeholder).toBe('new');
    expect(row1.$attrs.colA.disabled).toBe(true);
    expect(row1.$attrs.colA.validationRules).toEqual({ required: true });
  });

  it('merges validation rules into cell attributes when updating validation at cell level', () => {
    const next = components(baseState, {
      type: UPDATE_VALIDATION,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: { minLength: 2 }
    });

    const row1 = next.grid.model.values.find(row => row.id === 1);
    expect(row1.$attrs.colA.validationRules).toEqual({ required: true, minLength: 2 });
  });

  it('does not change state when cell selection stays the same', () => {
    const state = {
      grid: {
        address: { component: 'grid', view: 'base' },
        attributes: {
          id: 'id',
          showTotals: false,
          columnModel: [{ name: 'colA' }]
        },
        model: {
          values: [
            { id: 1, colA: { value: 'A' } }
          ]
        }
      }
    };

    const next = components(state, {
      type: UPDATE_MODEL,
      address: { component: 'grid', view: 'base', row: 1, column: 'colA' },
      data: { selected: ['A'] }
    });

    expect(next).toBe(state);
  });

  it('updates grid footer after updating a row model when totals are enabled', () => {
    const next = components(baseState, {
      type: UPDATE_ROW_MODEL,
      address: { component: 'grid', view: 'base', row: 1 },
      data: { id: 1, colA: 15 }
    });

    expect(next.grid.model.footer.colA.value).toBe(25);
  });
});
