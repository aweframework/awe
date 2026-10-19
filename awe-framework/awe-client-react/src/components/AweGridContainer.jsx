import React, {useCallback, useEffect, useState} from "react";
import {useScreenSize} from "../hooks/useSizeRegistry";
import useMaximize from "../hooks/useMaximize";
import PropTypes from "prop-types";
import AweGridCellTooltip from "./AweGridCellTooltip";
import {getMoveRowKeyPosition} from "../utilities/grid";
import AweGridToolbar from "./AweGridToolbar";
import {classNames} from "../utilities/components";
import {TestAttributes} from "../utilities/testIds";
import "./AweSkeleton.less";

/**
 * AWE Grid Container component (functional)
 * @category Components
 * @subcategory Grid
 */
function AweGridContainer(props) {
  const { id, onKeyCancelRow, onKeySaveRow, onKeyMoveRow, onContextMenu: onContextMenuProp, loading = false, reloading = false, children } = props;
  const [containerHeight, setContainerHeight] = useState(0);
  const [resizeCount, setResizeCount] = useState(0);
  const screenSize = useScreenSize();
  // The grid grows over the maximize target like a window, and it is measured again when the animation ends
  const onResize = useCallback(() => setResizeCount(count => count + 1), []);
  const {ref: containerRef, iconMaximized, style, className, toggle: toggleMaximized} = useMaximize({onResize});

  const checkKey = useCallback((e) => {
    const movePosition = onKeyMoveRow ? getMoveRowKeyPosition(e) : null;
    if (movePosition) {
      e.preventDefault();
      onKeyMoveRow(movePosition);
      return;
    }
    switch (e.key) {
      case "Escape":
        onKeyCancelRow && onKeyCancelRow();
        break;
      case "Enter":
        document.body.focus();
        setTimeout(() => onKeySaveRow && onKeySaveRow(), 100);
        break;
      default:
        break;
    }
  }, [onKeyCancelRow, onKeySaveRow, onKeyMoveRow]);

  const contextMenu = useCallback((e) => {
    onContextMenuProp && onContextMenuProp({ originalEvent: e, data: null, index: -1 });
  }, [onContextMenuProp]);

  useEffect(() => {
    setContainerHeight(containerRef?.current?.clientHeight || 0);
  }, [containerRef]);

  // The grid is measured again when the window changes and when the grid is maximized or restored (end of the animation)
  useEffect(() => {
    setContainerHeight(undefined);
  }, [screenSize?.height, screenSize?.current, resizeCount]);

  useEffect(() => {
    if (containerHeight === undefined) {
      setContainerHeight(containerRef?.current?.clientHeight);
    }
  }, [containerHeight, containerRef]);

  return (
    <div className={classNames("grid-container", "expand", "expandible-vertical", {"grid-reloading": reloading}, className)} ref={containerRef} style={style}
      aria-busy={loading ? "true" : undefined}
      {...(id ? {[TestAttributes.maximized]: String(iconMaximized)} : {})}>
      {id && <AweGridToolbar id={id} maximized={iconMaximized} iconMaximized={iconMaximized} onToggleMaximize={toggleMaximized} />}
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
  onKeyCancelRow: PropTypes.any,
  onKeyMoveRow: PropTypes.any,
  onKeySaveRow: PropTypes.any,
};

export default AweGridContainer;
