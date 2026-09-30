import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweSteps from "../../../src/components/AweSteps";

describe('awe-react-client/test/js/components/AweStepsTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      steps: {
        address: {component: 'steps', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        elementList: [],
        attributes: {
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Steps component', () => {
    renderWithProviders(<AweSteps id="steps"/>, {preloadedState});

    // fails
    expect(document.querySelector("nav.p-steps")).not.toBeNull();
  });
});
