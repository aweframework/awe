/*
 * action types
 */

import type {ComponentAddress} from "../../contract";
import type {ComponentState} from "../../utilities/mergeUtils";

/** Data an action carries to a component: attributes, model values or validation rules by key */
export type ComponentData = Record<string, unknown>;

/** A component of a multiple action: its address and the data to apply */
export interface ComponentUpdate {
  address: ComponentAddress;
  data?: ComponentData;
  [key: string]: unknown;
}

/** A component of a multiple attribute action: its address and the name of the attribute to restore */
export interface AttributeReference {
  address: ComponentAddress;
  data?: string;
  [key: string]: unknown;
}

/** Data of a keep model action: the snapshot is also the default model when it is the initial one */
export interface KeepModelData {
  initial?: boolean;
  defaultModel?: Record<string, unknown>;
}

export const CLEAR_COMPONENTS = 'CLEAR_COMPONENTS' as const;
export const CLEAR_ALL_COMPONENTS = 'CLEAR_ALL_COMPONENTS' as const;
export const UPDATE_VIEW_COMPONENTS = 'UPDATE_VIEW_COMPONENTS' as const;
export const REGISTER_COMPONENTS = 'REGISTER_COMPONENTS' as const;
export const UNREGISTER_COMPONENTS = 'UNREGISTER_COMPONENTS' as const;
export const UPDATE_COMPONENT = 'UPDATE_COMPONENT' as const;
export const UPDATE_ATTRIBUTES = 'UPDATE_ATTRIBUTES' as const;
export const UPDATE_SPECIFIC_ATTRIBUTES = 'UPDATE_SPECIFIC_ATTRIBUTES' as const;
export const UPDATE_MULTIPLE_ATTRIBUTES = 'UPDATE_MULTIPLE_ATTRIBUTES' as const;
export const UPDATE_MODEL = 'UPDATE_MODEL' as const;
export const UPDATE_ROW_MODEL = 'UPDATE_ROW_MODEL' as const;
export const UPDATE_VALIDATION = 'UPDATE_VALIDATION' as const;
export const UPDATE_MULTIPLE_VALIDATION = 'UPDATE_MULTIPLE_VALIDATION' as const;
export const UPDATE_MULTIPLE_COMPONENTS = 'UPDATE_MULTIPLE_COMPONENTS' as const;
export const UPDATE_MULTIPLE_MODELS = 'UPDATE_MULTIPLE_MODELS' as const;
export const UPDATE_COLUMN_ATTRIBUTES = 'UPDATE_COLUMN_ATTRIBUTES' as const;
export const UPDATE_MULTIPLE_COLUMN_ATTRIBUTES = 'UPDATE_MULTIPLE_COLUMN_ATTRIBUTES' as const;

// Keep, restore & reset actions
export const KEEP_VALIDATION = 'KEEP_VALIDATION' as const;
export const KEEP_ATTRIBUTE = 'KEEP_ATTRIBUTE' as const;
export const KEEP_MODEL = 'KEEP_MODEL' as const;
export const KEEP_ROW_MODEL = 'KEEP_ROW_MODEL' as const;
export const RESTORE_VALIDATION = 'RESTORE_VALIDATION' as const;
export const RESTORE_MULTIPLE_VALIDATION = 'RESTORE_MULTIPLE_VALIDATION' as const;
export const RESTORE_ATTRIBUTE = 'RESTORE_ATTRIBUTE' as const;
export const RESTORE_MULTIPLE_ATTRIBUTES = 'RESTORE_MULTIPLE_ATTRIBUTES' as const;
export const RESTORE_MODEL = 'RESTORE_MODEL' as const;
export const RESTORE_MULTIPLE_MODEL = 'RESTORE_MULTIPLE_MODEL' as const;
export const RESET_MODEL = 'RESET_MODEL' as const;
export const RESET_MULTIPLE_MODEL = 'RESET_MULTIPLE_MODEL' as const;

export const VALIDATE_COMPONENTS = 'VALIDATE_COMPONENTS' as const;
export const VALIDATE_ROW = 'VALIDATE_ROW' as const;

/*
 * action status
 */

export const ComponentStatus = {
  STATUS_DEFINED: 'defined',
  STATUS_COMPILED: 'compiled',
  STATUS_INITIALIZED: 'initialized'
} as const;

// Button types
export const ButtonTypes = {
  BUTTON_NORMAL: "button",
  BUTTON_RESET: "reset",
  BUTTON_SUBMIT: "submit"
} as const;

/*
 * action creators
 */

export function clearComponents(view: string) {
  return {type: CLEAR_COMPONENTS, view};
}

export function clearAllComponents() {
  return {type: CLEAR_ALL_COMPONENTS};
}

export function updateViewComponents(view: string, data: Record<string, ComponentState>) {
  return {type: UPDATE_VIEW_COMPONENTS, view, data};
}

/**
 * Add components to a loaded view, keeping the ones it has (the instances of a grid row detail)
 * @param data Components by id, built as the screen builds its own
 * @returns Action
 */
export function registerComponents(data: Record<string, ComponentState>) {
  return {type: REGISTER_COMPONENTS, data};
}

/**
 * Remove some components and all their state
 * @param ids Component ids
 * @returns Action
 */
export function unregisterComponents(ids: string[]) {
  return {type: UNREGISTER_COMPONENTS, ids};
}

export function updateComponent(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_COMPONENT, address, data};
}

export function updateMultipleComponents(componentList: ComponentUpdate[]) {
  return {type: UPDATE_MULTIPLE_COMPONENTS, componentList};
}

export function updateMultipleModels(componentList: ComponentUpdate[]) {
  return {type: UPDATE_MULTIPLE_MODELS, componentList};
}

export function updateAttributes(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_ATTRIBUTES, address, data};
}

export function updateSpecificAttributes(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_SPECIFIC_ATTRIBUTES, address, data};
}

export function updateMultipleAttributes(componentList: ComponentUpdate[]) {
  return {type: UPDATE_MULTIPLE_ATTRIBUTES, componentList};
}

export function updateModel(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_MODEL, address, data};
}

export function updateRowModel(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_ROW_MODEL, address, data};
}

export function updateValidation(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_VALIDATION, address, data};
}

export function updateMultipleValidation(componentList: ComponentUpdate[]) {
  return {type: UPDATE_MULTIPLE_VALIDATION, componentList};
}

export function updateColumnAttributes(address: ComponentAddress, data: ComponentData) {
  return {type: UPDATE_COLUMN_ATTRIBUTES, address, data};
}

export function updateMultipleColumnAttributes(componentList: ComponentUpdate[]) {
  return {type: UPDATE_MULTIPLE_COLUMN_ATTRIBUTES, componentList};
}

export function keepValidation(address: ComponentAddress, data?: ComponentData) {
  return {type: KEEP_VALIDATION, address, data};
}

export function keepAttribute(address: ComponentAddress, data?: string) {
  return {type: KEEP_ATTRIBUTE, address, data};
}

export function keepModel(address: ComponentAddress, data?: KeepModelData) {
  return {type: KEEP_MODEL, address, data};
}

export function keepRowModel(address: ComponentAddress, data?: ComponentData) {
  return {type: KEEP_ROW_MODEL, address, data};
}

export function restoreValidation(address: ComponentAddress, data?: ComponentData) {
  return {type: RESTORE_VALIDATION, address, data};
}

export function restoreMultipleValidation(componentList: ComponentUpdate[]) {
  return {type: RESTORE_MULTIPLE_VALIDATION, componentList};
}

export function restoreAttribute(address: ComponentAddress, data?: string) {
  return {type: RESTORE_ATTRIBUTE, address, data};
}

export function restoreMultipleAttributes(componentList: AttributeReference[]) {
  return {type: RESTORE_MULTIPLE_ATTRIBUTES, componentList};
}

export function restoreModel(address: ComponentAddress, data?: ComponentData, initial = false) {
  return {type: RESTORE_MODEL, address, data, initial};
}

export function restoreMultipleModel(componentList: ComponentUpdate[], initial = false) {
  return {type: RESTORE_MULTIPLE_MODEL, componentList, initial};
}

export function resetModel(address: ComponentAddress, data?: ComponentData) {
  return {type: RESET_MODEL, address, data};
}

export function resetMultipleModel(componentList: ComponentUpdate[]) {
  return {type: RESET_MULTIPLE_MODEL, componentList};
}
