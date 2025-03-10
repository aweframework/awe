import {
  ABORT_ACTION,
  ACCEPT_ACTION,
  ActionStatus,
  ADD_ACTION,
  ADD_ACTIONS,
  ADD_ACTIONS_TOP,
  ADD_STACK,
  CLOSE_ALL_ACTIONS,
  DELETE_STACK,
  REJECT_ACTION,
  REMOVE_ACTION,
  REMOVE_STACK,
  RUN_ACTION,
  START_ACTION,
  TOGGLE_ACTIONS_RUNNING
} from '../actions/actions';

const {STATUS_INITIAL, STATUS_STARTED, STATUS_RUNNING, STATUS_ACCEPTED, STATUS_REJECTED, STATUS_ABORTED} = ActionStatus;

/**
 * Add a sync action
 * @param {object} state Current state
 * @param {object} action action to add
 * @return {object} next state
 */
function addActionSync(state, action) {
  let currentStackIndex = Math.max(state.sync.length - 1, 0);
  let currentStack = state.sync[currentStackIndex];
  return {
    ...state,
    sync: [
      ...state.sync.slice(0, currentStackIndex),
      [
        ...currentStack,
        {
          ...action,
          status: STATUS_INITIAL
        }
      ]
    ]
  }
}

/**
 * Add an async action
 * @param {object} state Current state
 * @param {object} action action to add
 * @return {object} next state
 */
function addActionAsync(state, action) {
  return {
    ...state,
    async: [
      ...state.async,
      {
        ...action,
        status: STATUS_INITIAL
      }
    ]
  }
}

/**
 * Add an action in the right stack
 * @param state State
 * @param action Action
 * @return {object}
 */
function addAction(state, action) {
  if (action.async) {
    return addActionAsync(state, action);
  } else {
    return addActionSync(state, action);
  }
}

/**
 * Remove an async action
 * @param {object} state Current state
 * @param {object} action action to add
 * @return {object} next state
 */
function removeAction(state, action) {
  return runNext({
    ...state,
    sync: state.sync.map((stack) => stack.filter(a => a.id !== action.id)),
    async: state.async.filter(a => a.id !== action.id)
  });
}

/**
 * Remove all actions from current stack
 * @param {object} state Current state
 * @return {object} new state
 */
function deleteStack(state) {
  let currentStackIndex = Math.max(state.sync.length - 1, 0);
  return runNext({
    ...state,
    sync: [
      ...state.sync.slice(0, currentStackIndex),
      []
    ]
  })
}

/**
 * Set action attribute
 * @param action Action
 * @param actionId Action id
 * @param data Action data
 */
function setActionAttribute(action, actionId, data) {
  if (action.id === actionId) {
    return {
      ...action,
      ...data
    }
  }
  return action;
}

/**
 * Set action as running
 * @param state State
 * @param actionId Action id
 * @param data Action data
 */
function changeActionAttribute(state, actionId, data) {
  let currentStackIndex = Math.max(state.sync.length - 1, 0);
  let currentStack = state.sync[currentStackIndex];
  return {
    ...state,
    sync: [
      ...state.sync.slice(0, currentStackIndex),
      currentStack.map((actionInStack) => setActionAttribute(actionInStack, actionId, data))
    ],
    async: state.async.map((actionInStack) => setActionAttribute(actionInStack, actionId, data))
  };
}

/**
 * Run an action
 * @param state State
 * @param action Action to run
 * @return {object}
 */
function runAction(state, action) {
  return changeActionAttribute({...state, running: !action.silent}, action.id, {"status": STATUS_RUNNING});
}

/**
 * Run next action
 * @param state Current state
 * @return {object}
 */
function runNext(state) {
  let currentStackIndex = Math.max(state.sync.length - 1, 0);
  let currentStack = state.sync[currentStackIndex];
  if (currentStack.length === 0) {
    return toggleActionsRunning(state, false);
  } else {
    // Retrieve and launch action
    let action = currentStack[0];
    if (action.status === STATUS_INITIAL) {
      // If action is async, move the action to the other stack and call next action
      if (action.async) {
        // Remove from sync list
        let newState = removeAction(state, action);

        // Add to async list
        newState = addAction(newState, action);

        // Run action
        return runAction(newState, action);
      } else {
        // Run action
        return runAction(state, action);
      }
    }
  }

  return state;
}

/**
 * Set if actions are running
 * @public
 */
function toggleActionsRunning(state, running) {
  // Run the action
  if (state.running !== running) {
    return {...state, running: running};
  }

  return state;
}

let currentId = 1;

function getId() {
  return currentId++;
}

const InitialState = {
  running: false,
  sync: [[]],
  async: []
};

/**
 * Actions reducer
 * @param state Previous state
 * @param action Action
 * @returns new state
 */
export function actions(state = InitialState, action = {}) {
  let currentStackIndex;
  let currentStack
  switch (action.type) {
    case ADD_ACTION:
      return addAction(state, action.payload);
    case REMOVE_ACTION:
      return removeAction(state, action.payload);
    case ADD_ACTIONS:
      currentStackIndex = Math.max(state.sync.length - 1, 0);
      currentStack = state.sync[currentStackIndex];
      return runNext({
        ...state,
        sync: [
          ...state.sync.slice(0, currentStackIndex),
          [
            ...currentStack,
            ...action.payload.map(a => ({...a, id: "action-" + getId(), status: STATUS_INITIAL}))
          ]
        ]
      })
    case ADD_ACTIONS_TOP:
      currentStackIndex = Math.max(state.sync.length - 1, 0);
      currentStack = state.sync[currentStackIndex];
      return runNext({
        ...state,
        sync: [
          ...state.sync.slice(0, currentStackIndex),
          [
            ...action.payload.map(a => ({...a, id: "action-" + getId(), status: STATUS_INITIAL})),
            ...currentStack
          ]
        ]
      })
    case ADD_STACK:
      return runNext({
        ...state,
        sync: [
          ...state.sync,
          []
        ]
      });
    case REMOVE_STACK:
      let previousStackIndex = Math.max(state.sync.length - 2, 0);
      let previousStack = state.sync[previousStackIndex];
      currentStackIndex = Math.max(state.sync.length - 1, 0);
      currentStack = state.sync[currentStackIndex];
      if (currentStackIndex === 0) return state;
      return runNext({
        ...state,
        sync: [
          ...state.sync.slice(0, previousStackIndex),
          [
            ...currentStack,
            ...previousStack
          ]
        ]
      });
    case DELETE_STACK:
      return deleteStack(state);
    case RUN_ACTION:
      return runAction(state, action.payload);
    case START_ACTION:
      return changeActionAttribute(state, action.payload.id, {"status": STATUS_STARTED, "startDate": new Date().getDate()});
    case ACCEPT_ACTION:
      return removeAction(state, action.payload);
    case REJECT_ACTION:
      return deleteStack(state);
    case ABORT_ACTION:
      return removeAction(state, action.payload);
    case TOGGLE_ACTIONS_RUNNING:
      return toggleActionsRunning(state, action.running);
    case CLOSE_ALL_ACTIONS:
      return {
        ...state,
        running: false,
        sync: [[]],
        async: []
      }
    default:
      return state
  }
}
