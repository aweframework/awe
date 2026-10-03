import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweWindow from "../../../src/components/AweWindow";

describe('awe-react-client/test/js/criteria/AweWindowTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      window: {
        address: {component: 'window', view: 'report'},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "window",
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders window component', () => {
    renderWithProviders(<AweWindow id="window"/>, {preloadedState});

    // check
    expect(document.querySelector("div.p-panel")).not.toBeNull();
  });

  it('exposes its identifier as the window-id attribute, the one that dependency actions target', () => {
    renderWithProviders(<AweWindow id="ExecutionsWindow" style="expand hidden"/>, {preloadedState});

    expect(document.querySelector("[window-id='ExecutionsWindow']")).toBe(document.querySelector("div.p-panel"));
  });

});
