import React, { useCallback, useRef } from "react";
import { Button } from "primereact/button";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";
import { getIconCode, translateLabel } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";
import "./AweGridEmptyState.less";

export const DEFAULT_EMPTY_ICON = "inbox";
export const DEFAULT_EMPTY_TITLE = "SCREEN_TEXT_GRID_EMPTY_TITLE";
export const DEFAULT_EMPTY_TEXT = "SCREEN_TEXT_GRID_EMPTY_TEXT";

/**
 * A grid with an initial load is not known to be empty until its data arrives or a load finishes
 * @param {object} attributes Grid attributes
 * @param {boolean} modelLoaded The data of the grid has arrived
 * @returns {boolean} The grid can show its empty state
 */
export const isLoaded = (attributes, modelLoaded) => !attributes.initialLoad || modelLoaded || attributes.loading === false;

/**
 * Empty state of a grid (grid without rows): icon, title, text and an optional action.
 * The action refers to a button of the grid and, like in the AngularJS engine, it clicks that button
 * @param {object} props Component properties
 * @param {object} props.attributes Grid attributes (emptyIcon, emptyTitle, emptyText, emptyAction and buttonModel)
 * @returns {JSX.Element} Empty state
 */
function AweGridEmptyState({ attributes }) {
  const { emptyIcon, emptyTitle, emptyText, emptyAction, buttonModel = [] } = attributes;
  const { t } = useTranslation();
  const actionButton = emptyAction ? buttonModel.find(button => button.id === emptyAction) : undefined;

  const ref = useRef(null);

  // The button is looked up inside the grid that owns this empty state, never in the whole document
  const onAction = useCallback(() => {
    const grid = ref.current?.closest("[grid-id], [tree-grid-id]");
    Array.from(grid?.querySelectorAll("button[id]") ?? []).find(button => button.id === emptyAction)?.click();
  }, [emptyAction]);

  return <div ref={ref} className="grid-empty-state" {...testHook(TestIds.gridEmpty)}>
    {getIconCode(emptyIcon || DEFAULT_EMPTY_ICON, "grid-empty-icon")}
    <div className="grid-empty-title" {...testHook(TestIds.gridEmptyTitle)}>
      {translateLabel(emptyTitle || DEFAULT_EMPTY_TITLE, t)}
    </div>
    <div className="grid-empty-text" {...testHook(TestIds.gridEmptyText)}>
      {translateLabel(emptyText || DEFAULT_EMPTY_TEXT, t)}
    </div>
    {actionButton && <Button
      type="button"
      className="grid-empty-action p-button-outlined"
      icon={getIconCode(actionButton.icon, "p-button-icon p-c p-button-icon-left")}
      label={translateLabel(actionButton.label, t)}
      onClick={onAction}
      {...testHook(TestIds.gridEmptyAction)}
    />}
  </div>;
}

AweGridEmptyState.propTypes = {
  attributes: PropTypes.object.isRequired
};

export default AweGridEmptyState;
