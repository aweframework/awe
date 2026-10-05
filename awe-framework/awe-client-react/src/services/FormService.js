import {asArray} from "../utilities";
import {deleteStack} from "../redux/actions/actions";
import {
  checkModelEmptyAction,
  checkModelNoUpdatedAction,
  checkModelUpdatedAction,
  setInvalidAction,
  setValidAction,
  validateAction,
  verifyValidationAction
} from "../redux/thunks/validate";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {
  endLoadAction,
  fillAction,
  filterAction, keepAction,
  resetAction,
  restoreAction, restoreTargetAction, selectAction, startLoadAction,
  submitAction,
  updateControllerAction
} from "../redux/thunks/form";
import {serverAction, serverDownloadAction} from "../redux/thunks/server";

function useFormService() {
  const dispatch = useDispatch();
  const {t} = useTranslation();

  const validate = (action) => {
    dispatch(validateAction(action));
  };

  const verifyValidation = (action) => {
    dispatch(verifyValidationAction(action));
  };

  const setValid = (action) => {
    dispatch(setValidAction(action));
  };

  const setInvalid = (action) => {
    dispatch(setInvalidAction(action));
  };

  const submit = (action) => {
    dispatch(submitAction(action));
  };

  const server = (action) => dispatch(serverAction(action));
  const serverPrint = (action) => dispatch(serverAction(action, true, t));
  const serverDownload = (action) => dispatch(serverDownloadAction(action));

  /**
   * Update model with action values
   * @param {object} action Action received
   */
  const fill = (action) => dispatch(fillAction(action));

  /**
   * Update controller with action values
   * @param {object} action Action received
   */
  const updateController = (action) => dispatch(updateControllerAction(action));

  /**
   * Update model with action values
   * @param {object} action Action received
   */
  const select = (action) => dispatch(selectAction(action));

  /**
   * Reset view selected values
   * @param {object} action
   */
  const reset = (action) => dispatch(resetAction(action));

  /**
   * Restore view selected values
   * @param {object} action
   */
  const restore = (action) => dispatch(restoreAction(action));

  /**
   * Restore view values loaded for the first time
   * @param {object} action
   */
  const restoreTarget = (action) => dispatch(restoreTargetAction(action));

  /**
   * Check if model has been modified
   * @param {object} action
   */
  const checkModelUpdated = (action) => dispatch(checkModelUpdatedAction(action));

  /**
   * Check if model hasn't been modified
   * @param {object} action
   */
  const checkModelNoUpdated = (action) => dispatch(checkModelNoUpdatedAction(action));

  /**
   * Check if model has empty data
   * @param {object} action
   */
  const checkModelEmpty = (action) => dispatch(checkModelEmptyAction(action));

  /**
   * Set a static value for an element
   * @param {object} action
   */
  const value = (action) => {
    // Retrieve parameters
    action.parameters.values = asArray(action.value);
    select(action);
  };

  /**
   * Cancel all actions of the current stack
   */
  const cancel = () => dispatch(deleteStack());

  /**
   * Filter a component data
   * @param {object} action Action received
   */
  const filter = (action) => dispatch(filterAction(action));

  /**
   * Start loading
   * @param {object} action Action received
   */
  const startLoad = (action) => dispatch(startLoadAction(action));

  /**
   * Finish loading
   * @param {object} action Action received
   */
  const endLoad = (action) => dispatch(endLoadAction(action));

  /**
   * Keep criteria values after initial initialization
   * @param {object} action Action received
   */
  const keep = (action) => dispatch(keepAction(action));

  const getActions = () => {
    return {
      "validate": validate,
      "submit": submit,
      "verify-validation": verifyValidation,
      "set-valid": setValid,
      "set-invalid": setInvalid,
      "server": server,
      "server-print": serverPrint,
      "server-download": serverDownload,
      "reset": reset,
      "restore": restore,
      "restore-target": restoreTarget,
      "fill": fill,
      "update-controller": updateController,
      "select": select,
      "cancel": cancel,
      "confirm-updated-data": checkModelUpdated,
      "confirm-not-updated-data": checkModelNoUpdated,
      "confirm-empty-data": checkModelEmpty,
      "value": value,
      "filter": filter,
      "start-load": startLoad,
      "end-load": endLoad,
      "keep": keep
    };
  };

  return {getActions};
}

export default useFormService;
