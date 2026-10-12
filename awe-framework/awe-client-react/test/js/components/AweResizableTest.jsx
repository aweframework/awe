import React from 'react';
import {cleanup, fireEvent, screen} from '@testing-library/react';
import AweResizable from "../../../src/components/AweResizable";
import SizeRegistry from "../../../src/redux/registry/SizeRegistry";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/components/AweResizableTest.jsx', () => {

  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  const preloadedState = {};
  const children = [
    {elementType: "Tag", type: "div", id: "first", label: "primero", elementList: []},
    {elementType: "Tag", type: "div", id: "second", label: "segundo", elementList: []}
  ];

  const renderResizable = (props = {}) => renderWithProviders(
    <AweResizable directions="right" style="estilo-especifico" elementList={children.slice(0, 1)} {...props}/>,
    {preloadedState});

  const mockSize = (width, height) => {
    jest.spyOn(Element.prototype, "getBoundingClientRect").mockReturnValue({width, height});
  };

  it('renders the child inside a container with the style of the resizable', async () => {
    renderResizable();

    const container = document.querySelector(".resizable.estilo-especifico");
    expect(container).not.toBeNull();
    expect(container.querySelector("#first")).not.toBeNull();
    expect(await screen.findByText("primero")).toBeDefined();
  });

  it('shows the handle of a resizable with a single child, as the AngularJS engine does', () => {
    renderResizable({directions: "right"});

    expect(document.querySelectorAll(".resizable > .rg-right")).toHaveLength(1);
    expect(document.querySelector(".p-splitter")).toBeNull();
  });

  it('shows one handle for each direction and ignores the unknown ones', () => {
    renderResizable({directions: "right bottom  diagonal"});

    expect(document.querySelectorAll(".resizable > [class^='rg-']")).toHaveLength(2);
    expect(document.querySelector(".rg-right")).not.toBeNull();
    expect(document.querySelector(".rg-bottom")).not.toBeNull();
  });

  it('shows no handle without directions', () => {
    renderResizable({directions: undefined});

    expect(document.querySelectorAll("[class^='rg-']")).toHaveLength(0);
  });

  it('keeps the no-resizable-* responsive styles on the container, where the stylesheet turns the handles off', () => {
    renderResizable({style: "col-xs-12 no-resizable-xs no-resizable-lg"});

    expect(document.querySelector(".resizable.no-resizable-xs.no-resizable-lg")).not.toBeNull();
  });

  it('stacks all the children inside the container instead of splitting them', () => {
    renderResizable({elementList: children});

    expect(document.querySelectorAll(".resizable #first, .resizable #second")).toHaveLength(2);
    expect(document.querySelector(".p-splitter-panel")).toBeNull();
  });

  describe('dragging a handle', () => {
    const OWN = {width: 300, height: 200};
    const PARENT = {width: 1000, height: 800};

    beforeAll(() => {
      // jsdom has no pointer events
      if (!window.PointerEvent) {
        window.PointerEvent = class extends MouseEvent {
          constructor(type, init = {}) {
            super(type, init);
            this.pointerId = init.pointerId ?? 1;
          }
        };
      }
      Element.prototype.setPointerCapture = Element.prototype.setPointerCapture || jest.fn();
      Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || jest.fn();
    });

    const setup = (direction, {own = OWN, parent = PARENT, flexDirection = "row"} = {}) => {
      jest.spyOn(Element.prototype, "getBoundingClientRect").mockImplementation(function () {
        return this.classList.contains("resizable") ? {...own} : {...parent};
      });
      const setSize = jest.spyOn(SizeRegistry, "setSize");
      const result = renderResizable({directions: direction});
      const container = document.querySelector(".resizable");
      container.parentElement.style.display = "flex";
      container.parentElement.style.flexDirection = flexDirection;
      return {...result, container, setSize, handle: document.querySelector(`.rg-${direction}`)};
    };
    const point = (type, element, x, y) => fireEvent(element, new window.PointerEvent(type, {
      bubbles: true, clientX: x, clientY: y, pointerId: 7
    }));

    it.each([
      ["right", 100, 0, 150, 0, "width", "350px"],
      ["left", 100, 0, 150, 0, "width", "250px"],
      ["bottom", 0, 50, 0, 80, "height", "230px"],
      ["top", 0, 50, 0, 20, "height", "230px"]
    ])('resizes the own axis of the %s handle and notifies the size on release', (direction, x0, y0, x1, y1, dimension, expected) => {
      const {container, setSize, handle} = setup(direction);

      point("pointerdown", handle, x0, y0);
      expect(container.classList).toContain("no-transition");
      point("pointermove", handle, x1, y1);
      expect(container.style[dimension]).toBe(expected);
      expect(setSize).not.toHaveBeenCalled();

      point("pointerup", handle, x1, y1);
      expect(container.classList).not.toContain("no-transition");
      expect(setSize).toHaveBeenCalledWith(expect.objectContaining({current: parseInt(expected)}));
    });

    it('captures the pointer so touch and pen drags keep reaching the handle', () => {
      const capture = jest.spyOn(Element.prototype, "setPointerCapture");
      const {handle} = setup("right");

      point("pointerdown", handle, 100, 0);

      expect(capture).toHaveBeenCalledWith(7);
    });

    it('also sets the flex basis when the parent lays out along the axis of the handle', () => {
      const {container, handle} = setup("right", {flexDirection: "row"});

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 150, 0);

      expect(container.style.flexBasis).toBe("350px");
    });

    it('leaves the flex basis alone when the parent lays out across the axis of the handle', () => {
      const {container, handle} = setup("right", {flexDirection: "column"});

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 150, 0);

      expect(container.style.width).toBe("350px");
      expect(container.style.flexBasis).toBe("");
      expect(container.style.height).toBe("");
    });

    it.each([
      ["zero", -500, "160px"],
      ["negative", -5000, "160px"],
      ["too large", 5000, "990px"]
    ])('clamps a %s size to the limits of the stylesheet', (name, delta, expected) => {
      const {container, setSize, handle} = setup("right");

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 100 + delta, 0);
      expect(container.style.width).toBe(expected);

      point("pointerup", handle, 100 + delta, 0);
      expect(setSize).toHaveBeenCalledWith(expect.objectContaining({current: parseInt(expected)}));
    });

    it('does not limit the size by the parent when the parent has no size yet', () => {
      const {container, handle} = setup("right", {parent: {width: 0, height: 0}});

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 600, 0);

      expect(container.style.width).toBe("800px");
    });

    it('stops resizing once the handle is released', () => {
      const {container, handle} = setup("right");

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 120, 0);
      point("pointerup", handle, 120, 0);
      point("pointermove", handle, 400, 0);

      expect(container.style.width).toBe("320px");
    });

    it('stops resizing when the pointer is cancelled', () => {
      const {container, handle} = setup("right");

      point("pointerdown", handle, 100, 0);
      point("pointercancel", handle, 100, 0);
      point("pointermove", handle, 400, 0);

      expect(container.classList).not.toContain("no-transition");
      expect(container.style.width).toBe("");
    });

    it('ignores the movement of a pointer that is not dragging', () => {
      const {container, handle} = setup("right");

      point("pointermove", handle, 400, 0);

      expect(container.style.width).toBe("");
    });

    it('does not leave document listeners behind when it is unmounted while dragging', () => {
      const add = jest.spyOn(document, "addEventListener");
      const {unmount, handle} = setup("right");

      point("pointerdown", handle, 100, 0);
      unmount();

      expect(add.mock.calls.filter(([type]) => /pointer|mouse|touch/.test(type))).toHaveLength(0);
    });
  });
});
