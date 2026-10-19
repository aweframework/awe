import {
  applyColumnLayout,
  buildHeader,
  getColumnGroups,
  mergeColumnLayout,
  moveColumn,
  moveColumnTo,
  setColumnVisible
} from "../../../src/utilities/gridLayout";

const cols = (...names) => names.map(name => ({ name, label: name.toUpperCase() }));

describe("awe-react-client/test/js/utilities/gridLayoutTest.js", () => {
  describe("getColumnGroups", () => {
    it("uses the column names of each group header", () => {
      const headerModel = [{ startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"] }];
      expect(getColumnGroups(headerModel, cols("a", "b", "c", "d"))).toEqual({ b: 0, c: 0 });
    });

    it("skips the columns the screen hides when a header has no column names", () => {
      const headerModel = [{ startColumnName: "b", numberOfColumns: 2 }];
      const model = [...cols("a", "b"), { name: "key", hidden: true }, ...cols("c", "d")];
      expect(getColumnGroups(headerModel, model)).toEqual({ b: 0, c: 0 });
    });

    it("derives the members of a group header without column names from its position in the visible columns", () => {
      const headerModel = [{ startColumnName: "b", numberOfColumns: 2 }];
      expect(getColumnGroups(headerModel, cols("a", "b", "c", "d"))).toEqual({ b: 0, c: 0 });
    });
  });

  describe("buildHeader", () => {
    const headerModel = [{ startColumnName: "b", numberOfColumns: 3, label: "G", columnNames: ["b", "c", "d"] }];

    it("spans the group over its visible columns", () => {
      const header = buildHeader(cols("a", "b", "c", "d", "e"), headerModel, cols("a", "b", "c", "d", "e"));

      expect(header.columns.map(col => col.name ?? `group:${col.startColumnName}:${col.numberOfColumns}`))
        .toEqual(["a", "group:b:3", "e"]);
      expect(header.grouped.map(col => col.name)).toEqual(["b", "c", "d"]);
    });

    it("keeps the headers aligned when the first column of a group is hidden", () => {
      const header = buildHeader(cols("a", "c", "d", "e"), headerModel, cols("a", "b", "c", "d", "e"));

      expect(header.columns.map(col => col.name ?? `group:${col.startColumnName}:${col.numberOfColumns}`))
        .toEqual(["a", "group:c:2", "e"]);
      expect(header.grouped.map(col => col.name)).toEqual(["c", "d"]);
    });

    it("keeps the headers aligned when a column inside the group is hidden", () => {
      const header = buildHeader(cols("a", "b", "d", "e"), headerModel, cols("a", "b", "c", "d", "e"));

      expect(header.columns.find(col => col.startColumnName).numberOfColumns).toBe(2);
      expect(header.grouped.map(col => col.name)).toEqual(["b", "d"]);
    });

    it("does not render a group without visible columns", () => {
      const header = buildHeader(cols("a", "e"), headerModel, cols("a", "b", "c", "d", "e"));

      expect(header.columns.map(col => col.name)).toEqual(["a", "e"]);
      expect(header.grouped).toEqual([]);
    });

    it("works with group headers that only know their start column and count", () => {
      const legacy = [{ startColumnName: "b", numberOfColumns: 2, label: "G" }];
      const header = buildHeader(cols("a", "c", "d"), legacy, cols("a", "b", "c", "d"));

      expect(header.columns.find(col => col.startColumnName).numberOfColumns).toBe(1);
      expect(header.grouped.map(col => col.name)).toEqual(["c"]);
    });
  });

  describe("mergeColumnLayout", () => {
    const model = cols("a", "b", "c", "d");

    it("keeps the XML order without a stored layout", () => {
      expect(mergeColumnLayout(model, [], null)).toEqual({ order: ["a", "b", "c", "d"], hidden: [] });
    });

    it("applies the stored order and hidden columns, ignoring unknown ids", () => {
      const layout = { order: ["d", "gone", "a", "b", "c"], hidden: ["b", "gone"] };
      expect(mergeColumnLayout(model, [], layout)).toEqual({ order: ["d", "a", "b", "c"], hidden: ["b"] });
    });

    it("places a column added to the XML at its position", () => {
      expect(mergeColumnLayout(cols("n", "a", "b"), [], { order: ["b", "a"], hidden: [] }).order).toEqual(["n", "b", "a"]);
    });

    it("never moves a column out of its group", () => {
      const headerModel = [{ startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"] }];
      const layout = { order: ["c", "a", "d", "b"], hidden: [] };
      expect(mergeColumnLayout(model, headerModel, layout).order).toEqual(["a", "c", "b", "d"]);
    });

    it("leaves out the columns hidden by the screen", () => {
      const withHidden = [...model, { name: "key", hidden: true }];
      expect(mergeColumnLayout(withHidden, [], { order: ["key", "b", "a"], hidden: ["key"] })).toEqual({ order: ["b", "a", "c", "d"], hidden: [] });
    });

    it("never hides every column", () => {
      expect(mergeColumnLayout(cols("a", "b"), [], { order: ["a", "b"], hidden: ["a", "b"] }).hidden).toEqual([]);
    });
  });

  describe("applyColumnLayout", () => {
    it("returns the visible columns in the layout order, keeping the columns hidden by the screen out", () => {
      const model = [...cols("a", "b", "c"), { name: "key", hidden: true }];
      const visible = applyColumnLayout(model, [], { order: ["c", "a", "b"], hidden: ["a"] });

      expect(visible.map(col => col.name)).toEqual(["c", "b"]);
    });

    it("returns the columns of the screen when there is no layout", () => {
      const model = [...cols("a", "b"), { name: "key", hidden: true }];

      expect(applyColumnLayout(model, [], undefined).map(col => col.name)).toEqual(["a", "b"]);
    });
  });

  describe("moveColumn", () => {
    const headerModel = [{ startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"] }];
    const model = cols("a", "b", "c", "d", "e");
    const layout = { order: ["a", "b", "c", "d", "e"], hidden: [] };

    it("moves a column among the columns of its group", () => {
      expect(moveColumn(model, headerModel, layout, "c", -1).order).toEqual(["a", "c", "b", "d", "e"]);
    });

    it("does not move a column out of its group", () => {
      expect(moveColumn(model, headerModel, layout, "b", -1)).toEqual(layout);
      expect(moveColumn(model, headerModel, layout, "c", 1)).toEqual(layout);
    });

    it("moves ungrouped columns among the ungrouped ones, jumping over a group", () => {
      expect(moveColumn(model, headerModel, layout, "d", -1).order).toEqual(["d", "b", "c", "a", "e"]);
    });

    it("ignores columns that do not exist", () => {
      expect(moveColumn(model, headerModel, layout, "zzz", 1)).toEqual(layout);
    });
  });

  describe("moveColumnTo", () => {
    const headerModel = [{ startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"] }];
    const model = cols("a", "b", "c", "d", "e");
    const layout = { order: ["a", "b", "c", "d", "e"], hidden: [] };

    it("puts a column where a sibling is", () => {
      expect(moveColumnTo(model, headerModel, layout, "e", "a").order).toEqual(["e", "b", "c", "a", "d"]);
      expect(moveColumnTo(model, headerModel, layout, "b", "c").order).toEqual(["a", "c", "b", "d", "e"]);
    });

    it("ignores a target that is not a sibling", () => {
      expect(moveColumnTo(model, headerModel, layout, "b", "d")).toEqual(layout);
      expect(moveColumnTo(model, headerModel, layout, "a", "c")).toEqual(layout);
    });

    it("ignores a column dropped on itself", () => {
      expect(moveColumnTo(model, headerModel, layout, "d", "d")).toEqual(layout);
    });
  });

  describe("setColumnVisible", () => {
    const model = cols("a", "b", "c");

    it("hides and shows a column", () => {
      const hidden = setColumnVisible(model, [], { order: ["a", "b", "c"], hidden: [] }, "b", false);
      expect(hidden.hidden).toEqual(["b"]);
      expect(setColumnVisible(model, [], hidden, "b", true).hidden).toEqual([]);
    });

    it("keeps at least one column visible", () => {
      const layout = { order: ["a", "b", "c"], hidden: ["a", "b"] };
      expect(setColumnVisible(model, [], layout, "c", false)).toEqual(layout);
    });
  });
});
