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

    describe('with an adjacent sibling on the handle side', () => {
      // Rects by element id, so the siblings have their own size along both axes
      // A sibling laid out with a fixed flex basis gets it along the axis, unless its content does not let it be smaller
      const withSiblings = (direction, {flexDirection = "row", display = "flex", next = {width: 400, height: 300},
        prev = {width: 250, height: 150}, nextMinWidth = "", nextMinHeight = "", nextContentMin = 0, ownContentMin = 0} = {}) => {
        const sizes = {next, prev};
        const contentMin = {next: nextContentMin, prev: 0};
        const along = flexDirection.startsWith("row") ? "width" : "height";
        jest.spyOn(Element.prototype, "getBoundingClientRect").mockImplementation(function () {
          if (this.classList.contains("resizable")) {
            const own = parseFloat(this.style.flexBasis);
            return Number.isFinite(own) ? {...OWN, [along]: Math.max(own, ownContentMin)} : {...OWN};
          }
          if (!sizes[this.id]) {
            return {...PARENT};
          }
          const basis = parseFloat((this.style.flex || "").split(" ")[2]);
          return Number.isFinite(basis)
            ? {...sizes[this.id], [along]: Math.max(basis, contentMin[this.id])}
            : {...sizes[this.id]};
        });
        const result = renderResizable({directions: direction});
        const container = document.querySelector(".resizable");
        const parent = container.parentElement;
        parent.style.display = display;
        parent.style.flexDirection = flexDirection;
        const before = document.createElement("div");
        before.id = "prev";
        const after = document.createElement("div");
        after.id = "next";
        after.style.minWidth = nextMinWidth;
        after.style.minHeight = nextMinHeight;
        parent.insertBefore(before, container);
        parent.appendChild(after);
        return {...result, container, before, after, handle: document.querySelector(`.rg-${direction}`)};
      };

      it('grows the container and shrinks only the next sibling of a right handle', () => {
        const {container, before, after, handle} = withSiblings("right");

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        expect(container.style.width).toBe("350px");
        expect(container.style.flexBasis).toBe("350px");
        expect(after.style.flex).toBe("0 0 350px");
        expect(before.style.flex).toBe("");
      });

      it('leaves the neighbour fluid after a press on the handle without a move', () => {
        const {container, after, handle} = withSiblings("right");

        point("pointerdown", handle, 100, 0);
        point("pointerup", handle, 100, 0);

        expect(after.style.flex).toBe("");
        expect(container.style.width).toBe("");
      });

      it('grows the next sibling when the right handle moves back', () => {
        const {container, after, handle} = withSiblings("right");

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 40, 0);

        expect(container.style.width).toBe("240px");
        expect(after.style.flex).toBe("0 0 460px");
      });

      it('uses the previous sibling for a left handle', () => {
        const {container, before, after, handle} = withSiblings("left");

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 60, 0);

        expect(container.style.width).toBe("340px");
        expect(before.style.flex).toBe("0 0 210px");
        expect(after.style.flex).toBe("");
      });

      it('uses the next sibling for a bottom handle and the previous one for a top handle', () => {
        const bottom = withSiblings("bottom", {flexDirection: "column"});
        point("pointerdown", bottom.handle, 0, 50);
        point("pointermove", bottom.handle, 0, 80);
        expect(bottom.container.style.height).toBe("230px");
        expect(bottom.after.style.flex).toBe("0 0 270px");
        expect(bottom.before.style.flex).toBe("");
        cleanup();

        const top = withSiblings("top", {flexDirection: "column", prev: {width: 250, height: 250}});
        point("pointerdown", top.handle, 0, 50);
        point("pointermove", top.handle, 0, 20);
        expect(top.container.style.height).toBe("230px");
        expect(top.before.style.flex).toBe("0 0 220px");
        expect(top.after.style.flex).toBe("");
      });

      it('stops the container when the neighbour reaches its minimum size', () => {
        const {container, after, handle} = withSiblings("right");

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 900, 0);

        // The neighbour keeps MIN_SIZE and the container takes the rest of what the neighbour had
        expect(after.style.flex).toBe("0 0 160px");
        expect(container.style.width).toBe("540px");
      });

      it('respects the min-width of the neighbour when it is higher than the minimum size', () => {
        const {container, after, handle} = withSiblings("right", {nextMinWidth: "300px"});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 900, 0);

        expect(after.style.flex).toBe("0 0 300px");
        expect(container.style.width).toBe("400px");
      });

      it('gives back what the neighbour cannot shrink below its content, so its outer edge does not move', () => {
        const {container, after, handle} = withSiblings("right", {nextContentMin: 380});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        // The neighbour asked for 350px stays at the 380px of its content: the container only takes the 20px it gave
        expect(after.style.flex).toBe("0 0 380px");
        expect(container.style.width).toBe("320px");
        expect(container.style.flexBasis).toBe("320px");
      });

      it('takes back from the neighbour what the container cannot shrink below its content', () => {
        const {container, after, handle} = withSiblings("right", {ownContentMin: 280});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 50, 0);

        // The container asked for 250px stays at the 280px of its content: the neighbour only grows the 20px it got
        expect(container.style.width).toBe("280px");
        expect(after.style.flex).toBe("0 0 420px");
      });

      it('does not force a neighbour that is already smaller than the minimum size', () => {
        const {container, after, handle} = withSiblings("right", {next: {width: 100, height: 100}});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        expect(after.style.flex).toBe("0 0 100px");
        expect(container.style.width).toBe("300px");
      });

      it('keeps the previous behaviour when the parent lays out across the axis of the handle', () => {
        const {container, before, after, handle} = withSiblings("right", {flexDirection: "column"});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        expect(container.style.width).toBe("350px");
        expect(container.style.flexBasis).toBe("");
        expect(after.style.flex).toBe("");
        expect(before.style.flex).toBe("");
      });

      it('keeps the previous behaviour when the parent is not a flex container', () => {
        // A block parent still computes flex-direction: row, so the display decides
        const {container, after, handle} = withSiblings("right", {display: "block"});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        expect(container.style.width).toBe("350px");
        expect(after.style.flex).toBe("");
      });

      it('keeps the previous behaviour when the neighbour is not rendered', () => {
        const {container, after, handle} = withSiblings("right", {next: {width: 0, height: 0}});

        point("pointerdown", handle, 100, 0);
        point("pointermove", handle, 150, 0);

        expect(container.style.width).toBe("350px");
        expect(after.style.flex).toBe("");
      });
    });

    it('keeps the previous behaviour when there is no sibling on the handle side', () => {
      const {container, handle} = setup("right");

      point("pointerdown", handle, 100, 0);
      point("pointermove", handle, 150, 0);

      expect(container.style.width).toBe("350px");
      expect(container.style.flexBasis).toBe("350px");
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
