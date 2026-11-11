import {elementIdAt, markInspect, revealHiddenElements, swallowInspectEvents} from "../../../main/resources/js/awe/services/screenInspect";

describe("screenInspect", () => {
  describe("revealHiddenElements", () => {
    it("shows the hidden configurable elements and tells which ones were hidden", () => {
      const components = [
        {id: "name", controller: {visible: false}},
        {id: "age", controller: {visible: true}},
        {id: "other", controller: {visible: false}}
      ];

      const {hidden} = revealHiddenElements(components, new Set(["name", "age"]));

      expect([...hidden]).toEqual(["name"]);
      expect(components.map(component => component.controller.visible)).toEqual([true, true, false]);
    });

    it("draws the hidden columns of a grid narrow and tells which ones they are", () => {
      const components = [{id: "grid", controller: {columnModel: [{name: "a", hidden: true, width: 0}, {name: "b", hidden: false, width: 50}]}}];

      const {hiddenColumns} = revealHiddenElements(components, new Set(["grid", "a", "b"]));

      expect([...hiddenColumns]).toEqual(["a"]);
      expect(components[0].controller.columnModel[0].hidden).toBe(false);
      expect(components[0].controller.columnModel[1].width).toBe(50);
    });
  });

  describe("on the rendered screen", () => {
    let root;
    beforeEach(() => {
      root = document.createElement("div");
      root.innerHTML = `<div class="grp"><awe-input-text criterion-id="name"><input></awe-input-text></div>
        <awe-button button-id="save"><button>Save</button></awe-button>
        <awe-grid grid-id="grid"><div column-id="a">A</div><div column-id="b">B</div></awe-grid>`;
    });

    it("marks the configurable elements with their state", () => {
      markInspect(root, {configurable: new Set(["name", "save"]), selected: "save", personalized: new Set(["name"]), hidden: new Set(["name"]), hiddenColumns: new Set()});

      const name = root.querySelector("[criterion-id=name]");
      const save = root.querySelector("[button-id=save]");
      expect(name.classList.contains("awe-inspect-node")).toBe(true);
      expect(name.classList.contains("awe-inspect-hidden")).toBe(true);
      expect(name.classList.contains("awe-inspect-personalized")).toBe(true);
      expect(save.classList.contains("awe-inspect-selected")).toBe(true);
      expect(root.querySelector("[grid-id=grid]").classList.contains("awe-inspect-node")).toBe(false);
    });

    it("marks the cells of a configurable column, dimmed when the column is hidden", () => {
      markInspect(root, {configurable: new Set(["a", "b"]), selected: "b", personalized: new Set(), hidden: new Set(), hiddenColumns: new Set(["a"])});

      const [a, b] = root.querySelectorAll("[column-id]");
      expect(a.classList.contains("awe-inspect-hidden")).toBe(true);
      expect(b.classList.contains("awe-inspect-selected")).toBe(true);
      expect(b.classList.contains("awe-inspect-hidden")).toBe(false);
    });

    it("forgets the state of an element that stops being selected or hidden", () => {
      const state = {configurable: new Set(["name"]), selected: "name", personalized: new Set(), hidden: new Set(["name"]), hiddenColumns: new Set()};
      markInspect(root, state);
      markInspect(root, {...state, selected: null, hidden: new Set()});

      const name = root.querySelector("[criterion-id=name]");
      expect(name.classList.contains("awe-inspect-selected")).toBe(false);
      expect(name.classList.contains("awe-inspect-hidden")).toBe(false);
    });

    it("finds the configurable element under a click, the column before the grid", () => {
      const configurable = new Set(["name", "grid", "a"]);

      expect(elementIdAt(root, root.querySelector("input"), configurable)).toBe("name");
      expect(elementIdAt(root, root.querySelector("[column-id=a]"), configurable)).toBe("a");
      expect(elementIdAt(root, root.querySelector("[column-id=b]"), configurable)).toBe("grid");
      expect(elementIdAt(root, root.querySelector(".grp"), configurable)).toBeNull();
    });
  });

  describe("swallowInspectEvents", () => {
    let root;
    let button;
    let reached;
    let selected;
    let dispose;
    beforeEach(() => {
      root = document.createElement("div");
      root.innerHTML = "<div button-id=\"new\"><button type=\"button\">New</button></div><p>text</p>";
      document.body.appendChild(root);
      button = root.querySelector("button");
      reached = [];
      selected = [];
      ["pointerdown", "mousedown", "mouseup", "click", "dblclick", "keydown", "keyup"].forEach(type => button.addEventListener(type, () => reached.push(type)));
      dispose = swallowInspectEvents(root, () => new Set(["new"]), id => selected.push(id));
    });
    afterEach(() => {
      dispose();
      root.remove();
    });

    it("keeps the pointer events away from the element, so it neither runs its action nor arms a pending click", () => {
      ["pointerdown", "mousedown", "mouseup", "click", "dblclick"].forEach(type => button.dispatchEvent(new MouseEvent(type, {bubbles: true, cancelable: true})));

      expect(reached).toEqual([]);
    });

    it("selects the element on a click and on Enter or Space, and swallows those keys", () => {
      button.dispatchEvent(new MouseEvent("click", {bubbles: true, cancelable: true}));
      button.dispatchEvent(new KeyboardEvent("keydown", {key: "Enter", bubbles: true, cancelable: true}));
      button.dispatchEvent(new KeyboardEvent("keyup", {key: " ", bubbles: true, cancelable: true}));

      expect(selected).toEqual(["new", "new"]);
      expect(reached).toEqual([]);
    });

    it("swallows a click where no element is found, and leaves other keys alone", () => {
      const click = new MouseEvent("click", {bubbles: true, cancelable: true});
      root.querySelector("p").dispatchEvent(click);
      button.dispatchEvent(new KeyboardEvent("keydown", {key: "a", bubbles: true, cancelable: true}));

      expect(click.defaultPrevented).toBe(true);
      expect(selected).toEqual([]);
      expect(reached).toEqual(["keydown"]);
    });
  });
});
