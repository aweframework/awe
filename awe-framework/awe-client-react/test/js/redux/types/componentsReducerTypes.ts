// Type-level assertions for the components reducer, checked by `npm run typecheck` (not run by Jest)
import {components, actionHandlers, InitialState} from "../../../../src/redux/reducers/components";
import {components as componentsFromRoot} from "../../../../src/redux/reducers/componentsReducer";
import {attributeHandlers} from "../../../../src/redux/reducers/components/attributes";
import {gridModelHandlers} from "../../../../src/redux/reducers/components/gridModel";
import {lifecycleHandlers} from "../../../../src/redux/reducers/components/lifecycle";
import {modelHandlers} from "../../../../src/redux/reducers/components/model";
import {validationRuleHandlers} from "../../../../src/redux/reducers/components/validationRules";
import {
  getMergedComponent,
  updateComponentInState
} from "../../../../src/redux/reducers/components/stateHelpers";
import type {StateComponent} from "../../../../src/redux/reducers/components/stateHelpers";
import {validateComponent, validateRow} from "../../../../src/redux/reducers/validation";
import {
  CLEAR_ALL_COMPONENTS,
  UPDATE_ATTRIBUTES,
  UPDATE_MODEL,
  UPDATE_VALIDATION,
  updateAttributes,
  updateModel,
  updateMultipleComponents,
  updateValidation
} from "../../../../src/redux/actions/components";
import {updateMenu} from "../../../../src/redux/actions/menu";
import type {ComponentsState} from "../../../../src/redux/types";

/** True only when both types are the same type */
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
const assertType = <T extends true>(): T | undefined => undefined;

const address = {view: "base", component: "grid"};

// The reducer returns exactly the components slice, from an undefined state and from a state
assertType<Equal<ReturnType<typeof components>, ComponentsState>>();
assertType<Equal<ReturnType<typeof componentsFromRoot>, ComponentsState>>();
assertType<Equal<typeof InitialState, ComponentsState>>();

// The helpers speak in components states
assertType<Equal<ReturnType<typeof updateComponentInState>, ComponentsState>>();
assertType<Equal<ReturnType<typeof validateComponent>, ComponentsState>>();
assertType<Equal<ReturnType<typeof validateRow>, ComponentsState>>();
assertType<Equal<ReturnType<typeof getMergedComponent>, StateComponent | undefined>>();

// Every handler gets the action of its type, plus the settings the thunks add
type ActionOf<Handler> = Handler extends (state: ComponentsState, action: infer A) => ComponentsState ? A : never;
assertType<Equal<ActionOf<typeof attributeHandlers[typeof UPDATE_ATTRIBUTES]>["type"], typeof UPDATE_ATTRIBUTES>>();
assertType<Equal<ActionOf<typeof modelHandlers[typeof UPDATE_MODEL]>["type"], typeof UPDATE_MODEL>>();
assertType<Equal<ActionOf<typeof validationRuleHandlers[typeof UPDATE_VALIDATION]>["type"], typeof UPDATE_VALIDATION>>();
assertType<Equal<ActionOf<typeof lifecycleHandlers[typeof CLEAR_ALL_COMPONENTS]>["type"], typeof CLEAR_ALL_COMPONENTS>>();
assertType<Equal<keyof typeof gridModelHandlers, "UPDATE_ROW_MODEL" | "KEEP_ROW_MODEL">>();
assertType<Equal<keyof typeof actionHandlers extends string ? true : false, true>>();

// The reducer takes the typed actions of the components slice
const sliceComponents: ComponentsState = components(undefined, updateAttributes(address, {visible: false}));
const sliceModel: ComponentsState = components(sliceComponents, updateModel(address, {values: []}));
const sliceMultiple: ComponentsState = components(sliceModel, updateMultipleComponents([{address}]));
const sliceValidation: ComponentsState = components(sliceMultiple, updateValidation(address, {required: true}));
const sliceSettings: ComponentsState = components(sliceValidation, {...updateAttributes(address, {}), settings: undefined});

// The reducer rejects the actions of other slices and malformed ones
// @ts-expect-error a menu action is not a components action
components(undefined, updateMenu([]));
// @ts-expect-error the components of an action are a list
components(undefined, {type: "UPDATE_MULTIPLE_COMPONENTS", componentList: 1});
// @ts-expect-error the state is a map of components
components([], updateAttributes(address, {}));

export {sliceComponents, sliceModel, sliceMultiple, sliceValidation, sliceSettings};
