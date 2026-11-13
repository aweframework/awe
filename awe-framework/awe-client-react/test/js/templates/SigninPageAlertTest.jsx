import React from 'react';
import { MemoryRouter } from 'react-router';
import { act, cleanup, fireEvent, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import AweButton from '../../../src/components/AweButton';
import AweInputPassword from '../../../src/criteria/AweInputPassword';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { ADD_ACTIONS } from '../../../src/redux/actions/actions';
import { UPDATE_VIEW_COMPONENTS } from '../../../src/redux/actions/components';
import ActionsContainer from '../../../src/containers/ActionsContainer';
import { ButtonTypes } from '../../../src/redux/actions/components';
import { messageAction } from '../../../src/redux/thunks/messages';

// A failed login on the sign-in page (S17): the message of the server in a danger alert at the top of the form, as the
// prototype (.message--danger.login__alert), instead of the toast; the password cleared and focused; the alert hidden
// when the user edits a field or signs in again. Other pages keep their toasts
const VIEW = 'base';
const tag = (id, style, elementList = []) => ({ elementType: 'Tag', type: 'div', id, style, elementList });
const criterion = (id, component) => ({ elementType: 'Criteria', id, component });
const button = id => ({ elementType: 'Button', id });
const signinScreen = () => tag('page-signin', 'expand grid m-0', [
  tag(undefined, 'awe-signin signin-container', [
    tag(undefined, 'signin-info'),
    tag(undefined, 'signin-form', [
      criterion('cod_usr', 'text'),
      criterion('pwd_usr', 'password'),
      tag(undefined, 'p-2 mt-5', [button('ButLogIn')])
    ])
  ])
]);

const field = (id, label, value) => ({
  address: { component: id, view: VIEW }, model: { values: [{ value, selected: true }] }, validationRules: {},
  attributes: { label, readonly: false, visible: true }
});
const server = name => ({ type: 'server', parameters: { serverAction: name, targetAction: null } });
const components = () => ({
  cod_usr: field('cod_usr', 'SCREEN_TEXT_USER', 'jsmith'),
  pwd_usr: field('pwd_usr', 'SCREEN_TEXT_PASS', 'wrong'),
  ButLogIn: {
    address: { component: 'ButLogIn', view: VIEW },
    attributes: { visible: true, label: 'BUTTON_LOGIN', buttonType: ButtonTypes.BUTTON_SUBMIT },
    actions: [{ type: 'validate', parameters: {} }, server('login')]
  }
});
const SETTINGS = { ...DEFAULT_SETTINGS, language: 'en-GB', login: DEFAULT_LOGIN_SETTINGS };
const preloadedState = (actions = { running: false, sync: [[]], async: [] }) =>
  ({ settings: SETTINGS, actions, components: components() });

const renderSignin = state => renderWithProviders(<Tag {...signinScreen()} />,
  { preloadedState: state ?? preloadedState(), spyDispatch: true });

// The answer of the server to a failed login: a message of the login button (the server thunk gives every action of
// the answer the address of the button that launched the call)
const TITLE = 'Invalid credentials';
const TEXT = 'The user or the password are not valid. Check them and try again';
const failure = (address = { component: 'ButLogIn', view: VIEW }) => ({
  id: 'failure', type: 'message', address, parameters: { type: 'warning', title: TITLE, message: TEXT }
});
const fail = (store, action = failure()) => act(() => {
  store.dispatch(messageAction(action, key => i18n.t(key)));
});

const form = () => document.querySelector('.signin-form');
const alertBox = () => document.querySelector('.signin-alert');
const user = () => document.querySelector('input#cod_usr');
const password = () => document.querySelector('input#pwd_usr');
const toasts = store => store.getState().messages.showing;
const valueOf = (store, id) => store.getState().components[id].model.values.filter(v => v.selected).map(v => v.value).join('');

describe('awe-react-client/test/js/templates/SigninPageAlertTest.jsx', () => {
  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      BUTTON_LOGIN: 'Login',
      SCREEN_TEXT_USER: 'User',
      SCREEN_TEXT_PASS: 'Password',
      SCREEN_TEXT_LOGIN_TITLE: 'Welcome to AWE',
      SCREEN_TEXT_LOGIN_SUBTITLE: 'Enter your credentials to continue.',
      // A word of the server text that is also a locale key stays as the server wrote it
      Check: 'Verify'
    });
  });

  afterEach(cleanup);

  describe('Login error inline (S17)', () => {
    it('shows the message of the server in a danger alert at the top of the form, instead of the toast', () => {
      const { store } = renderSignin();
      expect(alertBox()).toBeNull();

      fail(store);

      const alert = screen.getByRole('alert');
      expect(alert).toBe(alertBox());
      expect(alert.classList.contains('signin-alert--danger')).toBe(true);
      // The title in bold, then the text, exactly as the server sent them
      expect(alert.querySelector('strong').textContent).toBe(TITLE);
      expect(alert.textContent).toBe(`${TITLE} ${TEXT}`);
      expect(alert.querySelector('.signin-alert-text').textContent).toBe(TEXT);
      // The Lucide circle-x icon, decorative
      const icon = alert.querySelector('svg');
      expect(icon.querySelector('use').getAttribute('href').split('#')[1]).toBe('circle-x');
      expect(alert.firstElementChild.getAttribute('aria-hidden')).toBe('true');
      // Under the title and the subtitle, above the fields
      expect(alert.parentElement).toBe(form());
      expect(alert.previousElementSibling.classList.contains('signin-subtitle')).toBe(true);
      expect(alert.compareDocumentPosition(user()) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
      // No toast for it
      expect(toasts(store)).toEqual([]);
    });

    it('carries the contract of an alert of the toast: the type of the message, its title and its text', () => {
      const { store } = renderSignin();
      fail(store);

      const alert = alertBox();
      expect(alert.getAttribute('data-testid')).toBe('alert');
      // The type of the message, as the toast of the same message renders it
      expect(alert.getAttribute('data-type')).toBe('warning');
      // The way a test locates the message of a failed login (the toast of the message actions is not drawn)
      expect(alert.querySelector('[data-testid="alert-title"]').textContent).toBe(TITLE);
      expect(alert.querySelector('[data-testid="alert-message"]').textContent).toBe(TEXT);
      expect(toasts(store)).toEqual([]);
    });

    it('names an error message as the toast does, and keeps the title out when the message has none', () => {
      const { store } = renderSignin();
      fail(store, { ...failure(), parameters: { type: 'error', message: 'User is locked' } });

      const alert = alertBox();
      expect(alert.getAttribute('data-type')).toBe('danger');
      expect(alert.querySelector('[data-testid="alert-title"]')).toBeNull();
      expect(alert.querySelector('[data-testid="alert-message"]').textContent).toBe('User is locked');
    });

    it('ends the message action at once: the actions after it run', () => {
      const { store } = renderSignin(preloadedState({
        running: true, async: [], sync: [[{ ...failure(), status: 'started' }]]
      }));

      fail(store);

      expect(store.getState().actions.sync.flat().map(action => action.id)).not.toContain('failure');
    });

    it('clears the password and gives it the focus, the user stays', () => {
      const { store } = renderSignin();
      expect(password().value).toBe('wrong');

      fail(store);

      expect(password().value).toBe('');
      expect(valueOf(store, 'pwd_usr')).toBe('');
      expect(document.activeElement).toBe(password());
      expect(user().value).toBe('jsmith');
      expect(valueOf(store, 'cod_usr')).toBe('jsmith');
    });

    it('hides the alert when the user edits a field', () => {
      [user, password].forEach(input => {
        const { store } = renderSignin();
        fail(store);
        expect(alertBox()).not.toBeNull();

        fireEvent.input(input(), { target: { value: 'x' } });

        expect(alertBox()).toBeNull();
        cleanup();
      });
    });

    it('hides the alert when a new login starts', () => {
      const { store } = renderSignin();
      fail(store);
      expect(alertBox()).not.toBeNull();

      act(() => {
        store.dispatch({ type: ADD_ACTIONS, payload: [{ ...server('login'), address: { component: 'ButLogIn', view: VIEW } }] });
      });

      expect(alertBox()).toBeNull();
    });

    it('shows the next failure again after the alert was hidden', () => {
      const { store } = renderSignin();
      fail(store);
      fireEvent.input(user(), { target: { value: 'x' } });
      expect(alertBox()).toBeNull();

      fail(store, { ...failure(), id: 'failure-2' });

      expect(alertBox().textContent).toBe(`${TITLE} ${TEXT}`);
      expect(toasts(store)).toEqual([]);
    });

    it('keeps the toast for a message of another component of the sign-in page', () => {
      const { store } = renderSignin();

      fail(store, failure({ component: 'cod_usr', view: VIEW }));

      expect(alertBox()).toBeNull();
      expect(toasts(store)).toHaveLength(1);
      expect(toasts(store)[0]).toEqual(expect.objectContaining({ summary: TITLE }));
      expect(password().value).toBe('wrong');
    });

    it('keeps the toast when the answer goes on to another screen (expired password)', () => {
      const address = { component: 'ButLogIn', view: VIEW };
      const { store } = renderSignin(preloadedState({
        running: true, async: [],
        sync: [[{ ...failure(), status: 'started' }, { id: 'change', type: 'screen', address, parameters: {} }]]
      }));

      fail(store);

      expect(alertBox()).toBeNull();
      expect(toasts(store)).toHaveLength(1);
    });

    describe('with the real answer of the server through the actions of the Login button', () => {
      // The answer of POST /action/login to a wrong password, as the server sends it
      const ANSWER = [
        { type: 'end-load', parameters: {} },
        { type: 'message', parameters: { type: 'warning', title: 'Invalid credentials',
          message: 'The credentials entered for the user -test- are not valid' } },
        { type: 'cancel', parameters: {} }
      ];

      beforeEach(() => {
        global.fetch = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(ANSWER) }));
        window.fetch = global.fetch;
      });

      afterEach(() => {
        delete global.fetch;
        delete window.fetch;
      });

      it('shows the message in the alert, without toast, and clears and focuses the password', async () => {
        // The screen registers its components as the view loads them: the base (actions included) goes to the
        // component registry and the state keeps only the changes
        const { store } = renderWithProviders(<MemoryRouter><Tag {...signinScreen()} /><ActionsContainer/></MemoryRouter>,
          { preloadedState: { settings: { ...SETTINGS, actionsStack: 0 }, components: {} } });
        act(() => {
          store.dispatch({ type: UPDATE_VIEW_COMPONENTS, view: VIEW, data: components() });
        });
        expect(store.getState().components.ButLogIn.actions).toBeUndefined();
        expect(password().value).toBe('wrong');

        fireEvent.click(document.querySelector('button#ButLogIn'));

        await waitFor(() => expect(alertBox()).not.toBeNull());
        expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('action/login'), expect.anything());
        expect(alertBox().querySelector('strong').textContent).toBe('Invalid credentials');
        expect(alertBox().querySelector('.signin-alert-text').textContent)
          .toBe('The credentials entered for the user -test- are not valid');
        expect(toasts(store)).toEqual([]);
        await waitFor(() => expect(password().value).toBe(''));
        expect(document.activeElement).toBe(password());
        expect(user().value).toBe('jsmith');
        // The answer ends: the stack is empty and the Login button is back to normal
        await waitFor(() => expect(store.getState().actions.sync.flat()).toEqual([]));
        expect(document.querySelector('button#ButLogIn').disabled).toBe(false);
      });
    });

    it('keeps the toast of a failed login out of the sign-in page', () => {
      const { store } = renderWithProviders(<><AweInputPassword id="pwd_usr"/><AweButton id="ButLogIn"/></>, {
        preloadedState: preloadedState()
      });

      fail(store);

      expect(alertBox()).toBeNull();
      expect(toasts(store)).toHaveLength(1);
      expect(toasts(store)[0]).toEqual(expect.objectContaining({ severity: 'warn', summary: TITLE }));
      expect(password().value).toBe('wrong');
    });
  });
});
