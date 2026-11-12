import React, {useCallback, useMemo} from "react";
import useMaximize from "../hooks/useMaximize";
import PropTypes from "prop-types";
import AweGridCellTooltip from "./AweGridCellTooltip";
import {MOVE_ROW_SHORTCUTS} from "../utilities/grid";
import {ANY_MODIFIERS, createKeyHandler} from "../utilities/keyboard";
import AweGridToolbar from "./AweGridToolbar";
import AweGridSelectionBar from "./AweGridSelectionBar";
import AweGridColumnFilterChips from "./AweGridColumnFilterChips";
import {classNames} from "../utilities/components";
import {TestAttributes} from "../utilities/testIds";
import "./AweSkeleton.less";

/**
 * AWE Grid Container component (functional)
 * @category Components
 * @subcategory Grid
 */
function AweGridContainer(props) {
  const { id, onKeyCancelRow, onKeySaveRow, onKeyMoveRow, onContextMenu: onContextMenuProp, loading = false, reloading = false, rowHeight, children } = props;
  // The grid grows over the maximize target like a window
  const {ref: containerRef, iconMaximized, style, className, toggle: toggleMaximized} = useMaximize();

  // Minimum row height of the grid in pixels (the rows read it in the stylesheet), as the AngularJS client does
  const rowHeightPx = parseInt(rowHeight, 10);
  const containerStyle = rowHeightPx > 0 ? { ...style, "--grid-row-height": `${rowHeightPx}px` } : style;

  // Escape and Enter work whatever modifiers are pressed. Alt+arrows and Alt+Home/End move the selected row when the grid allows it
  const checkKey = useMemo(() => createKeyHandler([
    ...(onKeyMoveRow ? MOVE_ROW_SHORTCUTS.map(({shortcut, position}) => ({
      shortcut,
      handler: (e) => {
        e.preventDefault();
        onKeyMoveRow(position);
      }
    })) : []),
    {shortcut: {key: "Escape", ...ANY_MODIFIERS}, handler: () => onKeyCancelRow && onKeyCancelRow()},
    {
      shortcut: {key: "Enter", ...ANY_MODIFIERS},
      handler: () => {
        document.body.focus();
        setTimeout(() => onKeySaveRow && onKeySaveRow(), 100);
      }
    }
  ]), [onKeyCancelRow, onKeySaveRow, onKeyMoveRow]);

  const contextMenu = useCallback((e) => {
    onContextMenuProp && onContextMenuProp({ originalEvent: e, data: null, index: -1 });
  }, [onContextMenuProp]);

  return (
    <div className={classNames("grid-container", "expand", "expandible-vertical", {"grid-reloading": reloading, "grid-row-height": rowHeightPx > 0}, className)} ref={containerRef} style={containerStyle}
      aria-busy={loading ? "true" : undefined}
      {...(id ? {[TestAttributes.maximized]: String(iconMaximized)} : {})}>
      {id && <AweGridToolbar id={id} maximized={iconMaximized} iconMaximized={iconMaximized} onToggleMaximize={toggleMaximized} />}
      {id && <AweGridColumnFilterChips id={id} />}
      {id && <AweGridSelectionBar id={id} />}
      <div
        className="grid-contents expand expandible-vertical"
        role="grid-container"
        onKeyDown={checkKey}
        onContextMenu={contextMenu}
      >
        {children}
      </div>
      <AweGridCellTooltip containerRef={containerRef} />
    </div>
  );
}

AweGridContainer.propTypes = {
  id: PropTypes.string,
  children: PropTypes.node,
  loading: PropTypes.bool,
  onContextMenu: PropTypes.any,
  reloading: PropTypes.bool,
  rowHeight: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onKeyCancelRow: PropTypes.any,
  onKeyMoveRow: PropTypes.any,
  onKeySaveRow: PropTypes.any,
};

export default AweGridContainer;
