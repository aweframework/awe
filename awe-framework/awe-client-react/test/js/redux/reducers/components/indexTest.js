import {actionHandlers, components} from '../../../../../src/redux/reducers/components';
import {components as reexported} from '../../../../../src/redux/reducers/componentsReducer';
import {attributeHandlers} from '../../../../../src/redux/reducers/components/attributes';
import {gridModelHandlers} from '../../../../../src/redux/reducers/components/gridModel';
import {lifecycleHandlers} from '../../../../../src/redux/reducers/components/lifecycle';
import {modelHandlers} from '../../../../../src/redux/reducers/components/model';
import {validationRuleHandlers} from '../../../../../src/redux/reducers/components/validationRules';

const EXPECTED_ACTIONS = [
  'CLEAR_ALL_COMPONENTS', 'CLEAR_COMPONENTS', 'KEEP_ATTRIBUTE', 'KEEP_MODEL', 'KEEP_ROW_MODEL', 'KEEP_VALIDATION',
  'REGISTER_COMPONENTS', 'RESET_MODEL', 'RESET_MULTIPLE_MODEL', 'RESTORE_ATTRIBUTE', 'RESTORE_MODEL', 'RESTORE_MULTIPLE_ATTRIBUTES',
  'RESTORE_MULTIPLE_MODEL', 'RESTORE_MULTIPLE_VALIDATION', 'RESTORE_VALIDATION', 'UPDATE_ATTRIBUTES',
  'UPDATE_COMPONENT', 'UPDATE_MODEL', 'UPDATE_MULTIPLE_ATTRIBUTES', 'UPDATE_MULTIPLE_COMPONENTS',
  'UPDATE_MULTIPLE_MODELS', 'UPDATE_MULTIPLE_VALIDATION', 'UPDATE_ROW_MODEL', 'UPDATE_SPECIFIC_ATTRIBUTES',
  'UNREGISTER_COMPONENTS', 'UPDATE_VALIDATION', 'UPDATE_VIEW_COMPONENTS', 'VALIDATE_COMPONENTS', 'VALIDATE_ROW'
];

const maps = [lifecycleHandlers, attributeHandlers, validationRuleHandlers, modelHandlers, gridModelHandlers];

describe('components reducer composition', () => {
  it('handles every action type of the original handler map', () => {
    expect(Object.keys(actionHandlers).sort()).toEqual([...EXPECTED_ACTIONS].sort());
  });

  it('gives every action type exactly one handler across the module maps', () => {
    const keys = maps.flatMap(map => Object.keys(map));

    expect(new Set(keys).size).toEqual(keys.length);
    expect(keys.length).toEqual(EXPECTED_ACTIONS.length);
  });

  it('returns the same state reference for an unknown action', () => {
    const state = {a: {}};

    expect(components(state, {type: 'UNKNOWN'})).toBe(state);
    expect(components(state)).toBe(state);
  });

  it('starts from an empty state', () => {
    expect(components(undefined, {type: 'UNKNOWN'})).toEqual({});
  });

  it('is still exported from componentsReducer', () => {
    expect(reexported).toBe(components);
  });
});
