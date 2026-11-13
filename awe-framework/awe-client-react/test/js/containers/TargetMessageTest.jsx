import React from 'react';
import {act, fireEvent, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import MessageContainer from "../../../src/containers/MessageContainer";
import {showPopover} from "../../../src/redux/actions/messages";
import {ACCEPT_ACTION, ActionStatus} from "../../../src/redux/actions/actions";
import {targetMessageAction} from "../../../src/redux/thunks/messages";

const POPOVER = '[data-testid="popover"]';
const translate = (text) => text;

describe('awe-react-client/test/js/containers/TargetMessageTest.jsx', () => {
  const runningAction = (id) => ({id, type: 'target-message', status: ActionStatus.STATUS_RUNNING});
  const preloadedState = (actions = []) => ({
    settings: DEFAULT_SETTINGS,
    messages: {defined: {base: {}}, showing: []},
    actions: {running: actions.length > 0, sync: [actions], async: []}
  });
  const popover = (id, action, extra = {}) => ({
    id, severity: 'warn', summary: 'Popover title', detail: 'Popover text', life: 0,
    address: {view: 'base', component: 'Crt'}, action, ...extra
  });
  const pendingActions = (store) => store.getState().actions.sync.flat().map((action) => action.id);
  // Accepts dispatched by the component (the thunk dispatches through the middleware, not through the store)
  const acceptedByComponent = (dispatchSpy, id) => dispatchSpy.mock.calls
    .filter(([dispatched]) => dispatched?.type === ACCEPT_ACTION && dispatched.payload?.id === id).length;
  // The panel transitions run on timers (120 ms to enter, 100 ms to exit), and it listens to outside clicks on a
  // timer started once its enter transition ends: the tests drive them with fake timers instead of the wall clock.
  // Advancing a bounded time keeps the timeout of the message (seconds) pending.
  const PANEL_TRANSITIONS = 200;
  const settle = () => act(() => {
    jest.advanceTimersByTime(PANEL_TRANSITIONS);
  });
  const shown = () => {
    expect(document.querySelector(POPOVER)).not.toBeNull();
    // Enter transition; the listener timer starts when React has rendered its end
    settle();
    settle();
  };
  const clickOutside = () => {
    fireEvent.mouseDown(document.body);
    fireEvent.click(document.body);
  };
  let criterion;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    criterion = document.createElement('div');
    criterion.setAttribute('criterion-id', 'Crt');
    document.body.appendChild(criterion);
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
    // The popover is a portal of the rendered tree: only the criterion added by the test is removed here
    criterion.remove();
  });

  it('shows a dispatched target-message as a popover on its criterion', async () => {
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState()});

    act(() => {
      store.dispatch(targetMessageAction({
        id: 'a1', type: 'target-message', address: {view: 'base', component: 'Crt'},
        parameters: {type: 'warning', title: 'Wrong value', message: 'The value is not valid'}
      }, translate));
    });

    const element = await waitFor(() => {
      const shown = document.querySelector(POPOVER);
      expect(shown).not.toBeNull();
      return shown;
    });
    expect(element.getAttribute('data-testid-owner')).toBe('Crt');
    expect(element.getAttribute('data-type')).toBe('warning');
    expect(element.querySelector('[data-testid="popover-title"]').textContent).toBe('Wrong value');
    expect(element.querySelector('[data-testid="popover-content"]').textContent).toBe('The value is not valid');
    expect(element.classList).toContain('target-message');
    expect(element.classList).toContain('target-message-warning');
    // No notification: the message is the popover
    expect(store.getState().messages.showing).toEqual([]);
  });

  it('hides the popover after the timeout and finishes its action', async () => {
    const action = runningAction('a1');
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState([action])});

    act(() => {
      store.dispatch(showPopover(popover(1, action, {life: 3000})));
    });
    act(() => {
      jest.advanceTimersByTime(100);
    });
    expect(document.querySelector(POPOVER)).not.toBeNull();
    expect(pendingActions(store)).toEqual(['a1']);

    act(() => {
      jest.advanceTimersByTime(3000);
    });
    act(() => {
      jest.runOnlyPendingTimers();
    });

    await waitFor(() => expect(document.querySelector(POPOVER)).toBeNull());
    expect(store.getState().messages.popover).toBeUndefined();
    expect(pendingActions(store)).toEqual([]);
  });

  it('keeps a popover without timeout until it is dismissed', async () => {
    const action = runningAction('a1');
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState([action])});

    act(() => {
      store.dispatch(showPopover(popover(1, action)));
    });
    act(() => {
      jest.advanceTimersByTime(60000);
    });

    expect(document.querySelector(POPOVER)).not.toBeNull();
    expect(pendingActions(store)).toEqual(['a1']);
  });

  it('replaces the text of the popover shown with the new one', async () => {
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState()});

    act(() => {
      store.dispatch(showPopover(popover(1, {id: 'a1'})));
    });
    await waitFor(() => expect(document.querySelector(POPOVER)).not.toBeNull());

    act(() => {
      store.dispatch(showPopover(popover(2, {id: 'a2'}, {severity: 'error', summary: 'Second', detail: 'Second text'})));
    });

    await waitFor(() => expect(document.querySelector('[data-testid="popover-title"]').textContent).toBe('Second'));
    expect(document.querySelectorAll(POPOVER)).toHaveLength(1);
    expect(document.querySelector(POPOVER).getAttribute('data-type')).toBe('danger');
    expect(store.getState().messages.popover.id).toBe(2);
  });

  it('finishes the action when the popover target is gone', async () => {
    const action = runningAction('a1');
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState([action])});
    criterion.remove();

    act(() => {
      store.dispatch(showPopover(popover(1, action)));
    });

    await waitFor(() => expect(store.getState().messages.popover).toBeUndefined());
    expect(pendingActions(store)).toEqual([]);
    expect(document.querySelector(POPOVER)).toBeNull();
  });

  it('finishes the action when the popover is dismissed with an outside click', async () => {
    const action = runningAction('a1');
    const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState([action])});

    act(() => {
      store.dispatch(showPopover(popover(1, action)));
    });
    shown();
    expect(pendingActions(store)).toEqual(['a1']);

    act(() => {
      clickOutside();
    });

    await waitFor(() => expect(document.querySelector(POPOVER)).toBeNull());
    expect(store.getState().messages.popover).toBeUndefined();
    expect(pendingActions(store)).toEqual([]);
  });

  it('does not finish again a popover replaced while it hides, and keeps the new one', async () => {
    const first = runningAction('a1');
    const second = runningAction('a2');
    const {store, dispatchSpy} = renderWithProviders(<MessageContainer/>, {
      preloadedState: preloadedState([first, second]), spyDispatch: true
    });
    const targetMessage = (action, title) => targetMessageAction({
      ...action, address: {view: 'base', component: 'Crt'}, parameters: {type: 'warning', title, message: title}
    }, translate);

    act(() => {
      store.dispatch(targetMessage(first, 'First'));
    });
    shown();

    // The first popover starts hiding and the second message arrives before it has gone
    act(() => {
      clickOutside();
    });
    act(() => {
      store.dispatch(targetMessage(second, 'Second'));
    });
    settle();

    // The thunk finished the replaced action: the component does not finish it again, nor the new one
    expect(acceptedByComponent(dispatchSpy, 'a1')).toBe(0);
    expect(acceptedByComponent(dispatchSpy, 'a2')).toBe(0);
    expect(pendingActions(store)).toEqual(['a2']);
    expect(store.getState().messages.popover.action.id).toBe('a2');
    expect(document.querySelector('[data-testid="popover-title"]').textContent).toBe('Second');
  });

  it('does not finish the replaced popover again when the panel hides', async () => {
    const first = runningAction('a1');
    const second = runningAction('a2');
    const {store, dispatchSpy} = renderWithProviders(<MessageContainer/>, {
      preloadedState: preloadedState([first, second]), spyDispatch: true
    });

    act(() => {
      store.dispatch(showPopover(popover(1, first)));
    });
    shown();

    // The new message replaces the shown one (whoever replaces it finishes it), but its target is gone
    act(() => {
      store.dispatch(showPopover(popover(2, second, {address: {view: 'base', component: 'Missing'}})));
    });
    settle();
    act(() => {
      clickOutside();
    });
    settle();

    expect(document.querySelector(POPOVER)).toBeNull();
    expect(store.getState().messages.popover).toBeUndefined();
    expect(acceptedByComponent(dispatchSpy, 'a1')).toBe(0);
    expect(acceptedByComponent(dispatchSpy, 'a2')).toBe(1);
  });

  describe('when the target leaves the page while the popover is shown', () => {
    const showOnCriterion = (store, action, extra) => {
      act(() => {
        store.dispatch(showPopover(popover(1, action, extra)));
      });
      shown();
      expect(pendingActions(store)).toContain(action.id);
    };

    it('hides a popover without timeout and finishes its action once', async () => {
      const action = runningAction('a1');
      const {store, dispatchSpy} = renderWithProviders(<MessageContainer/>, {
        preloadedState: preloadedState([action]), spyDispatch: true
      });
      showOnCriterion(store, action, {severity: 'error'});

      // The screen changes: the criterion is unmounted
      await act(async () => {
        criterion.remove();
      });
      settle();

      await waitFor(() => expect(document.querySelector(POPOVER)).toBeNull());
      expect(store.getState().messages.popover).toBeUndefined();
      expect(pendingActions(store)).toEqual([]);
      expect(acceptedByComponent(dispatchSpy, 'a1')).toBe(1);
    });

    it('keeps the popover while its target stays on the page', async () => {
      const action = runningAction('a1');
      const {store} = renderWithProviders(<MessageContainer/>, {preloadedState: preloadedState([action])});
      showOnCriterion(store, action);

      // Other parts of the page change
      await act(async () => {
        document.body.appendChild(document.createElement('div'));
        criterion.appendChild(document.createElement('span'));
      });
      settle();

      expect(document.querySelector(POPOVER)).not.toBeNull();
      expect(pendingActions(store)).toEqual(['a1']);
    });

    it('does not finish a replaced popover when the target of the replaced one leaves', async () => {
      const first = runningAction('a1');
      const second = runningAction('a2');
      const other = document.createElement('div');
      other.setAttribute('criterion-id', 'Other');
      document.body.appendChild(other);
      const {store, dispatchSpy} = renderWithProviders(<MessageContainer/>, {
        preloadedState: preloadedState([first, second]), spyDispatch: true
      });
      showOnCriterion(store, first);

      act(() => {
        store.dispatch(showPopover(popover(2, second, {address: {view: 'base', component: 'Other'}})));
      });
      settle();
      await act(async () => {
        criterion.remove();
      });
      settle();

      expect(document.querySelector(POPOVER)).not.toBeNull();
      expect(store.getState().messages.popover.id).toBe(2);
      expect(acceptedByComponent(dispatchSpy, 'a1')).toBe(0);
      expect(acceptedByComponent(dispatchSpy, 'a2')).toBe(0);
      other.remove();
    });
  });
});
