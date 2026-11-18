import React from "react";
import { fireEvent, screen } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import { getFormValues } from "../../../src/redux/selectors/form";
import AweFlowGraph from "../../../src/widgets/AweFlowGraph";

const row = (taskId, parentId, extra = {}) => ({ id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra });

const renderGraph = (values, parameters = { target: "update-scheduler-task" }) => renderWithProviders(
  <AweFlowGraph id="IdeTsk" />, {
    spyDispatch: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      screen: { breadcrumbs: [], report: { name: "scheduler-runs", option: "scheduler-runs" } },
      components: {
        IdeTsk: {
          address: { component: "IdeTsk", view: "report" },
          model: { values, selected: [] },
          attributes: { id: "IdeTsk", type: "flow-graph", parameters }
        }
      }
    }
  });

describe("awe-client-react/test/js/widgets/AweFlowGraphTest.jsx", () => {
  it("draws every task of the run with its state", () => {
    renderGraph([
      row(1, null, { rootRun: "run", executionStatus: 0 }),
      row(2, 1, { runStatus: "LAUNCHED", executionStatus: 0 }),
      row(3, 1, { runStatus: "LAUNCHED", executionStatus: 1 }),
      row(4, 2, { runStatus: "SKIPPED" }), row(4, 3, { runStatus: "SKIPPED" })
    ]);

    expect(screen.getAllByRole("button")).toHaveLength(4);
    expect(screen.getByRole("button", { name: /Task 3.*ENUM_STATUS_ERROR/ })).toBeDefined();
    expect(screen.getByRole("button", { name: /Task 4.*SCREEN_TEXT_FLOW_GRAPH_SKIPPED/ })).toBeDefined();
  });

  it("opens the task of a node sending it as the selected task", () => {
    const { store, dispatchSpy } = renderGraph([row(1, null), row(2, 1)]);

    fireEvent.click(screen.getByRole("button", { name: /Task 2/ }));

    expect(getFormValues(store.getState())["IdeTsk.selected"]).toBe("2");
    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({
      actions: [expect.objectContaining({ type: "screen", target: "update-scheduler-task" })]
    }));
  });

  it("tells when there is no flow to draw", () => {
    renderGraph([row(1, null)]);

    expect(screen.queryAllByRole("button")).toHaveLength(0);
    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_EMPTY")).toBeDefined();
  });
});
