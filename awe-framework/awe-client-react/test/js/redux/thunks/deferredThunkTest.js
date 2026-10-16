import {createStore} from '../../../../src/redux/store';
import {updateViewComponentsWithDependencies} from '../../../../src/redux/thunks/components';
import {initialModelAction, loadDeferredAction} from '../../../../src/redux/thunks/deferred';
import {getComponent} from '../../../../src/utilities';
import {getAllComponents} from '../../../../src/redux/selectors/componentSelectors';
import ViewRegistry from '../../../../src/redux/registry/ViewRegistry';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';

/**
 * A dialog or tab page with the deferred attribute loads the data of its components the first time it is opened (#824)
 */
describe('awe-react-client/test/js/redux/thunks/deferredThunkTest.js', () => {
  const dialog = {view: 'base', component: 'Dia'};
  const options = {view: 'base', component: 'Options'};
  const plain = {view: 'base', component: 'Plain'};
  const outside = {view: 'base', component: 'Outside'};

  let store;

  const component = (address, source, attributes = {}, model = {values: []}) => ({
    address,
    context: {view: address.view, source},
    attributes: {component: 'select', ...attributes},
    model,
    storedModel: {...model},
    defaultModel: {...model}
  });

  const current = (address) => getComponent(getAllComponents(store.getState()), address);

  const registerScreen = (dialogAttributes = {deferred: true}, optionsModel = {values: []}) => {
    store.dispatch(updateViewComponentsWithDependencies('base', {
      Dia: component(dialog, ['Dia'], dialogAttributes),
      Options: component(options, ['Dia', 'Options'], {}, optionsModel),
      Plain: component(plain, ['Dia', 'Plain']),
      Outside: component(outside, ['Outside'], {}, {values: [{value: 'x', label: 'X'}]})
    }));
  };

  const run = (action) => {
    const dispatch = jest.fn();
    action(dispatch, () => store.getState());
    return dispatch;
  };

  const sentActions = (dispatch) => dispatch.mock.calls
    .map(([call]) => call)
    .filter(call => call?.type === 'ADD_ACTIONS_TOP')
    .flatMap(call => call.payload);

  afterEach(() => {
    ComponentRegistry.clearAll();
  });

  beforeEach(() => {
    store = createStore();
    ViewRegistry.setView('base', {name: 'deferred-screen', option: 'DefScr', loading: false});
  });

  describe('loadDeferredAction', () => {
    it('sends one silent async deferred-data server action for the container', () => {
      registerScreen();

      const [serverAction] = sentActions(run(loadDeferredAction(dialog)));

      expect(serverAction.type).toBe('server');
      expect(serverAction.async).toBe(true);
      expect(serverAction.silent).toBe(true);
      expect(serverAction.address).toEqual(dialog);
      expect(serverAction.parameters).toMatchObject({serverAction: 'deferred-data', targetAction: 'Dia', option: 'DefScr'});
    });

    it('marks the container as loaded so it is requested only once', () => {
      registerScreen();
      const thunkDispatch = run(loadDeferredAction(dialog));
      const attributeUpdates = thunkDispatch.mock.calls.map(([call]) => call).filter(call => call?.type === 'UPDATE_ATTRIBUTES');

      expect(attributeUpdates).toHaveLength(1);
      expect(attributeUpdates[0].data).toEqual({deferredLoaded: true});

      store.dispatch(attributeUpdates[0]);
      expect(sentActions(run(loadDeferredAction(dialog)))).toHaveLength(0);
    });

    it('resets the loaded mark if the request fails, so the next opening tries again', () => {
      registerScreen();

      const [serverAction] = sentActions(run(loadDeferredAction(dialog)));

      expect(serverAction.errorAttributes).toEqual({deferredLoaded: false});
    });

    it('leaves the components which load by themselves (autoload) out of the request, as they would load twice', () => {
      store.dispatch(updateViewComponentsWithDependencies('base', {
        Dia: component(dialog, ['Dia'], {deferred: true}),
        Auto: component({view: 'base', component: 'Auto'}, ['Dia', 'Auto'], {autoload: true})
      }));

      const [serverAction] = sentActions(run(loadDeferredAction(dialog)));

      expect(serverAction.parameters.deferredLoaded).toEqual(['Auto']);
    });

    it('does nothing when the container is not deferred', () => {
      registerScreen({});

      expect(run(loadDeferredAction(dialog)).mock.calls).toHaveLength(0);
    });

    it('reports the components below the container which already have data or a value', () => {
      registerScreen({deferred: true}, {values: [{value: '1', label: 'One', selected: true}]});

      const [serverAction] = sentActions(run(loadDeferredAction(dialog)));

      expect(serverAction.parameters.deferredLoaded).toEqual(['Options']);
      expect(serverAction.parameters.deferredValued).toEqual(['Options']);
    });

    it('does not report components outside the container', () => {
      registerScreen();

      const [serverAction] = sentActions(run(loadDeferredAction(dialog)));

      expect(serverAction.parameters.deferredLoaded).toEqual([]);
      expect(serverAction.parameters.deferredValued).toEqual([]);
    });
  });

  describe('initialModelAction', () => {
    const initialModel = (parameters, target = 'Options') => ({type: 'initial-model', address: dialog, target, parameters});

    it('applies the loaded rows and the selected value as the model of the component', () => {
      registerScreen();

      store.dispatch(initialModelAction(initialModel({
        datalist: {rows: [{value: '1', label: 'One'}, {value: '2', label: 'Two'}], page: 1, total: 1, records: 2},
        selected: ['2'],
        defaultValues: ['2']
      })));

      const {model} = current(options);
      expect(model.values.map(row => [row.value, !!row.selected])).toEqual([['1', false], ['2', true]]);
      expect(model.records).toBe(2);
    });

    it('keeps the loaded model as the stored and the default one, like the screen load does', () => {
      registerScreen();

      store.dispatch(initialModelAction(initialModel({
        datalist: {rows: [{value: '1'}, {value: '2'}], page: 1, total: 1, records: 2},
        selected: ['2'],
        defaultValues: ['2']
      })));

      const {storedModel, defaultModel} = current(options);
      expect(storedModel.values.filter(row => row.selected).map(row => row.value)).toEqual(['2']);
      expect(defaultModel.values.filter(row => row.selected).map(row => row.value)).toEqual(['2']);
    });

    it('does not replace the value of a component which already had one', () => {
      registerScreen({deferred: true}, {values: [{value: '1', selected: true}, {value: '3'}]});

      store.dispatch(initialModelAction(initialModel({datalist: {rows: [{value: '1'}, {value: '2'}, {value: '3'}], page: 1, total: 1, records: 3}})));

      const {model} = current(options);
      expect(model.values.map(row => row.value)).toEqual(['1', '2', '3']);
      expect(model.values.filter(row => row.selected).map(row => row.value)).toEqual(['1']);
    });

    it('keeps the selection the component has even if the server sends another one', () => {
      registerScreen({deferred: true}, {values: [{value: '1', selected: true}]});

      store.dispatch(initialModelAction(initialModel({
        datalist: {rows: [{value: '1'}, {value: '2'}], page: 1, total: 1, records: 2},
        selected: ['2'],
        defaultValues: ['2']
      })));

      expect(current(options).model.values.filter(row => row.selected).map(row => row.value)).toEqual(['1']);
    });

    it('refreshes the stored and default models with the loaded rows even when the component keeps its value', () => {
      registerScreen({deferred: true}, {values: [{value: '1', selected: true}]});

      store.dispatch(initialModelAction(initialModel({datalist: {rows: [{value: '1'}, {value: '2'}], page: 1, total: 1, records: 2}})));

      expect(current(options).storedModel.values.map(row => row.value)).toEqual(['1', '2']);
      expect(current(options).defaultModel.values.map(row => row.value)).toEqual(['1', '2']);
    });

    it('restores the default values, which can differ from the loaded selection', () => {
      registerScreen();

      store.dispatch(initialModelAction(initialModel({
        datalist: {rows: [{value: '1'}, {value: '2'}, {value: '3'}], page: 1, total: 1, records: 3},
        selected: ['2'],
        defaultValues: ['3']
      })));

      const selectedOf = (model) => model.values.filter(row => row.selected).map(row => row.value);
      expect(selectedOf(current(options).model)).toEqual(['2']);
      expect(selectedOf(current(options).storedModel)).toEqual(['2']);
      expect(selectedOf(current(options).defaultModel)).toEqual(['3']);
    });

    it('accepts the action of the component identified by its address', () => {
      registerScreen();

      store.dispatch(initialModelAction({type: 'initial-model', address: options, parameters: {datalist: {rows: [{value: 'a'}], page: 1, total: 1, records: 1}}}));

      expect(current(options).model.values).toHaveLength(1);
    });
  });
});
