import React from "react";
import PropTypes from "prop-types";
import {Skeleton} from "primereact/skeleton";
import {getRowNumberWidthStyle, getWidthStyle, SELECTION_COLUMN_WIDTH} from "../utilities/grid";
import {TestIds, testHook} from "../utilities/testIds";
import AweLoader from "./AweLoader";
import "./AweSkeleton.less";
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
 * A grid that loads again while it has rows and the skeleton loader (the default one) shows them dimmed instead of the
 * skeleton: the rows stay where they are until the new data arrives. Any other loading icon keeps its icon
 * @param {boolean} loading The grid is loading
 * @param {string} [loadingIcon] Loading icon of the grid (see getGridIconLoading)
 * @param {number} rowCount Rows the grid has
 * @returns {boolean} True when the rows are shown dimmed
 */
export function isGridReloading(loading, loadingIcon, rowCount) {
  return !!loading && loadingIcon === SKELETON && rowCount > 0;
}

/**
 * Loading icon of a grid: the loading test hook of a dimmed reload (it draws nothing), or the icon of the loader
 * @param {boolean} reloading The rows are shown dimmed
 * @param {string} [icon] Loading icon name
 * @returns {JSX.Element} Loading icon
 */
export function gridLoadingIcon(reloading, icon) {
  return reloading
    ? <span className="grid-reload" aria-hidden="true" {...testHook(TestIds.gridLoader)} />
    : <AweLoader icon={icon} rootProps={testHook(TestIds.gridLoader)} />;
}

/**
 * Placeholder rows shown in the body of a grid while its rows are loading: one block per visible column, preceded by
 * one for each pre-column the grid shows (selection, then row number, as in the header), so the header stays visible
 * and the placeholders follow its columns
 * @category Components
 * @subcategory Grid
 * @param {object[]} columns Column model of the grid
 * @param {number} [rows] Page size of the grid (capped to ten rows)
 * @param {boolean} [multiselect] The grid shows the selection column
 * @param {boolean} [rowNumbers] The grid shows the row number column
 * @param {number} [first] Index of the first row of the page
 * @returns {JSX.Element} Grid skeleton
 */
function AweGridSkeleton({columns = [], rows, multiselect = false, rowNumbers = false, first = 0}) {
  const visibleColumns = columns.filter(col => !col.hidden);
  const rowCount = Math.max(1, Math.min(rows || MAX_SKELETON_ROWS, MAX_SKELETON_ROWS));
  const preColumns = [
    multiselect && {name: "multiselect", style: getWidthStyle(null, null, SELECTION_COLUMN_WIDTH)},
    rowNumbers && {name: "row-number", style: getRowNumberWidthStyle(first, rows)}
  ].filter(Boolean);
  return <div className="grid-skeleton" aria-hidden="true" {...testHook(TestIds.gridLoader)}>
    {Array.from({length: rowCount}, (_, row) =>
      <div key={row} className="grid-skeleton-row">
        {preColumns.map(({name, style}) =>
          <div key={name} className="grid-skeleton-cell" style={style}>
            <Skeleton />
          </div>)}
        {visibleColumns.map(({name, charlength = null, width = null}) =>
          <div key={name} className="grid-skeleton-cell" style={getWidthStyle(charlength, width)}>
            <Skeleton />
          </div>)}
      </div>)}
  </div>;
}

AweGridSkeleton.propTypes = {
  columns: PropTypes.array,
  rows: PropTypes.number,
  multiselect: PropTypes.bool,
  rowNumbers: PropTypes.bool,
  first: PropTypes.number
};

export default AweGridSkeleton;
