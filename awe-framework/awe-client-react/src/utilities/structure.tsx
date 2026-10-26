import React from 'react';
import type {ComponentAddress} from "../contract";
import {ComponentList, IGNORE_COMPONENT_LIST} from "../components/AweComponents";
import {CriteriaList} from "../criteria/AweCriteria";
import {classNames, getComponentId} from "./components";
import {Editor, Static} from "../columns/AweColumns";
import {renderSafeHtml} from "./sanitizeHtml";
import {extractCellModel, getColumnWrap} from "./grid";
import type {CellOption} from "./grid";
import {ComponentType, getFirstDefinedAndNotNullValue, isMultipleComponent} from "./general";

/** An element of the structure of a screen, as the screen sends it: its type and its own attributes */
export interface StructureElement {
  elementType?: string;
  id?: string;
  component?: string;
  [key: string]: unknown;
}

/** A source tag of a screen: its elements and, when it has a type, the element it generates around them */
export interface SourceElement {
  source?: string;
  type?: string;
  id?: string;
  style?: string;
  expand?: string;
  elementList?: StructureElement[];
}

/** A column of a grid, as the cells read it */
export interface ColumnElement {
  component?: string;
  address: ComponentAddress;
  wrap?: boolean;
  maxLines?: number;
  [key: string]: unknown;
}

// The registries are objects of components by name, read by the name an element brings. They are read when a component is
// created, never when this module loads: the components import this module back, so they are not defined yet then
type Registry = Record<string, React.ElementType>;
const components = (): Registry => ComponentList;
const criteria = (): Registry => CriteriaList;
const editors = (): Registry => Editor;
const statics = (): Registry => Static;

/**
 * Retrieves the first element from the provided list that matches the specified source.
 *
 * @param {string} source - The source value to match against the elements.
 * @param {Array.<Object>} elements - An array of objects where each object contains a `source` property.
 * @return {Object|undefined} The first object in the array with a `source` property matching the given source,
 * or undefined if no match is found.
 */
export function getSource(source: string, elements: SourceElement[]): SourceElement {
  return elements.find(n => n.source === source) || {elementList: []};
}

/**
 * Retrieves the content of a source tag of the screen. As in the AngularJS engine, a source tag with a type
 * generates its own element (with its id, style and expandible direction) around its children; without a type the
 * children go straight to the template.
 *
 * @param {Object} node - The source node containing an element list.
 * @return {Array|Object|null} The children, wrapped in the element of the source tag when it has a type, or null if the node is falsy.
 */
export function getSourceChildren(node: SourceElement | null | undefined = {elementList: []}): React.ReactNode[] | React.ReactElement | null {
  if (!node) {
    return null;
  }
  const children = (node.elementList || []).map((child, index) => Components(child, index));
  if (!node.type) {
    return children;
  }
  const className = classNames({[`expandible-${node.expand}`]: node.expand}, node.style);
  return React.createElement(node.type, {id: node.id, className: className || undefined}, children);
}

/**
 * Retrieves the style that a template applies to its own container of a source. When the source tag generates
 * its own element, the style belongs to that element and not to the container.
 *
 * @param {Object} node - The source node.
 * @return {string} The style of the container.
 */
export function getSourceStyle(node?: SourceElement | null): string {
  return node && !node.type && node.style ? node.style : "";
}

/**
 * Checks if the cell of a sparkline column is an already parsed series (a list of numbers, not a list of options)
 * @param {string} component Column component
 * @param {*} data Cell data
 * @return {boolean} The cell is a series
 */
function isSparklineSeries(component: string | undefined, data: unknown): boolean {
  return component === ComponentType.COMPONENT_SPARKLINE && Array.isArray(data) && !data.some(item => item !== null && typeof item === "object");
}

/**
 * Get component
 * @param node
 * @param index
 * @returns React component
 * @constructor
 */
export const Components = (node: StructureElement, index: number): React.ReactElement | null | undefined => {
  if (IGNORE_COMPONENT_LIST.includes(node.elementType as string)) return null;
  if (node.elementType === "Criteria") {
    return Criteria(node, index);
  } else if ((node.elementType as string) in ComponentList) {
    return React.createElement(components()[node.elementType as string], {
      ...node, key: node.id || `component-${index}`
    });
  }
};

/**
 * Get criterion
 * @param node
 * @param index
 * @returns React component
 * @constructor
 */
export const Criteria = (node: StructureElement, index: number): React.ReactElement => {
  if (typeof criteria()[node.component as string] !== "undefined") {
    return React.createElement(criteria()[node.component as string], {
      ...node, key: node.id || `criterion-${index}`
    });
  }
  return React.createElement(
    () => <div>The criterion {node.component} has not been created yet.</div>,
    {key: index}
  );
};

const clampStyles = new Map<number, Readonly<Record<string, number>>>();

/**
 * Style of a cell that is cut after a maximum number of lines. One object per maximum, so that the rows do not get a
 * new style on every render
 * @param {number} maxLines Maximum number of lines
 * @return {Object} Style
 */
function clampStyle(maxLines: number): Readonly<Record<string, number>> {
  if (!clampStyles.has(maxLines)) {
    clampStyles.set(maxLines, Object.freeze({"--cell-max-lines": maxLines}));
  }
  return clampStyles.get(maxLines) as Readonly<Record<string, number>>;
}

/**
 * Text of a cell. It is one line cut with an ellipsis (the full value as tooltip), or it wraps over several lines when
 * the column declares it, with an optional maximum number of lines that ends in an ellipsis (the full value is then
 * shown by the grid on hover and on keyboard focus).
 * @param {string} visibleValue Text of the cell
 * @param {Object} column Column definition
 * @param {string} [style] Cell style
 * @return {React.ReactElement} Text element
 */
function textCell(visibleValue: string, column: ColumnElement, style?: string): React.ReactElement {
  const {wrap, maxLines} = getColumnWrap(column);
  if (!wrap) {
    const className = classNames("p-cell-text", "white-space-nowrap", "p-text-truncate", style);
    return <span className={className} title={visibleValue}>{renderSafeHtml(visibleValue)}</span>;
  }
  if (maxLines === null) {
    return <span className={classNames("p-cell-text", "p-cell-wrap", style)}>{renderSafeHtml(visibleValue)}</span>;
  }
  return <span className={classNames("p-cell-text", "p-cell-wrap", "p-cell-clamp", style)}
    style={clampStyle(maxLines) as React.CSSProperties} tabIndex={0} data-full-text={visibleValue}>{renderSafeHtml(visibleValue)}</span>;
}

/**
 * Get column
 * @param node
 * @param data
 * @param attrs
 * @param editing
 * @returns React element
 * @constructor
 */
export const Columns = (node: ColumnElement, data: unknown = {}, attrs: Record<string, unknown> = {}, editing = false): React.ReactElement => {
  const {component} = node;
  const isMultiple = isMultipleComponent(component);
  const fixedData = isMultiple ? data : extractCellModel(isSparklineSeries(component, data) ? {value: data} : data);
  if (editing && typeof editors()[component as string] !== "undefined") {
    return React.createElement(editors()[component as string], {
      ...node,
      key: getComponentId(node.address),
      data: fixedData,
      attrs
    });
  } else if (typeof statics()[component as string] !== "undefined") {
    return React.createElement(statics()[component as string], {
      ...node,
      key: getComponentId(node.address),
      data: fixedData,
      attrs
    });
  }
  if (Array.isArray(fixedData)) {
    const selectedItems = fixedData.filter(item => item?.selected);
    const items = selectedItems.length > 0 ? selectedItems : fixedData;
    const visibleValue = items
      .map(item => getFirstDefinedAndNotNullValue(item?.label, item?.value, ""))
      .filter(value => value !== "")
      .join(", ");
    return textCell(visibleValue, node);
  }
  const {label, value, style} = fixedData as CellOption;
  // The label or the value of a cell are text, or a number that is shown as it comes
  const visibleValue = getFirstDefinedAndNotNullValue(label, value, "") as string;
  return textCell(visibleValue, node, style as string | undefined);
};
