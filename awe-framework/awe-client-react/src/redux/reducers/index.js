import { combineReducers } from "redux";

import { actions } from "./actionsReducer";
import { components } from "./componentsReducer";
import { menu } from "./menuReducer";
import { messages } from "./messagesReducer";
import { screen } from "./screenReducer";
import { settings } from "./settingsReducer";
import { navigation } from "./navigationReducer";
import { runtime } from "./runtimeReducer";

export default () => combineReducers({ actions, components, menu, messages, screen, settings, navigation, runtime });
