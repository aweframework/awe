import React from 'react';
import { act, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../test-utils';
import { DEFAULT_SETTINGS, updateSettings } from '../../../src/redux/actions/settings';
import { registerComponents, updateModel } from '../../../src/redux/actions/components';
import {
  applyThemeAppearance,
  observeModeClasses,
  pickThemeAppearance
} from '../../../src/utilities/themeAppearance';
import { getAllComponents } from '../../../src/redux/selectors/componentSelectors';
import AweApp from '../../../src/components/AweApp';

// AweApp renders the whole application: only its theme handling is under test here
jest.mock('../../../src/components/View', () => () => null);
jest.mock('../../../src/containers/ActionsContainer', () => () => null);
jest.mock('../../../src/containers/FormContainer', () => ({ children }) => <>{children}</>);
jest.mock('../../../src/containers/MessageContainer', () => () => null);

const root = document.documentElement;
const flushMutations = () => new Promise(resolve => setTimeout(resolve, 0));

const HEX_PRIMARY = {
  '--awe-primary-50': '#eef2ff',
  '--awe-primary-500': '#4f46e5',
  '--awe-primary-fill': '#4f46e5',
  '--awe-primary-on-fill': '#ffffff'
};
const HEX_SURFACE = { '--awe-surface-0': '#ffffff', '--awe-surface-50': '#f4f1ea' };

const resetRoot = () => {
  root.removeAttribute('class');
  root.removeAttribute('style');
  ['data-awe-primary', 'data-awe-surface', 'data-awe-density'].forEach(name => root.removeAttribute(name));
};

const inlineProperties = () => Array.from(root.style).filter(name => name.startsWith('--awe-'));

describe('awe-react-client/test/js/styles/themeAppearanceTest.jsx', () => {
  beforeEach(resetRoot);
  afterEach(resetRoot);

  describe('applyThemeAppearance', () => {
    it('renders a palette name as data-awe-primary and data-awe-surface without inline properties', () => {
      applyThemeAppearance({ themePrimary: 'indigo', themeSurface: 'stone', themeMode: 'system', density: 'comfortable', themePalette: {} });

      expect(root.getAttribute('data-awe-primary')).toBe('indigo');
      expect(root.getAttribute('data-awe-surface')).toBe('stone');
      expect(root.getAttribute('data-awe-density')).toBe('comfortable');
      expect(root.hasAttribute('style')).toBe(false);
    });

    it('renders a hex primary as custom and its palette as inline custom properties', () => {
      applyThemeAppearance({ themePrimary: '#4f46e5', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: HEX_PRIMARY });

      expect(root.getAttribute('data-awe-primary')).toBe('custom');
      Object.entries(HEX_PRIMARY).forEach(([name, value]) => expect(root.style.getPropertyValue(name)).toBe(value));
      expect(inlineProperties()).toHaveLength(Object.keys(HEX_PRIMARY).length);
    });

    it('renders a hex surface as custom and its palette as inline custom properties', () => {
      applyThemeAppearance({ themePrimary: 'emerald', themeSurface: '#f4f1ea', themeMode: 'system', density: 'comfortable', themePalette: HEX_SURFACE });

      expect(root.getAttribute('data-awe-primary')).toBe('emerald');
      expect(root.getAttribute('data-awe-surface')).toBe('custom');
      expect(root.style.getPropertyValue('--awe-surface-50')).toBe('#f4f1ea');
    });

    it('removes the previous generated properties on every change', () => {
      applyThemeAppearance({ themePrimary: '#4f46e5', themeSurface: '#f4f1ea', themeMode: 'system', density: 'comfortable', themePalette: { ...HEX_PRIMARY, ...HEX_SURFACE } });
      applyThemeAppearance({ themePrimary: '#123456', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: { '--awe-primary-500': '#123456' } });

      expect(inlineProperties()).toEqual(['--awe-primary-500']);
      expect(root.style.getPropertyValue('--awe-primary-500')).toBe('#123456');

      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: {} });

      expect(root.getAttribute('data-awe-primary')).toBe('sky');
      expect(inlineProperties()).toEqual([]);
      expect(root.hasAttribute('style')).toBe(false);
    });

    it('keeps the inline properties of <html> that are not a generated palette', () => {
      root.style.setProperty('--other-property', '1px');

      applyThemeAppearance({ themePrimary: '#4f46e5', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: HEX_PRIMARY });
      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: {} });

      expect(root.style.getPropertyValue('--other-property')).toBe('1px');
      expect(inlineProperties()).toEqual([]);
    });

    it('does not render data-awe-surface for the auto surface', () => {
      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'slate', themeMode: 'system', density: 'comfortable' });
      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'auto', themeMode: 'system', density: 'comfortable' });

      expect(root.hasAttribute('data-awe-surface')).toBe(false);
    });

    it('renders the density', () => {
      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'auto', themeMode: 'system', density: 'compact' });

      expect(root.getAttribute('data-awe-density')).toBe('compact');
    });

    it.each([
      ['light', ['light']],
      ['dark', ['dark']],
      ['system', []]
    ])('renders the %s mode as the classes %p of <html>', (themeMode, classes) => {
      root.classList.add('pxajs', 'light', 'dark');

      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'auto', themeMode, density: 'comfortable' });

      expect(Array.from(root.classList)).toEqual(['pxajs', ...classes]);
    });

    it('removes the attributes the settings do not carry', () => {
      applyThemeAppearance({ themePrimary: 'sky', themeSurface: 'slate', themeMode: 'dark', density: 'compact' });
      applyThemeAppearance({});

      expect(root.hasAttribute('data-awe-primary')).toBe(false);
      expect(root.hasAttribute('data-awe-surface')).toBe(false);
      expect(root.hasAttribute('data-awe-density')).toBe(false);
      expect(root.classList.contains('dark')).toBe(false);
    });
  });

  describe('pickThemeAppearance', () => {
    it('keeps only the appearance fields that the settings carry', () => {
      expect(pickThemeAppearance({ theme: 'sky', cometUID: 'x', themePrimary: 'sky', themeMode: 'dark', density: 'compact' }))
        .toEqual({ themePrimary: 'sky', themeMode: 'dark', density: 'compact' });
      expect(pickThemeAppearance({ message: 'error', status: 500 })).toEqual({});
    });
  });

  describe('observeModeClasses', () => {
    it('calls back only when one of the mode classes of the element changes', async () => {
      const onChange = jest.fn();
      const stop = observeModeClasses(document.body, ['dark', 'light'], onChange);

      document.body.classList.add('page-home');
      await flushMutations();
      expect(onChange).not.toHaveBeenCalled();

      document.body.classList.add('dark');
      await flushMutations();
      expect(onChange).toHaveBeenCalledTimes(1);

      stop();
      document.body.classList.remove('dark');
      await flushMutations();
      expect(onChange).toHaveBeenCalledTimes(1);
      document.body.className = '';
    });
  });

  describe('AweApp', () => {
    const appearance = { themePrimary: 'indigo', themeSurface: 'auto', themeMode: 'system', density: 'comfortable', themePalette: {} };
    const settingsState = (settings) => ({ settings: { ...DEFAULT_SETTINGS, theme: 'sky', ...settings } });
    const currentMode = (value) => ({ id: 'currentMode', model: { values: [{ value, selected: true }] } });

    beforeEach(() => {
      global.fetch = jest.fn();
      window.fetch = global.fetch;
    });

    afterEach(() => {
      document.head.querySelectorAll('link').forEach(link => link.remove());
      document.body.className = '';
    });

    it('renders the appearance of the loaded settings on <html> and updates it when the settings change', () => {
      const { store } = renderWithProviders(<AweApp/>, { preloadedState: settingsState(appearance) });

      expect(root.getAttribute('data-awe-primary')).toBe('indigo');
      expect(root.hasAttribute('data-awe-surface')).toBe(false);
      expect(root.getAttribute('data-awe-density')).toBe('comfortable');
      expect(root.classList.contains('dark')).toBe(false);

      act(() => store.dispatch(updateSettings({ themePrimary: '#4f46e5', themeMode: 'dark', density: 'compact', themePalette: HEX_PRIMARY })));

      expect(root.getAttribute('data-awe-primary')).toBe('custom');
      expect(root.style.getPropertyValue('--awe-primary-500')).toBe('#4f46e5');
      expect(root.classList.contains('dark')).toBe(true);
      expect(root.getAttribute('data-awe-density')).toBe('compact');
    });

    // The AWE theme of PrimeReact is bundled (#885): the theme name only selects the preset of the <html> attributes
    it('adds no theme stylesheet link and keeps the classes of the body', () => {
      document.body.classList.add('layout-dark');

      const { store } = renderWithProviders(<AweApp/>, { preloadedState: settingsState(appearance) });
      act(() => store.dispatch(updateSettings({ themeMode: 'light' })));

      const links = Array.from(document.head.querySelectorAll('link[rel="stylesheet"]')).map(link => link.getAttribute('href'));
      expect(links).toEqual([]);
      expect(Array.from(document.body.classList)).toEqual(['layout-dark']);
    });

    it('reads the appearance again from the server when the user changes the mode', async () => {
      global.fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ ...appearance, themeMode: 'dark', theme: 'ignored' }) });
      const { store } = renderWithProviders(<AweApp/>, {
        preloadedState: { ...settingsState(appearance), components: { currentMode: currentMode('') } }
      });
      expect(global.fetch).not.toHaveBeenCalled();

      act(() => store.dispatch(updateModel({ view: 'base', component: 'currentMode' }, { values: [{ value: 'dark', selected: true }] })));

      await waitFor(() => expect(root.classList.contains('dark')).toBe(true));
      expect(global.fetch).toHaveBeenCalledWith(expect.stringMatching(/\/settings\/appearance$/), expect.objectContaining({ method: 'POST' }));
      expect(store.getState().settings.theme).toBe('sky');
    });

    it('reads the appearance again when the user chooses a mode on a selector without value', async () => {
      global.fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ ...appearance, themeMode: 'light' }) });
      const { store } = renderWithProviders(<AweApp/>, {
        preloadedState: { ...settingsState(appearance), components: { currentMode: { id: 'currentMode', model: { values: [] } } } }
      });

      act(() => store.dispatch(updateModel({ view: 'base', component: 'currentMode' }, { values: [{ value: 'light', selected: true }] })));

      await waitFor(() => expect(root.classList.contains('light')).toBe(true));
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(global.fetch).toHaveBeenCalledWith(expect.stringMatching(/\/settings\/appearance$/), expect.anything());
    });

    it('does not read the appearance when the mode selector is mounted', () => {
      const { store } = renderWithProviders(<AweApp/>, { preloadedState: settingsState(appearance) });

      act(() => store.dispatch(registerComponents({ currentMode: { ...currentMode('dark'), address: { view: 'base', component: 'currentMode' } } })));

      expect(getAllComponents(store.getState()).currentMode.model.values[0].value).toBe('dark');
      expect(global.fetch).not.toHaveBeenCalled();
    });
  });
});
