import React from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {renderRowDetail} from "../utilities/gridRowDetail";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Identifier of the detail of a row, referenced by the "aria-controls" of its toggle
 * @param {string} gridId Grid identifier
 * @param {string|number} rowId Row identifier
 * @returns {string} Detail element id
 */
export function getRowDetailId(gridId, rowId) {
  return `${gridId}-row-detail-${rowId}`;
}

/**
 * Button that shows or hides the detail of a row. It is a native button, so it is reached with Tab and activated with
 * Enter and Space, and PrimeReact does not select the row when it is clicked
 * @category Components
 * @subcategory Grid
 * @param {string} gridId Grid identifier
 * @param {string|number} rowId Row identifier
 * @param {boolean} expanded The detail of the row is shown
 * @param {function} onToggle Called with the row identifier when the button is clicked
 * @returns {JSX.Element} Row detail toggle
 */
export function AweGridRowDetailToggle({gridId, rowId, expanded = false, onToggle}) {
  const {t} = useTranslation();
  return <button type="button" className="p-row-toggler p-link grid-row-detail-toggle"
                 aria-expanded={expanded} aria-controls={getRowDetailId(gridId, rowId)}
                 aria-label={t(expanded ? "SCREEN_TEXT_GRID_ROW_DETAIL_HIDE" : "SCREEN_TEXT_GRID_ROW_DETAIL_SHOW")}
                 onClick={() => onToggle(rowId)}
                 {...testHook(TestIds.gridRowDetailToggle, {attributes: {"row-id": rowId}})}>
    <i className={expanded ? "pi pi-chevron-down" : "pi pi-chevron-right"} aria-hidden="true"/>
  </button>;
}

AweGridRowDetailToggle.propTypes = {
  gridId: PropTypes.string,
  rowId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  expanded: PropTypes.bool,
  onToggle: PropTypes.func.isRequired
};

/**
 * Detail of a row, shown under it across the whole grid: the row detail template filled with the data of the row,
 * rendered as plain text with its line breaks, or the components of the row detail (its children) when it has them.
 * The style classes of the row detail are added to the area, and the components mode adds "grid-row-detail-components"
 * @category Components
 * @subcategory Grid
 * @param {string} gridId Grid identifier
 * @param {object} rowDetail Row detail of the grid ({template, style} or {component, style})
 * @param {object} rowData Row data
 * @param {node} children Components of the detail of the row (components mode)
 * @returns {JSX.Element} Row detail
 */
function AweGridRowDetail({gridId, rowDetail = {}, rowData = {}, children = null}) {
  return <div id={getRowDetailId(gridId, rowData.id)}
              className={classNames("grid-row-detail", {"grid-row-detail-components": !!rowDetail.component}, rowDetail.style)}
              {...testHook(TestIds.gridRowDetail, {attributes: {"row-id": rowData.id}})}>
    {rowDetail.component ? children : renderRowDetail(rowDetail.template, rowData)}
  </div>;
}

AweGridRowDetail.propTypes = {
  gridId: PropTypes.string,
  rowDetail: PropTypes.object,
  rowData: PropTypes.object,
  children: PropTypes.node
};

export default AweGridRowDetail;
