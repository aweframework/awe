import React from 'react';
import AweButton from '../../../src/components/AweButton';
import { renderWithProviders } from '../test-utils';
import { DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { ADD_ACTIONS_TOP } from '../../../src/redux/actions/actions';
import { ButtonTypes } from '../../../src/redux/actions/components';
import {act, render} from "@testing-library/react";
import { ButtonSkeleton } from '../../../src/components/AweSkeleton';

function baseState(overrides = {}) {
  const state = {
    settings: DEFAULT_SETTINGS,
    actions: { running: false },
    components: {
      btn1: {
        address: { view: 'v', component: 'btn1' },
        attributes: {
          label: 'Click me',
          size: 'lg',
          style: 'my-style',
          visible: true,
          buttonType: ButtonTypes.BUTTON_NORMAL
        },
        actions: []
      }
    }
  };
  return Object.assign({}, state, overrides);
}

function getAddActionsTopCalls(dispatchSpy) {
  if (!dispatchSpy) return [];
  // Support both Jest (mock.calls) and Jasmine (calls.allArgs()) spy APIs
  const allArgs = dispatchSpy.mock ? dispatchSpy.mock.calls : dispatchSpy.calls.allArgs();
  return allArgs.map(args => args[0]).filter(a => a && a.type === ADD_ACTIONS_TOP);
}

describe('awe-react-client/test/js/components/AweButtonTest.jsx', () => {
  it('renders with classes, label and disabled state from attributes/settings', () => {
    const preloadedState = baseState();
    const { container } = renderWithProviders(<AweButton id="btn1" />, { preloadedState });

    const btn = container.querySelector('#btn1');
    expect(btn).toBeTruthy();
    // classes
    expect(btn.className).toContain('help-button-btn1');
    expect(btn.className).toContain('mr-2');
    expect(btn.className).toContain('mt-2');
    expect(btn.className).toContain('p-button-outlined'); // normal type
    expect(btn.className).toContain('p-button-lg');
    expect(btn.className).toContain('text-lg');
    expect(btn.className).toContain('my-style');
    // enabled by default
    expect(btn.hasAttribute('disabled')).toBe(false);
  });

  it('adds hidden class when visible=false', () => {
    const preloaded = baseState({
      components: {
        btn1: {
          address: { view: 'v', component: 'btn1' },
          attributes: { visible: false, buttonType: ButtonTypes.BUTTON_NORMAL },
          actions: []
        }
      }
    });

    const { container } = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded });
    const btn = container.querySelector('#btn1');
    expect(btn.className).toContain('hidden');
  });

  it('disables when global running or attribute disabled', () => {
    // attribute disabled
    const preloaded1 = baseState({
      components: {
        btn1: {
          address: { view: 'v', component: 'btn1' },
          attributes: { disabled: true, buttonType: ButtonTypes.BUTTON_NORMAL },
          actions: []
        }
      }
    });
    let rendered = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded1 });
    let btn = rendered.container.querySelector('#btn1');
    expect(btn.hasAttribute('disabled')).toBe(true);

    // global running
    const preloaded2 = baseState({ actions: { running: true } });
    rendered = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded2 });
    btn = rendered.container.querySelector('#btn1');
    expect(btn.hasAttribute('disabled')).toBe(true);
  });

  it('click dispatches update thunk and then mapped actions when actions are provided', () => {
    const actionList = [{ type: 'do', parameters: { x: 1 } }];
    const preloaded = baseState({
      components: {
        btn1: {
          address: { view: 'v', component: 'btn1' },
          attributes: { buttonType: ButtonTypes.BUTTON_NORMAL },
          actions: actionList
        }
      }
    });

    const { container, dispatchSpy } = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded, spyDispatch: true });
    const btn = container.querySelector('#btn1');
    act(() => btn.click());

    // First dispatch should be a thunk (function)
    const firstCall = dispatchSpy.mock ? dispatchSpy.mock.calls[0][0] : dispatchSpy.calls.argsFor(0)[0];
    expect(typeof firstCall).toBe('function');

    // One addActionsTop should be dispatched with address mapped in each action
    const addTopCalls = getAddActionsTopCalls(dispatchSpy);
    expect(addTopCalls.length).toBe(1);
    const payload = addTopCalls[0].payload;
    expect(Array.isArray(payload)).toBe(true);
    expect(payload[0].type).toBe('do');
    expect(payload[0].address).toEqual({ view: 'v', component: 'btn1' });
  });

  it('click dispatches restore when buttonType=reset and no actions', () => {
    const preloaded = baseState({
      components: {
        btn1: {
          address: { view: 'v', component: 'btn1' },
          attributes: { buttonType: ButtonTypes.BUTTON_RESET },
          actions: [],
          dependencies: []
        }
      }
    });

    const { container, dispatchSpy } = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded, spyDispatch: true });
    const btn = container.querySelector('#btn1');
    act(() => btn.click());

    const addTopCalls = getAddActionsTopCalls(dispatchSpy);
    expect(addTopCalls.length).toBe(1);
    const restore = addTopCalls[0].payload[0];
    expect(restore.type).toBe('restore');
    expect(restore.address).toEqual({ view: 'v', component: 'btn1' });
  });

  it('click without actions and type normal only dispatches update thunk', () => {
    const preloaded = baseState({
      components: {
        btn1: {
          address: { view: 'v', component: 'btn1' },
          attributes: { buttonType: ButtonTypes.BUTTON_NORMAL },
          actions: []
        }
      }
    });

    const { container, dispatchSpy } = renderWithProviders(<AweButton id="btn1" />, { preloadedState: preloaded, spyDispatch: true });
    const btn = container.querySelector('#btn1');
    act(() => btn.click());

    // Only the thunk call should be present, no ADD_ACTIONS_TOP
    const addTopCalls = getAddActionsTopCalls(dispatchSpy);
    expect(addTopCalls.length).toBe(0);
  });

  it('does not crash when component exists without attributes', () => {
    const preloaded = {
      settings: DEFAULT_SETTINGS,
      actions: { running: false },
      components: {
        btnX: {
          address: { view: 'v', component: 'btnX' }
          // no attributes key
        }
      }
    };
    const { container } = renderWithProviders(<AweButton id="btnX" />, { preloadedState: preloaded });
    const btn = container.querySelector('#btnX');
    expect(btn).toBeTruthy();
    // Should have default outlined button class due to default type
    expect(btn.className).toContain('p-button-outlined');
  });

  describe('skeleton', () => {
    const state = (components) => ({ settings: DEFAULT_SETTINGS, actions: { running: false }, components });

    it('shows a hidden skeleton with the loader hook until the button registers', () => {
      const { container } = renderWithProviders(<AweButton id="btn1" style="my-style" />, { preloadedState: state({}) });

      const skeleton = container.querySelector("[data-testid='loader']");
      expect(skeleton).not.toBeNull();
      expect(skeleton.getAttribute('aria-hidden')).toBe('true');
      expect(skeleton.classList.contains('awe-skeleton-button')).toBe(true);
      expect(skeleton.classList.contains('my-style')).toBe(true);
      expect(container.querySelector('button')).toBeNull();
    });

    it('makes the skeleton as wide as the label and as high as the size of the button', () => {
      const { container } = render(<ButtonSkeleton attributes={{ label: 'Accept all', size: 'lg' }} />);

      const skeleton = container.querySelector("[data-testid='loader']");
      expect(skeleton.querySelector('.p-skeleton').style.width).toBe('14ch');
      expect(skeleton.classList.contains('awe-skeleton-lg')).toBe(true);
    });

    it('paints the button, and no skeleton, once it is registered', () => {
      const { container } = renderWithProviders(<AweButton id="btn1" />, { preloadedState: baseState() });

      expect(container.querySelector('button#btn1')).not.toBeNull();
      expect(container.querySelector("[data-testid='loader']")).toBeNull();
    });
  });
});
