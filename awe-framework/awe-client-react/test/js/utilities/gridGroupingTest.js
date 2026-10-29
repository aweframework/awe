import {
  buildGroupedRows,
  getGroupLevels,
  getGroupingState,
  getGroupPath,
  isDisplayRow,
  isGroupRow,
  isSubtotalRow
} from '../../../src/utilities/gridGrouping';

// Translator that shows the key, so the tests see which text is used
const t = key => `[${key}]`;

const COLUMNS = [
  {name: "country", label: "Country"},
  {name: "city", label: "City"},
  {name: "status", label: "Status", component: "select",
    model: {values: [{value: "O", label: "Open"}, {value: "C", label: "Closed"}]}},
  {name: "order", label: "Order"},
  {name: "amount", label: "Amount", summaryType: "sum", numberFormat: {mDec: 2, aSep: ".", aDec: ","}},
  {name: "lines", label: "Lines", summaryType: "max"},
  {name: "price", label: "Price", summaryType: "avg"},
  {name: "discount", label: "Discount", summaryType: "min"}
];

const ROWS = [
  {id: 1, country: "ES", city: "Madrid", status: "O", order: "SO-1", amount: 10, lines: 1, price: 4, discount: 3},
  {id: 2, country: "DE", city: "Berlin", status: "C", order: "SO-2", amount: 20, lines: 5, price: 6, discount: 1},
  {id: 3, country: "ES", city: "Bilbao", status: "C", order: "SO-3", amount: 5, lines: 2, price: 8, discount: 2},
  {id: 4, country: "ES", city: "Madrid", status: "C", order: "SO-4", amount: 7, lines: 3, price: 2, discount: 5},
  {id: 5, country: "", city: "Paris", status: "O", order: "SO-5", amount: 1, lines: 9, price: 10, discount: 0},
  {id: 6, country: "DE", city: "Berlin", status: "O", order: "SO-6", amount: 30, lines: 4, price: 1, discount: 4}
];

const build = (options = {}) => buildGroupedRows(ROWS, {columns: COLUMNS, t, ...options});

// A compact picture of the display list: G<level>:<label>(<count>) for groups, S<level>:<label> for subtotals and the
// id of the data rows
const shape = rows => rows.map(row => {
  if (isGroupRow(row)) {
    return `G${row.$group.level}:${row.$group.label}(${row.$group.count})`;
  }
  if (isSubtotalRow(row)) {
    return `S${row.$subtotal.level}:${row.$subtotal.label}`;
  }
  return row.id;
});

const groupOf = (rows, path) => rows.find(row => isGroupRow(row) && row.$group.path === path);
const subtotalOf = (rows, path) => rows.find(row => isSubtotalRow(row) && row.$subtotal.path === path);

describe('awe-react-client/test/js/utilities/gridGroupingTest.js', () => {
  describe('buildGroupedRows', () => {
    it('groups the rows by one column: group row, its data rows and its subtotal, groups by value ascending and empty last', () => {
      const rows = build({groupBy: ["country"]});

      expect(shape(rows)).toEqual([
        "G1:DE(2)", 2, 6, "S1:DE",
        "G1:ES(3)", 1, 3, 4, "S1:ES",
        "G1:[GRID_GROUP_EMPTY](1)", 5, "S1:[GRID_GROUP_EMPTY]"
      ]);
    });

    it('builds the group and subtotal rows with their identifiers, level, path, column, value and aggregates', () => {
      const rows = build({groupBy: ["country"]});
      const group = groupOf(rows, "ES");
      const subtotal = subtotalOf(rows, "ES");

      expect(group.id).toBe("$group-ES");
      expect(group.$group).toEqual(expect.objectContaining({level: 1, path: "ES", column: "country", value: "ES",
        label: "ES", count: 3, expanded: true}));
      expect(group.$group.aggregates.amount).toEqual({value: 22, label: "22,00"});
      expect(subtotal.id).toBe("$subtotal-ES");
      expect(subtotal.$subtotal).toEqual(expect.objectContaining({level: 1, path: "ES", label: "ES"}));
      expect(subtotal.$subtotal.aggregates).toEqual(group.$group.aggregates);
    });

    it('passes the data rows through: they are the same objects', () => {
      const rows = build({groupBy: ["country"]});

      const dataRows = rows.filter(row => !isDisplayRow(row));
      expect(dataRows).toHaveLength(ROWS.length);
      dataRows.forEach(row => expect(ROWS).toContain(row));
    });

    it('computes sum, avg, max and min of the group data rows with the summary type of each column', () => {
      const rows = build({groupBy: ["country"]});
      const {aggregates} = groupOf(rows, "DE").$group;

      expect(aggregates.amount.value).toBe(50);
      expect(aggregates.lines.value).toBe(5);
      expect(aggregates.price.value).toBe(3.5);
      expect(aggregates.discount.value).toBe(1);
      // Columns without summary type have no aggregate
      expect(aggregates.order).toBeUndefined();
      expect(aggregates.city).toBeUndefined();
    });

    it('groups by two levels: the path joins the values and each level has its own subtotal', () => {
      const rows = build({groupBy: ["country", "city"]});

      expect(shape(rows)).toEqual([
        "G1:DE(2)", "G2:Berlin(2)", 2, 6, "S2:Berlin", "S1:DE",
        "G1:ES(3)", "G2:Bilbao(1)", 3, "S2:Bilbao", "G2:Madrid(2)", 1, 4, "S2:Madrid", "S1:ES",
        "G1:[GRID_GROUP_EMPTY](1)", "G2:Paris(1)", 5, "S2:Paris", "S1:[GRID_GROUP_EMPTY]"
      ]);
      expect(groupOf(rows, "ES/Madrid").$group).toEqual(expect.objectContaining({level: 2, column: "city", count: 2}));
      expect(groupOf(rows, "/Paris")).toBeDefined();
    });

    it('groups by three levels with the translated label of the select options', () => {
      const rows = build({groupBy: ["country", "city", "status"]});

      expect(shape(rows).slice(0, 12)).toEqual([
        "G1:DE(2)", "G2:Berlin(2)", "G3:[Closed](1)", 2, "S3:[Closed]", "G3:[Open](1)", 6, "S3:[Open]", "S2:Berlin", "S1:DE",
        "G1:ES(3)", "G2:Bilbao(1)"
      ]);
      expect(groupOf(rows, "ES/Madrid/O").$group).toEqual(expect.objectContaining({level: 3, value: "O", label: "[Open]", count: 1}));
    });

    it('adds the subtotals of a level up to the subtotal of their parent and the first level up to the grand total', () => {
      const rows = build({groupBy: ["country", "city", "status"]});
      const sum = list => list.reduce((total, row) => total + row.$subtotal.aggregates.amount.value, 0);
      const subtotals = level => rows.filter(row => isSubtotalRow(row) && row.$subtotal.level === level);

      expect(sum(subtotals(1))).toBe(ROWS.reduce((total, row) => total + row.amount, 0));
      subtotals(1).forEach(parent => {
        const children = subtotals(2).filter(child => child.$subtotal.path.startsWith(`${parent.$subtotal.path}/`));
        expect(sum(children)).toBe(parent.$subtotal.aggregates.amount.value);
      });
      subtotals(2).forEach(parent => {
        const children = subtotals(3).filter(child => child.$subtotal.path.startsWith(`${parent.$subtotal.path}/`));
        expect(sum(children)).toBe(parent.$subtotal.aggregates.amount.value);
      });
    });

    it('encodes the values of the path, so a value with a slash does not split it', () => {
      const rows = buildGroupedRows([{id: 1, country: "A/B", city: "x y"}], {columns: COLUMNS, t, groupBy: ["country", "city"]});

      expect(groupOf(rows, "A%2FB/x%20y")).toBeDefined();
      expect(getGroupPath(["A/B", "x y"])).toBe("A%2FB/x%20y");
    });

    it('takes the value of cells with a model object', () => {
      const rows = buildGroupedRows([
        {id: 1, country: {value: "ES", label: "Spain"}},
        {id: 2, country: {value: "ES", label: "Spain"}}
      ], {columns: COLUMNS, t, groupBy: ["country"]});

      expect(shape(rows)).toEqual(["G1:Spain(2)", 1, 2, "S1:Spain"]);
    });

    it('orders the data rows inside each group by the sort', () => {
      const rows = build({groupBy: ["country"], sort: [{id: "amount", direction: "desc"}]});

      expect(shape(rows).filter(item => typeof item === "number")).toEqual([6, 2, 1, 4, 3, 5]);
    });

    it('orders the groups of a level by their value in the direction of a sort on its group column', () => {
      const rows = build({groupBy: ["country", "city"], sort: [{id: "city", direction: "desc"}]});

      expect(shape(rows).filter(item => String(item).startsWith("G"))).toEqual([
        "G1:DE(2)", "G2:Berlin(2)", "G1:ES(3)", "G2:Madrid(2)", "G2:Bilbao(1)", "G1:[GRID_GROUP_EMPTY](1)", "G2:Paris(1)"
      ]);
    });

    it('orders the first level descending and keeps the empty group last', () => {
      const rows = build({groupBy: ["country"], sort: [{id: "country", order: -1}]});

      expect(shape(rows).filter(item => String(item).startsWith("G"))).toEqual(["G1:ES(3)", "G1:DE(2)", "G1:[GRID_GROUP_EMPTY](1)"]);
    });

    it('orders the groups of every level by the aggregate of a sorted column with summary type', () => {
      const rows = build({groupBy: ["country", "city"], sort: [{id: "amount", direction: "asc"}]});

      expect(shape(rows).filter(item => String(item).startsWith("G"))).toEqual([
        "G1:[GRID_GROUP_EMPTY](1)", "G2:Paris(1)", "G1:ES(3)", "G2:Bilbao(1)", "G2:Madrid(2)", "G1:DE(2)", "G2:Berlin(2)"
      ]);
      const descending = build({groupBy: ["country"], sort: [{id: "amount", direction: "desc"}]});
      expect(shape(descending).filter(item => String(item).startsWith("G"))).toEqual(["G1:DE(2)", "G1:ES(3)", "G1:[GRID_GROUP_EMPTY](1)"]);
    });

    it('keeps the groups by value when the first sort is on a column without summary type that does not group', () => {
      const rows = build({groupBy: ["country"], sort: [{id: "order", direction: "desc"}]});

      expect(shape(rows)).toEqual([
        "G1:DE(2)", 6, 2, "S1:DE",
        "G1:ES(3)", 4, 3, 1, "S1:ES",
        "G1:[GRID_GROUP_EMPTY](1)", 5, "S1:[GRID_GROUP_EMPTY]"
      ]);
    });

    it('hides the children and the subtotal of a collapsed group, and keeps its count and aggregates', () => {
      const rows = build({groupBy: ["country", "city"], expansion: {all: true, paths: {"ES": false}}});

      expect(shape(rows)).toEqual([
        "G1:DE(2)", "G2:Berlin(2)", 2, 6, "S2:Berlin", "S1:DE",
        "G1:ES(3)",
        "G1:[GRID_GROUP_EMPTY](1)", "G2:Paris(1)", 5, "S2:Paris", "S1:[GRID_GROUP_EMPTY]"
      ]);
      expect(groupOf(rows, "ES").$group).toEqual(expect.objectContaining({expanded: false, count: 3}));
      expect(groupOf(rows, "ES").$group.aggregates.amount.value).toBe(22);
    });

    it('collapses every group and expands only the given paths', () => {
      const rows = build({groupBy: ["country", "city"], expansion: {all: false, paths: {"ES": true}}});

      expect(shape(rows)).toEqual(["G1:DE(2)", "G1:ES(3)", "G2:Bilbao(1)", "G2:Madrid(2)", "S1:ES", "G1:[GRID_GROUP_EMPTY](1)"]);
    });

    it('does not add subtotal rows when the subtotals are off', () => {
      const rows = build({groupBy: ["country"], subtotals: false});

      expect(rows.some(isSubtotalRow)).toBe(false);
      expect(shape(rows)).toEqual(["G1:DE(2)", 2, 6, "G1:ES(3)", 1, 3, 4, "G1:[GRID_GROUP_EMPTY](1)", 5]);
    });

    it('ignores the unknown group-by columns', () => {
      expect(shape(build({groupBy: ["unknown", "country"]}))).toEqual(shape(build({groupBy: ["country"]})));
    });

    it('returns the data rows as they are without grouping levels', () => {
      expect(build({groupBy: []})).toEqual(ROWS);
      expect(build({groupBy: ["unknown"]})).toEqual(ROWS);
    });
  });

  describe('getGroupLevels', () => {
    it('reads the comma separated columns, trimmed and without duplicates, ignoring unknown ones', () => {
      expect(getGroupLevels(" country, city ,unknown,country", COLUMNS)).toEqual(["country", "city"]);
      expect(getGroupLevels(["status", "nope"], COLUMNS)).toEqual(["status"]);
      expect(getGroupLevels(undefined, COLUMNS)).toEqual([]);
      expect(getGroupLevels("", COLUMNS)).toEqual([]);
    });
  });

  describe('getGroupingState', () => {
    const attributes = {groupBy: "country,city", columnModel: COLUMNS};

    it('groups by the group-by of the grid and starts expanded', () => {
      expect(getGroupingState(attributes)).toEqual(expect.objectContaining({
        levels: ["country", "city"], active: true, groupable: true, selector: false, subtotals: true,
        expansion: {all: true, paths: {}}
      }));
    });

    it('takes the levels chosen at runtime and keeps the expansion of the same levels', () => {
      const expansion = {all: false, paths: {"ES": true}, levels: ["city"]};
      expect(getGroupingState({...attributes, groupLevels: ["city"], groupExpansion: expansion})).toEqual(
        expect.objectContaining({levels: ["city"], active: true, expansion: {all: false, paths: {"ES": true}}}));
      // Other levels keep the expand/collapse all, not the groups
      expect(getGroupingState({...attributes, groupLevels: ["country"], groupExpansion: expansion}).expansion)
        .toEqual({all: false, paths: {}});
    });

    it('is not active without levels: no grouping chosen at runtime', () => {
      expect(getGroupingState({...attributes, groupSelector: true, groupLevels: []})).toEqual(
        expect.objectContaining({levels: [], active: false, groupable: true, selector: true}));
    });

    it('reads group-subtotals false', () => {
      expect(getGroupingState({...attributes, groupSubtotals: false}).subtotals).toBe(false);
    });

    it.each([
      ["tree grids", {treegrid: true}],
      ["editable grids", {editable: true}],
      ["send-operations grids", {multioperation: true}],
      ["move-rows grids", {moveRows: true}],
      ["row-detail grids", {rowDetail: {columns: []}}]
    ])('ignores grouping on %s', (_name, extra) => {
      expect(getGroupingState({...attributes, groupSelector: true, ...extra})).toEqual(
        expect.objectContaining({active: false, groupable: false, selector: false}));
    });
  });
});
