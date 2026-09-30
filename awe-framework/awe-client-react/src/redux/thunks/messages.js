import {addMessage, confirmMessage, MessageType, showMessages as showMessagesAction} from "../actions/messages";
import {translateLabel} from "../../utilities";

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

// Show modal confirm
export const confirmAction = (action, t) => {
  return (dispatch, getState) => {
    const {messages} = getState();
    const {title, message} = getMessage(action.parameters, messages.defined[action.address?.view || action.view]);
    dispatch(confirmMessage({title: translateLabel(title, t), message: translateLabel(message, t), action}));
  };
};