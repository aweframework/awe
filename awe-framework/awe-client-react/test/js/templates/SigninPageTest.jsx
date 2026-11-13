import React from 'react';
import { act, cleanup } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import { DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';

// The sign-in page (screen template "full") branded through settings.login (awe.application.login.*): the layout, the
// images and the texts of the page, over the classes and ids of the sign-in screen XML (a product may override that XML)
const tag = (id, style, elementList = [], extra = {}) => ({ elementType: 'Tag', type: 'div', id, style, elementList, ...extra });
const signinScreen = (pageId = 'page-signin') => tag(pageId, 'expand grid m-0', [
  tag(undefined, 'awe-signin signin-container align-self-center shadow-3', [
    tag(undefined, 'signin-info', [{ elementType: 'Tag', type: 'h1', elementList: [{ elementType: 'Tag', type: 'span', label: 'APP_CLIENT', elementList: [] }] }]),
    tag(undefined, 'signin-form', [tag('fields', 'p-2 mt-5')])
  ])
]);

const root = document.documentElement;
const flushMutations = () => new Promise(resolve => setTimeout(resolve, 0));

const renderSignin = (login, screen = signinScreen()) => renderWithProviders(<Tag {...screen} />,
  { preloadedState: { settings: login === undefined ? DEFAULT_SETTINGS : { ...DEFAULT_SETTINGS, login } } });

const page = container => container.querySelector('#page-signin');
const text = (container, selector) => container.querySelector(selector)?.textContent;

describe('awe-react-client/test/js/templates/SigninPageTest.jsx', () => {
  let base;

  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', {
      APP_CLIENT: 'Almis I.F.',
      SCREEN_TITLE_LOGIN: 'Application login',
      SCREEN_TEXT_POWERED_BY: 'Powered by',
      SCREEN_TEXT_LOGIN_TITLE: 'Welcome to AWE',
      SCREEN_TEXT_LOGIN_SUBTITLE: 'Enter your credentials to continue.',
      SCREEN_TEXT_LOGIN_PANEL_TITLE: 'AWE Framework',
      SCREEN_TEXT_LOGIN_PANEL_SUBTITLE: 'Low coding complete functional web applications',
      LOGIN_WELCOME: 'Welcome back',
      LOGIN_PANEL: 'Operations Hub',
      ACCEPT: 'Accept'
    });
  });

  beforeEach(() => {
    base = document.createElement('base');
    base.setAttribute('href', 'http://localhost/app/');
    document.head.appendChild(base);
  });

  afterEach(() => {
    // Unmounted first: the page follows the class of <html>
    cleanup();
    base.remove();
    root.removeAttribute('class');
    delete window.matchMedia;
  });

  describe('defaults: the framework sign-in page (S6, S13)', () => {
    it('draws the split layout with the AWE logo, the AWE brand panel, the AWE texts and "Powered by AWE 5"', () => {
      const { container } = renderSignin();

      expect(page(container).getAttribute('data-awe-login-layout')).toBe('split');
      expect(page(container).style.getPropertyValue('--awe-login-image')).toBe('');
      expect(container.querySelector('.signin-logo')).toBeNull();
      expect(container.querySelector('.signin-info').classList.contains('signin-info--logo')).toBe(false);
      expect(text(container, '.signin-form > .signin-title')).toBe('Welcome to AWE');
      expect(text(container, '.signin-form > .signin-subtitle')).toBe('Enter your credentials to continue.');
      expect(text(container, '.signin-caption-title')).toBe('AWE Framework');
      expect(text(container, '.signin-caption-text')).toBe('Low coding complete functional web applications');
      expect(container.querySelector('.signin-footer-text')).toBeNull();
      expect(text(container, '#page-signin > .signin-footer .signin-powered')).toBe('Powered by AWE 5');
    });

    it('keeps the ids, the classes and the content of the sign-in screen XML (S9)', () => {
      const { container } = renderSignin();

      // The classes of the XML, and the class of the login pages (signin.css)
      expect(page(container).className).toBe('expand grid m-0 signin-page');
      expect(container.querySelector('#page-signin > .signin-container.awe-signin.shadow-3')).not.toBeNull();
      expect(text(container, '.signin-info > h1 > span')).toBe('Almis I.F.');
      expect(container.querySelector('.signin-container > .signin-form > #fields')).not.toBeNull();
    });

    it('applies the defaults when the server sends no sign-in settings', () => {
      const { container } = renderWithProviders(<Tag {...signinScreen()} />,
        { preloadedState: { settings: { ...DEFAULT_SETTINGS, login: undefined } } });

      expect(page(container).getAttribute('data-awe-login-layout')).toBe('split');
      expect(text(container, '.signin-title')).toBe('Welcome to AWE');
      expect(text(container, '.signin-powered')).toBe('Powered by AWE 5');
    });
  });

  describe('layout (S4)', () => {
    it.each(['split', 'form-only', 'card'])('marks the page with the %s layout', layout => {
      const { container } = renderSignin({ layout, panelTitle: 'LOGIN_PANEL' });

      expect(page(container).getAttribute('data-awe-login-layout')).toBe(layout);
    });

    it('falls back to the split layout for an unknown value', () => {
      const { container } = renderSignin({ layout: 'diagonal' });

      expect(page(container).getAttribute('data-awe-login-layout')).toBe('split');
    });

    it.each(['split', 'card'])('draws the caption over the image of the %s layout', layout => {
      const { container } = renderSignin({ layout, panelTitle: 'LOGIN_PANEL', panelSubtitle: 'Built on AWE 5' });

      expect(text(container, '#page-signin > .signin-caption > .signin-caption-title')).toBe('Operations Hub');
      expect(text(container, '#page-signin > .signin-caption > .signin-caption-text')).toBe('Built on AWE 5');
    });

    it('draws no caption in the form-only layout, which has no image', () => {
      const { container } = renderSignin({ layout: 'form-only', panelTitle: 'LOGIN_PANEL', panelSubtitle: 'Built on AWE 5' });

      expect(container.querySelector('.signin-caption')).toBeNull();
    });
  });

  describe('images (S5)', () => {
    it('sets the background as the login image, resolved against the base URI of the document', () => {
      const { container } = renderSignin({ layout: 'split', background: 'images/brand/background.jpg' });

      expect(page(container).style.getPropertyValue('--awe-login-image'))
        .toBe('url("http://localhost/app/images/brand/background.jpg")');
    });

    it.each([
      ['an absolute URL', 'https://cdn.example.com/bg.jpg', 'https://cdn.example.com/bg.jpg'],
      ['a root relative URL', '/static/bg.jpg', 'http://localhost/static/bg.jpg']
    ])('keeps %s of the background absolute', (_, background, expected) => {
      const { container } = renderSignin({ layout: 'card', background });

      expect(page(container).style.getPropertyValue('--awe-login-image')).toBe(`url("${expected}")`);
    });

    it('replaces the AWE logo with the logo, resolved against the base URI of the document', () => {
      const { container } = renderSignin({ layout: 'split', logo: 'images/brand/logo.svg' });

      const info = container.querySelector('.signin-info');
      expect(info.classList.contains('signin-info--logo')).toBe(true);
      expect(info.querySelector('img.signin-logo').getAttribute('src')).toBe('http://localhost/app/images/brand/logo.svg');
      // The name of the client keeps telling assistive technology who the page belongs to
      expect(info.querySelector('img.signin-logo').getAttribute('alt')).toBe('');
      expect(text(container, '.signin-info > h1')).toBe('Almis I.F.');
    });

    it('uses the dark logo while the page is dark and goes back to the logo when it turns light', async () => {
      root.classList.add('dark');
      const { container } = renderSignin({ layout: 'split', logo: 'logo.svg', logoDark: 'logo-dark.svg' });
      const logo = () => container.querySelector('.signin-logo').getAttribute('src');

      expect(logo()).toBe('http://localhost/app/logo-dark.svg');

      await act(async () => {
        root.classList.replace('dark', 'light');
        await flushMutations();
      });

      expect(logo()).toBe('http://localhost/app/logo.svg');
    });

    it('uses the dark logo in the system mode when the system prefers dark', () => {
      window.matchMedia = jest.fn(query => ({
        matches: query === '(prefers-color-scheme: dark)',
        addEventListener: jest.fn(),
        removeEventListener: jest.fn()
      }));
      const { container } = renderSignin({ layout: 'split', logo: 'logo.svg', logoDark: 'logo-dark.svg' });

      expect(container.querySelector('.signin-logo').getAttribute('src')).toBe('http://localhost/app/logo-dark.svg');
    });

    it('keeps the logo in dark mode when there is no dark logo', () => {
      root.classList.add('dark');
      const { container } = renderSignin({ layout: 'split', logo: 'logo.svg' });

      expect(container.querySelector('.signin-logo').getAttribute('src')).toBe('http://localhost/app/logo.svg');
    });
  });

  describe('texts (S5, S8)', () => {
    it('translates the texts that are locale keys', () => {
      const { container } = renderSignin({
        layout: 'split', title: 'LOGIN_WELCOME', subtitle: 'SCREEN_TITLE_LOGIN', panelTitle: 'LOGIN_PANEL',
        panelSubtitle: 'LOGIN_WELCOME', footerText: 'APP_CLIENT', poweredBy: true
      });

      expect(text(container, '.signin-form > .signin-title')).toBe('Welcome back');
      expect(text(container, '.signin-form > .signin-subtitle')).toBe('Application login');
      expect(text(container, '.signin-caption-title')).toBe('Operations Hub');
      expect(text(container, '.signin-caption-text')).toBe('Welcome back');
      expect(text(container, '.signin-footer .signin-footer-text')).toBe('Almis I.F.');
    });

    it('shows as they are the texts that are not locale keys, even when one of their words is a key', () => {
      const literals = {
        title: 'Sign in to  Veloria', subtitle: 'ACCEPT the terms to continue.', panelTitle: 'Veloria Logistics',
        panelSubtitle: 'From the warehouse: to the door.', footerText: 'Veloria Logistics © 2026'
      };
      const { container } = renderSignin({ layout: 'split', ...literals, poweredBy: true });

      expect(text(container, '.signin-title')).toBe(literals.title);
      expect(text(container, '.signin-subtitle')).toBe(literals.subtitle);
      expect(text(container, '.signin-caption-title')).toBe(literals.panelTitle);
      expect(text(container, '.signin-caption-text')).toBe(literals.panelSubtitle);
      expect(text(container, '.signin-footer-text')).toBe(literals.footerText);
    });

    it('draws no element for a text that is not configured', () => {
      const { container } = renderSignin({ layout: 'split', panelSubtitle: 'Built on AWE 5', poweredBy: true });

      expect(container.querySelector('.signin-title')).toBeNull();
      expect(container.querySelector('.signin-subtitle')).toBeNull();
      expect(container.querySelector('.signin-caption-title')).toBeNull();
      expect(text(container, '.signin-caption-text')).toBe('Built on AWE 5');
      expect(container.querySelector('.signin-footer-text')).toBeNull();
    });

    it('hides "Powered by AWE 5" when powered-by is false, and the footer when it has nothing to show', () => {
      const nothing = { languageSelector: false, modeSelector: false };
      const withText = renderSignin({ layout: 'split', footerText: 'Veloria © 2026', poweredBy: false, ...nothing }).container;
      expect(text(withText, '.signin-footer-text')).toBe('Veloria © 2026');
      expect(withText.querySelector('.signin-powered')).toBeNull();

      const empty = renderSignin({ layout: 'split', poweredBy: false, ...nothing }).container;
      expect(empty.querySelector('.signin-footer')).toBeNull();
    });
  });

  it('gives the other login pages the frame of the sign-in page but not its title (S19)', () => {
    const { container } = renderSignin({ layout: 'card', logo: 'logo.svg', title: 'LOGIN_WELCOME', footerText: 'Veloria' },
      signinScreen('page-change-expired-password'));

    const other = container.querySelector('#page-change-expired-password');
    expect(other.getAttribute('data-awe-login-layout')).toBe('card');
    ['.signin-logo', '.signin-footer'].forEach(selector => expect(container.querySelector(selector)).not.toBeNull());
    expect(container.querySelector('.signin-title')).toBeNull();
  });
});
