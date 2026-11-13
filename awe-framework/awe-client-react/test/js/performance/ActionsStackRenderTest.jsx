import React, {Profiler} from "react";
import {act} from "@testing-library/react";
import {MemoryRouter} from "react-router";
import {renderWithProviders} from "../test-utils";
import ActionsContainer from "../../../src/containers/ActionsContainer";
import {ActionStatus, addStack} from "../../../src/redux/actions/actions";
import * as utilities from "../../../src/utilities";

const {STATUS_INITIAL} = ActionStatus;

/**
 * The actions stack is a debug view (Alt+Shift+digit). While it is hidden, the queue of actions changes on every client
 * action of a response: neither the stack nor the container that holds it render for those changes (#962)
 */
describe("awe-react-client/test/js/performance/ActionsStackRenderTest.jsx", () => {
  const state = (actionsStack) => ({
    settings: {actionsStack, helpTimeout: 0},
    actions: {
      running: false,
      sync: [[{id: "action-1", type: "noop", status: STATUS_INITIAL}]],
      async: [{id: "action-2", type: "notify", async: true, status: STATUS_INITIAL}]
    }
  });

  const renderContainer = (actionsStack) => {
    const renders = {count: 0};
    const result = renderWithProviders(
      <MemoryRouter>
        <Profiler id="actions" onRender={() => renders.count++}>
          <ActionsContainer/>
        </Profiler>
      </MemoryRouter>, {preloadedState: state(actionsStack)});
    return {...result, renders};
  };

  beforeEach(() => {
    // The fake actions of the fixture are not client actions: the container must not run them
    jest.spyOn(utilities, "checkActions").mockImplementation(() => undefined);
  });

  afterEach(() => jest.restoreAllMocks());

  it("does not render the actions of the stack while it is hidden", () => {
    const {container} = renderContainer(0);

    expect(container.querySelector(".actions-zone").style.display).toBe("none");
    expect(container.querySelectorAll(".action")).toHaveLength(0);
  });

  it("renders the actions of the stack while it is shown", () => {
    const {container} = renderContainer(1000);

    expect(container.querySelector(".actions-zone").style.display).toBe("block");
    expect(container.querySelectorAll(".action")).toHaveLength(2);
  });

  it("does not render again when the queue changes while the stack is hidden", () => {
    const {store, renders} = renderContainer(0);
    renders.count = 0;

    act(() => {
      store.dispatch(addStack());
    });

    expect(store.getState().actions.sync).toHaveLength(2);
    expect(renders.count).toBe(0);
  });

  it("renders the queue changes while the stack is shown", () => {
    const {store, renders, container} = renderContainer(1000);
    renders.count = 0;

    act(() => {
      store.dispatch(addStack());
    });

    expect(renders.count).toBeGreaterThan(0);
    expect(container.querySelectorAll(".action-stack").length).toBeGreaterThan(0);
  });
});
