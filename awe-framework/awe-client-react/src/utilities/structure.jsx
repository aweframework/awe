import React from 'react';
import {ComponentList, IGNORE_COMPONENT_LIST} from "../components/AweComponents";
import {CriteriaList} from "../criteria/AweCriteria";
import {getComponentId} from "./components";
import {Editor, Static} from "../columns/AweColumns";
import parse from "html-react-parser";
import {extractCellModel} from "./grid";
import {getFirstDefinedAndNotNullValue} from "./general";

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
 * Retrieves a list of child components from the provided node.
 *
 * @param {Object} node - The source node containing an element list.
 * @return {Array|null} Returns an array of child components generated from the node's element list, or null if the node is falsy.
 */
export function getSourceChildren(node= {elementList: []}) {
  return node ? node.elementList.map((child, index) => Components(child, index)) : null;
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
 * @param editing
 * @returns React element
 * @constructor
 */
export const Columns = (node, data, editing) => {
  const {component} = node;
  let fixedData = extractCellModel(data);
  if (editing && typeof Editor[component] !== "undefined") {
    return React.createElement(Editor[component], {
      ...node,
      key: getComponentId(node.address),
      data: fixedData
    });
  } else if (typeof Static[component] !== "undefined") {
    return React.createElement(Static[component], {
      ...node,
      key: getComponentId(node.address),
      data: fixedData
    });
  }
  const visibleValue = getFirstDefinedAndNotNullValue(fixedData.label, fixedData.value, "");
  return <span className="p-cell-text white-space-nowrap p-text-truncate" title={visibleValue}>{parse(String(visibleValue))}</span>;
};
