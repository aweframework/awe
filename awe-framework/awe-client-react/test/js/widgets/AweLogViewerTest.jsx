import React from 'react';
import {act, cleanup, fireEvent, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweLogViewer from "../../../src/widgets/AweLogViewer";
import {LazyLog} from "react-lazylog";
import {fetchLogAction} from "../../../src/redux/thunks/files";

// Same component, but its props are recorded so the text handed to the log can be checked
jest.mock("react-lazylog", () => {
  const React = require("react");
  const actual = jest.requireActual("react-lazylog");
  return {...actual, LazyLog: jest.fn(props => React.createElement(actual.LazyLog, props))};
});

// Same thunk, but a test can make it hand a given text to the viewer
jest.mock("../../../src/redux/thunks/files", () => {
  const actual = jest.requireActual("../../../src/redux/thunks/files");
  return {...actual, fetchLogAction: jest.fn(actual.fetchLogAction)};
});

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

  it('renders AWE Log Viewer widget', async () => {
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    jest.advanceTimersByTime(150);

    expect(await screen.findByPlaceholderText("Search")).toBeDefined();
  });

  it('renders AWE Log Viewer widget and disables log autorefresh', async () => {
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    jest.advanceTimersByTime(150);

    expect(await screen.findByPlaceholderText("Search")).toBeDefined();

    fireEvent.click(screen.getByTestId("autoload-button"));
  });

  it('marks the container of the log with the log-viewer hook', async () => {
    const { container } = renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    jest.advanceTimersByTime(150);

    expect(await screen.findByPlaceholderText("Search")).toBeDefined();
    expect(container.querySelector("[data-testid='log-viewer']")).not.toBeNull();
  });

  it('hands the log text to the viewer closed with a line break', async () => {
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    // The log is read once per autorefresh period
    await act(async () => {
      jest.advanceTimersByTime(1100);
    });

    await waitFor(() => expect(LazyLog.mock.calls.some(([props]) => props.text.includes("lala"))).toBe(true));
    LazyLog.mock.calls.forEach(([props]) => expect(props.text.endsWith("\n")).toBe(true));
  });

  it.each([[null], [undefined], [""], ["   "]])('renders the viewer without crashing when the log text is %p', async (text) => {
    fetchLogAction.mockImplementationOnce((serverAction, targetAction, offset, setLogText) => () => setLogText(text));
    LazyLog.mockClear();
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    await act(async () => {
      jest.advanceTimersByTime(1100);
    });

    expect(await screen.findByPlaceholderText("Search")).toBeDefined();
    // Nothing to show: a single blank line, always closed with a line break
    const texts = LazyLog.mock.calls.map(([props]) => props.text);
    expect(texts[texts.length - 1]).toBe(" \n");
  });

  it('keeps reading the log after an empty answer', async () => {
    fetchLogAction.mockImplementationOnce((serverAction, targetAction, offset, setLogText) => () => setLogText(null));
    renderWithProviders(<AweLogViewer id="logViewer"/>, {preloadedState});

    await act(async () => {
      jest.advanceTimersByTime(2200);
    });

    await waitFor(() => expect(LazyLog.mock.calls.some(([props]) => props.text.includes("lala"))).toBe(true));
  });
});
