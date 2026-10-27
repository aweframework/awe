// Source-traceable focused Jest parity for components/resizable.js.
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";

describe("components/resizable.js", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  it("migrates directional drag updates and resize publication", () => {
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue({values: []});
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue({
      id: "Resizable",
      style: "demo-style",
      directions: "top right bottom left",
      visible: true
    });
    jest.spyOn(refs.$utilities, "publish").mockImplementation(jest.fn());
    jest.spyOn(refs.$utilities, "stopPropagation").mockImplementation(jest.fn());
    jest.spyOn(window, "getComputedStyle").mockReturnValue({
      getPropertyValue: property => property === "width" ? "200px" : "100px"
    });

    const element = refs.$compile("<awe-resizable resizable-id='Resizable'></awe-resizable>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    Object.defineProperty(element[0], "offsetWidth", {configurable: true, value: 200});
    Object.defineProperty(element[0], "offsetHeight", {configurable: true, value: 100});
    element[0].style.width = "200px";
    element[0].style.height = "100px";

    const scope = element.isolateScope() || element.scope();
    expect(scope.directions).toEqual(["top", "right", "bottom", "left"]);

    scope.dragStart({clientX: 200, clientY: 100}, "right");
    document.dispatchEvent(new MouseEvent("mousemove", {clientX: 240, clientY: 100}));
    expect(element[0].style.flexBasis).toBe("240px");

    document.dispatchEvent(new MouseEvent("mouseup"));
    expect(element.hasClass("no-transition")).toBe(false);
    expect(refs.$utilities.publish).toHaveBeenCalledWith("resize-action");
    expect(refs.$utilities.stopPropagation).toHaveBeenCalledWith({clientX: 200, clientY: 100}, true);
  });

  function compileResizable(directions = "top right bottom left") {
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockReturnValue({values: []});
    jest.spyOn(refs.$control, "getAddressController").mockReturnValue({id: "Resizable", style: "demo-style", directions, visible: true});
    jest.spyOn(refs.$utilities, "publish").mockImplementation(jest.fn());
    jest.spyOn(refs.$utilities, "stopPropagation").mockImplementation(jest.fn());
    jest.spyOn(window, "getComputedStyle").mockReturnValue({getPropertyValue: property => property === "width" ? "200px" : "100px"});
    const element = refs.$compile("<awe-resizable resizable-id='Resizable'></awe-resizable>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    Object.defineProperty(element[0], "offsetWidth", {configurable: true, value: 200});
    Object.defineProperty(element[0], "offsetHeight", {configurable: true, value: 100});
    element[0].style.width = "200px";
    element[0].style.height = "100px";
    return {element, scope: element.isolateScope() || element.scope()};
  }

  it("checks drag start from the left edge", () => {
    const {element, scope} = compileResizable("left");

    scope.dragStart({clientX: 200, clientY: 100}, "left");
    document.dispatchEvent(new MouseEvent("mousemove", {clientX: 150, clientY: 100}));

    expect(element[0].style.flexBasis).toBe("250px");
  });

  it("checks drag start from the top edge", () => {
    const {element, scope} = compileResizable("top");

    scope.dragStart({clientX: 200, clientY: 100}, "top");
    document.dispatchEvent(new MouseEvent("mousemove", {clientX: 200, clientY: 80}));

    expect(element[0].style.flexBasis).toBe("120px");
  });

  it("checks drag start from the bottom edge", () => {
    const {element, scope} = compileResizable("bottom");

    scope.dragStart({clientX: 200, clientY: 100}, "bottom");
    document.dispatchEvent(new MouseEvent("mousemove", {clientX: 200, clientY: 140}));

    expect(element[0].style.flexBasis).toBe("140px");
  });

  it("publishes resize and clears no-transition on mouseup", () => {
    const {element, scope} = compileResizable("right");

    scope.dragStart({clientX: 200, clientY: 100}, "right");
    document.dispatchEvent(new MouseEvent("mouseup"));

    expect(element.hasClass("no-transition")).toBe(false);
    expect(refs.$utilities.publish).toHaveBeenCalledWith("resize-action");
  });

  describe("with an adjacent sibling on the handle side", () => {
    // Computed style by element: the parent lays out in the given direction, the siblings carry their own minimum
    function compileBetweenSiblings(direction, {flexDirection = "row", display = "flex", next = {width: 400, height: 300},
      prev = {width: 250, height: 250}, nextMinWidth = "", nextContentMin = 0, ownContentMin = 0} = {}) {
      const {element, scope} = compileResizable(direction);
      const parent = document.createElement("div");
      const before = document.createElement("div");
      const after = document.createElement("div");
      after.style.minWidth = nextMinWidth;
      parent.append(before, element[0], after);
      const sizes = new Map([[before, prev], [after, next]]);
      const contentMin = new Map([[before, 0], [after, nextContentMin]]);
      const along = flexDirection.startsWith("row") ? "width" : "height";
      // A sibling laid out with a fixed flex basis gets it along the axis, unless its content does not let it be smaller
      jest.spyOn(window.Element.prototype, "getBoundingClientRect").mockImplementation(function () {
        if (this === element[0]) {
          const own = parseFloat(this.style.flexBasis);
          return Number.isFinite(own) ? {width: 200, height: 100, [along]: Math.max(own, ownContentMin)} : {width: 200, height: 100};
        }
        if (!sizes.has(this)) {
          return {width: 0, height: 0};
        }
        const basis = parseFloat((this.style.flex || "").split(" ")[2]);
        return Number.isFinite(basis) ? {...sizes.get(this), [along]: Math.max(basis, contentMin.get(this))} : {...sizes.get(this)};
      });
      window.getComputedStyle.mockImplementation(target => ({
        display: target === parent ? display : "",
        flexDirection: target === parent ? flexDirection : "",
        minWidth: target.style.minWidth,
        minHeight: target.style.minHeight,
        getPropertyValue: property => property === "width" ? "200px" : "100px"
      }));
      return {element, scope, before, after};
    }
    const move = (x, y) => document.dispatchEvent(new MouseEvent("mousemove", {clientX: x, clientY: y}));

    it("shrinks only the next sibling when a right handle grows the container", () => {
      const {element, scope, before, after} = compileBetweenSiblings("right");

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(240, 100);

      expect(element[0].style.flexBasis).toBe("240px");
      expect(after.style.flex).toBe("0 0 360px");
      expect(before.style.flex).toBe("");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("leaves the neighbour fluid after a press on the handle without a move", () => {
      const {element, scope, after} = compileBetweenSiblings("right");

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      document.dispatchEvent(new MouseEvent("mouseup"));

      expect(after.style.flex).toBe("");
      expect(element[0].style.flexBasis).toBe("");
    });

    it("grows the next sibling when a right handle shrinks the container", () => {
      const {element, scope, after} = compileBetweenSiblings("right");

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(170, 100);

      expect(element[0].style.flexBasis).toBe("170px");
      expect(after.style.flex).toBe("0 0 430px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("uses the previous sibling for a left handle", () => {
      const {element, scope, before, after} = compileBetweenSiblings("left");

      scope.dragStart({clientX: 200, clientY: 100}, "left");
      move(150, 100);

      expect(element[0].style.flexBasis).toBe("250px");
      expect(before.style.flex).toBe("0 0 200px");
      expect(after.style.flex).toBe("");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("uses the next sibling for a bottom handle and the previous one for a top handle", () => {
      const bottom = compileBetweenSiblings("bottom", {flexDirection: "column"});
      bottom.scope.dragStart({clientX: 200, clientY: 100}, "bottom");
      move(200, 140);
      // The container starts at 100px high, under the minimum size: it goes to 160px and the neighbour gives the 60px
      expect(bottom.element[0].style.flexBasis).toBe("160px");
      expect(bottom.after.style.flex).toBe("0 0 240px");
      document.dispatchEvent(new MouseEvent("mouseup"));

      const top = compileBetweenSiblings("top", {flexDirection: "column"});
      top.scope.dragStart({clientX: 200, clientY: 100}, "top");
      move(200, 80);
      expect(top.element[0].style.flexBasis).toBe("160px");
      expect(top.before.style.flex).toBe("0 0 190px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("gives back what the neighbour cannot shrink below its content, so its outer edge does not move", () => {
      const {element, scope, after} = compileBetweenSiblings("right", {nextContentMin: 380});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(240, 100);

      // The neighbour asked for 360px stays at the 380px of its content: the container only takes the 20px it gave
      expect(after.style.flex).toBe("0 0 380px");
      expect(element[0].style.flexBasis).toBe("220px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("takes back from the neighbour what the container cannot shrink below its content", () => {
      const {element, scope, after} = compileBetweenSiblings("right", {ownContentMin: 180});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(150, 100);

      // The container asked for 150px stays at the 180px of its content: the neighbour only grows the 20px it got
      expect(element[0].style.flexBasis).toBe("180px");
      expect(after.style.flex).toBe("0 0 420px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("does not shrink the container under the minimum size", () => {
      const {element, scope, after} = compileBetweenSiblings("right");

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(0, 100);

      // The container stops at 160px and the neighbour only takes the 40px it gave
      expect(element[0].style.flexBasis).toBe("160px");
      expect(after.style.flex).toBe("0 0 440px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("keeps the previous behaviour when the parent is not a flex container", () => {
      // A block parent still computes flex-direction: row, so the display decides
      const {element, scope, after} = compileBetweenSiblings("right", {display: "block"});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(240, 100);

      expect(element[0].style.flexBasis).toBe("240px");
      expect(after.style.flex).toBe("");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("stops the container when the neighbour reaches its minimum size", () => {
      const {element, scope, after} = compileBetweenSiblings("right");

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(1000, 100);

      expect(after.style.flex).toBe("0 0 160px");
      expect(element[0].style.flexBasis).toBe("440px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("respects the min-width of the neighbour", () => {
      const {element, scope, after} = compileBetweenSiblings("right", {nextMinWidth: "300px"});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(1000, 100);

      expect(after.style.flex).toBe("0 0 300px");
      expect(element[0].style.flexBasis).toBe("300px");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("keeps the previous behaviour when the parent lays out across the axis of the handle", () => {
      const {element, scope, after} = compileBetweenSiblings("right", {flexDirection: "column"});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(240, 100);

      expect(element[0].style.flexBasis).toBe("240px");
      expect(after.style.flex).toBe("");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });

    it("keeps the previous behaviour when the neighbour is not rendered", () => {
      const {element, scope, after} = compileBetweenSiblings("right", {next: {width: 0, height: 0}});

      scope.dragStart({clientX: 200, clientY: 100}, "right");
      move(240, 100);

      expect(element[0].style.flexBasis).toBe("240px");
      expect(after.style.flex).toBe("");
      document.dispatchEvent(new MouseEvent("mouseup"));
    });
  });
});
