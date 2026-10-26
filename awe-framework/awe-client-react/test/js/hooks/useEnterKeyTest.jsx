import React from "react";
import {fireEvent, render, renderHook} from "@testing-library/react";
import useEnterKey from "../../../src/hooks/useEnterKey";

describe("hooks/useEnterKey", () => {
  function Probe({onEnter, when, onParentKeyDown}) {
    const onKeyDown = useEnterKey(onEnter, {when});
    return <div onKeyDown={onParentKeyDown}><input data-testid="input" onKeyDown={onKeyDown}/></div>;
  }

  it("runs the handler on Enter, prevents the default and does not bubble", () => {
    const onEnter = jest.fn();
    const onParentKeyDown = jest.fn();
    const onWindowKeyDown = jest.fn();
    window.addEventListener("keydown", onWindowKeyDown);
    const {getByTestId} = render(<Probe onEnter={onEnter} onParentKeyDown={onParentKeyDown}/>);

    const notPrevented = fireEvent.keyDown(getByTestId("input"), {key: "Enter"});
    window.removeEventListener("keydown", onWindowKeyDown);

    expect(notPrevented).toBe(false);
    expect(onEnter).toHaveBeenCalledTimes(1);
    expect(onParentKeyDown).not.toHaveBeenCalled();
    expect(onWindowKeyDown).not.toHaveBeenCalled();
  });

  it("lets other keys bubble with their default", () => {
    const onEnter = jest.fn();
    const onParentKeyDown = jest.fn();
    const {getByTestId} = render(<Probe onEnter={onEnter} onParentKeyDown={onParentKeyDown}/>);

    expect(fireEvent.keyDown(getByTestId("input"), {key: "a"})).toBe(true);
    expect(onEnter).not.toHaveBeenCalled();
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

  it("lets Enter bubble when the condition is false", () => {
    const onEnter = jest.fn();
    const onParentKeyDown = jest.fn();
    const {getByTestId} = render(<Probe onEnter={onEnter} when={() => false} onParentKeyDown={onParentKeyDown}/>);

    expect(fireEvent.keyDown(getByTestId("input"), {key: "Enter"})).toBe(true);
    expect(onEnter).not.toHaveBeenCalled();
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

  it("keeps the same handler and runs the latest callbacks", () => {
    const first = jest.fn();
    const second = jest.fn();
    const {result, rerender} = renderHook(({onEnter}) => useEnterKey(onEnter), {initialProps: {onEnter: first}});
    const handler = result.current;

    rerender({onEnter: second});
    result.current({key: "Enter", code: "Enter", preventDefault: jest.fn(), stopPropagation: jest.fn()});

    expect(result.current).toBe(handler);
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledTimes(1);
  });
});
