import React from 'react';
import { act, cleanup, fireEvent, screen, within } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { LOGIN_MODE_KEY } from '../../../src/utilities/signin';

// A tag with the class awe-preferences shows the language and colour mode selectors of the sign-in page footer
const LOGIN = { ...DEFAULT_LOGIN_SETTINGS, languages: ['en-GB', 'es-ES', 'fr-FR'] };
const render = (login = LOGIN, others = {}) => renderWithProviders(
  <Tag elementType="Tag" type="div" id="prefs" style="awe-preferences" elementList={[]} />,
  { preloadedState: { settings: { ...DEFAULT_SETTINGS, language: 'en-GB', ...others, login } } });
const modeGroup = () => screen.getByRole('radiogroup', { name: 'Mode' });

describe('awe-react-client/test/js/components/PreferencesTest.jsx', () => {
  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      PARAMETER_LANGUAGE: 'Language',
      PARAMETER_THEME_MODE: 'Mode',
      ENUM_THEME_MODE_LIGHT: 'Light',
      ENUM_THEME_MODE_DARK: 'Dark',
      ENUM_THEME_MODE_SYSTEM: 'System',
      'ENUM_LAN_EN-GB': 'English',
      'ENUM_LAN_ES-ES': 'Spanish',
      'ENUM_LAN_FR-FR': 'French'
    });
  });

  afterEach(() => {
    cleanup();
    localStorage.clear();
  });

  it('renders one ghost button for the language and one for the mode, with their current value', () => {
    const { container } = render(LOGIN, { themeMode: 'dark' });

    const language = screen.getByRole('button', { name: 'Language' });
    expect(container.querySelector('#prefs.awe-preferences')).not.toBeNull();
    expect(language.classList.contains('btn-ghost')).toBe(true);
    expect(language.textContent).toBe('EN');
    expect(language.getAttribute('title')).toBe('Language');
    const mode = screen.getByRole('button', { name: 'Mode' });
    expect(mode.classList.contains('btn-ghost')).toBe(true);
    expect(mode.querySelector('use').getAttribute('href').split('#')[1]).toBe('moon');
    expect(screen.queryByRole('radiogroup')).toBeNull();
  });

  it('opens a menu with the current option marked and stores the choice as the sign-in page does', () => {
    const { store } = render(LOGIN, { themeMode: 'light' });

    fireEvent.click(screen.getByRole('button', { name: 'Mode' }));
    const modes = within(screen.getByRole('menu', { name: 'Mode', hidden: true })).getAllByRole('menuitemradio', { hidden: true });
    expect(modes.map(option => option.textContent)).toEqual(['Light', 'Dark', 'System']);
    expect(modes.map(option => option.getAttribute('aria-checked'))).toEqual(['true', 'false', 'false']);
    fireEvent.click(modes[1]);
    expect(store.getState().settings.themeMode).toBe('dark');
    expect(localStorage.getItem(LOGIN_MODE_KEY)).toBe('dark');

    fireEvent.click(screen.getByRole('button', { name: 'Language' }));
    const languages = within(screen.getByRole('menu', { name: 'Language', hidden: true })).getAllByRole('menuitemradio', { hidden: true });
    expect(languages.map(option => option.textContent)).toEqual(['English', 'Spanish', 'French']);
    fireEvent.click(languages[2]);
    expect(store.getState().settings.language).toBe('fr-FR');
  });

  it('draws again the mode chosen before, and hides the selectors the application does not offer', () => {
    localStorage.setItem(LOGIN_MODE_KEY, 'dark');
    const { store } = render({ ...LOGIN, languages: ['en-GB'] }, { themeMode: 'light' });

    expect(store.getState().settings.themeMode).toBe('dark');
    expect(screen.queryByRole('button', { name: 'Language' })).toBeNull();
    cleanup();
    render({ ...LOGIN, modeSelector: false });
    expect(screen.queryByRole('button', { name: 'Mode' })).toBeNull();
  });
});
