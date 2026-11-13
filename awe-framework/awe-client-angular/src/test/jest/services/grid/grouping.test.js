import {
  buildGroupedRows,
  calculateAggregate,
  formatGroupNumber,
  getGroupLevels,
  getGroupPath,
  isDisplayRow
} from "../../../../main/resources/js/awe/services/grid/grouping";

// Row grouping of the grids (#833): the pure function that builds the display rows (group rows, data rows and subtotal
// rows) from the data rows of a grid
describe("grid row grouping (#833)", () => {
  const COLUMNS = [
    {name: "id", label: "ID", hidden: true},
    {name: "country", label: "Country"},
    {name: "city", label: "City"},
    {name: "office", label: "Office"},
    {name: "name", label: "Name"},
    {name: "amount", label: "Amount", summaryType: "sum", numberFormat: {mDec: 2, aPad: true}},
    {name: "lines", label: "Lines", summaryType: "sum", numberFormat: {mDec: 0}},
    {name: "price", label: "Price", summaryType: "avg", numberFormat: {mDec: 2, aPad: true}},
    {name: "top", label: "Top", summaryType: "max", numberFormat: {mDec: 0}},
    {name: "low", label: "Low", summaryType: "min", numberFormat: {mDec: 0}}
  ];

  const row = (id, country, city, office, name, amount, lines) => ({id, country, city, office, name, amount, lines, price: amount, top: lines, low: lines});
  const ROWS = [
    row("r1", "ES", "Madrid", "M1", "Ann", 100, 1),
    row("r2", "DE", "Berlin", "B1", "Bob", 50.5, 4),
    row("r3", "ES", "Bilbao", "B2", "Carl", 20, 2),
    row("r4", "ES", "Madrid", "M2", "Dora", 30, 3),
    row("r5", "DE", "Berlin", "B1", "Eve", 10, 6),
    row("r6", "ES", "Madrid", "M1", "Finn", 5, 5)
  ];

  const build = (options = {}) => buildGroupedRows(ROWS, {groupBy: ["country"], columns: COLUMNS, ...options});
  const kinds = rows => rows.map(item => (item.$group ? `G${item.$group.level}:${item.$group.label}` : item.$subtotal ? `S${item.$subtotal.level}:${item.$subtotal.label}` : item.id));
  const groups = rows => rows.filter(item => item.$group).map(item => item.$group);
  const subtotals = rows => rows.filter(item => item.$subtotal).map(item => item.$subtotal);

  describe("one level", () => {
    it("builds a group row, its data rows and a subtotal row per group, groups ordered by value", () => {
      expect(kinds(build())).toEqual(["G1:DE", "r2", "r5", "S1:DE", "G1:ES", "r1", "r3", "r4", "r6", "S1:ES"]);
    });

    it("gives the group and subtotal rows their identifier, level, path, column, value, label, count and state", () => {
      const rows = build();

      expect(rows[0]).toEqual({
        id: "$group-DE",
        $group: {
          level: 1, path: "DE", column: "country", value: "DE", label: "DE", count: 2, expanded: true,
          aggregates: expect.any(Object)
        }
      });
      expect(rows[3]).toEqual({id: "$subtotal-DE", $subtotal: {level: 1, path: "DE", label: "DE", aggregates: rows[0].$group.aggregates}});
    });

    it("passes the data rows through unchanged (the same objects)", () => {
      const rows = build();

      expect(rows[1]).toBe(ROWS[1]);
      expect(rows[2]).toBe(ROWS[4]);
      expect(rows.filter(item => !isDisplayRow(item))).toHaveLength(ROWS.length);
      expect(ROWS.every(item => !("$group" in item) && !("$subtotal" in item))).toBe(true);
    });

    it("returns the data rows (a copy of the list) without group-by levels", () => {
      const rows = buildGroupedRows(ROWS, {groupBy: [], columns: COLUMNS});

      expect(rows).toEqual(ROWS);
      expect(rows).not.toBe(ROWS);
      expect(rows[0]).toBe(ROWS[0]);
    });

    it("ignores the unknown group-by column names", () => {
      expect(kinds(build({groupBy: ["unknown", "country", "nope"]}))).toEqual(kinds(build()));
      expect(buildGroupedRows(ROWS, {groupBy: "nope", columns: COLUMNS})).toEqual(ROWS);
    });

    it("reads the group-by levels from a comma-separated list", () => {
      expect(getGroupLevels(" country , city,unknown,country ", COLUMNS)).toEqual(["country", "city"]);
      expect(getGroupLevels(["city"], COLUMNS)).toEqual(["city"]);
      expect(getGroupLevels(null, COLUMNS)).toEqual([]);
      expect(getGroupLevels("", COLUMNS)).toEqual([]);
    });
  });

  describe("aggregates", () => {
    const aggregates = rows => groups(rows).map(group => group.aggregates);

    it("adds the sum, average, maximum and minimum of the columns with summary type, formatted", () => {
      const [de, es] = aggregates(build());

      expect(de).toEqual({
        amount: {value: 60.5, label: "60.50"},
        lines: {value: 10, label: "10"},
        price: {value: 30.25, label: "30.25"},
        top: {value: 6, label: "6"},
        low: {value: 4, label: "4"}
      });
      expect(es.amount).toEqual({value: 155, label: "155.00"});
      expect(es.price).toEqual({value: 38.75, label: "38.75"});
      expect(es.top).toEqual({value: 5, label: "5"});
      expect(es.low).toEqual({value: 1, label: "1"});
    });

    it("reads the values of the cells given as objects and skips the ones which are not numbers", () => {
      const values = [{id: "a", country: "ES", amount: {value: "10.5", label: "10,5"}}, {id: "b", country: "ES", amount: "abc"},
        {id: "c", country: "ES", amount: null}, {id: "d", country: "ES", amount: 4.5}];
      const columns = [{name: "country", label: "Country"}, {name: "amount", summaryType: "sum"}, {name: "avg", summaryType: "avg"}];

      const [group] = groups(buildGroupedRows(values.map(item => ({...item, avg: item.amount})), {groupBy: ["country"], columns}));

      expect(group.aggregates.amount.value).toBe(15);
      // The average divides by the number of rows of the group, as the footer of the grid
      expect(group.aggregates.avg.value).toBe(3.75);
    });

    it("calculates each summary type as the footer of the grid", () => {
      const values = [{n: "1"}, {n: {value: -3}}, {n: "x"}, {n: 7.5}];

      expect(calculateAggregate("sum", values, "n")).toBe(5.5);
      expect(calculateAggregate("avg", values, "n")).toBe(5.5 / 4);
      expect(calculateAggregate("max", values, "n")).toBe(7.5);
      expect(calculateAggregate("min", values, "n")).toBe(-3);
      expect(calculateAggregate("max", [{n: -2}, {n: -5}], "n")).toBe(-2);
      expect(calculateAggregate("avg", [], "n")).toBeNull();
      expect(calculateAggregate("max", [{n: "x"}], "n")).toBeNull();
      expect(calculateAggregate("sum", [], "n")).toBe(0);
      expect(calculateAggregate("count", values, "n")).toBeNull();
    });

    it("makes the subtotals of a level add up to the subtotal of their parent and to the grand total", () => {
      const rows = buildGroupedRows(ROWS, {groupBy: ["country", "city", "office"], columns: COLUMNS});
      const total = ROWS.reduce((sum, item) => sum + item.amount, 0);
      const level = n => subtotals(rows).filter(subtotal => subtotal.level === n);
      const sum = list => Math.round(list.reduce((acc, subtotal) => acc + subtotal.aggregates.amount.value, 0) * 100) / 100;

      expect(sum(level(1))).toBe(total);
      expect(sum(level(2))).toBe(total);
      expect(sum(level(3))).toBe(total);
      level(1).forEach(parent => {
        const children = level(2).filter(child => child.path.startsWith(parent.path + "/"));
        expect(sum(children)).toBe(parent.aggregates.amount.value);
      });
      level(2).forEach(parent => {
        const children = level(3).filter(child => child.path.startsWith(parent.path + "/"));
        expect(sum(children)).toBe(parent.aggregates.amount.value);
      });
    });

    it("formats the numbers with the number format of the column", () => {
      expect(formatGroupNumber(1234567.891, {mDec: 2, aPad: true})).toBe("1,234,567.89");
      expect(formatGroupNumber(1234.5, {aSep: ".", aDec: ",", mDec: 2, aPad: true, aSign: " EUR", pSign: "s"})).toBe("1.234,50 EUR");
      expect(formatGroupNumber(-1234.5, {aSign: "$", pSign: "p", mDec: 0})).toBe("-$1,235");
      expect(formatGroupNumber(12.5, {precision: 2})).toBe("12.5");
      expect(formatGroupNumber(12.5)).toBe("12.5");
      expect(formatGroupNumber(12.345)).toBe("12.35");
      expect(formatGroupNumber(0.5, {vMin: "-999", vMax: "999", aPad: true})).toBe("1");
      expect(formatGroupNumber(null)).toBe("");
    });

    it("uses the formatter given by the engine", () => {
      const format = jest.fn((value, column) => `${column.name}=${value}`);

      const [de] = groups(build({format}));

      expect(de.aggregates.amount.label).toBe("amount=60.5");
      expect(format).toHaveBeenCalledWith(60.5, COLUMNS[5]);
    });
  });

  describe("several levels", () => {
    it("nests the groups of two levels, each one closed by its subtotal", () => {
      expect(kinds(build({groupBy: ["country", "city"]}))).toEqual([
        "G1:DE", "G2:Berlin", "r2", "r5", "S2:Berlin", "S1:DE",
        "G1:ES", "G2:Bilbao", "r3", "S2:Bilbao", "G2:Madrid", "r1", "r4", "r6", "S2:Madrid", "S1:ES"
      ]);
    });

    it("nests three levels with their counts and paths", () => {
      const rows = build({groupBy: ["country", "city", "office"]});

      expect(kinds(rows)).toEqual([
        "G1:DE", "G2:Berlin", "G3:B1", "r2", "r5", "S3:B1", "S2:Berlin", "S1:DE",
        "G1:ES", "G2:Bilbao", "G3:B2", "r3", "S3:B2", "S2:Bilbao",
        "G2:Madrid", "G3:M1", "r1", "r6", "S3:M1", "G3:M2", "r4", "S3:M2", "S2:Madrid", "S1:ES"
      ]);
      expect(groups(rows).map(group => [group.path, group.count])).toEqual([
        ["DE", 2], ["DE/Berlin", 2], ["DE/Berlin/B1", 2],
        ["ES", 4], ["ES/Bilbao", 1], ["ES/Bilbao/B2", 1], ["ES/Madrid", 3], ["ES/Madrid/M1", 2], ["ES/Madrid/M2", 1]
      ]);
      expect(rows.find(item => item.$group?.path === "ES/Madrid/M1").id).toBe("$group-ES/Madrid/M1");
      expect(rows.find(item => item.$subtotal?.path === "ES/Madrid").id).toBe("$subtotal-ES/Madrid");
    });

    it("builds the path from the encoded values of the levels", () => {
      expect(getGroupPath(null, "a/b c")).toBe("a%2Fb%20c");
      expect(getGroupPath("x", 3)).toBe("x/3");
      expect(getGroupPath("x", null)).toBe("x/");
    });
  });

  describe("labels and empty values", () => {
    const values = [
      {id: "a", state: {value: "1", label: "Active"}},
      {id: "b", state: null},
      {id: "c", state: {value: "0", label: "Inactive"}},
      {id: "d", state: ""},
      {id: "e", state: {value: "1", label: "Active"}}
    ];
    const columns = [{name: "state", label: "State"}];

    it("shows the label of the cell, groups by its value and puts the empty values in a last translated group", () => {
      const rows = buildGroupedRows(values, {groupBy: ["state"], columns, translate: key => `t(${key})`});

      expect(kinds(rows)).toEqual(["G1:Inactive", "c", "S1:Inactive", "G1:Active", "a", "e", "S1:Active", "G1:t(GRID_GROUP_EMPTY)", "b", "d", "S1:t(GRID_GROUP_EMPTY)"]);
      expect(groups(rows).map(group => group.value)).toEqual(["0", "1", null]);
      expect(groups(rows)[2].path).toBe("");
    });

    it("uses the label given by the engine (select and suggest labels)", () => {
      const rows = buildGroupedRows([{id: "a", code: "ES"}, {id: "b", code: "FR"}], {
        groupBy: ["code"], columns: [{name: "code", label: "Code"}], getLabel: (item, column) => `${column.name}:${item.code}`
      });

      expect(groups(rows).map(group => group.label)).toEqual(["code:ES", "code:FR"]);
    });

    it("keeps the empty group last also when its level is sorted descending", () => {
      const rows = buildGroupedRows(values, {groupBy: ["state"], columns, sort: [{id: "state", direction: "desc"}]});

      expect(groups(rows).map(group => group.value)).toEqual(["1", "0", null]);
    });
  });

  describe("sorting", () => {
    it("sorts the data rows inside each group with the sort list (several columns)", () => {
      expect(kinds(build({sort: [{id: "name", direction: "desc"}]}))).toEqual(["G1:DE", "r5", "r2", "S1:DE", "G1:ES", "r6", "r4", "r3", "r1", "S1:ES"]);
      expect(kinds(build({sort: [{id: "city", direction: "asc"}, {id: "amount", direction: "asc"}]})))
        .toEqual(["G1:DE", "r5", "r2", "S1:DE", "G1:ES", "r3", "r6", "r4", "r1", "S1:ES"]);
    });

    it("finds the column of a sort entry by its sort field", () => {
      const columns = COLUMNS.map(column => (column.name === "name" ? {...column, sortField: "sortName"} : column));

      expect(kinds(buildGroupedRows(ROWS, {groupBy: ["country"], columns, sort: [{id: "sortName", direction: "desc"}]})))
        .toEqual(["G1:DE", "r5", "r2", "S1:DE", "G1:ES", "r6", "r4", "r3", "r1", "S1:ES"]);
    });

    it("keeps the order of the data rows without sort and puts the empty values last", () => {
      const values = [{id: "a", g: 1, n: null}, {id: "b", g: 1, n: 2}, {id: "c", g: 1, n: 1}];
      const columns = [{name: "g", label: "G"}, {name: "n", label: "N"}];

      expect(kinds(buildGroupedRows(values, {groupBy: ["g"], columns}))).toEqual(["G1:1", "a", "b", "c", "S1:1"]);
      expect(kinds(buildGroupedRows(values, {groupBy: ["g"], columns, sort: [{id: "n", direction: "asc"}]}))).toEqual(["G1:1", "c", "b", "a", "S1:1"]);
    });

    it("orders the groups of a level by its group-by column when it is the first sort", () => {
      expect(kinds(build({groupBy: ["country", "city"], sort: [{id: "country", direction: "desc"}]}))).toEqual([
        "G1:ES", "G2:Bilbao", "r3", "S2:Bilbao", "G2:Madrid", "r1", "r4", "r6", "S2:Madrid", "S1:ES",
        "G1:DE", "G2:Berlin", "r2", "r5", "S2:Berlin", "S1:DE"
      ]);
      expect(groups(build({groupBy: ["country", "city"], sort: [{id: "city", direction: "desc"}]})).map(group => group.label))
        .toEqual(["DE", "Berlin", "ES", "Madrid", "Bilbao"]);
    });

    it("orders the groups of every level by an aggregate when its column is the first sort", () => {
      const asc = build({groupBy: ["country", "city"], sort: [{id: "amount", direction: "asc"}]});
      const desc = build({groupBy: ["country", "city"], sort: [{id: "amount", direction: "desc"}]});

      expect(groups(asc).map(group => group.label)).toEqual(["DE", "Berlin", "ES", "Bilbao", "Madrid"]);
      expect(groups(desc).map(group => group.label)).toEqual(["ES", "Madrid", "Bilbao", "DE", "Berlin"]);
      // The data rows inside the groups follow the same sort
      expect(kinds(desc).slice(0, 6)).toEqual(["G1:ES", "G2:Madrid", "r1", "r4", "r6", "S2:Madrid"]);
    });
  });

  describe("expanded state", () => {
    it("starts with every group expanded", () => {
      expect(groups(build({groupBy: ["country", "city"]})).every(group => group.expanded)).toBe(true);
    });

    it("hides the children and the subtotal of a collapsed group and keeps its row", () => {
      const rows = build({groupBy: ["country", "city"], expanded: path => path !== "ES"});

      expect(kinds(rows)).toEqual(["G1:DE", "G2:Berlin", "r2", "r5", "S2:Berlin", "S1:DE", "G1:ES"]);
      expect(rows[6].$group).toMatchObject({expanded: false, count: 4});
    });

    it("hides the rows of a collapsed inner group", () => {
      const rows = build({groupBy: ["country", "city"], expanded: path => path !== "ES/Madrid"});

      expect(kinds(rows)).toEqual(["G1:DE", "G2:Berlin", "r2", "r5", "S2:Berlin", "S1:DE", "G1:ES", "G2:Bilbao", "r3", "S2:Bilbao", "G2:Madrid", "S1:ES"]);
    });

    it("reads the expanded state from a map of paths", () => {
      expect(kinds(build({expanded: {DE: false}}))).toEqual(["G1:DE", "G1:ES", "r1", "r3", "r4", "r6", "S1:ES"]);
    });

    it("leaves out the subtotal rows when the grid has no subtotals", () => {
      expect(kinds(build({groupBy: ["country", "city"], subtotals: false}))).toEqual([
        "G1:DE", "G2:Berlin", "r2", "r5", "G1:ES", "G2:Bilbao", "r3", "G2:Madrid", "r1", "r4", "r6"
      ]);
    });
  });
});
