// Type-level assertions for utilities/components, checked by `npm run typecheck` (not run by Jest)
import type {SyntheticEvent} from "react";
import {
  ComponentAddressType,
  UploadStatus,
  checkModelIsEmpty,
  checkModelIsUnchanged,
  checkModelIsUpdated,
  classNames,
  clickDropdown,
  fixController,
  fixDefaultModel,
  fixModel,
  fixSelectedModel,
  getAddressType,
  getCheckboxData,
  getComponentData,
  getComponentId,
  getComponentIdentifierKey,
  getComponentTraceContext,
  getCriterionData,
  getCriterionDataAsList,
  getCriterionPrintData,
  getDateRangeData,
  getDependencyComponentId,
  getSelectedValues,
  getSpecificAttributes,
  getTabData,
  getTabPrintData,
  getTriggerId,
  inspectComponentStructure,
  isGridComponent,
  isTopLevelFormComponent,
  isVoidElement,
  parseRule,
  parseValidationRules,
  warnMalformedComponent
} from "../../../../src/utilities/components";

const address = {view: "report", component: "Grd"};
const model = {values: [{value: 1, label: "One", selected: true}, {value: 2, label: "Two", selected: false}]};
const criterion = {attributes: {id: "Cri", component: "text"}, address};
const props = {t: (key: string) => key};
const settings = {recordsPerPage: 30, defaultComponentSize: "md", numericOptions: {precision: 2}};

// The literals of the upload status and the address type are checked
const uploaded: "uploaded" = UploadStatus.UPLOADED;
const cell: "cell" = ComponentAddressType.ADDRESS_CELL;
// @ts-expect-error an upload status is a literal, not any text
const wrongStatus: "initial" = UploadStatus.UPLOADED;
// @ts-expect-error there is no such address type
const unknownAddress = ComponentAddressType.ADDRESS_OTHER;

// The checks answer with a boolean
const isVoid: boolean = isVoidElement("br");
const isGrid: boolean = isGridComponent({attributes: {columnModel: []}});
const noGrid: boolean = isGridComponent(undefined);
const identifierKey: string = getComponentIdentifierKey({attributes: {treegrid: true, treeId: "node"}});
const topLevel: boolean = isTopLevelFormComponent({address}, {origin: "test", operation: "check"});
const malformed: false = warnMalformedComponent({}, {origin: "test"});
// @ts-expect-error a component has an address
isTopLevelFormComponent({address: "report"});
// @ts-expect-error the tag is not a number
const wrongVoid: number = isVoidElement("br");

// The trace of a component is indexed by text
const trace: Record<string, unknown> = getComponentTraceContext({address, attributes: {id: "Grd"}}, {componentKey: "Grd", extra: {a: 1}});
const emptyTrace: Record<string, unknown> = getComponentTraceContext();
// @ts-expect-error the address of a component is an object
getComponentTraceContext({address: "report"});

// The rules are parsed as an object
const rule: Record<string, unknown> = parseRule("required", address);
const objectRule: Record<string, unknown> = parseRule({required: true}, address);
const rules: Record<string, unknown> = parseValidationRules("required email", address);
const noRules: Record<string, unknown> = parseValidationRules(undefined, address);
// @ts-expect-error a rule is text or an object
parseRule(1, address);

// The model of the form is checked over the components of the props
const formProps = {components: {Cri: {address, attributes: {id: "Cri", checkEmpty: true}, model, storedModel: model}}, settings: {token: "t", tokenKey: "k"}};
const empty: boolean = checkModelIsEmpty(formProps);
const updated: boolean = checkModelIsUpdated(formProps);
const unchanged: boolean = checkModelIsUnchanged(formProps);
// @ts-expect-error the components are an object by identifier
checkModelIsEmpty({components: []});

// The selected values of a model are a list
const selected: unknown[] = getSelectedValues(model);
const noSelected: unknown[] = getSelectedValues();
// @ts-expect-error the values are options
getSelectedValues({values: "a"});

// The data of every component is indexed by text
const checkbox: Record<string, unknown> = getCheckboxData(criterion, model, props, false);
const criterionData: Record<string, unknown> = getCriterionData(criterion, model, props, true);
const asList: Record<string, unknown> = getCriterionDataAsList(criterion, model, props);
const range: Record<string, unknown> = getDateRangeData(criterion, model, props, false);
const printData: Record<string, unknown> = getCriterionPrintData(criterion, model, props);
const tab: Record<string, unknown> = getTabData(criterion, model, props, false);
const tabPrint: Record<string, unknown> = getTabPrintData(criterion, model, props);
const component: Record<string, unknown> = getComponentData(formProps.components.Cri, formProps, false, "storedModel");
// @ts-expect-error the model to use is the model or the stored model
getComponentData(formProps.components.Cri, formProps, false, "other");
// @ts-expect-error the translator is a function
getCriterionPrintData(criterion, model, {t: "t"});

// The model is fixed, keeping the options
const fixed: {selected: null; values: {selected?: unknown; value?: unknown}[]} = fixModel({selected: [1], values: []}, false);
const fixedDefault: {values: {selected?: unknown}[]} = fixDefaultModel({values: []}, [1], false);
const selectedModel: {value?: unknown} | null = fixSelectedModel("a");
// @ts-expect-error the selected model may be null
const definiteModel: {value?: unknown} = fixSelectedModel(null);
// @ts-expect-error the model has values
fixModel({selected: [1]}, false);

// The controller is fixed with its number format evaluated, and the specific attributes are numbers
const controller: {numberFormat: {precision?: number | string}; size?: string; max?: number} = fixController({id: "Cri", numberFormat: "{precision: 2}"}, false, settings);
const specific: {max: number | undefined; rows: number | undefined; sort: unknown[]; page: number; first: number} = getSpecificAttributes({max: 5}, false, settings);
// @ts-expect-error the settings are an object
fixController({id: "Cri"}, false, "settings");

// The structure is inspected into the context of every identifier
const inspected: Record<string, string[]> = inspectComponentStructure({id: "Grd", elementType: "Grid", elementList: [{id: "Col"}]}, [], {});
// @ts-expect-error the context is a list
inspectComponentStructure({id: "Grd"}, "context", {});

// The class names are text
const classes: string = classNames("a", ["b", "c"], {d: true, e: false}, undefined, 4);
// @ts-expect-error a class name is not a function
classNames(() => "a");

// The identifiers are text, null when the address is not valid
const id: string | null = getComponentId({view: "report", component: "Grd", row: "1", column: "c"});
const dependencyId: string = getDependencyComponentId(address, {column: "c", row: "1", index: 2});
const triggerId: string = getTriggerId({id: "Cri", attribute1: "value", event: "change"}, {address});
const addressType: "cell" | "column" | "component" | "view" | "invalid" = getAddressType(address);
// @ts-expect-error the identifier may be null
const definiteId: string = getComponentId(address);
// @ts-expect-error a dependency has an address
getTriggerId({id: "Cri"}, {});

// The dropdown is toggled, and the actions are sent to the dispatch
declare const event: SyntheticEvent;
const clicked: void = clickDropdown(
  event,
  {addActionsTop: (actions: object[]) => ({type: "add", actions}), updateModelWithDependencies: (a: object, data: object) => ({type: "update", a, data}),
    actions: [{type: "message"}], address, dispatch: (action: object) => action},
  {toggle: (e: SyntheticEvent) => e}
);
// @ts-expect-error the dropdown has a toggle
clickDropdown(event, {addActionsTop: () => 1, updateModelWithDependencies: () => 1, actions: [], address, dispatch: () => 1}, {});

export {uploaded, cell, wrongStatus, unknownAddress, isVoid, isGrid, noGrid, identifierKey, topLevel, malformed, wrongVoid};
export {trace, emptyTrace, rule, objectRule, rules, noRules, empty, updated, unchanged, selected, noSelected, checkbox};
export {criterionData, asList, range, printData, tab, tabPrint, component, fixed, fixedDefault, selectedModel, definiteModel};
export {controller, specific, inspected, classes, id, dependencyId, triggerId, addressType, definiteId, clicked};
