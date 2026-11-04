import {
  getGridSearchRestore,
  getGridSearchState,
  isGridSearchInEffect,
  parseGridSearchValues
} from "../../../src/utilities/gridSavedSearches";

// Saved searches of a grid (#912, S21): the state a search keeps, what is restored and when it is still in effect
describe("awe-react-client/test/js/utilities/gridSavedSearchesTest.ts", () => {
  const COLUMNS = [
    {name: "Nam", sortField: "Nam", filter: "text", filterFields: "Nam,FulNam"},
    {name: "Sta", sortField: "IdeSta", filter: "values"},
    {name: "Eml", filter: "text", sortable: false},
    {name: "Amt"}
  ];

  it("keeps the labels of the filtered columns only, and the quick filter and search only when the grid has them", () => {
    expect(getGridSearchState({
      columnFilters: [{id: "Sta", fields: ["IdeSta"], type: "values", values: ["ACT"]}],
      columnFilterLabels: {Sta: {ACT: "Active"}, Old: {X: "Gone"}},
      sort: [{id: "Amt", direction: "DESC"}, {id: "Nam"}],
      quickFilter: "LCK",
      search: "jo"
    })).toEqual({
      columnFilters: [{id: "Sta", fields: ["IdeSta"], type: "values", values: ["ACT"]}],
      columnFilterLabels: {Sta: {ACT: "Active"}},
      sort: [{id: "Amt", direction: "desc"}]
    });
  });

  it("drops the filters and sorts of columns the grid no longer filters or sorts, and takes the fields of the column", () => {
    const restored = getGridSearchRestore({
      columnFilters: [
        {id: "Nam", fields: ["Nam"], type: "text", conditions: [{operator: "contains", value: "an"}]},
        {id: "Sta", fields: ["Sta"], type: "text", conditions: [{operator: "eq", value: "x"}]},
        {id: "Gone", fields: ["Gone"], type: "values", values: [1]}
      ],
      columnFilterLabels: {Gone: {1: "One"}},
      sort: [{id: "IdeSta", direction: "asc"}, {id: "Eml", direction: "asc"}, {id: "Gone", direction: "desc"}],
      quickFilter: "LCK"
    }, COLUMNS, {hasQuickFilter: false, hasSearch: true});

    expect(restored).toEqual({
      columnFilters: [{id: "Nam", fields: ["Nam", "FulNam"], type: "text", conditions: [{operator: "contains", value: "an"}]}],
      columnFilterLabels: {},
      sort: [{id: "IdeSta", direction: "asc"}],
      search: ""
    });
  });

  it("is in effect while the filters, sort, quick filter and search are the ones of the search, whatever the labels", () => {
    const saved = {
      columnFilters: [{id: "Nam", fields: ["Nam"], type: "text" as const, union: "and" as const,
        conditions: [{operator: "eq", value: "x"}]}],
      columnFilterLabels: {},
      sort: [{id: "Nam", direction: "asc" as const}],
      quickFilter: "",
      search: "ana"
    };
    const current = {...saved, columnFilters: [{id: "Nam", type: "text" as const, conditions: [{operator: "eq", value: "x"}]}],
      columnFilterLabels: {Nam: {}}};

    expect(isGridSearchInEffect(current, saved)).toBe(true);
    expect(isGridSearchInEffect({...current, search: "an"}, saved)).toBe(false);
    expect(isGridSearchInEffect({...current, sort: []}, saved)).toBe(false);
    // The order of the keys does not matter
    expect(isGridSearchInEffect({...current, columnFilters: [{conditions: [{value: "x", operator: "eq"}], type: "text" as const,
      id: "Nam"}]}, saved)).toBe(true);
  });

  it("reads no state from values that are not an object", () => {
    expect(parseGridSearchValues("[1]")).toEqual({});
    expect(parseGridSearchValues("not json")).toEqual({});
    expect(parseGridSearchValues("{\"search\":\"a\"}")).toEqual({search: "a"});
  });
});
