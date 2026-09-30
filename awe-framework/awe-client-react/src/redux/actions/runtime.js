// Runtime reducer action types
export const SET_RUNTIME_EVENT = 'SET_RUNTIME_EVENT';
export const CLEAR_RUNTIME_EVENT = 'CLEAR_RUNTIME_EVENT';

/**
 * Set runtime event
 * @param {Object} address - Component address that triggered the event
 * @param {string} event - Event type (e.g., "change", "blur", "focus")
 * @returns {Object} Redux action
 */
export function setRuntimeEvent(address, event) {
    return {
        type: SET_RUNTIME_EVENT,
        address,
        event,
        timestamp: Date.now()
    };
}

/**
 * Clear runtime event
 * @returns {Object} Redux action
 */
export function clearRuntimeEvent() {
    return {
        type: CLEAR_RUNTIME_EVENT
    };
}
