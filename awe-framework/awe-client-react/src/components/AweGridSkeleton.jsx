import React from "react";
import PropTypes from "prop-types";
import {Skeleton} from "primereact/skeleton";
import {getWidthStyle} from "../utilities/grid";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweGridSkeleton.less";

export const SKELETON = "skeleton";
const MAX_SKELETON_ROWS = 10;

/**
 * Loading icon of a grid: its own icon-loading, or the application default (gridIconLoading setting) when it has none
 * @param {string} [iconLoading] icon-loading attribute of the grid
 * @param {object} [settings] Application settings
 * @returns {string} Loading icon name
 */
export function getGridIconLoading(iconLoading, settings = {}) {
  return iconLoading || settings.gridIconLoading;
}

/**
 * Placeholder rows shown in the body of a grid while its rows are loading: one block per visible column, so the
 * header stays visible and the placeholders follow its columns
 * @category Components
 * @subcategory Grid
 * @param {object[]} columns Column model of the grid
 * @param {number} [rows] Page size of the grid (capped to ten rows)
 * @returns {JSX.Element} Grid skeleton
 */
function AweGridSkeleton({columns = [], rows}) {
  const visibleColumns = columns.filter(col => !col.hidden);
  const rowCount = Math.max(1, Math.min(rows || MAX_SKELETON_ROWS, MAX_SKELETON_ROWS));
  return <div className="grid-skeleton" aria-hidden="true" {...testHook(TestIds.gridLoader)}>
    {Array.from({length: rowCount}, (_, row) =>
      <div key={row} className="grid-skeleton-row">
        {visibleColumns.map(({name, charlength = null, width = null}) =>
          <div key={name} className="grid-skeleton-cell" style={getWidthStyle(charlength, width)}>
            <Skeleton />
          </div>)}
      </div>)}
  </div>;
}

AweGridSkeleton.propTypes = {
  columns: PropTypes.array,
  rows: PropTypes.number
};

export default AweGridSkeleton;
