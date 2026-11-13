import {getMoveRowsColumn, initRowDrag, MOVE_ROWS_COLUMN} from "../../../../main/resources/js/awe/services/grid/rowDrag";

describe("awe-framework/awe-client-angular/src/main/resources/js/awe/services/grid/rowDrag.js", () => {
  let element;
  let component;

  // Two containers (body and frozen) render each row, both as .ui-grid-row with the row identifier as id
  const rowHtml = id => `<div class="ui-grid-row" id="${id}"><span class="grid-row-grip" draggable="true" row-id="${id}"><i></i></span><span class="cell">${id}</span></div>`;
  const row = (id, container = 0) => element.find(`.ui-grid-row#${id}`).eq(container);
  const dataTransfer = () => ({effectAllowed: null, dropEffect: null, data: {}, setData(type, value) { this.data[type] = value; }});
  const fire = (target, type, init = {}) => {
    const event = new Event(type, {bubbles: true, cancelable: true});
    Object.assign(event, {dataTransfer: dataTransfer(), clientY: 0, relatedTarget: null}, init);
    target.dispatchEvent(event);
    return event;
  };
  // The pointer is on the upper (before) or lower (after) half of a row of 100 pixels high placed at the top
  const over = (id, half, container = 0) => {
    const target = row(id, container)[0];
    target.getBoundingClientRect = () => ({top: 0, height: 100});
    return fire(target, "dragover", {clientY: half === "upper" ? 20 : 80});
  };
  const grip = id => element.find(`.grid-row-grip[row-id='${id}']`)[0];

  beforeEach(() => {
    element = $(`<div>${["1", "2", "3"].map(rowHtml).join("")}${["1", "2", "3"].map(rowHtml).join("")}</div>`);
    document.body.appendChild(element[0]);
    component = {
      scope: {$apply: fn => fn(), $on: jest.fn()},
      moveRow: jest.fn().mockReturnValue(true),
      storeEvent: jest.fn()
    };
    initRowDrag(component, element);
  });

  afterEach(() => element.remove());

  it("describes a narrow leading column without sorting, filtering, moving or resizing", () => {
    const column = getMoveRowsColumn({hasFrozen: true});

    expect(column).toEqual(expect.objectContaining({
      name: MOVE_ROWS_COLUMN, width: 30, minWidth: 30, frozen: true, headerCellTemplate: "grid/header",
      enableColumnResizing: false, enableColumnMoving: false, enableColumnMenu: false, enableFiltering: false, sortable: false
    }));
    expect(column.cellTemplate).toContain("grid-row-grip");
    expect(column.cellTemplate).toContain('draggable="true"');
    expect(column.cellTemplate).toContain("GRID_ROW_DRAG");
    expect(column.cellTemplate).toContain('aria-hidden="true"');
  });

  it("starts a drag from the grip, as a move", () => {
    const event = fire(grip("2"), "dragstart");

    expect(event.dataTransfer.effectAllowed).toBe("move");
    expect(event.dataTransfer.data["text/plain"]).toBe("2");
  });

  it("accepts the drop over another row, and marks where the row would land (upper half before, lower half after)", () => {
    fire(grip("1"), "dragstart");

    const upper = over("3", "upper");
    expect(upper.defaultPrevented).toBe(true);
    expect(upper.dataTransfer.dropEffect).toBe("move");
    expect(row("3").hasClass("grid-row-drop-before")).toBe(true);
    expect(row("3").hasClass("grid-row-drop-after")).toBe(false);

    over("3", "lower");
    expect(row("3").hasClass("grid-row-drop-before")).toBe(false);
    expect(row("3").hasClass("grid-row-drop-after")).toBe(true);
  });

  it("marks the row in every container that renders it, and only one row at a time", () => {
    fire(grip("1"), "dragstart");

    over("2", "upper");
    over("3", "lower");

    expect(element.find(".grid-row-drop-before").length).toBe(0);
    expect(element.find(".grid-row-drop-after").map((i, node) => node.id).get()).toEqual(["3", "3"]);
  });

  it("neither accepts nor marks the row being dragged", () => {
    fire(grip("1"), "dragstart");

    const event = over("1", "upper");

    expect(event.defaultPrevented).toBe(false);
    expect(element.find(".grid-row-drop-before, .grid-row-drop-after").length).toBe(0);
  });

  it("ignores what is not dragged by a grip", () => {
    const event = over("2", "upper");

    expect(event.defaultPrevented).toBe(false);
    expect(element.find(".grid-row-drop-before, .grid-row-drop-after").length).toBe(0);
    const drop = fire(row("2")[0], "drop");
    expect(drop.defaultPrevented).toBe(false);
    expect(component.moveRow).not.toHaveBeenCalled();
  });

  it("removes the mark when the pointer leaves the row, not when it moves over its cells", () => {
    fire(grip("1"), "dragstart");
    over("3", "upper");

    fire(row("3").find(".cell")[0], "dragleave", {relatedTarget: row("3")[0]});
    expect(row("3").hasClass("grid-row-drop-before")).toBe(true);

    fire(row("3")[0], "dragleave", {relatedTarget: row("2")[0]});
    expect(row("3").hasClass("grid-row-drop-before")).toBe(false);
  });

  it.each([
    ["upper", "before"],
    ["lower", "after"]
  ])("drops the row on the %s half of another row with the same move and event as the shortcuts (%s)", (half, position) => {
    fire(grip("1"), "dragstart");
    const target = row("3")[0];
    target.getBoundingClientRect = () => ({top: 0, height: 100});
    over("3", half);

    const event = fire(target, "drop", {clientY: half === "upper" ? 20 : 80});

    expect(event.defaultPrevented).toBe(true);
    expect(component.moveRow).toHaveBeenCalledWith(position, "1", "3");
    expect(component.storeEvent).toHaveBeenCalledWith("move-row");
    expect(element.find(".grid-row-drop-before, .grid-row-drop-after").length).toBe(0);
  });

  it("does not store the event when the row did not move", () => {
    component.moveRow.mockReturnValue(false);
    fire(grip("1"), "dragstart");
    const target = row("3")[0];
    target.getBoundingClientRect = () => ({top: 0, height: 100});

    fire(target, "drop", {clientY: 80});

    expect(component.moveRow).toHaveBeenCalledTimes(1);
    expect(component.storeEvent).not.toHaveBeenCalled();
  });

  it("does nothing when a row is dropped on itself", () => {
    fire(grip("2"), "dragstart");

    fire(row("2")[0], "drop");

    expect(component.moveRow).not.toHaveBeenCalled();
    expect(component.storeEvent).not.toHaveBeenCalled();
  });

  it("forgets the dragged row when the drag ends, so a later drop does nothing", () => {
    fire(grip("1"), "dragstart");
    over("3", "upper");

    fire(grip("1"), "dragend");
    fire(row("3")[0], "drop");

    expect(element.find(".grid-row-drop-before, .grid-row-drop-after").length).toBe(0);
    expect(component.moveRow).not.toHaveBeenCalled();
  });

  it("stops listening when the grid is destroyed", () => {
    const [event, destroy] = component.scope.$on.mock.calls[0];
    expect(event).toBe("$destroy");

    destroy();
    fire(grip("1"), "dragstart");
    const drop = fire(row("3")[0], "drop");

    expect(drop.defaultPrevented).toBe(false);
  });
});
