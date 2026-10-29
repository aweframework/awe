import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweButton from "./AweButton";
import { isSelectionButton } from "../utilities/grid";
import { getIconNode } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";

// The destructive action of the bar is the button with the danger style of the screen (style="btn-danger")
const DANGER_STYLE = /(^|\s)btn-danger(\s|$)/;

/**
 * Selection bar of a grid: between the toolbar and the table while there are selected rows, with their number and the
 * selection buttons of the grid (selection="true"), which are not in its footer. They are small ghost buttons, the one
 * with style="btn-danger" is the danger one. The bar is a polite live region, so the number is announced; while there
 * is no selected row it is hidden and its buttons are not rendered. Grids without selection buttons have no bar.
 * @category Components
 * @subcategory Grid
 */
function AweGridSelectionBar({ id }) {
  const { t } = useTranslation();
  const { address, attributes = {}, model = {} } = useComponentState(id);
  const buttons = (attributes.buttonModel || []).filter(isSelectionButton);
  if (buttons.length === 0 || !address) {
    return null;
  }

  const count = (model.values || []).filter(row => row.selected).length;
  const text = count > 0 ? t(count === 1 ? "SCREEN_TEXT_GRID_SELECTED_ROW" : "SCREEN_TEXT_GRID_SELECTED_ROWS", { count }) : "";
  return <div className={count > 0 ? "grid-selection-bar is-visible" : "grid-selection-bar"} aria-live="polite"
    data-count={count} {...testHook(TestIds.gridSelectionBar, { owner: address.component })}>
    <span className="grid-selection-bar-icon" aria-hidden="true">{getIconNode("check-circle-o")}</span>
    <span className="grid-selection-bar-count" {...testHook(TestIds.gridSelectionCount)}>{text}</span>
    <span className="grid-selection-bar-spacer" />
    <div className="grid-selection-bar-actions">
      {count > 0 && buttons.map(button => <AweButton key={button.id} id={button.id} className="p-button-sm"
        variant={DANGER_STYLE.test(button.style || "") ? "p-button-danger" : "p-button-text"} />)}
    </div>
  </div>;
}

AweGridSelectionBar.propTypes = {
  id: PropTypes.string
};

export default AweGridSelectionBar;
