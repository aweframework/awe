import React from 'react';
import {ComponentList, IGNORE_COMPONENT_LIST} from "../components/AweComponents";
import {CriteriaList} from "../criteria/AweCriteria";
import {getComponentId} from "./components";
import {Editor, Static} from "../columns/AweColumns";
import parse from "html-react-parser";
import {extractCellModel} from "./grid";
import {getFirstDefinedAndNotNullValue} from "./general";

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
