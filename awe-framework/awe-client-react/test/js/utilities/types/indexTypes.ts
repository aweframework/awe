// Type-level assertions for utilities (index), checked by `npm run typecheck` (not run by Jest)
import type {ReactElement} from "react";
import type {AppDispatch} from "../../../../src/redux/types";
import {
  addEventListenerTimeout,
  asArray,
  bindMethods,
  checkActions,
  componentValue,
  evaluateExpression,
  extractIcon,
  fetchAction,
  fetchFile,
  fetchHtml,
  fetchJson,
  fetchScreen,
  formatMessage,
  formule,
  generateAddress,
  generateMessageAction,
  generateServerAction,
  getActionAddress,
  getActionSource,
  getBaseHref,
  getComponent,
  getComponentValue,
  getContextPath,
  getCookie,
  getDataDependingOnList,
  getGridAndValues,
  getGroupSelectedValues,
  getHelpTooltipNode,
  getIconCode,
  getRestUrl,
  getSelectedValues,
  getSizeString,
  getVisibleTextData,
  isInsideContext,
  parseBoolean,
  replaceWildcard,
  translateLabel,
  updateArrayElement
} from "../../../../src/utilities";

declare const dispatch: AppDispatch;
const t = (key: string) => key;

// The REST url joins the segments that are not empty
const url: string = getRestUrl("action", undefined, "save");
// @ts-expect-error the url is text
const wrongUrl: number = getRestUrl("a");

// The address always has a view and a component; the column and the row are optional
const address = generateAddress("report", "Grd");
const cellAddress = generateAddress("report", "Grd", "col", "7");
const view: string = address.view;
const component: string = address.component;
const column: string | undefined = cellAddress.column;
// @ts-expect-error the view of an address is text
const wrongView: number = address.view;
// @ts-expect-error the view is required
generateAddress();
// @ts-expect-error the row is text
generateAddress("report", "Grd", "col", 7);

// A value is managed as a list that keeps the type of its items
const numbers: number[] = asArray(1);
const strings: string[] = asArray(["a", "b"]);
const nothing: string[] = asArray(undefined as string | undefined);
// @ts-expect-error the items are numbers
const wrongItems: string[] = asArray(1);

// The value of a list is null, the item or the list
const single: number | number[] | null = componentValue([1, 2]);
// @ts-expect-error the value may be null or a list
const definite: number = componentValue([1]);
// @ts-expect-error a list is needed
componentValue(1);

// Expressions evaluate to anything
const evaluated: unknown = evaluateExpression("1 + 1");
const formuled: unknown = formule("[a] + 1", {a: 1});
// @ts-expect-error the expression is text
evaluateExpression(1);
// @ts-expect-error the values of a formule are an object
formule("[a]", "a");

// The wildcards are replaced in a text, null when there is no text
const replaced: string | null = replaceWildcard("Hello [name]", {name: "Ada"});
const replacedNothing: string | null = replaceWildcard(undefined, null);
// @ts-expect-error the result may be null
const replacedDefinite: string = replaceWildcard("a", {});

// The running actions are checked with the handlers each type has
const checked: void = checkActions(
  {message: (action, props) => props.dispatch(action as never)},
  {prevRunningActions: [], runningActions: [{type: "message"}]},
  {settings: {actionsStack: 0}, dispatch}
);
// @ts-expect-error the running actions are a list of actions
checkActions({}, {runningActions: "message"}, {settings: {}, dispatch});
// @ts-expect-error the props have a dispatch
checkActions({}, {runningActions: []}, {settings: {}});

// A message keeps its text, and is empty without error
const message: string | undefined = formatMessage({message: "ERR", values: {a: 1}}, t);
const noMessage: string | undefined = formatMessage(undefined, t);
// @ts-expect-error the message may be undefined
const messageDefinite: string = formatMessage(null, t);

// The values of a component are the ones that are selected
const selected: unknown[] = getSelectedValues({model: {values: [{value: 1, selected: true}]}});
const last: unknown = getComponentValue({model: {values: [{value: 1, selected: true}]}});
const noValues: unknown[] = getSelectedValues(undefined);
const group: unknown[] = getGroupSelectedValues(
  {attributes: {group: "g"}},
  {A: {attributes: {group: "g"}, model: {values: [{value: 1, selected: true}]}}}
);
// @ts-expect-error a component has attributes
getGroupSelectedValues({}, {});
// @ts-expect-error the selected values are a list
const wrongSelected: string = getSelectedValues(undefined);

// The context and the source are checked by view
const inside: boolean = isInsideContext({view: "report", source: ["a", "b"]}, "report", "a");
// @ts-expect-error the context has a source
isInsideContext({view: "report"}, "report", "a");
const source: string | undefined = getActionSource({target: "Btn"}, {});
const callerSource: string | undefined = getActionSource(
  {address: {component: "Btn"}},
  {Btn: {context: {source: ["a", "b"]}}}
);
// @ts-expect-error the source may be undefined
const sourceDefinite: string = getActionSource({target: "Btn"}, {});

// The cookie is text or undefined
const cookie: string | undefined = getCookie("XSRF-TOKEN");
// @ts-expect-error a cookie may not exist
const cookieDefinite: string = getCookie("XSRF-TOKEN");

// The requests answer with the data the caller expects, or with the error
const action: Promise<unknown> = fetchAction("server", "target", {a: 1}, "token");
const typed: Promise<{ok: boolean} | {message: string; status: number}> = fetchJson<{ok: boolean}>("POST", "/x", {}, "token");
const screen: Promise<unknown> = fetchScreen("option", "view", "token", {a: 1});
const file: Promise<unknown> = fetchFile("/file", {}, "token");
const html: Promise<string> = fetchHtml("/html", "token");
// @ts-expect-error the html is text
const wrongHtml: Promise<number> = fetchHtml("/html", "token");
// @ts-expect-error a method is text
fetchJson(1, "/x", {}, "token");

// The context path and the base are text
const contextPath: string = getContextPath();
const baseHref: string = getBaseHref();

// The methods bound are methods of the object
const service = {run() {return this;}, name: "service"};
const bound: void = bindMethods(service, ["run"]);
// @ts-expect-error the name is not a method
bindMethods(service, ["name"]);
// @ts-expect-error the method does not exist
bindMethods(service, ["missing"]);

// The server action and the message action are described
const server = generateServerAction({a: 1}, "save", "target", address, true, false, {serverActionKey: "serverAction", targetActionKey: "targetAction"});
const serverType: "server" = server.type;
const serverAddress: string = server.address.view;
const serverDefault = generateServerAction({}, "save", "target", address);
const messageAction = generateMessageAction("warning", "Title", "Body");
const defaultMessage: "message" = generateMessageAction().type;
const messageTitle: string | null = messageAction.parameters.title;
// @ts-expect-error the type of the server action is a literal
const wrongServerType: "message" = server.type;
// @ts-expect-error the address is required
generateServerAction({}, "save", "target");

// The size is text
const size: string = getSizeString(2048);
// @ts-expect-error the size is a number
getSizeString("2048");

// The components are found by address
const components = {Btn: {address: {view: "report", component: "Btn"}, model: {values: [{a: 1}]}}};
const found: (typeof components)["Btn"] | undefined = getComponent(components, {view: "report", component: "Btn"});
const grid = getGridAndValues(components, {view: "report", component: "Btn"});
const gridValues: {a: number}[] | undefined = grid.values;
// @ts-expect-error the component may not be found
const foundDefinite: (typeof components)["Btn"] = getComponent(components, {view: "report"});
const actionAddress = getActionAddress({target: "Btn", address: {view: "report"}});
const actionAddressView: string | undefined = actionAddress.view;

// The icon has a name and a family, and its code is an element
const icon: {name: string; family: string} = extractIcon("mdi:home");
const iconCode: ReactElement | null = getIconCode("mdi:home", "extra");
const noIcon: ReactElement | null = getIconCode(undefined);
// @ts-expect-error the icon code is not text
const wrongIconCode: string = getIconCode("fa-home");

// The listener is added to a node
const listener: void = addEventListenerTimeout(document.body, "click", () => undefined, () => undefined, 100);
// @ts-expect-error the timeout is a number
addEventListenerTimeout(document.body, "click", () => undefined, () => undefined, "100");

// The help tooltip is an element, or nothing
const tooltip: ReactElement | null = getHelpTooltipNode("help", "image.png", t, ".target");
// @ts-expect-error the translator is a function
getHelpTooltipNode("help", undefined, "t", ".target");

// The booleans and the texts are parsed
const parsed: boolean = parseBoolean("true");
const parsedBoolean: boolean = parseBoolean(true);
const parsedDefault: boolean = parseBoolean();
// @ts-expect-error a boolean is parsed from text or boolean
parseBoolean(1);
const visible: string = getVisibleTextData("label", t);
const translated: string = translateLabel("label", t);

// The array is updated with the changes of an element, keeping the type of its items
const updated: {a: number; b: string}[] = updateArrayElement([{a: 1, b: "x"}], 0, {a: 2});
// @ts-expect-error the changes are properties of the item
updateArrayElement([{a: 1, b: "x"}], 0, {c: 2});

// The data depends on the list length
const data: number | number[] | null = getDataDependingOnList([1, 2]);
// @ts-expect-error the data may be null or a list
const dataDefinite: number = getDataDependingOnList([1]);

export {url, wrongUrl, view, component, column, wrongView, numbers, strings, nothing, wrongItems, single, definite};
export {evaluated, formuled, replaced, replacedNothing, replacedDefinite, checked, message, noMessage, messageDefinite};
export {selected, last, noValues, group, wrongSelected, inside, source, callerSource, sourceDefinite, cookie};
export {cookieDefinite, action, typed, screen, file, html, wrongHtml, contextPath, baseHref, bound, server};
export {serverType, serverAddress, serverDefault, messageAction, defaultMessage, messageTitle, wrongServerType};
export {size, found, gridValues, foundDefinite, actionAddressView, icon, iconCode, noIcon, wrongIconCode};
export {listener, tooltip, parsed, parsedBoolean, parsedDefault, visible, translated, updated, data, dataDefinite};
