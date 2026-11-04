import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import { useColumnFilters } from "../hooks/useColumnFilters";
import { getIconNode, translateLabel } from "../utilities";
import { summarizeColumnFilter } from "../utilities/columnFilters";
import { TestIds, testHook } from "../utilities/testIds";
import "./AweWindowAdvanced.less";
import "./AweGridColumnFilter.less";

/**
 * Give the focus to the funnel of a column of a grid (the first one without column), as its chip is gone
 * @param {string} grid Grid identifier
 * @param {string} [column] Column name
 */
const focusFunnel = (grid, column) => {
  const quote = (value) => `"${String(value).replace(/["\\]/g, "\\$&")}"`;
  const funnel = `[data-testid=${quote(TestIds.gridColumnFilter)}][data-testid-owner=${quote(grid)}]`;
  document.querySelector(column ? `${funnel}[column-id=${quote(column)}]` : funnel)?.focus();
};

/**
 * Column filters in effect of a grid (#912, S16), under its toolbar: "Filtered by", a removable chip per filtered column
 * ("<label>: <summary>", also for a hidden column) and Clear all. Removing a chip or Clear all reloads the grid. It has
 * the look of the filters in effect of a window (#790) and is not shown while no column is filtered.
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnFilterChips({ id }) {
  const { t } = useTranslation();
  const { attributes = {} } = useComponentState(id) || {};
  const { columnFilters, labels, removeColumnFilter, clearColumnFilters } = useColumnFilters(id);
  if (columnFilters.length === 0 || attributes.visible === false) {
    return null;
  }
  const columns = attributes.columnModel || [];
  const title = t("SCREEN_TEXT_FILTERED_BY");
  return <div className="grid-column-filter-chips" role="group" aria-label={title}
    {...testHook(TestIds.gridColumnFilterChips, { owner: id })}>
    <span className="window-filter-chips-title">{title}</span>
    {columnFilters.map(filter => {
      const column = columns.find(item => item.name === filter.id);
      const label = translateLabel(column?.label || filter.id, t);
      const summary = summarizeColumnFilter(filter, t, labels[filter.id]);
      return <span key={filter.id} className="window-filter-chip"
        {...testHook(TestIds.gridColumnFilterChip, { owner: id, attributes: { "column-id": filter.id } })}>
        <span className="window-filter-chip-text">{label}: <b>{summary}</b></span>
        <button type="button" className="window-filter-chip-remove p-link"
          aria-label={`${t("SCREEN_TEXT_REMOVE_FILTER")} ${label}: ${summary}`}
          onClick={() => {
            removeColumnFilter(filter.id);
            focusFunnel(id, filter.id);
          }}
          {...testHook(TestIds.gridColumnFilterChipRemove, { owner: id })}>
          {getIconNode("lucide:x", { "aria-hidden": "true" })}
        </button>
      </span>;
    })}
    <button type="button" className="window-filter-clear-all p-button p-component p-button-text p-button-sm"
      onClick={() => {
        clearColumnFilters();
        focusFunnel(id);
      }} {...testHook(TestIds.gridColumnFilterClearAll, { owner: id })}>
      {t("SCREEN_TEXT_CLEAR_ALL_FILTERS")}
    </button>
  </div>;
}

AweGridColumnFilterChips.propTypes = {
  id: PropTypes.string
};

export default AweGridColumnFilterChips;
