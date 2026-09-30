import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweTabs from "../../../src/components/AweTabs";

describe('awe-react-client/test/js/components/AweTabsTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      tabs: {
        address: {component: 'tabs', view: 'report'},
        model: {values: [{label: 'test', value: 'test', title: 'test',  icon: 'test',  unit: 'test', selected: true}]},
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

  it('renders Awe Tabs component', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState});

    // fails
    expect(document.querySelector("div.p-tabmenu")).not.toBeNull();
  });
});
