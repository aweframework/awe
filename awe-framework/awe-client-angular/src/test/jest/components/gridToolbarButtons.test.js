// End buttons of the grid toolbar: the Columns and Maximize buttons each grid can hide (show-columns-button,
// show-maximize-button), and their text, shown or only as the tooltip and accessible name (toolbar-labels over the
// gridToolbarLabels setting)
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {mountGrid} from "./gridTestUtils";

const hook = id => `[data-testid='${id}']`;
const COLUMNS = [{name: "a", label: "A"}, {name: "b", label: "B"}, {name: "c", label: "C"}];

describe("grid toolbar end buttons", () => {
  let refs;
  let mounted;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    mounted?.element.remove();
    mounted = null;
    cleanupHeavyComponentTest();
  });

  const mount = (controller = {}) => {
    mounted = mountGrid(refs, {showToolbar: true, columnModel: COLUMNS.map(column => ({...column})), ...controller});
    return mounted;
  };
  const setLabels = labels => inject(["AweSettings", settings => settings.update({gridToolbarLabels: labels})]);
  const endButtons = element => [hook("grid-group-by"), hook("grid-columns-button"), hook("grid-maximize-button")]
    .map(selector => element.find(selector));

  it("hides the Columns button of a grid with showColumnsButton false", () => {
    const {element} = mount({showColumnsButton: false});

    expect(element.find(hook("grid-columns-button")).length).toBe(0);
    expect(element.find(hook("grid-maximize-button")).length).toBe(1);
  });

  it("hides the Maximize button of a grid with showMaximizeButton false", () => {
    const {element} = mount({showMaximizeButton: false});

    expect(element.find(hook("grid-maximize-button")).length).toBe(0);
    expect(element.find(hook("grid-columns-button")).length).toBe(1);
  });

  it("does not render the toolbar when it has nothing to show", () => {
    expect(mount({showColumnsButton: false, showMaximizeButton: false}).element.find(hook("grid-toolbar")).length).toBe(0);
    mounted.element.remove();

    const toolbar = mount({showColumnsButton: false, showMaximizeButton: false, search: "CrtTxt"}).element.find(hook("grid-toolbar"));
    expect(toolbar.length).toBe(1);
    expect(toolbar.find(".grid-toolbar-spacer").length).toBe(0);
  });

  it("shows the end buttons as icons by default, with their text as tooltip and accessible name", () => {
    const {element} = mount({groupSelector: true});

    const [groupBy, columns, maximize] = endButtons(element);
    expect(element.find(`${hook("grid-toolbar")} .button-text`).length).toBe(0);
    expect([groupBy, columns, maximize].map(button => [button.attr("title"), button.attr("aria-label")])).toEqual([
      ["GRID_GROUP_BY", "GRID_GROUP_BY"],
      ["GRID_COLUMNS_BUTTON", "GRID_COLUMNS_BUTTON"],
      ["SCREEN_TEXT_MAXIMIZE", "SCREEN_TEXT_MAXIMIZE"]
    ]);
  });

  it("shows the text of the end buttons when the application settings say so", () => {
    setLabels(true);

    const {element} = mount({groupSelector: true});

    expect(endButtons(element).map(button => button.find(".button-text").text())).toEqual(["GRID_GROUP_BY", "GRID_COLUMNS_BUTTON", "SCREEN_TEXT_MAXIMIZE"]);
    expect(endButtons(element).map(button => button.attr("aria-label"))).toEqual([undefined, undefined, undefined]);
  });

  it("shows the text only of the end buttons listed in the toolbar-labels of the grid", () => {
    const {element} = mount({groupSelector: true, savedSearches: true, toolbarLabels: ["saved-searches", "columns"]});

    const [groupBy, columns, maximize] = endButtons(element);
    const saved = element.find(hook("window-saved-searches"));
    expect(saved.find(".window-saved-searches-label").text()).toBe("SCREEN_TEXT_SAVED_SEARCHES");
    expect([saved.attr("aria-label"), saved.hasClass("grid-toolbar-button-icon")]).toEqual([undefined, false]);
    expect(columns.find(".button-text").text()).toBe("GRID_COLUMNS_BUTTON");
    expect([columns.attr("aria-label"), columns.hasClass("grid-toolbar-button-icon")]).toEqual([undefined, false]);
    expect([groupBy, maximize].map(button => [button.find(".button-text").length, button.attr("aria-label"), button.hasClass("grid-toolbar-button-icon")]))
      .toEqual([[0, "GRID_GROUP_BY", true], [0, "SCREEN_TEXT_MAXIMIZE", true]]);
  });

  it.each([
    [true, [], 0],
    [false, ["group-by", "saved-searches", "columns", "maximize"], 3],
    [true, undefined, 3],
    [false, undefined, 0]
  ])("lets the toolbar-labels of the grid win over the settings (settings %s, grid %j)", (setting, grid, texts) => {
    setLabels(setting);

    const {element} = mount({groupSelector: true, ...(grid === undefined ? {} : {toolbarLabels: grid})});

    expect(element.find(`${hook("grid-toolbar")} .button-text`).length).toBe(texts);
  });

  it("does not apply the stored layout of a grid without the Columns button", () => {
    const {scope} = mount({showColumnsButton: false, columnLayout: {order: ["c", "b", "a"], hidden: ["b"]}});

    expect(scope.component.hasColumnLayout()).toBe(false);
    const defs = scope.gridOptions.columnDefs.filter(def => ["a", "b", "c"].includes(def.name));
    expect(defs.map(def => [def.name, Boolean(def.hidden)])).toEqual([["a", false], ["b", false], ["c", false]]);
  });
});
