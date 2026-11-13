// Type-level assertions for the leaf thunks, checked by `npm run typecheck` (not run by Jest)
import type {AppThunk} from "../../../../src/redux/types";
import type {Suggestion} from "../../../../src/utilities/suggest";
import type {ClientAction} from "../../../../src/redux/thunks/types";
import {isComponentAction, runActionOnTarget} from "../../../../src/redux/thunks/targets";
import {showContextMenuAction} from "../../../../src/redux/thunks/contextMenu";
import {resetGridLayout, saveGridLayout} from "../../../../src/redux/thunks/gridLayout";
import {serverAction, serverDownloadAction} from "../../../../src/redux/thunks/server";
import {initialSuggestAction, suggestAction} from "../../../../src/redux/thunks/suggest";
import {
  fetchFileContentAction,
  fetchHelpAction,
  fetchLogAction,
  fetchPdfAction
} from "../../../../src/redux/thunks/files";
import {syncRowDetailInstances} from "../../../../src/redux/thunks/rowDetail";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const action: ClientAction = {type: "click", address: {view: "report", component: "Button"}};
const address = {view: "report", component: "Grid"};

// Targets
assertType<Equal<ReturnType<typeof isComponentAction>, boolean>>();
assertType<Equal<ReturnType<typeof runActionOnTarget>, AppThunk>>();
runActionOnTarget(action, () => undefined);
// @ts-expect-error the action to run is a function
runActionOnTarget(action, "run");
// @ts-expect-error the type of an action is a string
isComponentAction(3);

// Context menu
assertType<Equal<ReturnType<typeof showContextMenuAction>, AppThunk>>();
// @ts-expect-error the action is an object
showContextMenuAction("click");

// Grid layout: the thunk says whether the service worked
assertType<Equal<ReturnType<typeof saveGridLayout>, AppThunk<Promise<boolean>>>>();
assertType<Equal<ReturnType<typeof resetGridLayout>, AppThunk<Promise<boolean>>>>();
saveGridLayout(address, "report", {order: ["a"], hidden: []});
saveGridLayout(address, "report", {order: ["a"], hidden: []}, {title: "Error", message: "Not saved"});
// @ts-expect-error the layout holds the order and the hidden columns
saveGridLayout(address, "report", {order: "a"});
// @ts-expect-error the screen is an identifier
resetGridLayout(address, 5);

// Server
assertType<Equal<ReturnType<typeof serverAction>, AppThunk<Promise<boolean>>>>();
assertType<Equal<ReturnType<typeof serverDownloadAction>, AppThunk>>();
serverAction({type: "server", parameters: {serverAction: "maintain", targetAction: "save"}});
serverAction({type: "server", parameters: {serverAction: "maintain", targetAction: "save"}}, true, (label) => label);
// @ts-expect-error a server action carries the server and the target action in its parameters
serverAction({type: "server"});
// @ts-expect-error the translator receives and returns a text
serverAction({type: "server", parameters: {serverAction: "a", targetAction: "b"}}, false, 3);

// Suggest
assertType<Equal<ReturnType<typeof suggestAction>, AppThunk<Promise<Suggestion[]>>>>();
assertType<Equal<ReturnType<typeof initialSuggestAction>, AppThunk<Promise<Suggestion[]>>>>();
suggestAction(null, "ab", {targetAction: "Cities", strict: false, signal: new AbortController().signal});
initialSuggestAction(["1", "2"], {address, serverAction: "data", targetAction: "Cities"});
initialSuggestAction("1");
// @ts-expect-error the target action is a text
suggestAction(null, "ab", {targetAction: 3});

// Files
assertType<Equal<ReturnType<typeof fetchLogAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof fetchPdfAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof fetchHelpAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof fetchFileContentAction>, AppThunk<Promise<false | void>>>>();
fetchLogAction("data", "Log", 0, () => undefined, () => undefined);
fetchHelpAction(() => undefined);
fetchHelpAction(() => undefined, "application-help");
// @ts-expect-error the setter of the text is a function
fetchLogAction("data", "Log", 0, "text", () => undefined);
// @ts-expect-error the setter of the pdf is a function
fetchPdfAction("Target");

// Row detail
assertType<Equal<ReturnType<typeof syncRowDetailInstances>, AppThunk>>();
syncRowDetailInstances(address);
syncRowDetailInstances(address, [{id: 1}, {id: "2"}]);
// @ts-expect-error the open rows are a list
syncRowDetailInstances(address, {id: 1});
