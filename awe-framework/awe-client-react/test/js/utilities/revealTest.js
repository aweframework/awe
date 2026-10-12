import {
  findFirstInvalid,
  focusComponent,
  getInvalidPanelValues,
  getRevealSelections,
  getStructureOrder
} from "../../../src/utilities/reveal";

const ERROR = {message: "x"};

const component = (id, source, error = null, model = {}) => ({
  address: {view: "base", component: id},
  context: {view: "base", source},
  attributes: {error},
  model
});

const tabs = (selected) => ({
  address: {view: "base", component: "tabs"},
  context: {view: "base", source: ["tabs"]},
  attributes: {},
  model: {values: [{value: "one", selected: selected === "one"}, {value: "two", selected: selected === "two"}]}
});

const accordion = (attributes) => ({
  address: {view: "base", component: "acc"},
  attributes,
  model: {values: [{value: "i1", selected: true}, {value: "i2", selected: false}]}
});

describe("awe-react-client/test/js/utilities/revealTest.js", () => {
  describe("getStructureOrder", () => {
    it("lists the ids in document order and ignores dependency elements", () => {
      const structure = {
        id: "root",
        elementList: [
          {id: "a", elementList: [{id: "dep", elementType: "DependencyElement"}, {id: "b"}]},
          {id: "c"}
        ]
      };
      expect(getStructureOrder(structure)).toEqual(["root", "a", "b", "c"]);
    });

    it("returns an empty list without structure", () => {
      expect(getStructureOrder(undefined)).toEqual([]);
    });
  });

  describe("findFirstInvalid", () => {
    const components = {
      late: component("late", ["late"], ERROR),
      early: component("early", ["early"], ERROR),
      ok: component("ok", ["ok"])
    };

    it("returns null when every component is valid", () => {
      expect(findFirstInvalid({ok: components.ok})).toBeNull();
    });

    it("returns the first invalid component of the state when there is no structure", () => {
      expect(findFirstInvalid(components).address.component).toBe("late");
    });

    it("returns the invalid component that comes first in the screen structure", () => {
      expect(findFirstInvalid(components, ["early", "late"]).address.component).toBe("early");
    });

    it("sends the components out of the structure to the end", () => {
      expect(findFirstInvalid(components, ["early"]).address.component).toBe("early");
      expect(findFirstInvalid(components, ["unknown"]).address.component).toBe("late");
    });
  });

  describe("getRevealSelections", () => {
    it("selects the tab that holds the invalid field", () => {
      const target = component("field", ["tabs", "two", "field"], ERROR);
      const selections = getRevealSelections({tabs: tabs("one"), field: target}, target);

      expect(selections).toHaveLength(1);
      expect(selections[0].address.component).toBe("tabs");
      expect(selections[0].values.map(item => item.selected)).toEqual([false, true]);
    });

    it("does not change a panel that already shows the field", () => {
      const target = component("field", ["tabs", "two", "field"], ERROR);
      expect(getRevealSelections({tabs: tabs("two"), field: target}, target)).toEqual([]);
    });

    it("opens every nested panel, from the outer one to the inner one", () => {
      const inner = {
        address: {view: "base", component: "inner"},
        attributes: {},
        model: {values: [{value: "a", selected: true}, {value: "b", selected: false}]}
      };
      const target = component("field", ["tabs", "two", "inner", "b", "field"], ERROR);
      const selections = getRevealSelections({tabs: tabs("one"), inner, field: target}, target);

      expect(selections.map(selection => selection.address.component)).toEqual(["tabs", "inner"]);
    });

    it("keeps the other items open in an accordion that allows several", () => {
      const target = component("field", ["acc", "i2", "field"], ERROR);
      const [selection] = getRevealSelections({acc: accordion({autocollapse: false}), field: target}, target);

      expect(selection.values.map(item => item.selected)).toEqual([true, true]);
    });

    it("collapses the other items in an accordion with autocollapse", () => {
      const target = component("field", ["acc", "i2", "field"], ERROR);
      const [selection] = getRevealSelections({acc: accordion({}), field: target}, target);

      expect(selection.values.map(item => item.selected)).toEqual([false, true]);
    });

    it("ignores components without context or without a target", () => {
      expect(getRevealSelections({}, null)).toEqual([]);
      expect(getRevealSelections({}, {address: {component: "x"}})).toEqual([]);
      const target = component("field", ["missing", "one", "field"], ERROR);
      expect(getRevealSelections({field: target}, target)).toEqual([]);
    });

    it("ignores a parent that is not a list of options for its child", () => {
      const parent = {address: {view: "base", component: "box"}, attributes: {}, model: {}};
      const target = component("field", ["box", "field"], ERROR);
      expect(getRevealSelections({box: parent, field: target}, target)).toEqual([]);
    });
  });

  describe("getInvalidPanelValues", () => {
    it("lists the items of a panel that contain invalid fields", () => {
      const components = {
        a: component("a", ["tabs", "two", "a"], ERROR),
        b: component("b", ["tabs", "one", "b"]),
        c: component("c", ["tabs", "two", "c"], ERROR),
        d: component("d", ["other", "x", "d"], ERROR),
        e: component("e", ["tabs"], ERROR)
      };
      expect(getInvalidPanelValues(components, "tabs")).toEqual(["two"]);
    });

    it("returns an empty list when the panel has no invalid fields", () => {
      expect(getInvalidPanelValues({a: component("a", ["tabs", "one", "a"])}, "tabs")).toEqual([]);
    });
  });

  describe("focusComponent", () => {
    afterEach(() => {
      document.body.innerHTML = "";
      jest.useRealTimers();
    });

    it("focuses the input of the criterion and scrolls it into view", () => {
      document.body.innerHTML = `<div criterion-id="field"><input id="inner"/></div>`;
      const wrapper = document.querySelector("[criterion-id='field']");
      wrapper.scrollIntoView = jest.fn();

      expect(focusComponent("field")).toBe(true);
      expect(document.activeElement.id).toBe("inner");
      expect(wrapper.scrollIntoView).toHaveBeenCalledWith({block: "center", behavior: "smooth"});
    });

    it("scrolls to a criterion that has no control to focus", () => {
      document.body.innerHTML = `<div criterion-id="field"><span>text</span></div>`;
      const wrapper = document.querySelector("[criterion-id='field']");
      wrapper.scrollIntoView = jest.fn();

      expect(focusComponent("field")).toBe(true);
      expect(wrapper.scrollIntoView).toHaveBeenCalled();
    });

    it("waits for a criterion that is rendered later", () => {
      jest.useFakeTimers();
      expect(focusComponent("late", {attempts: 5, delay: 10})).toBe(false);
      document.body.innerHTML = `<div criterion-id="late"><input id="late-input"/></div>`;

      jest.advanceTimersByTime(20);

      expect(document.activeElement.id).toBe("late-input");
    });

    it("gives up when the criterion never shows up", () => {
      jest.useFakeTimers();
      expect(focusComponent("never", {attempts: 2, delay: 10})).toBe(false);
      jest.advanceTimersByTime(100);
      expect(jest.getTimerCount()).toBe(0);
    });
  });
});
