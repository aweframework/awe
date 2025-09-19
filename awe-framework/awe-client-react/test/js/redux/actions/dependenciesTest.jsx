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
  });

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
            type: "and", source: "value", value: "2", target: "disable-autorefresh", initial: true,
            elements: [{id: 'component2', checkChanges: true}, {id: 'component3', checkChanges: true}],
            actions: []
          }]
        }
      }};
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle show and hide dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'show', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'hide', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle show-column and hide-column dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'show-column', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'hide-column', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle set-visible and set-invisible dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'set-visible', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'set-invisible', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle enable and disable dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'enable', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'disable', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle set-editable and set-readonly dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'set-editable', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'set-readonly', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle set-required and set-optional dependencies', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', target: 'set-required', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] },
            { type: 'and', source: 'value', target: 'set-optional', initial: true, elements: [{ id: 'component2', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should execute validate-false (restoreValidation) when condition is false', function () {
    // Use component3 null value to make the trigger false (is not empty => false)
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'value', value: 'required', target: 'validate', initial: true, elements: [{ id: 'component3', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should execute format-number-false (restoreAttributes) when condition is false', function () {
    // Use component3 null value to make the trigger false
    state = {
      ...state,
      components: {
        ...state.components,
        component: {
          ...state.components.component,
          dependencies: [
            { type: 'and', source: 'formule', formule: "{format:'lala'}", target: 'format-number', initial: true, elements: [{ id: 'component3', checkChanges: true }], actions: [] }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle hasDataColumn on a grid column', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        grid: {
          address: { component: 'grid', view: 'base' },
          attributes: {},
          model: { values: [ { c1: 'A' }, { c1: null }, { c1: '' } ] }
        },
        component: {
          ...state.components.component,
          dependencies: [
            {
              type: 'and', source: 'value', target: 'show', initial: true,
              elements: [ { id: 'grid', attribute1: 'hasDataColumn', column1: 'c1', condition: 'is not false', checkChanges: true } ],
              actions: []
            }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle emptyDataColumn on a grid column', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        grid: {
          address: { component: 'grid', view: 'base' },
          attributes: {},
          model: { values: [ { c1: null }, { c1: '' }, { c1: undefined } ] }
        },
        component: {
          ...state.components.component,
          dependencies: [
            {
              type: 'and', source: 'value', target: 'show', initial: true,
              elements: [ { id: 'grid', attribute1: 'emptyDataColumn', column1: 'c1', condition: 'is not false', checkChanges: true } ],
              actions: []
            }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });

  it('should handle fullDataColumn on a grid column', function () {
    state = {
      ...state,
      components: {
        ...state.components,
        grid: {
          address: { component: 'grid', view: 'base' },
          attributes: {},
          model: { values: [ { c1: 'A' }, { c1: 'B' }, { c1: 'C' } ] }
        },
        component: {
          ...state.components.component,
          dependencies: [
            {
              type: 'and', source: 'value', target: 'show', initial: true,
              elements: [ { id: 'grid', attribute1: 'fullDataColumn', column1: 'c1', condition: 'is not false', checkChanges: true } ],
              actions: []
            }
          ]
        }
      }
    };
    initializeDependencies('base', state, dispatch);
    checkDependencies(state, dispatch);
    expect(dispatch).toHaveBeenCalled();
  });
});
