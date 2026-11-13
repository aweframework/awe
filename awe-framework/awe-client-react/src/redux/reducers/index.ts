import { combineReducers } from "@reduxjs/toolkit";
import type { Action, Reducer, UnknownAction } from "@reduxjs/toolkit";

import { actions } from "./actionsReducer";
import { components } from "./componentsReducer";
import { menu } from "./menuReducer";
import { messages } from "./messagesReducer";
import { screen } from "./screenReducer";
import { settings } from "./settingsReducer";
import { navigation } from "./navigationReducer";
import { runtime } from "./runtimeReducer";

/**
 * Slice reducers declare the actions they handle (a union their `switch` narrows), but the store dispatches any
 * action and a reducer that does not handle one returns its state: this is that contract, seen from the store.
 * @param reducer Slice reducer
 * @returns The same reducer, typed as accepting any action
 */
const forAnyAction = <S, A extends Action>(reducer: (state?: S, action?: A) => S) =>
  reducer as Reducer<S, UnknownAction>;

export default () =>
  combineReducers({
    actions: forAnyAction(actions),
    components: forAnyAction(components),
    menu: forAnyAction(menu),
    messages: forAnyAction(messages),
    screen: forAnyAction(screen),
    settings: forAnyAction(settings),
    navigation: forAnyAction(navigation),
    runtime: forAnyAction(runtime)
  });
