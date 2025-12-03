import React from 'react';
import { renderWithProviders } from '../test-utils';
import ActionsContainer from '../../../src/containers/ActionsContainer';
import { ActionStatus } from '../../../src/redux/actions/actions';
import { MemoryRouter } from 'react-router';

const { STATUS_RUNNING, STATUS_STARTED, STATUS_INITIAL } = ActionStatus;

function buildState(overrides = {}) {
  const base = {
    settings: {
      actionsStack: 0,
      // minimal defaults used elsewhere in app
      helpTimeout: 0
    },
    actions: {
      running: false,
      sync: [
        // first stack (only one by default)
        [
          { type: 'save', target: 'tgt1', status: STATUS_RUNNING },
          { type: 'load', status: STATUS_STARTED },
          { type: 'noop', status: STATUS_INITIAL }
        ]
      ],
      async: [
        { type: 'notify', target: 'bg', async: true, status: STATUS_INITIAL }
      ]
    }
  };
  return Object.assign({}, base, overrides);
}

describe('awe-react-client/test/js/containers/ActionsContainerTest.jsx', () => {
  it('renders hidden when settings.actionsStack = 0 and shows when > 0', () => {
    const preloaded = buildState();
    const { container, rerender } = renderWithProviders(<MemoryRouter><ActionsContainer /></MemoryRouter>, { preloadedState: preloaded });

    // Hidden
    const zone = container.querySelector('.actions-zone');
    expect(zone).toBeTruthy();
    expect(zone.style.display).toBe('none');

    // Show by setting actionsStack > 0
    const preloaded2 = buildState({ settings: { actionsStack: 1000 } });
    rerender(<MemoryRouter><ActionsContainer /></MemoryRouter>);

    // Note: rerender keeps the same store, so render again with new state using a fresh render
    const { container: container2 } = renderWithProviders(<MemoryRouter><ActionsContainer /></MemoryRouter>, { preloadedState: preloaded2 });
    const zone2 = container2.querySelector('.actions-zone');
    expect(zone2.style.display).toBe('block');
  });

  it('renders async and sync stacks with proper labels, classes and icons coming from Actions.jsx', () => {
    const preloaded = buildState({ settings: { actionsStack: 1000 } });
    const { container } = renderWithProviders(<MemoryRouter><ActionsContainer /></MemoryRouter>, { preloadedState: preloaded });

    // Async action (p-button-success) with label including type and (target)
    const asyncBtn = Array.from(container.querySelectorAll('.action')).find(el => el.textContent.includes('notify'));
    expect(asyncBtn).toBeTruthy();
    expect(asyncBtn.className).toContain('p-button-success');
    expect(asyncBtn.textContent).toContain('notify');
    expect(asyncBtn.textContent).toContain('(bg)');

    // RUNNING action -> p-button-info and play icon
    const runBtn = Array.from(container.querySelectorAll('.action')).find(el => el.textContent.includes('save'));
    expect(runBtn.className).toContain('p-button-info');
    // Icon element from primereact should have classes containing pi-play
    const runIcon = runBtn.querySelector('.pi-play');
    expect(runIcon).not.toBeNull();

    // STARTED action -> p-button-warning and spinner icon
    const startedBtn = Array.from(container.querySelectorAll('.action')).find(el => el.textContent.includes('load'));
    expect(startedBtn.className).toContain('p-button-warning');
    const spinIcon = startedBtn.querySelector('.pi-spinner');
    expect(spinIcon).not.toBeNull();

    // DEFAULT (initial) action -> p-button-help and no icon
    const defaultBtn = Array.from(container.querySelectorAll('.action')).find(el => el.textContent === 'noop');
    expect(defaultBtn.className).toContain('p-button-help');
  });

  it('renders sync stacks with header and positions based on index', () => {
    const preloaded = buildState({
      settings: { actionsStack: 1000 },
      actions: {
        running: false,
        // two sync stacks so that we can check header and right style for index 1
        sync: [
          [ { type: 'A', status: STATUS_INITIAL } ],
          [ { type: 'B', status: STATUS_INITIAL } ]
        ],
        async: []
      }
    });

    const { container } = renderWithProviders(<MemoryRouter><ActionsContainer /></MemoryRouter>, { preloadedState: preloaded });

    const stacks = container.querySelectorAll('.action-stack');
    expect(stacks.length).toBe(3);
    // First stack header text
    const header1 = stacks[1].querySelector('.p-tag-header');
    expect(header1.textContent).toContain('sync stack 1');
    // Second stack should be positioned to the right by 198px
    expect(stacks[2].style.right).toBe('198px');
  });

  it('toggles actions stack visibility with Alt+Shift+DigitX keyboard shortcut (uses updateSettings)', () => {
    const preloaded = buildState();
    const { store } = renderWithProviders(<MemoryRouter><ActionsContainer /></MemoryRouter>, { preloadedState: preloaded });

    // Simulate the hotkey Alt+Shift+Digit2 -> 2 * 1000
    const evt = new KeyboardEvent('keydown', { altKey: true, shiftKey: true, code: 'Digit2' });
    window.dispatchEvent(evt);

    const state = store.getState();
    expect(state.settings.actionsStack).toBe(2000);
  });
});
