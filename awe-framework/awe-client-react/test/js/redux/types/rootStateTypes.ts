// Type-level assertions for the root state, checked by `npm run typecheck` (not run by Jest)
import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "../../../../src/redux/reducers";
import type {
  ActionsState,
  ComponentsState,
  MenuState,
  MessagesState,
  NavigationState,
  RootState,
  RuntimeState,
  ScreenState,
  SettingsState
} from "../../../../src/redux/types";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

// The root state is what the root reducer returns, and it is made of the slice types
type ReducedState = ReturnType<ReturnType<typeof createRootReducer>>;
assertType<Equal<RootState, ReducedState>>();
assertType<Equal<RootState, {
  actions: ActionsState;
  components: ComponentsState;
  menu: MenuState;
  messages: MessagesState;
  screen: ScreenState;
  settings: SettingsState;
  navigation: NavigationState;
  runtime: RuntimeState;
}>>();

// The root reducer reduces any action and is accepted by the store as it is, with no cast
const reducer = createRootReducer();
const next: RootState = reducer(undefined, {type: "ANY"});
const store = configureStore({reducer});
store.dispatch({type: "ANY"});

export {next};
