import {ADD_ACTIONS_TOP, addActionsTop} from '@awe-client/redux/actions/actions';
import {
  closeDialogAction,
  closeDialogAndCancelAction,
  goToFirstStepAction,
  goToLastStepAction,
  goToNextStepAction,
  goToNthStepAction,
  goToPrevStepAction,
  openDialogAction,
  updateModelWithDependencies,
} from '@awe-client/redux/thunks/components';
import {
  cancelRowGridAction,
  changeFilterGridAction,
  changePageGridAction,
  changeSortGridAction,
  editRowGridAction,
  saveRowGridAction,
  selectAllRowsGridAction,
  selectFirstRowGridAction,
  selectLastRowGridAction,
  selectRowGridAction,
  toggleBranchGridAction,
  unselectAllRowsGridAction,
  afterCancelRowGridAction,
  afterSaveRowGridAction,
} from '@awe-client/redux/thunks/grid';
import {selectAction} from '@awe-client/redux/thunks/form';
import {showContextMenuAction} from '@awe-client/redux/thunks/contextMenu';
import {confirmAction, messageAction, targetMessageAction} from '@awe-client/redux/thunks/messages';
import {answerActions, routeActions, runKind} from './clientActions';

// The value action of the client gives the component the values of the action as its selection
const valueAction = item => selectAction({...item, parameters: {...item.parameters, values: [item.value ?? item.parameters?.value]}});

// The thunk of the client that runs each action: the same ones its actions container uses (those that translate
// get the translate function of the example). The keys are the CLIENT_ACTIONS of clientActions.js
const THUNKS = {
  'dialog': openDialogAction,
  'close': closeDialogAction,
  'close-cancel': closeDialogAndCancelAction,
  'next-step': goToNextStepAction,
  'prev-step': goToPrevStepAction,
  'first-step': goToFirstStepAction,
  'last-step': goToLastStepAction,
  'nth-step': goToNthStepAction,
  'select-row': selectRowGridAction,
  'select-first-row': selectFirstRowGridAction,
  'select-last-row': selectLastRowGridAction,
  'select-all-rows': selectAllRowsGridAction,
  'unselect-all-rows': unselectAllRowsGridAction,
  'change-page': changePageGridAction,
  'change-sort': changeSortGridAction,
  'change-filter': changeFilterGridAction,
  'tree-branch': toggleBranchGridAction,
  'edit-row': editRowGridAction,
  'save-row': saveRowGridAction,
  'cancel-row': cancelRowGridAction,
  'after-save-row': afterSaveRowGridAction,
  'after-cancel-row': afterCancelRowGridAction,
  'confirm': confirmAction,
  'message': messageAction,
  'target-message': targetMessageAction,
  'show-context-menu': showContextMenuAction,
  'value': valueAction,
};

/**
 * Store middleware that runs the client-only actions of the stack (dialogs, wizard steps, messages, context menus and
 * the client side of grids, see clientActions.js) and lets the rest (server actions, validations...) go on to the
 * stack, where nothing consumes them. What is run and what goes on is decided by routeActions, which node:test covers.
 * A server action of a target action that has an answer is not sent: the actions of its answer run instead, as if the
 * server had sent them.
 * @param {{t?: function(string): string, answers?: Object<string, Array>}} [options] Translate function of the example
 *   (the messages show translated texts) and the answers of the example to the server actions, by target action
 */
export const createClientActionsMiddleware = ({t = key => key, answers} = {}) => store => next => action => {
  const {run, forward} = routeActions(action, ADD_ACTIONS_TOP, answers);
  run.forEach(item => {
    const kind = runKind(item, answers);
    const thunk = THUNKS[item.type];
    if (kind === 'answer') {
      // The answer of the example: its actions go on the stack, which this middleware sees again
      store.dispatch(addActionsTop(answerActions(item, answers)));
    } else if (kind === 'delete-file') {
      // The delete of an uploaded file: the uploader empties its value
      store.dispatch(updateModelWithDependencies(item.address, {values: []}));
    } else if (thunk) {
      store.dispatch(thunk(item, t));
    } else {
      // A client action without thunk would otherwise break the dispatch of the whole stack
      console.warn(`The live examples have no thunk for the action ${item.type}`);
    }
  });
  return forward ? next(forward) : undefined;
};
