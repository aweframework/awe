// Type-level assertions for the slice reducers, checked by `npm run typecheck` (not run by Jest)
import {actions} from "../../../../src/redux/reducers/actionsReducer";
import {menu} from "../../../../src/redux/reducers/menuReducer";
import {messages} from "../../../../src/redux/reducers/messagesReducer";
import {navigation} from "../../../../src/redux/reducers/navigationReducer";
import {runtime} from "../../../../src/redux/reducers/runtimeReducer";
import {screen} from "../../../../src/redux/reducers/screenReducer";
import {settings} from "../../../../src/redux/reducers/settingsReducer";
import {size} from "../../../../src/redux/reducers/sizeReducer";
import {view} from "../../../../src/redux/reducers/viewReducer";
import {addActions} from "../../../../src/redux/actions/actions";
import {updateMenu, updateStatus} from "../../../../src/redux/actions/menu";
import {addMessage, removePopover} from "../../../../src/redux/actions/messages";
import {setRuntimeEvent} from "../../../../src/redux/actions/runtime";
import {updateScreen} from "../../../../src/redux/actions/screen";
import {updateSettings} from "../../../../src/redux/actions/settings";
import {updateSize} from "../../../../src/redux/actions/size";
import {setView} from "../../../../src/redux/actions/view";
import type {
  ActionsState,
  MenuState,
  MessagesState,
  NavigationState,
  RuntimeState,
  ScreenState,
  SettingsState,
  SizeState,
  ViewState
} from "../../../../src/redux/types";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

// Every reducer returns exactly the type of its slice, from an undefined state and from a state
assertType<Equal<ReturnType<typeof actions>, ActionsState>>();
assertType<Equal<ReturnType<typeof menu>, MenuState>>();
assertType<Equal<ReturnType<typeof messages>, MessagesState>>();
assertType<Equal<ReturnType<typeof navigation>, NavigationState>>();
assertType<Equal<ReturnType<typeof runtime>, RuntimeState>>();
assertType<Equal<ReturnType<typeof screen>, ScreenState>>();
assertType<Equal<ReturnType<typeof settings>, SettingsState>>();
assertType<Equal<ReturnType<typeof size>, SizeState>>();
assertType<Equal<ReturnType<typeof view>, ViewState>>();

// The reducers take the typed actions of their slice
const sliceActions: ActionsState = actions(undefined, addActions([{id: "a"}]));
const sliceMenu: MenuState = menu(undefined, updateMenu([{name: "home"}]));
const sliceStatus: MenuState = menu(sliceMenu, updateStatus({minimized: true}));
const sliceMessages: MessagesState = messages(undefined, addMessage({id: 1}));
const slicePopover: MessagesState = messages(sliceMessages, removePopover({id: 1}));
const sliceNavigation: NavigationState = navigation(undefined, {type: "NAVIGATE_TO", payload: "home"});
const sliceRuntime: RuntimeState = runtime(undefined, setRuntimeEvent({view: "base", component: "id"}, "change"));
const sliceScreen: ScreenState = screen(undefined, updateScreen({a: 1}));
const sliceSettings: SettingsState = settings(undefined, updateSettings({language: "es"}));
const sliceSize: SizeState = size(undefined, updateSize({width: 1}));
const sliceView: ViewState = view(undefined, setView("base", {loading: false}));

// The reducers reject the actions of other slices and malformed ones
// @ts-expect-error a menu action is not an actions action
actions(undefined, updateMenu([]));
// @ts-expect-error the menu data is an option array
menu(undefined, {type: "UPDATE_MENU", data: 1});
// @ts-expect-error a message action is not a menu action
menu(undefined, addMessage({id: 1}));
// @ts-expect-error the target of a navigation is a screen or a position
navigation(undefined, {type: "NAVIGATE_TO", payload: {}});
// @ts-expect-error a settings action does not touch the screen slice
screen(undefined, updateSettings({}));
// @ts-expect-error the settings are known
settings(undefined, updateSettings({unknownSetting: true}));

export {
  sliceActions, sliceMenu, sliceStatus, sliceMessages, slicePopover, sliceNavigation, sliceRuntime, sliceScreen,
  sliceSettings, sliceSize, sliceView
};
