import {addMessage, confirmMessage, MessageType, showPopover, showSigninAlert} from "../actions/messages";
import {acceptAction} from "../actions/actions";
import type {ActionItem} from "../actions/actions";
import type {Settings} from "../actions/settings";
import type {ComponentAddress} from "../../contract";
import {getComponent, translateLabel} from "../../utilities";
import {findMessageTarget} from "../../utilities/messageTarget";
import {inSigninPage, launchesServerAction, LOGIN_ACTION} from "../../utilities/signin";
import {getAllComponents} from "../selectors/componentSelectors";
import type {AppThunk, MessagesState, RootState} from "../types";
import type {ClientAction, Translate} from "./types";

/** The title and the text of a message */
interface MessageText {
  title?: string;
  message?: string;
}

/** An action that shows a message: its parameters give the type and the title and text, or the predefined message */
interface MessageClientAction extends ClientAction {
  parameters: {type: string; title?: string; message?: string; target?: string};
  callbackTarget?: ComponentAddress;
}

const { MESSAGE_OK, MESSAGE_WARNING, MESSAGE_ERROR, MESSAGE_WRONG } = MessageType;

let messageId = 0;

/**
 * Translate incoming type
 * @param {String} incomingType Incoming type
 * @return {String} Translated type
 */
function getMessageType(incomingType: string): string {
  switch (incomingType) {
    case MESSAGE_ERROR:
    case MESSAGE_WRONG:
      return "error";
    case MESSAGE_OK:
      return "success";
    case MESSAGE_WARNING:
      return "warn";
    default:
      return incomingType;
  }
}

/**
 * Translate message life
 * @param {String} incomingType Incoming type
 * @param {Object} settings Application settings
 * @return {Integer} Translated lifetime
 */
function getMessageLife(
  incomingType: string,
  // A message is always shown with the settings: without them reading the timeouts fails, as it always did
  settings: Pick<Settings, "messageTimeout"> = {} as Pick<Settings, "messageTimeout">
): number {
  const { messageTimeout } = settings;
  const { ok, info, error, warning } = messageTimeout;
  switch (incomingType) {
    case MESSAGE_ERROR:
    case MESSAGE_WRONG:
      return error;
    case MESSAGE_OK:
      return ok;
    case MESSAGE_WARNING:
      return warning;
    default:
      return info;
  }
}

/**
 * Retrieve message from action or predefined message
 * @param parameters Action parameters
 * @param messages Predefined messages
 * @return {{title: *, message: *}}
 */
function getMessage(parameters: MessageClientAction["parameters"], messages: Record<string, unknown>): MessageText {
  const { title, message, target } = parameters || {} as MessageClientAction["parameters"];
  if (target) {
    return { ...messages[target] as MessageText };
  } else {
    return { title, message };
  }
}

/**
 * Predefined messages of a view
 * @param {object} messages Messages of the state
 * @param {string} view View of the message action, the view of its address or its own one
 * @return {object} Predefined messages by target
 */
const getDefinedMessages = (messages: MessagesState, view: string | undefined): Record<string, unknown> =>
  messages.defined[view as string];

/**
 * Check whether a message is the answer of a failed login on the sign-in page: a message of the button that launches
 * the login server action, drawn inside the sign-in page. An answer that goes on to another screen (the change of an
 * expired password) keeps its toast, which survives the change of screen
 * @param action Message action
 * @param state State
 * @return The sign-in page shows the message in its alert
 */
function isSigninLoginMessage(action: MessageClientAction, state: RootState): boolean {
  const address = action.address;
  if (!address || !state.components || !inSigninPage(findMessageTarget(address))) {
    return false;
  }
  // The actions of a component are in its registered base, not in the changes the state keeps: merge both
  const button = getComponent(getAllComponents(state), address);
  if (!launchesServerAction(button?.actions, LOGIN_ACTION)) {
    return false;
  }
  const stack = state.actions?.sync?.at(-1) ?? [];
  return !stack.some(pending => pending.type === "screen" && pending.id !== action.id);
}

export const messageAction = (action: MessageClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const {settings, messages} = getState();
    const {type} = action.parameters;
    const {title, message} = getMessage(action.parameters, getDefinedMessages(messages, action.address?.view || action.view));

    // On the sign-in page a failed login is shown in the alert of the form, with the text of the server as it is
    if (isSigninLoginMessage(action, getState())) {
      dispatch(showSigninAlert({id: messageId, severity: getMessageType(type), summary: title, detail: message}));
      messageId++;
      dispatch(acceptAction(action as ActionItem));
      return;
    }

    const life = getMessageLife(type, settings);

    dispatch(addMessage({
      severity: getMessageType(type),
      summary: translateLabel(title, t),
      detail: translateLabel(message, t),
      sticky: life === 0,
      action,
      life,
      id: messageId,
      closable: true
    }));

    messageId++;
  };
};

/**
 * Show a message over the target component of the action (a popover), as the AngularJS client does. The target is the
 * callback target of the action or, in this client, its address (the server thunk gives every response action the
 * address of the component that launched the call). A new popover replaces the previous one, whose action finishes.
 * Without a rendered target the message is shown as a notification.
 * @param {object} action Target message action
 * @param {function} t Translate function
 */
export const targetMessageAction = (action: MessageClientAction, t: Translate): AppThunk => {
  return (dispatch, getState, extra) => {
    const address = action.callbackTarget || action.address;
    if (!findMessageTarget(address)) {
      return messageAction(action, t)(dispatch, getState, extra);
    }

    const {settings, messages} = getState();
    const {type} = action.parameters;
    // The address is there: without it no target is found
    const {title, message} = getMessage(action.parameters, getDefinedMessages(messages, (address as ComponentAddress).view || action.view));
    if (messages.popover) {
      dispatch(acceptAction(messages.popover.action as ActionItem));
    }

    dispatch(showPopover({
      id: messageId,
      severity: getMessageType(type),
      summary: translateLabel(title, t),
      detail: translateLabel(message, t),
      life: getMessageLife(type, settings),
      address,
      action
    }));

    messageId++;
  };
};

// Show modal confirm
export const confirmAction = (action: MessageClientAction, t: Translate): AppThunk => {
  return (dispatch, getState) => {
    const {messages} = getState();
    const {title, message} = getMessage(action.parameters, getDefinedMessages(messages, action.address?.view || action.view));
    dispatch(confirmMessage({title: translateLabel(title, t), message: translateLabel(message, t), action}));
  };
};