import {createStore} from "../../../src/redux/store";
import {createNavigator} from "../../../src/redux/navigator";
import {navigationActions} from "../../../src/redux/actions/navigation";

describe('awe-react-client/test/js/redux/storeTest.js', () => {
  it('does not lose a navigation dispatched before the router is ready', () => {
    const navigator = createNavigator();
    const store = createStore({navigator});
    const navigate = jest.fn();

    // The router effect of main.jsx has not run yet
    store.dispatch(navigationActions.navigateTo("/screen/private/home", {replace: true}));
    expect(navigate).not.toHaveBeenCalled();

    navigator.attach(navigate);

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith("/screen/private/home", {replace: true});
  });

  it('navigates right away once the router is ready', () => {
    const navigator = createNavigator();
    const store = createStore({navigator});
    const navigate = jest.fn();
    navigator.attach(navigate);

    store.dispatch(navigationActions.navigateTo(-1));

    expect(navigate).toHaveBeenCalledWith(-1, {});
  });

  it('flushes the queued navigations in the order they were requested, only once', () => {
    const navigator = createNavigator();
    const navigate = jest.fn();
    navigator.navigate("/first", {});
    navigator.navigate("/second", {});

    navigator.attach(navigate);
    navigator.attach(navigate);

    expect(navigate.mock.calls).toEqual([["/first", {}], ["/second", {}]]);
  });

  it('keeps every store with its own router', () => {
    const first = createNavigator();
    const second = createNavigator();
    const navigate = jest.fn();
    first.attach(navigate);

    createStore({navigator: second}).dispatch(navigationActions.navigateTo("/other"));

    expect(navigate).not.toHaveBeenCalled();
  });

  it('creates its own navigator when none is given', () => {
    expect(() => createStore().dispatch(navigationActions.navigateTo("/x"))).not.toThrow();
  });
});
