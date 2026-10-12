import React from 'react';
import {ComponentList, IGNORE_COMPONENT_LIST} from "../components/AweComponents";
import {CriteriaList} from "../criteria/AweCriteria";
import {classNames, getComponentId} from "./components";
import {Editor, Static} from "../columns/AweColumns";
import {renderSafeHtml} from "./sanitizeHtml";
import {extractCellModel} from "./grid";
import {getFirstDefinedAndNotNullValue, isMultipleComponent} from "./general";

/**
 * Retrieves the first element from the provided list that matches the specified source.
 *
 * @param {string} source - The source value to match against the elements.
 * @param {Array.<Object>} elements - An array of objects where each object contains a `source` property.
 * @return {Object|undefined} The first object in the array with a `source` property matching the given source,
 * or undefined if no match is found.
 */
export function getSource(source, elements) {
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
export function getSourceChildren(node= {elementList: []}) {
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
export function getSourceStyle(node) {
  return node && !node.type && node.style ? node.style : "";
}

/**
 * Get component
 * @param node
 * @param index
 * @returns React component
 * @constructor
 */
export const Components = (node, index) => {
  if (IGNORE_COMPONENT_LIST.includes(node.elementType)) return null;
  if (node.elementType === "Criteria") {
    return Criteria(node, index);
  } else if (node.elementType in ComponentList) {
    return React.createElement(ComponentList[node.elementType], {
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
export const Criteria = (node, index) => {
  if (typeof CriteriaList[node.component] !== "undefined") {
    return React.createElement(CriteriaList[node.component], {
      ...node, key: node.id || `criterion-${index}`
    });
  }
  return React.createElement(
    () => <div>The criterion {node.component} has not been created yet.</div>,
    {key: index}
  );
};

/**
 * Get column
 * @param node
 * @param data
 * @param attrs
 * @param editing
 * @returns React element
 * @constructor
 */
export const Columns = (node, data = {}, attrs = {}, editing = false) => {
  const {component} = node;
  const isMultiple = isMultipleComponent(component);
  let fixedData = isMultiple ? data : extractCellModel(data);
  if (editing && typeof Editor[component] !== "undefined") {
    return React.createElement(Editor[component], {
      ...node,
      key: getComponentId(node.address),
      data: fixedData,
      attrs
    });
  } else if (typeof Static[component] !== "undefined") {
    return React.createElement(Static[component], {
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
    const className = classNames("p-cell-text", "white-space-nowrap", "p-text-truncate");
    return <span className={className} title={visibleValue}>{renderSafeHtml(visibleValue)}</span>;
  }
  const {label, value, style} = fixedData;
  const visibleValue = getFirstDefinedAndNotNullValue(label, value, "");
  const className = classNames("p-cell-text", "white-space-nowrap", "p-text-truncate", style);
  return <span className={className} title={visibleValue}>{renderSafeHtml(visibleValue)}</span>;
};
