import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";

const hook = testId => `[data-testid='${testId}']`;
const ROWS = [{id: "r1", name: "Ann"}, {id: "r2", name: "Bob"}, {id: "r3", name: "Carl"}];
const COLUMNS = [{name: "name", label: "Name", sendable: true}];

/**
 * A grid with move-rows has a leading column with the grip that drags each row
 */
describe("grid with move-rows (awe-grid rendered by ui-grid)", () => {
  let refs;
  let mounted;

  const mount = (controller = {}) => {
    mounted = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column})), ...controller}, ROWS.map(row => ({...row})));
    return mounted;
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    mounted?.element.remove();
    mounted = null;
    cleanupHeavyComponentTest();
  });

  it("keeps a grid without move-rows unchanged: no grip column", () => {
    const {scope, element} = mount();

    expect(scope.gridOptions.columnDefs.map(column => column.name)).toEqual(["name"]);
    expect(element.find(".grid-row-grip").length).toBe(0);
  });

  it("adds the grip column before the other leading columns, with a draggable grip per row", () => {
    const {scope, element} = mount({moveRows: true, multiselect: true, rowNumbers: true});

    expect(scope.gridOptions.columnDefs.map(column => column.name)).toEqual(["moveRowsGrip", "rowNum", "chkBox", "name"]);
    const grips = element.find(".grid-row-grip");
    expect(grips.map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r1", "r2", "r3"]);
    expect(grips.first().attr("draggable")).toBe("true");
    expect(grips.first().find("svg.awe-icon").attr("aria-hidden")).toBe("true");
    expect(grips.first().find("svg.awe-icon > use").attr("href")).toMatch(/#menu$/);
    expect(grips.first().closest(hook("grid-cell")).attr("column-id")).toBe("moveRowsGrip");
  });

  it("gives the grip the translated text of GRID_ROW_DRAG", () => {
    const {element} = mount({moveRows: true});

    expect(element.find(".grid-row-grip").first().attr("title")).toBe("GRID_ROW_DRAG");
    expect(element.find(".grid-row-grip").first().attr("aria-label")).toBe("GRID_ROW_DRAG");
  });
});
