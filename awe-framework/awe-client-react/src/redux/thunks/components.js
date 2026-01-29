import { checkDependencies, initializeDependencies } from "../actions/dependencies";
import {
  ButtonTypes,
  resetModel, resetMultipleModel,
  restoreModel, restoreMultipleModel, updateAttributes,
  updateModel,
  updateMultipleComponents,
  updateMultipleModels,
  updateViewComponents
} from "../actions/components";
import { setRuntimeEvent, clearRuntimeEvent } from "../actions/runtime";
import { getActionAddress, getComponent } from "../../utilities";
import { acceptAction, addActionsTop, addStack, rejectAction, removeStack } from "../actions/actions";
import MenuRegistry from "../registry/MenuRegistry";
import { getAllComponents } from "../selectors/componentSelectors";
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

    const { settings } = getState();
    // Update the model first (without event)
    dispatch({ ...updateModel(address, modelData), settings });

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

export function restoreMultipleModelWithDependencies(componentList) {
  return (dispatch, getState) => {
    const { settings } = getState();
    // Update the model first
    dispatch({ ...restoreMultipleModel(componentList), settings });

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

export const addPointsAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    // Change state
    dispatch(updateModelWithDependencies(address, {
      values: [...component.model.values, action.parameters.value]
    }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const addSeriesAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const series = action.parameters.series || [];
    const currentSeries = (component.attributes?.chartModel?.series || []).filter(serie => !series.map(s => s.id).includes(serie.id));

    // Add serie
    dispatch(updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: [
          ...currentSeries,
          ...series
        ]
      }
    }));

    const values = [...currentSeries, ...series].map((s) => s.data.map(([k, v]) => ({ [s.xValue]: k, [s.yValue]: v })))
      .reduce((t, a) => [...a.map((ar, i) => ({ ...component.model.values[i] || {}, ...ar, ...t[i] || {} }))], []);

    // Change model
    dispatch(updateModelWithDependencies(address, { values }));

    // Accept action
    dispatch(acceptAction(action));
  };
};

export const removeSeriesAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const address = getActionAddress(action);
    const component = getComponent(components, address);
    const series = action.parameters.series || [];
    const currentSeries = (component.attributes?.chartModel?.series || []).filter(serie => !series.map(s => s.id).includes(serie.id));

    // Remove serie
    dispatch(updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: currentSeries
      }
    }));

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

    // Replace serie
    dispatch(updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: [
          ...series
        ]
      }
    }));

    const values = [...series].map((s) => s.data.map(([k, v]) => ({ [s.xValue]: k, [s.yValue]: v })))
      .reduce((t, a) => [...a.map((ar, i) => ({ ...ar, ...t[i] || {} }))], []);

    // Change model
    dispatch(updateModelWithDependencies(address, { values }));

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
