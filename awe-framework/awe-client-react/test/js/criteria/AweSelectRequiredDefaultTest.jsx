import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelect from "../../../src/criteria/AweSelect";
import {act, cleanup} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweSelectRequiredDefaultTest.jsx', () => {
  afterEach(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      select: {
        address: {component: 'select', view: 'report'},
        model: {values: [{label: 'One', value: '1'}, {label: 'Two', value: '2'}]},
        attributes: {optional: false},
        validationRules: {},
        specificAttributes: {sort: []}
      }
    }
  };

  it('selects the first value of a non optional select with nothing selected, once', async () => {
    const errors = jest.spyOn(console, 'error').mockImplementation(() => null);
    try {
      const {store} = renderWithProviders(<AweSelect id="select"/>, {preloadedState});
      await act(async () => {});

      expect(errors.mock.calls.map(call => String(call[0])).filter(m => /Maximum update depth/.test(m))).toEqual([]);
      expect(store.getState().components.select.model.values.filter(v => v.selected).map(v => v.value)).toEqual(['1']);
    } finally {
      errors.mockRestore();
    }
  });
});
