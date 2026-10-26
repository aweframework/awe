import React from 'react';
import {act, cleanup, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweFileViewer from "../../../src/widgets/AweFileViewer";
import Widgets from "../../../src/widgets";
import {updateAttributes} from "../../../src/redux/actions/components";

const fileResponse = (status, text) => Promise.resolve({
  status,
  ok: status < 400,
  text: () => Promise.resolve(text)
});

const buildState = (attributes = {}) => ({
  settings: DEFAULT_SETTINGS,
  components: {
    fileViewer: {
      address: {component: 'fileViewer', view: 'report'},
      model: {values: []},
      attributes: {serverAction: "file", targetAction: "text", autoload: true, style: "expand scrollable", ...attributes},
      specificAttributes: {}
    }
  }
});

describe('awe-react-client/test/js/widgets/AweFileViewerTest.jsx', () => {

  afterAll(cleanup);
  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  beforeEach(() => {
    global.fetch = jest.fn(() => fileResponse(201, "first line\nsecond line"));
  });

  it('is registered as the file-viewer widget', () => {
    expect(Widgets({type: "file-viewer", id: "fileViewer"}, 0).type).toBe(AweFileViewer);
  });

  it('posts the form values to the generic file endpoint and shows the content in a pre', async () => {
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState()});

    await waitFor(() => expect(container.querySelector("pre").textContent).toBe("first line\nsecond line"));
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe("/file/text");
    expect(options.method).toBe("POST");
  });

  it('marks the pre of the file with the file-viewer hook', async () => {
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState()});

    await waitFor(() => expect(container.querySelector("[data-testid='file-viewer']").textContent).toBe("first line\nsecond line"));
  });

  it('applies the style of the controller to the container', async () => {
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState()});

    await waitFor(() => expect(container.querySelector("pre")).not.toBeNull());
    expect(container.querySelector("#fileViewer").className).toContain("expand scrollable");
  });

  it.each([[200, "not a file"], [201, ""], [201, null]])('keeps the content shown when a later answer is status %p with %p', async (status, text) => {
    jest.useFakeTimers();
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({autorefresh: 1})});
    await act(async () => {});
    expect(container.querySelector("pre").textContent).toBe("first line\nsecond line");

    global.fetch.mockImplementation(() => fileResponse(status, text));
    await act(async () => {
      jest.advanceTimersByTime(1100);
    });
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(container.querySelector("pre").textContent).toBe("first line\nsecond line");
  });

  it('renders nothing when it is not visible', () => {
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({visible: false})});

    expect(container.querySelector("pre")).toBeNull();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('does not load the file at the start without autoload', async () => {
    renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({autoload: false})});

    await act(async () => {});
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('shows the content under React StrictMode, which mounts the widget twice in development', async () => {
    const {container} = renderWithProviders(<React.StrictMode><AweFileViewer id="fileViewer"/></React.StrictMode>,
      {preloadedState: buildState()});

    await waitFor(() => expect(container.querySelector("pre").textContent).toBe("first line\nsecond line"));
  });

  it('reads the file again every autorefresh period', async () => {
    jest.useFakeTimers();
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({autorefresh: 2})});

    await act(async () => {});
    expect(global.fetch).toHaveBeenCalledTimes(1);
    global.fetch.mockImplementation(() => fileResponse(201, "updated"));
    await act(async () => {
      jest.advanceTimersByTime(2100);
    });

    await waitFor(() => expect(container.querySelector("pre").textContent).toBe("updated"));
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });

  it('does not start a new read while the previous one is still pending, like the AngularJS refresh after each answer', async () => {
    jest.useFakeTimers();
    let answer;
    global.fetch.mockImplementation(() => new Promise(resolve => { answer = resolve; }));
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({autorefresh: 1})});

    await act(async () => {
      jest.advanceTimersByTime(3100);
    });
    expect(global.fetch).toHaveBeenCalledTimes(1);

    await act(async () => answer({status: 201, ok: true, text: () => Promise.resolve("slow answer")}));
    expect(container.querySelector("pre").textContent).toBe("slow answer");
    global.fetch.mockImplementation(() => fileResponse(201, "next"));
    await act(async () => {
      jest.advanceTimersByTime(1100);
    });
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });

  it('starts a new read when the pending one hangs for more than 30 seconds, and ignores its late answer', async () => {
    jest.useFakeTimers();
    let hung;
    global.fetch.mockImplementationOnce(() => new Promise(resolve => { hung = resolve; }));
    const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: buildState({autorefresh: 1})});
    await act(async () => {
      jest.advanceTimersByTime(29000);
    });
    expect(global.fetch).toHaveBeenCalledTimes(1);

    global.fetch.mockImplementation(() => fileResponse(201, "fresh"));
    await act(async () => {
      jest.advanceTimersByTime(2000);
    });
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(container.querySelector("pre").textContent).toBe("fresh");

    await act(async () => hung({status: 201, ok: true, text: () => Promise.resolve("stale")}));
    expect(container.querySelector("pre").textContent).toBe("fresh");
  });

  describe('stickBottom', () => {
    const scrollState = (parameters, autorefresh = 0) => ({...buildState(), components: {fileViewer: {
      ...buildState().components.fileViewer,
      attributes: {...buildState().components.fileViewer.attributes, parameters, autorefresh}
    }}});

    const mockScroll = (element, {scrollHeight, clientHeight, scrollTop}) => {
      Object.defineProperty(element, "scrollHeight", {configurable: true, value: scrollHeight});
      Object.defineProperty(element, "clientHeight", {configurable: true, value: clientHeight});
      element.scrollTop = scrollTop;
    };

    it('scrolls to the bottom on the first load', async () => {
      const spy = jest.spyOn(HTMLElement.prototype, "scrollHeight", "get").mockReturnValue(500);
      const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: scrollState({stickBottom: true})});

      await waitFor(() => expect(container.querySelector("pre").textContent).not.toBe(""));
      expect(container.querySelector("#fileViewer").scrollTop).toBe(500);
      spy.mockRestore();
    });

    it('does not scroll without the stickBottom parameter', async () => {
      const spy = jest.spyOn(HTMLElement.prototype, "scrollHeight", "get").mockReturnValue(500);
      const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>, {preloadedState: scrollState({})});

      await waitFor(() => expect(container.querySelector("pre").textContent).not.toBe(""));
      expect(container.querySelector("#fileViewer").scrollTop).toBe(0);
      spy.mockRestore();
    });

    it('goes back to the bottom when the widget is shown again, even if the text did not change', async () => {
      const spy = jest.spyOn(HTMLElement.prototype, "scrollHeight", "get").mockReturnValue(500);
      const {container, store} = renderWithProviders(<AweFileViewer id="fileViewer"/>,
        {preloadedState: scrollState({stickBottom: true})});
      await waitFor(() => expect(container.querySelector("#fileViewer").scrollTop).toBe(500));

      const address = {component: 'fileViewer', view: 'report'};
      act(() => store.dispatch(updateAttributes(address, {visible: false})));
      expect(container.querySelector("#fileViewer")).toBeNull();
      act(() => store.dispatch(updateAttributes(address, {visible: true})));

      await waitFor(() => expect(container.querySelector("#fileViewer").scrollTop).toBe(500));
      spy.mockRestore();
    });

    it('keeps the position of a user who scrolled up and follows one that is at the bottom', async () => {
      jest.useFakeTimers();
      const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>,
        {preloadedState: scrollState({stickBottom: true}, 1)});
      await act(async () => {});
      const element = container.querySelector("#fileViewer");
      let call = 0;
      global.fetch.mockImplementation(() => fileResponse(201, `more content ${call++}`));

      // The user scrolled up: a new content does not move the scroll
      mockScroll(element, {scrollHeight: 1000, clientHeight: 100, scrollTop: 200});
      await act(async () => {
        jest.advanceTimersByTime(1100);
      });
      expect(element.scrollTop).toBe(200);

      // The user is at the bottom: the new content is followed
      element.scrollTop = 900;
      await act(async () => {
        jest.advanceTimersByTime(1100);
      });
      expect(element.scrollTop).toBe(1000);
    });
    it('does not take an unchanged answer at the bottom as a reason to follow later content', async () => {
      jest.useFakeTimers();
      const {container} = renderWithProviders(<AweFileViewer id="fileViewer"/>,
        {preloadedState: scrollState({stickBottom: true}, 1)});
      await act(async () => {});
      const element = container.querySelector("#fileViewer");
      global.fetch.mockImplementation(() => fileResponse(201, "first line\nsecond line"));

      // Same text while the user is at the bottom: nothing changes
      mockScroll(element, {scrollHeight: 1000, clientHeight: 100, scrollTop: 900});
      await act(async () => {
        jest.advanceTimersByTime(1100);
      });

      // The user scrolls up and new text arrives: the scroll stays
      element.scrollTop = 200;
      global.fetch.mockImplementation(() => fileResponse(201, "a new line"));
      await act(async () => {
        jest.advanceTimersByTime(1100);
      });
      expect(element.scrollTop).toBe(200);
    });
  });
});
