import React from "react";
import {MemoryRouter} from "react-router";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import ActionsContainer from "../../../src/containers/ActionsContainer";
import {ActionStatus, addActionsTop} from "../../../src/redux/actions/actions";

const {STATUS_RUNNING, STATUS_INITIAL} = ActionStatus;
const ADDRESS = {view: "base", component: "dialog"};

/**
 * Closing a dialog through the real action pipeline: the dialog is open on top of the stack of the action that opened it,
 * which still has a follow-up action pending
 */
describe("awe-react-client/test/js/containers/DialogCloseCancelPipelineTest.jsx", () => {
  let warnSpy;
  // The follow-up has no registered handler: an unknown action is skipped (never stalls), so it only gets to run when it was not dropped
  const followUpRan = () => warnSpy.mock.calls.some(([message]) => String(message).includes("pending-action"));
  const openAction = {id: "open", type: "dialog", target: "dialog", address: ADDRESS, status: STATUS_RUNNING};
  const followUp = {id: "follow-up", type: "pending-action", status: STATUS_INITIAL};

  const closeDialog = (type) => {
    const {store} = renderWithProviders(<MemoryRouter><ActionsContainer/></MemoryRouter>, {
      preloadedState: {
        settings: {actionsStack: 0},
        components: {dialog: {address: ADDRESS, attributes: {isShowing: true, action: openAction}}},
        actions: {running: true, sync: [[openAction, followUp], []], async: []}
      }
    });
    act(() => {
      store.dispatch(addActionsTop([{type, target: "dialog", address: ADDRESS}]));
    });
    return store.getState();
  };

  beforeEach(() => {
    jest.spyOn(console, "debug").mockImplementation(() => undefined);
    warnSpy = jest.spyOn(console, "warn").mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("rejects the action that opened the dialog with close-cancel, dropping its pending actions", () => {
    const state = closeDialog("close-cancel");

    expect(state.components.dialog.attributes.isShowing).toBe(false);
    expect(state.actions.sync.flat().map(a => a.id)).toEqual([]);
    expect(followUpRan()).toBe(false);
  });

  it("accepts the action that opened the dialog with close, so its pending actions go on (the follow-up runs)", () => {
    const state = closeDialog("close");

    expect(state.components.dialog.attributes.isShowing).toBe(false);
    expect(state.actions.sync.flat().map(a => a.id)).toEqual([]);
    expect(followUpRan()).toBe(true);
  });
});
