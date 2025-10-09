import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "./reducers";
import {DEFAULT_SETTINGS} from "./actions/settings";

let latestNavigate = null;

export const setNavigateFn = (navigate) => {
  latestNavigate = navigate;
};

export const createStore = () => {
  return configureStore({
    reducer: createRootReducer(),
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        thunk: {extraArgument: { get navigate() {return latestNavigate;}}},
        immutableCheck: false,
        serializableCheck: false
      }),
    devTools: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
    },
  });
};