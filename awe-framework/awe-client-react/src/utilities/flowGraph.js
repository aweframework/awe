const ORDER_SWEEPS = 4;

/**
 * Longest-path layer of every node: a node goes one layer after its deepest parent.
 * Nodes left in a cycle (rejected by the scheduler since #978) go to one extra layer.
 */
function getLayers(ids, links) {
  const children = new Map(ids.map(id => [id, []]));
  const pending = new Map(ids.map(id => [id, 0]));
  links.forEach(({ from, to }) => {
    children.get(from).push(to);
    pending.set(to, pending.get(to) + 1);
  });

  const layer = new Map();
  const queue = ids.filter(id => pending.get(id) === 0);
  queue.forEach(id => layer.set(id, 0));
  while (queue.length) {
    const id = queue.shift();
    children.get(id).forEach(child => {
      layer.set(child, Math.max(layer.get(child) ?? 0, layer.get(id) + 1));
      pending.set(child, pending.get(child) - 1);
      if (pending.get(child) === 0) queue.push(child);
    });
  }

  const cycleLayer = layer.size ? Math.max(...layer.values()) + 1 : 0;
  ids.filter(id => pending.get(id) > 0).forEach(id => layer.set(id, cycleLayer));
  return layer;
}

/**
 * Sort every layer by the mean position of its neighbours in the previous one (barycenter), sweeping down and up,
 * so parents and children stay at the same height and the edges cross less
 */
function orderLayers(layers, links) {
  const position = new Map();
  const updatePositions = () => layers.forEach(layer => layer.forEach((id, index) => position.set(id, index)));
  const neighbours = (id, down) => links.filter(link => (down ? link.to : link.from) === id)
    .map(link => position.get(down ? link.from : link.to));

  updatePositions();
  for (let sweep = 0; sweep < ORDER_SWEEPS; sweep++) {
    const down = sweep % 2 === 0;
    const sequence = down ? layers.slice(1) : layers.slice(0, -1).reverse();
    sequence.forEach(layer => {
      const weight = new Map(layer.map(id => {
        const around = neighbours(id, down);
        return [id, around.length ? around.reduce((sum, value) => sum + value, 0) / around.length : position.get(id)];
      }));
      layer.sort((a, b) => weight.get(a) - weight.get(b));
      layer.forEach((id, index) => position.set(id, index));
    });
  }
}

/**
 * Layered layout (left to right) of a directed acyclic graph: one column per dependency level.
 * shortcut: edges that skip levels are drawn straight across, without dummy nodes; add them if a flow gets crowded.
 * @param {{id: string}[]} nodes Graph nodes (any extra field is kept)
 * @param {{from: string, to: string}[]} edges Parent → child edges
 * @param {{nodeWidth: number, nodeHeight: number, gapX: number, gapY: number}} size Node size and gaps
 * @returns {{nodes: object[], edges: object[], width: number, height: number}} Positioned nodes and edges
 */
export function layoutFlowGraph(nodes, edges, { nodeWidth, nodeHeight, gapX, gapY }) {
  if (!nodes.length) return { nodes: [], edges: [], width: 0, height: 0 };

  const ids = nodes.map(node => node.id);
  const known = new Set(ids);
  const valid = edges.filter(({ from, to }) => known.has(from) && known.has(to) && from !== to);
  const layerOf = getLayers(ids, valid);
  const links = valid.filter(({ from, to }) => layerOf.get(from) < layerOf.get(to));

  const layers = [];
  ids.forEach(id => (layers[layerOf.get(id)] ??= []).push(id));
  orderLayers(layers, links);

  const layerHeight = layer => layer.length * nodeHeight + (layer.length - 1) * gapY;
  const maxHeight = Math.max(...layers.map(layerHeight));
  const placed = new Map();
  layers.forEach((layer, layerIndex) => layer.forEach((id, index) => placed.set(id, {
    layer: layerIndex,
    x: gapX + layerIndex * (nodeWidth + gapX),
    y: gapY + (maxHeight - layerHeight(layer)) / 2 + index * (nodeHeight + gapY)
  })));

  return {
    nodes: nodes.map(node => ({ ...node, ...placed.get(node.id) })),
    edges: links.map(({ from, to, ...rest }) => {
      const parent = placed.get(from);
      const child = placed.get(to);
      return {
        ...rest, from, to,
        start: { x: parent.x + nodeWidth, y: parent.y + nodeHeight / 2 },
        end: { x: child.x, y: child.y + nodeHeight / 2 }
      };
    }),
    width: layers.length * nodeWidth + (layers.length + 1) * gapX,
    height: maxHeight + 2 * gapY
  };
}

// Execution status codes of the scheduler (StaTyp): queued counts as running in a run
const EXECUTION_STATES = { 0: "ok", 1: "error", 2: "warning", 3: "stopped", 4: "running", 5: "running", 6: "interrupted" };
// Join state of a task in a run (AweSchRunTsk.Sta) when it has no execution yet
const JOIN_STATES = { WAITING: "waiting", SKIPPED: "skipped", LAUNCHED: "running" };

const isSet = value => value !== null && value !== undefined && value !== "";
const isTrue = value => isSet(value) && Number(value) === 1;

/**
 * State of a task in a run: the status of its execution, else its join state, else not reached
 * @param {object} row Graph row of the task
 * @returns {string} Node state
 */
function getRunState({ executionStatus, runStatus }) {
  if (isSet(executionStatus)) return EXECUTION_STATES[executionStatus] ?? "running";
  return JOIN_STATES[runStatus] ?? "not-reached";
}

/**
 * Condition a parent puts on its children: the worst parent status that still launches them
 * @param {object} parent Parent node
 * @returns {string} ok, warning or error
 */
function getCondition(parent) {
  if (isTrue(parent.onError)) return "error";
  return isTrue(parent.onWarning) ? "warning" : "ok";
}

/**
 * Nodes and edges of a flow graph from the rows of the getFlowGraph query (one row per task and parent).
 * A root task (rootTask) or the root of a run (rootRun) narrows the graph to the tasks reachable from it; without a
 * root every flow is shown, leaving out the tasks without dependencies.
 * @param {object[]} rows Query rows
 * @returns {{nodes: object[], edges: object[], run: boolean}} Graph
 */
export function buildFlowGraph(rows = []) {
  const run = rows.some(row => isSet(row.rootRun));
  const nodes = new Map();
  const edges = [];
  rows.forEach(row => {
    const id = String(row.taskId);
    if (!nodes.has(id)) {
      nodes.set(id, {
        id, name: row.name, launchIcon: row.launchIcon, launchLabel: row.launchLabel,
        active: !isSet(row.active) || isTrue(row.active),
        onWarning: row.onWarning, onError: row.onError,
        root: isSet(row.rootTask) || isSet(row.rootRun), ...(run ? { state: getRunState(row) } : {})
      });
    }
    if (isSet(row.parentId)) edges.push({ from: String(row.parentId), to: id });
  });

  const linked = edges.filter(edge => nodes.has(edge.from));
  const roots = [...nodes.values()].filter(node => node.root).map(node => node.id);
  const scope = new Set(roots.length ? roots : linked.flatMap(edge => [edge.from, edge.to]));
  if (roots.length) {
    const pending = [...roots];
    while (pending.length) {
      const id = pending.pop();
      linked.filter(edge => edge.from === id && !scope.has(edge.to)).forEach(edge => {
        scope.add(edge.to);
        pending.push(edge.to);
      });
    }
  }

  return {
    nodes: [...nodes.values()].filter(node => scope.has(node.id))
      .map(({ onWarning, onError, root, ...node }) => node),
    edges: linked.filter(edge => scope.has(edge.from) && scope.has(edge.to))
      .map(edge => ({ ...edge, condition: getCondition(nodes.get(edge.from)) })),
    run
  };
}
