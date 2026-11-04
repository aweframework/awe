import {
  BLANKS_KEY,
  buildConditionsFilter,
  buildDateTree,
  buildRelativeDateFilter,
  buildValuesFilter,
  getCheckState,
  getFilterFields,
  getInitialChecked,
  getRelativeDateRange,
  setColumnFilter,
  summarizeColumnFilter
} from "../../../src/utilities/columnFilters";
import type {ColumnFilter, ColumnValue} from "../../../src/utilities/columnFilters";

// Translation that answers the key, so the summaries show which text they use
const t = (key: string) => key;

const PROFILE = {name: "ProTxt", filter: "values", filterFields: "Pro", label: "Profile"};
const PROFILES: ColumnValue[] = [{value: "OPE", label: "Operator", count: 3}, {value: "VIE", label: "Viewer", count: 2},
  {value: "ADM", label: "Administrator", count: 1}, {value: null, label: null, count: 4}];
const keys = (...values: unknown[]) => new Set(values.map(value => value === null ? BLANKS_KEY : String(value)));

describe("awe-react-client/test/js/utilities/columnFiltersTest.ts", () => {

  describe("fields of a column", () => {
    it("are its filter-fields, else its sort-field, else its name", () => {
      expect(getFilterFields({name: "Usr", filterFields: "Nam, Eml", sortField: "UsrSrt"})).toEqual(["Nam", "Eml"]);
      expect(getFilterFields({name: "Usr", sortField: "UsrSrt"})).toEqual(["UsrSrt"]);
      expect(getFilterFields({name: "Usr"})).toEqual(["Usr"]);
    });
  });

  describe("entry of the values list", () => {
    it("sends the checked values, and the blanks as empty", () => {
      expect(buildValuesFilter(PROFILE, PROFILES, keys("OPE", "VIE", null))).toEqual(
        {id: "ProTxt", fields: ["Pro"], type: "values", values: ["OPE", "VIE"], empty: true});
    });

    it("sends only the blanks as an empty list of values", () => {
      expect(buildValuesFilter(PROFILE, PROFILES, keys(null))).toEqual(
        {id: "ProTxt", fields: ["Pro"], type: "values", values: [], empty: true});
    });

    it("is no filter when every value is checked", () => {
      expect(buildValuesFilter(PROFILE, PROFILES, keys("OPE", "VIE", "ADM", null))).toBeNull();
    });

    it("sends the checked values of a search even when all of them are checked", () => {
      const found = PROFILES.slice(0, 2);

      expect(buildValuesFilter(PROFILE, found, keys("OPE", "VIE"), false)).toEqual(
        {id: "ProTxt", fields: ["Pro"], type: "values", values: ["OPE", "VIE"]});
    });
  });

  describe("entry of the conditions", () => {
    const USER = {name: "Usr", filter: "text"};
    const AMOUNT = {name: "Amt", filter: "number", sortField: "AmtSrt"};
    const LAST_ACCESS = {name: "LstAcc", filter: "date"};

    it("joins two text conditions by their union", () => {
      expect(buildConditionsFilter(USER, [{operator: "contains", value: " martin "}, {operator: "notEndsWith", value: "@ext.es"}], "or"))
        .toEqual({id: "Usr", fields: ["Usr"], type: "text", union: "or",
          conditions: [{operator: "contains", value: "martin"}, {operator: "notEndsWith", value: "@ext.es"}]});
    });

    it("sends a number range with its end and leaves out the incomplete conditions", () => {
      expect(buildConditionsFilter(AMOUNT, [{operator: "between", value: "10", to: "20.5"}, {operator: "gt", value: "abc"}]))
        .toEqual({id: "Amt", fields: ["AmtSrt"], type: "number", conditions: [{operator: "between", value: "10", to: "20.5"}]});
    });

    it("reads a number with the decimal separator of the application and sends it with a point", () => {
      expect(buildConditionsFilter(AMOUNT, [{operator: "gt", value: "1,5"}], "and", ","))
        .toEqual({id: "Amt", fields: ["AmtSrt"], type: "number", conditions: [{operator: "gt", value: "1.5"}]});
      expect(buildConditionsFilter(AMOUNT, [{operator: "gt", value: "1,5"}])).toBeNull();
    });

    it("sends an operator without value with no value", () => {
      expect(buildConditionsFilter(LAST_ACCESS, [{operator: "empty", value: "ignored"}]))
        .toEqual({id: "LstAcc", fields: ["LstAcc"], type: "date", conditions: [{operator: "empty"}]});
    });

    it("is no filter without a complete condition: a date that is not a dd/MM/yyyy date or an operator of another type", () => {
      expect(buildConditionsFilter(LAST_ACCESS, [{operator: "eq", value: "2026-10-01"}, {operator: "contains", value: "x"}])).toBeNull();
    });

    it("sends a relative date as a between on its days", () => {
      expect(buildRelativeDateFilter(LAST_ACCESS, "lastMonth", new Date(2026, 10, 4)))
        .toEqual({id: "LstAcc", fields: ["LstAcc"], type: "date", conditions: [{operator: "between", value: "01/10/2026", to: "31/10/2026"}]});
    });
  });

  describe("relative dates", () => {
    // Wednesday 4 November 2026
    const today = new Date(2026, 10, 4);

    it.each([
      ["today", "04/11/2026", "04/11/2026"],
      ["yesterday", "03/11/2026", "03/11/2026"],
      ["thisWeek", "02/11/2026", "08/11/2026"],
      ["lastMonth", "01/10/2026", "31/10/2026"],
      ["thisYear", "01/01/2026", "31/12/2026"]
    ] as const)("%s goes from %s to %s", (relative, from, to) => {
      expect(getRelativeDateRange(relative, today)).toEqual({from, to});
    });
  });

  describe("checklist", () => {
    it("is checked, unchecked or mixed", () => {
      expect(getCheckState(3, 3)).toBe("checked");
      expect(getCheckState(0, 3)).toBe("unchecked");
      expect(getCheckState(1, 3)).toBe("mixed");
    });

    it("starts with every value checked, or with the values of the filter of the column and its blanks", () => {
      expect(getInitialChecked(undefined, PROFILES)).toEqual(keys("OPE", "VIE", "ADM", null));
      expect(getInitialChecked({id: "ProTxt", type: "values", values: ["VIE"], empty: true}, PROFILES)).toEqual(keys("VIE", null));
    });
  });

  describe("date tree", () => {
    it("groups the days by month and year, adds their counts and leaves the blanks out", () => {
      const tree = buildDateTree([{value: "30/09/2025", count: 1}, {value: "01/10/2026", count: 2}, {value: "15/10/2026", count: 3},
        {value: null, count: 4}], "en");

      expect(tree.map(year => [year.label, year.count, year.children.map(month => [month.label, month.count,
        month.children.map(day => [day.label, day.value])])])).toEqual([
        ["2025", 1, [["September", 1, [["30", "30/09/2025"]]]]],
        ["2026", 5, [["October", 5, [["01", "01/10/2026"], ["15", "15/10/2026"]]]]]
      ]);
    });
  });

  describe("column filters of the grid", () => {
    const first: ColumnFilter = {id: "A", type: "text", conditions: [{operator: "eq", value: "1"}]};
    const second: ColumnFilter = {id: "B", type: "values", values: ["x"]};

    it("replace the filter of a column in its place, add a new one last and remove one", () => {
      const changed: ColumnFilter = {id: "A", type: "text", conditions: [{operator: "ne", value: "2"}]};

      expect(setColumnFilter([first, second], "A", changed)).toEqual([changed, second]);
      expect(setColumnFilter([first], "B", second)).toEqual([first, second]);
      expect(setColumnFilter([first, second], "A", null)).toEqual([second]);
    });
  });

  describe("chip summary", () => {
    it("lists the labels of the values, the blanks last, and counts the ones after the third", () => {
      const labels = {OPE: "Operator", VIE: "Viewer", ADM: "Administrator", AUD: "Auditor"};

      expect(summarizeColumnFilter({id: "P", type: "values", values: ["OPE", "VIE"]}, t, labels)).toBe("Operator, Viewer");
      expect(summarizeColumnFilter({id: "P", type: "values", values: ["OPE", "VIE", "ADM"], empty: true}, t, labels))
        .toBe("Operator, Viewer, Administrator +1");
      expect(summarizeColumnFilter({id: "P", type: "values", values: ["NEW"]}, t, labels)).toBe("NEW");
    });

    it("shows a range as its two ends and joins the conditions by their union", () => {
      expect(summarizeColumnFilter({id: "D", type: "date", conditions: [{operator: "between", value: "01/10/2026", to: "31/10/2026"}]}, t))
        .toBe("01/10/2026 – 31/10/2026");
      expect(summarizeColumnFilter({id: "U", type: "text", union: "or",
        conditions: [{operator: "contains", value: "martin"}, {operator: "empty"}]}, t))
        .toBe("grid_filter_operator_contains martin grid_filter_or grid_filter_operator_empty");
    });
  });
});
