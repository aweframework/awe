import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweLink from "../../../src/components/AweLink";

describe('awe-react-client/test/js/criteria/AweLinkTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      link: {
        address: {component: 'link', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "link",
          url: "url"
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Link component', () => {
    renderWithProviders(<AweLink id="link"/>, {preloadedState});

    // check
    expect(document.querySelector("a span")).not.toBeNull();
  });

  it('renders Link component with empty state', () => {
    renderWithProviders(<AweLink id="link"/>, {components: {link: {}}, settings: DEFAULT_SETTINGS});

    // check
    expect(document.querySelector("a span")).not.toBeNull();
  });

});
