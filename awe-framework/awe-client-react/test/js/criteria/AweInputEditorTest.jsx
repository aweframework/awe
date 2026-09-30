import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputEditor from "../../../src/criteria/AweInputEditor";

describe('awe-react-client/test/js/criteria/AweInputEditorTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      editor: {
        address: {component: 'editor', view: 'report'},
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

  it('renders Awe Input Editor component', async () => {
    await act(async () => {
      renderWithProviders(<AweInputEditor id="editor"/>, {preloadedState});
    });

    // check
    expect(document.querySelector("#editor")).not.toBeNull();
  }, 15000);

});
