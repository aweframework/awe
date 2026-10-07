import {checkDependencies, initializeDependencies} from "../actions/dependencies";
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
import {clearRuntimeEvent, setRuntimeEvent} from "../actions/runtime";
import {getActionAddress, getComponent} from "../../utilities";
import {acceptAction, addActionsTop, addStack, rejectAction, removeStack} from "../actions/actions";
import MenuRegistry from "../registry/MenuRegistry";
import {getAllComponents} from "../selectors/componentSelectors";

const { BUTTON_RESET } = ButtonTypes;

export function updateViewComponentsWithDependencies(view, data) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateViewComponents(view, data), settings });

    // Dispatch calculate dependencies
    initializeDependencies(view, getState(), dispatch);
  };
}

export function updateMultipleComponentsWithDependencies(componentList) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateMultipleComponents(componentList), settings });

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function updateMultipleModelsWithDependencies(data) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...updateMultipleModels(data), settings });

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function updateModelWithDependencies(address, data) {
  return (dispatch, getState) => {
    const { event, ...modelData } = data;

    // Only update the model if there are actual model properties (not just an event)
    if (Object.keys(modelData).length > 0) {
      const { settings } = getState();
      dispatch({ ...updateModel(address, modelData), settings });
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

export function restoreModelWithDependencies(address, data) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...restoreModel(address, data), settings });

    // Set runtime event
    dispatch(setRuntimeEvent(address, "change"));

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);

    // Clear runtime event
    dispatch(clearRuntimeEvent());
  };
}

export function restoreMultipleModelWithDependencies(componentList, initial = false) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...restoreMultipleModel(componentList, initial), settings });

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export function resetModelWithDependencies(address, data) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...resetModel(address, data), settings });

    // Set runtime event
    dispatch(setRuntimeEvent(address, "change"));

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);

    // Clear runtime event
    dispatch(clearRuntimeEvent());
  };
}

export function resetMultipleModelWithDependencies(componentList) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...resetMultipleModel(componentList), settings });

    // Dispatch calculate dependencies
    checkDependencies(getState(), dispatch);
  };
}

export const clickButtonAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);

    if (component.actions.length > 0) {
      dispatch(addActionsTop(component.actions.map(item => ({ ...item, address: { ...component.address } }))));
    } else if (BUTTON_RESET === component.attributes.buttonType) {
      dispatch(addActionsTop([{ type: "restore", address: { ...component.address } }]));
    }

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const finishUploadAction = (action) => {
  return (dispatch) => {
    const { name, path, size, type } = action.parameters;

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

export const deleteUploadAction = (action) => {
  return (dispatch) => {
    // Accept action
    dispatch(acceptAction(action));

    // Change component data
    const address = getActionAddress(action);
    dispatch(resetModelWithDependencies(address));
  };
};

export const openDialogAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const { isShowing } = component.attributes;
    if (!isShowing) {
      dispatch(addStack());
      dispatch(updateAttributes(address, { isShowing: true, action: action }));
    }
  };
};

export const closeDialogAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    dispatch(acceptAction(action));
    const component = getComponent(components, address);
    const { isShowing } = component.attributes;
    if (isShowing) {
      dispatch(removeStack());
      dispatch(acceptAction(component.attributes.action));
      dispatch(updateAttributes(address, { isShowing: false, action: null }));
    }
  };
};

export const closeDialogAndCancelAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    dispatch(acceptAction(action));
    const component = getComponent(components, address);
    const { isShowing } = component.attributes;
    if (isShowing) {
      dispatch(removeStack());
      dispatch(rejectAction(component.attributes.action));
      dispatch(updateAttributes(address, { isShowing: false, action: null }));
    }
  };
};

export const goToNextStepAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
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

export const goToPrevStepAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
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

export const goToFirstStepAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === 0
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToLastStepAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === component.model.values.length - 1
      }))
    }));

    dispatch(acceptAction(action));
  };
};

export const goToNthStepAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    dispatch(updateModelWithDependencies(address, {
      values: component.model.values.map(item => ({
        ...item,
        selected: item.value === action.parameters.value
      }))
    }));

    dispatch(acceptAction(action));
  };
};

/**
 * Retrieve the points of an add-points action
 * @param {object} [data] Data list sent by the server
 * @param {object} [value] Single point
 * @returns {object[]} Points
 */
function getPoints(data, value) {
  if (Array.isArray(data?.rows)) {
    return data.rows;
  }
  return value ? [value] : [];
}

export const addPointsAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const {data, value} = action.parameters || {};
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

const ECHARTS_TYPES = {column: "bar", bar: "bar", pie: "pie", scatter: "scatter", bubble: "scatter"};

/**
 * Retrieve the ECharts series of a series that a chart action sends. The server sends the translation in the
 * "echarts" property; the series is built from its Highcharts fields when it is missing
 * @param {object} serie Series of the action
 * @param {boolean} inverted The chart is inverted, so its axes are swapped
 * @returns {object} ECharts series
 */
function getEChartsSerie(serie, inverted) {
  const translated = serie.echarts || {
    id: serie.id,
    name: serie.name ?? serie.label,
    type: ECHARTS_TYPES[serie.type] || "line",
    ...(serie.color ? {itemStyle: {color: serie.color}} : {}),
    awe: {
      type: serie.type,
      xValue: serie.xValue,
      yValue: serie.yValue,
      ...(serie.zValue ? {zValue: serie.zValue} : {})
    }
  };
  if (!inverted) {
    return translated;
  }
  const {xAxisIndex, yAxisIndex, ...rest} = translated;
  return {
    ...rest,
    ...(yAxisIndex === undefined ? {} : {xAxisIndex: yAxisIndex}),
    ...(xAxisIndex === undefined ? {} : {yAxisIndex: xAxisIndex})
  };
}

/**
 * Merge the points of the series of an action into the values of the chart, row by row
 * @param {object[]} values Current values
 * @param {object[]} series Series of the action
 * @returns {object[]} New values
 */
function mergeSeriesPoints(values, series) {
  const rows = [...values];
  series.forEach(serie => {
    const {xValue = serie.echarts?.awe?.xValue, yValue = serie.echarts?.awe?.yValue} = serie;
    const zValue = serie.zValue ?? serie.echarts?.awe?.zValue;
    if (!xValue || !yValue) {
      return;
    }
    (serie.data || []).forEach(([x, y, z], index) => {
      rows[index] = {...rows[index], [xValue]: x, [yValue]: y, ...(zValue ? {[zValue]: z} : {})};
    });
  });
  return rows;
}

/**
 * Update the series of the ECharts model of a chart
 * @param {function} dispatch Dispatch
 * @param {object} address Address of the chart
 * @param {object} component Chart
 * @param {function(object[]): object[]} update Retrieves the new series from the current ones
 */
function updateEChartsSeries(dispatch, address, component, update) {
  const echartsModel = component.attributes?.echartsModel;
  if (!echartsModel) {
    console.warn(`[WARNING] The chart '${address.component}' has no echartsModel, its series are not changed`);
    return;
  }
  dispatch(updateAttributes(address, {echartsModel: {...echartsModel, series: update(echartsModel.series || [])}}));
}

export const addSeriesAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const series = action.parameters.series || [];
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

export const removeSeriesAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const ids = new Set((action.parameters.series || []).map(serie => serie.id));

    // Remove serie
    updateEChartsSeries(dispatch, address, component, current => current.filter(serie => !ids.has(serie.id)));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const replaceSeriesAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const series = action.parameters.series || [];
    const inverted = Boolean(component.attributes?.echartsModel?.awe?.inverted);

    // Replace serie
    updateEChartsSeries(dispatch, address, component, () => series.map(serie => getEChartsSerie(serie, inverted)));

    // Change model
    dispatch(updateModelWithDependencies(address, {values: mergeSeriesPoints([], series)}));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotSortersAction = (action) => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { sorters = {} } = parameters;

    // Change attributes
    dispatch(updateAttributes(address, { sorters }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotGroupRowsAction = (action) => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { rows = "" } = parameters;

    // Change attributes
    dispatch(updateAttributes(address, { rows }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const setPivotGroupColsAction = (action) => {
  return (dispatch) => {
    const address = getActionAddress(action);
    const { parameters = {} } = action;
    const { cols = "" } = parameters;

    // Change attributes
    dispatch(updateAttributes(address, { cols }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const toggleMenuAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);

    dispatch(updateAttributes(address, { minimized: !component.attributes.minimized }));

    dispatch(acceptAction(action));
  };
};

export const toggleNavbarAction = (action) => {
  return (dispatch) => {
    dispatch(acceptAction(action));
  };
};

export const changeMenuAction = (action) => {
  return (dispatch) => {
    const { options } = action.parameters;

    MenuRegistry.setOptions(options);

    dispatch(acceptAction(action));
  };
};
