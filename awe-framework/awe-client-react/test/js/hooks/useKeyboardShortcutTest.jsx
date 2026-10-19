import React from "react";
import {fireEvent, render, renderHook} from "@testing-library/react";
import useKeyboardShortcut from "../../../src/hooks/useKeyboardShortcut";

const binding = (handler, key = "Enter") => [{shortcut: {key}, handler}];

describe("hooks/useKeyboardShortcut", () => {
  afterEach(() => {
    window.onkeydown = null;
    jest.restoreAllMocks();
  });

  it("listens to the window by default", () => {
    const handler = jest.fn();
    renderHook(() => useKeyboardShortcut(binding(handler)));
    fireEvent.keyDown(window, {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
    fireEvent.keyDown(window, {key: "a"});
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("listens to the element of a ref", () => {
    const handler = jest.fn();
    function Probe() {
      const ref = React.useRef(null);
      useKeyboardShortcut(binding(handler), {target: ref});
      return <div ref={ref} data-testid="box"><input data-testid="inner"/></div>;
    }
    const {getByTestId} = render(<Probe/>);
    fireEvent.keyDown(getByTestId("inner"), {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
    fireEvent.keyDown(window, {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("listens to an event target", () => {
    const handler = jest.fn();
    const target = document.createElement("div");
    renderHook(() => useKeyboardShortcut(binding(handler), {target}));
    fireEvent.keyDown(target, {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("does nothing when the target is not available", () => {
    const handler = jest.fn();
    const add = jest.spyOn(window, "addEventListener");
    expect(() => renderHook(() => useKeyboardShortcut(binding(handler), {target: {current: null}}))).not.toThrow();
    expect(add).not.toHaveBeenCalledWith("keydown", expect.any(Function), expect.anything());
  });

  it("stops listening after unmount", () => {
    const handler = jest.fn();
    const {unmount} = renderHook(() => useKeyboardShortcut(binding(handler)));
    unmount();
    fireEvent.keyDown(window, {key: "Enter"});
    expect(handler).not.toHaveBeenCalled();
  });

  it("keeps an existing window.onkeydown and other listeners", () => {
    const onkeydown = jest.fn();
    const listener = jest.fn();
    const handler = jest.fn();
    window.onkeydown = onkeydown;
    window.addEventListener("keydown", listener);
    renderHook(() => useKeyboardShortcut(binding(handler)));
    fireEvent.keyDown(window, {key: "Enter"});
    expect(window.onkeydown).toBe(onkeydown);
    expect(onkeydown).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledTimes(1);
    window.removeEventListener("keydown", listener);
  });

  it("does nothing when it is disabled and listens again when enabled", () => {
    const handler = jest.fn();
    const {rerender} = renderHook(({enabled}) => useKeyboardShortcut(binding(handler), {enabled}), {initialProps: {enabled: false}});
    fireEvent.keyDown(window, {key: "Enter"});
    expect(handler).not.toHaveBeenCalled();
    rerender({enabled: true});
    fireEvent.keyDown(window, {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("uses the latest handler without subscribing again", () => {
    const first = jest.fn();
    const second = jest.fn();
    const add = jest.spyOn(window, "addEventListener");
    const remove = jest.spyOn(window, "removeEventListener");
    const {rerender} = renderHook(({handler}) => useKeyboardShortcut(binding(handler)), {initialProps: {handler: first}});
    const subscriptions = () => add.mock.calls.filter(([name]) => name === "keydown").length;
    const count = subscriptions();
    rerender({handler: second});
    expect(subscriptions()).toBe(count);
    expect(remove.mock.calls.filter(([name]) => name === "keydown")).toHaveLength(0);
    fireEvent.keyDown(window, {key: "Enter"});
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledTimes(1);
  });

  it("supports another event name and capture", () => {
    const handler = jest.fn();
    const add = jest.spyOn(window, "addEventListener");
    renderHook(() => useKeyboardShortcut(binding(handler), {eventName: "keyup", capture: true}));
    expect(add).toHaveBeenCalledWith("keyup", expect.any(Function), true);
    fireEvent.keyUp(window, {key: "Enter"});
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
