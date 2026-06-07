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
});
