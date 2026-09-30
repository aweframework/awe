import { SET_RUNTIME_EVENT, CLEAR_RUNTIME_EVENT } from '../actions/runtime';

const initialState = {
    lastEvent: null
};

/**
 * Action handlers map
 */
const actionHandlers = {
    [SET_RUNTIME_EVENT]: (state, action) => ({
        ...state,
        lastEvent: {
            event: action.event,
            address: action.address,
            timestamp: action.timestamp
        }
    }),

    [CLEAR_RUNTIME_EVENT]: (state) => ({
        ...state,
        lastEvent: null
    })
};

/**
 * Runtime reducer
 * Manages runtime state like current event being processed
 * @param {Object} state - Current runtime state
 * @param {Object} action - Redux action
 * @returns {Object} New runtime state
 */
export function runtime(state = initialState, action = {}) {
    const handler = actionHandlers[action.type];
    return handler ? handler(state, action) : state;
}
