import React from 'react';
import { act, cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import SigninPage from '../../../src/components/SigninPage';
import AweApp from '../../../src/components/AweApp';
import AweInputPassword from '../../../src/criteria/AweInputPassword';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { LOGIN_MODE_KEY } from '../../../src/utilities/signin';

// AweApp renders the whole application: only its theme handling (the mode of <html>) is used here
jest.mock('../../../src/components/View', () => () => null);
jest.mock('../../../src/containers/ActionsContainer', () => () => null);
jest.mock('../../../src/containers/FormContainer', () => ({ children }) => <>{children}</>);
jest.mock('../../../src/containers/MessageContainer', () => () => null);

// The controls of the sign-in page as the prototype: the language and colour mode selectors of the footer (S11), the
// show / hide button and the Caps Lock warning of the password (S12) and the framework default texts (S13)
const tag = (id, style, elementList = []) => ({ elementType: 'Tag', type: 'div', id, style, elementList });
const signinScreen = () => tag('page-signin', 'expand grid m-0', [
  tag(undefined, 'awe-signin signin-container', [tag(undefined, 'signin-info'), tag(undefined, 'signin-form')])
]);

const root = document.documentElement;
const flushMutations = () => new Promise(resolve => setTimeout(resolve, 0));
// Choose a mode and let the page follow the class of <html>
const chooseMode = name => act(async () => {
  fireEvent.click(within(modeGroup()).getByRole('radio', { name }));
  await flushMutations();
});
const LOGIN = { ...DEFAULT_LOGIN_SETTINGS, languages: ['en-GB', 'es-ES', 'fr-FR'] };
const settingsWith = (login, others = {}) => ({ ...DEFAULT_SETTINGS, language: 'en-GB', ...others, login });

const renderSignin = (login = LOGIN, others = {}) => renderWithProviders(<Tag {...signinScreen()} />,
  { preloadedState: { settings: settingsWith(login, others) } });

const PASSWORD = {
  address: { component: 'pwd_usr', view: 'base' },
  model: { values: [] },
  attributes: { label: 'SCREEN_TEXT_PASS', icon: 'lock', readonly: false, visible: true },
  validationRules: {}
};
const renderPassword = (inSignin = true) => {
  const field = <AweInputPassword id="pwd_usr"/>;
  return renderWithProviders(inSignin ? <SigninPage id="page-signin">{field}</SigninPage> : field,
    { preloadedState: { settings: settingsWith(LOGIN), components: { pwd_usr: PASSWORD } } });
};

const password = () => document.querySelector('input#pwd_usr');
const capsWarning = () => document.querySelector('.signin-caps');
const modeGroup = () => screen.getByRole('radiogroup', { name: 'Mode' });

describe('awe-react-client/test/js/templates/SigninPageControlsTest.jsx', () => {
  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      SCREEN_TEXT_LOGIN_TITLE: 'Welcome to AWE',
      SCREEN_TEXT_LOGIN_SUBTITLE: 'Enter your credentials to continue.',
      SCREEN_TEXT_LOGIN_PANEL_TITLE: 'AWE Framework',
      SCREEN_TEXT_LOGIN_PANEL_SUBTITLE: 'Low coding complete functional web applications',
      SCREEN_TEXT_POWERED_BY: 'Powered by',
      SCREEN_TEXT_PASS: 'Password',
      SCREEN_TEXT_SHOW_PASSWORD: 'Show password',
      SCREEN_TEXT_HIDE_PASSWORD: 'Hide password',
      SCREEN_TEXT_CAPS_LOCK_ON: 'Caps Lock is on',
      PARAMETER_LANGUAGE: 'Language',
      PARAMETER_THEME_MODE: 'Mode',
      ENUM_THEME_MODE_LIGHT: 'Light',
      ENUM_THEME_MODE_DARK: 'Dark',
      ENUM_THEME_MODE_SYSTEM: 'System',
      'ENUM_LAN_EN-GB': 'English',
      'ENUM_LAN_ES-ES': 'Spanish',
      'ENUM_LAN_FR-FR': 'French'
    });
    i18n.addResources('fr-FR', 'translation', {
      SCREEN_TEXT_LOGIN_TITLE: 'Bienvenue sur AWE',
      'ENUM_LAN_FR-FR': 'Français'
    });
  });

  afterEach(async () => {
    cleanup();
    localStorage.clear();
    root.removeAttribute('class');
    act(() => {
      fireEvent.keyUp(document, { key: 'CapsLock', modifierCapsLock: false });
    });
    await act(() => i18n.changeLanguage('en-GB'));
  });

  describe('framework default texts (S13)', () => {
    it('shows "Welcome to AWE", the direct subtitle and the AWE Framework caption with the default settings', () => {
      const { container } = renderSignin(DEFAULT_LOGIN_SETTINGS);

      expect(container.querySelector('.signin-title').textContent).toBe('Welcome to AWE');
      expect(container.querySelector('.signin-subtitle').textContent).toBe('Enter your credentials to continue.');
      expect(container.querySelector('.signin-caption-title').textContent).toBe('AWE Framework');
      expect(container.querySelector('.signin-caption-text').textContent)
        .toBe('Low coding complete functional web applications');
      expect(DEFAULT_LOGIN_SETTINGS).toEqual(expect.objectContaining({
        title: 'SCREEN_TEXT_LOGIN_TITLE', subtitle: 'SCREEN_TEXT_LOGIN_SUBTITLE',
        panelTitle: 'SCREEN_TEXT_LOGIN_PANEL_TITLE', panelSubtitle: 'SCREEN_TEXT_LOGIN_PANEL_SUBTITLE',
        poweredBy: true, languageSelector: true, modeSelector: true
      }));
    });
  });

  describe('footer (S11)', () => {
    it('puts the footer text over "Powered by AWE 5" on the left and the selectors on the right', () => {
      const { container } = renderSignin({ ...LOGIN, footerText: 'Veloria © 2026' });

      const legal = container.querySelector('.signin-footer > .signin-legal');
      expect(Array.from(legal.children).map(child => child.className)).toEqual(['signin-footer-text', 'signin-powered']);
      const prefs = container.querySelector('.signin-footer > .signin-prefs');
      expect(prefs.querySelector('.signin-language')).not.toBeNull();
      expect(prefs.querySelector('.signin-mode')).not.toBeNull();
    });

    it('selects the language of the settings in the AWE select, with the globe icon', () => {
      const { container } = renderSignin(LOGIN, { language: 'es-ES' });

      const select = container.querySelector('.signin-language');
      expect(select.classList.contains('p-dropdown')).toBe(true);
      // The only native select is the hidden one of the AWE select for assistive technology: never a native popup
      container.querySelectorAll('select').forEach(native => expect(native.closest('.p-hidden-accessible')).not.toBeNull());
      expect(select.querySelector('.p-dropdown-label').textContent).toBe('Spanish');
      expect(select.querySelector('.p-dropdown-label svg use').getAttribute('href')).toMatch(/#globe$/);
      // The focusable input of the select (keyboard: arrows, Enter, Escape) is named after the language
      expect(select.querySelector('input[aria-haspopup="listbox"]').getAttribute('aria-label')).toBe('Language');
    });

    it('changes the language of the settings and the page with the option chosen', async () => {
      const { container, store } = renderSignin();

      fireEvent.click(container.querySelector('.signin-language'));
      const panel = await waitFor(() => {
        const element = document.querySelector('.signin-language-panel');
        expect(element).not.toBeNull();
        return element;
      });
      // The panel is still entering (its transition does not run in jsdom)
      const options = within(panel).getAllByRole('option', { hidden: true });
      expect(options.map(option => option.getAttribute('aria-label'))).toEqual(['English', 'Spanish', 'French']);
      fireEvent.click(within(panel).getByRole('option', { name: 'French', hidden: true }));

      expect(store.getState().settings.language).toBe('fr-FR');
      // ViewContainer turns the language of the settings into the language of i18n, as the change-language action
      await act(() => i18n.changeLanguage(store.getState().settings.language));
      expect(container.querySelector('.signin-title').textContent).toBe('Bienvenue sur AWE');
      expect(container.querySelector('.signin-language .p-dropdown-label').textContent).toBe('Français');
    });

    it('checks the current colour mode in a light / dark / system radio group with the Lucide icons', () => {
      renderSignin(LOGIN, { themeMode: 'dark' });

      const radios = within(modeGroup()).getAllByRole('radio');
      expect(radios.map(radio => radio.value)).toEqual(['light', 'dark', 'system']);
      expect(radios.map(radio => radio.checked)).toEqual([false, true, false]);
      expect(['Light', 'Dark', 'System'].map(name => within(modeGroup()).getByRole('radio', { name }).value))
        .toEqual(['light', 'dark', 'system']);
      expect(Array.from(modeGroup().querySelectorAll('svg use')).map(use => use.getAttribute('href').split('#')[1]))
        .toEqual(['sun', 'moon', 'monitor']);
    });

    it('checks the system mode when the settings have no light or dark mode', () => {
      renderSignin(LOGIN, { themeMode: undefined });

      expect(within(modeGroup()).getByRole('radio', { name: 'System' }).checked).toBe(true);
    });

    it('switches the page to the mode chosen at once and keeps the choice for the next visit', async () => {
      const { store } = renderWithProviders(<><AweApp/><Tag {...signinScreen()} /></>,
        { preloadedState: { settings: settingsWith(LOGIN, { themeMode: 'light' }) } });
      expect(root.classList.contains('light')).toBe(true);

      await chooseMode('Dark');

      expect(store.getState().settings.themeMode).toBe('dark');
      expect(root.classList.contains('dark')).toBe(true);
      expect(root.classList.contains('light')).toBe(false);
      expect(localStorage.getItem(LOGIN_MODE_KEY)).toBe('dark');

      await chooseMode('System');

      expect(root.classList.contains('dark')).toBe(false);
      expect(localStorage.getItem(LOGIN_MODE_KEY)).toBe('system');
    });

    it('restores the mode chosen before on the sign-in page', () => {
      localStorage.setItem(LOGIN_MODE_KEY, 'dark');
      const { store } = renderSignin(LOGIN, { themeMode: 'light' });

      expect(store.getState().settings.themeMode).toBe('dark');
      expect(within(modeGroup()).getByRole('radio', { name: 'Dark' }).checked).toBe(true);
    });

    it('hides the language selector with language-selector false and the mode selector with mode-selector false', () => {
      const noLanguage = renderSignin({ ...LOGIN, languageSelector: false }).container;
      expect(noLanguage.querySelector('.signin-language')).toBeNull();
      expect(noLanguage.querySelector('.signin-mode')).not.toBeNull();
      noLanguage.remove();

      localStorage.setItem(LOGIN_MODE_KEY, 'dark');
      const { container, store } = renderSignin({ ...LOGIN, modeSelector: false }, { themeMode: 'light' });
      expect(container.querySelector('.signin-mode')).toBeNull();
      expect(container.querySelector('.signin-language')).not.toBeNull();
      // Without the selector the mode of the application is not replaced by a previous choice
      expect(store.getState().settings.themeMode).toBe('light');
    });

    it('names each mode radio after its translated mode, on the radio itself', () => {
      renderSignin();

      expect(within(modeGroup()).getAllByRole('radio').map(radio => radio.getAttribute('aria-label')))
        .toEqual(['Light', 'Dark', 'System']);
    });

    it('hides the language selector when the application has fewer than two languages', () => {
      [[], ['en-GB']].forEach(languages => {
        const { container } = renderSignin({ ...LOGIN, languages });
        expect(container.querySelector('.signin-language')).toBeNull();
        expect(container.querySelector('.signin-mode')).not.toBeNull();
        cleanup();
      });

      const { container } = renderSignin({ ...LOGIN, languages: ['en-GB', 'es-ES'] });
      expect(container.querySelector('.signin-language')).not.toBeNull();
    });

    it('draws no selector area when both selectors are hidden', () => {
      const { container } = renderSignin({ ...LOGIN, languageSelector: false, modeSelector: false });

      expect(container.querySelector('.signin-prefs')).toBeNull();
      expect(container.querySelector('.signin-powered')).not.toBeNull();
    });
  });

  describe('password field (S12)', () => {
    it('shows and hides the password with the eye button', () => {
      renderPassword();

      const toggle = screen.getByRole('button', { name: 'Show password' });
      expect(toggle.getAttribute('type')).toBe('button');
      expect(toggle.getAttribute('aria-pressed')).toBe('false');
      expect(toggle.getAttribute('aria-controls')).toBe('pwd_usr');
      expect(password().getAttribute('type')).toBe('password');

      fireEvent.click(toggle);

      expect(password().getAttribute('type')).toBe('text');
      expect(toggle.getAttribute('aria-pressed')).toBe('true');
      expect(toggle.getAttribute('aria-label')).toBe('Hide password');
      expect(toggle.querySelector('svg use').getAttribute('href')).toMatch(/#eye-off$/);

      fireEvent.click(toggle);

      expect(password().getAttribute('type')).toBe('password');
      expect(toggle.getAttribute('aria-label')).toBe('Show password');
    });

    it('warns under the password while Caps Lock is on, described by the field', () => {
      renderPassword();
      expect(capsWarning()).toBeNull();
      expect(password().hasAttribute('aria-describedby')).toBe(false);

      fireEvent.keyDown(password(), { key: 'A', modifierCapsLock: true });

      const warning = capsWarning();
      expect(warning.textContent).toBe('Caps Lock is on');
      expect(warning.classList.contains('help--warning')).toBe(true);
      expect(warning.parentElement.getAttribute('criterion-id')).toBe('pwd_usr');
      expect(password().getAttribute('aria-describedby')).toBe(warning.id);

      fireEvent.keyUp(password(), { key: 'CapsLock', modifierCapsLock: false });

      expect(capsWarning()).toBeNull();
      expect(password().hasAttribute('aria-describedby')).toBe(false);
    });

    it('keeps the label above and the leading icon of the field', () => {
      const { container } = renderPassword();

      const field = container.querySelector('[criterion-id="pwd_usr"]');
      expect(field.firstElementChild.tagName).toBe('LABEL');
      expect(field.querySelector('.p-inputgroup > .p-inputgroup-addon:first-child svg use').getAttribute('href'))
        .toMatch(/#lock$/);
    });

    it('leaves the password fields out of the sign-in page as they are', () => {
      renderPassword(false);
      fireEvent.keyDown(password(), { key: 'A', modifierCapsLock: true });

      expect(screen.queryByRole('button', { name: 'Show password' })).toBeNull();
      expect(capsWarning()).toBeNull();
      expect(document.querySelector('.caps-lock-icon')).not.toBeNull();
    });
  });
});
