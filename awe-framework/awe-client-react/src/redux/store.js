import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "./reducers";
import {DEFAULT_SETTINGS} from "./actions/settings";
import {createNavigator} from "./navigator";

/**
 * Create the application store
 * @param {{navigator: {navigate: function}}} [options] Navigator used by thunks to navigate
 * @returns {object} Store
 */
export const createStore = ({navigator = createNavigator()} = {}) => {
  return configureStore({
    reducer: createRootReducer(),
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        thunk: {extraArgument: {navigate: navigator.navigate}},
        immutableCheck: false,
        serializableCheck: false
      }),
    devTools: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
    },
  });
};
