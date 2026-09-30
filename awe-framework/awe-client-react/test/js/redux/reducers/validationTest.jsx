import {validateComponent, validateRow} from "../../../../src/redux/reducers/validation";
import {parseValidationRules} from "../../../../src/utilities/components";

import {isEmpty} from "../../../../src/utilities/general";
import ComponentRegistry from "../../../../src/redux/registry/ComponentRegistry";

function launchTest(state, validation, values, expected) {
  let validationRules = parseValidationRules(validation, state.components.component.address);
  let components = Object.entries({
    ...state.components,
    component: {
      ...state.components.component,
      validationRules
    }
  }).reduce((components, [key, component]) => ({...components, [key]: key in values ? {...component, model: {values: isEmpty(values[key]) ? [] : [{value: values[key], selected: true}]}}: component}), {});
  //console.info("Initial components: \n" + JSON.stringify(components, undefined, 2));
  //console.info("Validation: " + validation);
  let result = validateComponent(components, components.component, state.settings);
  //console.info("Result: \n" + JSON.stringify(result, undefined, 2));

  Object.entries(expected || {}).forEach(([key, value]) => {
    expect((result.component.attributes.error || {})[key]).toEqual(value);
  });
}

describe('awe-react-client/test/js/redux/reducers/validationTest.jsx', function() {
  let state;
  let stateWithGroup;

  // Mock module
  beforeEach(function() {
    ComponentRegistry.clearAll();
    state = {
      settings: {
        activeDependencies: true,
        lala: "asasdad",
        passwordPattern: "^[a-z]+$"
      },
      components: {
        'component': {
          address: {component: 'component', view: 'base'},
          attributes: {}
        },
        'component2': {
          address: {component: 'component2', view: 'base'},
          attributes: {},
          model: {values: [{selected: true, value: "tutu"}]}
        },
        'tutu': {
          address: {component: 'tutu', view: 'base'},
          attributes: {},
          model: {values: [{selected: true, value: "lalala"}]}
        }
      }
    };

    stateWithGroup = {
      settings: {
        activeDependencies: true,
        lala: "asasdad",
        passwordPattern: "^[a-z]+$"
      },
      components: {
        'component': {
          address: {component: 'component', view: 'base'},
          attributes: { group: 'test' }
        },
        'component2': {
          address: {component: 'component2', view: 'base'},
          attributes: { group: 'test' },
          model: {values: [{selected: true, value: "tutu"}]}
        },
        'component3': {
          address: {component: 'component3', view: 'base'},
          attributes: { group: 'test' },
          model: {values: [{selected: true, value: "lalala"}]}
        }
      }
    };
  });

  /**
   * Test a validation
   * @param validation Validation
   * @param value Value to add
   * @param expected Expected result
   */
  function testValidation(validation, value, expected) {
    launchTest(state, validation, {component: value}, expected);
  }

  /**
   * Test a validation with groups
   * @param validation Validation
   * @param values Value to add
   * @param expected Expected result
   */
  function testGroupValidation(validation, values, expected) {
    launchTest(stateWithGroup, validation, values, expected);
  }

  // Require validation
  it('should launch a require validation OK', function() {
    testValidation("required", "test", null);
  });

  // Require validation failed
  it('should launch a require validation KO', function() {
    testValidation("{required:{value: true}}", "", { message: 'VALIDATOR_MESSAGE_REQUIRED' });
  });

  // Require validation group
  it('should launch a group require validation OK', function() {
    testGroupValidation("required", {component: null}, true, null);
  });

  // Require validation group
  it('should launch a group require validation KO', function() {
    testGroupValidation("{required: {value: true}}", {component: null, component2: null, component3: null}, { message: "VALIDATOR_MESSAGE_REQUIRED" } );
  });

  // Require validation multiple
  it('should launch a multiple require validation OK', function() {
    testValidation("required", ["tutu", "lala"], null);
  });

  // Require validation group
  it('should launch a multiple require validation KO', function() {
    testValidation("{required: {value: true}}", [], { message: "VALIDATOR_MESSAGE_REQUIRED" } );
  });

  // Text validation
  it('should launch a text validation OK', function() {
    testValidation("text", "asda", null);
  });

  // Text validation
  it('should launch a text validation KO', function() {
    testValidation("{text: {message: \"Naaaah, no es igual\"}}", "45225", { message: 'Naaaah, no es igual' });
  });

  // Text with spaces validation
  it('should launch a text with spaces validation OK', function() {
    testValidation("textWithSpaces", "asda as dtr", null);
  });

  // Text with spaces validation
  it('should launch a text with spaces validation KO', function() {
    testValidation("{textWithSpaces: {message: \"Naaaah, {value1} no tiene espacios\"}}", "asda", { message: 'Naaaah, {value1} no tiene espacios' });
  });

  // Number validation
  it('should launch a number validation OK', function() {
    testValidation("number", "123.12", null);
  });

  // Number validation
  it('should launch a number validation KO', function() {
    testValidation("{number: {message: \"Naaaah, no es numerico\"}}", "as33sd", { message: 'Naaaah, no es numerico' });
  });

  // Integer validation
  it('should launch a integer validation OK', function() {
    testValidation("integer", "1232", null);
  });

  // Integer validation
  it('should launch a integer validation KO', function() {
    testValidation("{integer: {message: \"Naaaah, no es entero\"}}", "1232.21", { message: 'Naaaah, no es entero' });
  });

  // Digits validation
  it('should launch a digits validation OK', function() {
    testValidation("digits", "071232", null);
  });

  // Digits validation
  it('should launch a digits validation KO', function() {
    testValidation("{digits: {message: \"Naaaah, no son solo digitos\"}}", "1232.21", { message: 'Naaaah, no son solo digitos' });
  });

  describe('component registry', function() {
    it('should validate component and store error as delta when registry is enabled', function() {
      const baseComponent = {
        address: {component: 'comp', view: 'base'},
        attributes: {id: 'comp'},
        validationRules: {required: {value: true}},
        model: {values: []}
      };
      ComponentRegistry.register('comp', baseComponent);

      const registryState = {
        settings: {useComponentRegistry: true},
        components: {comp: {}}
      };

      const next = validateComponent(registryState.components, baseComponent, registryState.settings);
      expect(next.comp.attributes.error.message).toBe('VALIDATOR_MESSAGE_REQUIRED');
    });

    it('should validate grid row and store row attrs as delta when registry is enabled', function() {
      const baseGrid = {
        address: {component: 'grid', view: 'base'},
        attributes: {
          id: 'id',
          columnModel: [{name: 'col1', component: 'text', validationRules: {required: {value: true}}}]
        },
        model: {values: [{id: '1', col1: ''}]}
      };
      ComponentRegistry.register('grid', baseGrid);

      const registryState = {
        settings: {useComponentRegistry: true},
        components: {grid: {}}
      };

      const next = validateRow(registryState.components, {component: 'grid', view: 'base', row: '1'}, registryState.settings);
      expect(next.grid.model.values[0].$attrs.col1.error.message).toBe('VALIDATOR_MESSAGE_REQUIRED');
    });
  });

  // Email validation
  it('should launch a email validation OK', function() {
    testValidation("email", "test@awe.com", null);
  });

  // Email validation
  it('should launch a email validation KO', function() {
    testValidation("{email: {message: \"Naaaah, {value1} no es válido\"}}", "tutu@alal@aea.qwq", { message: 'Naaaah, {value1} no es válido' });
  });

  // Date validation
  it('should launch a date validation OK', function() {
    testValidation("date", "21/01/2018", null);
  });

  // Date validation
  it('should launch a date validation KO', function() {
    testValidation("{date: {message: \"Naaaah, {value1} no es válido\"}}", "29/02/2019", { message: 'Naaaah, {value1} no es válido' });
  });

  // Date validation
  it('should launch a date validation KO (bad formatted)', function() {
    testValidation("{date: {message: \"Naaaah, {value1} no es válido\"}}", "29-02-2019", { message: 'Naaaah, {value1} no es válido' });
  });

  // Time validation
  it('should launch a time validation OK', function() {
    testValidation("time", "23:11:02", null);
  });

  // Time validation
  it('should launch a time validation KO', function() {
    testValidation("{time: {message: \"Naaaah, {value1} no es válido\"}}", "25:11:02", { message: 'Naaaah, {value1} no es válido' });
  });

  // Equal validation
  it('should launch an equal validation OK', function() {
    testValidation("{eq: {criterion: \"tutu\"}}", "asda", null);
  });

  // Equal validation
  it('should launch an equal validation KO', function() {
    testValidation("{eq: {criterion: \"tutu\", message: \"Naaaah, no es igual\"}}", "asda",{ message: 'Naaaah, no es igual' });
  });

  // NE validation
  it('should launch a ne validation OK', function() {
    testValidation("{ne: {setting: \"lala\"}}", "asda", null);
  });

  // NE validation
  it('should launch a ne validation KO', function() {
    testValidation("{ne: {criterion: \"tutu\", message: \"Naaaah, no es igual\"}}", "lalala", { message: 'Naaaah, no es igual' });
  });

  // LT validation
  it('should launch a lt validation OK', function() {
    testValidation("{lt: {value: \"12\"}}", "11", null);
  });

  // LT validation
  it('should launch a lt validation OK because the second parameter is null', function() {
    testValidation("{lt: {otros: \"0\"}}", "123", null);
  });

  // LT validation
  it('should launch a lt validation KO', function() {
    testValidation("{lt: {value: \"11\"}}", "11", { message: 'VALIDATOR_MESSAGE_LESS_THAN' });
  });

  // LE validation
  it('should launch a le validation OK', function() {
    testValidation("{le: {value: \"12/10/2009\", type: \"date\"}}", "11/10/2009", null);
  });

  // LE validation
  it('should launch a le validation OK because the second parameter is null', function() {
    testValidation("{le: {value: \"\", type: \"date\"}}", "11/10/2009", null);
  });

  // LE validation
  it('should launch a le validation KO', function() {
    testValidation("{le: {value: \"12/09/2009\", type: \"date\"}}", "11/10/2009", { message: 'VALIDATOR_MESSAGE_LESS_OR_EQUAL' });
  });

  // GT validation
  it('should launch a gt validation OK', function() {
    testValidation("{gt: {value: \"12\", type: \"integer\"}}", "13", null);
  });

  // GT validation
  it('should launch a gt validation OK because the second parameter is null', function() {
    testValidation("{gt: {otros: \"0\", type: \"integer\"}}", "123", null);
  });

  // GT validation
  it('should launch a gt validation KO', function() {
    testValidation("{gt: {value: \"11\", type: \"float\"}}", "11", { message: 'VALIDATOR_MESSAGE_GREATER_THAN' });
  });

  // GE validation
  it('should launch a ge validation OK', function() {
    testValidation("{ge: {value: \"10/10/2008\", type: \"date\"}}", "11/10/2009", null);
  });

  // GE validation
  it('should launch a ge validation OK because the second parameter is null', function() {
    testValidation("{ge: {value: \"\", type: \"float\"}}", "112.12",null);
  });

  // GE validation
  it('should launch a ge validation KO', function() {
    testValidation("{ge: {value: \"10/11/2011\", type: \"date\"}}", "11/10/2009",  { message: 'VALIDATOR_MESSAGE_GREATER_OR_EQUAL' });
  });

  // MOD validation
  it('should launch a mod validation OK', function() {
    testValidation("{mod: {value: \"2\", type: \"integer\"}}", "6",  null);
  });

  // MOD validation
  it('should launch a mod validation OK because the second parameter is null', function() {
    testValidation("{mod: {value: null, type: \"integer\"}}", "6", null);
  });

  // MOD validation
  it('should launch a mod validation KO', function() {
    testValidation("{mod: {value: \"2\", type: \"integer\"}}", "5", { message: 'VALIDATOR_MESSAGE_DIVISIBLE_BY' });
  });

  // RANGE validation
  it('should launch a range validation OK', function() {
    testValidation("{range: {from: {value: 2}, to: {value: 7}, type: \"integer\"}}", "6", null);
  });

  // RANGE validation
  it('should launch a range validation KO', function() {
    testValidation("{range: {from: {value: \"2\"}, to: {value: 4}, type: \"integer\"}}", "5", { message: 'VALIDATOR_MESSAGE_RANGE' });
  });

  // Date RANGE validation
  it('should launch a date range validation OK', function() {
    testValidation("{range: {from: {value: \"25/08/2015\"}, to: {value: \"01/11/2016\"}, type: \"date\"}}", "23/10/2016", null);
  });

  // Date RANGE validation
  it('should launch a date range validation OK because one value is empty', function() {
    testValidation("{range: {from: {value: \"25/08/2015\"}, to: {other: \"01/11/2016\"}, type: \"date\"}}", "23/10/2016", null);
  });

  // Date RANGE validation
  it('should launch a date range validation KO', function() {
    testValidation("{range: {from: {value: \"25/08/2015\"}, to: {value: \"01/11/2016\"}, type: \"date\"}}", "26/11/2014",  { message: 'VALIDATOR_MESSAGE_RANGE' });
  });

  // Equal length validation
  it('should launch a equal length validation OK', function() {
    testValidation("{equallength: {value: 10}}", "23/10/2016",null);
  });

  // Equal length validation
  it('should launch a equal length validation OK because one value is empty', function() {
    testValidation("{equallength: {other: null}}", "23/10/2016",null);
  });

  // Equal length validation
  it('should launch a equal length validation KO', function() {
    testValidation("{equallength: {value: 8}}", "26/11/2014",{ message: 'VALIDATOR_MESSAGE_EQUAL_LENGTH' });
  });

  // Maxlength validation
  it('should launch a maxlength validation OK', function() {
    testValidation("{maxlength: {value: 10}}", "23/10/2016", null);
  });

  // Maxlength validation
  it('should launch a maxlength validation OK because one value is empty', function() {
    testValidation("{maxlength: {other: null}}", "23/10/2016", null);
  });

  // Maxlength validation
  it('should launch a maxlength validation KO', function() {
    testValidation("{maxlength: {value: 8}}", "26/11/2014", { message: 'VALIDATOR_MESSAGE_MAXLENGTH' });
  });

  // Minlength validation
  it('should launch a minlength validation OK', function() {
    testValidation("{minlength: {value: 8}}", "23/10/2016", null);
  });

  // Minlength validation
  it('should launch a minlength validation OK because one value is empty', function() {
    testValidation("{minlength: {other: null}}", "23/10/2016", null);
  });

  // Minlength validation
  it('should launch a minlength validation KO', function() {
    testValidation("{minlength: {value: 11}}", "26/11/2014", { message: 'VALIDATOR_MESSAGE_MINLENGTH' });
  });

  // Pattern validation
  it('should launch a pattern validation OK', function() {
    testValidation("{pattern: {value: \"^[a-z]+$\"}}", "aadrsasa", null);
  });

  // Pattern validation
  it('should launch a password pattern validation OK', function() {
    testValidation("{pattern:{settings:\"passwordPattern\"}}", "aadrsasa", null);
  });

  // Pattern validation
  it('should launch a pattern validation KO', function() {
    testValidation("{pattern:{value: \"^[a-z]+$\"}}", "aEDaxsa", { message: 'VALIDATOR_MESSAGE_PATTERN' });
  });

  // Check at least validation
  it('should launch a checkAtLeast validation OK', function() {
    testGroupValidation("{checkAtLeast: {value: \"2\"}}", {component: null}, null);
  });

  // Check at least validation
  it('should launch a checkAtLeast validation OK because second parameter is not a number', function() {
    testGroupValidation("{checkAtLeast: {value: null}}", {component: null}, null);
  });

  // Check at least validation
  it('should launch a checkAtLeast validation KO', function() {
    testGroupValidation("{checkAtLeast: {value: \"2\"}}", {component: null, component2: null, component3: "tutu"}, { message: 'VALIDATOR_MESSAGE_CHECK_AT_LEAST' });
  });

  // Invalid validation
  it('should launch a invalid validation not active', function() {
    testValidation("invalid", "test", {message: ""});
  });

  // Invalid validation
  it('should launch a invalid validation active', function() {
    testValidation("{invalid: {message: \"Invalid validation for {value1}\"}}", "test", {message: "Invalid validation for {value1}"});
  });
});
