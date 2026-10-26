import {createStore} from '../../../src/redux/store';
import type {RootState} from '../../../src/redux/types';

// RootState is declared by hand because the reducers are JavaScript (src/redux/types.ts). This test keeps it honest:
// the key list below must match RootState at compile time, and the store the reducers build must match it at runtime.
const SLICES: Record<keyof RootState, true> = {
  actions: true, components: true, menu: true, messages: true, screen: true, settings: true, navigation: true, runtime: true
};

describe('awe-react-client/test/js/redux/storeShapeTest.ts', () => {
  const state = createStore().getState() as unknown as Record<string, unknown>;

  it('has exactly the slices declared by RootState', () => {
    expect(Object.keys(state).sort()).toEqual(Object.keys(SLICES).sort());
  });

  it('builds every slice with the shape RootState declares', () => {
    const {actions, components, menu, messages, screen, settings, navigation, runtime} = state as unknown as RootState;
    expect(actions).toEqual(expect.objectContaining({running: expect.any(Boolean), sync: expect.any(Array), async: expect.any(Array)}));
    expect(components).toEqual(expect.any(Object));
    expect(menu).toEqual(expect.objectContaining({
      options: expect.any(Array),
      breadcrumbs: expect.any(Object),
      status: expect.objectContaining({minimized: expect.any(Boolean), animating: expect.any(Boolean), resolution: expect.any(String)})
    }));
    expect(messages).toEqual(expect.objectContaining({defined: expect.any(Object), showing: expect.any(Array)}));
    expect(screen).toEqual(expect.any(Object));
    expect(settings).toEqual(expect.any(Object));
    expect(navigation).toHaveProperty('currentScreen');
    expect(runtime).toEqual(expect.objectContaining({lastEvent: null}));
  });
});
