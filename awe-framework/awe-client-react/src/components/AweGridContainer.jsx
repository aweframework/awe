import React, {useCallback, useEffect, useRef, useState} from "react";
import {useScreenSize} from "../hooks/useSizeRegistry";
import PropTypes from "prop-types";
import {getMoveRowKeyPosition} from "../utilities/grid";

/**
 * AWE Grid Container component (functional)
 * @category Components
 * @subcategory Grid
 */
function AweGridContainer(props) {
  const { onKeyCancelRow, onKeySaveRow, onKeyMoveRow, onContextMenu: onContextMenuProp, loading = false, children } = props;
  const containerRef = useRef(null);
  const [containerHeight, setContainerHeight] = useState(0);
  const screenSize = useScreenSize();

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
  }, []);

  useEffect(() => {
    setContainerHeight(undefined);
  }, [screenSize?.height, screenSize?.current]);

  useEffect(() => {
    if (containerHeight === undefined) {
      setContainerHeight(containerRef?.current?.clientHeight);
    }
  }, [containerHeight]);

  return (
    <div className="grid-container expand expandible-vertical" ref={containerRef} aria-busy={loading ? "true" : undefined}>
      <div
        className="grid-contents expand expandible-vertical"
        role="grid-container"
        onKeyDown={checkKey}
        onContextMenu={contextMenu}
      >
        {children}
      </div>
    </div>
  );
}

AweGridContainer.propTypes = {
  children: PropTypes.node,
  loading: PropTypes.bool,
  onContextMenu: PropTypes.any,
  onKeyCancelRow: PropTypes.any,
  onKeyMoveRow: PropTypes.any,
  onKeySaveRow: PropTypes.any,
};

export default AweGridContainer;
