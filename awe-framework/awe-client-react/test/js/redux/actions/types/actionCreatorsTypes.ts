// Type-level assertions for the redux action creators, checked by `npm run typecheck` (not run by Jest)
import * as actions from "../../../../../src/redux/actions/actions";
import * as components from "../../../../../src/redux/actions/components";
import * as menu from "../../../../../src/redux/actions/menu";
import * as messages from "../../../../../src/redux/actions/messages";
import {navigationActions} from "../../../../../src/redux/actions/navigation";
import * as runtime from "../../../../../src/redux/actions/runtime";
import {updateScreen} from "../../../../../src/redux/actions/screen";
import {DEFAULT_SETTINGS, Settings, updateSettings} from "../../../../../src/redux/actions/settings";
import {updateSize} from "../../../../../src/redux/actions/size";
import * as view from "../../../../../src/redux/actions/view";
import type {ComponentAddress} from "../../../../../src/contract";
import type {AppThunk} from "../../../../../src/redux/types";

// The type of an action is its literal constant, so a reducer can narrow on it
const added: {type: "ADD_ACTION"; payload: actions.ActionItem} = actions.addAction({id: "a"});
const top: {type: "ADD_ACTIONS_TOP"; actions: actions.ActionItem[]; payload: actions.ActionItem[]} = actions.addActionsTop([{id: "a"}]);
const stack: {type: "ADD_STACK"} = actions.addStack();
const toggled: {type: "TOGGLE_ACTIONS_RUNNING"; running: boolean} = actions.toggleActionsRunning(true);
// @ts-expect-error the running flag is a boolean
actions.toggleActionsRunning("yes");
// @ts-expect-error a list of actions is needed
actions.addActions({id: "a"});
const status: actions.ActionStatusValue = actions.ActionStatus.STATUS_RUNNING;
// @ts-expect-error there is no such status
const unknownStatus: actions.ActionStatusValue = "STATUS_UNKNOWN";
actions.delayActions(100, () => undefined);
actions.delayActions(undefined, () => undefined);
// @ts-expect-error the function to launch is needed
actions.delayActions(100);

// Component actions carry the address of the component
const address = {view: "report", component: "Grd"};
const model: {type: "UPDATE_MODEL"; address: ComponentAddress; data: Record<string, unknown>} = components.updateModel(address, {value: 1});
const kept: {type: "KEEP_MODEL"} = components.keepModel(address);
const restored: {type: "RESTORE_MODEL"; initial: boolean} = components.restoreModel(address, {});
const cleared: {type: "CLEAR_COMPONENTS"; view: string} = components.clearComponents("report");
const list: {type: "RESET_MULTIPLE_MODEL"} = components.resetMultipleModel([{address, data: {}}]);
// @ts-expect-error the address is not a component address
components.updateModel({unknown: "x"}, {});
// @ts-expect-error the view is text
components.clearComponents(1);
const componentStatus: "defined" | "compiled" | "initialized" = components.ComponentStatus.STATUS_DEFINED;
const buttonType: "button" | "reset" | "submit" = components.ButtonTypes.BUTTON_RESET;

// The other action modules
const menuUpdated: {type: "UPDATE_MENU"} = menu.updateMenu([{name: "home", options: []}]);
const crumbs: {type: "UPDATE_BREADCRUMBS"; items: unknown[]} = menu.updateBreadcrumbs("home", []);
const popover: {type: "SHOW_POPOVER"} = messages.showPopover({id: "a"});
const messageType: "ok" | "warning" | "error" | "info" | "wrong" = messages.MessageType.MESSAGE_OK;
const event: {type: "SET_RUNTIME_EVENT"; event: string; timestamp: number} = runtime.setRuntimeEvent(address, "change");
// @ts-expect-error the event is text
runtime.setRuntimeEvent(address, 1);
const viewSet: {type: "SET_SCREEN_VIEW"; view: string} = view.setView("report", {});
const viewCleared: {type: "CLEAR_ALL_VIEWS"} = view.clearAllViews();
const screenUpdated: {type: "UPDATE_SCREEN"} = updateScreen({});
const sizeUpdated: {type: "UPDATE_SIZE"} = updateSize({});

// Settings are typed from their defaults
const settings: Settings = DEFAULT_SETTINGS;
const settingsUpdated: {type: "UPDATE_SETTINGS"; payload: Partial<Settings>} = updateSettings({theme: "dark"});
// @ts-expect-error the page size is a number
updateSettings({recordsPerPage: "10"});
const language: string | null = DEFAULT_SETTINGS.language;

// navigateTo is a thunk of the store
const navigation: AppThunk = navigationActions.navigateTo("/home", {replace: true});
navigationActions.navigateTo(-1);
// @ts-expect-error the target is a path or a history delta
navigationActions.navigateTo(true);

export {added, top, stack, toggled, status, unknownStatus, model, kept, restored, cleared, list, componentStatus, buttonType};
export {menuUpdated, crumbs, popover, messageType, event, viewSet, viewCleared, screenUpdated, sizeUpdated, settings};
export {settingsUpdated, language, navigation};
