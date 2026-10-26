import type {Dispatch, ThunkAction, ThunkDispatch, UnknownAction} from "@reduxjs/toolkit";
import type {ComponentAddress} from "../contract";
import type {ComponentState} from "../utilities/mergeUtils";
import type {ActionItem} from "./actions/actions";
import type {Settings} from "./actions/settings";
import type createRootReducer from "./reducers";

/*
 * The types of the slices live here, and the reducers declare them as their return type: RootState is not written by
 * hand, it is what the root reducer returns.
 */

/** Actions slice: the sync stacks, the async actions and the running flag */
export interface ActionsState {
  running: boolean;
  sync: ActionItem[][];
  async: ActionItem[];
}

/** Components slice: the deltas of every component over its base state in the ComponentRegistry */
export type ComponentsState = Record<string, ComponentState>;

/** An option of the menu tree */
export interface MenuOption {
  name?: string;
  visible?: boolean;
  restricted?: boolean;
  allowed?: boolean;
  options?: MenuOption[];
  [key: string]: unknown;
}

/** Menu slice */
export interface MenuState {
  options: MenuOption[];
  breadcrumbs: Record<string, unknown>;
  status: {minimized: boolean; animating: boolean; resolution: string};
  selected?: Record<string, unknown>;
}

/** A message that is showing, a confirm or a popover */
export type MessageItem = Record<string, unknown>;

/** Messages slice */
export interface MessagesState {
  defined: Record<string, Record<string, unknown>>;
  showing: MessageItem[];
  confirm?: MessageItem;
  popover?: MessageItem;
}

/** Navigation slice */
export interface NavigationState {
  currentScreen: string | number | null;
}

/** The last event of a component, as the runtime reducer keeps it */
export interface RuntimeEvent {
  event: string;
  address: ComponentAddress;
  timestamp: number;
}

/** Runtime slice */
export interface RuntimeState {
  lastEvent: RuntimeEvent | null;
}

/** Screen slice: the data of the screen that is showing */
export type ScreenState = Record<string, unknown>;

/** Size slice: the sizes the layout has measured */
export type SizeState = Record<string, unknown>;

/** The data of a view */
export type ViewData = Record<string, unknown>;

/** View slice: the data of every view by name and the name of the current one */
export interface ViewState {
  view?: string;
  [name: string]: ViewData | string | undefined;
}

/** Settings slice */
export type SettingsState = Settings;

/** Whole state of the store, as `createRootReducer` combines it */
export type RootState = ReturnType<ReturnType<typeof createRootReducer>>;

/** Extra argument every thunk receives: the router `navigate` function, queued until the router is ready */
export interface ThunkExtra {
  navigate: (target: string | number, options?: Record<string, unknown>) => void;
}

/** Dispatch of the store: plain actions and thunks */
export type AppDispatch = ThunkDispatch<RootState, ThunkExtra, UnknownAction> & Dispatch<UnknownAction>;

/** A thunk of the store */
export type AppThunk<ReturnValue = void> = ThunkAction<ReturnValue, RootState, ThunkExtra, UnknownAction>;
