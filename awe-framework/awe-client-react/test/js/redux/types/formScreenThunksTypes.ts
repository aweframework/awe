// Type-level assertions for the form and screen thunks, checked by `npm run typecheck` (not run by Jest)
import type {ComponentState} from "../../../../src/utilities/mergeUtils";
import {DEFAULT_SETTINGS} from "../../../../src/redux/actions/settings";
import type {AppThunk} from "../../../../src/redux/types";
import type {ClientAction} from "../../../../src/redux/thunks/types";
import {
  endLoadAction,
  fillAction,
  fillSuggestAction,
  filterAction,
  keepAction,
  resetAction,
  restoreAction,
  restoreTargetAction,
  selectAction,
  startLoadAction,
  submitAction,
  updateControllerAction
} from "../../../../src/redux/thunks/form";
import {
  backAction,
  buildComponent,
  changeLanguageAction,
  changeThemeAction,
  getFileAction,
  loadScreen,
  logoutAction,
  redirectAction,
  reloadScreenAction,
  screenAction
} from "../../../../src/redux/thunks/screen";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const action: ClientAction = {type: "filter", address: {view: "report", component: "Grid"}};
const t = (label: string) => label;

// Form thunks work on an action and finish it
assertType<Equal<ReturnType<typeof submitAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof resetAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof restoreAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof restoreTargetAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof filterAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof fillAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof fillSuggestAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof updateControllerAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof selectAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof startLoadAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof endLoadAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof keepAction>, AppThunk>>();
fillAction({...action, parameters: {datalist: {rows: [{value: 1, label: "One"}], page: 1}}});
selectAction({...action, parameters: {values: ["a", {value: "b"}]}});
updateControllerAction({...action, parameters: {attribute: "label", value: "Name"}});
// @ts-expect-error the action is an object
submitAction("submit");
// @ts-expect-error the action needs its type
filterAction({address: {view: "report", component: "Grid"}});
// @ts-expect-error the address of an action is an object
fillAction({type: "fill", address: "Grid"});

// Screen
assertType<Equal<ReturnType<typeof buildComponent>, ComponentState>>();
assertType<Equal<ReturnType<typeof loadScreen>, AppThunk<Promise<void>>>>();
assertType<Equal<ReturnType<typeof screenAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof reloadScreenAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof backAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeLanguageAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof changeThemeAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof getFileAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof logoutAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof redirectAction>, AppThunk>>();
buildComponent("report", {id: "Criteria", controller: {name: "Criteria"}, model: {values: []}}, DEFAULT_SETTINGS);
buildComponent("report", {id: "Criteria", controller: {}, model: {values: []}}, DEFAULT_SETTINGS, ["Panel"]);
loadScreen("report", "option", t);
screenAction({type: "screen", target: "/home"}, "/current");
screenAction({type: "screen", target: "/home"}, "/current", "/current?page=2");
reloadScreenAction(action, "/current");
// @ts-expect-error a component of the screen has a model with values
buildComponent("report", {id: "Criteria", controller: {}, model: {}}, DEFAULT_SETTINGS);
// @ts-expect-error the view is a text
loadScreen(1, "option", t);
// @ts-expect-error the path of the current screen is needed
screenAction({type: "screen"});
// @ts-expect-error the path is a text
reloadScreenAction(action, 3);
// @ts-expect-error the logout takes no action
logoutAction(action);
