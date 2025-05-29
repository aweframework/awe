import {MessageType} from "../redux/actions/messages";
import {translateLabel} from "../utilities";

const {MESSAGE_OK, MESSAGE_WARNING, MESSAGE_ERROR, MESSAGE_WRONG} = MessageType;

/**
 * Translate incoming type
 * @param {String} incomingType Incoming type
 * @return {String} Translated type
 */
function getMessageType(incomingType) {
  // Translate class name
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
  const {messageTimeout} = settings;
  const {ok, info, error, warning} = messageTimeout;
  // Translate class name
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
  const {title, message, target} = parameters || {};
  // Retrieve title and parameters
  if (target) {
    // If message target in parameters, retrieve from message target
    return {...messages[target]};
  } else {
    return {title, message};
  }
}

/**
 * Message service
 * @category Services
 */
class MessageService {

  /**
   * Create the service
   */
  constructor(props) {
    this.message = this.message.bind(this);
    this.props = props;
    this.messageId = 0;
  }

  getActions() {
    return {
      "message": this.message.bind(this),
      "confirm": this.confirm.bind(this)
    }
  }

  /**
   * Generate a new message
   * @param {Object} action
   * @param {Object} props Properties
   */
  message(action, props) {
    const {type} = action.parameters;
    const {title, message} = getMessage(action.parameters, props.messages.defined[action.address?.view || action.view]);
    const {t} = props;
    const life = getMessageLife(type, this.props.settings);

    props.addMessage({
      severity: getMessageType(type),
      summary: translateLabel(title, t),
      detail: translateLabel(message, t),
      sticky: life === 0,
      action: action,
      life,
      id: this.messageId++,
      closable: true
    });
  }

  /**
   * Show modal confirm
   *
   * @param {object} action
   * @param {object} props
   */
  confirm(action, props) {
    const {title, message} = getMessage(action.parameters, props.messages.defined[action.address?.view || action.view]);
    props.confirmMessage({title, message, action});
  }
}

export default MessageService;
