import React, { useId } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweButton from "./AweButton";
import { isSelectionButton } from "../utilities/grid";
import { getIconNode, translateLabel } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";

// The destructive action of the bar is the button with the danger style of the screen (style="btn-danger")
const DANGER_STYLE = /(^|\s)btn-danger(\s|$)/;

/**
 * Value of an option or a search as text: an empty value (null or undefined) is the empty text
 * @param {*} value Value
 * @returns {string} Value as text
 */
const asText = (value) => (value === null || value === undefined ? "" : String(value));

/**
 * Label of the option a quick filter has applied (the chosen one, or the first one when none is chosen): none for the
 * "all" option, the one with the empty value, which does not filter the grid
 * @param {object} model Model of the quick filter
 * @param {function} t Translate function
 * @returns {string} Label, the empty text without filter
 */
const quickFilterLabel = (model = {}, t) => {
  const values = model.values || [];
  const current = values.find(option => option.selected) || values[0];
  return current && asText(current.value) !== "" ? translateLabel(current.label, t) : "";
};

/**
 * Text of a grid search: its selected value, the empty text without it
 * @param {object} model Model of the search
 * @returns {string} Text
 */
const searchText = (model = {}) => asText((model.values || []).find(option => option.selected)?.value);

/**
 * Number of selected rows of a grid and whether it has a selection bar (selection buttons, selection="true")
 * @param {object} state State of the grid
 * @returns {{count: number, hasBar: boolean}} Selection of the grid
 */
export const getGridSelection = ({ attributes = {}, model = {} } = {}) => ({
  count: (model.values || []).filter(row => row.selected).length,
  hasBar: (attributes.buttonModel || []).some(isSelectionButton)
});

/**
 * Selection bar of a grid: while there are selected rows it takes the band of the grid toolbar (over its search, quick
 * filters, saved searches and grouping, the Columns and Maximize buttons stay at its end) or, in a grid without
 * toolbar, the row of the column headers, leaving the select-all checkbox: the rows never move. It shows a clear button
 * (as Esc in the grid), the number of selected rows, the quick filter and the search the selection was made with and
 * the selection buttons of the grid (selection="true"), which are not in its footer. They are small ghost buttons, the
 * one with style="btn-danger" is the danger one. The bar is a region named by its number, which is announced politely;
 * while there is no selected row it is hidden and its buttons are not rendered. Grids without selection buttons have
 * no bar.
 * @category Components
 * @subcategory Grid
 */
function AweGridSelectionBar({ id, onClear, placement = "toolbar", style }) {
  const { t } = useTranslation();
  const countId = useId();
  const state = useComponentState(id);
  const { address, attributes = {} } = state;
  const quickFilter = useComponentState(attributes.quickFilter);
  const search = useComponentState(attributes.search);
  const { count, hasBar } = getGridSelection(state);
  if (!hasBar || !address) {
    return null;
  }

  const buttons = (attributes.buttonModel || []).filter(isSelectionButton);
  const text = count > 0 ? t(count === 1 ? "SCREEN_TEXT_GRID_SELECTED_ROW" : "SCREEN_TEXT_GRID_SELECTED_ROWS", { count }) : "";
  const context = [quickFilterLabel(quickFilter?.model, t), searchText(search?.model)];
  const clearLabel = `${t("SCREEN_TEXT_GRID_CLEAR_SELECTION")} (Esc)`;
  // Esc on the bar (its clear button, its selection buttons) clears the selection, as in the rows
  const onKeyDown = (event) => {
    if (event.key === "Escape" && count > 0) {
      event.preventDefault();
      event.stopPropagation();
      onClear && onClear();
    }
  };
  return <div className={`grid-selection-bar grid-selection-bar-${placement}${count > 0 ? " is-visible" : ""}`} role="region"
    aria-labelledby={countId} data-count={count} style={style} onKeyDown={onKeyDown}
    {...testHook(TestIds.gridSelectionBar, { owner: address.component })}>
    {count > 0 && <button type="button" className="grid-selection-bar-clear" aria-label={clearLabel} title={clearLabel}
      onClick={onClear} {...testHook(TestIds.gridSelectionClear)}>
      {getIconNode("times", { "aria-hidden": "true" })}
    </button>}
    <span className="grid-selection-bar-icon" aria-hidden="true">{getIconNode("check-circle-o")}</span>
    <span id={countId} className="grid-selection-bar-count" aria-live="polite" {...testHook(TestIds.gridSelectionCount)}>{text}</span>
    {count > 0 && context.some(Boolean) && <span className="grid-selection-bar-context" title={context.filter(Boolean).join(" · ")}
      {...testHook(TestIds.gridSelectionContext)}>
      <span className="p-hidden-accessible">{`${t("SCREEN_TEXT_FILTERED_BY")} `}</span>
      <span className="grid-selection-bar-context-icon" aria-hidden="true">{getIconNode("filter")}</span>
      {context[0] && <span className="grid-selection-bar-context-filter">{context[0]}</span>}
      {context[0] && context[1] && " "}
      {context[1] && <span className="grid-selection-bar-context-search">{`“${context[1]}”`}</span>}
    </span>}
    <span className="grid-selection-bar-spacer" />
    <div className="grid-selection-bar-actions">
      {count > 0 && buttons.map(button => <AweButton key={button.id} id={button.id} className="p-button-sm"
        variant={DANGER_STYLE.test(button.style || "") ? "p-button-danger" : "p-button-text"} />)}
    </div>
  </div>;
}

AweGridSelectionBar.propTypes = {
  id: PropTypes.string,
  onClear: PropTypes.func,
  placement: PropTypes.oneOf(["toolbar", "header"]),
  style: PropTypes.object
};

export default AweGridSelectionBar;
