import React, {useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from "react";
import {useDispatch} from "react-redux";
import useMaximize from "../hooks/useMaximize";
import PropTypes from "prop-types";
import AweGridCellTooltip from "./AweGridCellTooltip";
import {MOVE_ROW_SHORTCUTS} from "../utilities/grid";
import {ANY_MODIFIERS, createKeyHandler} from "../utilities/keyboard";
import AweGridToolbar, {hasGridToolbar} from "./AweGridToolbar";
import AweGridSelectionBar, {getGridSelection} from "./AweGridSelectionBar";
import AweGridColumnFilterChips from "./AweGridColumnFilterChips";
import {useComponentState} from "../hooks/useComponentState";
import {addActionsTop} from "../redux/actions/actions";
import {classNames} from "../utilities/components";
import {TestAttributes, TestIds} from "../utilities/testIds";
import "./AweSkeleton.less";

const byTestId = (testId) => `[${TestAttributes.testId}="${testId}"]`;

/**
 * Give the focus back to the grid once its selection is cleared: to its search or, without search, to its select-all
 * checkbox
 * @param {HTMLElement} container Grid container
 */
const focusAfterClear = (container) => {
  const target = container?.querySelector(`${byTestId(TestIds.gridSearch)} input`) ||
    container?.querySelector(`${byTestId(TestIds.gridHeaderCheckbox)} input`);
  target?.focus();
};

/**
 * Place of the selection bar of a grid without toolbar: over the row of its column headers, from the end of the
 * select-all checkbox cell, with the height of the headers. It is measured (and measured again when the grid resizes)
 * while the bar is shown
 * @param {object} contentsRef Reference to the contents of the grid (the table)
 * @param {boolean} active The bar is over the headers
 * @returns {object|undefined} Style of the bar
 */
function useHeaderCover(contentsRef, active) {
  const [cover, setCover] = useState(undefined);
  useLayoutEffect(() => {
    const contents = contentsRef.current;
    if (!active || !contents) {
      return undefined;
    }
    const measure = () => {
      const header = contents.querySelector("thead");
      if (!header) {
        return;
      }
      const origin = contents.getBoundingClientRect();
      const headerBox = header.getBoundingClientRect();
      const checkbox = header.querySelector(byTestId(TestIds.gridHeaderCheckbox))?.closest("th");
      const start = checkbox ? checkbox.getBoundingClientRect().right - origin.left : 0;
      setCover({
        "--grid-selection-bar-top": `${headerBox.top - origin.top}px`,
        "--grid-selection-bar-start": `${Math.max(start, 0)}px`,
        "--grid-selection-bar-height": `${headerBox.height}px`
      });
    };
    measure();
    if (typeof ResizeObserver === "undefined") {
      return undefined;
    }
    const observer = new ResizeObserver(measure);
    observer.observe(contents);
    return () => observer.disconnect();
  }, [contentsRef, active]);
  return active ? cover : undefined;
}

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

  const dispatch = useDispatch();
  const contentsRef = useRef(null);
  const state = useComponentState(id);
  const {address, attributes = {}, model = {}} = state;
  const {count, hasBar} = getGridSelection(state);
  const selecting = hasBar && count > 0;
  const editing = (model.values || []).some(row => row.$row?.editing);
  const withToolbar = hasGridToolbar(attributes);

  // Clear the selection (the clear button of the selection bar, Esc): the focus goes back to the grid once it is cleared
  const focusPending = useRef(false);
  const clearSelection = useCallback(() => {
    if (address) {
      focusPending.current = true;
      dispatch(addActionsTop([{type: "unselect-all-rows", address}]));
    }
  }, [dispatch, address]);
  useEffect(() => {
    if (count === 0 && focusPending.current) {
      focusPending.current = false;
      focusAfterClear(containerRef.current);
    }
  }, [count, containerRef]);

  // Escape cancels the edition of the row being edited or, without it, clears the selection
  const onEscape = useCallback(() => {
    if (editing) {
      onKeyCancelRow && onKeyCancelRow();
    } else if (count > 0) {
      clearSelection();
    }
  }, [editing, count, onKeyCancelRow, clearSelection]);

  // Escape and Enter work whatever modifiers are pressed. Alt+arrows and Alt+Home/End move the selected row when the grid allows it
  const headerCover = useHeaderCover(contentsRef, selecting && !withToolbar);

  const checkKey = useMemo(() => createKeyHandler([
    ...(onKeyMoveRow ? MOVE_ROW_SHORTCUTS.map(({shortcut, position}) => ({
      shortcut,
      handler: (e) => {
        e.preventDefault();
        onKeyMoveRow(position);
      }
    })) : []),
    {shortcut: {key: "Escape", ...ANY_MODIFIERS}, handler: onEscape},
    {
      shortcut: {key: "Enter", ...ANY_MODIFIERS},
      handler: () => {
        document.body.focus();
        setTimeout(() => onKeySaveRow && onKeySaveRow(), 100);
      }
    }
  ]), [onEscape, onKeySaveRow, onKeyMoveRow]);

  const contextMenu = useCallback((e) => {
    onContextMenuProp && onContextMenuProp({ originalEvent: e, data: null, index: -1 });
  }, [onContextMenuProp]);

  return (
    <div className={classNames("grid-container", "expand", "expandible-vertical", {"grid-reloading": reloading, "grid-row-height": rowHeightPx > 0}, className)} ref={containerRef} style={containerStyle}
      aria-busy={loading ? "true" : undefined}
      {...(id ? {[TestAttributes.maximized]: String(iconMaximized)} : {})}>
      {id && <AweGridToolbar id={id} maximized={iconMaximized} iconMaximized={iconMaximized} onToggleMaximize={toggleMaximized}
        selecting={selecting} selectionBar={withToolbar ? <AweGridSelectionBar id={id} onClear={clearSelection} /> : null} />}
      {id && <AweGridColumnFilterChips id={id} />}
      <div
        ref={contentsRef}
        className="grid-contents expand expandible-vertical"
        role="grid-container"
        onKeyDown={checkKey}
        onContextMenu={contextMenu}
      >
        {children}
        {/* Without toolbar the selection bar covers the column headers */}
        {id && !withToolbar && <AweGridSelectionBar id={id} onClear={clearSelection} placement="header" style={headerCover} />}
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
