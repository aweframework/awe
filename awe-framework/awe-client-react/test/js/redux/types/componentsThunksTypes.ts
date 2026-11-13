// Type-level assertions for the components thunks, checked by `npm run typecheck` (not run by Jest)
import type {ComponentState} from "../../../../src/utilities/mergeUtils";
import type {AppThunk} from "../../../../src/redux/types";
import type {ClientAction} from "../../../../src/redux/thunks/types";
import {
  addPointsAction,
  addSeriesAction,
  changeMenuAction,
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
  polylineAction,
  raiseEventOnDependencies,
  removeSeriesAction,
  replaceSeriesAction,
  resetModelWithDependencies,
  resetMultipleModelWithDependencies,
  restoreModelWithDependencies,
  restoreMultipleModelWithDependencies,
  setPivotGroupColsAction,
  setPivotGroupRowsAction,
  setPivotSortersAction,
  toggleMenuAction,
  toggleNavbarAction,
  updateAttributesWithDependencies,
  updateModelWithDependencies,
  updateMultipleComponentsWithDependencies,
  updateMultipleModelsWithDependencies,
  updateViewComponentsWithDependencies
} from "../../../../src/redux/thunks/components";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const address = {view: "report", component: "Grid"};
const action: ClientAction = {type: "click", address};

// Every thunk of the components works on an address or an action and returns a thunk of the store
assertType<Equal<ReturnType<typeof updateViewComponentsWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateMultipleComponentsWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateMultipleModelsWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateModelWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof raiseEventOnDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateAttributesWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof restoreModelWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof restoreMultipleModelWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof resetModelWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof resetMultipleModelWithDependencies>, AppThunk>>();
assertType<Equal<ReturnType<typeof clickButtonAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof finishUploadAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof deleteUploadAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof openDialogAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof closeDialogAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof closeDialogAndCancelAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof goToNextStepAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof goToPrevStepAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof goToFirstStepAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof goToLastStepAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof goToNthStepAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof addPointsAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof polylineAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof addSeriesAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof removeSeriesAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof replaceSeriesAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof setPivotSortersAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof setPivotGroupRowsAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof setPivotGroupColsAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof toggleMenuAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof toggleNavbarAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeMenuAction>, AppThunk>>();

// Model and attribute changes: the data may carry the event to raise
const component: ComponentState = {address, model: {values: []}};
updateViewComponentsWithDependencies("report", {Grid: component});
updateMultipleComponentsWithDependencies([{address, data: {values: []}}]);
updateMultipleModelsWithDependencies([{address, data: {values: []}}]);
updateModelWithDependencies(address, {values: [{value: 1}]});
updateModelWithDependencies(address, {event: "open"});
updateAttributesWithDependencies(address, {visible: false, event: "change"});
restoreModelWithDependencies(address);
restoreMultipleModelWithDependencies([{address}], true);
resetModelWithDependencies(address);
resetMultipleModelWithDependencies([{address}]);
raiseEventOnDependencies(address, "change");
// @ts-expect-error the event is a text
updateModelWithDependencies(address, {event: 1});
// @ts-expect-error the event is a text
updateAttributesWithDependencies(address, {event: true});
// @ts-expect-error the address is an object
updateModelWithDependencies("Grid", {values: []});
// @ts-expect-error a list of components is an array
updateMultipleModelsWithDependencies({address});
// @ts-expect-error the components of a list have an address
resetMultipleModelWithDependencies([{data: {}}]);
// @ts-expect-error the view is a text
updateViewComponentsWithDependencies(1, {});
// @ts-expect-error the event to raise is needed
raiseEventOnDependencies(address);

// Actions
clickButtonAction(action);
goToNthStepAction({...action, parameters: {value: "two"}});
addSeriesAction({...action, parameters: {series: [{id: "serie", type: "column", data: [[1, 2]]}]}});
changeMenuAction({...action, parameters: {options: [{name: "home", visible: true}]}});
// @ts-expect-error the action is an object
clickButtonAction("click");
// @ts-expect-error the action needs its type
openDialogAction({address});
// @ts-expect-error the address of an action is an object
polylineAction({type: "polyline", address: "Map"});
// @ts-expect-error the toggle of the navbar takes the action
toggleNavbarAction();
