import {addMessage, confirmMessage, MessageType, showPopover} from "../actions/messages";
import {acceptAction} from "../actions/actions";
import {translateLabel} from "../../utilities";
import {findMessageTarget} from "../../utilities/messageTarget";

const { MESSAGE_OK, MESSAGE_WARNING, MESSAGE_ERROR, MESSAGE_WRONG } = MessageType;

let messageId = 0;

/**
 * Translate incoming type
 * @param {String} incomingType Incoming type
 * @return {String} Translated type
 */
function getMessageType(incomingType) {
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
function getMessageLife(incomingType, settings = {}) {
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
function getMessage(parameters, messages) {
  const { title, message, target } = parameters || {};
  if (target) {
    return { ...messages[target] };
  } else {
    return { title, message };
  }
}

export const messageAction = (action, t) => {
  return (dispatch, getState) => {
    const {settings, messages} = getState();
    const {type} = action.parameters;
    const {title, message} = getMessage(action.parameters, messages.defined[action.address?.view || action.view]);
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
export const targetMessageAction = (action, t) => {
  return (dispatch, getState) => {
    const address = action.callbackTarget || action.address;
    if (!findMessageTarget(address)) {
      return messageAction(action, t)(dispatch, getState);
    }

    const {settings, messages} = getState();
    const {type} = action.parameters;
    const {title, message} = getMessage(action.parameters, messages.defined[address.view || action.view]);
    if (messages.popover) {
      dispatch(acceptAction(messages.popover.action));
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
export const confirmAction = (action, t) => {
  return (dispatch, getState) => {
    const {messages} = getState();
    const {title, message} = getMessage(action.parameters, messages.defined[action.address?.view || action.view]);
    dispatch(confirmMessage({title: translateLabel(title, t), message: translateLabel(message, t), action}));
  };
};