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

  it('exposes the number of every step, whatever the step shows (a number or an icon)', () => {
    renderWithProviders(<AweSteps id="steps"/>, {
      preloadedState: {
        ...preloadedState,
        components: {
          steps: {
            ...preloadedState.components.steps,
            model: {
              values: [
                {label: 'first', value: 'first', icon: 'pi:user', selected: true},
                {label: 'second', value: 'second'}
              ]
            }
          }
        }
      }
    });

    const steps = [...document.querySelectorAll("[data-testid='wizard-step']")];
    expect(steps.map(step => step.getAttribute("data-step-number"))).toEqual(["1", "2"]);
  });

  const withInvalidField = (error) => ({
    ...preloadedState,
    components: {
      steps: {
        ...preloadedState.components.steps,
        model: {values: [{label: 'one', value: 'one', selected: true}, {label: 'two', value: 'two'}]}
      },
      field: {
        address: {component: 'field', view: 'report'},
        context: {view: 'report', source: ['steps', 'two', 'field']},
        model: {values: []},
        attributes: {error: error},
        validationRules: {}
      }
    }
  });

  it('marks the step that contains an invalid field', () => {
    renderWithProviders(<AweSteps id="steps"/>, {preloadedState: withInvalidField({message: 'Required'})});

    const steps = [...document.querySelectorAll("[data-testid='wizard-step']")];
    expect(steps.map(step => step.getAttribute("data-invalid"))).toEqual(["false", "true"]);
    expect(steps[1].classList.contains("p-step-invalid")).toBe(true);
  });

  it('does not mark any step when every field is valid', () => {
    renderWithProviders(<AweSteps id="steps"/>, {preloadedState: withInvalidField(null)});

    const steps = [...document.querySelectorAll("[data-testid='wizard-step']")];
    expect(steps.map(step => step.getAttribute("data-invalid"))).toEqual(["false", "false"]);
  });
});
