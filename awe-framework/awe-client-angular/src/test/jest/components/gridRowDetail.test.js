import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";

const hook = testId => `[data-testid='${testId}']`;
const TOGGLE = hook("grid-row-detail-toggle");
const DETAIL = hook("grid-row-detail");
const ROW = hook("grid-row");
const ROWS = [
  {id: "r1", name: "Ann", notes: "First notes"},
  {id: "r2", name: "Bob", notes: "Second notes"},
  {id: "r3", name: "Carl", notes: "Third notes"}
];
const COLUMNS = [{name: "name", label: "Name", sendable: true}, {name: "notes", label: "Notes", hidden: true, sendable: true}];
const ROW_DETAIL = {template: "Notes of {name}: {notes}\n<b>plain</b>"};

/**
 * The AngularJS grid shows the row detail with the same DOM contract as the React grid: a leading column with a native
 * toggle button per row ("aria-expanded", "aria-controls") and the detail under its row, as plain text
 */
describe("grid row detail (awe-grid rendered by ui-grid)", () => {
  let refs;
  let mounted;

  const mount = (controller = {}, values = ROWS) => {
    mounted = mountGrid(refs, {columnModel: COLUMNS.map(column => ({...column})), ...controller}, values.map(row => ({...row})));
    return mounted;
  };
  const find = selector => mounted.element.find(selector);
  const toggleOf = rowId => find(`${TOGGLE}[row-id='${rowId}']`);
  const detailOf = rowId => find(`${DETAIL}[row-id='${rowId}']`);
  const rowOf = rowId => find(`${ROW}[row-id='${rowId}']`);
  // ui-grid refreshes the rendered rows of a page in a timeout
  const flush = () => inject(["$timeout", $timeout => $timeout.flush()]);
  const click = element => {
    element[0].click();
    refs.$rootScope.$digest();
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    mounted?.element.remove();
    mounted = null;
    cleanupHeavyComponentTest();
  });

  it("keeps a grid without row detail unchanged: no toggle column, no details and the same rows and cells", () => {
    const {scope, element} = mount();

    expect(scope.gridOptions.columnDefs.map(column => column.name)).toEqual(["name", "notes"]);
    expect(find(TOGGLE).length).toBe(0);
    expect(find(DETAIL).length).toBe(0);
    expect(find(ROW).map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r1", "r2", "r3"]);
    expect(rowOf("r2").find(`${hook("grid-cell")}[column-id='name']`).text()).toBe("Bob");
    expect(element.hasClass("grid-with-row-detail")).toBe(false);
    expect(find(".grid-row-detail-open").length).toBe(0);
    expect(scope.gridOptions.virtualizationThreshold).toBe(50);
  });

  it("adds a leading toggle column with a native button per row, collapsed", () => {
    const {scope} = mount({rowDetail: ROW_DETAIL, multiselect: true, rowNumbers: true});

    expect(scope.gridOptions.columnDefs.map(column => column.name)).toEqual(["rowDetailToggle", "rowNum", "chkBox", "name", "notes"]);
    expect(find(TOGGLE).map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r1", "r2", "r3"]);
    const toggle = toggleOf("r1");
    expect(toggle.prop("tagName")).toBe("BUTTON");
    expect(toggle.attr("type")).toBe("button");
    expect(toggle.attr("tabindex")).toBeUndefined();
    expect(toggle.attr("aria-expanded")).toBe("false");
    expect(toggle.attr("aria-controls")).toBe("Grid-row-detail-r1");
    expect(toggle.attr("aria-label")).toBe("SCREEN_TEXT_GRID_ROW_DETAIL_SHOW");
    expect(toggle.closest(hook("grid-cell")).attr("column-id")).toBe("rowDetailToggle");
    expect(find(DETAIL).length).toBe(0);
  });

  it("shows the detail under its row with the data of hidden columns as plain text, and hides it again", () => {
    mount({rowDetail: ROW_DETAIL});

    click(toggleOf("r1"));

    const detail = detailOf("r1");
    expect(detail.length).toBe(1);
    expect(detail.text()).toBe("Notes of Ann: First notes\n<b>plain</b>");
    expect(detail.find("b").length).toBe(0);
    expect(detail.hasClass("grid-row-detail")).toBe(true);
    expect(detail.attr("id")).toBe("Grid-row-detail-r1");
    expect(toggleOf("r1").attr("aria-controls")).toBe(detail.attr("id"));
    expect(toggleOf("r1").attr("aria-expanded")).toBe("true");
    expect(toggleOf("r1").attr("aria-label")).toBe("SCREEN_TEXT_GRID_ROW_DETAIL_HIDE");
    // Under its row: inside the row of the canvas, after the cells of the row
    expect(detail.prev()[0]).toBe(rowOf("r1")[0]);
    expect(detail.parent().hasClass("grid-row-detail-open")).toBe(true);

    click(toggleOf("r1"));

    expect(detailOf("r1").length).toBe(0);
    expect(toggleOf("r1").attr("aria-expanded")).toBe("false");
    expect(toggleOf("r1").attr("aria-label")).toBe("SCREEN_TEXT_GRID_ROW_DETAIL_SHOW");
    expect(find(".grid-row-detail-open").length).toBe(0);
  });

  it("adds the style classes of the row detail to its area", () => {
    mount({rowDetail: {...ROW_DETAIL, style: "row-detail-card text-muted"}});

    click(toggleOf("r1"));

    const classes = detailOf("r1").attr("class").split(/\s+/);
    expect(classes).toEqual(expect.arrayContaining(["grid-row-detail", "row-detail-card", "text-muted"]));
  });

  it("keeps only the grid-row-detail class on the area of a row detail without style", () => {
    mount({rowDetail: ROW_DETAIL});

    click(toggleOf("r1"));

    const classes = detailOf("r1").attr("class").split(/\s+/).filter(name => !name.startsWith("ng-"));
    expect(classes).toEqual(["grid-row-detail"]);
  });

  it("keeps several rows open at once", () => {
    mount({rowDetail: ROW_DETAIL});

    click(toggleOf("r1"));
    click(toggleOf("r3"));

    expect(find(DETAIL).map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r1", "r3"]);
    expect(find(TOGGLE).map((i, node) => node.getAttribute("aria-expanded")).get()).toEqual(["true", "false", "true"]);
    expect(detailOf("r3").text()).toBe("Notes of Carl: Third notes\n<b>plain</b>");
  });

  it("does not select the row when its detail is toggled, while a click on a cell still selects it", () => {
    const {model} = mount({rowDetail: ROW_DETAIL});

    click(toggleOf("r2"));

    expect(rowOf("r2").attr("data-selected")).toBe("false");
    expect(model.selected).toEqual([]);

    click(rowOf("r2").find(`${hook("grid-cell")}[column-id='name']`).closest(".ui-grid-cell"));

    expect(rowOf("r2").attr("data-selected")).toBe("true");
    expect(detailOf("r2").length).toBe(1);
  });

  it("closes every detail when the data of the grid is reloaded, also for the rows that come back", () => {
    const {component} = mount({rowDetail: ROW_DETAIL});
    click(toggleOf("r1"));
    click(toggleOf("r2"));

    component.api.updateModelValues({values: ROWS.map(row => ({...row})), records: 3, page: 1, total: 1});
    refs.$rootScope.$digest();

    expect(find(DETAIL).length).toBe(0);
    expect(find(TOGGLE).map((i, node) => node.getAttribute("aria-expanded")).get()).toEqual(["false", "false", "false"]);
  });

  it("keeps the details open when the model changes without new data (the selection)", () => {
    const {component} = mount({rowDetail: ROW_DETAIL});
    click(toggleOf("r1"));

    component.api.updateModelValues({selected: ["r1"]});
    refs.$rootScope.$digest();

    expect(detailOf("r1").length).toBe(1);
  });

  it("keeps the detail of a row open across the client-side pages of a grid that loads all its rows", () => {
    const {component, model} = mount({rowDetail: ROW_DETAIL, loadAll: true, max: 2});
    click(toggleOf("r1"));
    model.total = 2;

    component.setPage(2);
    flush();

    expect(find(ROW).map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r3"]);
    expect(find(DETAIL).length).toBe(0);

    component.setPage(1);
    flush();

    expect(detailOf("r1").length).toBe(1);
    expect(toggleOf("r1").attr("aria-expanded")).toBe("true");
  });

  it("keeps the detail of a row open when a grid that loads all its rows is sorted on the client", () => {
    const {component} = mount({rowDetail: ROW_DETAIL, loadAll: true});
    click(toggleOf("r1"));
    const grid = component.grid.api.grid;

    grid.sortColumn(grid.getColumn("name"), "desc");
    grid.refresh();
    refs.$rootScope.$digest();

    expect(find(ROW).map((i, node) => node.getAttribute("row-id")).get()).toEqual(["r3", "r2", "r1"]);
    expect(detailOf("r1").length).toBe(1);
  });

  it("renders every row of the page, since an open detail makes its row taller than the others", () => {
    const {scope, element} = mount({rowDetail: ROW_DETAIL});

    expect(scope.gridOptions.virtualizationThreshold).toBe(Number.MAX_SAFE_INTEGER);
    expect(element.hasClass("grid-with-row-detail")).toBe(true);
  });
});
