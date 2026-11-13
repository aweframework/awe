import {configureStore} from '@reduxjs/toolkit';
import createRootReducer from '../../../../src/redux/reducers';
import {DEFAULT_SETTINGS} from '../../../../src/redux/actions/settings';
import {ADD_ACTIONS_TOP} from '../../../../src/redux/actions/actions';
import {updateModelWithDependencies, updateViewComponentsWithDependencies} from '../../../../src/redux/thunks/components';
import {fillAction} from '../../../../src/redux/thunks/form';
import {selectRowGridAction, updateCellGridAction} from '../../../../src/redux/thunks/grid';
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

/**
 * A query dependency of a side panel launched by the selection of a grid (the additional profiles of the users screen):
 * as in AngularJS, an element with an event launches the dependency when the event is raised, and the end of the event
 * (the next check of the screen, i.e. when the answer of the query fills the criterion) does not launch it again
 */
describe('awe-react-client/test/js/redux/thunks/gridChangeEventThunkTest.js select-row', () => {
  const grid = {view: 'report', component: 'GrdUsrAdm'};
  const criterion = {view: 'report', component: 'AddPro'};
  let recorded;
  let store;

  const queries = () => recorded
    .filter(({type}) => type === ADD_ACTIONS_TOP)
    .flatMap(({actions}) => actions)
    .filter(action => action.type === 'server' && action.parameters.targetAction === 'UsrAddProSel');

  beforeEach(() => {
    recorded = [];
    const recorder = () => (next) => (action) => {
      recorded.push(action);
      return next(action);
    };
    store = configureStore({
      reducer: createRootReducer(),
      preloadedState: {settings: {...DEFAULT_SETTINGS, activeDependencies: true}},
      middleware: (getDefaultMiddleware) => getDefaultMiddleware({immutableCheck: false, serializableCheck: false}).concat(recorder)
    });
    store.dispatch(updateViewComponentsWithDependencies('report', {
      GrdUsrAdm: {
        address: grid,
        context: {view: 'report', source: ['report', 'GrdUsrAdm']},
        attributes: {component: 'grid', columnModel: [{name: 'id'}, {name: 'IdeOpe'}], loading: false},
        model: {values: [{id: '1', IdeOpe: 1}, {id: '2', IdeOpe: 2}]}
      },
      AddPro: {
        address: criterion,
        context: {view: 'report', source: ['report', 'AddPro']},
        attributes: {component: 'suggest-multiple', visible: true},
        model: {values: []},
        dependencies: [{
          type: 'and',
          elements: [
            {id: 'GrdUsrAdm', column1: 'IdeOpe', attribute1: 'selectedRowValue', alias: 'IdeOpeSel', optional: true},
            {id: 'GrdUsrAdm', event: 'select-row', optional: true}
          ],
          source: 'query',
          target: 'input',
          serverAction: 'value',
          targetAction: 'UsrAddProSel',
          silent: true,
          initial: false
        }]
      }
    }));
    recorded = [];
  });

  it('launches the query once per selection, not again when its answer fills the criterion', () => {
    store.dispatch(selectRowGridAction({type: 'select-row', address: grid, parameters: {values: ['1']}}));

    expect(queries()).toHaveLength(1);
    expect(queries()[0].silent).toBe(true);

    // The answer of the query fills the criterion: the screen is checked again
    store.dispatch(updateModelWithDependencies(criterion, {values: [{value: 3, selected: true}]}));

    expect(queries()).toHaveLength(1);

    // Another selection launches it again
    store.dispatch(selectRowGridAction({type: 'select-row', address: grid, parameters: {values: ['2']}}));

    expect(queries()).toHaveLength(2);
  });
});

/**
 * A cell of a new row that stays read-only until another cell of the row is filled (the screen configuration grid): the
 * dependency reads a column of the selected row and listens to after-add-row. As in AngularJS, an edit of a cell of the
 * selected row launches it although its event is not raised; a change of the selection without the event does not
 */
describe('awe-react-client/test/js/redux/thunks/gridChangeEventThunkTest.js after-add-row', () => {
  const grid = {view: 'report', component: 'GrdScrCnf'};
  const criterion = {view: 'report', component: 'Nam'};
  let store;

  const readonly = () => getComponent(getAllComponents(store.getState()), criterion).attributes.readonly;

  beforeEach(() => {
    store = configureStore({
      reducer: createRootReducer(),
      preloadedState: {settings: {...DEFAULT_SETTINGS, activeDependencies: true}},
      middleware: (getDefaultMiddleware) => getDefaultMiddleware({immutableCheck: false, serializableCheck: false})
    });
    store.dispatch(updateViewComponentsWithDependencies('report', {
      GrdScrCnf: {
        address: grid,
        context: {view: 'report', source: ['report', 'GrdScrCnf']},
        attributes: {component: 'grid', columnModel: [{name: 'id'}, {name: 'Scr'}], loading: false},
        model: {values: [{id: '1', Scr: 'users'}]}
      },
      Nam: {
        address: criterion,
        context: {view: 'report', source: ['report', 'Nam']},
        attributes: {component: 'suggest', visible: true, readonly: false},
        model: {values: []},
        dependencies: [{
          type: 'and',
          elements: [
            {id: 'GrdScrCnf', column1: 'Scr', attribute1: 'selectedRowValue', condition: 'is empty'},
            {id: 'GrdScrCnf', event: 'after-add-row', optional: true}
          ],
          target: 'set-readonly',
          initial: true
        }]
      }
    }));
  });

  it('launches the dependency again when a cell of the added row is filled', () => {
    // The added row is selected and its screen is empty: the element is read-only
    store.dispatch(updateModelWithDependencies(grid, {
      values: [{id: '1', Scr: 'users'}, {id: 'new-1', Scr: '', selected: true}],
      event: 'after-add-row'
    }));
    expect(readonly()).toBe(true);

    // The screen of the added row is chosen: the element can be edited
    store.dispatch(updateCellGridAction({type: 'update-cell', address: {...grid, row: 'new-1', column: 'Scr'}, parameters: {data: 'users'}}));
    expect(readonly()).toBe(false);
  });

  it('does not launch the dependency when the selection changes without its event', () => {
    store.dispatch(updateModelWithDependencies(grid, {
      values: [{id: '1', Scr: 'users'}, {id: 'new-1', Scr: '', selected: true}],
      event: 'after-add-row'
    }));
    expect(readonly()).toBe(true);

    // Another row is selected, without after-add-row: the element keeps its state
    store.dispatch(updateModelWithDependencies(grid, {
      values: [{id: '1', Scr: 'users', selected: true}, {id: 'new-1', Scr: ''}]
    }));
    expect(readonly()).toBe(true);
  });
});
