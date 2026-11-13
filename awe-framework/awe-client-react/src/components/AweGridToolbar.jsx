import React, { useRef, useState } from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweGridColumnsDialog from "./AweGridColumnsDialog";
import AweGridQuickFilter from "../criteria/AweGridQuickFilter";
import AweGridSearch from "../criteria/AweGridSearch";
import AweGridGroupBy from "./AweGridGroupBy";
import AweGridSavedSearches from "./AweGridSavedSearches";
import { TestIds, testHook } from "../utilities/testIds";
import { getIconNode } from "../utilities";
import { getGroupingState } from "../utilities/gridGrouping";

/**
 * Toolbar of a grid: the search, the quick filters and the row grouping controls (#833) of the grid first, then a spacer,
 * the Saved searches menu (#912, saved-searches="true"), the Columns panel (a popover anchored to the Columns button, a
 * dialog or a side panel, as its columns-panel says) and the Maximize toggle. A grid hides its Columns button with
 * show-columns-button="false" and its Maximize button with show-maximize-button="false"; show-toolbar="false" hides the
 * grouping controls and both buttons: its toolbar is then shown only for its search, quick filters and saved searches,
 * and not at all when nothing is left to show. The end buttons (Group by, Saved searches, Columns and Maximize) are icon
 * only, with their text as tooltip and accessible name, unless the grid toolbar-labels lists them (group-by,
 * saved-searches, columns, maximize) or, when the grid does not set it, the application gridToolbarLabels setting asks
 * for their labels. The selection bar of the grid shares the band of the search, the quick filters, the grouping and the
 * Saved searches menu (one cell, so the band keeps its height): while rows are selected it covers them and the Columns
 * and Maximize buttons stay at the end.
 * @category Components
 * @subcategory Grid
 */
function AweGridToolbar({ id, maximized, iconMaximized, onToggleMaximize, selectionBar = null, selecting = false }) {
  const { t } = useTranslation();
  const [columnsOpen, setColumnsOpen] = useState(false);
  const columnsButton = useRef(null);
  const { address, attributes = {} } = useComponentState(id);
  // The buttons listed in the grid toolbar-labels, or all of them as the application default (gridToolbarLabels
  // setting) says when the grid does not set it
  const defaultLabels = useSelector(state => state.settings?.gridToolbarLabels);

  const { quickFilter, search, savedSearches } = attributes;
  const { showColumns, showMaximize, showGrouping } = getToolbarContents(attributes);
  if (!hasGridToolbar(attributes) || !address) {
    return null;
  }

  const { toolbarLabels } = attributes;
  const showLabel = (button) => Array.isArray(toolbarLabels) ? toolbarLabels.includes(button) : Boolean(defaultLabels);
  const columnsLabels = showLabel("columns");
  const maximizeLabels = showLabel("maximize");
  const closeColumns = () => setColumnsOpen(false);
  // The button takes the focus (some browsers do not focus a clicked button), so it gets it back when the panel closes
  const openColumns = (event) => {
    event.currentTarget.focus();
    setColumnsOpen(true);
  };
  const columnsLabel = t("GRID_COLUMNS_BUTTON");
  const maximizeLabel = t(iconMaximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
  const buttonClass = (labels) => `p-button p-component p-button-outlined p-button-sm grid-toolbar-button${labels ? "" : " p-button-icon-only"}`;
  return <div className={selecting ? "grid-toolbar is-selecting" : "grid-toolbar"} {...testHook(TestIds.gridToolbar, { owner: address.component, maximized: !!maximized })}>
    <div className="grid-toolbar-band">
      {/* Covered (hidden, out of the tab order) by the selection bar while rows are selected */}
      <div className={selecting ? "grid-toolbar-main is-covered" : "grid-toolbar-main"} inert={selecting || undefined}>
        {search && <AweGridSearch id={search} grid={address.component} />}
        {quickFilter && <AweGridQuickFilter id={quickFilter} grid={address.component} />}
        {/* Row grouping (#833): Expand all, Collapse all and Group by */}
        {showGrouping && <AweGridGroupBy address={address} attributes={attributes} labels={showLabel("group-by")} />}
        {/* The search, the quick filters and the grouping go at the start, the Saved searches, Columns and Maximize buttons at the end */}
        {(savedSearches || showColumns || showMaximize) && <span className="grid-toolbar-spacer" aria-hidden="true" />}
        {savedSearches && <AweGridSavedSearches id={address.component} labels={showLabel("saved-searches")} />}
      </div>
      {selectionBar}
    </div>
    {showColumns && <button ref={columnsButton} type="button" className={buttonClass(columnsLabels)}
      title={t("GRID_COLUMNS_TITLE")} aria-label={columnsLabel} aria-haspopup="dialog" aria-expanded={columnsOpen} onClick={openColumns}
      {...testHook(TestIds.gridColumnsButton)}>
      {getIconNode("pi-table")}{columnsLabels && <> <span className="p-button-label">{columnsLabel}</span></>}
    </button>}
    {showMaximize && <button type="button" className={buttonClass(maximizeLabels)}
      title={maximizeLabel} aria-pressed={!!maximized} onClick={onToggleMaximize}
      {...testHook(TestIds.gridMaximizeButton, { attributes: { "aria-label": maximizeLabel } })}>
      {getIconNode(iconMaximized ? "pi-window-minimize" : "pi-window-maximize")}
      {maximizeLabels && <span className="p-button-label">{maximizeLabel}</span>}
    </button>}
    {columnsOpen && <AweGridColumnsDialog address={address} attributes={attributes} visible
      anchor={columnsButton} onHide={closeColumns} />}
  </div>;
}

AweGridToolbar.propTypes = {
  id: PropTypes.string,
  maximized: PropTypes.bool,
  iconMaximized: PropTypes.bool,
  onToggleMaximize: PropTypes.func,
  selectionBar: PropTypes.node,
  selecting: PropTypes.bool
};

/**
 * What the toolbar of a grid shows besides its search, quick filters and saved searches: show-toolbar="false" hides the
 * grouping controls and the Columns and Maximize buttons
 * @param {object} attributes Grid attributes
 * @returns {{showColumns: boolean, showMaximize: boolean, showGrouping: boolean}} Shown parts
 */
function getToolbarContents(attributes) {
  const showButtons = attributes.showToolbar !== false;
  const { active, selector } = getGroupingState(attributes);
  return {
    showColumns: showButtons && attributes.showColumnsButton !== false,
    showMaximize: showButtons && attributes.showMaximizeButton !== false,
    showGrouping: Boolean(showButtons && (active || selector))
  };
}

/**
 * Check if a grid shows its toolbar: a grid that is explicitly hidden hides its toolbar too, and a toolbar with nothing
 * to show is not rendered (the selection bar of the grid then covers its column headers)
 * @param {object} attributes Grid attributes
 * @returns {boolean} The toolbar is shown
 */
export function hasGridToolbar(attributes = {}) {
  const { quickFilter, search, savedSearches } = attributes;
  const { showColumns, showMaximize, showGrouping } = getToolbarContents(attributes);
  return Boolean(search || quickFilter || savedSearches || showGrouping || showColumns || showMaximize) &&
    attributes.visible !== false;
}

export default AweGridToolbar;
