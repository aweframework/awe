import React from 'react';
import {cleanup} from '@testing-library/react';
import SubViewContainer from "../../../src/containers/SubViewContainer";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/containers/SubViewContainerTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS
  };

  it('renders SubView container', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
        headers: {
          get: () => 'application/json;charset=UTF-8'
        },
        status: 200,
        ok: true,
        json: () => Promise.resolve({})
      }));
    renderWithProviders(<SubViewContainer match={{params: {subScreenId: "subscreen"}}}/>, {preloadedState});

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });

  it('renders a SubView container unauthorized', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'plain/text;charset=UTF-8'
      },
      status: 403,
      ok: false,
      text: () => Promise.resolve("UNAUTHORIZED")
    }));

    renderWithProviders(<SubViewContainer match={{params: {subScreenId: "subscreen"}}}/>, {preloadedState});

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });

  it('renders a SubView container with structure', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      json: () => Promise.resolve({
        structure: [], components: [{
          id: "text",
          controller: {},
          model: {values: []}
        }]
      })
    }));

    renderWithProviders(<SubViewContainer match={{params: {subScreenId: "subscreen"}}}/>, {preloadedState});

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });
});
