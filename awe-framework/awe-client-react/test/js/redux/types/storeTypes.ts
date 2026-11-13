// Type-level assertions for the redux store types, checked by `npm run typecheck` (not run by Jest)
import {createStore} from "../../../../src/redux/store";
import type {AppDispatch, AppThunk, RootState, ThunkExtra} from "../../../../src/redux/types";

const store = createStore();

// The store state is the root state and its slices are known
const state: RootState = store.getState();
const running: boolean = state.actions.running;
const language: string | null = state.settings.language;
const minimized: boolean = state.menu.status.minimized;
const current: string | number | null = state.navigation.currentScreen;
// @ts-expect-error the running flag is not a number
const wrongRunning: number = state.actions.running;
// @ts-expect-error there is no such slice
const unknownSlice = state.unknown;

// A component of the state has the parts of the component state
const model: Record<string, unknown> | undefined = state.components["id"]?.model;

// The store dispatches plain actions and thunks
const dispatch: AppDispatch = store.dispatch;
dispatch({type: "ANY"});
dispatch((innerDispatch, getState, {navigate}) => {
  const inner: RootState = getState();
  navigate("/path", {replace: true});
  innerDispatch({type: "ANY"});
  return inner;
});

// The extra argument of the thunks is the navigate function of the router
const extra: ThunkExtra = {navigate: () => undefined};
// @ts-expect-error navigate is required
const noNavigate: ThunkExtra = {};

// A thunk returns what its type says
const thunk: AppThunk<number> = () => 1;
const result: number = dispatch(thunk);
// @ts-expect-error the thunk returns a number
const wrongThunk: AppThunk<string> = () => 1;

export {unknownSlice, state, running, language, minimized, current, wrongRunning, model, extra, noNavigate, result, wrongThunk};
