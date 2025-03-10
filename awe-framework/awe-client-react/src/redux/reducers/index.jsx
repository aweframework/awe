import {combineReducers} from "redux";
import { connectRouter } from 'connected-react-router';

import {actions} from "./actionsReducer";
import {components} from "./componentsReducer";
import {menu} from "./menuReducer";
import {messages} from "./messagesReducer";
import {screen} from "./screenReducer";
import {settings} from "./settingsReducer";

export default (history) => combineReducers({actions, components, menu, messages, screen, settings, router: connectRouter(history)});