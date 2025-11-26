/*
 * action types
 */

import {addActionsTop} from "./actions";

export const CLEAR_COMPONENTS = 'CLEAR_COMPONENTS';
export const CLEAR_ALL_COMPONENTS = 'CLEAR_ALL_COMPONENTS';
export const GENERATE_CELL_COMPONENTS = 'GENERATE_CELL_COMPONENTS';
export const UPDATE_VIEW_COMPONENTS = 'UPDATE_VIEW_COMPONENTS';
export const UPDATE_COMPONENT = 'UPDATE_COMPONENT';
export const UPDATE_ATTRIBUTES = 'UPDATE_ATTRIBUTES';
export const UPDATE_SPECIFIC_ATTRIBUTES = 'UPDATE_SPECIFIC_ATTRIBUTES';
export const UPDATE_MULTIPLE_ATTRIBUTES = 'UPDATE_MULTIPLE_ATTRIBUTES';
export const UPDATE_MODEL = 'UPDATE_MODEL';
export const UPDATE_ROW_MODEL = 'UPDATE_ROW_MODEL';
export const UPDATE_VALIDATION = 'UPDATE_VALIDATION';
export const UPDATE_MULTIPLE_VALIDATION = 'UPDATE_MULTIPLE_VALIDATION';
export const UPDATE_MULTIPLE_COMPONENTS = 'UPDATE_MULTIPLE_COMPONENTS';
export const UPDATE_MULTIPLE_MODELS = 'UPDATE_MULTIPLE_MODELS';
export const UPDATE_COLUMN_ATTRIBUTES = 'UPDATE_COLUMN_ATTRIBUTES';
export const UPDATE_MULTIPLE_COLUMN_ATTRIBUTES = 'UPDATE_MULTIPLE_COLUMN_ATTRIBUTES';

// Keep, restore & reset actions
export const KEEP_VALIDATION = 'KEEP_VALIDATION';
export const KEEP_ATTRIBUTE = 'KEEP_ATTRIBUTE';
export const KEEP_MODEL = 'KEEP_MODEL';
export const KEEP_ROW_MODEL = 'KEEP_ROW_MODEL';
export const RESTORE_VALIDATION = 'RESTORE_VALIDATION';
export const RESTORE_MULTIPLE_VALIDATION = 'RESTORE_MULTIPLE_VALIDATION';
export const RESTORE_ATTRIBUTE = 'RESTORE_ATTRIBUTE';
export const RESTORE_MULTIPLE_ATTRIBUTES = 'RESTORE_MULTIPLE_ATTRIBUTES';
export const RESTORE_MODEL = 'RESTORE_MODEL';
export const RESTORE_MULTIPLE_MODEL = 'RESTORE_MULTIPLE_MODEL';
export const RESET_MODEL = 'RESET_MODEL';
export const RESET_MULTIPLE_MODEL = 'RESET_MULTIPLE_MODEL';

export const VALIDATE_COMPONENTS = 'VALIDATE_COMPONENTS';
export const VALIDATE_ROW = 'VALIDATE_ROW';

/*
 * action status
 */

export const ComponentStatus = {
  STATUS_DEFINED: 'defined',
  STATUS_COMPILED: 'compiled',
  STATUS_INITIALIZED: 'initialized'
};

// Button types
export const ButtonTypes = {
  BUTTON_NORMAL: "button",
  BUTTON_RESET: "reset",
  BUTTON_SUBMIT: "submit"
}



/*
 * action creators
 */

export function clearComponents(view) {
  return {type: CLEAR_COMPONENTS, view};
}

export function clearAllComponents() {
  return {type: CLEAR_ALL_COMPONENTS};
}



export function updateViewComponents(view, data) {
  return {type: UPDATE_VIEW_COMPONENTS, view, data};
}

export function updateComponent(address, data) {
  return {type: UPDATE_COMPONENT, address, data};
}

export function updateMultipleComponents(componentList) {
  return {type: UPDATE_MULTIPLE_COMPONENTS, componentList};
}

export function updateMultipleModels(componentList) {
  return {type: UPDATE_MULTIPLE_MODELS, componentList};
}

export function generateCellComponents(address, data) {
  return {type: GENERATE_CELL_COMPONENTS, address, data};
}

export function updateAttributes(address, data) {
  return {type: UPDATE_ATTRIBUTES, address, data};
}

export function updateSpecificAttributes(address, data) {
  return {type: UPDATE_SPECIFIC_ATTRIBUTES, address, data};
}

export function updateMultipleAttributes(componentList) {
  return {type: UPDATE_MULTIPLE_ATTRIBUTES, componentList};
}

export function updateModel(address, data) {
  return {type: UPDATE_MODEL, address, data};
}

export function updateRowModel(address, data) {
  return {type: UPDATE_ROW_MODEL, address, data};
}

export function updateValidation(address, data) {
  return {type: UPDATE_VALIDATION, address, data};
}

export function updateMultipleValidation(componentList) {
  return {type: UPDATE_MULTIPLE_VALIDATION, componentList};
}

export function updateColumnAttributes(address, data) {
  return {type: UPDATE_COLUMN_ATTRIBUTES, address, data};
}

export function updateMultipleColumnAttributes(componentList) {
  return {type: UPDATE_MULTIPLE_COLUMN_ATTRIBUTES, componentList};
}

export function keepValidation(address, data) {
  return {type: KEEP_VALIDATION, address, data};
}

export function keepAttribute(address, data) {
  return {type: KEEP_ATTRIBUTE, address, data};
}

export function keepModel(address, data) {
  return {type: KEEP_MODEL, address, data};
}

export function keepRowModel(address, data) {
  return {type: KEEP_ROW_MODEL, address, data};
}

export function restoreValidation(address, data) {
  return {type: RESTORE_VALIDATION, address, data};
}

export function restoreMultipleValidation(componentList) {
  return {type: RESTORE_MULTIPLE_VALIDATION, componentList};
}

export function restoreAttribute(address, data) {
  return {type: RESTORE_ATTRIBUTE, address, data};
}

export function restoreMultipleAttributes(componentList) {
  return {type: RESTORE_MULTIPLE_ATTRIBUTES, componentList};
}

export function restoreModel(address, data) {
  return {type: RESTORE_MODEL, address, data};
}

export function restoreMultipleModel(componentList) {
  return {type: RESTORE_MULTIPLE_MODEL, componentList};
}

export function resetModel(address, data) {
  return {type: RESET_MODEL, address, data};
}

export function resetMultipleModel(componentList) {
  return {type: RESET_MULTIPLE_MODEL, componentList};
}
