import React, { useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import AweGridColumnsDialog from "./AweGridColumnsDialog";
import { TestIds, testHook } from "../utilities/testIds";
import { getIconNode } from "../utilities";

/**
 * Toolbar of a grid: the Columns dialog and the Maximize toggle. A grid opts out with show-toolbar="false".
 * @category Components
 * @subcategory Grid
 */
function AweGridToolbar({ id, maximized, iconMaximized, onToggleMaximize }) {
  const { t } = useTranslation();
  const [columnsOpen, setColumnsOpen] = useState(false);
  const { address, attributes = {} } = useComponentState(id);

  // A grid that is explicitly hidden hides its toolbar too
  if (attributes.showToolbar === false || attributes.visible === false || !address) {
    return null;
  }

  const maximizeLabel = t(iconMaximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
  return <div className="grid-toolbar" {...testHook(TestIds.gridToolbar, { owner: address.component, maximized: !!maximized })}>
    <button type="button" className="p-button p-component p-button-outlined p-button-sm grid-toolbar-button"
      title={t("GRID_COLUMNS_TITLE")} onClick={() => setColumnsOpen(true)}
      {...testHook(TestIds.gridColumnsButton)}>
      {getIconNode("pi-table")} <span className="p-button-label">{t("GRID_COLUMNS_BUTTON")}</span>
    </button>
    <button type="button" className="p-button p-component p-button-outlined p-button-sm grid-toolbar-button"
      title={maximizeLabel} aria-pressed={!!maximized} onClick={onToggleMaximize}
      {...testHook(TestIds.gridMaximizeButton, { attributes: { "aria-label": maximizeLabel } })}>
      {getIconNode(iconMaximized ? "pi-window-minimize" : "pi-window-maximize")}
      <span className="p-button-label">{maximizeLabel}</span>
    </button>
    {columnsOpen && <AweGridColumnsDialog address={address} attributes={attributes} visible
      onHide={() => setColumnsOpen(false)} />}
  </div>;
}

AweGridToolbar.propTypes = {
  id: PropTypes.string,
  maximized: PropTypes.bool,
  iconMaximized: PropTypes.bool,
  onToggleMaximize: PropTypes.func
};

export default AweGridToolbar;
