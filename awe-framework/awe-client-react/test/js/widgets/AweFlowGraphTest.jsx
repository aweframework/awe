import React from "react";
import { act, fireEvent, screen } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import { getFormValues } from "../../../src/redux/selectors/form";
import AweFlowGraph from "../../../src/widgets/AweFlowGraph";
import i18n from "../i18nForTests";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";
import { updateModelWithDependencies } from "../../../src/redux/thunks/components";
import { updateAttributes } from "../../../src/redux/actions/components";

// A node of the run mode launches the actions of the widget: the test sees the model update with its event
jest.mock("../../../src/redux/thunks/components", () => {
  const actual = jest.requireActual("../../../src/redux/thunks/components");
  return { ...actual, updateModelWithDependencies: jest.fn(actual.updateModelWithDependencies) };
});

// The screen is loaded again through its thunk: the test sees what it is asked to load
jest.mock("../../../src/redux/thunks/screen", () => ({
  loadScreen: (view, option, t, fresh, parameters) => ({ type: "TEST_LOAD_SCREEN", view, option, parameters })
}));

const row = (taskId, parentId, extra = {}) => ({ id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra });

const renderGraph = (values, parameters = { target: "update-scheduler-task" }, id = "IdeTsk", others = {}, attributes = {}) => renderWithProviders(
  <AweFlowGraph id={id} />, {
    spyDispatch: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      screen: { breadcrumbs: [], report: { name: "scheduler-runs", option: "scheduler-runs" } },
      components: {
        ...others,
        [id]: {
          address: { component: id, view: "report" },
          model: { values, selected: [] },
          attributes: { id, type: "flow-graph", parameters, ...attributes }
        }
      }
    }
  });

// The one-level graph of the task 2 (the task screen): its parent 1 and its child 3, which also waits for the task 4
const taskRows = (extra = {}) => [
  row(1, null, { lastStatus: 0, onWarning: 1, lastWhen: "hoy 08:00", nextRun: "Next: tomorrow 03:00", ...extra[1] }),
  row(2, 1, { rootTask: 2, lastStatus: 2, ...extra[2] }),
  row(3, 2, extra[3]), row(3, 4), row(4, null, { name: "Bank answer" })
];
// The task screen: the task of the screen is its hidden criterion IdeTsk.selected, the one the node opens
const TASK_SCREEN = { mode: "task", target: "scheduler-task", criterion: "IdeTsk.selected" };
const taskCriterion = {
  "IdeTsk.selected": {
    address: { component: "IdeTsk.selected", view: "report" },
    model: { values: [], selected: ["2"] },
    attributes: { id: "IdeTsk.selected", component: "hidden" }
  }
};

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

  it("draws the one-level graph of a task in three columns, without buttons and with the condition of every edge", () => {
    renderGraph([
      row(1, null, { lastStatus: 0, onWarning: 1 }),
      row(2, 1, { rootTask: 2, lastStatus: 2 }),
      row(3, 2), row(4, 3)
    ], { mode: "task" });

    expect(screen.queryAllByRole("button")).toHaveLength(0);
    expect(screen.getAllByRole("listitem").map(node => node.getAttribute("aria-label"))).toEqual([
      "SCREEN_TEXT_FLOW_GRAPH_PARENTS · Task 1 · ENUM_STATUS_OK",
      "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2 · ENUM_STATUS_WARNING",
      "SCREEN_TEXT_FLOW_GRAPH_CHILDREN · Task 3"]);
    expect(screen.getAllByRole("img", { name: "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING" })).toHaveLength(1);
    expect(screen.getAllByRole("img", { name: "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK" })).toHaveLength(1);
    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_THIS_TASK", { selector: ".flow-graph-column" })).toBeDefined();
  });

  it("draws only one side of the task in two columns when the side parameter asks for it", () => {
    const rows = [row(1, null, { onWarning: 1 }), row(2, 1, { rootTask: 2 }), row(3, 2)];
    const { container, unmount } = renderGraph(rows, { mode: "task", side: "parents" });
    const titles = () => Array.from(container.querySelectorAll(".flow-graph-column")).map(column => column.textContent);

    expect(screen.getAllByRole("listitem").map(node => node.getAttribute("aria-label"))).toEqual([
      "SCREEN_TEXT_FLOW_GRAPH_PARENTS · Task 1", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2"]);
    expect(titles()).toEqual(["SCREEN_TEXT_FLOW_GRAPH_PARENTS", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK"]);
    unmount();

    const children = renderGraph(rows, { mode: "task", side: "children" });
    expect(screen.getAllByRole("listitem").map(node => node.getAttribute("aria-label"))).toEqual([
      "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN · Task 3"]);
    expect(Array.from(children.container.querySelectorAll(".flow-graph-column")).map(column => column.textContent))
      .toEqual(["SCREEN_TEXT_FLOW_GRAPH_THIS_TASK", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN"]);
  });

  it("opens the task screen of a parent or a child of the task, and the task itself is not a link", () => {
    i18n.addResource("en", "translation", "SCREEN_TEXT_FLOW_GRAPH_OPEN_TASK", "Open {0}");
    const { store, dispatchSpy } = renderGraph(taskRows(), TASK_SCREEN, "TskDpnGph", taskCriterion);

    expect(screen.getAllByRole("button").map(button => button.getAttribute("aria-label"))).toEqual([
      "Open Task 1", "Open Task 3"]);
    expect(screen.getByRole("group", { name: /SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2/ })).toBeDefined();

    // From another screen: the criterion gets the task and the target screen opens
    fireEvent.click(screen.getByRole("button", { name: "Open Task 3" }));

    expect(getFormValues(store.getState())["IdeTsk.selected"]).toBe("3");
    expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({
      actions: [expect.objectContaining({ type: "screen", target: "scheduler-task" })]
    }));
  });

  it("loads the task screen again with the task of the node when the graph is in that screen", () => {
    ViewRegistry.setView("report", { option: "scheduler-task" });
    const { dispatchSpy } = renderGraph(taskRows(), TASK_SCREEN, "TskDpnGph", taskCriterion);

    fireEvent.click(screen.getAllByRole("button")[0]);

    expect(dispatchSpy).toHaveBeenCalledWith({ type: "TEST_LOAD_SCREEN", view: "report", option: "scheduler-task",
      parameters: { "IdeTsk.selected": "1" } });
    ViewRegistry.clearView("report");
  });

  it("marks an inactive task with a tag, also when it has a state", () => {
    renderGraph(taskRows({ 1: { active: 0 } }), { mode: "task" });

    expect(screen.getByRole("listitem", { name: /Task 1/ }).textContent).toContain("SCREEN_TEXT_FLOW_GRAPH_INACTIVE");
    expect(screen.getByRole("listitem", { name: /Task 1/ }).textContent).toContain("ENUM_STATUS_OK");
  });

  it("tells when each task last ran, the next execution of a scheduled one and the other tasks a child waits for", () => {
    renderGraph(taskRows(), { mode: "task" });

    const parent = screen.getByRole("listitem", { name: /Task 1/ });
    expect(parent.textContent).toContain("ENUM_STATUS_OK · hoy 08:00");
    expect(parent.textContent).toContain("Next: tomorrow 03:00");
    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS")).toBeDefined();
    expect(screen.getByText("Bank answer")).toBeDefined();
    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_LEGEND")).toBeDefined();
  });

  it("tells when the task has no dependencies", () => {
    renderGraph([row(1, null, { rootTask: 1 })], { mode: "task" });

    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_NO_DEPENDENCIES")).toBeDefined();
  });

  it("tells when there is no flow to draw", () => {
    renderGraph([row(1, null)]);

    expect(screen.queryAllByRole("button")).toHaveLength(0);
    expect(screen.getByText("SCREEN_TEXT_FLOW_GRAPH_EMPTY")).toBeDefined();
  });
});

// The rows of getFlowRunGraph (the flow run page): 1 → 2, 1 → 3, 2 → 4 and 3 → 4 (a join closed by 2), 4 → 5 not reached
const at = time => `17/11/2026 ${time}`;
const runRows = () => [
  row(1, null, { rootRun: "run", executionStatus: 0, executionId: 1092, IdeTsk: 1, ExeTsk: 1092, ExeTim: "1 min 12 s",
    startDate: at("08:05:00"), endDate: at("08:06:12"), onWarning: 1 }),
  row(2, 1, { executionStatus: 2, executionId: 640, IdeTsk: 2, ExeTsk: 640, startDate: at("08:06:12"),
    endDate: at("08:09:52"), edgeState: "MET", conditionLabel: "OK o Aviso" }),
  row(3, 1, { executionStatus: 4, executionId: 875, IdeTsk: 3, ExeTsk: 875, deferred: 1, startDate: at("08:06:12"),
    ExeDueLbl: "Vence 12:15", edgeState: "MET", conditionLabel: "OK o Aviso" }),
  ...[2, 3].map(parent => row(4, parent, { runStatus: "WAITING", IdeTsk: 4, joinedBy: null,
    edgeState: parent === 2 ? "MET" : "PENDING", conditionLabel: "OK" })),
  row(5, 4, { IdeTsk: 5, edgeState: "PENDING" })
];
const RUN = { mode: "run" };
const runNode = name => screen.getByRole("button", { name: new RegExp(`^${name} ·`) });

describe("awe-client-react/test/js/widgets/AweFlowGraphTest.jsx run mode", () => {
  beforeEach(() => {
    Element.prototype.scrollIntoView = jest.fn();
    updateModelWithDependencies.mockClear();
  });

  it("draws every task of the run with its state, execution, times and due date, and every edge with its condition", () => {
    renderGraph(runRows(), RUN, "FlwRunGph");

    expect(runNode("Task 1").getAttribute("aria-label")).toBe(
      "Task 1 · ENUM_STATUS_OK · SCREEN_TEXT_EXECUTION 1092 · 1 min 12 s · 08:05 – 08:06");
    expect(runNode("Task 3").textContent).toContain("Vence 12:15");
    expect(runNode("Task 3").textContent).toContain("SCREEN_TEXT_ACTIVITY_WAITING");
    expect(runNode("Task 4").textContent).toContain("SCREEN_TEXT_FLOW_RUN_PENDING");
    expect(screen.getAllByText("OK o Aviso")).toHaveLength(2);
    expect(screen.getAllByText("OK")).toHaveLength(2);
    expect(screen.getByText("SCREEN_TEXT_FLOW_RUN_EDGE_OK")).toBeDefined();
  });

  it("selects the node of an execution, launches the actions of the widget and sends its row like a grid row", () => {
    const { store } = renderGraph(runRows(), RUN, "FlwRunGph");

    fireEvent.click(runNode("Task 2"));

    expect(updateModelWithDependencies).toHaveBeenCalledWith({ component: "FlwRunGph", view: "report" },
      { event: "click", selectedNode: "2" });
    expect(getFormValues(store.getState())).toMatchObject({ "FlwRunGph.selected": "2", "IdeTsk.selected": 2,
      "ExeTsk.selected": 640 });
    expect(runNode("Task 2").getAttribute("aria-pressed")).toBe("true");
    expect(runNode("Task 1").getAttribute("aria-pressed")).toBe("false");
    expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({ block: "nearest", inline: "nearest" });
    expect(screen.getByText(/^SCREEN_TEXT_FLOW_GRAPH_SELECTED/).getAttribute("aria-live")).toBe("polite");
  });

  it("keeps a task without execution focusable but it opens nothing", () => {
    renderGraph(runRows(), RUN, "FlwRunGph");
    const pending = runNode("Task 4");

    pending.focus();
    fireEvent.click(pending);

    expect(document.activeElement).toBe(pending);
    expect(pending.getAttribute("aria-disabled")).toBe("true");
    expect(updateModelWithDependencies).not.toHaveBeenCalled();
  });

  it("clears the selection with Escape", () => {
    const { store } = renderGraph(runRows(), RUN, "FlwRunGph");
    fireEvent.click(runNode("Task 2"));

    fireEvent.keyDown(runNode("Task 2"), { key: "Escape" });

    expect(getFormValues(store.getState())).toMatchObject({ "FlwRunGph.selected": null });
    expect(runNode("Task 2").getAttribute("aria-pressed")).toBe("false");
  });

  it("zooms in and out a step and fits the graph to the width it has", () => {
    const { container } = renderGraph(runRows(), RUN, "FlwRunGph");
    const zoom = () => container.querySelector(".flow-graph-zoom").textContent;

    fireEvent.click(screen.getByRole("button", { name: "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN" }));
    expect(zoom()).toBe("125 %");
    fireEvent.click(screen.getByRole("button", { name: "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT" }));
    fireEvent.click(screen.getByRole("button", { name: "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT" }));
    expect(zoom()).toBe("75 %");

    Object.defineProperty(container.querySelector(".flow-graph-viewport"), "clientWidth", { value: 0 });
    fireEvent.click(screen.getByRole("button", { name: "SCREEN_TEXT_FLOW_GRAPH_FIT" }));
    expect(zoom()).toBe("50 %");
  });

  it("starts at the zoom that fits a graph wider than its viewport (50 % at least), once the viewport is measured", () => {
    // A viewport of 300 x 900 px while it is shown, nothing while it is hidden (as a browser measures it)
    const VIEWPORT = { clientWidth: 300, clientHeight: 900 };
    const spies = Object.keys(VIEWPORT).map(name => jest.spyOn(HTMLElement.prototype, name, "get")
      .mockImplementation(function () {
        return this.classList.contains("flow-graph-viewport") && !this.closest("[hidden]") ? VIEWPORT[name] : 0;
      }));
    try {
      // Hidden by its dependency it cannot be measured: it fits when it is shown
      const { container, store } = renderGraph(runRows(), RUN, "FlwRunGph", {}, { visible: false });
      const zoom = () => container.querySelector(".flow-graph-zoom").textContent;
      expect(zoom()).toBe("100 %");

      act(() => store.dispatch(updateAttributes({ component: "FlwRunGph", view: "report" }, { visible: true })));
      expect(zoom()).toBe("50 %");
      // The zoom buttons still change it
      fireEvent.click(screen.getByRole("button", { name: "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN" }));
      expect(zoom()).toBe("75 %");
    } finally {
      spies.forEach(spy => spy.mockRestore());
    }
  });

  it("hides the view while a dependency hides it (one view at a time), keeping its tasks in the page", () => {
    const { container } = renderGraph(runRows(), { ...RUN, view: "timeline" }, "FlwRunGph", {}, { visible: false });

    expect(container.querySelector("#FlwRunGph").hidden).toBe(true);
    expect(screen.queryAllByRole("button")).toHaveLength(0);
    expect(screen.getAllByRole("button", { hidden: true }).length).toBeGreaterThan(0);
  });
});

describe("awe-client-react/test/js/widgets/AweFlowGraphTest.jsx run views", () => {
  let now;

  beforeEach(() => {
    Element.prototype.scrollIntoView = jest.fn();
    updateModelWithDependencies.mockClear();
    now = jest.spyOn(Date, "now").mockReturnValue(new Date(2026, 10, 17, 10, 42).getTime());
  });

  afterEach(() => {
    now.mockRestore();
  });

  it("draws the timeline: one row per task, its bars as toggle buttons that select the task, «Now» and the break", () => {
    const { store } = renderGraph(runRows(), { mode: "run", view: "timeline" }, "FlwRunGph");
    const timeline = screen.getByRole("table", { name: "SCREEN_TEXT_FLOW_RUN_TIMELINE" });
    const bar = name => screen.getByRole("button", { name: new RegExp(`^${name} ·`) });

    expect(screen.getAllByRole("rowheader").map(header => header.textContent)).toEqual([
      "Task 1ENUM_STATUS_OK1092", "Task 2ENUM_STATUS_WARNING640", "Task 4SCREEN_TEXT_FLOW_RUN_PENDING",
      "Task 5SCREEN_TEXT_FLOW_RUN_PENDING", "Task 3SCREEN_TEXT_ACTIVITY_WAITING875Vence 12:15"]);
    expect(bar("Task 1").getAttribute("aria-label")).toBe("Task 1 · ENUM_STATUS_OK · 08:05 – 08:06 · 1 min 12 s");
    expect(bar("Task 3").textContent).toBe("SCREEN_TEXT_ACTIVITY_WAITING · 2 h 35 min");
    expect(timeline.textContent).toContain("SCREEN_TEXT_FLOW_RUN_NOW 10:42");
    expect(timeline.textContent).toContain("≈");
    expect(screen.queryAllByRole("button", { name: /^Task 4/ })).toHaveLength(0);

    fireEvent.click(bar("Task 2"));

    expect(updateModelWithDependencies).toHaveBeenCalledWith({ component: "FlwRunGph", view: "report" },
      { event: "click", selectedNode: "2" });
    expect(getFormValues(store.getState())).toMatchObject({ "IdeTsk.selected": 2, "ExeTsk.selected": 640 });
    expect(bar("Task 2").getAttribute("aria-pressed")).toBe("true");
    expect(bar("Task 1").getAttribute("aria-pressed")).toBe("false");
  });

  it("draws the list: a nested ordered list, a join under the parent that closed it and «also feeds» under the other", () => {
    const rows = runRows().map(item => item.taskId === 4 ? { ...item, joinedBy: 2 } : item);
    renderGraph(rows, { mode: "run", view: "list" }, "FlwRunGph");
    const list = screen.getByRole("list", { name: "SCREEN_TEXT_FLOW_RUN_LIST" });
    const item = name => screen.getByRole("button", { name: new RegExp(`^${name} ·`) });
    const nested = element => element.closest("li").parentElement.closest("li")?.querySelector("button");

    expect(list.tagName).toBe("OL");
    expect(nested(item("Task 4"))).toBe(item("Task 2"));
    expect(nested(item("Task 5"))).toBe(item("Task 4"));
    expect(item("Task 2").textContent).toContain("SCREEN_TEXT_TASK_WHEN_AFTER");
    expect(item("Task 3").closest("li").textContent).toContain("SCREEN_TEXT_FLOW_RUN_ALSO_FEEDS");
    expect(item("Task 4").getAttribute("aria-disabled")).toBe("true");

    fireEvent.click(item("Task 4"));
    expect(updateModelWithDependencies).not.toHaveBeenCalled();
    fireEvent.click(item("Task 3"));

    expect(updateModelWithDependencies).toHaveBeenCalledWith({ component: "FlwRunGph", view: "report" },
      { event: "click", selectedNode: "3" });
    expect(item("Task 3").getAttribute("aria-pressed")).toBe("true");
  });
});
