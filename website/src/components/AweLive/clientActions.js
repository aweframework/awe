/**
 * Client actions the examples run for real. The docs have no actions container, so nothing consumes the stack the
 * client dispatches to: a button that opens a dialog or moves a wizard would do nothing. These are the actions that
 * only change the client (no server): AweLiveRoot runs them with the thunks of the client itself.
 * Pure on purpose (no JSX, no client import) so that node:test can run it.
 */
const CLIENT_ACTIONS = new Set([
  // Dialogs and wizards
  'dialog', 'close', 'close-cancel',
  'next-step', 'prev-step', 'first-step', 'last-step', 'nth-step',
  // Grids and tree grids with `load-all`: they select, page, sort and edit their rows on the client. The ones that
  // reload from the server (`filter`) and the row validation are never raised by such a grid
  'select-row', 'select-first-row', 'select-last-row', 'select-all-rows', 'unselect-all-rows',
  'change-page', 'change-sort', 'change-filter', 'tree-branch',
  'edit-row', 'save-row', 'cancel-row', 'after-save-row', 'after-cancel-row',
  // Messages: the confirm dialog of a predefined message, and the toasts and popovers a server answer sends
  'confirm', 'message', 'target-message',
  // The context menu of a button, and the static value an option or a button gives a component
  'show-context-menu', 'value',
]);

// The server actions the examples answer: removing the file of an uploader, which clears its value
const isDeleteFile = item => item?.type === 'server' && item.parameters?.serverAction === 'delete-file';

// ... and the ones of a target action the example gives an answer for ({target action: [actions]})
const isAnswered = (item, answers) => item?.type === 'server' && Object.hasOwn(answers ?? {}, item.parameters?.targetAction);

/**
 * The actions a server action gets as an answer. As the server does, each one runs in the context of the action that
 * asked (its address) unless it carries its own, which completes that one.
 * @param {{address?: Object, parameters?: {targetAction?: string}}} item Server action
 * @param {Object<string, Array>} [answers] Actions of the answer by target action
 * @returns {Array} Actions to run (none without an answer)
 */
function answerActions(item, answers) {
  // An answered server action inside an answer would be answered again by the middleware, for ever: it is dropped
  const answer = (answers?.[item?.parameters?.targetAction] ?? []).filter(action => !isAnswered(action, answers));
  return answer.map(action => ({ ...action, address: { ...item.address, ...action.address } }));
}

/**
 * How the middleware runs a handled action: `answer` (the actions of the answer of the example go on the stack),
 * `delete-file` (the uploader empties its value) or `thunk` (the thunk of the client for its type).
 * @param {{type: string}} item Action the examples handle
 * @param {Object<string, Array>} [answers] Answers of the example to the server actions, by target action
 * @returns {'answer'|'delete-file'|'thunk'} Way to run it
 */
function runKind(item, answers) {
  if (isAnswered(item, answers)) return 'answer';
  return isDeleteFile(item) ? 'delete-file' : 'thunk';
}

/**
 * Splits a list of actions into the ones the examples run (`handled`) and the rest, keeping the order of each.
 * @param {Array<{type: string}>} [items] Actions of the stack
 * @param {Object<string, Array>} [answers] Answers of the example to the server actions, by target action
 * @returns {{handled: Array, rest: Array}} The actions the examples run and the ones nobody handles
 */
function partitionActions(items, answers) {
  const handled = [];
  const rest = [];
  for (const item of items ?? []) {
    (CLIENT_ACTIONS.has(item?.type) || isDeleteFile(item) || isAnswered(item, answers) ? handled : rest).push(item);
  }
  return { handled, rest };
}

/**
 * Decides what the store does with a redux action. A push of actions to the stack is split: the simulated ones run and
 * the rest go on to the stack, where nothing consumes them. Any other action goes on untouched.
 * @param {{type: string, actions?: Array, payload?: Array}} [action] Redux action
 * @param {string} stackType Type of the redux action that pushes actions to the top of the stack
 * @param {Object<string, Array>} [answers] Answers of the example to the server actions, by target action
 * @returns {{run: Array, forward: (Object|undefined)}} Actions to run and the redux action to pass on (none: nothing is left)
 */
function routeActions(action, stackType, answers) {
  if (action?.type !== stackType) {
    return { run: [], forward: action };
  }
  const { handled, rest } = partitionActions(action.payload, answers);
  if (handled.length === 0) {
    return { run: [], forward: action };
  }
  return { run: handled, forward: rest.length > 0 ? { ...action, actions: rest, payload: rest } : undefined };
}

module.exports = { CLIENT_ACTIONS, runKind, answerActions, partitionActions, routeActions };
