import {
  ADD_MESSAGE,
  CONFIRM_MESSAGE,
  REMOVE_CONFIRM,
  REMOVE_MESSAGE,
  SHOW_MESSAGES,
  UPDATE_MESSAGES
} from '../actions/messages';

const InitialState = {
  defined: {
    base: {},
    report: {}
  },
  showing: []
};

/**
 * Messages reducer
 * @param state Old state
 * @param action Action
 * @returns New state
 */
export function messages(state = InitialState, action = {}) {
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
    case REMOVE_CONFIRM:
      return {
        ...state,
        confirm: undefined
      };
    default:
      return state;
  }
}
