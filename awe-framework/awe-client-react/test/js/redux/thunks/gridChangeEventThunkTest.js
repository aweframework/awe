import {configureStore} from '@reduxjs/toolkit';
import createRootReducer from '../../../../src/redux/reducers';
import {DEFAULT_SETTINGS} from '../../../../src/redux/actions/settings';
import {updateViewComponentsWithDependencies} from '../../../../src/redux/thunks/components';
import {fillAction} from '../../../../src/redux/thunks/form';
import {updateCellGridAction} from '../../../../src/redux/thunks/grid';
import {getComponent} from '../../../../src/utilities';
import {getAllComponents} from '../../../../src/redux/selectors/componentSelectors';

/**
 * AngularJS raises the change event of a grid once its model is loaded (services/grid/base.js, updateModelSpecific),
 * so a dependency with <dependency-element id="grid" event="change"/> runs after every load from the server
 */
describe('awe-react-client/test/js/redux/thunks/gridChangeEventThunkTest.js', () => {
  const grid = {view: 'report', component: 'grid'};
  const criterion = {view: 'report', component: 'criterion'};
  let recorded;
  let store;

  const raisedEvents = () => recorded.filter(({type}) => type === 'SET_RUNTIME_EVENT').map(({address, event}) => [address.component, event]);
  const fill = (address, rows) => ({type: 'fill', address, parameters: {datalist: {rows}}});

  beforeEach(() => {
    recorded = [];
    const recorder = () => (next) => (action) => {
      recorded.push(action);
      return next(action);
    };
    store = configureStore({
      reducer: createRootReducer(),
      preloadedState: {settings: DEFAULT_SETTINGS},
      middleware: (getDefaultMiddleware) => getDefaultMiddleware({immutableCheck: false, serializableCheck: false}).concat(recorder)
    });
    store.dispatch(updateViewComponentsWithDependencies('report', {
      grid: {
        address: grid,
        context: {view: 'report', source: ['report', 'grid']},
        attributes: {component: 'grid', columnModel: [{name: 'id'}, {name: 'name'}], loading: false},
        model: {values: []}
      },
      criterion: {
        address: criterion,
        context: {view: 'report', source: ['report', 'criterion']},
        attributes: {component: 'select', visible: true},
        model: {values: []}
      },
      listener: {
        address: {view: 'report', component: 'listener'},
        context: {view: 'report', source: ['report', 'listener']},
        attributes: {component: 'text', label: 'none'},
        model: {values: []},
        dependencies: [{
          type: 'and',
          elements: [{id: 'grid', event: 'change', checkChanges: false}],
          target: 'label',
          source: 'value',
          value: 'loaded',
          initial: false
        }]
      }
    }));
    recorded = [];
  });

  const label = () => getComponent(getAllComponents(store.getState()), {view: 'report', component: 'listener'}).attributes.label;

  it('raises change once per load of the grid and runs the dependency that listens to it', () => {
    store.dispatch(fillAction(fill(grid, [{id: '1', name: 'a'}])));

    expect(raisedEvents()).toEqual([['grid', 'change']]);
    expect(label()).toBe('loaded');

    store.dispatch(fillAction(fill(grid, [{id: '2', name: 'b'}])));

    expect(raisedEvents()).toEqual([['grid', 'change'], ['grid', 'change']]);
  });

  it('does not raise change on a local edit of a cell', () => {
    store.dispatch(fillAction(fill(grid, [{id: '1', name: 'a'}])));
    recorded = [];

    store.dispatch(updateCellGridAction({type: 'update-cell', address: {...grid, row: '1', column: 'name'}, parameters: {data: 'z'}}));

    expect(raisedEvents()).toEqual([]);
  });

  it('does not raise change when the filled component is not a grid', () => {
    store.dispatch(fillAction(fill(criterion, [{value: '1', label: 'One'}])));

    expect(raisedEvents()).toEqual([]);
    expect(label()).toBe('none');
  });
});
