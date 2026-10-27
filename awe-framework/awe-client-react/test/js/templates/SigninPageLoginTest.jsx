import React from 'react';
import { act, cleanup, screen } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import AweButton from '../../../src/components/AweButton';
import AweInputText from '../../../src/criteria/AweInputText';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { ACCEPT_ACTION, ADD_ACTIONS, ADD_ACTIONS_TOP, REJECT_ACTION } from '../../../src/redux/actions/actions';
import { ButtonTypes } from '../../../src/redux/actions/components';

// The actions of the sign-in form as the prototype: the loading Login button (S14), the official "Sign in with
// Microsoft" button (S15) and the SSO block under the "or continue with" divider (S16), over the ids, the classes and
// the server actions of the sign-in screen XML
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
      tag(undefined, 'p-2 mt-5', [button('ButLogIn'), button('ButLogInAzure'), button('ButLogInSSO')])
    ])
  ])
]);

const field = (id, label, icon) => ({
  address: { component: id, view: VIEW }, model: { values: [] }, validationRules: {},
  attributes: { label, icon, readonly: false, visible: true }
});
const server = name => ({ type: 'server', parameters: { serverAction: name, targetAction: null } });
const loginButton = (id, attributes, serverAction, visible = true) => ({
  address: { component: id, view: VIEW },
  attributes: { visible, ...attributes },
  actions: serverAction === 'login' ? [{ type: 'validate', parameters: {} }, server('login')] : [server(serverAction)]
});
const components = ({ azure = true, sso = true, login = true } = {}) => ({
  cod_usr: field('cod_usr', 'SCREEN_TEXT_USER', 'user'),
  pwd_usr: field('pwd_usr', 'SCREEN_TEXT_PASS', 'lock'),
  ButLogIn: loginButton('ButLogIn', { label: 'BUTTON_LOGIN', buttonType: ButtonTypes.BUTTON_SUBMIT, style: 'm-0 col-12' },
    'login', login),
  ButLogInAzure: loginButton('ButLogInAzure', { style: 'sign-in-azure-btn-light' }, 'login-azure', azure),
  ButLogInSSO: loginButton('ButLogInSSO', { label: 'BUTTON_LOGIN_SSO', style: 'm-0 col-12' }, 'login-sso', sso)
});

const renderSignin = (visibility) => renderWithProviders(<Tag {...signinScreen()} />, {
  preloadedState: {
    settings: { ...DEFAULT_SETTINGS, language: 'en-GB', login: DEFAULT_LOGIN_SETTINGS },
    actions: { running: false, sync: [[]], async: [] },
    components: components(visibility)
  }
});

const form = () => document.querySelector('.signin-form');
const login = () => document.querySelector('button#ButLogIn');
const microsoft = () => document.querySelector('button#ButLogInAzure');
const sso = () => document.querySelector('button#ButLogInSSO');
const divider = () => document.querySelector('.signin-divider');
const inputs = () => [document.querySelector('input#cod_usr'), document.querySelector('input#pwd_usr')];
const iconOf = element => element.querySelector('svg use')?.getAttribute('href')?.split('#')[1];

// Click on Login: the actions of the button reach the actions stack and the login server action runs
const clickLogin = store => act(() => {
  store.dispatch({ type: ADD_ACTIONS, payload: [server('login')].map(a => ({ ...a, address: { component: 'ButLogIn', view: VIEW } })) });
});
const loginAction = store => store.getState().actions.sync[0].find(a => a.parameters?.serverAction === 'login');

const expectIdle = () => {
  expect(form().getAttribute('aria-busy')).toBe('false');
  expect(login().disabled).toBe(false);
  expect(login().classList.contains('is-loading')).toBe(false);
  expect(login().textContent).toBe('Login');
  expect(login().querySelector('.signin-spin')).toBeNull();
  inputs().forEach(input => expect(input.disabled).toBe(false));
};

describe('awe-react-client/test/js/templates/SigninPageLoginTest.jsx', () => {
  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      BUTTON_LOGIN: 'Login',
      BUTTON_LOGIN_SSO: 'Login with SSO',
      SCREEN_TEXT_SIGNING_IN: 'Signing in…',
      SCREEN_TEXT_SIGN_IN_MICROSOFT: 'Sign in with Microsoft',
      SCREEN_TEXT_LOGIN_OR: 'or continue with'
    });
  });

  afterEach(cleanup);

  describe('Login loading (S14)', () => {
    it('shows the spinner and "Signing in…" and disables the button and the fields while the login runs', () => {
      const { store } = renderSignin();
      expectIdle();

      clickLogin(store);

      expect(form().getAttribute('aria-busy')).toBe('true');
      expect(login().disabled).toBe(true);
      expect(login().classList.contains('is-loading')).toBe(true);
      expect(login().textContent).toBe('Signing in…');
      const spinner = login().querySelector('.signin-spin');
      expect(iconOf(spinner)).toBe('loader-circle');
      // The rotation of the icon value (fa-spin), stopped when the user prefers reduced motion (awe-icon.css)
      expect(spinner.classList.contains('awe-icon-spin')).toBe(true);
      inputs().forEach(input => expect(input.disabled).toBe(true));
      expect(document.querySelector('.signin-password-toggle').disabled).toBe(true);

      // The response of the server: its actions run and the login server action ends
      act(() => {
        store.dispatch({ type: ADD_ACTIONS_TOP, payload: [{ type: 'message', parameters: {}, address: { component: 'ButLogIn', view: VIEW } }] });
        store.dispatch({ type: ACCEPT_ACTION, payload: loginAction(store) });
      });

      expect(form().getAttribute('aria-busy')).toBe('false');
      expect(login().classList.contains('is-loading')).toBe(false);
      expect(login().textContent).toBe('Login');
      inputs().forEach(input => expect(input.disabled).toBe(false));
    });

    it('goes back to normal when the login fails', () => {
      const { store } = renderSignin();
      clickLogin(store);
      expect(form().getAttribute('aria-busy')).toBe('true');

      act(() => {
        store.dispatch({ type: REJECT_ACTION, payload: loginAction(store) });
      });

      expectIdle();
    });

    it('does not show the Login button loading while another server action runs', () => {
      const { store } = renderSignin();

      act(() => {
        store.dispatch({ type: ADD_ACTIONS, payload: [{ ...server('login-sso'), address: { component: 'ButLogInSSO', view: VIEW } }] });
      });

      expect(form().getAttribute('aria-busy')).toBe('false');
      expect(login().classList.contains('is-loading')).toBe(false);
      expect(login().textContent).toBe('Login');
      inputs().forEach(input => expect(input.disabled).toBe(false));
    });
  });

  it('leaves the buttons and the fields out of the sign-in page as they are', () => {
    const { store } = renderWithProviders(<><AweInputText id="cod_usr"/><AweButton id="ButLogIn"/>
      <AweButton id="ButLogInAzure"/><AweButton id="ButLogInSSO"/></>, {
      preloadedState: {
        settings: { ...DEFAULT_SETTINGS, language: 'en-GB' },
        actions: { running: false, sync: [[]], async: [] },
        components: components()
      }
    });
    clickLogin(store);

    expect(login().classList.contains('is-loading')).toBe(false);
    expect(login().textContent).toBe('Login');
    expect(document.querySelector('input#cod_usr').disabled).toBe(false);
    expect(microsoft().classList.contains('signin-microsoft')).toBe(false);
    expect(microsoft().classList.contains('p-button-outlined')).toBe(true);
    expect(sso().querySelector('svg')).toBeNull();
    expect(divider()).toBeNull();
  });

  describe('Login button (S18)', () => {
    it('draws the Login button with its text alone, without icon, as the AngularJS engine', () => {
      renderSignin();

      expect(login().querySelector('svg, .p-button-icon')).toBeNull();
      expect(login().textContent).toBe('Login');
      // The main button of the form (semibold in signin.css), not an outlined one
      expect(login().classList.contains('signin-submit')).toBe(true);
      expect(login().classList.contains('p-button-outlined')).toBe(false);
    });
  });

  describe('Sign in with Microsoft (S15)', () => {
    it('draws the official button: the Microsoft logo and the translated "Sign in with Microsoft"', () => {
      renderSignin();

      const button = microsoft();
      expect(button.classList.contains('signin-microsoft')).toBe(true);
      expect(button.textContent).toBe('Sign in with Microsoft');
      expect(screen.getByRole('button', { name: 'Sign in with Microsoft' })).toBe(button);
      const logo = button.querySelector('.signin-microsoft-logo');
      expect(logo.getAttribute('aria-hidden')).toBe('true');
      // The logo comes before the text
      expect(button.firstElementChild).toBe(logo);
      // An official button, not an outlined or secondary one
      expect(button.classList.contains('p-button-outlined')).toBe(false);
    });
  });

  describe('SSO block (S16)', () => {
    it('puts the "or continue with" divider after the Login button when an SSO button is visible', () => {
      renderSignin();

      expect(divider().textContent).toBe('or continue with');
      expect(login().nextElementSibling).toBe(divider());
      [{ azure: false }, { sso: false }].forEach(visibility => {
        cleanup();
        renderSignin(visibility);
        expect(divider()).not.toBeNull();
      });
    });

    it('shows no divider when no SSO button is visible or the Login button is hidden', () => {
      renderSignin({ azure: false, sso: false });
      expect(divider()).toBeNull();
      cleanup();

      renderSignin({ login: false });
      expect(divider()).toBeNull();
    });

    it('draws the generic SSO button neutral as the Microsoft one, with the key icon and its own label (S18)', () => {
      renderSignin();

      const button = sso();
      // Neither the primary outline of the theme nor a filled primary button: the neutral look of .signin-sso
      expect(button.classList.contains('p-button-outlined')).toBe(false);
      expect(button.classList.contains('p-button-secondary')).toBe(false);
      expect(button.classList.contains('signin-sso')).toBe(true);
      expect(button.textContent).toBe('Login with SSO');
      expect(iconOf(button)).toBe('key-round');
    });

    it('draws the key icon once, as the icon of the button, before its label', () => {
      renderSignin();

      const button = sso();
      expect(button.querySelectorAll('svg')).toHaveLength(1);
      // The icon of the button of the theme, the same element as the icon of any other AWE button
      const icon = button.querySelector('.p-button-icon');
      // A child of the button, as the logo of the Microsoft button: the gap of the button sets them apart (T16)
      expect(icon.parentElement).toBe(button);
      expect(icon.classList.contains('p-button-icon-left')).toBe(true);
      expect(icon.classList.contains('p-c')).toBe(true);
      expect(icon.nextElementSibling.classList.contains('p-button-label')).toBe(true);
    });

    it('keeps the icon of the sign-in screen on the SSO button instead of the key', () => {
      const state = components();
      state.ButLogInSSO.attributes.icon = 'user';
      renderWithProviders(<Tag {...signinScreen()} />, {
        preloadedState: {
          settings: { ...DEFAULT_SETTINGS, language: 'en-GB', login: DEFAULT_LOGIN_SETTINGS },
          actions: { running: false, sync: [[]], async: [] },
          components: state
        }
      });

      expect(sso().querySelectorAll('svg')).toHaveLength(1);
      expect(iconOf(sso())).toBe('user');
    });
  });
});
