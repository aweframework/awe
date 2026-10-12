import React from "react";
import {MemoryRouter} from "react-router";
import {renderWithProviders} from "../test-utils";
import ActionsContainer from "../../../src/containers/ActionsContainer";
import {ActionStatus} from "../../../src/redux/actions/actions";

const {STATUS_RUNNING, STATUS_INITIAL} = ActionStatus;

describe("awe-react-client/test/js/containers/ActionsContainerUnknownActionTest.jsx", () => {
  beforeEach(() => {
    jest.spyOn(console, "warn").mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("logs an unknown action type and goes on with the next actions of the queue", () => {
    const print = jest.spyOn(window, "print").mockImplementation(() => undefined);
    const state = {
      settings: {actionsStack: 0},
      components: {},
      actions: {
        running: true,
        sync: [[
          {id: "unknown", type: "not-a-real-action", parameters: {}, status: STATUS_RUNNING},
          {id: "next", type: "print", parameters: {}, status: STATUS_INITIAL}
        ]],
        async: []
      }
    };

    const {store} = renderWithProviders(<MemoryRouter><ActionsContainer/></MemoryRouter>, {preloadedState: state});

    expect(console.warn).toHaveBeenCalledWith(expect.stringContaining("not-a-real-action"), expect.objectContaining({id: "unknown"}));
    expect(print).toHaveBeenCalledTimes(1);
    expect(store.getState().actions.sync[0].map(a => a.id)).not.toContain("unknown");
  });
});
