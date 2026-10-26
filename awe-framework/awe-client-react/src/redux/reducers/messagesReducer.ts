import {
  ADD_MESSAGE,
  CONFIRM_MESSAGE,
  REMOVE_CONFIRM,
  REMOVE_MESSAGE,
  REMOVE_POPOVER,
  SHOW_MESSAGES,
  SHOW_POPOVER,
  UPDATE_MESSAGES
} from '../actions/messages';
import type {
  addMessage,
  confirmMessage,
  removeConfirm,
  removeMessage,
  removePopover,
  showMessages,
  showPopover,
  updateMessages
} from '../actions/messages';
import type {MessagesState} from '../types';

const InitialState: MessagesState = {
  defined: {
    base: {},
    report: {}
  },
  showing: []
};

/** Actions the reducer handles */
export type MessagesAction = ReturnType<
  typeof addMessage | typeof showMessages | typeof removeMessage | typeof updateMessages | typeof confirmMessage |
  typeof removeConfirm | typeof showPopover | typeof removePopover>;

/**
 * Messages reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function messages(
  state: MessagesState = InitialState,
  action: MessagesAction = {} as MessagesAction
): MessagesState {
  switch (action.type) {
    case ADD_MESSAGE:
      return {
        ...state,
        showing: [
          ...state.showing,
          {...action.data}
        ]
      };
    case SHOW_MESSAGES:
      return {
        ...state,
        showing: state.showing.map(message => action.data.includes(message) ? {...message, show: true} : message)
      };
    case REMOVE_MESSAGE:
      return {
        ...state,
        showing: state.showing.filter(message => message.id !== action.data.id)
      };
    case UPDATE_MESSAGES:
      return {
        ...state,
        defined: {
          ...state.defined,
          [action.view]: {
            ...action.data
          }
        }
      };
    case CONFIRM_MESSAGE:
      return {
        ...state,
        confirm: {
          ...action.data
        }
      };
    // A target message shows one popover at a time: a new one replaces the previous one
    case SHOW_POPOVER:
      return {
        ...state,
        popover: {...action.data}
      };
    case REMOVE_POPOVER:
      return state.popover?.id === action.data?.id ? {...state, popover: undefined} : state;
    case REMOVE_CONFIRM:
      return {
        ...state,
        confirm: undefined
      };
    default:
      return state;
  }
}
