import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputTextView from "../../../src/criteria/AweInputTextView";
import {fireEvent, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputTextViewTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      textview: {
        address: {component: 'textview', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          helpImage: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input Text View component', () => {
    renderWithProviders(<AweInputTextView id="textview"/>, {preloadedState});

    // check
    expect(document.querySelector("[criterion-id='textview']")).not.toBeNull();
  });

  it('renders on Awe Input Text View component and clicks on it', () => {
    renderWithProviders(<AweInputTextView id="textview"/>, {preloadedState: {
        settings: DEFAULT_SETTINGS,
        components: {
          textview: {
            address: {component: 'textview', view: 'report'},
            model: {values: [{label: 'test', value: 'test', selected: true}]},
            attributes: {
              placeholder: "placeholder",
              readonly: false,
              helpImage: "test",
              icon: "mdi:check",
              unit: "€"
            }
          }
        }
      }});

    // check
    expect(document.querySelector("[criterion-id='textview']")).not.toBeNull();

    // Click on text view
    fireEvent.click(screen.getByRole("button"));

    // Keydown on text view
    fireEvent.keyDown(screen.getByRole("button"));
  });

});
