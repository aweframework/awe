import { buildFlowGraph, layoutFlowGraph } from "../../../src/utilities/flowGraph";

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
