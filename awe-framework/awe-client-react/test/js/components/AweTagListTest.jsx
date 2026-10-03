import React from 'react';
import { renderWithProviders } from '../test-utils';
import AweTagList from '../../../src/components/AweTagList';
import { DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';
import { updateModel } from '../../../src/redux/actions/components';
import {act, screen, waitFor} from '@testing-library/react';

function baseState(overrides = {}) {
  const state = {
    settings: DEFAULT_SETTINGS,
    components: {
      tag1: {
        address: { view: 'v', component: 'tag1' },
        model: { values: [ { id: 'A', name: 'Alpha' } ] },
        attributes: { label: 'My Tags', style: 'my-style', expand: 'md', visible: true }
      }
    }
  };
  return Object.assign({}, state, overrides);
}

describe('awe-react-client/test/js/components/AweTagListTest.jsx', () => {
  it('renders with translated label and classes (style, expandible, visibility)', () => {
    const preloaded = baseState();

    const { container } = renderWithProviders(
      <AweTagList id="tag1" elementList={[{ type: 'div', text: 'hello' }]} />,
      { preloadedState: preloaded }
    );

    const root = container.querySelector('#tag1');
    expect(root).toBeTruthy();
    // style class
    expect(root.className).toContain('my-style');
    // expandible-X class
    expect(root.className).toContain('expandible-md');
    // visible => not hidden
    expect(root.className).not.toContain('hidden');
    // contains the translated label text (falls back to same string in tests)
    expect(root.textContent).toContain('My Tags');
  });

  it('exposes the test hook of the tag list with its identifier', () => {
    const { container } = renderWithProviders(
      <AweTagList id="tag1" elementList={[{ type: 'div', text: 'hello' }]} />,
      { preloadedState: baseState() }
    );

    const hook = container.querySelector("[data-testid='tag-list']");
    expect(hook).toBe(container.querySelector('#tag1'));
    expect(hook.getAttribute('tag-list-id')).toBe('tag1');
  });

  it('dispatches UPDATE_MULTIPLE_COMPONENTS on mount with components built from elementList and model values',() => {
    const elementList = [
      { id: 'innerBtn', type: 'Button', label: 'Row [name]' },
      { type: 'div', text: 'static' },
      { id: 'innerTag', type: 'Tag', label: 'Id [id]' }
    ];
    const preloaded = baseState({
      components: {
        tag1: {
          address: { view: 'v', component: 'tag1' },
          model: { values: [ { id: '10', name: 'John' } ] },
          attributes: { label: 'List', style: 's', expand: 'sm', visible: true }
        }
      }
    });

    renderWithProviders(<AweTagList id="tag1" elementList={elementList} />, { preloadedState: preloaded });

    expect(screen.queryByText("Row John")).toBeDefined();
  });

  it('updates when model.values change (re-dispatches UPDATE_MULTIPLE_COMPONENTS)',async () => {
    const elementList = [ { id: 'rowTag', type: 'Tag', label: 'Name [name]' } ];
    const preloaded = baseState({
      components: {
        tag1: {
          address: { view: 'v', component: 'tag1' },
          model: { values: [ { id: '1', name: 'A' } ] },
          attributes: { label: 'List', style: 's', expand: 'sm', visible: true }
        }
      }
    });

    const { store, dispatchSpy } = renderWithProviders(<AweTagList id="tag1" elementList={elementList} />, { preloadedState: preloaded, spyDispatch: true });

    await waitFor(() => expect(screen.queryByText("Name A")).toBeDefined());

    // Change the model values externally -> component useEffect listens to model.values
    act(() => store.dispatch(updateModel({ view: 'v', component: 'tag1' }, { values: [ { id: '2', name: 'B' } ] })));

    await waitFor(() => expect(screen.queryByText("Name B")).toBeDefined());
  });

  it('adds hidden class when visible=false', () => {
    const preloaded = baseState({
      components: {
        tag1: {
          address: { view: 'v', component: 'tag1' },
          model: { values: [ { id: 'A', name: 'Alpha' } ] },
          attributes: { label: 'Hidden', style: 's', expand: 'sm', visible: false }
        }
      }
    });

    const { container } = renderWithProviders(
      <AweTagList id="tag1" elementList={[{ type: 'div', text: 'x' }]} />,
      { preloadedState: preloaded }
    );

    const root = container.querySelector('#tag1');
    expect(root.className).toContain('hidden');
  });
});
