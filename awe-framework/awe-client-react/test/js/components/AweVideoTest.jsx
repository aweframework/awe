import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweVideo from "../../../src/components/AweVideo";

describe('awe-react-client/test/js/criteria/AweVideoTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      video: {
        address: {component: 'video', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "link",
          url: "https://www.youtube.com/watch?v=LXb3EKWsInQ"
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders video component', () => {
    renderWithProviders(<AweVideo id={"video"}/>, {preloadedState});

    // check
    expect(document.querySelector("div#video")).not.toBeNull();
  });

});
