import { SET_RUNTIME_EVENT, CLEAR_RUNTIME_EVENT } from '../actions/runtime';
import type { clearRuntimeEvent, setRuntimeEvent } from '../actions/runtime';
import type { RuntimeState } from '../types';

/** Actions the reducer handles */
export type RuntimeAction = ReturnType<typeof setRuntimeEvent | typeof clearRuntimeEvent>;

const initialState: RuntimeState = {
    lastEvent: null
};

/**
 * Action handlers map
 */
const actionHandlers = {
    [SET_RUNTIME_EVENT]: (state: RuntimeState, action: ReturnType<typeof setRuntimeEvent>): RuntimeState => ({
        ...state,
        lastEvent: {
            event: action.event,
            address: action.address,
            timestamp: action.timestamp
        }
    }),

    [CLEAR_RUNTIME_EVENT]: (state: RuntimeState): RuntimeState => ({
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
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as is
export function runtime(state: RuntimeState = initialState, action: RuntimeAction = {} as RuntimeAction): RuntimeState {
    // The handler of a type only receives the actions of that type: the lookup by a union loses that pairing
    const handler = actionHandlers[action.type] as
        ((state: RuntimeState, action: RuntimeAction) => RuntimeState) | undefined;
    return handler ? handler(state, action) : state;
}
