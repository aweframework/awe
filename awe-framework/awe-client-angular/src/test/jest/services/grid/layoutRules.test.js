import {
  applyColumnLayout,
  getColumnGroups,
  getColumnHeaders,
  isActionColumn,
  mergeColumnLayout,
  moveColumn,
  moveColumnTo,
  setColumnVisible
} from "../../../../main/resources/js/awe/services/grid/layoutRules";

const cols = (...names) => names.map(name => ({name, label: name.toUpperCase()}));

describe("services/grid/layoutRules.js", () => {
  describe("group membership", () => {
    it("uses the column names of each group header", () => {
      const headerModel = [{startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"]}];
      expect(getColumnGroups(headerModel, cols("a", "b", "c", "d"))).toEqual({b: 0, c: 0});
    });

    it("derives the members of a header without column names from the whole model", () => {
      const headerModel = [{startColumnName: "b", numberOfColumns: 2}];
      expect(getColumnGroups(headerModel, cols("a", "b", "c", "d"))).toEqual({b: 0, c: 0});
    });

    it("maps each grouped column to its header", () => {
      const headerModel = [{label: "G1", columnNames: ["b", "c"]}, {label: "G2", columnNames: ["e"]}];
      const headers = getColumnHeaders(headerModel, cols("a", "b", "c", "d", "e"));

      expect(Object.keys(headers)).toEqual(["b", "c", "e"]);
      expect(headers.c.label).toBe("G1");
      expect(headers.e.label).toBe("G2");
    });
  });

  describe("mergeColumnLayout", () => {
    const model = cols("a", "b", "c", "d");

    it("keeps the XML order without a stored layout", () => {
      expect(mergeColumnLayout(model, [], null)).toEqual({order: ["a", "b", "c", "d"], hidden: []});
    });

    it("applies the stored order and hidden columns, ignoring unknown ids", () => {
      expect(mergeColumnLayout(model, [], {order: ["d", "gone", "a", "b", "c"], hidden: ["b", "gone"]}))
        .toEqual({order: ["d", "a", "b", "c"], hidden: ["b"]});
    });

    it("places a column added to the XML at its position", () => {
      expect(mergeColumnLayout(cols("n", "a", "b"), [], {order: ["b", "a"], hidden: []}).order).toEqual(["n", "b", "a"]);
    });

    it("never moves a column out of its group", () => {
      const headerModel = [{startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"]}];
      expect(mergeColumnLayout(model, headerModel, {order: ["c", "a", "d", "b"], hidden: []}).order).toEqual(["a", "c", "b", "d"]);
    });

    it("leaves out the columns hidden by the screen", () => {
      const withHidden = [...model, {name: "key", hidden: true}];
      expect(mergeColumnLayout(withHidden, [], {order: ["key", "b", "a"], hidden: ["key"]})).toEqual({order: ["b", "a", "c", "d"], hidden: []});
    });

    it("keeps the columns the user hid manageable", () => {
      const withHidden = [...cols("a", "b"), {name: "c", hidden: true, layoutHidden: true}];
      expect(mergeColumnLayout(withHidden, [], {order: ["c", "a", "b"], hidden: ["c"]})).toEqual({order: ["c", "a", "b"], hidden: ["c"]});
    });

    it("ignores a stored layout that is not well formed", () => {
      const model = cols("a", "b", "c");
      const identity = {order: ["a", "b", "c"], hidden: []};

      expect(mergeColumnLayout(model, [], {order: "c,b,a", hidden: ["a"]}).order).toEqual(identity.order);
      expect(mergeColumnLayout(model, [], {order: ["c", "b", "a"], hidden: "a"})).toEqual({order: ["c", "b", "a"], hidden: []});
      expect(mergeColumnLayout(model, [], {order: {0: "c"}, hidden: {0: "a"}})).toEqual(identity);
      expect(mergeColumnLayout(model, [], "oops")).toEqual(identity);
      expect(mergeColumnLayout(model, [], {order: [null, 3, "b"], hidden: [{}, "c"]})).toEqual({order: ["a", "b", "c"], hidden: ["c"]});
    });

    it("never hides every column", () => {
      expect(mergeColumnLayout(cols("a", "b"), [], {order: ["a", "b"], hidden: ["a", "b"]}).hidden).toEqual([]);
    });
  });

  describe("applyColumnLayout", () => {
    it("returns the visible columns in the layout order", () => {
      const model = [...cols("a", "b", "c"), {name: "key", hidden: true}];
      expect(applyColumnLayout(model, [], {order: ["c", "a", "b"], hidden: ["a"]}).map(col => col.name)).toEqual(["c", "b"]);
    });
  });

  describe("moving and showing columns", () => {
    const headerModel = [{startColumnName: "b", numberOfColumns: 2, columnNames: ["b", "c"]}];
    const model = cols("a", "b", "c", "d", "e");
    const layout = {order: ["a", "b", "c", "d", "e"], hidden: []};

    it("moves a column among the columns of its group", () => {
      expect(moveColumn(model, headerModel, layout, "c", -1).order).toEqual(["a", "c", "b", "d", "e"]);
    });

    it("does not move a column out of its group", () => {
      expect(moveColumn(model, headerModel, layout, "b", -1)).toEqual(layout);
      expect(moveColumn(model, headerModel, layout, "c", 1)).toEqual(layout);
    });

    it("moves ungrouped columns among the ungrouped ones", () => {
      expect(moveColumn(model, headerModel, layout, "d", -1).order).toEqual(["d", "b", "c", "a", "e"]);
    });

    it("puts a column where a sibling is, and ignores other targets", () => {
      expect(moveColumnTo(model, headerModel, layout, "e", "a").order).toEqual(["e", "b", "c", "a", "d"]);
      expect(moveColumnTo(model, headerModel, layout, "b", "d")).toEqual(layout);
      expect(moveColumnTo(model, headerModel, layout, "d", "d")).toEqual(layout);
    });

    it("hides and shows a column, keeping at least one visible", () => {
      const small = cols("a", "b");
      const hidden = setColumnVisible(small, [], {order: ["a", "b"], hidden: []}, "a", false);
      expect(hidden.hidden).toEqual(["a"]);
      expect(setColumnVisible(small, [], hidden, "b", false)).toEqual(hidden);
      expect(setColumnVisible(small, [], hidden, "a", true).hidden).toEqual([]);
    });
  });

  describe("action columns (#793)", () => {
    const button = name => ({name, label: name.toUpperCase(), component: "button"});

    it("tells a button column is an action column, and no other one", () => {
      expect(isActionColumn(button("edit"))).toBe(true);
      expect(isActionColumn({name: "a"})).toBe(false);
      expect(isActionColumn({name: "a", component: "icon"})).toBe(false);
      expect(isActionColumn(undefined)).toBe(false);
    });

    it("leaves the button columns out of the layout, whatever its stored entries say", () => {
      const model = [...cols("a", "b"), button("edit"), ...cols("c")];
      expect(mergeColumnLayout(model, [], null)).toEqual({order: ["a", "b", "c"], hidden: []});
      expect(mergeColumnLayout(model, [], {order: ["edit", "c", "b", "a"], hidden: ["edit", "b"]}))
        .toEqual({order: ["c", "b", "a"], hidden: ["b"]});
    });

    it("does not let the user hide or move a button column", () => {
      const model = [...cols("a", "b"), button("edit")];
      const layout = {order: ["a", "b"], hidden: []};
      expect(setColumnVisible(model, [], layout, "edit", false)).toEqual(layout);
      expect(moveColumn(model, [], layout, "edit", -1)).toEqual(layout);
      expect(moveColumnTo(model, [], layout, "edit", "a")).toEqual(layout);
    });

    it("keeps the button columns visible at their position of the screen", () => {
      const model = [...cols("a", "b"), button("edit"), ...cols("c"), button("more")];
      expect(applyColumnLayout(model, [], {order: ["c", "b", "a"], hidden: ["b"]}).map(col => col.name))
        .toEqual(["c", "edit", "a", "more"]);
      expect(applyColumnLayout(model, [], null).map(col => col.name)).toEqual(["a", "b", "edit", "c", "more"]);
    });
  });
});
