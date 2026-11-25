import * as dependencies from '../../../../src/redux/actions/dependencies';
import {configureStore} from '@reduxjs/toolkit';
import createRootReducer from '../../../../src/redux/reducers';
import {DEFAULT_SETTINGS} from '../../../../src/redux/actions/settings';
import {updateModel} from '../../../../src/redux/actions/components';
import {
  raiseEventOnDependencies,
  updateModelWithDependencies,
  updateViewComponentsWithDependencies
} from '../../../../src/redux/thunks/components';
import {getComponent} from '../../../../src/utilities';
import {getAllComponents} from '../../../../src/redux/selectors/componentSelectors';

/**
 * #993: the text of a criterion without labels (a text input) is its value, as in AngularJS. The task editors build the
 * command of a task from the text of the command field (a launcher dependency with attribute="text").
 */
describe('awe-react-client/test/js/redux/actions/dependenciesTest.js', () => {
  const SpecificDependencyRegistry = require('../../../../src/redux/registry/SpecificDependencyRegistry').default;
  let dispatch;

  const buildState = (values) => ({
    components: {
      CmdExe: {
        address: {view: 'testView', component: 'CmdExe'},
        attributes: {component: 'custom'},
        validationRules: {},
        model: {values: []},
        dependencies: [{
          type: 'and', initial: true, source: 'launcher', target: 'specific', query: 'commandVal',
          elements: [{id: 'command', attribute1: 'text', alias: 'commandVal', checkChanges: true}]
        }]
      },
      command: {
        address: {view: 'testView', component: 'command'},
        attributes: {component: 'text'},
        validationRules: {},
        model: {values},
        dependencies: []
      }
    },
    settings: {activeDependencies: true, serverActionKey: 'serverAction', targetActionKey: 'targetAction'},
    view: {view: 'testView', testView: {name: 'Test Screen'}}
  });

  beforeEach(() => {
    dispatch = jest.fn();
    jest.spyOn(console, 'info').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    SpecificDependencyRegistry.clear();
    jest.restoreAllMocks();
  });

  it('reads the value of a criterion without label as its text, and the label of one that has it', () => {
    const handler = jest.fn(() => null);
    SpecificDependencyRegistry.register('custom', handler);

    dependencies.initializeDependencies('testView', buildState([{value: 'echo T35', selected: true}]), dispatch);
    dependencies.initializeDependencies('testView', buildState([{value: 'run', label: 'Run report', selected: true}]), dispatch);

    expect(handler.mock.calls.map(([, value]) => value)).toEqual(['echo T35', 'Run report']);
  });
});

/**
 * AngularJS writes the value of an input dependency with Control.changeModelAttribute(..., publish = true)
 * (services/dependency.js, applyTarget "input"), which publishes modelChanged (services/control.js,
 * publishModelChanged) and the screen checks the dependencies again (controllers/screen.js): a criterion whose value
 * a formule (or a value) computes in place launches the dependencies that read it
 */
describe('awe-react-client/test/js/redux/actions/dependenciesTest.js (#993 formule → dependencies)', () => {
  const view = 'report';
  const address = (component) => ({view, component});
  let store;
  let recorded;

  const criterion = (id, dependencies = [], attributes = {}) => ({
    address: address(id),
    context: {view, source: [view, id]},
    attributes: {component: 'text', visible: true, label: 'none', ...attributes},
    model: {values: []},
    dependencies
  });
  const component = (id) => getComponent(getAllComponents(store.getState()), address(id));
  const selected = (id) => (component(id).model.values.find((row) => row.selected) || {}).value;

  const load = (components) => {
    recorded = [];
    const recorder = () => (next) => (action) => {
      recorded.push(action);
      return next(action);
    };
    store = configureStore({
      reducer: createRootReducer(),
      preloadedState: {settings: DEFAULT_SETTINGS},
      middleware: (getDefaultMiddleware) => getDefaultMiddleware({immutableCheck: false, serializableCheck: false})
        .concat(recorder)
    });
    store.dispatch(updateViewComponentsWithDependencies(view, components));
  };

  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('launches the dependencies that read a criterion whose value a formule computes in place', () => {
    load({
      amount: criterion('amount'),
      total: criterion('total', [{
        type: 'and', source: 'formule', formule: '[amount] * 2', target: 'input', initial: false,
        elements: [{id: 'amount', condition: 'is not empty'}], actions: []
      }]),
      reader: criterion('reader', [{
        type: 'and', source: 'formule', formule: '"total " + [total]', target: 'label', initial: false,
        elements: [{id: 'total', condition: 'is not empty'}], actions: []
      }])
    });

    store.dispatch(updateModelWithDependencies(address('amount'), {selected: 5}));

    expect(selected('total')).toBe(10);
    expect(component('reader').attributes.label).toBe('total 10');
  });

  it('launches the dependencies that read a criterion whose value a value dependency sets', () => {
    load({
      type: criterion('type'),
      preset: criterion('preset', [{
        type: 'and', source: 'value', value: 'daily', target: 'input', initial: false,
        elements: [{id: 'type', condition: 'eq', value: '7'}], actions: []
      }]),
      days: criterion('days', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'preset', condition: 'eq', value: 'daily'}], actions: []
      }], {visible: false})
    });

    store.dispatch(updateModelWithDependencies(address('type'), {selected: '7'}));

    expect(selected('preset')).toBe('daily');
    expect(component('days').attributes.visible).toBe(true);
  });

  it('launches the readers only when the written value changes, as AngularJS compares it with the stored one', () => {
    load({
      amount: criterion('amount'),
      total: criterion('total', [{
        type: 'and', source: 'value', value: 'fixed', target: 'input', initial: false,
        elements: [{id: 'amount', condition: 'is not empty'}], actions: []
      }]),
      reader: criterion('reader', [{
        type: 'and', source: 'none', target: 'none', initial: false,
        elements: [{id: 'total', condition: 'is not empty'}], actions: [{type: 'message'}]
      }])
    });
    const launches = () => recorded.filter(({type}) => type === 'ADD_ACTIONS_TOP').length;

    store.dispatch(updateModelWithDependencies(address('amount'), {selected: 1}));
    store.dispatch(updateModelWithDependencies(address('amount'), {selected: 2}));

    expect(selected('total')).toBe('fixed');
    expect(launches()).toBe(1);
  });

  it('launches the dependencies that read a criterion a reset dependency empties', () => {
    load({
      clear: criterion('clear'),
      name: {...criterion('name', [{
        type: 'and', source: 'reset', target: 'input', initial: false,
        elements: [{id: 'clear', condition: 'eq', value: 'yes'}], actions: []
      }]), model: {values: [{value: 'old', selected: true}]}},
      empty: criterion('empty', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'name', condition: 'is empty'}], actions: []
      }], {visible: false})
    });

    store.dispatch(updateModelWithDependencies(address('clear'), {selected: 'yes'}));

    expect(selected('name')).toBeUndefined();
    expect(component('empty').attributes.visible).toBe(true);
  });

  it('launches the dependencies that read a criterion a criteria-value dependency copies into, and those of its group', () => {
    load({
      source: criterion('source'),
      copy: criterion('copy', [{
        type: 'and', source: 'criteria-value', query: 'src', target: 'input', initial: false,
        elements: [{id: 'source', alias: 'src', condition: 'is not empty'}], actions: []
      }], {group: 'copies'}),
      byCriterion: criterion('byCriterion', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'copy', condition: 'eq', value: 'abc'}], actions: []
      }], {visible: false}),
      byGroup: criterion('byGroup', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'copies', condition: 'is not empty'}], actions: []
      }], {visible: false})
    });

    store.dispatch(updateModelWithDependencies(address('source'), {selected: 'abc'}));

    expect(selected('copy')).toBe('abc');
    expect(component('byCriterion').attributes.visible).toBe(true);
    expect(component('byGroup').attributes.visible).toBe(true);
  });

  it('checks again only the readers of the written value, without the event that launched the writer', () => {
    load({
      button: criterion('button'),
      other: criterion('other'),
      field: criterion('field', [{
        type: 'and', source: 'value', value: 'set', target: 'input', initial: false,
        elements: [{id: 'button', event: 'click'}], actions: []
      }]),
      reader: criterion('reader', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'field', condition: 'is not empty'}], actions: []
      }], {visible: false}),
      eventReader: criterion('eventReader', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'button', event: 'click'}, {id: 'field', condition: 'is not empty'}], actions: []
      }], {visible: false}),
      bystander: criterion('bystander', [{
        type: 'and', target: 'show', initial: false,
        elements: [{id: 'other', condition: 'is not empty'}], actions: []
      }], {visible: false})
    });

    // A change of the model that is not checked: a check of every dependency would launch the bystander
    store.dispatch({...updateModel(address('other'), {selected: 'x'}), settings: DEFAULT_SETTINGS});
    store.dispatch(raiseEventOnDependencies(address('button'), 'click'));

    expect(selected('field')).toBe('set');
    expect(component('reader').attributes.visible).toBe(true);
    expect(component('eventReader').attributes.visible).toBe(false);
    expect(component('bystander').attributes.visible).toBe(false);
  });

  it('propagates a linear chain of formules for 10 chained rounds and cuts it there', () => {
    const chain = Object.fromEntries([...Array(13).keys()].map((index) => [`c${index}`, criterion(`c${index}`,
      index === 0 ? [] : [{
        type: 'and', source: 'formule', formule: `[c${index - 1}] + 1`, target: 'input', initial: false,
        elements: [{id: `c${index - 1}`, condition: 'is not empty'}], actions: []
      }])]));
    load(chain);

    store.dispatch(updateModelWithDependencies(address('c0'), {selected: 1}));

    // The change of c0 writes c1, and each of the 10 chained rounds writes the next one, up to c11
    expect([...Array(13).keys()].map((index) => selected(`c${index}`)))
      .toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, undefined]);
    expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('[c11] after 10 rounds'));
  });

  it('stops a chain of formules that never settles instead of hanging the screen', () => {
    load({
      ping: criterion('ping', [{
        type: 'and', source: 'formule', formule: '[pong] + 1', target: 'input', initial: false,
        elements: [{id: 'pong', condition: 'is not empty'}], actions: []
      }]),
      pong: criterion('pong', [{
        type: 'and', source: 'formule', formule: '[ping] + 1', target: 'input', initial: false,
        elements: [{id: 'ping', condition: 'is not empty'}], actions: []
      }])
    });

    store.dispatch(updateModelWithDependencies(address('ping'), {selected: 1}));

    expect(typeof selected('ping')).toBe('number');
    expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('[Dependency]'));
  });
});
