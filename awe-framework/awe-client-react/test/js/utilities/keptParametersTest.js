import {getKeptParameters, storeKeptParameters, withKeptParameters} from "../../../src/utilities/keptParameters";

// A Web Storage in memory
const memoryStorage = (items = {}) => ({
  items,
  getItem: key => (key in items ? items[key] : null),
  setItem: (key, value) => {
    items[key] = value;
  }
});

const hidden = (id, selected, keepOnReload = true) => ({id, controller: {id, component: "hidden", keepOnReload}, model: {selected}});

describe("awe-react-client/test/js/utilities/keptParametersTest.js", () => {
  it("keeps the value of the criteria with keep-on-reload that have one, a single value alone", () => {
    expect(getKeptParameters([
      hidden("IdeTsk.selected", [9013]),
      hidden("CrtLst", [1, 2], "true"),
      hidden("CrtNot", [7], false),
      hidden("CrtEmp", []),
      hidden("CrtNul", null),
      {id: "Grd", controller: {component: "grid"}, model: {}}
    ])).toEqual({"IdeTsk.selected": 9013, CrtLst: [1, 2]});
  });

  it("sends the kept values of the screen when the request lacks them, the values of the request winning", () => {
    const storage = memoryStorage();
    storeKeptParameters(storage, "scheduler-task", [hidden("IdeTsk.selected", [9013]), hidden("CrtOth", ["a"])]);

    // A reload sends no values: the kept ones go
    expect(withKeptParameters(storage, "scheduler-task", {view: "base"}))
      .toEqual({view: "base", "IdeTsk.selected": 9013, CrtOth: "a"});
    // The screen the user comes from gives another task: it wins; an empty value does not
    expect(withKeptParameters(storage, "scheduler-task", {"IdeTsk.selected": 9017, CrtOth: ""}))
      .toEqual({"IdeTsk.selected": 9017, CrtOth: "a"});
    // Another screen gets nothing
    const other = {view: "base"};
    expect(withKeptParameters(storage, "scheduler-tasks", other)).toBe(other);
  });

  it("leaves the kept values as they are when the screen keeps none, and ignores a storage it cannot read", () => {
    const storage = memoryStorage();
    storeKeptParameters(storage, "scheduler-task", [hidden("IdeTsk.selected", [9013])]);
    storeKeptParameters(storage, "scheduler-task", [hidden("IdeTsk.selected", [])]);
    expect(withKeptParameters(storage, "scheduler-task", {})).toEqual({"IdeTsk.selected": 9013});

    const parameters = {view: "base"};
    expect(withKeptParameters(memoryStorage({"awe-kept-parameters:broken": "{not json"}), "broken", parameters))
      .toBe(parameters);
    expect(withKeptParameters(null, "scheduler-task", parameters)).toBe(parameters);
    expect(() => storeKeptParameters({setItem: () => {
      throw new Error("QuotaExceededError");
    }}, "scheduler-task", [hidden("IdeTsk.selected", [1])])).not.toThrow();
  });
});
