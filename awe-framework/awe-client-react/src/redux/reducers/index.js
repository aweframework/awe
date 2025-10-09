import {combineReducers} from "redux";

import {actions} from "./actionsReducer";
import {components} from "./componentsReducer";
import {menu} from "./menuReducer";
import {messages} from "./messagesReducer";
import {screen} from "./screenReducer";
import {settings} from "./settingsReducer";
import {size} from "./sizeReducer";
import {view} from "./viewReducer";
import {navigation} from "./navigationReducer";

export default () => combineReducers({actions, components, menu, messages, screen, settings, size, view, navigation});