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
 * Nodes that all carry a `layer` keep it (an empty layer keeps its column) instead of the layer of their dependencies.
 * @param {{nodeWidth: number, nodeHeight: number, gapX: number, gapY: number, top: number, layers: number}} size Node
 * size, gaps, the room above the nodes (the titles of the columns) and the least number of columns (the empty ones at
 * the end keep their column too)
 * @returns {{nodes: object[], edges: object[], width: number, height: number, columns: number[]}} Positioned nodes,
 * edges and the left side of each column
 */
export function layoutFlowGraph(nodes, edges, { nodeWidth, nodeHeight, gapX, gapY, top = 0, layers: least = 0 }) {
  if (!nodes.length) return { nodes: [], edges: [], width: 0, height: 0 };

  const ids = nodes.map(node => node.id);
  const known = new Set(ids);
  const valid = edges.filter(({ from, to }) => known.has(from) && known.has(to) && from !== to);
  // Nodes that all bring their layer keep it (the one-level graph of a task), else the layer of their dependencies
  const fixed = nodes.every(node => Number.isInteger(node.layer));
  const layerOf = fixed ? new Map(nodes.map(node => [node.id, node.layer])) : getLayers(ids, valid);
  const links = valid.filter(({ from, to }) => layerOf.get(from) < layerOf.get(to));

  const layers = [];
  ids.forEach(id => (layers[layerOf.get(id)] ??= []).push(id));
  // A fixed layer may be empty (a task without parents): it keeps its column
  for (let index = 0; index < Math.max(layers.length, least); index++) layers[index] ??= [];
  orderLayers(layers, links);

  const layerHeight = layer => Math.max(0, layer.length * nodeHeight + (layer.length - 1) * gapY);
  const maxHeight = Math.max(...layers.map(layerHeight));
  const placed = new Map();
  layers.forEach((layer, layerIndex) => layer.forEach((id, index) => placed.set(id, {
    layer: layerIndex,
    x: gapX + layerIndex * (nodeWidth + gapX),
    y: top + gapY + (maxHeight - layerHeight(layer)) / 2 + index * (nodeHeight + gapY)
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
    height: top + maxHeight + 2 * gapY,
    // Left side of each column, for the titles of the columns
    columns: layers.map((layer, index) => gapX + index * (nodeWidth + gapX))
  };
}

// Execution status codes of the scheduler (StaTyp): queued counts as running in a run
const EXECUTION_STATES = { 0: "ok", 1: "error", 2: "warning", 3: "stopped", 4: "running", 5: "running", 6: "interrupted" };
// Join state of a task in a run (AweSchRunTsk.Sta) when it has no execution yet
const JOIN_STATES = { WAITING: "waiting", SKIPPED: "skipped", LAUNCHED: "running" };

const isSet = value => value !== null && value !== undefined && value !== "";
const isTrue = value => isSet(value) && Number(value) === 1;

/**
 * State of a task in a run: deferred when its execution waits for a confirmation, else the status of its execution,
 * else skipped when the server says so (a task never reached in an ended run, getFlowRunGraph state column), else its
 * join state, else not reached
 * @param {object} row Graph row of the task
 * @returns {string} Node state
 */
function getRunState({ executionStatus, runStatus, deferred, state }) {
  if (isTrue(deferred)) return "deferred";
  if (isSet(executionStatus)) return EXECUTION_STATES[executionStatus] ?? "running";
  if (state === "skipped") return "skipped";
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

// Run columns of a task row (the flow run page) and the node field each one fills, with its conversion
const RUN_COLUMNS = [
  ["executionId", "executionId"], ["startDate", "start"], ["endDate", "end"], ["duration", "duration", Number],
  ["deferredDate", "deferredSince"], ["dueDate", "due"], ["joinedBy", "joinedBy", String],
  ["joinWait", "joinWait", Number], ["skipCause", "skipCause"],
  // Texts the server already translated: the parent that closed the join, its wait, the due date and the duration
  ["joinedByText", "joinedByText", String], ["joinWaitText", "joinWaitText", String], ["ExeDueLbl", "dueText", String],
  ["ExeTim", "durationText", String]
];

/**
 * Run fields of a task: only the run columns its row brings
 * @param {object} row Graph row of the task
 * @returns {object} Node fields
 */
function getRunDetails(row) {
  return Object.fromEntries(RUN_COLUMNS.filter(([column]) => isSet(row[column]))
    .map(([column, field, convert]) => [field, convert ? convert(row[column]) : row[column]]));
}

/**
 * Icon, label and line of the box of every state of a task in a run (the flow run page): pending (waiting or not
 * reached) dotted, skipped and deferred (waiting for a confirmation) dashed, the rest solid
 */
export const RUN_STATES = {
  ok: { icon: "fa-check", label: "ENUM_STATUS_OK", line: "solid" },
  warning: { icon: "fa-exclamation-triangle", label: "ENUM_STATUS_WARNING", line: "solid" },
  error: { icon: "fa-times", label: "ENUM_STATUS_ERROR", line: "solid" },
  stopped: { icon: "fa-pause", label: "ENUM_STATUS_STOPPED", line: "solid" },
  interrupted: { icon: "fa-exclamation-circle", label: "ENUM_STATUS_INTERRUPTED", line: "solid" },
  running: { icon: "fa-refresh", label: "ENUM_STATUS_RUNNING", line: "solid" },
  deferred: { icon: "fa-clock-o", label: "SCREEN_TEXT_ACTIVITY_WAITING", line: "dashed" },
  skipped: { icon: "fa-ban", label: "SCREEN_TEXT_FLOW_GRAPH_SKIPPED", line: "dashed" },
  waiting: { icon: "fa-circle-o", label: "SCREEN_TEXT_FLOW_RUN_PENDING", line: "dotted" },
  "not-reached": { icon: "fa-circle-o", label: "SCREEN_TEXT_FLOW_RUN_PENDING", line: "dotted" }
};

const EDGE_LABELS = {
  "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK": "SCREEN_TEXT_FLOW_RUN_EDGE_OK",
  "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING": "SCREEN_TEXT_FLOW_RUN_EDGE_OK_WARNING",
  "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_ERROR": "SCREEN_TEXT_FLOW_RUN_EDGE_OK_ERROR",
  "SCREEN_TEXT_TASK_WHEN_AFTER_ANY": "SCREEN_TEXT_FLOW_RUN_EDGE_ANY"
};

/**
 * Condition of an edge of a run from the options of its parent (LchDepWrn, LchDepErr): a short label for the graph
 * («OK», «OK o Aviso», «OK o Error», «OK, Aviso o Error») and the sentence of getConditionLabel for the list
 * @param {{onWarning: boolean, onError: boolean}} options Options of the parent
 * @returns {{label: string, sentence: string}} Locale keys
 */
export function getEdgeLabel(options) {
  const sentence = getConditionLabel(options);
  return { label: EDGE_LABELS[sentence], sentence };
}

const FINISHED_STATES = ["ok", "warning", "error", "stopped", "interrupted"];

/**
 * State of an edge of a run from the state of its parent, when the server does not bring it: cut when the parent was
 * skipped, pending while the parent has not finished, else met when the parent ended in one of the endings that launch
 * the child (getConditionIcons) and not met otherwise
 * @param {string} parentState Run state of the parent
 * @param {{onWarning: boolean, onError: boolean}} options Options of the parent
 * @returns {string} met, not-met, cut or pending
 */
export function getEdgeState(parentState, options) {
  if (parentState === "skipped") return "cut";
  if (!FINISHED_STATES.includes(parentState)) return "pending";
  return getConditionIcons(options).includes(parentState) ? "met" : "not-met";
}

/**
 * Run fields of an edge: its labels, its state (the edgeState column, else derived) and whether it closed the join
 * @param {{from: string, column: ?string}} edge Edge with the edgeState column of its row
 * @param {object} parent Parent node
 * @param {object} child Child node
 * @param {boolean} join The child has more than one parent
 * @returns {{label: string, sentence: string, state: string, closing: boolean}} Edge fields
 */
function getRunEdge({ from, column }, parent, child, join) {
  const options = { onWarning: isTrue(parent.onWarning), onError: isTrue(parent.onError) };
  const state = isSet(column) ? String(column).toLowerCase().replace(/_/g, "-") : null;
  const closed = state === "closed-join";
  return {
    ...getEdgeLabel(options),
    state: closed ? "met" : state ?? getEdgeState(parent.state, options),
    closing: closed || (join && child.joinedBy === from)
  };
}

/**
 * Nodes and edges of a flow graph from the rows of the getFlowGraph query (one row per task and parent).
 * A root task (rootTask) or the root of a run (rootRun) narrows the graph to the tasks reachable from it; without a
 * root every flow is shown, leaving out the tasks without dependencies.
 *
 * Run rows (a rootRun in the rows) give every node its state (RUN_STATES) and, when the row brings them, these run
 * columns (repeated in every row of the task), each one filling a node field:
 * - executionId → executionId: last execution of the task in the run
 * - startDate → start, endDate → end: times of that execution (epoch ms, ISO 8601 or dd/MM/yyyy HH:mm:ss, see toTime);
 *   no endDate while it runs or waits
 * - duration → duration: duration of the execution in ms
 * - deferred (1/0): the execution waits for a confirmation (RUNNING + AweSchExeExt): the state is deferred
 * - deferredDate → deferredSince: when it started to wait (DfrDat); dueDate → due: when it expires (DueDat)
 * - joinedBy → joinedBy: task id of the parent that closed its join (the parent with the latest end <= its start)
 * - joinWait → joinWait: wait in the join in ms (end of the parent that closed it − end of the first parent)
 * - skipCause → skipCause: why it was skipped (text or locale key)
 * - joinedByText, joinWaitText, ExeDueLbl → dueText, ExeTim → durationText: those values as texts the server already
 *   translated («Unido por …», «esperó …», the due date, the duration), shown instead of the ones built here
 * and every edge, besides its condition, its labels (getEdgeLabel), its state (met, not-met, cut or pending) and
 * whether it closed the join (closing). The state comes from the edgeState column of the row of the child with that
 * parent (MET, NOT_MET, CUT, PENDING, or CLOSED_JOIN for a met edge that closed the join), else from getEdgeState;
 * the conditionLabel column (the condition as a text the server translated) gives it its conditionText.
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
        root: isSet(row.rootTask) || isSet(row.rootRun),
        ...(run ? { state: getRunState(row), ...getRunDetails(row) } : {})
      });
    }
    if (isSet(row.parentId)) {
      edges.push({ from: String(row.parentId), to: id, column: row.edgeState, text: row.conditionLabel });
    }
  });

  const linked = uniqueEdges(edges).filter(edge => nodes.has(edge.from));
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

  const scoped = linked.filter(edge => scope.has(edge.from) && scope.has(edge.to));
  const join = id => scoped.filter(edge => edge.to === id).length > 1;
  return {
    nodes: [...nodes.values()].filter(node => scope.has(node.id))
      .map(({ onWarning, onError, root, ...node }) => node),
    edges: scoped.map(({ column, text, ...edge }) => ({
      ...edge, condition: getCondition(nodes.get(edge.from)),
      ...(run ? getRunEdge({ ...edge, column }, nodes.get(edge.from), nodes.get(edge.to), join(edge.to)) : {}),
      ...(run && isSet(text) ? { conditionText: String(text) } : {})
    })),
    run
  };
}

// dd/MM/yyyy[ HH:mm[:ss[.SSS]]], the AWE date format
const DATE_TIME = /^(\d{2})\/(\d{2})\/(\d{4})(?:[ T](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?)?$/;

/**
 * Time of a date of a run row, in epoch ms: a number, a numeric text, dd/MM/yyyy HH:mm:ss (local time) or anything
 * Date.parse reads (ISO 8601)
 * @param {*} value Date
 * @returns {?number} Epoch ms, null when it is not set or not a date
 */
export function toTime(value) {
  if (!isSet(value)) return null;
  if (typeof value === "number") return value;
  const text = String(value).trim();
  if (/^\d+$/.test(text)) return Number(text);
  const match = DATE_TIME.exec(text);
  if (match) {
    const [, day, month, year, hour = 0, minute = 0, second = 0, milli = 0] = match;
    return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second),
      Number(String(milli).padEnd(3, "0"))).getTime();
  }
  const time = Date.parse(text);
  return Number.isNaN(time) ? null : time;
}

/**
 * The run as a nested ordered list from its root tasks, following the edges in their order: a join appears once, under
 * the parent that closed it (joinedBy, else its first parent); the other parents keep an «also feeds» entry to it.
 * Every item is its node with the edge that reaches it (null for a root), its join (the parents, the one that closed
 * it and the wait; null when it has one parent), its children and its alsoFeeds entries ({id, name, edge}).
 * @param {{nodes: object[], edges: object[]}} graph Run graph (buildFlowGraph)
 * @returns {object[]} Root items
 */
export function buildRunList({ nodes, edges }) {
  const byId = new Map(nodes.map(node => [node.id, node]));
  const parentsOf = id => edges.filter(edge => edge.to === id);
  const home = id => {
    const parents = parentsOf(id);
    return (parents.find(edge => edge.from === byId.get(id).joinedBy) ?? parents[0])?.from;
  };
  const seen = new Set();
  const item = (id, edge) => {
    seen.add(id);
    const node = byId.get(id);
    const parents = parentsOf(id).map(parent => parent.from);
    const children = [];
    const alsoFeeds = [];
    edges.filter(out => out.from === id && byId.has(out.to)).forEach(out => {
      if (home(out.to) === id && !seen.has(out.to)) children.push(item(out.to, out));
      else alsoFeeds.push({ id: out.to, name: byId.get(out.to).name, edge: out });
    });
    return {
      ...node, edge: edge ?? null, children, alsoFeeds,
      join: parents.length > 1 ? { parents, joinedBy: node.joinedBy ?? edge.from, wait: node.joinWait ?? null } : null
    };
  };
  return nodes.filter(node => !parentsOf(node.id).length).map(node => item(node.id, null));
}

const MINUTE = 60000;
const HOUR = 60 * MINUTE;
// Steps of the ticks of a linear and of a compressed segment of the timeline axis
const LINEAR_STEPS = [1, 2, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440].map(minutes => minutes * MINUTE);
const COMPRESSED_STEPS = [1, 2, 3, 6, 12, 24, 48, 168].map(hours => hours * HOUR);
const LINEAR_TICKS = 8;
const COMPRESSED_TICKS = 4;

/**
 * Bars, join wait and due date of a task on the timeline: its execution (open up to now while it runs); a deferred
 * execution runs up to deferredSince and then waits, open up to now
 */
function getTaskSpans(node, nodes, now) {
  const start = toTime(node.start);
  if (start === null) return { bars: [], wait: null, due: null };
  const end = toTime(node.end);
  const bars = [];
  if (node.state === "deferred") {
    const since = toTime(node.deferredSince) ?? start;
    if (since > start) bars.push({ state: "running", from: start, to: since, open: false });
    bars.push({ state: "deferred", from: since, to: Math.max(since, now), open: true });
  } else {
    bars.push({ state: node.state, from: start, to: end ?? Math.max(start, now), open: end === null });
  }
  const closedAt = isSet(node.joinedBy) ? toTime(nodes.get(node.joinedBy)?.end) ?? start : null;
  const wait = closedAt !== null && node.joinWait > 0 ? { from: closedAt - node.joinWait, to: closedAt } : null;
  const due = node.state === "deferred" ? toTime(node.due) : null;
  return { bars, wait, due };
}

/**
 * Merged busy intervals: the execution bars, not the waits for a confirmation
 */
function getWork(spans) {
  const work = spans.flatMap(span => span.bars).filter(bar => bar.state !== "deferred")
    .map(({ from, to }) => [from, to]).sort((a, b) => a[0] - b[0]);
  return work.reduce((merged, [from, to]) => {
    const last = merged[merged.length - 1];
    if (last && from <= last[1]) last[1] = Math.max(last[1], to);
    else merged.push([from, to]);
    return merged;
  }, []);
}

/**
 * Segments of the axis: linear where the run works, compressed in every idle gap longer than gapShare of the run
 */
function getSegments(from, to, work, gapShare) {
  const idle = [];
  let cursor = from;
  [...work, [to, to]].forEach(([start, end]) => {
    if (start - cursor > gapShare * (to - from)) idle.push([cursor, start]);
    cursor = Math.max(cursor, end);
  });
  const segments = [];
  cursor = from;
  idle.forEach(([start, end]) => {
    if (start > cursor) segments.push({ from: cursor, to: start, compressed: false });
    segments.push({ from: start, to: end, compressed: true });
    cursor = end;
  });
  if (to > cursor || !segments.length) segments.push({ from: cursor, to, compressed: false });
  return segments;
}

/**
 * First tick step of a list that leaves at most a number of ticks in a time span
 */
const getStep = (span, steps, most) => steps.find(step => span / step <= most) ?? steps[steps.length - 1];

/**
 * Ticks of a segment, at the multiples of its step in local time
 */
function getTicks(segment, scale) {
  const offset = time => -new Date(time).getTimezoneOffset() * MINUTE;
  const unit = segment.step >= 24 * HOUR ? "day" : segment.step >= HOUR ? "hour" : "minute";
  const ticks = [];
  let time = Math.ceil((segment.from + offset(segment.from)) / segment.step) * segment.step - offset(segment.from);
  for (; time <= segment.to; time += segment.step) {
    ticks.push({ time, x: scale(time), unit });
  }
  return ticks;
}

/**
 * Timeline of a run (the Cronograma view): one row per task in the order of the list (buildRunList) with its bars
 * (state, x, width, open while it runs or waits for a confirmation), its join wait and the due date of a deferred task,
 * on a broken axis: every idle gap longer than gapShare of the run is compressed into a segment of its own (each one
 * breakShare of the width, together at most 60 %), so minutes of work and hours of waiting fit one row; the work keeps
 * the rest of the width, linear. Times are epoch ms; x and width are in the units of the width. Ticks are data (time,
 * x and unit minute, hour or day to format them); every compressed segment is a break (≈) at its x. now («Ahora») is
 * set while a bar is open.
 * @param {{nodes: object[], edges: object[]}} graph Run graph (buildFlowGraph)
 * @param {{now: number, width: number, gapShare: number, breakShare: number}} [options] Current time, width of the axis
 * (1 by default), share of the run an idle gap needs to be compressed (0.25) and width share of a compressed segment
 * (0.3)
 * @returns {{from: ?number, to: ?number, width: number, segments: object[], breaks: object[], ticks: object[],
 * now: ?{time: number, x: number}, rows: object[]}} Timeline
 */
export function buildRunTimeline(graph, { now = Date.now(), width = 1, gapShare = 0.25, breakShare = 0.3 } = {}) {
  const nodes = new Map(graph.nodes.map(node => [node.id, node]));
  const order = [];
  const visit = items => items.forEach(item => {
    order.push(item.id);
    visit(item.children);
  });
  visit(buildRunList(graph));
  graph.nodes.filter(node => !order.includes(node.id)).forEach(node => order.push(node.id));
  const spans = order.map(id => ({ node: nodes.get(id), ...getTaskSpans(nodes.get(id), nodes, now) }));

  const times = spans.flatMap(({ bars, wait, due }) => [...bars.flatMap(bar => [bar.from, bar.to]),
    ...(wait ? [wait.from, wait.to] : []), ...(due !== null ? [due] : [])]);
  const open = spans.some(span => span.bars.some(bar => bar.open));
  if (!times.length) {
    return {
      from: null, to: null, width, segments: [], breaks: [], ticks: [], now: null,
      rows: spans.map(({ node }) => ({ id: node.id, name: node.name, state: node.state, executionId: node.executionId,
        bars: [], wait: null, due: null }))
    };
  }
  const from = Math.min(...times);
  const to = Math.max(...times, ...(open ? [now] : []));

  const segments = getSegments(from, to, getWork(spans), gapShare);
  const compressed = segments.filter(segment => segment.compressed);
  const linear = segments.filter(segment => !segment.compressed);
  const compressedWidth = linear.length ? Math.min(breakShare * compressed.length, 0.6) * width : width;
  const linearSpan = linear.reduce((sum, segment) => sum + segment.to - segment.from, 0);
  let x = 0;
  segments.forEach(segment => {
    const span = segment.to - segment.from;
    segment.x = x;
    segment.width = segment.compressed ? compressedWidth / compressed.length
      : (linearSpan ? span / linearSpan : 1 / linear.length) * (width - compressedWidth);
    segment.step = segment.compressed ? getStep(span, COMPRESSED_STEPS, COMPRESSED_TICKS)
      : getStep(span, LINEAR_STEPS, Math.max(2, Math.round(LINEAR_TICKS * segment.width / width)));
    x += segment.width;
  });
  const scale = time => {
    const segment = segments.find(item => time <= item.to) ?? segments[segments.length - 1];
    const span = segment.to - segment.from;
    const offset = span ? (Math.min(Math.max(time, segment.from), segment.to) - segment.from) / span : 0;
    return segment.x + offset * segment.width;
  };
  const place = ({ from: start, to: end, ...rest }) => ({ ...rest, from: start, to: end, x: scale(start),
    width: scale(end) - scale(start) });

  return {
    from, to, width,
    segments: segments.map(({ from: start, to: end, x: left, width: size, compressed: broken, step }) =>
      ({ from: start, to: end, x: left, width: size, compressed: broken, step })),
    breaks: compressed.map(segment => ({ x: segment.x, from: segment.from, to: segment.to })),
    // A tick on the border of two segments is drawn once
    ticks: segments.flatMap(segment => getTicks(segment, scale))
      .filter((tick, index, ticks) => !index || tick.time !== ticks[index - 1].time),
    now: open ? { time: now, x: scale(now) } : null,
    rows: spans.map(({ node, bars, wait, due }) => ({
      id: node.id, name: node.name, state: node.state, executionId: node.executionId,
      bars: bars.map(place), wait: wait ? place(wait) : null, due: due !== null ? { time: due, x: scale(due) } : null
    }))
  };
}

/**
 * Geometry of the run graph (the flow run page), in px: taller nodes for their state, a name in two lines, the times
 * and the join or the due date, and room between levels for the condition of every edge
 */
export const RUN_GRAPH_SIZE = { nodeWidth: 224, nodeHeight: 120, gapX: 120, gapY: 24 };

/**
 * Zoom of the run graph: from half to twice its size, a quarter each step
 */
export const RUN_ZOOM = { min: 0.5, max: 2, step: 0.25 };

const clampZoom = zoom => Math.min(RUN_ZOOM.max, Math.max(RUN_ZOOM.min, Math.round(zoom * 100) / 100));

/**
 * Next zoom of the run graph: one step in or out, or the zoom that fits the graph to the width it has (never over
 * 100 %), always between RUN_ZOOM.min and RUN_ZOOM.max
 * @param {number} zoom Current zoom (1 = 100 %)
 * @param {string} action in, out or fit
 * @param {{available: number, width: number}} [room] Width the graph has and its width at 100 % (fit)
 * @returns {number} Zoom
 */
export function getNextZoom(zoom, action, { available = 0, width = 0 } = {}) {
  if (action === "in") return clampZoom(zoom + RUN_ZOOM.step);
  if (action === "out") return clampZoom(zoom - RUN_ZOOM.step);
  return width > 0 ? clampZoom(Math.min(1, available / width)) : zoom;
}

/**
 * Zoom a run graph starts at: the one that fits its width and its height in its viewport (never over 100 %), always
 * between RUN_ZOOM.min and RUN_ZOOM.max, so a flow of a few tasks is seen whole without scroll
 * @param {{width: number, height: number}} graph Size of the graph at 100 %
 * @param {{width: number, height: number}} viewport Size of its viewport
 * @returns {number|null} Zoom, or null while the viewport has no size (hidden, not laid out yet)
 */
export function getFitZoom({ width = 0, height = 0 } = {}, viewport = {}) {
  if (!(viewport.width > 0 && viewport.height > 0) || !(width > 0 && height > 0)) return null;
  return clampZoom(Math.min(1, viewport.width / width, viewport.height / height));
}

const pad = value => String(value).padStart(2, "0");

/**
 * Clock time (HH:mm, local) of a date of a run row
 */
const getClock = value => {
  const time = toTime(value);
  if (time === null) return null;
  const date = new Date(time);
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

/**
 * A duration in ms as text: 1 h 5 min, 3 min 40 s or 48 s
 * @param {number} millis Duration
 * @returns {string} Text
 */
export function formatRunDuration(millis) {
  const seconds = Math.max(0, Math.round(millis / 1000));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours) return `${hours} h ${minutes} min`;
  return minutes ? `${minutes} min ${seconds % 60} s` : `${seconds % 60} s`;
}

/**
 * What a node of the run graph shows, the same in both engines: its state (icon, text and the line of its box), its
 * execution, its duration with its times (up to «Ahora» while it runs), the due date of a deferred one, the parent that
 * closed a join with its wait, why it was skipped, the accessible label with all of them and whether it opens its
 * execution (only a node with one: a pending or skipped task has none). The texts the server translated win over the
 * ones built here.
 * @param {object} node Node of the run graph (buildFlowGraph)
 * @param {{nodes: object[], edges: object[]}} graph Run graph
 * @param {function(string, ...string): string} translate Translate a locale key with its arguments
 * @returns {{icon: string, line: string, stateText: string, executionId: ?string, timing: ?string, due: ?string,
 * join: ?string, skip: ?string, label: string, selectable: boolean}} Node view
 */
export function getRunNodeView(node, graph, translate) {
  const state = RUN_STATES[node.state] ?? RUN_STATES["not-reached"];
  const stateText = translate(state.label);
  const executionId = isSet(node.executionId) ? String(node.executionId) : null;
  const start = getClock(node.start);
  const duration = node.durationText ?? (Number.isFinite(node.duration) ? formatRunDuration(node.duration) : null);
  const times = start ? `${start} – ${getClock(node.end) ?? translate("SCREEN_TEXT_FLOW_RUN_NOW")}` : null;
  const timing = [duration, times].filter(Boolean).join(" · ") || null;
  const dueClock = getClock(node.due);
  const due = node.state === "deferred"
    ? node.dueText ?? (dueClock ? translate("SCREEN_TEXT_FLOW_RUN_DUE", dueClock) : null) : null;
  // «Unido por …» and the wait only on a join (a task with more than one parent)
  const isJoin = graph.edges.filter(edge => edge.to === node.id).length > 1;
  const closer = isSet(node.joinedBy) ? graph.nodes.find(item => item.id === String(node.joinedBy)) : null;
  const joinedBy = node.joinedByText ?? (closer ? translate("SCREEN_TEXT_FLOW_RUN_JOINED_BY", closer.name) : null);
  // The wait only when it waited: a join whose parents ended at the same time (joinWait 0) says nothing about it
  const waited = !isSet(node.joinWait) || node.joinWait > 0;
  const wait = waited ? node.joinWaitText
    ?? (node.joinWait > 0 ? translate("SCREEN_TEXT_FLOW_RUN_JOIN_WAIT", formatRunDuration(node.joinWait)) : null) : null;
  const join = isJoin ? [joinedBy, wait].filter(Boolean).join(" · ") || null : null;
  const skip = node.state === "skipped" && isSet(node.skipCause) ? translate(node.skipCause) : null;
  const execution = executionId ? `${translate("SCREEN_TEXT_EXECUTION")} ${executionId}` : null;
  return {
    icon: state.icon, line: state.line, stateText, executionId, timing, due, join, skip,
    label: [node.name, stateText, execution, timing, due, join, skip].filter(Boolean).join(" · "),
    selectable: executionId !== null
  };
}

/**
 * Text of the condition of an edge of the run graph: the one the server translated (conditionLabel), else the short
 * label of getEdgeLabel
 * @param {object} edge Edge of the run graph
 * @param {function(string): string} translate Translate a locale key
 * @returns {string} Text
 */
export const getRunEdgeText = (edge, translate) => edge.conditionText ?? translate(edge.label);

/**
 * Kind of an edge of the run graph for its line and its arrow: closing (the edge that closed the join, thicker),
 * else its state (met, not-met, cut or pending)
 * @param {object} edge Edge of the run graph
 * @returns {string} Kind
 */
export const getRunEdgeKind = edge => (edge.closing ? "closing" : edge.state ?? "pending");

/**
 * Kinds of the edges of the run graph, in the order of the legend
 */
export const RUN_EDGE_KINDS = ["met", "closing", "not-met", "cut", "pending"];

/**
 * Locale keys of the legend of the run graph: one entry per state label (pending once) and per edge kind
 */
export const RUN_LEGEND = {
  states: ["ok", "warning", "error", "stopped", "interrupted", "running", "deferred", "skipped", "waiting"],
  edges: {
    met: "SCREEN_TEXT_FLOW_RUN_EDGE_MET", closing: "SCREEN_TEXT_FLOW_RUN_EDGE_CLOSED_JOIN",
    "not-met": "SCREEN_TEXT_FLOW_RUN_EDGE_NOT_MET", cut: "SCREEN_TEXT_FLOW_RUN_EDGE_CUT",
    pending: "SCREEN_TEXT_FLOW_RUN_PENDING"
  }
};

/**
 * Views of a run (the widget parameter view): the graph, the timeline (Cronograma) and the list (Lista)
 */
export const RUN_VIEWS = ["graph", "timeline", "list"];

/**
 * View of a run from the widget parameter view: the graph unless it asks for the timeline or the list
 * @param {string} [view] Widget parameter
 * @returns {string} graph, timeline or list
 */
export const getRunView = view => (RUN_VIEWS.includes(view) ? view : "graph");

/**
 * Day (dd/MM, local) of a time of the timeline
 */
const getDay = time => {
  const date = new Date(time);
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`;
};

/**
 * What the timeline view of a run shows, the same in both engines (positions in % of the width of the axis): the ticks
 * with their clock (their day when the step is a day or more), every compressed segment of the broken axis, «Ahora»
 * with its clock while a bar is open and one row per task (buildRunTimeline order) with what its node shows
 * (getRunNodeView), its bars (state, duration as text, an accessible label with the task, the state, its times and the
 * duration; a deferred one also says its state), its join wait («esperó …»), the due date of a deferred task and a
 * note with the due date, the join it closed or why it was skipped
 * @param {{nodes: object[], edges: object[]}} graph Run graph (buildFlowGraph)
 * @param {function(string, ...string): string} translate Translate a locale key with its arguments
 * @param {{now: number}} [options] Current time
 * @returns {{ticks: object[], breaks: object[], now: ?{x: number, text: string}, rows: object[]}} Timeline view
 */
export function getRunTimelineView(graph, translate, { now = Date.now() } = {}) {
  const timeline = buildRunTimeline(graph, { now, width: 100 });
  const byId = new Map(graph.nodes.map(node => [node.id, node]));
  const nowText = translate("SCREEN_TEXT_FLOW_RUN_NOW");
  return {
    ticks: timeline.ticks.map(tick => ({ x: tick.x, text: tick.unit === "day" ? getDay(tick.time) : getClock(tick.time) })),
    breaks: timeline.segments.filter(segment => segment.compressed).map(segment => ({ x: segment.x, width: segment.width })),
    now: timeline.now ? { x: timeline.now.x, text: `${nowText} ${getClock(timeline.now.time)}` } : null,
    rows: timeline.rows.map(row => {
      const node = byId.get(row.id);
      const view = getRunNodeView(node, graph, translate);
      const bars = row.bars.map((bar, index) => {
        const stateText = translate((RUN_STATES[bar.state] ?? RUN_STATES["not-reached"]).label);
        const duration = formatRunDuration(bar.to - bar.from);
        const times = `${getClock(bar.from)} – ${bar.open ? nowText : getClock(bar.to)}`;
        return {
          key: `${row.id}-${index}`, state: bar.state, x: bar.x, width: bar.width, open: bar.open,
          text: bar.state === "deferred" ? `${stateText} · ${duration}` : duration,
          label: [node.name, stateText, times, duration].join(" · ")
        };
      });
      const waitText = row.wait ? translate("SCREEN_TEXT_FLOW_RUN_JOIN_WAIT", formatRunDuration(row.wait.to - row.wait.from))
        : null;
      return {
        id: row.id, name: node.name, state: node.state, icon: view.icon, line: view.line, stateText: view.stateText,
        executionId: view.executionId, label: view.label, selectable: view.selectable, bars,
        wait: row.wait ? { x: row.wait.x, width: row.wait.width, text: waitText,
          label: [node.name, waitText, `${getClock(row.wait.from)} – ${getClock(row.wait.to)}`].join(" · ") } : null,
        due: row.due ? { x: row.due.x, text: view.due ?? getClock(row.due.time) } : null,
        note: [view.due, view.join, view.skip].filter(Boolean).join(" · ") || null
      };
    })
  };
}

/**
 * What the list view of a run shows, the same in both engines: the tree of buildRunList with what every node shows
 * (getRunNodeView), the condition of the edge that reaches it («Después de <parent> si termina en OK…»), an accessible
 * label with both and, under the other parents of a join, its «También alimenta a <task> si termina en…» entries
 * @param {{nodes: object[], edges: object[]}} graph Run graph (buildFlowGraph)
 * @param {function(string, ...string): string} translate Translate a locale key with its arguments
 * @returns {object[]} Root items, each one with its children and alsoFeeds ({id, text})
 */
export function getRunListView(graph, translate) {
  const byId = new Map(graph.nodes.map(node => [node.id, node]));
  const item = entry => {
    const view = getRunNodeView(entry, graph, translate);
    const condition = entry.edge
      ? `${translate("SCREEN_TEXT_TASK_WHEN_AFTER", byId.get(entry.edge.from).name)} ${translate(entry.edge.sentence)}`
      : null;
    return {
      id: entry.id, name: entry.name, state: entry.state, icon: view.icon, line: view.line, stateText: view.stateText,
      executionId: view.executionId, timing: view.timing, due: view.due, join: view.join, skip: view.skip,
      selectable: view.selectable, condition, label: [view.label, condition].filter(Boolean).join(" · "),
      children: entry.children.map(item),
      alsoFeeds: entry.alsoFeeds.map(feed => ({ id: feed.id,
        text: `${translate("SCREEN_TEXT_FLOW_RUN_ALSO_FEEDS", feed.name)} ${translate(feed.edge.sentence)}` }))
    };
  };
  return buildRunList(graph).map(item);
}

/**
 * Values a run graph sends for its selected node, like the selected row of a grid: every column of the first row of
 * the node as <column>.selected (a cell with a value sends its value), nothing without a selected node
 * @param {object[]} rows Rows of the graph
 * @param {*} selected Selected node (a task id, or a list with it)
 * @returns {object} Values
 */
export function getRunSelectedValues(rows = [], selected = null) {
  const id = Array.isArray(selected) ? selected[0] : selected;
  if (!isSet(id)) return {};
  const row = rows.find(item => String(item.taskId) === String(id));
  if (!row) return {};
  return Object.fromEntries(Object.entries(row).map(([column, value]) => [`${column}.selected`,
    value !== null && typeof value === "object" && "value" in value ? value.value : value ?? null]));
}

/**
 * Edges without repetitions (a task with two last executions started at the same time brings its rows twice)
 * @param {{from: string, to: string}[]} edges Edges
 * @returns {{from: string, to: string}[]} Every edge once
 */
function uniqueEdges(edges) {
  const seen = new Set();
  return edges.filter(({ from, to }) => {
    const key = `${from}>${to}`;
    return seen.has(key) ? false : seen.add(key);
  });
}

/**
 * Locale of the condition of an edge of the one-level graph: what the parent has to end with to launch the child
 * @param {{onWarning: boolean, onError: boolean}} edge Edge
 * @returns {string} Locale key
 */
export function getConditionLabel({ onWarning, onError }) {
  if (onWarning && onError) return "SCREEN_TEXT_TASK_WHEN_AFTER_ANY";
  if (onWarning) return "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING";
  return onError ? "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_ERROR" : "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK";
}

/**
 * Condition of an edge of the one-level graph as icons: the endings of the parent that launch the child, always OK and
 * also warning and error when the parent launches it on them
 * @param {{onWarning: boolean, onError: boolean}} edge Edge
 * @returns {string[]} ok, warning and error, in this order
 */
export function getConditionIcons({ onWarning, onError }) {
  return ["ok", ...(onWarning ? ["warning"] : []), ...(onError ? ["error"] : [])];
}

/**
 * Titles of the three columns of the one-level graph of a task
 */
export const TASK_GRAPH_COLUMNS = ["SCREEN_TEXT_FLOW_GRAPH_PARENTS", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK",
  "SCREEN_TEXT_FLOW_GRAPH_CHILDREN"];

/**
 * Columns of the one-level graph of a task for a side (the widget parameter side): the parents side draws the tasks it
 * runs after and the task, the children side the task and the tasks it launches, both in two columns; without a side
 * the three columns (#993, the task editor)
 * @param {string} [side] parents, children or none
 * @returns {string[]} Titles of its columns, left to right
 */
export function getTaskGraphTitles(side) {
  if (side === "parents") return TASK_GRAPH_COLUMNS.slice(0, 2);
  if (side === "children") return TASK_GRAPH_COLUMNS.slice(1);
  return TASK_GRAPH_COLUMNS;
}

/**
 * Text of an empty column of the one-level graph of a task
 */
export const TASK_GRAPH_NONE = "SCREEN_TEXT_FLOW_GRAPH_NONE";

/**
 * Columns of the one-level graph of a task, left to right: the tasks it runs after, the task and the tasks it launches.
 * Each one has its title and its left side; an empty one (a task without parents or without children) also has the
 * box of its «None» text, beside the task
 * @param {{nodes: object[], columns: number[]}} layout Layout of the graph (layoutFlowGraph with three layers)
 * @param {{nodeWidth: number, nodeHeight: number}} size Node size
 * @param {string[]} [titles] Titles of the columns (getTaskGraphTitles), the three columns by default
 * @returns {{title: string, left: number, width: number, empty: boolean, none: ?{top: number, height: number}}[]}
 */
export function getTaskGraphColumns(layout, { nodeWidth, nodeHeight }, titles = TASK_GRAPH_COLUMNS) {
  const self = layout.nodes.find(node => node.role === "self");
  return titles.map((title, index) => {
    const empty = !layout.nodes.some(node => node.layer === index);
    return {
      title, left: layout.columns[index], width: nodeWidth, empty,
      none: empty && self ? { top: self.y, height: nodeHeight } : null
    };
  });
}

/**
 * One-level graph of a task (the task screen) from the rows of the getFlowGraph query: the tasks it runs after (its
 * parents, first column), the task (rootTask, second column) and the tasks it launches (its children, third column),
 * each edge with the condition of its parent (onWarning, onError). Every node has its role (parent, self or child)
 * and, when its row brings them, the state of its last execution (lastStatus), when it started (lastWhen) and its next
 * execution (nextRun); a child also has the other tasks it waits for (alsoWaitsFor: the parents that are not the task).
 * With a side only that side is drawn, in two columns (see getTaskGraphTitles): the parents and the task, or the task
 * and its children; the graph has no nodes when that side is empty.
 * @param {object[]} rows Query rows
 * @param {string} [side] parents, children or none (both sides, three columns)
 * @returns {{nodes: object[], edges: object[], run: boolean, task: boolean}} Graph, without nodes when the task has
 * no parent nor child
 */
export function buildTaskGraph(rows = [], side = null) {
  const nodes = new Map();
  const edges = [];
  let root = null;
  rows.forEach(row => {
    const id = String(row.taskId);
    if (!nodes.has(id)) {
      nodes.set(id, {
        id, name: row.name, launchIcon: row.launchIcon, launchLabel: row.launchLabel,
        active: !isSet(row.active) || isTrue(row.active), onWarning: isTrue(row.onWarning), onError: isTrue(row.onError),
        ...(isSet(row.lastStatus) ? { state: EXECUTION_STATES[row.lastStatus] ?? "running" } : {}),
        ...(isSet(row.lastWhen) ? { lastWhen: row.lastWhen } : {}),
        ...(isSet(row.nextRun) ? { nextRun: row.nextRun } : {})
      });
    }
    if (isSet(row.rootTask)) root = id;
    if (isSet(row.parentId)) edges.push({ from: String(row.parentId), to: id });
  });

  const known = uniqueEdges(edges).filter(edge => nodes.has(edge.from) && edge.from !== edge.to);
  const allParents = known.filter(edge => edge.to === root).map(edge => edge.from);
  const parents = side === "children" ? [] : allParents;
  const children = side === "parents" ? []
    : known.filter(edge => edge.from === root && !allParents.includes(edge.to)).map(edge => edge.to);
  if (root === null || (!parents.length && !children.length)) return { nodes: [], edges: [], run: false, task: true };
  // The children side starts at the task: its columns are the task and its children
  const shift = side === "children" ? 1 : 0;

  const node = (id, role, layer) => {
    const { onWarning, onError, ...rest } = nodes.get(id);
    return { ...rest, role, layer: layer - shift };
  };
  // The parents of a child that are not the task: the child also waits for them (a join)
  const alsoWaitsFor = id => known.filter(edge => edge.to === id && edge.from !== root)
    .map(edge => ({ id: edge.from, name: nodes.get(edge.from).name }));
  const edge = (from, to) => {
    const { onWarning, onError } = nodes.get(from);
    return { from, to, onWarning, onError, condition: getCondition(nodes.get(from)) };
  };
  return {
    nodes: [...parents.map(id => node(id, "parent", 0)), node(root, "self", 1), ...children.map(id => ({ ...node(id, "child", 2), alsoWaitsFor: alsoWaitsFor(id) }))],
    edges: [...parents.map(id => edge(id, root)), ...children.map(id => edge(root, id))],
    run: false,
    task: true
  };
}
