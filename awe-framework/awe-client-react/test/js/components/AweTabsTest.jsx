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

  const withInvalidField = (error) => ({
    ...preloadedState,
    components: {
      tabs: {
        ...preloadedState.components.tabs,
        model: {values: [{label: 'one', value: 'one', selected: true}, {label: 'two', value: 'two'}]}
      },
      field: {
        address: {component: 'field', view: 'report'},
        context: {view: 'report', source: ['tabs', 'two', 'field']},
        model: {values: []},
        attributes: {error: error},
        validationRules: {}
      }
    }
  });

  it('marks the tab that contains an invalid field', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState: withInvalidField({message: 'Required'})});

    const tabs = [...document.querySelectorAll("[data-testid='tab']")];
    expect(tabs.map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "true"]);
    expect(tabs[1].classList.contains("p-tab-invalid")).toBe(true);
  });

  it('does not mark any tab when every field is valid', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState: withInvalidField(null)});

    const tabs = [...document.querySelectorAll("[data-testid='tab']")];
    expect(tabs.map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "false"]);
  });
});
