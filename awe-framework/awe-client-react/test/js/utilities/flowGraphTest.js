import {
  buildFlowGraph, buildTaskGraph, getConditionIcons, getConditionLabel, getTaskGraphColumns, layoutFlowGraph
} from "../../../src/utilities/flowGraph";

const SIZE = { nodeWidth: 100, nodeHeight: 40, gapX: 50, gapY: 20 };
const byId = (layout) => Object.fromEntries(layout.nodes.map(node => [node.id, node]));

describe("awe-client-react/test/js/utilities/flowGraphTest.js", () => {
  describe("layoutFlowGraph", () => {
    it("draws the diamond A → B, A → C, B → D, C → D with D joined once", () => {
      const layout = layoutFlowGraph(["A", "B", "C", "D"].map(id => ({ id })), [
        { from: "A", to: "B" }, { from: "A", to: "C" }, { from: "B", to: "D" }, { from: "C", to: "D" }
      ], SIZE);
      const nodes = byId(layout);

      expect(layout.nodes).toHaveLength(4);
      expect([nodes.A.layer, nodes.B.layer, nodes.C.layer, nodes.D.layer]).toEqual([0, 1, 1, 2]);
      expect(nodes.B.y).not.toBe(nodes.C.y);
      expect(layout.edges).toHaveLength(4);
      expect(layout.edges.filter(edge => edge.to === "D")).toHaveLength(2);
    });

    it("puts a node after its deepest parent", () => {
      const layout = layoutFlowGraph(["A", "B", "C"].map(id => ({ id })), [
        { from: "A", to: "B" }, { from: "B", to: "C" }, { from: "A", to: "C" }
      ], SIZE);

      expect(byId(layout).C.layer).toBe(2);
    });

    it("joins every edge from the right side of the parent to the left side of the child", () => {
      const layout = layoutFlowGraph([{ id: "A" }, { id: "B" }], [{ from: "A", to: "B" }], SIZE);
      const { A, B } = byId(layout);
      const [edge] = layout.edges;

      expect(edge.start).toEqual({ x: A.x + SIZE.nodeWidth, y: A.y + SIZE.nodeHeight / 2 });
      expect(edge.end).toEqual({ x: B.x, y: B.y + SIZE.nodeHeight / 2 });
      expect(layout.width).toBe(2 * SIZE.nodeWidth + 3 * SIZE.gapX);
    });

    it("orders the children by their parents so the edges do not cross", () => {
      // Without ordering, A2's child would be drawn before A1's child and the edges would cross
      const layout = layoutFlowGraph(["A1", "A2", "B2", "B1"].map(id => ({ id })), [
        { from: "A1", to: "B1" }, { from: "A2", to: "B2" }
      ], SIZE);
      const nodes = byId(layout);

      expect(nodes.A1.y).toBeLessThan(nodes.A2.y);
      expect(nodes.B1.y).toBeLessThan(nodes.B2.y);
    });

    it("ignores the edges to unknown tasks and keeps the nodes of a cycle", () => {
      const layout = layoutFlowGraph([{ id: "A" }, { id: "B" }], [
        { from: "A", to: "B" }, { from: "B", to: "A" }, { from: "A", to: "Z" }
      ], SIZE);

      expect(layout.nodes.map(node => node.id).sort()).toEqual(["A", "B"]);
      expect(layout.edges.every(edge => edge.to !== "Z")).toBe(true);
    });

    it("keeps the layer every node brings, an empty layer keeping its column", () => {
      const layout = layoutFlowGraph([{ id: "A", layer: 1 }, { id: "B", layer: 2 }], [{ from: "A", to: "B" }],
        { ...SIZE, top: 30 });
      const { A, B } = byId(layout);

      expect([A.layer, B.layer]).toEqual([1, 2]);
      expect(A.x).toBe(SIZE.gapX + SIZE.nodeWidth + SIZE.gapX);
      expect(A.y).toBe(30 + SIZE.gapY);
      expect(layout.columns).toEqual([50, 200, 350]);
    });

    it("returns an empty layout without nodes", () => {
      expect(layoutFlowGraph([], [], SIZE)).toEqual({ nodes: [], edges: [], width: 0, height: 0 });
    });
  });
});

describe("awe-client-react/test/js/utilities/flowGraphTest.js buildFlowGraph", () => {
  const row = (taskId, parentId = null, extra = {}) => ({ id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra });
  const diamond = (extra = {}) => [
    row(1, null, extra[1]), row(2, 1, extra[2]), row(3, 1, extra[3]), row(4, 2, extra[4]), row(4, 3, extra[4])
  ];
  const ids = graph => graph.nodes.map(node => node.id).sort();

  it("builds one node per task and one edge per parent → child", () => {
    const graph = buildFlowGraph(diamond());

    expect(ids(graph)).toEqual(["1", "2", "3", "4"]);
    expect(graph.edges.map(edge => `${edge.from}>${edge.to}`).sort()).toEqual(["1>2", "1>3", "2>4", "3>4"]);
    expect(graph.run).toBe(false);
  });

  it("shows only the tasks reachable from the root task", () => {
    const graph = buildFlowGraph([...diamond({ 2: { rootTask: 2 } }), row(5, 1), row(6)]);

    expect(ids(graph)).toEqual(["2", "4"]);
  });

  it("shows every flow without a root, leaving out the tasks without dependencies", () => {
    expect(ids(buildFlowGraph([...diamond(), row(6)]))).toEqual(["1", "2", "3", "4"]);
  });

  it("labels each edge with the condition of its parent", () => {
    const graph = buildFlowGraph(diamond({ 2: { onWarning: 1 }, 3: { onError: 1, onWarning: 1 } }));
    const condition = (from, to) => graph.edges.find(edge => edge.from === from && edge.to === to).condition;

    expect([condition("1", "2"), condition("2", "4"), condition("3", "4")]).toEqual(["ok", "warning", "error"]);
  });

  it("maps the state of every task in a run, a task after a failed parent being skipped", () => {
    const graph = buildFlowGraph([
      row(1, null, { rootRun: "run", executionStatus: 0 }),
      row(2, 1, { runStatus: "LAUNCHED", executionStatus: 4 }),
      row(3, 1, { runStatus: "LAUNCHED", executionStatus: 1 }),
      row(4, 2, { runStatus: "SKIPPED" }), row(4, 3, { runStatus: "SKIPPED" }),
      row(5, 2, { runStatus: "WAITING" }),
      row(6, 5),
      row(7, 1, { runStatus: "LAUNCHED", executionStatus: 2 }),
      row(8, 1, { runStatus: "LAUNCHED", executionStatus: 3 })
    ]);
    const state = Object.fromEntries(graph.nodes.map(node => [node.id, node.state]));

    expect(graph.run).toBe(true);
    expect(state).toEqual({
      1: "ok", 2: "running", 3: "error", 4: "skipped", 5: "waiting", 6: "not-reached", 7: "warning", 8: "stopped"
    });
  });
});

describe("awe-client-react/test/js/utilities/flowGraphTest.js buildTaskGraph", () => {
  const row = (taskId, parentId = null, extra = {}) => ({ id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra });
  // 1 → 3, 2 → 3, 3 → 4, 3 → 5, 4 → 6: the task 3 is the root
  const rows = (extra = {}) => [row(1, null, extra[1]), row(2, null, extra[2]), row(3, 1, { rootTask: 3, ...extra[3] }),
    row(3, 2, { rootTask: 3, ...extra[3] }), row(4, 3, extra[4]), row(5, 3), row(6, 4)];

  it("keeps one level: the parents of the task, the task and its children, each in its column", () => {
    const graph = buildTaskGraph(rows());

    expect(graph.nodes.map(node => [node.id, node.role, node.layer])).toEqual([
      ["1", "parent", 0], ["2", "parent", 0], ["3", "self", 1], ["4", "child", 2], ["5", "child", 2]]);
    expect(graph.edges.map(edge => `${edge.from}>${edge.to}`)).toEqual(["1>3", "2>3", "3>4", "3>5"]);
    expect(graph.task).toBe(true);
  });

  it("gives every edge the condition of its parent and every task the state of its last execution", () => {
    const graph = buildTaskGraph(rows({ 1: { onWarning: 1, lastStatus: 0 }, 3: { onError: 1, lastStatus: 2 } }));
    const edge = (from, to) => graph.edges.find(item => item.from === from && item.to === to);

    expect(getConditionLabel(edge("1", "3"))).toBe("SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING");
    expect(getConditionLabel(edge("2", "3"))).toBe("SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK");
    expect(getConditionLabel(edge("3", "4"))).toBe("SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_ERROR");
    expect(getConditionLabel({ onWarning: true, onError: true })).toBe("SCREEN_TEXT_TASK_WHEN_AFTER_ANY");
    expect(Object.fromEntries(graph.nodes.map(node => [node.id, node.state]))).toEqual({
      1: "ok", 2: undefined, 3: "warning", 4: undefined, 5: undefined });
  });

  it("draws an edge once when the rows of a task repeat", () => {
    expect(buildTaskGraph([...rows(), row(3, 1, { rootTask: 3 })]).edges).toHaveLength(4);
  });

  it("keeps the three columns in order when one side is empty, the empty one beside the task", () => {
    const size = { nodeWidth: 100, nodeHeight: 40, gapX: 50, gapY: 10, top: 20, layers: 3 };
    // Only parents: the column of the children stays the third one, empty
    const graph = buildTaskGraph([row(1, null), row(3, 1, { rootTask: 3 })]);
    const layout = layoutFlowGraph(graph.nodes, graph.edges, size);
    const columns = getTaskGraphColumns(layout, size);
    const self = layout.nodes.find(node => node.role === "self");

    expect(columns.map(column => [column.title, column.left, column.empty])).toEqual([
      ["SCREEN_TEXT_FLOW_GRAPH_PARENTS", 50, false], ["SCREEN_TEXT_FLOW_GRAPH_THIS_TASK", 200, false],
      ["SCREEN_TEXT_FLOW_GRAPH_CHILDREN", 350, true]]);
    expect(columns[2].none).toEqual({ top: self.y, height: 40 });
    expect(layout.width).toBe(3 * 100 + 4 * 50);
    // Only children: the column of the parents is the first one, empty
    const children = buildTaskGraph([row(3, null, { rootTask: 3 }), row(4, 3)]);
    expect(getTaskGraphColumns(layoutFlowGraph(children.nodes, children.edges, size), size)
      .map(column => column.empty)).toEqual([true, false, false]);
  });

  it("gives every task when it last ran and its next execution, and a child the other tasks it waits for", () => {
    // 3 is the root; its child 4 also waits for 7, a task out of the level
    const graph = buildTaskGraph([...rows({ 1: { lastWhen: "hoy 08:00", nextRun: "Próxima: mañana 03:00" } }),
      row(4, 7), row(7, null)]);
    const node = id => graph.nodes.find(item => item.id === id);

    expect([node("1").lastWhen, node("1").nextRun]).toEqual(["hoy 08:00", "Próxima: mañana 03:00"]);
    expect([node("3").lastWhen, node("3").nextRun]).toEqual([undefined, undefined]);
    expect(node("4").alsoWaitsFor).toEqual([{ id: "7", name: "Task 7" }]);
    expect(node("5").alsoWaitsFor).toEqual([]);
    expect(node("1").alsoWaitsFor).toBeUndefined();
  });

  it("tells the condition of an edge as the endings of its parent that launch the child", () => {
    expect(getConditionIcons({ onWarning: false, onError: false })).toEqual(["ok"]);
    expect(getConditionIcons({ onWarning: true, onError: false })).toEqual(["ok", "warning"]);
    expect(getConditionIcons({ onWarning: false, onError: true })).toEqual(["ok", "error"]);
    expect(getConditionIcons({ onWarning: true, onError: true })).toEqual(["ok", "warning", "error"]);
  });

  it("has no nodes for a task without parents nor children", () => {
    expect(buildTaskGraph([row(1, null, { rootTask: 1 }), row(2, 3)]).nodes).toEqual([]);
  });
});

describe("awe-client-react/test/js/utilities/flowGraphTest.js flow run", () => {
  const flowGraph = require("../../../src/utilities/flowGraph");
  const row = (taskId, parentId = null, extra = {}) =>
    ({ id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra });
  const at = time => `17/11/2026 ${time}`;
  const time = clock => new Date(2026, 10, 17, ...clock.split(":").map(Number)).getTime();
  // The flow of the mock-up: 1 → 2, 1 → 3, 2 → 4, 3 → 4 (a join closed by 2), 4 → 5 (deferred, due at 12:15)
  const flow = () => flowGraph.buildFlowGraph([
    row(1, null, { rootRun: "run", executionStatus: 0, executionId: 1092, onWarning: 1, startDate: at("08:05:00"),
      endDate: at("08:06:12"), duration: 72000 }),
    row(2, 1, { runStatus: "LAUNCHED", executionStatus: 2, executionId: 640, onWarning: 1, startDate: at("08:06:12"),
      endDate: at("08:09:52"), edgeState: "MET" }),
    row(3, 1, { runStatus: "LAUNCHED", executionStatus: 0, executionId: 875, startDate: at("08:06:12"),
      endDate: at("08:08:17"), edgeState: "MET" }),
    ...[2, 3].map(parent => row(4, parent, { runStatus: "LAUNCHED", executionStatus: 0, executionId: 512,
      startDate: at("08:09:52"), endDate: at("08:10:40"), joinedBy: 2, joinWait: 95000,
      edgeState: parent === 2 ? "CLOSED_JOIN" : "MET" })),
    row(5, 4, { runStatus: "LAUNCHED", executionStatus: 4, executionId: 415, deferred: 1, startDate: at("08:10:40"),
      deferredDate: at("08:15:00"), dueDate: at("12:15:00"), edgeState: "MET" })
  ]);
  const node = (graph, id) => graph.nodes.find(item => item.id === id);
  const edge = (graph, from, to) => graph.edges.find(item => item.from === from && item.to === to);

  it("gives every task of a run its execution, times, due date, the parent that closed its join and the wait", () => {
    const graph = flow();

    expect(node(graph, "4")).toMatchObject({ state: "ok", executionId: 512, start: at("08:09:52"),
      end: at("08:10:40"), joinedBy: "2", joinWait: 95000 });
    expect(node(graph, "1").duration).toBe(72000);
    expect(node(graph, "5")).toMatchObject({ state: "deferred", executionId: 415, deferredSince: at("08:15:00"),
      due: at("12:15:00") });
    expect(node(graph, "5").end).toBeUndefined();
  });

  it("maps every state of a run to its icon and its label", () => {
    const { RUN_STATES } = flowGraph;

    expect(Object.keys(RUN_STATES).sort()).toEqual(["deferred", "error", "interrupted", "not-reached", "ok",
      "running", "skipped", "stopped", "waiting", "warning"]);
    expect(RUN_STATES.deferred).toMatchObject({ icon: "fa-clock-o", label: "SCREEN_TEXT_ACTIVITY_WAITING" });
    expect([RUN_STATES.waiting, RUN_STATES["not-reached"]]).toMatchObject([
      { icon: "fa-circle-o", label: "SCREEN_TEXT_FLOW_RUN_PENDING" },
      { icon: "fa-circle-o", label: "SCREEN_TEXT_FLOW_RUN_PENDING" }]);
    expect(RUN_STATES.ok).toMatchObject({ icon: "fa-check", label: "ENUM_STATUS_OK" });
  });

  it("labels an edge with the endings of its parent that launch the child, short and as a sentence", () => {
    const { getEdgeLabel } = flowGraph;

    expect(getEdgeLabel({ onWarning: false, onError: false })).toEqual(
      { label: "SCREEN_TEXT_FLOW_RUN_EDGE_OK", sentence: "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK" });
    expect(getEdgeLabel({ onWarning: true, onError: false }).label).toBe("SCREEN_TEXT_FLOW_RUN_EDGE_OK_WARNING");
    expect(getEdgeLabel({ onWarning: false, onError: true }).label).toBe("SCREEN_TEXT_FLOW_RUN_EDGE_OK_ERROR");
    expect(getEdgeLabel({ onWarning: true, onError: true })).toEqual(
      { label: "SCREEN_TEXT_FLOW_RUN_EDGE_ANY", sentence: "SCREEN_TEXT_TASK_WHEN_AFTER_ANY" });
  });

  it("gives every edge of a run its label, the state the server brings and marks the edge that closed the join", () => {
    const graph = flow();

    expect(edge(graph, "1", "2")).toMatchObject({ label: "SCREEN_TEXT_FLOW_RUN_EDGE_OK_WARNING", state: "met",
      closing: false });
    expect(edge(graph, "2", "4")).toMatchObject({ state: "met", closing: true });
    expect(edge(graph, "3", "4")).toMatchObject({ label: "SCREEN_TEXT_FLOW_RUN_EDGE_OK", state: "met",
      closing: false });
    expect(edge(graph, "2", "4").condition).toBe("warning");
  });

  it("derives the state of an edge from its parent when the server does not bring it", () => {
    const { getEdgeState } = flowGraph;
    const options = { onWarning: true, onError: false };

    expect(["ok", "warning", "error", "stopped", "interrupted", "skipped", "running", "deferred", "waiting",
      "not-reached"].map(state => getEdgeState(state, options))).toEqual(["met", "met", "not-met", "not-met",
      "not-met", "cut", "pending", "pending", "pending", "pending"]);
    const graph = flowGraph.buildFlowGraph([row(1, null, { rootRun: "run", executionStatus: 1 }), row(2, 1)]);
    expect(edge(graph, "1", "2").state).toBe("not-met");
  });

  it("draws the timeline on one linear axis when the run has no long idle gap", () => {
    const graph = flowGraph.buildFlowGraph([
      row(1, null, { rootRun: "run", executionStatus: 0, startDate: at("08:00:00"), endDate: at("08:01:00") }),
      row(2, 1, { executionStatus: 0, startDate: "2026-11-17T08:01:00", endDate: "2026-11-17T08:02:00" })]);
    const timeline = flowGraph.buildRunTimeline(graph, { now: time("10:00:00"), width: 1000 });

    expect(timeline.segments).toEqual([{ from: time("08:00:00"), to: time("08:02:00"), x: 0, width: 1000,
      compressed: false, step: 60000 }]);
    expect(timeline.breaks).toEqual([]);
    expect(timeline.now).toBeNull();
    expect(timeline.rows.map(item => item.bars.map(bar => [bar.state, bar.x, bar.width, bar.open]))).toEqual([
      [["ok", 0, 500, false]], [["ok", 500, 500, false]]]);
    expect(timeline.ticks.map(tick => [tick.time, tick.x, tick.unit])).toEqual([
      [time("08:00:00"), 0, "minute"], [time("08:01:00"), 500, "minute"], [time("08:02:00"), 1000, "minute"]]);
  });

  it("breaks the axis of a long idle gap so minutes of work and hours of waiting fit one row", () => {
    const timeline = flowGraph.buildRunTimeline(flow(), { now: time("10:42:00"), width: 1000 });
    const [linear, compressed] = timeline.segments;
    const row5 = timeline.rows.find(item => item.id === "5");

    expect(timeline.segments).toHaveLength(2);
    expect(linear).toMatchObject({ from: time("08:05:00"), to: time("08:15:00"), x: 0, width: 700,
      compressed: false, step: 120000 });
    expect(compressed).toMatchObject({ from: time("08:15:00"), to: time("12:15:00"), x: 700, width: 300,
      compressed: true, step: 3600000 });
    expect(timeline.breaks).toEqual([{ x: 700, from: time("08:15:00"), to: time("12:15:00") }]);
    expect(timeline.ticks.map(tick => [tick.time, tick.unit])).toEqual([
      ["08:06", "minute"], ["08:08", "minute"], ["08:10", "minute"], ["08:12", "minute"], ["08:14", "minute"],
      ["09:00", "hour"], ["10:00", "hour"], ["11:00", "hour"], ["12:00", "hour"]]
      .map(([clock, unit]) => [time(`${clock}:00`), unit]));
    expect(timeline.now.time).toBe(time("10:42:00"));
    expect(timeline.now.x).toBeCloseTo(883.75);
    expect(row5.bars.map(bar => [bar.state, bar.open])).toEqual([["running", false], ["deferred", true]]);
    expect(row5.bars[1].x).toBeCloseTo(700);
    expect(row5.bars[1].width).toBeCloseTo(183.75);
    expect(row5.due).toEqual({ time: time("12:15:00"), x: 1000 });
    expect(timeline.rows.map(item => item.id)).toEqual(["1", "2", "4", "5", "3"]);
  });

  it("puts the join wait before the task, from the end of the first parent to the end of the closing one", () => {
    const timeline = flowGraph.buildRunTimeline(flow(), { now: time("10:42:00"), width: 1000 });
    const { wait } = timeline.rows.find(item => item.id === "4");

    expect([wait.from, wait.to]).toEqual([time("08:08:17"), time("08:09:52")]);
    expect(wait.x).toBeCloseTo(197 / 600 * 700);
    expect(wait.width).toBeCloseTo(95 / 600 * 700);
    expect(timeline.rows.find(item => item.id === "2").wait).toBeNull();
  });

  it("lists the run as a tree with a join under the parent that closed it and «also feeds» under the other", () => {
    const list = flowGraph.buildRunList(flow());
    const tree = items => items.map(item => [item.id, tree(item.children)]);
    const [root] = list;
    const [two, three] = root.children;
    const four = two.children[0];

    expect(tree(list)).toEqual([["1", [["2", [["4", [["5", []]]]]], ["3", []]]]]);
    expect(root.edge).toBeNull();
    expect(two.edge).toMatchObject({ from: "1", label: "SCREEN_TEXT_FLOW_RUN_EDGE_OK_WARNING" });
    expect(four.join).toEqual({ parents: ["2", "3"], joinedBy: "2", wait: 95000 });
    expect(two.join).toBeNull();
    expect(three.alsoFeeds.map(item => [item.id, item.name, item.edge.label])).toEqual([
      ["4", "Task 4", "SCREEN_TEXT_FLOW_RUN_EDGE_OK"]]);
    expect(two.alsoFeeds).toEqual([]);
  });

  // Translation of the tests: the key with its arguments
  const translate = (key, ...args) => [key, ...args].join(" ");

  it("shows a run node with its state, execution, times, due date and the join it closed, built from the dates", () => {
    const graph = flow();
    const view = id => flowGraph.getRunNodeView(node(graph, id), graph, translate);

    expect(view("1")).toMatchObject({ icon: "fa-check", stateText: "ENUM_STATUS_OK",
      executionId: "1092", timing: "1 min 12 s · 08:05 – 08:06", due: null, join: null, selectable: true });
    expect(view("4").join).toBe("SCREEN_TEXT_FLOW_RUN_JOINED_BY Task 2 · SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s");
    expect(view("5")).toMatchObject({ timing: "08:10 – SCREEN_TEXT_FLOW_RUN_NOW",
      due: "SCREEN_TEXT_FLOW_RUN_DUE 12:15" });
    expect(view("5").label).toBe("Task 5 · SCREEN_TEXT_ACTIVITY_WAITING · SCREEN_TEXT_EXECUTION 415 · "
      + "08:10 – SCREEN_TEXT_FLOW_RUN_NOW · SCREEN_TEXT_FLOW_RUN_DUE 12:15");
  });

  it("shows the texts the server translated instead of the ones built here, and a task without execution opens nothing", () => {
    const graph = flowGraph.buildFlowGraph([
      row(1, null, { rootRun: "run", executionStatus: 0, executionId: 7, ExeTim: "1 min" }),
      row(3, 1, { executionStatus: 0, executionId: 8 }),
      ...[1, 3].map(parent => row(2, parent, { executionStatus: 4, executionId: 9, deferred: 1, joinedBy: 1,
        joinedByText: "Unido por Task 1", joinWaitText: "esperó 2 s", ExeDueLbl: "Vence 12:15",
        conditionLabel: parent === 1 ? "OK o Aviso" : null })),
      row(4, 2, { runStatus: "SKIPPED", skipCause: "Omitida: Task 2 terminó en Error" })
    ]);
    const view = id => flowGraph.getRunNodeView(node(graph, id), graph, translate);

    expect(view("1").timing).toBe("1 min");
    expect(view("2")).toMatchObject({ due: "Vence 12:15", join: "Unido por Task 1 · esperó 2 s" });
    expect(flowGraph.getRunEdgeText(edge(graph, "1", "2"), translate)).toBe("OK o Aviso");
    expect(flowGraph.getRunEdgeText(edge(graph, "3", "2"), translate)).toBe("SCREEN_TEXT_FLOW_RUN_EDGE_OK");
    expect(view("4")).toMatchObject({ stateText: "SCREEN_TEXT_FLOW_GRAPH_SKIPPED", executionId: null,
      skip: "Omitida: Task 2 terminó en Error", selectable: false });
  });

  it("says the wait of a join only when it waited, in the node, the list and the timeline note", () => {
    // 2 and 3 ended at the same time: 4 waited nothing (an older server still sent «esperó » without a duration)
    const graph = flowGraph.buildFlowGraph([
      row(1, null, { rootRun: "run", executionStatus: 0, executionId: 1, startDate: at("08:00:00"),
        endDate: at("08:01:00") }),
      ...[2, 3].map(task => row(task, 1, { executionStatus: 0, executionId: task, startDate: at("08:01:00"),
        endDate: at("08:02:00") })),
      ...[2, 3].map(parent => row(4, parent, { executionStatus: 0, executionId: 4, startDate: at("08:02:00"),
        endDate: at("08:03:00"), joinedBy: 2, joinWait: 0, joinedByText: "Unido por Task 2", joinWaitText: "esperó " }))
    ]);
    const noText = flowGraph.buildFlowGraph([row(1, null, { rootRun: "run" }), row(2, 1), row(3, 1),
      ...[2, 3].map(parent => row(4, parent, { joinedBy: 2, joinWait: 0 }))]);
    const [root] = flowGraph.getRunListView(graph, translate);
    const timeline = flowGraph.getRunTimelineView(graph, translate, { now: time("10:00:00") });

    expect(flowGraph.getRunNodeView(node(graph, "4"), graph, translate).join).toBe("Unido por Task 2");
    expect(flowGraph.getRunNodeView(node(noText, "4"), noText, translate).join)
      .toBe("SCREEN_TEXT_FLOW_RUN_JOINED_BY Task 2");
    expect(root.children[0].children[0].join).toBe("Unido por Task 2");
    expect(timeline.rows.find(item => item.id === "4")).toMatchObject({ wait: null, note: "Unido por Task 2" });
  });

  it("skips a task the server marks as skipped, a task never reached in an ended run", () => {
    const graph = flowGraph.buildFlowGraph([row(1, null, { rootRun: "run", executionStatus: 1 }),
      row(2, 1, { state: "skipped", edgeState: "CUT" }), row(3, 1)]);

    expect([node(graph, "2").state, node(graph, "3").state]).toEqual(["skipped", "not-reached"]);
    expect(edge(graph, "1", "2").state).toBe("cut");
  });

  it("zooms a step in or out between half and twice the size, and fits the width without going over 100 %", () => {
    const { getNextZoom } = flowGraph;

    expect([getNextZoom(1, "in"), getNextZoom(1, "out"), getNextZoom(2, "in"), getNextZoom(0.5, "out")])
      .toEqual([1.25, 0.75, 2, 0.5]);
    expect(getNextZoom(1, "fit", { available: 600, width: 1000 })).toBe(0.6);
    expect(getNextZoom(0.5, "fit", { available: 2000, width: 1000 })).toBe(1);
    expect(getNextZoom(1, "fit", { available: 100, width: 1000 })).toBe(0.5);
  });

  it("starts a run graph at the zoom that fits its width and height (never over 100 % nor under 50 %), not before it is measured", () => {
    const { getFitZoom } = flowGraph;

    expect(getFitZoom({ width: 1000, height: 400 }, { width: 800, height: 600 })).toBe(0.8);
    expect(getFitZoom({ width: 1000, height: 400 }, { width: 2000, height: 300 })).toBe(0.75);
    expect(getFitZoom({ width: 500, height: 300 }, { width: 2000, height: 900 })).toBe(1);
    expect(getFitZoom({ width: 3000, height: 400 }, { width: 600, height: 900 })).toBe(0.5);
    expect([getFitZoom({ width: 1000, height: 400 }, { width: 0, height: 600 }),
      getFitZoom({ width: 1000, height: 400 }, { width: 800, height: 0 })]).toEqual([null, null]);
  });

  it("sends every column of the row of the selected node as <column>.selected, like the selected row of a grid", () => {
    const rows = [row(1, null, { IdeTsk: 1, ExeTsk: 1092 }), row(2, 1, { IdeTsk: 2, ExeTsk: { value: 640 } }),
      row(2, 3, { IdeTsk: 2, ExeTsk: 999 })];

    expect(flowGraph.getRunSelectedValues(rows, ["2"])).toEqual({ "id.selected": "2-1", "taskId.selected": 2,
      "parentId.selected": 1, "name.selected": "Task 2", "IdeTsk.selected": 2, "ExeTsk.selected": 640 });
    expect(flowGraph.getRunSelectedValues(rows, [])).toEqual({});
  });

  it("draws the graph view unless the view parameter asks for the timeline or the list", () => {
    const { getRunView } = flowGraph;

    expect([undefined, "graph", "timeline", "list", "gantt"].map(getRunView))
      .toEqual(["graph", "graph", "timeline", "list", "graph"]);
  });

  it("shows the timeline with clock ticks, the break, «Now», the bars, the join wait and the due date as texts", () => {
    const view = flowGraph.getRunTimelineView(flow(), translate, { now: time("10:42:00") });
    const row = id => view.rows.find(item => item.id === id);

    expect(view.ticks.map(tick => tick.text)).toEqual(["08:06", "08:08", "08:10", "08:12", "08:14", "09:00", "10:00",
      "11:00", "12:00"]);
    expect(view.breaks).toEqual([{ x: 70, width: 30 }]);
    expect(view.now).toEqual({ x: expect.closeTo(88.375), text: "SCREEN_TEXT_FLOW_RUN_NOW 10:42" });
    expect(row("1")).toMatchObject({ name: "Task 1", stateText: "ENUM_STATUS_OK", executionId: "1092", selectable: true });
    expect(row("1").bars.map(bar => [bar.text, bar.label])).toEqual([["1 min 12 s",
      "Task 1 · ENUM_STATUS_OK · 08:05 – 08:06 · 1 min 12 s"]]);
    expect(row("5").bars.map(bar => [bar.state, bar.open, bar.text])).toEqual([["running", false, "4 min 20 s"],
      ["deferred", true, "SCREEN_TEXT_ACTIVITY_WAITING · 2 h 27 min"]]);
    expect(row("5").bars[1].label).toBe("Task 5 · SCREEN_TEXT_ACTIVITY_WAITING · 08:15 – SCREEN_TEXT_FLOW_RUN_NOW · 2 h 27 min");
    expect(row("5").due).toEqual({ x: 100, text: "SCREEN_TEXT_FLOW_RUN_DUE 12:15" });
    expect(row("4").wait).toMatchObject({ text: "SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s",
      label: "Task 4 · SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s · 08:08 – 08:09" });
    expect(row("4").note).toBe("SCREEN_TEXT_FLOW_RUN_JOINED_BY Task 2 · SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s");
  });

  it("shows the list with the condition of every edge, a join once and «also feeds» under its other parent", () => {
    const [root] = flowGraph.getRunListView(flow(), translate);
    const [two, three] = root.children;
    const four = two.children[0];

    expect(root).toMatchObject({ id: "1", stateText: "ENUM_STATUS_OK", timing: "1 min 12 s · 08:05 – 08:06",
      condition: null, selectable: true });
    expect(two.condition).toBe("SCREEN_TEXT_TASK_WHEN_AFTER Task 1 SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING");
    expect(four).toMatchObject({ id: "4",
      join: "SCREEN_TEXT_FLOW_RUN_JOINED_BY Task 2 · SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s" });
    expect(four.label).toBe("Task 4 · ENUM_STATUS_OK · SCREEN_TEXT_EXECUTION 512 · 08:09 – 08:10 · "
      + "SCREEN_TEXT_FLOW_RUN_JOINED_BY Task 2 · SCREEN_TEXT_FLOW_RUN_JOIN_WAIT 1 min 35 s · "
      + "SCREEN_TEXT_TASK_WHEN_AFTER Task 2 SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING");
    expect(four.children.map(item => item.id)).toEqual(["5"]);
    expect(three.children).toEqual([]);
    expect(three.alsoFeeds).toEqual([{ id: "4",
      text: "SCREEN_TEXT_FLOW_RUN_ALSO_FEEDS Task 4 SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK" }]);
  });
});
