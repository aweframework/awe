import React from "react";
import { cleanup, createEvent, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

const ADDRESS = { component: "Grd", view: "report" };
const ROWS = [
  { id: "r1", name: "Ann", selected: true },
  { id: "r2", name: "Bob" },
  { id: "r3", name: "Carl" }
];
const GRIP = ".grid-row-grip";

function gridState({ attributes = {} } = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: ADDRESS,
        model: { values: ROWS, records: ROWS.length },
        attributes: {
          columnModel: [{ name: "name", label: "Name" }],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const renderGrid = (attributes) => renderWithProviders(<AweGrid id="Grd" />, {
  preloadedState: gridState({ attributes }),
  spyDispatch: true
});
const rowOf = (container, rowId) => container.querySelector(`[data-testid='grid-row'][row-id='${rowId}']`);
const gripOf = (container, rowId) => rowOf(container, rowId).querySelector(GRIP);
const headerCells = (container) => container.querySelectorAll("thead th").length;
const moveActions = (dispatchSpy) => dispatchSpy.mock.calls
  .map(([action]) => action)
  .filter(action => action.type === "ADD_ACTIONS_TOP")
  .flatMap(action => action.payload)
  .filter(action => action.type.startsWith("move-row"));

// jsdom does not lay anything out: the row is 20px high from y = 100, so its middle is y = 110
const layOut = (row) => {
  row.getBoundingClientRect = () => ({ top: 100, height: 20, bottom: 120, left: 0, right: 100, width: 100 });
};
const UPPER_HALF = { clientY: 104 };
const LOWER_HALF = { clientY: 116 };
// jsdom has no DragEvent: the pointer position and the element the pointer comes from are set on the event by hand
const fire = (type, element, { clientY, relatedTarget, ...init } = {}) => {
  const event = createEvent[type](element, init);
  Object.defineProperty(event, "clientY", { value: clientY });
  Object.defineProperty(event, "relatedTarget", { value: relatedTarget });
  fireEvent(element, event);
};
const dataTransfer = () => ({ effectAllowed: "", dropEffect: "", setData: jest.fn(), getData: jest.fn() });

describe("awe-react-client/test/js/components/AweGridMoveRowsTest.jsx", () => {
  afterEach(cleanup);

  describe("grip column", () => {
    it("is not rendered without the move-rows attribute", () => {
      const { container } = renderGrid();

      expect(container.querySelector(GRIP)).toBeNull();
      // Name and the filler column
      expect(headerCells(container)).toBe(2);
    });

    it("is not rendered when move-rows is false", () => {
      const { container } = renderGrid({ moveRows: false });

      expect(container.querySelector(GRIP)).toBeNull();
    });

    it("is not rendered in a tree grid", () => {
      const { container } = renderGrid({ moveRows: true, treegrid: true });

      expect(container.querySelector(GRIP)).toBeNull();
      expect(headerCells(container)).toBe(2);
    });

    it("adds one leading column with a grip in every row, and one to the header and the totals", () => {
      const { container } = renderGrid({ moveRows: true, showTotals: true });

      expect(container.querySelectorAll(GRIP)).toHaveLength(3);
      // Grip, Name and the filler column
      expect(headerCells(container)).toBe(3);
      expect(container.querySelectorAll("tfoot td")).toHaveLength(3);
    });

    it("renders an accessible grip that can be dragged", () => {
      const { container } = renderGrid({ moveRows: true });
      const grip = gripOf(container, "r2");

      expect(grip.getAttribute("draggable")).toBe("true");
      expect(grip.getAttribute("aria-label")).toBe("GRID_ROW_DRAG");
      expect(grip.getAttribute("title")).toBe("GRID_ROW_DRAG");
      expect(grip.querySelector("i").getAttribute("aria-hidden")).toBe("true");
      // Only the grip starts a drag, not the row
      expect(rowOf(container, "r2").getAttribute("draggable")).toBeNull();
    });
  });

  describe("drag and drop", () => {
    const start = (container, rowId) => fireEvent.dragStart(gripOf(container, rowId), { dataTransfer: dataTransfer() });
    const over = (container, rowId, position) => {
      const row = rowOf(container, rowId);
      layOut(row);
      fire("dragOver", row, { dataTransfer: dataTransfer(), ...position });
    };
    const drop = (container, rowId, position) => {
      const row = rowOf(container, rowId);
      layOut(row);
      fire("drop", row, { dataTransfer: dataTransfer(), ...position });
    };

    it("announces a move when the grip starts the drag", () => {
      const { container } = renderGrid({ moveRows: true });
      const transfer = dataTransfer();

      fireEvent.dragStart(gripOf(container, "r1"), { dataTransfer: transfer });

      expect(transfer.effectAllowed).toBe("move");
      expect(transfer.setData).toHaveBeenCalled();
    });

    it("shows the drop line above the row when the pointer is on its upper half", () => {
      const { container } = renderGrid({ moveRows: true });

      start(container, "r1");
      over(container, "r3", UPPER_HALF);

      expect(rowOf(container, "r3").classList.contains("grid-row-drop-before")).toBe(true);
      expect(rowOf(container, "r3").classList.contains("grid-row-drop-after")).toBe(false);
    });

    it("shows the drop line below the row when the pointer is on its lower half and moves it with the pointer", () => {
      const { container } = renderGrid({ moveRows: true });

      start(container, "r1");
      over(container, "r3", LOWER_HALF);
      expect(rowOf(container, "r3").classList.contains("grid-row-drop-after")).toBe(true);

      over(container, "r2", UPPER_HALF);
      expect(rowOf(container, "r3").className).not.toContain("grid-row-drop");
      expect(rowOf(container, "r2").classList.contains("grid-row-drop-before")).toBe(true);
    });

    it("shows no drop line over the dragged row itself, nor without a drag started by a grip", () => {
      const { container } = renderGrid({ moveRows: true });

      over(container, "r2", UPPER_HALF);
      expect(rowOf(container, "r2").className).not.toContain("grid-row-drop");

      start(container, "r1");
      over(container, "r1", LOWER_HALF);
      expect(rowOf(container, "r1").className).not.toContain("grid-row-drop");
    });

    it("hides the drop line when the pointer leaves the row, and when the drag ends", () => {
      const { container } = renderGrid({ moveRows: true });

      start(container, "r1");
      over(container, "r3", UPPER_HALF);
      fire("dragLeave", rowOf(container, "r3"), { relatedTarget: document.body });
      expect(rowOf(container, "r3").className).not.toContain("grid-row-drop");

      over(container, "r3", UPPER_HALF);
      fireEvent.dragEnd(gripOf(container, "r1"));
      expect(rowOf(container, "r3").className).not.toContain("grid-row-drop");
    });

    it("keeps the drop line while the pointer moves over the cells of the row", () => {
      const { container } = renderGrid({ moveRows: true });

      start(container, "r1");
      over(container, "r3", UPPER_HALF);
      fire("dragLeave", rowOf(container, "r3"), { relatedTarget: rowOf(container, "r3").querySelector("td") });

      expect(rowOf(container, "r3").classList.contains("grid-row-drop-before")).toBe(true);
    });

    it("moves the row before the row it is dropped on with the move-row-up action", () => {
      const { container, dispatchSpy } = renderGrid({ moveRows: true });

      start(container, "r1");
      drop(container, "r3", UPPER_HALF);

      expect(moveActions(dispatchSpy)).toEqual([
        { type: "move-row-up", address: ADDRESS, parameters: { rowId: "r1", targetRowId: "r3" } }
      ]);
      expect(rowOf(container, "r3").className).not.toContain("grid-row-drop");
    });

    it("moves the row after the row it is dropped on with the move-row-down action", () => {
      const { container, dispatchSpy } = renderGrid({ moveRows: true });

      start(container, "r3");
      drop(container, "r1", LOWER_HALF);

      expect(moveActions(dispatchSpy)).toEqual([
        { type: "move-row-down", address: ADDRESS, parameters: { rowId: "r3", targetRowId: "r1" } }
      ]);
    });

    it("does nothing when a row is dropped on itself", () => {
      const { container, dispatchSpy } = renderGrid({ moveRows: true });

      start(container, "r2");
      drop(container, "r2", LOWER_HALF);

      expect(moveActions(dispatchSpy)).toEqual([]);
    });

    it("does nothing when a drop does not come from a grip", () => {
      const { container, dispatchSpy } = renderGrid({ moveRows: true });

      drop(container, "r2", LOWER_HALF);

      expect(moveActions(dispatchSpy)).toEqual([]);
    });

    it("forgets the dragged row once it is dropped", () => {
      const { container, dispatchSpy } = renderGrid({ moveRows: true });

      start(container, "r1");
      drop(container, "r3", UPPER_HALF);
      drop(container, "r2", UPPER_HALF);

      expect(moveActions(dispatchSpy)).toHaveLength(1);
    });
  });

  describe("shortcuts", () => {
    const pressKey = (init) => fireEvent.keyDown(document.querySelector("[role='grid-container']"), { bubbles: true, ...init });

    it("move the selected row of a grid with move-rows even when it is not editable", () => {
      const { dispatchSpy } = renderGrid({ moveRows: true, editable: false });

      pressKey({ key: "ArrowDown", altKey: true });

      expect(moveActions(dispatchSpy)).toEqual([{ type: "move-row-down", address: ADDRESS }]);
    });

    it("stay off in a grid that is neither editable nor movable", () => {
      const { dispatchSpy } = renderGrid({ editable: false });

      pressKey({ key: "ArrowDown", altKey: true });

      expect(moveActions(dispatchSpy)).toEqual([]);
    });
  });
});
