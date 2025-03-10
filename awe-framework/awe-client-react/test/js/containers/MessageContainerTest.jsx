import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import MessageContainer from "../../../src/containers/MessageContainer";

describe('awe-react-client/test/js/containers/MessageContainerTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS
  };

  it('renders message container', () => {
    renderWithProviders(<MessageContainer/>, {preloadedState:{...preloadedState, messages: {showing: []}}});

    // check
    expect(document.querySelector(".p-toast")).not.toBeNull();
  });

  it('renders a message container with messages', () => {
    renderWithProviders(<MessageContainer/>, {preloadedState:{...preloadedState, messages: {showing: [
      {
        severity:"warn", summary:"Invalid credentials", detail:"The credentials entered for the user -test- are not valid",
        sticky:false, life:4000, id:1,  closable:true, show:true
      }
      ]}}});

    // check
    expect(document.querySelector(".p-toast")).not.toBeNull();
  });
});
