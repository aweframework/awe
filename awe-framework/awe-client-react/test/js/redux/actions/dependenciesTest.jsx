import {checkDependencies, initializeDependencies} from "../../../../src/redux/actions/dependencies";

describe('awe-react-client/test/js/redux/actions/dependenciesTest.jsx', function () {
  let state;
  let dispatch;

  beforeEach(function () {
    state = {
      settings: {activeDependencies: true},
      components: {
        'component': {
          address: {component: 'component', view: 'base'},
          attributes: {},
          dependencies: [{
            initial: true,
            elements: [{id: 'component2', checkChanges: true}, {id: 'component3', optional: true}],
            actions: []
          }]
        },
        'component2': {
          address: {component: 'component2', view: 'base'},
          attributes: {},
          model: {values: [{selected: true, value: "tutu"}]}
        },
        'component3': {
          address: {component: 'component3', view: 'base'},
          attributes: {},
          model: {values: [{selected: true, value: null}]}
        }
      }
    };

    dispatch = jasmine.createSpy('dispatch');
  })

  it('should initialize and check an empty dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component2: {
          ...state.components.component2,
          model: {values: [{selected: true, value: "tutu"}]}
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).not.toHaveBeenCalled();
  });

  it('should initialize and check a unit dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "value", target: "unit", initial: true,
            elements: [{id: 'component2', checkChanges: true}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "value", value: "value", target: "unit", initial: true,
            elements: [{id: 'component4'}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check an icon dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "label", label: "value", target: "icon", initial: true,
            elements: [{id: 'component2', checkChanges: true}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "formule", formule: "1 + 2", target: "icon", initial: true,
            elements: [{id: 'component4'}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check a label dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "criteria-value", query: "criteriaValue", target: "label", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "label", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check a chart-options dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "formule", formule: "{test:1}", target: "chart-options", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "chart-options", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check an attribute dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "2", query: "label", target: "attribute", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "attribute", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check an input dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "2", target: "input", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "input", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check an input dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "formule", formule: "{format:'lala'}", target: "format-number", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "format-number", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check a validate dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "required", target: "validate", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", query: "criteriaASecas", target: "validate", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check an enable-autorefresh dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "2", target: "enable-autorefresh", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", value: "2", query: "criteriaASecas", target: "enable-autorefresh", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should initialize and check a disable-autorefresh dependency', function () {
    state = {...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [{
            type: "and", source: "value", value: "2", target: "enable-autorefresh", initial: true,
            elements: [{id: 'component2', checkChanges: true, alias: "criteriaValue"}, {id: 'component3', optional: true, checkChanges: true}],
            actions: []
          },{
            type: "and", source: "launcher", value: "2", query: "criteriaASecas", target: "enable-autorefresh", initial: true,
            elements: [{id: 'component4', alias: "criteriaASecas"}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });
});