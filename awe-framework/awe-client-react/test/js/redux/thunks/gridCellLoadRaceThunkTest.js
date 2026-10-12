import {createStore} from '../../../../src/redux/store';
import {updateAttributes} from '../../../../src/redux/actions/components';
import {updateViewComponentsWithDependencies} from '../../../../src/redux/thunks/components';
import {endLoadAction, fillAction, selectAction} from '../../../../src/redux/thunks/form';
import {updateCellGridAction} from '../../../../src/redux/thunks/grid';
import {getComponent} from '../../../../src/utilities';
import {getAllComponents} from '../../../../src/redux/selectors/componentSelectors';

/**
 * A pushed update-cell (websocket) can meet a load of the same grid: the load was requested before the push but
 * its response arrives after it. The loaded rows come from a query run before the pushed change, so they must not
 * wipe it out.
 */
describe('awe-react-client/test/js/redux/thunks/gridCellLoadRaceThunkTest.js', () => {
  const address = {view: 'report', component: 'GrdExeLst'};
  const running = {value: 'running', icon: 'fa-refresh fa-spin', style: 'text-info'};
  const success = {value: 'success', icon: 'fa-check', style: 'text-success'};
  const staleRows = () => [{id: '5-1', ExeStaIco: running}];

  let store;

  const cellOf = (rowId) => {
    const component = getComponent(getAllComponents(store.getState()), address);
    return component.model.values.find(row => row.id === rowId)?.ExeStaIco;
  };
  const updateCell = (data) => ({type: 'update-cell', address: {...address, row: '5-1', column: 'ExeStaIco'}, parameters: {data}});
  const fill = (rows) => ({type: 'fill', address, parameters: {datalist: {rows}}});
  const loading = () => getComponent(getAllComponents(store.getState()), address).attributes.loading;

  beforeEach(() => {
    store = createStore();
    store.dispatch(updateViewComponentsWithDependencies('report', {
      GrdExeLst: {
        address,
        context: {view: 'report', source: ['report', 'GrdExeLst']},
        attributes: {component: 'grid', columnModel: [{name: 'id'}, {name: 'ExeStaIco'}], loading: false},
        model: {values: []}
      }
    }));
  });

  it('keeps the pushed cell when a stale load response lands after it (the row was already there)', () => {
    store.dispatch(fillAction(fill(staleRows())));
    store.dispatch(updateAttributes(address, {loading: true}));

    store.dispatch(updateCellGridAction(updateCell(success)));
    expect(cellOf('5-1')).toEqual(success);

    store.dispatch(fillAction(fill(staleRows())));

    expect(cellOf('5-1')).toEqual(success);
    expect(loading()).toBe(false);
  });

  it('applies a cell pushed while the load is pending to a row which only the response brings', () => {
    store.dispatch(updateAttributes(address, {loading: true}));

    store.dispatch(updateCellGridAction(updateCell(success)));
    store.dispatch(fillAction(fill(staleRows())));

    expect(cellOf('5-1')).toEqual(success);
  });

  it('does not keep a pushed cell for the next load', () => {
    store.dispatch(updateAttributes(address, {loading: true}));
    store.dispatch(updateCellGridAction(updateCell(success)));
    store.dispatch(fillAction(fill(staleRows())));

    store.dispatch(updateAttributes(address, {loading: true}));
    store.dispatch(fillAction(fill(staleRows())));

    expect(cellOf('5-1')).toEqual(running);
  });

  it('updates the cell as before when no load is pending', () => {
    store.dispatch(fillAction(fill(staleRows())));
    store.dispatch(fillAction(fill(staleRows())));

    store.dispatch(updateCellGridAction(updateCell(success)));

    expect(cellOf('5-1')).toEqual(success);
  });

  it('does not replay a pushed cell on a later load when the pending load ended in a failure (end-load)', () => {
    store.dispatch(updateAttributes(address, {loading: true}));
    store.dispatch(updateCellGridAction(updateCell(success)));

    store.dispatch(endLoadAction({type: 'end-load', address}));
    expect(loading()).toBe(false);

    store.dispatch(fillAction(fill(staleRows())));

    expect(cellOf('5-1')).toEqual(running);
  });

  it('does not replay a pushed cell on a later load when the loading ended with a selection', () => {
    store.dispatch(updateAttributes(address, {loading: true}));
    store.dispatch(updateCellGridAction(updateCell(success)));

    store.dispatch(selectAction({type: 'select', address, parameters: {values: []}}));

    store.dispatch(fillAction(fill(staleRows())));

    expect(cellOf('5-1')).toEqual(running);
  });

  it('does not recreate a row which the load response does not bring', () => {
    store.dispatch(updateAttributes(address, {loading: true}));
    store.dispatch(updateCellGridAction(updateCell(success)));

    store.dispatch(fillAction(fill([{id: '9-9', ExeStaIco: running}])));

    expect(cellOf('5-1')).toBeUndefined();
    expect(cellOf('9-9')).toEqual(running);
  });
});
