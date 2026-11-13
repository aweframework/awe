// Type-level assertions for the redux selectors, checked by `npm run typecheck` (not run by Jest)
import {
  getAllComponents,
  getContextMenuAttributes,
  makeGetComponent,
  makeGetMultipleComponents
} from "../../../../../src/redux/selectors/componentSelectors";
import {getFormValues} from "../../../../../src/redux/selectors/form";
import {getModelValidation} from "../../../../../src/redux/selectors/modelValidation";
import {selectActionsRunning, selectMessages, selectSettings} from "../../../../../src/redux/selectors/stateSelectors";
import {createStore} from "../../../../../src/redux/store";
import type {ComponentState} from "../../../../../src/utilities/mergeUtils";
import type {MessagesState, RootState, SettingsState} from "../../../../../src/redux/types";

const state: RootState = createStore().getState();

// The slice selectors return their slice
const settings: SettingsState = selectSettings(state);
const messages: MessagesState = selectMessages(state);
const running: boolean = selectActionsRunning(state);
// @ts-expect-error the running flag is not a number
const wrongRunning: number = selectActionsRunning(state);

// A selector of a component takes the state and the component id
const getComponent = makeGetComponent();
const component: ComponentState = getComponent(state, "Grd");
// @ts-expect-error the component id is text
getComponent(state, 1);
const all: Record<string, ComponentState> = getAllComponents(state);
const some: Record<string, ComponentState> = makeGetMultipleComponents(["a", "b"])(state);
const menu: Record<string, Record<string, unknown>> = getContextMenuAttributes(state, [{id: "opt"}]);
// @ts-expect-error the options of the context menu have an id
getContextMenuAttributes(state, [{name: "opt"}]);

// The form values are an object by component id
const values: Record<string, unknown> = getFormValues(state);
getFormValues(state, true, (label: string) => label, {view: "report", component: "Grd"});
// @ts-expect-error the translate function takes and returns text
getFormValues(state, false, 1);

// The model validation reports three flags
const validation: {isEmptyModel: boolean; isUpdatedModel: boolean; isUnchangedModel: boolean} = getModelValidation(state);
// @ts-expect-error the state is the root state
getModelValidation({});

export {settings, messages, running, wrongRunning, component, all, some, menu, values, validation};
