import React from 'react';
import {cleanup} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from "react-router";
import SubViewContainer from "../../../src/containers/SubViewContainer";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/containers/SubViewContainerTest.jsx', () => {

  beforeEach(() => {
    global.fetch = jest.fn();
    window.fetch = global.fetch;
  });

  afterAll(cleanup);

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.fetch;
    delete window.fetch;
  });

  const preloadedState = {
    settings: DEFAULT_SETTINGS
  };

  function renderSubViewContainer() {
    return renderWithProviders(
      <MemoryRouter initialEntries={["/subscreen"]}>
        <Routes>
          <Route path="/:subScreenId" element={<SubViewContainer />} />
        </Routes>
      </MemoryRouter>,
      {preloadedState}
    );
  }

  it('renders SubView container', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.spyOn(console, 'info').mockImplementation(() => {});

    jest.spyOn(window, "fetch").mockReturnValue(Promise.resolve({
        headers: {
          get: () => 'application/json;charset=UTF-8'
        },
        status: 200,
        ok: true,
        json: () => Promise.resolve({})
      }));
    renderSubViewContainer();

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });

  it('renders a SubView container unauthorized', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.spyOn(console, 'info').mockImplementation(() => {});

    jest.spyOn(window, "fetch").mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'plain/text;charset=UTF-8'
      },
      status: 403,
      ok: false,
      text: () => Promise.resolve("UNAUTHORIZED")
    }));

    renderSubViewContainer();

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });

  it('renders a SubView container with structure', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.spyOn(console, 'info').mockImplementation(() => {});

    jest.spyOn(window, "fetch").mockReturnValue(Promise.resolve({
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

    renderSubViewContainer();

    // fails
    expect(document.querySelector(".p-progress-spinner")).not.toBeNull();
  });
});
