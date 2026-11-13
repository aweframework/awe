// Type-level assertions for the websocket, messages, deferred and validate thunks, checked by `npm run typecheck`
import type {Client} from "@stomp/stompjs";
import type {AppThunk} from "../../../../src/redux/types";
import type {ClientAction} from "../../../../src/redux/thunks/types";
import {connectWebSocketAction, disconnectWebSocketAction} from "../../../../src/redux/thunks/websocket";
import {confirmAction, messageAction, targetMessageAction} from "../../../../src/redux/thunks/messages";
import {
  deferredFailedAction,
  initialModelAction,
  isInPendingDeferredContainer,
  loadDeferredAction
} from "../../../../src/redux/thunks/deferred";
import {
  checkModelEmptyAction,
  checkModelNoUpdatedAction,
  checkModelUpdatedAction,
  revealFirstInvalidAction,
  setInvalidAction,
  setValidAction,
  validateAction,
  validateComponents,
  validateRow,
  verifyValidationAction
} from "../../../../src/redux/thunks/validate";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const action: ClientAction = {type: "validate", address: {view: "report", component: "Criteria"}};
const message = {type: "message", parameters: {type: "ok", title: "Title", message: "Text"}};
const address = {view: "report", component: "Dialog"};
const t = (label: string) => label;

// Websocket: the client is created by the connection and given back by the disconnection
assertType<Equal<ReturnType<typeof connectWebSocketAction>, AppThunk<Client>>>();
assertType<Equal<ReturnType<typeof disconnectWebSocketAction>, AppThunk<Client | null | undefined>>>();
connectWebSocketAction({type: "connectWebsocket", parameters: {token: "token"}}, t);
disconnectWebSocketAction({type: "disconnectWebsocket"}, undefined);
// @ts-expect-error the translator is a function
connectWebSocketAction(action, "t");
// @ts-expect-error the client is a stomp client
disconnectWebSocketAction(action, {deactivate: () => undefined});

// Messages
assertType<Equal<ReturnType<typeof messageAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof targetMessageAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof confirmAction>, AppThunk>>();
messageAction(message, t);
targetMessageAction({...message, callbackTarget: address}, t);
confirmAction(message, t);
// @ts-expect-error a message action carries the type of the message in its parameters
messageAction({type: "message"}, t);
// @ts-expect-error the type of a message is a text
messageAction({type: "message", parameters: {type: 3}}, t);

// Deferred containers
assertType<Equal<ReturnType<typeof isInPendingDeferredContainer>, boolean>>();
assertType<Equal<ReturnType<typeof loadDeferredAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof deferredFailedAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof initialModelAction>, AppThunk>>();
isInPendingDeferredContainer({address}, {Dialog: {address, attributes: {deferred: true}}});
loadDeferredAction(address);
// @ts-expect-error the container is an address
loadDeferredAction("Dialog");
// @ts-expect-error the component is an object
isInPendingDeferredContainer("Dialog", {});

// Validation
assertType<Equal<ReturnType<typeof revealFirstInvalidAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof validateComponents>, AppThunk>>();
assertType<Equal<ReturnType<typeof validateRow>, AppThunk>>();
assertType<Equal<ReturnType<typeof validateAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof verifyValidationAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof setValidAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof setInvalidAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkModelUpdatedAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkModelNoUpdatedAction>, AppThunk>>();
assertType<Equal<ReturnType<typeof checkModelEmptyAction>, AppThunk>>();
validateComponents([{address, attributes: {}}]);
validateRow(address);
validateAction(action);
// @ts-expect-error the components to validate are a list
validateComponents({address});
// @ts-expect-error the address of the row is an object
validateRow("row");
// @ts-expect-error the action is an object
setValidAction("Criteria");
