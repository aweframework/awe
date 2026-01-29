import { combineReducers } from "redux";

import { actions } from "./actionsReducer";
import { components } from "./componentsReducer";
import { messages } from "./messagesReducer";
import { screen } from "./screenReducer";
import { settings } from "./settingsReducer";
import { navigation } from "./navigationReducer";
import { runtime } from "./runtimeReducer";

export default () => combineReducers({ actions, components, messages, screen, settings, navigation, runtime });
