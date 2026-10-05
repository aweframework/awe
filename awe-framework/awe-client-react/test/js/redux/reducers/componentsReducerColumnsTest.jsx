import {components} from "../../../../src/redux/reducers/componentsReducer";
import {UPDATE_MODEL, UPDATE_VALIDATION, UPDATE_ATTRIBUTES, RESTORE_ATTRIBUTE} from "../../../../src/redux/actions/components";

/**
 * New tests focused on column-level updates in componentsReducer
 */
describe('awe-react-client/test/js/redux/reducers/componentsReducerColumnsTest.jsx', function() {
  let state;

  beforeEach(function () {
    state = {
      grid: {
        address: {component: 'grid', view: 'base'},
        attributes: {
          columnModel: [
            {name: 'colA', model: {values: [{value: '1', label: 'One'}]}, validationRules: {required: false}},
            {name: 'colB', model: {values: [{value: 'X', label: 'Ex'}]}, validationRules: {}}
          ],
          id: 'id'
        },
        model: {
          values: [
            {id: 1, colA: {values: [{value: '1', selected: true}]}, colB: 'X'},
            {id: 2, colA: {values: [{value: '1', selected: false}]}, colB: 'X'}
          ]
        }
      }
    };
  });

  it('should update validation rules at column level (updateValidationColumn)', function () {
    const newState = components(state, {
      type: UPDATE_VALIDATION,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {required: true, pattern: '^[0-9]$'}
    });

    expect(newState.grid.attributes.columnModel[0].validationRules).toEqual({required: true, pattern: '^[0-9]$'});
    // Other columns remain untouched
    expect(newState.grid.attributes.columnModel[1].validationRules).toEqual({});
  });

  it('should update model definition at column level (updateColumnModel)', function () {
    const newState = components(state, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base', column: 'colB'},
      data: {values: [{value: 'Y', label: 'Why'}], extra: true}
    });

    expect(newState.grid.attributes.columnModel[1].model).toEqual({values: [{value: 'Y', label: 'Why'}], extra: true});
    // colA model remains the same
    expect(newState.grid.attributes.columnModel[0].model).toEqual({values: [{value: '1', label: 'One'}]});
  });

  it('should update validation at cell level does not affect column validation', function () {
    const newState = components(state, {
      type: UPDATE_VALIDATION,
      address: {component: 'grid', view: 'base', column: 'colA', row: 1},
      data: {minLength: 2}
    });

    // Column validation stays the same
    expect(newState.grid.attributes.columnModel[0].validationRules).toEqual({required: false});
    // Cell validation is merged on the cell model
    const updatedRow = newState.grid.model.values.find(r => String(r.id) === '1');
    expect(updatedRow.$attrs.colA.validationRules).toEqual({minLength: 2});
  });
  it('should keep the number format separators of a column when a partial number format is applied to it', function () {
    state.grid.attributes.columnModel[0].numberFormat = {digitGroupSeparator: '.', decimalCharacter: ',', mDec: 2};

    const newState = components(state, {
      type: UPDATE_ATTRIBUTES,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {numberFormat: {mDec: 2, aSign: ' $', pSign: 's'}}
    });

    // The separators come from the application settings: a format that only changes the sign must not drop them
    expect(newState.grid.attributes.columnModel[0].numberFormat).toEqual({
      digitGroupSeparator: '.', decimalCharacter: ',', mDec: 2, aSign: ' $', pSign: 's'
    });
    expect(newState.grid.attributes.columnModel[1].numberFormat).toBeUndefined();
  });

  it('should update column attributes via UPDATE_ATTRIBUTES without affecting other columns', function () {
    const newState = components(state, {
      type: UPDATE_ATTRIBUTES,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {width: 250, headerStyle: 'bold'}
    });
    expect(newState.grid.attributes.columnModel[0].width).toBe(250);
    expect(newState.grid.attributes.columnModel[0].headerStyle).toBe('bold');
    // other column untouched
    expect(newState.grid.attributes.columnModel[1].width).toBeUndefined();
  });

  it('should overwrite validationRules when using UPDATE_ATTRIBUTES at column level', function () {
    const newState = components(state, {
      type: UPDATE_ATTRIBUTES,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {validationRules: {min: 1}}
    });
    expect(newState.grid.attributes.columnModel[0].validationRules).toEqual({min: 1});
  });

  it('should restore a specific column attribute from storedAttributes (restoreAttributeColumn)', function () {
    const baseState = {
      grid: {
        address: {component: 'grid', view: 'base'},
        attributes: {
          columnModel: [
            {name: 'colA', model: {values: [{value: '1', label: 'One'}]}, validationRules: {required: false}},
            {name: 'colB', model: {values: [{value: 'X', label: 'Ex'}]}, validationRules: {}}
          ],
          id: 'id'
        },
        storedAttributes: {
          columnModel: [
            {model: {values: [{value: '1', label: 'One'}]}, validationRules: {required: true}},
            {model: {values: [{value: 'X', label: 'Ex'}]}, validationRules: {max: 5}}
          ]
        },
        model: state.grid.model
      }
    };

    // First change the validationRules on colB
    const changed = components(baseState, {
      type: UPDATE_ATTRIBUTES,
      address: {component: 'grid', view: 'base', column: 'colB'},
      data: {validationRules: {different: true}}
    });
    expect(changed.grid.attributes.columnModel[1].validationRules).toEqual({different: true});

    // Now restore validationRules from storedAttributes for colB
    const restored = components(changed, {
      type: RESTORE_ATTRIBUTE,
      address: {component: 'grid', view: 'base', column: 'colB'},
      data: 'validationRules'
    });
    expect(restored.grid.attributes.columnModel[1].validationRules).toEqual({max: 5});
  });

  it('should merge validation rules when using UPDATE_VALIDATION at column level (merge behavior)', function () {
    const first = components(state, {
      type: UPDATE_VALIDATION,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {pattern: '^[0-9]$'}
    });
    // required stays (false) and pattern is added
    expect(first.grid.attributes.columnModel[0].validationRules).toEqual({required: false, pattern: '^[0-9]$'});

    const second = components(first, {
      type: UPDATE_VALIDATION,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {required: true}
    });
    expect(second.grid.attributes.columnModel[0].validationRules).toEqual({required: true, pattern: '^[0-9]$'});
  });

  it('should merge the column model definition rather than overwrite non-provided keys', function () {
    const newState = components(state, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {extra: true}
    });
    expect(newState.grid.attributes.columnModel[0].model).toEqual({values: [{value: '1', label: 'One'}], extra: true});
  });

  it('should not change grid values when updating a column model definition', function () {
    const beforeValues = JSON.stringify(state.grid.model.values);
    const newState = components(state, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {extra: true}
    });
    expect(JSON.stringify(newState.grid.model.values)).toBe(beforeValues);
  });

  it('should ignore UPDATE_MODEL with only selected at column address (no-op)', function () {
    const newState = components(state, {
      type: UPDATE_MODEL,
      address: {component: 'grid', view: 'base', column: 'colA'},
      data: {selected: ['1']}
    });
    // For column address, selected should not be processed; state equals previous
    expect(newState).toEqual(state);
  });
});
