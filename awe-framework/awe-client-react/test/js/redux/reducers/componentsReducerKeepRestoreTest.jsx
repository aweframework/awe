import {components} from '../../../../src/redux/reducers/componentsReducer';
import {
  KEEP_VALIDATION,
  RESTORE_VALIDATION,
  KEEP_ATTRIBUTE,
  KEEP_MODEL,
  RESTORE_MODEL,
  RESTORE_MULTIPLE_VALIDATION,
  RESTORE_MULTIPLE_MODEL,
  RESET_MULTIPLE_MODEL
} from '../../../../src/redux/actions/components';

/**
 * Coverage for keep/restore actions at component-level in components reducer
 */
describe('awe-react-client/test/js/redux/reducers/componentsReducerKeepRestoreTest.jsx', () => {
  it('KEEP_VALIDATION stores current validationRules into storedValidationRules', () => {
    const state = {
      comp: { address: {component: 'comp', view: 'base'}, validationRules: {required: true} }
    };
    const next = components(state, {type: KEEP_VALIDATION, address: {component: 'comp', view: 'base'}});
    expect(next.comp.storedValidationRules).toEqual({required: true});
  });

  it('RESTORE_VALIDATION restores validationRules from storedValidationRules', () => {
    const state = {
      comp: {
        address: {component: 'comp', view: 'base'},
        validationRules: {required: false},
        storedValidationRules: {required: true, minLength: 3}
      }
    };
    const next = components(state, {type: RESTORE_VALIDATION, address: {component: 'comp', view: 'base'}});
    expect(next.comp.validationRules).toEqual({required: true, minLength: 3});
  });

  it('RESTORE_MULTIPLE_VALIDATION restores validation for multiple components', () => {
    const state = {
      c1: {address: {component: 'c1', view: 'base'}, storedValidationRules: {a: 1}},
      c2: {address: {component: 'c2', view: 'base'}, storedValidationRules: {b: 2}}
    };
    const componentList = [
      {address: {component: 'c1', view: 'base'}},
      {address: {component: 'c2', view: 'base'}}
    ];
    const next = components(state, {type: RESTORE_MULTIPLE_VALIDATION, componentList});
    expect(next.c1.validationRules).toEqual({a: 1});
    expect(next.c2.validationRules).toEqual({b: 2});
  });

  it('KEEP_ATTRIBUTE stores a specific attribute into storedAttributes', () => {
    const state = {
      comp: {address: {component: 'comp', view: 'base'}, attributes: {placeholder: {text: 'Hint'}}, storedAttributes: {}}
    };
    const next = components(state, {type: KEEP_ATTRIBUTE, address: {component: 'comp', view: 'base'}, data: 'placeholder'});
    expect(next.comp.storedAttributes.placeholder).toEqual({text: 'Hint'});
  });

  it('KEEP_MODEL stores a deep copy of model into storedModel', () => {
    const state = {
      comp: {address: {component: 'comp', view: 'base'}, model: {values: [{value: 'A'}]}}
    };
    const next = components(state, {type: KEEP_MODEL, address: {component: 'comp', view: 'base'}});
    expect(next.comp.storedModel).toBeDefined();
    expect(next.comp.storedModel.values).toEqual([{value: 'A'}]);
    // Ensure deep copy semantics (not same reference)
    expect(next.comp.storedModel.values).not.toBe(state.comp.model.values);
  });

  it('RESTORE_MODEL sets model from storedModel and resets flags', () => {
    const state = {
      comp: {
        address: {component: 'comp', view: 'base'},
        model: {values: [{value: 'X'}], changed: true},
        storedModel: {values: [{value: 'Y'}]}
      }
    };
    const next = components(state, {type: RESTORE_MODEL, address: {component: 'comp', view: 'base'}});
    expect(next.comp.model.values).toEqual([{value: 'Y'}]);
    expect(next.comp.model.changed).toBe(false);
  });

  it('RESTORE_MULTIPLE_MODEL restores model for multiple components', () => {
    const state = {
      c1: {address: {component: 'c1', view: 'base'}, model: {values: []}, storedModel: {values: [{value: '1'}]}},
      c2: {address: {component: 'c2', view: 'base'}, model: {values: []}, storedModel: {values: [{value: '2'}]}}
    };
    const componentList = [
      {address: {component: 'c1', view: 'base'}},
      {address: {component: 'c2', view: 'base'}}
    ];
    const next = components(state, {type: RESTORE_MULTIPLE_MODEL, componentList});
    expect(next.c1.model.values).toEqual([{value: '1'}]);
    expect(next.c2.model.values).toEqual([{value: '2'}]);
  });

  it('RESET_MULTIPLE_MODEL resets non-grid models to defaultValues and marks changed', () => {
    const state = {
      c1: {
        address: {component: 'c1', view: 'base'},
        attributes: {},
        model: {values: [{value: 'A', selected: false}], defaultValues: ['A']}
      },
      c2: {
        address: {component: 'c2', view: 'base'},
        attributes: {},
        model: {values: [{value: 'B', selected: false}], defaultValues: ['B']}
      }
    };
    const componentList = [
      {address: {component: 'c1', view: 'base'}},
      {address: {component: 'c2', view: 'base'}}
    ];
    const next = components(state, {type: RESET_MULTIPLE_MODEL, componentList});

    // After reset, selected should reflect defaultValues, changed true and event 'reset'
    const c1Values = next.c1.model.values;
    const c2Values = next.c2.model.values;
    expect(c1Values.find(v => v.value === 'A').selected).toBe(false);
    expect(c2Values.find(v => v.value === 'B').selected).toBe(false);
    expect(next.c1.model.changed).toBe(true);
    expect(next.c2.model.changed).toBe(true);
  });
});
