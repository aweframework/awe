import {TestIds} from "../../data/testIds";
import {iconMarkup} from "../icon";

/**
 * Drag and drop of the rows of a grid with move-rows: a leading column with a grip per row. Only the grip starts a drag
 * (native HTML5 events); the rows are the drop targets, and a drop moves the row through the same method as the keyboard
 * shortcuts and the move-row actions (component.moveRow) and stores the same event
 */

// Name of the leading column with the grips
export const MOVE_ROWS_COLUMN = "moveRowsGrip";

// Classes of the row that marks where the dragged row lands: just above or just below it
const DROP_BEFORE = "grid-row-drop-before";
const DROP_AFTER = "grid-row-drop-after";

// Grip of a row: only it is draggable, so the cells keep their text selection and clicks
const GRIP_CELL_TEMPLATE = `<div class="no-animate ui-grid-cell-contents ui-grid-row-grip text-center" column-id="{{col.name}}" data-testid="${TestIds.gridCell}">
  <span class="grid-row-grip" draggable="true" ng-attr-row-id="{{row.entity[grid.appScope.component.constants.ROW_IDENTIFIER]}}"
        title="{{'GRID_ROW_DRAG' | translateMultiple}}" aria-label="{{'GRID_ROW_DRAG' | translateMultiple}}">
    ${iconMarkup("bars")}
  </span>
</div>`;

/**
 * Column with the grips that drag each row (a leading column, as the row detail toggles)
 * @param {object} component Grid component
 * @returns {object} ui-grid column definition
 */
export function getMoveRowsColumn(component) {
  return {
    width: 30,
    minWidth: 30,
    label: "",
    displayName: "",
    name: MOVE_ROWS_COLUMN,
    frozen: component.hasFrozen,
    headerCellTemplate: "grid/header",
    cellTemplate: GRIP_CELL_TEMPLATE,
    enableColumnResizing: false,
    enableColumnMoving: false,
    enableColumnMenu: false,
    enableFiltering: false,
    sortable: false
  };
}

/**
 * Bind the drag and drop of the rows of a grid. The events are delegated from the grid element, as the rows are
 * rendered (and rendered again) by ui-grid
 * @param {object} component Grid component
 * @param {object} element Grid element (jQuery)
 */
export function initRowDrag(component, element) {
  // Identifier of the row being dragged (null when the drag was not started by a grip)
  let dragged = null;

  // The body and the frozen containers render each row: all of them show the mark
  const rowsOf = id => element.find(".ui-grid-row").filter((index, row) => row.id === String(id));
  const clearMarks = () => element.find(`.${DROP_BEFORE}, .${DROP_AFTER}`).removeClass(`${DROP_BEFORE} ${DROP_AFTER}`);
  const getEvent = event => event.originalEvent || event;

  // The pointer is on the upper (before) or lower (after) half of the row it is over
  const getPosition = (event, row) => {
    const {top, height} = row.getBoundingClientRect();
    return getEvent(event).clientY < top + height / 2 ? "before" : "after";
  };

  element.on("dragstart.rowDrag", ".grid-row-grip", function (event) {
    dragged = $(this).attr("row-id");
    const transfer = getEvent(event).dataTransfer;
    if (transfer) {
      transfer.effectAllowed = "move";
      transfer.setData("text/plain", String(dragged));
    }
  });

  element.on("dragend.rowDrag", ".grid-row-grip", function () {
    dragged = null;
    clearMarks();
  });

  element.on("dragover.rowDrag", ".ui-grid-row", function (event) {
    // Only a row dragged by its grip can be dropped, and not on itself
    if (dragged === null || this.id === String(dragged)) {
      return;
    }
    event.preventDefault();
    const transfer = getEvent(event).dataTransfer;
    if (transfer) {
      transfer.dropEffect = "move";
    }
    clearMarks();
    rowsOf(this.id).addClass(getPosition(event, this) === "before" ? DROP_BEFORE : DROP_AFTER);
  });

  element.on("dragleave.rowDrag", ".ui-grid-row", function (event) {
    // Moving over the cells of the row is not leaving it
    if (!this.contains(getEvent(event).relatedTarget)) {
      rowsOf(this.id).removeClass(`${DROP_BEFORE} ${DROP_AFTER}`);
    }
  });

  element.on("drop.rowDrag", ".ui-grid-row", function (event) {
    const movedRowId = dragged;
    if (movedRowId === null) {
      return;
    }
    event.preventDefault();
    dragged = null;
    clearMarks();
    if (this.id !== String(movedRowId)) {
      const position = getPosition(event, this);
      component.scope.$apply(() => {
        if (component.moveRow(position, movedRowId, this.id)) {
          component.storeEvent("move-row");
        }
      });
    }
  });

  component.scope.$on("$destroy", () => element.off(".rowDrag"));
}
