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
import type {
  abortAction,
  acceptAction,
  ActionItem,
  addAction as addActionCreator,
  addActions,
  addActionsTop,
  addStack,
  closeAllActions,
  deleteStack as deleteStackCreator,
  rejectAction,
  removeAction as removeActionCreator,
  removeStack,
  runAction as runActionCreator,
  startAction,
  toggleActionsRunning as toggleActionsRunningCreator
} from '../actions/actions';
import type {ActionsState} from '../types';

const {STATUS_INITIAL, STATUS_STARTED, STATUS_RUNNING} = ActionStatus;

const InitialState: ActionsState = {
  running: false,
  sync: [[]],
  async: []
};

/**
 * Add a sync action
 * @param {object} state Current state
 * @param {object} action action to add
 * @return {object} next state
 */
function addActionSync(state: ActionsState, action: ActionItem): ActionsState {
  const {sync = [[]]} = state;
  const currentStackIndex = Math.max(sync.length - 1, 0);
  const currentStack = sync[currentStackIndex];
  return {
    ...state,
    sync: [
      ...sync.slice(0, currentStackIndex),
      [
        ...currentStack,
        {
          ...action,
          status: STATUS_INITIAL
        }
      ]
    ]
  };
}

/**
 * Add an async action
 * @param {object} state Current state
 * @param {object} action action to add
 * @return {object} next state
 */
function addActionAsync(state: ActionsState, action: ActionItem): ActionsState {
  return {
    ...state,
    async: [
      ...state.async,
      {
        ...action,
        status: STATUS_INITIAL
      }
    ]
  };
}

/**
 * Add an action in the right stack
 * @param state State
 * @param action Action
 * @return {object}
 */
function addAction(state: ActionsState, action: ActionItem): ActionsState {
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
function removeAction(state: ActionsState, action?: ActionItem): ActionsState {
  const {sync = [[]], async = []} = state;
  return runNext({
    ...state,
    sync: sync.map((stack) => stack.filter(a => a.id !== action?.id)),
    async: async.filter(a => a.id !== action?.id)
  });
}

/**
 * Remove all actions from current stack
 * @param {object} state Current state
 * @return {object} new state
 */
function deleteStack(state: ActionsState): ActionsState {
  const {sync = [[]]} = state;
  const currentStackIndex = Math.max(sync.length - 1, 0);
  return runNext({
    ...state,
    sync: [
      ...sync.slice(0, currentStackIndex),
      []
    ]
  });
}

/**
 * Set action attribute
 * @param action Action
 * @param actionId Action id
 * @param data Action data
 */
function setActionAttribute(
  action: ActionItem,
  actionId: string | undefined,
  data: Partial<ActionItem>
): ActionItem {
  if (action.id === actionId) {
    return {
      ...action,
      ...data
    };
  }
  return action;
}

/**
 * Set action as running
 * @param state State
 * @param actionId Action id
 * @param data Action data
 */
function changeActionAttribute(
  state: ActionsState,
  actionId: string | undefined,
  data: Partial<ActionItem>
): ActionsState {
  const {sync = [[]], async = []} = state;
  const currentStackIndex = Math.max(sync.length - 1, 0);
  const currentStack = sync[currentStackIndex];
  return {
    ...state,
    sync: [
      ...sync.slice(0, currentStackIndex),
      currentStack.map((actionInStack) => setActionAttribute(actionInStack, actionId, data))
    ],
    async: async.map((actionInStack) => setActionAttribute(actionInStack, actionId, data))
  };
}

/**
 * Run an action. Only a sync action that is not silent disables the screen, as in the AngularJS engine: a silent
 * action leaves the flag as it was and an async action runs in the background
 * @param state State
 * @param action Action to run
 * @return {object}
 */
function runAction(state: ActionsState, action: ActionItem): ActionsState {
  const nextState = action.silent || action.async ? state : toggleActionsRunning(state, true);
  return changeActionAttribute(nextState, action.id, {"status": STATUS_RUNNING});
}

/**
 * Run next action
 * @param state Current state
 * @return {object}
 */
function runNext(state: ActionsState): ActionsState {
  const {sync = [[]]} = state;
  const currentStackIndex = Math.max(sync.length - 1, 0);
  const currentStack = sync[currentStackIndex];
  if (currentStack.length === 0) {
    return toggleActionsRunning(state, false);
  } else {
    // Retrieve and launch action
    const action = currentStack[0];
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
function toggleActionsRunning(state: ActionsState, running: boolean): ActionsState {
  // Run the action
  if (state.running !== running) {
    return {...state, running: running};
  }

  return state;
}

/** Actions the reducer handles */
export type ActionsAction = ReturnType<
  typeof addActionCreator | typeof removeActionCreator | typeof addActions | typeof addActionsTop |
  typeof addStack | typeof removeStack | typeof deleteStackCreator | typeof runActionCreator |
  typeof startAction | typeof acceptAction | typeof rejectAction | typeof abortAction |
  typeof closeAllActions | typeof toggleActionsRunningCreator>;

let currentId = 1;

function getId(): number {
  return currentId++;
}

/**
 * Actions reducer
 * @param state Previous state
 * @param action Action
 * @returns new state
 */
// The default `{}` is the no-action of the JavaScript version: it matches no case, so the state is returned as it is
export function actions(
  state: ActionsState = InitialState,
  action: ActionsAction = {} as ActionsAction
): ActionsState {
  let currentStackIndex;
  let currentStack;
  let previousStackIndex;
  let previousStack;
  const {sync = [[]]} = state;

  switch (action.type) {
    case ADD_ACTION:
      return addAction(state, action.payload);
    case REMOVE_ACTION:
      return removeAction(state, action.payload);
    case ADD_ACTIONS:
      currentStackIndex = Math.max(sync.length - 1, 0);
      currentStack = sync[currentStackIndex];
      return runNext({
        ...state,
        sync: [
          ...sync.slice(0, currentStackIndex),
          [
            ...currentStack,
            ...action.payload.map(a => ({...a, id: "action-" + getId(), status: STATUS_INITIAL}))
          ]
        ]
      });
    case ADD_ACTIONS_TOP:
      currentStackIndex = Math.max(sync.length - 1, 0);
      currentStack = sync[currentStackIndex];
      return runNext({
        ...state,
        sync: [
          ...sync.slice(0, currentStackIndex),
          [
            ...action.payload.map(a => ({...a, id: "action-" + getId(), status: STATUS_INITIAL})),
            ...currentStack
          ]
        ]
      });
    case ADD_STACK:
      return runNext({
        ...state,
        sync: [
          ...sync,
          []
        ]
      });
    case REMOVE_STACK:
      previousStackIndex = Math.max(sync.length - 2, 0);
      previousStack = sync[previousStackIndex];
      currentStackIndex = Math.max(sync.length - 1, 0);
      currentStack = sync[currentStackIndex];
      if (currentStackIndex === 0) return state;
      return runNext({
        ...state,
        sync: [
          ...sync.slice(0, previousStackIndex),
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
      };
    default:
      return state;
  }
}
