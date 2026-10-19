import React from 'react';
import {act, fireEvent, render} from '@testing-library/react';
import useMaximize, {MAXIMIZE_ANIMATION_TIME} from '../../../src/hooks/useMaximize';

function Harness({onResize}) {
  const {ref, maximized, iconMaximized, resizing, style, className, toggle} = useMaximize({onResize});
  return <div className="maximize-target" id="target">
    <div id="sibling">sibling</div>
    <div id="wrapper">
      <div id="wrapper-sibling">wrapper sibling</div>
      <div id="box" ref={ref} style={style} data-state={`${maximized}-${iconMaximized}-${resizing}`}
        className={className}>
        <button id="toggle" onClick={toggle}>toggle</button>
      </div>
    </div>
  </div>;
}

const state = () => document.getElementById("box").getAttribute("data-state");
const run = (time) => act(() => {
  jest.advanceTimersByTime(time);
});
const sizeOf = (node) => [node.style.top, node.style.left, node.style.width, node.style.height];

describe('awe-react-client/test/js/hooks/useMaximizeTest.jsx', () => {
  let rects;

  const mountHarness = (onResize = jest.fn()) => {
    const view = render(<Harness onResize={onResize}/>);
    jest.spyOn(document.getElementById("target"), "getBoundingClientRect").mockImplementation(() => rects.target);
    jest.spyOn(document.getElementById("box"), "getBoundingClientRect").mockImplementation(() => rects.box);
    return view;
  };

  beforeEach(() => {
    jest.useFakeTimers();
    rects = {target: {top: 40, left: 10, width: 800, height: 500}, box: {top: 100, left: 300, width: 200, height: 200}};
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('grows a clone over the maximize target and then fills it, like the AngularJS maximize', () => {
    mountHarness();
    fireEvent.click(document.getElementById("toggle"));

    // The animation clone starts over the element, which is hidden while the clone grows
    const clone = document.querySelector(".maximize-clone");
    expect(clone).not.toBeNull();
    expect(sizeOf(clone)).toEqual(["100px", "300px", "200px", "200px"]);
    expect(state()).toBe("false-false-true");
    expect(document.getElementById("box").classList).toContain("maximize-resizing");

    run(200);
    expect(sizeOf(clone)).toEqual(["40px", "10px", "800px", "500px"]);
    expect(clone.style.transitionDuration).toBe(`${MAXIMIZE_ANIMATION_TIME}ms`);
    expect(document.getElementById("sibling").style.opacity).toBe("0");

    run(MAXIMIZE_ANIMATION_TIME);
    expect(document.querySelector(".maximize-clone")).toBeNull();
    expect(state()).toBe("true-true-false");
    const box = document.getElementById("box");
    expect(box.style.position).toBe("fixed");
    expect(box.classList).toContain("maximized");
    expect(sizeOf(box)).toEqual(["40px", "10px", "800px", "500px"]);
  });

  it('minimizes the siblings and the siblings of the parents, and undoes it on restore', () => {
    mountHarness();
    fireEvent.click(document.getElementById("toggle"));
    run(200 + MAXIMIZE_ANIMATION_TIME);

    expect(document.getElementById("sibling").classList).toContain("minimized");
    expect(document.getElementById("wrapper-sibling").classList).toContain("minimized");
    expect(document.getElementById("wrapper").classList).toContain("maximizeParent");
    expect(document.getElementById("box").classList).not.toContain("minimized");

    fireEvent.click(document.getElementById("toggle"));
    expect(document.getElementById("sibling").classList).not.toContain("minimized");
    expect(document.getElementById("wrapper").classList).not.toContain("maximizeParent");
    // Restoring: the grid is already in the flow, the icon changes when the animation ends
    expect(state()).toBe("false-true-true");

    run(150);
    const clone = document.querySelector(".maximize-clone");
    expect(sizeOf(clone)).toEqual(["100px", "300px", "200px", "200px"]);
    run(MAXIMIZE_ANIMATION_TIME);
    expect(document.querySelector(".maximize-clone")).toBeNull();
    expect(state()).toBe("false-false-false");
    expect(document.getElementById("box").style.position).toBe("");
    expect(document.getElementById("sibling").style.opacity).toBe("");
  });

  it('asks for a resize when the animation ends and follows the target when the window changes', () => {
    const onResize = jest.fn();
    mountHarness(onResize);

    fireEvent.click(document.getElementById("toggle"));
    run(200 + MAXIMIZE_ANIMATION_TIME);
    expect(onResize).toHaveBeenCalledTimes(1);

    rects.target = {top: 40, left: 10, width: 600, height: 400};
    act(() => {
      window.dispatchEvent(new Event("resize"));
    });
    expect(document.getElementById("box").style.width).toBe("600px");

    fireEvent.click(document.getElementById("toggle"));
    run(150 + MAXIMIZE_ANIMATION_TIME);
    expect(onResize).toHaveBeenCalledTimes(2);
  });

  it('does not keep the ids of the timers that already fired', () => {
    const clear = jest.spyOn(window, "clearTimeout");
    const view = mountHarness();
    fireEvent.click(document.getElementById("toggle"));
    run(200 + MAXIMIZE_ANIMATION_TIME);
    fireEvent.click(document.getElementById("toggle"));
    run(150 + MAXIMIZE_ANIMATION_TIME + 150);
    clear.mockClear();

    view.unmount();

    expect(clear).not.toHaveBeenCalled();
    clear.mockRestore();
  });

  it('ignores the toggle while it is animating', () => {
    mountHarness();
    fireEvent.click(document.getElementById("toggle"));
    fireEvent.click(document.getElementById("toggle"));

    expect(document.querySelectorAll(".maximize-clone").length).toBe(1);
  });

  it('fills the viewport when there is no maximize target', () => {
    const view = render(<Harness/>);
    document.getElementById("target").classList.remove("maximize-target");
    fireEvent.click(document.getElementById("toggle"));
    run(200 + MAXIMIZE_ANIMATION_TIME);

    const box = document.getElementById("box");
    expect([box.style.position, box.style.top, box.style.left]).toEqual(["fixed", "0px", "0px"]);
    view.unmount();
  });

  it('removes the clone and the classes when it unmounts while animating', () => {
    const view = mountHarness();
    fireEvent.click(document.getElementById("toggle"));
    run(200);
    const sibling = document.getElementById("sibling");

    view.unmount();

    expect(document.querySelector(".maximize-clone")).toBeNull();
    expect(sibling.classList).not.toContain("minimized");
  });
});
