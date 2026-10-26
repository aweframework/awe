import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "./reducers";
import {DEFAULT_SETTINGS} from "./actions/settings";
import {createNavigator} from "./navigator";
import type {Navigator} from "./navigator";
import type {ThunkExtra} from "./types";

/**
 * Create the application store
 * @param options Navigator used by thunks to navigate
 * @returns Store
 */
export const createStore = ({navigator = createNavigator()}: {navigator?: Navigator} = {}) => {
  return configureStore({
    reducer: createRootReducer(),
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        thunk: {extraArgument: {navigate: navigator.navigate} satisfies ThunkExtra},
        immutableCheck: false,
        serializableCheck: false
      }),
    devTools: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
    },
  });
};
