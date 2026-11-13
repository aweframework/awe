import React from 'react';
import { act, cleanup } from '@testing-library/react';
import i18n from 'i18next';
import { renderWithProviders } from '../test-utils';
import '../../../src/components/AweComponents';
import Tag from '../../../src/components/Tag';
import { DEFAULT_LOGIN_SETTINGS, DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { register, unregister } from '../../../src/utilities/loginEffects';

// The canvas effect of the login pages (S23): settings.login.backgroundEffect names an effect the application
// registered, drawn where the background image goes (the panel of split, the backdrop of card; form-only has none). An
// unknown name draws nothing and leaves the image, and an effect registered later starts when it registers
const EFFECT = 'brand-wave';
const tag = (id, style, elementList = []) => ({ elementType: 'Tag', type: 'div', id, style, elementList });
const loginScreen = (pageId = 'page-signin') => tag(pageId, 'expand grid m-0', [
  tag(undefined, 'awe-signin signin-container align-self-center shadow-3', [
    tag(undefined, 'signin-info'),
    tag(undefined, 'signin-form', [tag('fields', 'p-2 mt-5')])
  ])
]);

const renderLogin = (login, pageId) => renderWithProviders(<Tag {...loginScreen(pageId)} />,
  { preloadedState: { settings: { ...DEFAULT_SETTINGS, login: { ...DEFAULT_LOGIN_SETTINGS, ...login } } } });

describe('awe-react-client/test/js/templates/SigninPageEffectTest.jsx', () => {
  let effect, stop;

  beforeAll(() => {
    i18n.addResources('en-GB', 'translation', { SCREEN_TEXT_LOGIN_PANEL_TITLE: 'AWE Framework' });
  });

  beforeEach(() => {
    stop = jest.fn();
    effect = jest.fn(() => stop);
  });

  afterEach(() => {
    unregister(EFFECT);
    cleanup();
    jest.restoreAllMocks();
  });

  it.each(['split', 'card'])('draws the registered effect as the panel of the %s layout', layout => {
    register(EFFECT, effect);
    const { container } = renderLogin({ layout, backgroundEffect: EFFECT });
    const page = container.querySelector('#page-signin');
    const wrapper = page.querySelector(':scope > .signin-effect');

    expect(page.getAttribute('data-awe-login-effect')).toBe(EFFECT);
    expect(page.hasAttribute('data-awe-login-effect-running')).toBe(true);
    expect(wrapper.getAttribute('aria-hidden')).toBe('true');
    expect(wrapper.children).toHaveLength(1);
    const canvas = wrapper.querySelector(':scope > canvas');
    expect(effect).toHaveBeenCalledTimes(1);
    expect(effect).toHaveBeenCalledWith(canvas);
    // The caption stays over the effect
    expect(page.querySelector(':scope > .signin-caption > .signin-caption-title').textContent).toBe('AWE Framework');
  });

  it('starts the effect when it is registered after the page is drawn', () => {
    const { container } = renderLogin({ layout: 'split', backgroundEffect: EFFECT });
    const page = container.querySelector('#page-signin');
    expect(page.hasAttribute('data-awe-login-effect-running')).toBe(false);
    expect(page.querySelector('.signin-effect')).toBeNull();
    expect(effect).not.toHaveBeenCalled();

    act(() => register(EFFECT, effect));

    expect(page.hasAttribute('data-awe-login-effect-running')).toBe(true);
    expect(effect).toHaveBeenCalledWith(page.querySelector(':scope > .signin-effect > canvas'));
  });

  it('stops the effect and stops waiting for it when the page goes away', () => {
    register(EFFECT, effect);
    const { container, unmount } = renderLogin({ layout: 'split', backgroundEffect: EFFECT });
    expect(stop).not.toHaveBeenCalled();

    unmount();
    unregister(EFFECT);
    register(EFFECT, effect);

    expect(stop).toHaveBeenCalledTimes(1);
    expect(effect).toHaveBeenCalledTimes(1);
    expect(container.querySelector('.signin-effect')).toBeNull();
  });

  it('draws no effect in the form-only layout, which has no background', () => {
    register(EFFECT, effect);
    const { container } = renderLogin({ layout: 'form-only', backgroundEffect: EFFECT });
    const page = container.querySelector('#page-signin');

    expect(page.querySelector('.signin-effect')).toBeNull();
    expect(page.hasAttribute('data-awe-login-effect')).toBe(false);
    expect(effect).not.toHaveBeenCalled();
  });

  it.each([['no effect', undefined], ['an empty name', '  ']])('keeps the background image with %s', (_name, name) => {
    register(EFFECT, effect);
    const { container } = renderLogin({ layout: 'split', backgroundEffect: name });
    const page = container.querySelector('#page-signin');

    expect(page.querySelector('.signin-effect')).toBeNull();
    expect(page.hasAttribute('data-awe-login-effect')).toBe(false);
    expect(effect).not.toHaveBeenCalled();
  });

  it('keeps the background image with an unknown name: no wrapper covers the panel', () => {
    const { container } = renderLogin({ layout: 'split', backgroundEffect: 'plasma-wave' });
    const page = container.querySelector('#page-signin');

    expect(page.getAttribute('data-awe-login-effect')).toBe('plasma-wave');
    expect(page.hasAttribute('data-awe-login-effect-running')).toBe(false);
    expect(page.querySelector('.signin-effect')).toBeNull();
  });

  it('keeps the page and the image when the effect throws', () => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    register(EFFECT, () => {
      throw new Error('no 2d context');
    });
    const { container } = renderLogin({ layout: 'split', backgroundEffect: EFFECT });
    const page = container.querySelector('#page-signin');

    expect(page.querySelector('#fields')).not.toBeNull();
    expect(page.hasAttribute('data-awe-login-effect-running')).toBe(false);
    expect(page.querySelector('.signin-effect')).toBeNull();
    expect(error).toHaveBeenCalled();
  });

  it('draws the effect on the other login pages too, which take the frame of the sign-in page (S19)', () => {
    register(EFFECT, effect);
    const { container } = renderLogin({ layout: 'card', backgroundEffect: EFFECT }, 'page-change-expired-password');
    const page = container.querySelector('#page-change-expired-password');

    expect(page.getAttribute('data-awe-login-effect')).toBe(EFFECT);
    expect(page.querySelector(':scope > .signin-effect > canvas')).not.toBeNull();
    expect(effect).toHaveBeenCalledTimes(1);
  });
});
