import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {
  generateFooter,
  generateGridFooter,
  updateComponentFooters,
  updateGridFooter
} from '../../../../../src/redux/reducers/components/gridFooter';

const address = {view: 'base', component: 'grid'};

const gridComponent = (showTotals = true) => ({
  address,
  attributes: {
    showTotals,
    columnModel: [
      {name: 'amount', summaryType: 'sum'},
      {name: 'label'}
    ]
  },
  model: {values: [{id: 1, amount: 2, label: 'a'}, {id: 2, amount: 3, label: 'b'}]}
});

describe('awe-react-client/test/js/redux/reducers/components/gridFooterTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  describe('generateFooter', () => {
    it('computes the summary of every column', () => {
      const footer = generateFooter(gridComponent());

      expect(footer.amount.value).toBe(5);
      expect(footer.label).toEqual({value: null, label: ''});
    });

    it('returns an empty footer without columns', () => {
      expect(generateFooter({})).toEqual({});
      expect(generateFooter()).toEqual({});
    });

    it('summarises a grid without model values as empty', () => {
      const footer = generateFooter({attributes: {columnModel: [{name: 'amount', summaryType: 'sum'}]}});

      expect(footer.amount.value).toBe(0);
    });
  });

  describe('generateGridFooter', () => {
    it('attaches the footer to the model keeping the values', () => {
      const result = generateGridFooter(gridComponent());

      expect(result.model.footer.amount.value).toBe(5);
      expect(result.model.values).toHaveLength(2);
    });
  });

  describe('updateGridFooter', () => {
    it('stores the footer as a delta of a registered grid', () => {
      ComponentRegistry.register('grid', gridComponent());

      const state = updateGridFooter({}, address, undefined);

      expect(state.grid.model.footer.amount.value).toBe(5);
    });

    it('regenerates the footer directly on the state for an unregistered grid', () => {
      const state = updateGridFooter({grid: gridComponent()}, address, undefined);

      expect(state.grid.model.footer.amount.value).toBe(5);
    });
  });

  describe('updateComponentFooters', () => {
    it('regenerates only the footers of grids that show totals', () => {
      const plain = {address: {view: 'base', component: 'other'}, attributes: {}};
      const result = updateComponentFooters({grid: gridComponent(), noTotals: gridComponent(false), other: plain});

      expect(result.grid.model.footer.amount.value).toBe(5);
      expect(result.noTotals.model.footer).toBeUndefined();
      expect(result.other).toBe(plain);
    });
  });
});
