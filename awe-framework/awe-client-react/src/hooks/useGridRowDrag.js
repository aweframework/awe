import { useCallback, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { addActionsTop } from "../redux/actions/actions";
import { MoveRowActionType, RowPositionType } from "../utilities/grid";

const { BEFORE, AFTER } = RowPositionType;

/**
 * Drop line classes: the dragged row lands just above or below the row they mark
 */
const DROP_CLASS = { [BEFORE]: "grid-row-drop-before", [AFTER]: "grid-row-drop-after" };

// The pointer is on the upper or the lower half of the row it is over
const getPlacement = (event) => {
  const { top, height } = event.currentTarget.getBoundingClientRect();
  return event.clientY < top + height / 2 ? BEFORE : AFTER;
};

/**
 * Drag and drop of the rows of a grid. Only the grip of a row starts a drag; the rows are the drop targets. A drop
 * launches the same move-row action as the keyboard shortcuts (move-row-up or move-row-down), told which row it moves
 * and which row it ends up next to
 * @param {object} address Address of the grid
 * @returns {{onGripDragStart: function, onGripDragEnd: function, getRowProps: function}} Handlers of the grips and
 * props (style class and drag events) of the row with an identifier
 */
export function useGridRowDrag(address) {
  const dispatch = useDispatch();
  const dragged = useRef(null);
  const [dropTarget, setDropTarget] = useState(null);

  const onGripDragStart = useCallback((event, rowId) => {
    dragged.current = rowId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", String(rowId));
    }
  }, []);

  const onGripDragEnd = useCallback(() => {
    dragged.current = null;
    setDropTarget(null);
  }, []);

  const onRowDragOver = useCallback((event, rowId) => {
    // Only a row dragged by its grip can be dropped, and not on itself
    if (dragged.current === null || dragged.current === rowId) {
      return;
    }
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = "move";
    }
    const placement = getPlacement(event);
    setDropTarget(current => current?.rowId === rowId && current.placement === placement ? current : { rowId, placement });
  }, []);

  const onRowDragLeave = useCallback((event, rowId) => {
    // Moving over the cells of the row is not leaving it
    if (!event.currentTarget.contains(event.relatedTarget)) {
      setDropTarget(current => current?.rowId === rowId ? null : current);
    }
  }, []);

  const onRowDrop = useCallback((event, rowId) => {
    const movedRowId = dragged.current;
    if (movedRowId === null) {
      return;
    }
    event.preventDefault();
    dragged.current = null;
    setDropTarget(null);
    if (movedRowId !== rowId) {
      dispatch(addActionsTop([{
        type: MoveRowActionType[getPlacement(event)],
        address,
        parameters: { rowId: movedRowId, targetRowId: rowId }
      }]));
    }
  }, [dispatch, address]);

  const getRowProps = useCallback((rowId) => ({
    className: dropTarget?.rowId === rowId ? DROP_CLASS[dropTarget.placement] : undefined,
    onDragOver: event => onRowDragOver(event, rowId),
    onDragLeave: event => onRowDragLeave(event, rowId),
    onDrop: event => onRowDrop(event, rowId)
  }), [dropTarget, onRowDragOver, onRowDragLeave, onRowDrop]);

  return { onGripDragStart, onGripDragEnd, getRowProps };
}
