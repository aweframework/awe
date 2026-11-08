import React from 'react';
import { fireEvent, screen, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import {renderWithProviders} from "../test-utils";
import SessionExpiredContainer from "../../../src/containers/SessionExpiredContainer";
import useClientActions from "../../../src/services/ClientActionsService";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

describe('awe-react-client/test/js/containers/SessionExpiredContainerTest.jsx', () => {

  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('shows the translated title and message and one button to restart the application (#935, S5)', () => {
    renderWithProviders(<SessionExpiredContainer/>, {
      preloadedState: {settings: {...DEFAULT_SETTINGS, sessionExpired: true}}
    });

    expect(screen.getByText('ERROR_TITLE_SESSION_EXPIRED')).toBeInTheDocument();
    expect(screen.getByText('ERROR_MESSAGE_SESSION_EXPIRED')).toBeInTheDocument();
    // The restart button is the only labelled button of the panel: no cancel, no accept
    const restartButtons = screen.getAllByRole('button')
      .filter(button => button.textContent?.includes('BUTTON_RESTART_APPLICATION'));
    expect(restartButtons).toHaveLength(1);
    expect(screen.queryByText('BUTTON_CANCEL')).toBeNull();
    expect(screen.queryByText('BUTTON_ACCEPT')).toBeNull();
  });

  it('navigates to the application root inside the context path of the deployment through its navigation seam (#935, S5)', () => {
    // The client addresses every request of its own with getContextPath(): a WAR at /awe restarts at /awe/
    const base = document.createElement('base');
    base.href = 'http://localhost/awe/';
    document.head.appendChild(base);
    const navigate = jest.fn();
    const {dispatchSpy} = renderWithProviders(<SessionExpiredContainer navigate={navigate}/>, {
      preloadedState: {settings: {...DEFAULT_SETTINGS, sessionExpired: true}},
      spyDispatch: true
    });

    // The restart button is the only control of the panel: the click navigates and nothing else does
    const restart = screen.getAllByRole('button').find((button) => button.textContent?.includes('BUTTON_RESTART_APPLICATION'));
    fireEvent.click(restart);

    base.remove();
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith("/awe/");
    // The panel unblocks its own settings before navigating
    expect(dispatchSpy.mock.calls.some(([call]) => (
      call.type === 'UPDATE_SETTINGS' && call.payload.sessionExpired === false
    ))).toBe(true);
  });

  it('is rendered as a blocking dialog, without close or cancel (#935, S5)', () => {
    const {baseElement} = renderWithProviders(<SessionExpiredContainer/>, {
      preloadedState: {settings: {...DEFAULT_SETTINGS, sessionExpired: true}}
    });

    expect(baseElement.querySelector('.awe-session-expired-dialog')).not.toBeNull();
    // No close icon in the header of the dialog
    expect(baseElement.querySelectorAll('.p-dialog-header-icon')).toHaveLength(0);
  });

  it('registers the session-expired client action in the actions registry (#935, S5)', () => {
    let registry;
    const TestHarness = () => {
      registry = useClientActions();
      return null;
    };
    renderWithProviders(<MemoryRouter><TestHarness/></MemoryRouter>, {
      preloadedState: {settings: DEFAULT_SETTINGS}
    });
    expect(typeof registry['session-expired']).toBe('function');
  });

  it('runs the session-expired action by marking the session expired and accepting the action (#935, S5, S6)', () => {
    let registry;
    const TestHarness = () => {
      registry = useClientActions();
      return null;
    };
    const {store, dispatchSpy} = renderWithProviders(<MemoryRouter><TestHarness/></MemoryRouter>, {
      preloadedState: {settings: DEFAULT_SETTINGS},
      spyDispatch: true
    });

    const action = {type: 'session-expired', id: 1, address: {view: 'base', component: 'ButLgn'}};
    act(() => {
      dispatchSpy.mockClear();
      registry['session-expired'](action);
    });

    expect(store.getState().settings.sessionExpired).toBe(true);
    expect(dispatchSpy.mock.calls.some(([call]) => call.type === 'UPDATE_SETTINGS' && call.payload.sessionExpired === true)).toBe(true);
    expect(dispatchSpy.mock.calls.some(([call]) => call.type === 'ACCEPT_ACTION')).toBe(true);
  });

  it('shows nothing while the session is not over (#935, S5)', () => {
    renderWithProviders(<SessionExpiredContainer/>, {
      preloadedState: {settings: DEFAULT_SETTINGS}
    });
    expect(screen.queryByText('ERROR_TITLE_SESSION_EXPIRED')).toBeNull();
  });
});
