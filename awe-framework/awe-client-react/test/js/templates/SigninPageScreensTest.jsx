import React from 'react';
import { act, cleanup, fireEvent } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import FullTemplate from '../../../src/templates/FullTemplate';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { ACCEPT_ACTION, ADD_ACTIONS, REJECT_ACTION } from '../../../src/redux/actions/actions';
import { ButtonTypes } from '../../../src/redux/actions/components';
import { messageAction } from '../../../src/redux/thunks/messages';

// The change password, expired password and two-factor screens with the frame of the sign-in page (S19): the layout,
// the images, the caption and the footer of settings.login, the fields and the buttons as the sign-in, over the ids,
// the classes and the actions of their screen XML. The behaviours of the Login button (the title and subtitle of
// settings.login, the inline error, the divider and the SSO block) stay on the sign-in page
const VIEW = 'base';
const tag = (id, style, elementList = [], extra = {}) => ({ elementType: 'Tag', type: 'div', id, style, elementList, ...extra });
const text = (style, label) => tag(undefined, style, [{ elementType: 'Tag', type: 'span', label, elementList: [] }]);
const criterion = (id, component) => ({ elementType: 'Criteria', id, component });
const button = id => ({ elementType: 'Button', id });
const brand = style => tag(undefined, style, [
  { elementType: 'Tag', type: 'h1', elementList: [{ elementType: 'Tag', type: 'span', label: 'APP_CLIENT', elementList: [] }] },
  tag(undefined, 'slogan', [], { label: 'APP_NAME' })
]);
const panel = id => tag(id, 'fullScreen', [tag(undefined, 'overlay-image fullScreen'), tag(undefined, 'overlay fullScreen')]);
const actions = buttons => tag(undefined, 'change-password-with', [tag(undefined, 'form-actions', buttons.map(button))]);

// The center source of the screens shared with the AngularJS engine (awe/screen/security): a source tag with a type, the
// image panel of the page and the form column, without an id on the page
const center = (panelId, column) => ({ source: 'center', type: 'div', style: panelId === 'page-totp-bg' ? 'totp-screen fullScreen' : 'fullScreen', elementList: [panel(panelId), column] });
// ChdPwdPub.xml (and change-password.xml, the same page with ButChdPwd and without the back button)
const changePasswordScreen = () => center('page-change-password-bg', tag(undefined, 'awe-change-password change-password-container', [
  brand('change-password-header'),
  tag(undefined, 'change-password-form', [
    text('change-password-text', 'SCREEN_TEXT_CHANGE_PWD'),
    criterion('Usr', 'text'), criterion('OldPas', 'password'), criterion('Pas', 'password'), criterion('RetPas', 'password'),
    actions(['ButCnf', 'ButBck'])
  ])
]));
// check-2fa.xml
const checkTwoFactorScreen = () => center('page-totp-bg', tag(undefined, 'awe-totp totp-container', [
  brand('totp-header'),
  tag(undefined, 'totp-form', [
    text('totp-text', 'SCREEN_TITLE_CHECK_2FA'),
    criterion('code', 'text'),
    tag(undefined, 'totp-with', [tag(undefined, 'form-actions', [button('ButChk2fa')])])
  ])
]));
// activate-2fa.xml: the QR code and the secret code in the tag list of the form
const activateTwoFactorScreen = () => center('page-totp-bg', tag(undefined, 'awe-totp totp-container', [
  brand('totp-header'),
  tag(undefined, 'totp-form', [
    text('totp-text', 'SCREEN_TITLE_ACTIVATE_2FA'),
    tag(undefined, 'qr-code-container text-center', [{ elementType: 'Image', id: 'qr-code' }]),
    tag(undefined, 'panel panel-info panel-body-colorful', [tag(undefined, 'panel-body text-center text-bold', [], { label: 'ABCD EFGH' })]),
    tag(undefined, 'totp-with', [tag(undefined, 'form-actions', [button('ButChk2fa')])])
  ])
]));
// The expired password screen of the React engine (awe-react/screen/security/change-expired-password.xml): the classes
// of the sign-in screen XML on a page with its own id
const expiredPasswordScreen = () => tag('page-change-expired-password', 'expand grid m-0', [
  tag(undefined, 'awe-signin signin-container align-self-center shadow-3', [
    brand('signin-info'),
    tag(undefined, 'signin-form', [
      text('p-2', 'SCREEN_TEXT_CHANGE_EXPIRED_PWD'),
      criterion('Pas', 'password'), criterion('RetPas', 'password'),
      tag(undefined, 'p-2 mt-5', [button('ButChdExpPwd'), button('ButBckExpPwd')])
    ])
  ])
]);

const field = (id, label, icon) => ({
  address: { component: id, view: VIEW }, model: { values: [] }, validationRules: {},
  attributes: { label, icon, readonly: false, visible: true }
});
const server = (name, target = null) => ({ type: 'server', parameters: { serverAction: name, targetAction: target } });
const submit = (id, serverActions) => ({
  address: { component: id, view: VIEW },
  attributes: { visible: true, label: 'BUTTON_CONFIRM', buttonType: ButtonTypes.BUTTON_SUBMIT,
    style: 'no-class btn btn-primary change-password-btn bg-primary' },
  actions: [{ type: 'validate', parameters: {} }, ...serverActions]
});
const back = (id, target) => ({
  address: { component: id, view: VIEW },
  attributes: { visible: true, label: 'BUTTON_BACK', buttonType: ButtonTypes.BUTTON_NORMAL,
    style: 'no-class btn btn-default change-password-btn' },
  actions: [{ type: 'screen', parameters: { target } }]
});
const CHANGE_PASSWORD = {
  Usr: field('Usr', 'PARAMETER_USER', 'user change-password-form-icon'),
  OldPas: field('OldPas', 'PARAMETER_OLD_PASSWD', 'lock change-password-form-icon'),
  Pas: field('Pas', 'PARAMETER_PASSWD', 'lock change-password-form-icon'),
  RetPas: field('RetPas', 'PARAMETER_RETYPE_PASSWD', 'lock change-password-form-icon'),
  ButCnf: submit('ButCnf', [server('maintain', 'ChdPwdPub'), server('login')]),
  ButBck: back('ButBck', 'Lgn')
};
const EXPIRED_PASSWORD = {
  Pas: field('Pas', 'PARAMETER_PASSWD', 'lock'),
  RetPas: field('RetPas', 'PARAMETER_RETYPE_PASSWD', 'lock'),
  ButChdExpPwd: submit('ButChdExpPwd', [server('maintain', 'ChdPwdExpired')]),
  ButBckExpPwd: back('ButBckExpPwd', 'signin')
};
const CHECK_TWO_FACTOR = {
  code: field('code', 'SCREEN_TEXT_TOTP_CODE', 'key totp-form-icon'),
  ButChk2fa: submit('ButChk2fa', [server('maintain-silent', 'verify2faCode')])
};
const ACTIVATE_TWO_FACTOR = {
  'qr-code': { address: { component: 'qr-code', view: VIEW }, attributes: { visible: true, url: '/access/qr-code' } },
  ButChk2fa: submit('ButChk2fa', [server('afterLogin')])
};

const LOGIN = {
  ...DEFAULT_LOGIN_SETTINGS, layout: 'card', background: 'images/brand/bg.jpg', logo: 'images/brand/logo.svg',
  panelTitle: 'Operations Hub', footerText: 'Veloria © 2026', languages: ['en-GB', 'es-ES']
};
const preloadedState = (components, login = LOGIN) => ({
  settings: { ...DEFAULT_SETTINGS, language: 'en-GB', login },
  actions: { running: false, sync: [[]], async: [] },
  components
});
// A shared screen goes through the full template, as the screen of the server; the expired password screen is a tag
const renderShared = (screen, components, login) => renderWithProviders(<FullTemplate elementList={[screen]}/>,
  { preloadedState: preloadedState(components, login) });
const renderExpired = (login) => renderWithProviders(<Tag {...expiredPasswordScreen()}/>,
  { preloadedState: preloadedState(EXPIRED_PASSWORD, login) });

// The page of a screen: the element that holds its image panel, or the page with its own id
const pageOf = panelId => document.getElementById(panelId).parentElement;
const form = () => document.querySelector('.signin-form');
const buttonOf = id => document.querySelector(`button#${id}`);
const inputs = () => [...form().querySelectorAll('input')];
const iconOf = element => element.querySelector('svg use')?.getAttribute('href')?.split('#')[1];

// Click on the submit button: its server action reaches the actions stack and runs
const runServer = (store, id, name) => act(() => {
  store.dispatch({ type: ADD_ACTIONS, payload: [{ ...server(name), address: { component: id, view: VIEW } }] });
});
const runningAction = (store, name) => store.getState().actions.sync[0].find(a => a.parameters?.serverAction === name);

// The frame of settings.login on a login page that is not the sign-in page
const expectFrame = (page, title) => {
  expect(page.classList.contains('signin-page')).toBe(true);
  expect(page.getAttribute('data-awe-login-layout')).toBe('card');
  expect(page.style.getPropertyValue('--awe-login-image')).toBe('url("http://localhost/app/images/brand/bg.jpg")');
  // The logo of the product alone in the brand of the column
  const logo = page.querySelector('.signin-container > .signin-info > img.signin-logo');
  expect(logo.getAttribute('src')).toBe('http://localhost/app/images/brand/logo.svg');
  expect(logo.parentElement.classList.contains('signin-info--logo')).toBe(true);
  expect(page.querySelector(':scope > .signin-caption > .signin-caption-title').textContent).toBe('Operations Hub');
  const footer = page.querySelector(':scope > .signin-footer');
  expect(footer.querySelector('.signin-footer-text').textContent).toBe('Veloria © 2026');
  expect(footer.querySelector('.signin-powered')).not.toBeNull();
  expect(footer.querySelector('.signin-language')).not.toBeNull();
  expect(footer.querySelector('.signin-mode')).not.toBeNull();
  // Its own title, not the title, the subtitle, the alert or the divider of the sign-in page
  expect(form().textContent).toContain(title);
  expect(page.querySelector('.signin-title, .signin-subtitle, .signin-alert, .signin-divider')).toBeNull();
};

// The submit button loading while its server action runs: the spinner, its own label, the button and the fields
// disabled, the form busy
const expectLoading = (id, label) => {
  const submitButton = buttonOf(id);
  expect(form().getAttribute('aria-busy')).toBe('true');
  expect(submitButton.disabled).toBe(true);
  expect(submitButton.classList.contains('is-loading')).toBe(true);
  expect(submitButton.textContent).toBe(label);
  expect(iconOf(submitButton.querySelector('.signin-spin'))).toBe('loader-circle');
  inputs().forEach(input => expect(input.disabled).toBe(true));
};
const expectIdle = (id, label, actionsRunning = false) => {
  const submitButton = buttonOf(id);
  expect(form().getAttribute('aria-busy')).toBe('false');
  // While any action runs every button of the engine is disabled (ActionsRunningButton)
  expect(submitButton.disabled).toBe(actionsRunning);
  expect(submitButton.classList.contains('is-loading')).toBe(false);
  expect(submitButton.textContent).toBe(label);
  expect(submitButton.querySelector('.signin-spin')).toBeNull();
  inputs().forEach(input => expect(input.disabled).toBe(false));
};

// The password fields as the sign-in: the label above, the leading lock, no placeholder, the eye and the Caps Lock warning
const expectPasswords = ids => {
  ids.forEach(id => {
    const input = document.querySelector(`input#${id}`);
    expect(input.getAttribute('placeholder') ?? '').toBe('');
    expect(document.querySelector(`label[for="${id}"]`)).not.toBeNull();
    expect(iconOf(input.closest('.p-inputgroup').querySelector('.p-inputgroup-addon'))).toBe('lock');
    const toggle = document.querySelector(`.signin-password-toggle[aria-controls="${id}"]`);
    expect(toggle.getAttribute('aria-label')).toBe('Show password');
    fireEvent.click(toggle);
    expect(input.getAttribute('type')).toBe('text');
  });
  act(() => {
    fireEvent.keyDown(document, { key: 'CapsLock', modifierCapsLock: true });
  });
  expect(document.querySelectorAll('.signin-caps')).toHaveLength(ids.length);
};

describe('awe-react-client/test/js/templates/SigninPageScreensTest.jsx', () => {
  let base;

  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      SCREEN_TEXT_CHANGE_PWD: 'Change your password',
      SCREEN_TEXT_CHANGE_EXPIRED_PWD: 'Your password has expired',
      SCREEN_TITLE_CHECK_2FA: 'Check the code',
      SCREEN_TITLE_ACTIVATE_2FA: 'Activate two-factor',
      SCREEN_TEXT_TOTP_CODE: 'Code',
      BUTTON_CONFIRM: 'Confirm',
      BUTTON_BACK: 'Back',
      SCREEN_TEXT_SHOW_PASSWORD: 'Show password',
      SCREEN_TEXT_HIDE_PASSWORD: 'Hide password',
      SCREEN_TEXT_CAPS_LOCK_ON: 'Caps Lock is on'
    });
  });

  beforeEach(() => {
    base = document.createElement('base');
    base.setAttribute('href', 'http://localhost/app/');
    document.head.appendChild(base);
  });

  afterEach(() => {
    cleanup();
    base.remove();
    document.documentElement.removeAttribute('class');
    localStorage.clear();
    act(() => {
      fireEvent.keyUp(document, { key: 'CapsLock', modifierCapsLock: false });
    });
  });

  describe('change password screens (S19)', () => {
    it('frames the change password page with settings.login: layout, image, logo, caption and footer', () => {
      renderShared(changePasswordScreen(), CHANGE_PASSWORD);

      const page = pageOf('page-change-password-bg');
      expectFrame(page, 'Change your password');
      // The page keeps the classes of its screen XML, the ids of its fields and buttons
      expect(page.classList.contains('fullScreen')).toBe(true);
      expect(document.querySelector('.awe-change-password.change-password-container').classList
        .contains('signin-container')).toBe(true);
      expect(form().classList.contains('change-password-form')).toBe(true);
      ['Usr', 'OldPas', 'Pas', 'RetPas'].forEach(id => expect(document.querySelector(`input#${id}`)).not.toBeNull());
    });

    it('draws the fields as the sign-in: labels, leading icons, the eye and the Caps Lock warning on every password', () => {
      renderShared(changePasswordScreen(), CHANGE_PASSWORD);

      expect(iconOf(document.querySelector('input#Usr').closest('.p-inputgroup'))).toBe('user');
      expectPasswords(['OldPas', 'Pas', 'RetPas']);
    });

    it('shows the confirm button loading while its server action runs, and the back button neutral outlined', () => {
      const { store } = renderShared(changePasswordScreen(), CHANGE_PASSWORD);
      expectIdle('ButCnf', 'Confirm');
      const backButton = buttonOf('ButBck');
      expect(backButton.classList.contains('signin-secondary')).toBe(true);
      expect(backButton.classList.contains('p-button-outlined')).toBe(false);
      expect(buttonOf('ButCnf').classList.contains('signin-submit')).toBe(true);

      runServer(store, 'ButCnf', 'maintain');
      expectLoading('ButCnf', 'Confirm');
      expect(backButton.classList.contains('is-loading')).toBe(false);

      act(() => {
        store.dispatch({ type: ACCEPT_ACTION, payload: runningAction(store, 'maintain') });
      });
      expectIdle('ButCnf', 'Confirm');
    });

    it('goes back to normal when the change fails, and keeps the toast for its errors', () => {
      const { store } = renderShared(changePasswordScreen(), CHANGE_PASSWORD);
      // ChdPwdPub launches the login server action too: its answer is not a failed login of the sign-in page
      runServer(store, 'ButCnf', 'login');
      expectLoading('ButCnf', 'Confirm');

      act(() => {
        store.dispatch({ type: REJECT_ACTION, payload: runningAction(store, 'login') });
        store.dispatch(messageAction({ id: 'failure', type: 'message', address: { component: 'ButCnf', view: VIEW },
          parameters: { type: 'error', title: 'Error', message: 'The old password is not valid' } }, key => i18n.t(key)));
      });

      expectIdle('ButCnf', 'Confirm');
      expect(store.getState().messages.showing).toHaveLength(1);
      expect(store.getState().messages.signinAlert).toBeUndefined();
      expect(document.querySelector('.signin-alert')).toBeNull();
    });

    it('does not show the confirm button loading while a server action of another component runs', () => {
      const { store } = renderShared(changePasswordScreen(), CHANGE_PASSWORD);

      runServer(store, 'Usr', 'data');

      expectIdle('ButCnf', 'Confirm', true);
    });

    it('frames the expired password page of the React engine and draws its fields and buttons as the sign-in', () => {
      const { store } = renderExpired();

      const page = document.getElementById('page-change-expired-password');
      expectFrame(page, 'Your password has expired');
      expectPasswords(['Pas', 'RetPas']);
      expect(buttonOf('ButBckExpPwd').classList.contains('signin-secondary')).toBe(true);

      runServer(store, 'ButChdExpPwd', 'maintain');
      expectLoading('ButChdExpPwd', 'Confirm');
    });
  });

  describe('two-factor screens (S19)', () => {
    it('frames the code check page and draws its code field and confirm button as the sign-in', () => {
      const { store } = renderShared(checkTwoFactorScreen(), CHECK_TWO_FACTOR);

      const page = pageOf('page-totp-bg');
      expectFrame(page, 'Check the code');
      expect(page.classList.contains('totp-screen')).toBe(true);
      const code = document.querySelector('input#code');
      expect(code.getAttribute('placeholder') ?? '').toBe('');
      expect(document.querySelector('label[for="code"]')).not.toBeNull();
      expect(iconOf(code.closest('.p-inputgroup'))).toBe('key');

      runServer(store, 'ButChk2fa', 'maintain-silent');
      expectLoading('ButChk2fa', 'Confirm');
    });

    it('keeps the QR code and the secret code of the activation page in the frame, its button loading', () => {
      const { store } = renderShared(activateTwoFactorScreen(), ACTIVATE_TWO_FACTOR);

      expectFrame(pageOf('page-totp-bg'), 'Activate two-factor');
      expect(form().querySelector('.qr-code-container')).not.toBeNull();
      expect(form().querySelector('.panel-body').textContent).toBe('ABCD EFGH');

      runServer(store, 'ButChk2fa', 'afterLogin');
      expectLoading('ButChk2fa', 'Confirm');
    });
  });

  it('leaves a source tag of another screen as it is', () => {
    const { container } = renderShared({ source: 'center', type: 'div', id: 'center', style: 'fullScreen',
      elementList: [tag('page-other-bg', 'fullScreen')] }, {});

    const page = container.querySelector('#center');
    expect(page.className).toBe('fullScreen');
    expect(page.hasAttribute('data-awe-login-layout')).toBe(false);
    expect(page.querySelector('.signin-footer')).toBeNull();
  });
});
