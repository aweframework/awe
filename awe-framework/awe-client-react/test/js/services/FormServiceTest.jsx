import useFormService from "../../../src/services/FormService";
import {renderWithProviders} from "../test-utils";
import React from "react";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";

describe('awe-react-client/test/js/services/FormServiceTest.jsx', () => {
  let service;
  let actions;

  function TestHarness() {
    service = useFormService();
    actions = service.getActions();
    return null;
  }

  beforeEach(function () {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    renderWithProviders(<TestHarness />);
  });

  afterEach(function () {
    jest.restoreAllMocks();
  });

  it('should get all form actions', () => {
    expect(Object.keys(actions).length).toBe(26);
    expect(Object.keys(actions)).toContain('fill-suggest');
    expect(Object.keys(actions)).toContain('initial-model');
    expect(Object.keys(actions)).toContain('deferred-failed');
  });

  it('should restore the first loaded values on restore-target and the default ones on restore', () => {
    expect(actions['restore-target']).not.toBe(actions['restore']);
  });

  it('sets a static value without mutating an action that belongs to a registered component', () => {
    ComponentRegistry.register('button', {
      address: {view: 'base', component: 'button'},
      attributes: {},
      actions: [{type: 'value', value: 'abc', parameters: {target: 'crit'}}]
    });
    const action = ComponentRegistry.get('button').actions[0];

    expect(() => actions.value(action)).not.toThrow();
    expect(action.parameters.values).toBeUndefined();
    ComponentRegistry.clearAll();
  });

});
