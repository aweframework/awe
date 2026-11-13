import type {ComponentAddress} from "../../contract";
import {checkDependencies, forgetInputDependencies, initializeDependencies} from "../actions/dependencies";
import {
  ButtonTypes,
  resetModel,
  resetMultipleModel,
  restoreModel,
  restoreMultipleModel,
  updateAttributes,
  updateModel,
  updateMultipleComponents,
  updateMultipleModels,
  updateViewComponents
} from "../actions/components";
import type {ComponentData, ComponentUpdate} from "../actions/components";
import {clearRuntimeEvent, setRuntimeEvent} from "../actions/runtime";
import {getActionAddress, getComponent} from "../../utilities";
import type {Address} from "../../utilities";
import {acceptAction, addActionsTop, addStack, rejectAction, removeStack} from "../actions/actions";
import type {ActionItem} from "../actions/actions";
import MenuRegistry from "../registry/MenuRegistry";
import {getEChartsSerie, mergeSeriesPoints} from "../../utilities/echartsSeries";
import type {ActionSerie} from "../../utilities/echartsSeries";
import type {ModelNode, ModelSeries} from "../../utilities/echartsContext";
import {getAllComponents} from "../selectors/componentSelectors";
import {DEFAULT_LATITUDE, DEFAULT_LONGITUDE, toPosition} from "../../utilities/map";
import type {ComponentState} from "../../utilities/mergeUtils";
import type {ComponentsAction} from "../reducers/components";
import type {WithSettings} from "../reducers/components/stateHelpers";
import type {AppDispatch, AppThunk, MenuOption} from "../types";
import type {ClientAction} from "./types";

const { BUTTON_RESET } = ButtonTypes;

/** Data of a model or attribute change: the properties to change, and the event to raise once they are applied */
export type EventData = ComponentData & {event?: string};

/** A row of the model of a component: a step, a point, an option */
type ModelRow = Record<string, unknown>;

/** A button: the actions it launches and the type that decides what it does when it has none */
type ButtonComponent = ComponentState & {
  address: ComponentAddress;
  actions: ActionItem[];
  attributes: {buttonType?: string};
};

/** A dialog: whether it is showing and the action that opened it */
type DialogComponent = ComponentState & {attributes: {isShowing?: boolean; action?: ActionItem | null}};

/** A component whose model has a list of values (the steps of a wizard, the points of a chart) */
type ValuesComponent = ComponentState & {model: {values: ModelRow[]}};

/** A menu: it is minimized or not */
type MenuComponent = ComponentState & {attributes: {minimized?: boolean}};

/** The chart model of an ECharts chart: an ECharts option with the series */
type EChartsModel = ModelNode & {series?: ModelSeries[]};

/** A chart as the series actions read it */
type ChartComponent = ComponentState & {
  attributes?: {echartsModel?: EChartsModel};
  model: {values: ModelRow[]};
};

/** Parameters of an action that changes the series of a chart */
interface SeriesParameters {
  series?: ActionSerie[];
}

/** Parameters of an add-points action: the data list of the server, or a single point */
interface PointsParameters {
  data?: {rows?: unknown};
  value?: ModelRow;
}

/** Parameters of a polyline action: the rows of the route */
interface PolylineParameters {
  rows?: unknown;
}

/** Parameters of a pivot action: the sorters, the rows or the columns to group */
interface PivotParameters {
  sorters?: Record<string, unknown>;
  rows?: string;
  cols?: string;
}

/** Parameters of a change-menu action: the options of the menu */
interface MenuParameters {
  options?: MenuOption[];
}

/** Parameters of a finish-upload action: the file that was uploaded */
interface UploadParameters {
  name: string;
  path: string;
  size: number;
  type: string;
}

/**
 * The component an action works on. An action is launched on a component that exists: when it does not, reading its
 * attributes fails, as it always did
 * @param components Components of the state
 * @param address Address of the component
 * @returns Component
 */
const getActionComponent = <C extends ComponentState = ComponentState>(
  components: Record<string, ComponentState>,
  address: ComponentAddress
): C => getComponent(components, address) as C;

export function updateViewComponentsWithDependencies(view: string, data: Record<string, ComponentState>): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateViewComponents(view, data), settings } satisfies WithSettings<ComponentsAction>);

    // Dispatch calculate dependencies
    initializeDependencies(view, getState(), dispatch);
  };
}

export function updateMultipleComponentsWithDependencies(componentList: ComponentUpdate[]): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateMultipleComponents(componentList), settings } satisfies WithSettings<ComponentsAction>);

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function updateMultipleModelsWithDependencies(data: ComponentUpdate[]): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateMultipleModels(data), settings } satisfies WithSettings<ComponentsAction>);

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function updateModelWithDependencies(address: ComponentAddress, data: EventData): AppThunk {
  return (dispatch, getState) => {
    const { event, ...modelData } = data;

    // Only update the model if there are actual model properties (not just an event)
    if (Object.keys(modelData).length > 0) {
      const { settings } = getState();
      dispatch({ ...updateModel(address, modelData), settings } satisfies WithSettings<ComponentsAction>);
    }

    // Set runtime event if provided
    if (event) {
      dispatch(setRuntimeEvent(address, event));
    }

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);

    // Clear runtime event to prevent re-triggering
    if (event) {
      dispatch(clearRuntimeEvent());
    }
  };
}

/**
 * Raise an event that only notifies, without changing the model: only the dependencies that listen to the event are
 * checked, as AngularJS does, so the others (e.g. the column dependencies of a grid being edited) keep waiting for
 * their own change
 * @param address Address of the component that raises the event
 * @param event Event raised
 * @returns Thunk
 */
export function raiseEventOnDependencies(address: ComponentAddress, event: string): AppThunk {
  return (dispatch, getState) => {
    dispatch(setRuntimeEvent(address, event));
    try {
      // The component that raises an event is on screen, so its address has its view and its name
      checkDependencies(getState(), dispatch, { address: address as Address, event });
    } finally {
      dispatch(clearRuntimeEvent());
    }
  };
}

/**
 * Update the attributes of a component and check the dependencies that read them. When the data has an event, the
 * event is raised while the dependencies are checked (the chart events, which write attributes such as x or xMin)
 * @param address Component address
 * @param data Attributes to update, and the optional event to raise
 * @returns Thunk
 */
export function updateAttributesWithDependencies(address: ComponentAddress, data: EventData): AppThunk {
  return (dispatch, getState) => {
    const { event, ...attributes } = data;
    const { settings } = getState();
    dispatch({ ...updateAttributes(address, attributes), settings } satisfies WithSettings<ComponentsAction>);

    if (!event) {
      checkDependencies(getState(), dispatch);
      return;
    }

    dispatch(setRuntimeEvent(address, event));
    try {
      checkDependencies(getState(), dispatch);
    } finally {
      // The event must not stay raised for later checks, even when a dependency throws
      dispatch(clearRuntimeEvent());
    }
  };
}

export function restoreModelWithDependencies(address: ComponentAddress, data?: ComponentData): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...restoreModel(address, data), settings } satisfies WithSettings<ComponentsAction>);

    // Set runtime event
    dispatch(setRuntimeEvent(address, "change"));

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);

    // Clear runtime event
    dispatch(clearRuntimeEvent());
  };
}

export function restoreMultipleModelWithDependencies(componentList: ComponentUpdate[], initial = false): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...restoreMultipleModel(componentList, initial), settings } satisfies WithSettings<ComponentsAction>);

    // The restored models lose the values the dependencies wrote: launch them again
    // The restored components are on screen, so their addresses have their view and their name
    forgetInputDependencies(componentList as Parameters<typeof forgetInputDependencies>[0]);

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function resetModelWithDependencies(address: ComponentAddress, data?: ComponentData): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...resetModel(address, data), settings } satisfies WithSettings<ComponentsAction>);

    // Set runtime event
    dispatch(setRuntimeEvent(address, "change"));

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);

    // Clear runtime event
    dispatch(clearRuntimeEvent());
  };
}

export function resetMultipleModelWithDependencies(componentList: ComponentUpdate[]): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...resetMultipleModel(componentList), settings } satisfies WithSettings<ComponentsAction>);

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export const clickButtonAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ButtonComponent>(components, address);

    if (component.actions.length > 0) {
      dispatch(addActionsTop(component.actions.map(item => ({ ...item, address: { ...component.address } }))));
    } else if (BUTTON_RESET === component.attributes.buttonType) {
      dispatch(addActionsTop([{ type: "restore", address: { ...component.address } }]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const finishUploadAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const { name, path, size, type } = action.parameters as unknown as UploadParameters;

    // Accept action
    dispatch(acceptAction(action));

    // Change component data
    const address = getActionAddress(action);
    dispatch(updateModelWithDependencies(address, {
      values: [{
        value: path,
        label: name,
        size: size,
        type: type,
        selected: true
      }]
    }));
  };
};

export const deleteUploadAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    // Accept action
    dispatch(acceptAction(action));

    // Change component data
    const address = getActionAddress(action);
    dispatch(resetModelWithDependencies(address));
  };
};

export const openDialogAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<DialogComponent>(components, address);
    const { isShowing } = component.attributes;
    if (!isShowing) {
      dispatch(addStack());
      dispatch(updateAttributes(address, { isShowing: true, action: action }));
      // As AngularJS, the dialog stores the open event once it is opened
      dispatch(updateModelWithDependencies(address, { event: "open" }));
    }
  };
};

export const closeDialogAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    dispatch(acceptAction(action));
    const component = getActionComponent<DialogComponent>(components, address);
    const { isShowing } = component.attributes;
    if (isShowing) {
      // As AngularJS, the dialog stores the close event before it is closed
      dispatch(updateModelWithDependencies(address, { event: "close" }));
      dispatch(removeStack());
      dispatch(acceptAction(component.attributes.action as ActionItem));
      dispatch(updateAttributes(address, { isShowing: false, action: null }));
    }
  };
};

export const closeDialogAndCancelAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    dispatch(acceptAction(action));
    const component = getActionComponent<DialogComponent>(components, address);
    const { isShowing } = component.attributes;
    if (isShowing) {
      // As AngularJS, the dialog stores the close event before it is closed
      dispatch(updateModelWithDependencies(address, { event: "close" }));
      dispatch(removeStack());
      dispatch(rejectAction(component.attributes.action as ActionItem));
      dispatch(updateAttributes(address, { isShowing: false, action: null }));
    }
  };
};

export const goToNextStepAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    const nextIndex = Math.min(component.model.values.findIndex(item => item.selected) + 1, component.model.values.length - 1);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === nextIndex
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToPrevStepAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    const nextIndex = Math.max(component.model.values.findIndex(item => item.selected) - 1, 0);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === nextIndex
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToFirstStepAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === 0
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToLastStepAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === component.model.values.length - 1
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToNthStepAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map(item => ({
        ...item,
        selected: item.value === (action.parameters as {value?: unknown}).value
      }))
    }));

    dispatch(acceptAction(action));
  };
};

/**
 * Retrieve the points of an add-points action
 * @param data Data list sent by the server
 * @param value Single point
 * @returns Points
 */
function getPoints(data?: PointsParameters["data"], value?: ModelRow): ModelRow[] {
  if (Array.isArray(data?.rows)) {
    return data.rows;
  }
  return value ? [value] : [];
}

export const addPointsAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ValuesComponent>(components, address);
    const {data, value} = (action.parameters || {}) as PointsParameters;
    // The server sends a data list, the points are its rows (a single point in "value" is still accepted)
    const points = getPoints(data, value);

    // Change state
    dispatch(updateModelWithDependencies(address, {
      values: [...component.model.values, ...points]
    }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

/**
 * Show a route in a map: the rows of the action (with the latitude and longitude columns of the map) become the
 * route that the map draws. The route is not a change of the data of the map, so no dependency is checked.
 * @param action Action with the route rows in "parameters.rows"
 * @returns Thunk
 */
export const polylineAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    // The map is not on screen when the action arrives late: its columns are the default ones then
    const {attributes = {}} = (getComponent(components, address) || {}) as {
      attributes?: {latitude?: string; longitude?: string};
    };
    const {latitude = DEFAULT_LATITUDE, longitude = DEFAULT_LONGITUDE} = attributes;
    const {rows: routeRows} = (action.parameters || {}) as PolylineParameters;
    const rows = Array.isArray(routeRows) ? routeRows as ModelRow[] : [];
    const route = rows.map(row => toPosition(row, latitude, longitude)).filter(Boolean);

    // The reducer needs the settings to update a component of the registry
    dispatch({...updateModel(address, {route}), settings: getState().settings});

    // Accept action
    dispatch(acceptAction(action));
  };
};

/**
 * Update the series of the ECharts model of a chart
 * @param dispatch Dispatch
 * @param address Address of the chart
 * @param component Chart
 * @param update Retrieves the new series from the current ones
 */
function updateEChartsSeries(
  dispatch: AppDispatch,
  address: ComponentAddress,
  component: ChartComponent,
  update: (current: ModelSeries[]) => ModelSeries[]
): void {
  const echartsModel = component.attributes?.echartsModel;
  if (!echartsModel) {
    console.warn(`[WARNING] The chart '${address.component}' has no echartsModel, its series are not changed`);
    return;
  }
  dispatch(updateAttributes(address, {echartsModel: {...echartsModel, series: update(echartsModel.series || [])}}));
}

export const addSeriesAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ChartComponent>(components, address);
    const series = (action.parameters as SeriesParameters).series || [];
    const ids = new Set(series.map(serie => serie.id));
    const inverted = Boolean(component.attributes?.echartsModel?.awe?.inverted);

    // Add serie
    updateEChartsSeries(dispatch, address, component, current => [
      ...current.filter(serie => !ids.has(serie.id)),
      ...series.map(serie => getEChartsSerie(serie, inverted))
    ]);

    // Change model
    dispatch(updateModelWithDependencies(address, {values: mergeSeriesPoints(component.model.values, series)}));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const removeSeriesAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ChartComponent>(components, address);
    const ids = new Set(((action.parameters as SeriesParameters).series || []).map(serie => serie.id));

    // Remove serie
    updateEChartsSeries(dispatch, address, component, current => current.filter(serie => !ids.has(serie.id)));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const replaceSeriesAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<ChartComponent>(components, address);
    const series = (action.parameters as SeriesParameters).series || [];
    const inverted = Boolean(component.attributes?.echartsModel?.awe?.inverted);

    // Replace serie
    updateEChartsSeries(dispatch, address, component, () => series.map(serie => getEChartsSerie(serie, inverted)));

    // Change model
    dispatch(updateModelWithDependencies(address, {values: mergeSeriesPoints([], series)}));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotSortersAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { sorters = {} } = parameters as PivotParameters;

    // Change attributes
    dispatch(updateAttributes(address, { sorters }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotGroupRowsAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { rows = "" } = parameters as PivotParameters;

    // Change attributes
    dispatch(updateAttributes(address, { rows }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotGroupColsAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { cols = "" } = parameters as PivotParameters;

    // Change attributes
    dispatch(updateAttributes(address, { cols }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const toggleMenuAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getActionComponent<MenuComponent>(components, address);

    dispatch(updateAttributes(address, { minimized: !component.attributes.minimized }));

    dispatch(acceptAction(action));
  };
};

export const toggleNavbarAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    dispatch(acceptAction(action));
  };
};

export const changeMenuAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const { options } = action.parameters as MenuParameters;

    MenuRegistry.setOptions(options);

    dispatch(acceptAction(action));
  };
};
