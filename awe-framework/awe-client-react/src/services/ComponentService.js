import {
  addPointsAction,
  addSeriesAction, changeMenuAction,
  clickButtonAction,
  closeDialogAction,
  closeDialogAndCancelAction,
  deleteUploadAction,
  finishUploadAction,
  goToFirstStepAction,
  goToLastStepAction,
  goToNextStepAction,
  goToNthStepAction,
  goToPrevStepAction,
  openDialogAction,
  removeSeriesAction,
  replaceSeriesAction,
  setPivotGroupColsAction,
  setPivotGroupRowsAction,
  setPivotSortersAction,
  toggleMenuAction,
  toggleNavbarAction,
} from "../redux/thunks/components";
import {useDispatch} from "react-redux";

/**
 * Component service (Functional version)
 * @category Services
 */
const useComponentService = () => {

  const dispatch = useDispatch();

  /**
   * Launch when click on button
   * @param {Action} action Action received
   */
  const onButtonClick = (action) => dispatch(clickButtonAction(action));

  /**
   * Launch when uploader has finished the upload process
   * @param {Action} action Action received
   */
  const onUploaderFinishUpload = (action) => dispatch(finishUploadAction(action));

  /**
   * Launch when uploader has deleted the uploaded file
   * @param {Action} action Action received
   */
  const onUploaderDelete = (action) => dispatch(deleteUploadAction(action));

  /**
   * Launch when a dialog is opened
   * @param {Action} action Action received
   */
  const onDialogOpen = (action) => dispatch(openDialogAction(action));

  /**
   * Launch when a dialog is closed
   * @param {Action} action Action received
   */
  const onDialogClose = (action) => dispatch(closeDialogAction(action));

  const onDialogCloseAndCancel = (action) => dispatch(closeDialogAndCancelAction(action));

  const onNextStep = (action) => dispatch(goToNextStepAction(action));

  const onPrevStep = (action) => dispatch(goToPrevStepAction(action));

  const onFirstStep = (action) => dispatch(goToFirstStepAction(action));

  const onLastStep = (action) => dispatch(goToLastStepAction(action));

  const onNthStep = (action) => dispatch(goToNthStepAction(action));

  const onAddPoints = (action) => dispatch(addPointsAction(action));

  const onAddSeries = (action) => dispatch(addSeriesAction(action));

  const onRemoveSeries = (action) => dispatch(removeSeriesAction(action));

  const onReplaceSeries = (action) => dispatch(replaceSeriesAction(action));

  const onSetPivotSorters = (action) => dispatch(setPivotSortersAction(action));

  const onSetPivotGroupRows = (action) => dispatch(setPivotGroupRowsAction(action));

  const onSetPivotGroupCols = (action) => dispatch(setPivotGroupColsAction(action));

  const toggleMenu = (action) => dispatch(toggleMenuAction(action));

  const toggleNavbar = (action) => dispatch(toggleNavbarAction(action));

  const changeMenu = (action) => dispatch(changeMenuAction(action));

  const getActions = () => ({
    // Button
    "click": onButtonClick,
    // Uploader
    "file-uploaded": onUploaderFinishUpload,
    "clear-file": onUploaderDelete,
    // Dialog
    "dialog": onDialogOpen,
    "close": onDialogClose,
    "close-cancel": onDialogCloseAndCancel,
    // Wizard
    "next-step": onNextStep,
    "prev-step": onPrevStep,
    "first-step": onFirstStep,
    "last-step": onLastStep,
    "nth-step": onNthStep,
    // Chart
    "add-points": onAddPoints,
    "add-chart-series": onAddSeries,
    "remove-chart-series": onRemoveSeries,
    "replace-chart-series": onReplaceSeries,
    // Pivot
    "set-pivot-sorters": onSetPivotSorters,
    "set-pivot-group-rows": onSetPivotGroupRows,
    "set-pivot-group-cols": onSetPivotGroupCols,
    // Menu
    "toggle-menu": toggleMenu,
    "toggle-navbar": toggleNavbar,
    "change-menu": changeMenu,
  });

  return {
    getActions,
  };
};

export default useComponentService;
