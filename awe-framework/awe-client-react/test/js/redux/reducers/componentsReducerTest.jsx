import {components} from "../../../../src/redux/reducers/componentsReducer";
import {
  CLEAR_ALL_COMPONENTS,
  CLEAR_COMPONENTS,
  KEEP_MODEL,
  RESET_MODEL,
  UPDATE_ATTRIBUTES,
  UPDATE_COMPONENT,
  UPDATE_MODEL,
  UPDATE_MULTIPLE_ATTRIBUTES,
  UPDATE_MULTIPLE_COMPONENTS,
  UPDATE_SPECIFIC_ATTRIBUTES,
  UPDATE_VIEW_COMPONENTS
} from "../../../../src/redux/actions/components";


describe('awe-react-client/test/js/redux/reducers/componentsReducerTest.jsx', function () {
  let state;

  // Mock module
  beforeEach(function () {
    state = {
      settings: {
        activeDependencies: true,
        lala: "asasdad",
        passwordPattern: "^[a-z]+$"
      },
      components: {
        'component': {
          address: { component: 'component', view: 'base' },
          attributes: { something: "12" }
        },
        'component2': {
          address: { component: 'component2', view: 'base' },
          attributes: {},
          model: { values: [{ selected: true, value: "tutu" }] }
        },
        'tutu': {
          address: { component: 'tutu', view: 'base' },
          attributes: {},
          model: { values: [{ selected: true, value: "lalala" }] }
        }
      }
    };
  });

  it('should clear components', function () {
    const newState = components(state.components, { type: CLEAR_COMPONENTS, view: "base" });
    expect(newState).toEqual({});
  });

  it('should clear all components', function () {
    const newState = components(state.components, { type: CLEAR_ALL_COMPONENTS });
    expect(newState).toEqual({});
  });

  it('should update view components', function () {
    const component = {
      address: { component: 'component', view: 'base' },
      attributes: {}
    };
    const newState = components(state.components, { type: UPDATE_VIEW_COMPONENTS, view: "base", data: { component } });
    expect(newState).toEqual({ component });
  });

  it('should update a component data', function () {
    const component = {
      attributes: { test: true }
    };
    const newState = components(state.components, { type: UPDATE_COMPONENT, view: "base", data: component, address: { component: 'component', view: 'base' } });
    expect(newState.component.attributes).toEqual(component.attributes);
  });

  it('should update multiple components', function () {
    const componentList = [{
      address: { component: "component", view: "base" },
      attributes: { test: true }
    }, {
      address: { component: "component2", view: "base" },
      attributes: { test: true }
    }
    ];
    const newState = components(state.components, { type: UPDATE_MULTIPLE_COMPONENTS, view: "base", componentList });
    expect(newState.component.attributes).toEqual(componentList[0].attributes);
    expect(newState.component2.attributes).toEqual(componentList[1].attributes);
  });

  it('should update a component attributes', function () {
    const attributes = { test: true };
    const newState = components(state.components, { type: UPDATE_ATTRIBUTES, view: "base", data: attributes, address: { component: 'component', view: 'base' } });
    expect(newState.component).toEqual({
      address: { component: 'component', view: 'base' },
      attributes: { something: "12", test: true }
    });
  });

  it('should update a component specific attributes', function () {
    const attributes = { test: true };
    const newState = components(state.components, { type: UPDATE_SPECIFIC_ATTRIBUTES, view: "base", data: attributes, address: { component: 'component', view: 'base' } });
    expect(newState.component).toEqual({
      address: { component: 'component', view: 'base' },
      attributes: { something: "12" },
      specificAttributes: { test: true }
    });
  });

  it('should update multiple attributes', function () {
    const componentList = [{
      address: { component: "component", view: "base" },
      data: { test: true }
    }, {
      address: { component: "component2", view: "base" },
      data: { test: true }
    }
    ];
    const newState = components(state.components, { type: UPDATE_MULTIPLE_ATTRIBUTES, view: "base", componentList });
    expect(newState.component.attributes).toEqual({ ...state.components.component.attributes, ...componentList[0].data });
    expect(newState.component2.attributes).toEqual(componentList[1].data);
  });

  it('should update a component model', function () {
    const model = { values: [{ value: true }] };
    const newState = components(state.components, { type: UPDATE_MODEL, view: "base", data: model, address: { component: 'component', view: 'base' } });
    expect(newState.component).toEqual({
      address: { component: 'component', view: 'base' },
      attributes: { something: "12", error: null },
      model: { values: [{ value: true }], changed: true }
    });
  });

  it('should reset a component model', function () {
    const newState = components(state.components, { type: RESET_MODEL, view: "base", address: { component: 'component', view: 'base' } });
    expect(newState.component).toEqual({
      address: { component: 'component', view: 'base' },
      attributes: { something: "12", error: null },
      model: { values: [], changed: true }
    });
  });

  it('should reset a non-grid model by clearing selections without removing values', function () {
    const newState = components(state.components, { type: RESET_MODEL, view: "base", address: { component: 'component2', view: 'base' } });
    expect(newState.component2).toEqual({
      address: { component: 'component2', view: 'base' },
      attributes: { error: null },
      model: { values: [{ selected: false, value: "tutu" }], changed: true }
    });
  });

  it('should keep a component model', function () {
    const newState = components(state.components, { type: KEEP_MODEL, view: "base", address: { component: 'component2', view: 'base' } });
    expect(newState.component2).toEqual({
      address: { component: 'component2', view: 'base' },
      attributes: {},
      model: { values: [{ selected: true, value: "tutu" }] },
      storedModel: { values: [{ selected: true, value: "tutu" }] }
    });
  });

});
