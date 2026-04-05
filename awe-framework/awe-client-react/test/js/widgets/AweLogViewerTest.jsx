import React from 'react';
import {cleanup, fireEvent, screen} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweLogViewer from "../../../src/widgets/AweLogViewer";

describe('awe-react-client/test/js/widgets/AweLogViewerTest.jsx', () => {

  afterAll(cleanup);
  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      logViewer: {
        address: {component: 'logViewer', view: 'report'},
        model: {values: []},
        attributes: {
          autorefresh: 1
        },
        specificAttributes: {sort: []}
      }
    }
  };

  beforeEach(() => {
    jest.useFakeTimers();

    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      json: () => Promise.resolve([{type: "log-delta", parameters: {log:["tutu", "lala"]}}])
    }));
  });

  it('renders AWE Log Viewer widget', () => {
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    jest.advanceTimersByTime(150);

    expect(screen.findByPlaceholderText("Search")).toBeDefined();
  });

  it('renders AWE Log Viewer widget and disables log autorefresh', () => {
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    jest.advanceTimersByTime(150);

    expect(screen.findByPlaceholderText("Search")).toBeDefined();

    fireEvent.click(screen.getByTestId("autoload-button"));
  });
});
