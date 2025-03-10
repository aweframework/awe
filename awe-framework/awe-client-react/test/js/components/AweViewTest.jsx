import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {AweView} from "../../../src/components/AweView";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/components/AweViewTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS
  };

  it('renders Awe View component', () => {
    renderWithProviders(<AweView/>, {preloadedState});

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });
});
