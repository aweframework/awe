import React, { useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";
import { useComponentState } from "../hooks/useComponentState";
import { updateModel } from "../redux/actions/components";
import { addActionsTop } from "../redux/actions/actions";
import { loadScreen } from "../redux/thunks/screen";
import { updateModelWithDependencies } from "../redux/thunks/components";
import ViewRegistry from "../redux/registry/ViewRegistry";
import { getIconCode, translateLabel } from "../utilities";
import {
  buildFlowGraph, buildTaskGraph, getConditionIcons, getConditionLabel, getFitZoom, getNextZoom, getRunEdgeKind, getRunEdgeText,
  getRunListView, getRunNodeView, getRunTimelineView, getRunView, getTaskGraphColumns, getTaskGraphTitles,
  layoutFlowGraph, RUN_EDGE_KINDS, RUN_GRAPH_SIZE, RUN_LEGEND, RUN_STATES, RUN_ZOOM, TASK_GRAPH_NONE
} from "../utilities/flowGraph";
import { TestIds, testHook } from "../utilities/testIds";
import "./AweFlowGraph.less";

// Geometry of the graph, in SVG user units (the layout positions the nodes with them)
// The gap between levels leaves room for the condition label of an edge
const SIZE = { nodeWidth: 208, nodeHeight: 56, gapX: 104, gapY: 20 };
// The one-level graph of a task always has its three columns, leaves room above the nodes for their titles and between
// them for the condition of every edge; its nodes are taller, for a name in two lines, its state and its next execution
const TASK_SIZE = { nodeWidth: 240, nodeHeight: 92, gapX: 136, gapY: 20, top: 28, layers: 3 };
// One side of a task (the task editor): two narrower columns, so the graph fits in a form column
const SIDE_SIZE = { nodeWidth: 220, nodeHeight: 92, gapX: 96, gapY: 20, top: 28, layers: 2 };

const STATE_LABELS = {
  ok: "ENUM_STATUS_OK", error: "ENUM_STATUS_ERROR", warning: "ENUM_STATUS_WARNING", stopped: "ENUM_STATUS_STOPPED",
  interrupted: "ENUM_STATUS_INTERRUPTED", running: "ENUM_STATUS_RUNNING", waiting: "SCREEN_TEXT_FLOW_GRAPH_WAITING",
  skipped: "SCREEN_TEXT_FLOW_GRAPH_SKIPPED", "not-reached": "SCREEN_TEXT_FLOW_GRAPH_NOT_REACHED"
};
const STATE_ICONS = {
  ok: "fa-check", error: "fa-times", warning: "fa-exclamation-triangle", stopped: "fa-pause",
  interrupted: "fa-exclamation-circle", running: "fa-refresh", waiting: "fa-hourglass-half", skipped: "fa-ban",
  "not-reached": "fa-circle-o"
};
const CONDITION_LABELS = { warning: "SCREEN_TEXT_FLOW_GRAPH_ON_WARNING", error: "SCREEN_TEXT_FLOW_GRAPH_ON_ERROR" };
// Icons of the conditions of the one-level graph and their label in the legend
const CONDITION_ICONS = { ok: "fa-check", warning: "fa-exclamation-triangle", error: "fa-times" };
const CONDITION_LEGEND = { ok: "ENUM_STATUS_OK", warning: "ENUM_STATUS_WARNING", error: "ENUM_STATUS_ERROR" };

/**
 * Cubic curve from the right side of a parent to the left side of a child
 */
const edgePath = ({ start, end }) => {
  const bend = (end.x - start.x) / 2;
  return `M ${start.x} ${start.y} C ${start.x + bend} ${start.y}, ${end.x - bend} ${end.y}, ${end.x} ${end.y}`;
};

/**
 * State of a task in the timeline and the list: its icon, its text and its execution
 */
const RunState = ({ item }) => <span className={`flow-graph-node-state flow-graph-legend-state is-${item.state}`}>
  {getIconCode(item.icon)}
  <span className="flow-graph-node-state-text">{item.stateText}</span>
  {item.executionId && <span className="flow-graph-node-execution">{item.executionId}</span>}
</span>;

RunState.propTypes = { item: PropTypes.object.isRequired };

/**
 * Grid lines, broken segments and «Now» of the timeline, under the bars of a row
 */
const TimelineMarks = ({ timeline }) => <span className="flow-graph-timeline-marks" aria-hidden="true">
  {timeline.ticks.map(tick => <span key={tick.x} className="flow-graph-timeline-line" style={{ left: `${tick.x}%` }} />)}
  {timeline.breaks.map(item => <span key={item.x} className="flow-graph-timeline-break"
    style={{ left: `${item.x}%`, width: `${item.width}%` }} />)}
  {timeline.now && <span className="flow-graph-timeline-now" style={{ left: `${timeline.now.x}%` }} />}
</span>;

TimelineMarks.propTypes = { timeline: PropTypes.object.isRequired };

/**
 * Timeline view of a run (Cronograma): a table with one row per task (its name, state, execution and a note with its
 * due date or join) and its bars on a broken time axis with its ticks, the breaks (≈) and «Now»; every bar of a task with
 * an execution is a toggle button that selects it like its node
 */
function RunTimeline({ timeline, selected, select, keep, translate }) {
  return <div className="flow-graph-timeline" role="table" aria-label={translate("SCREEN_TEXT_FLOW_RUN_TIMELINE")}>
    <div className="flow-graph-timeline-row flow-graph-timeline-head" role="row">
      <span className="flow-graph-timeline-task" role="columnheader">{translate("SCREEN_TEXT_FLOW_RUN_TASK")}</span>
      <span className="flow-graph-timeline-axis" role="columnheader">
        <TimelineMarks timeline={timeline} />
        {timeline.ticks.map(tick => <span key={tick.x} className="flow-graph-timeline-tick"
          style={{ left: `${tick.x}%` }}>{tick.text}</span>)}
        {timeline.breaks.map(item => <span key={item.x} className="flow-graph-timeline-break-mark" aria-hidden="true"
          style={{ left: `${item.x + item.width / 2}%` }}>≈</span>)}
        {timeline.now && <span className="flow-graph-timeline-now-label"
          style={{ left: `${timeline.now.x}%` }}>{timeline.now.text}</span>}
      </span>
    </div>
    {timeline.rows.map(row => <div key={row.id} className="flow-graph-timeline-row" role="row">
      <span className="flow-graph-timeline-task" role="rowheader">
        <span className="flow-graph-timeline-name" title={row.name}>{row.name}</span>
        <RunState item={row} />
        {row.note && <span className="flow-graph-node-meta" title={row.note}>{row.note}</span>}
      </span>
      <span className="flow-graph-timeline-track" role="cell">
        <TimelineMarks timeline={timeline} />
        {row.wait && <span className="flow-graph-timeline-bar is-join-wait" role="img" aria-label={row.wait.label}
          title={row.wait.label} style={{ left: `${row.wait.x}%`, width: `${row.wait.width}%` }}>{row.wait.text}</span>}
        {row.bars.map(bar => <button key={bar.key} type="button" ref={keep(row.id)}
          className={`flow-graph-timeline-bar is-${bar.state}${bar.open ? " is-open" : ""}`}
          style={{ left: `${bar.x}%`, width: `${bar.width}%` }} aria-label={bar.label} title={bar.label}
          aria-pressed={selected === row.id} aria-disabled={row.selectable ? undefined : true}
          onClick={() => select(row.id, row.selectable)} {...testHook(TestIds.flowGraphNode)}>{bar.text}</button>)}
        {row.due && <span className="flow-graph-timeline-due" aria-hidden="true" style={{ left: `${row.due.x}%` }}>
          <span>{row.due.text}</span></span>}
      </span>
    </div>)}
  </div>;
}

RunTimeline.propTypes = {
  timeline: PropTypes.object.isRequired,
  selected: PropTypes.string,
  select: PropTypes.func.isRequired,
  keep: PropTypes.func.isRequired,
  translate: PropTypes.func.isRequired
};

/**
 * Items of the list view of a run: each task a toggle button that selects it like its node (state, name, times, due
 * date, join, skip cause and the condition of its edge), its children nested and its «also feeds» entries
 */
function RunListItems({ items, selected, select, keep }) {
  return items.map(item => <li key={item.id} className="flow-graph-list-entry">
    <button type="button" ref={keep(item.id)} className={`flow-graph-list-item is-${item.state} is-line-${item.line}`}
      aria-label={item.label} aria-pressed={selected === item.id} aria-disabled={item.selectable ? undefined : true}
      onClick={() => select(item.id, item.selectable)} {...testHook(TestIds.flowGraphNode)}>
      <RunState item={item} />
      <span className="flow-graph-list-name">{item.name}</span>
      {item.timing && <span className="flow-graph-node-meta">{item.timing}</span>}
      {item.due && <span className="flow-graph-node-meta">{getIconCode("lucide:hourglass")}{item.due}</span>}
      {item.join && <span className="flow-graph-node-meta">{getIconCode("lucide:git-merge")}{item.join}</span>}
      {item.skip && <span className="flow-graph-node-meta">{item.skip}</span>}
      {item.condition && <span className="flow-graph-list-condition">{item.condition}</span>}
    </button>
    {(item.children.length > 0 || item.alsoFeeds.length > 0) && <ol className="flow-graph-list">
      <RunListItems items={item.children} selected={selected} select={select} keep={keep} />
      {item.alsoFeeds.map(feed => <li key={`feeds-${feed.id}`} className="flow-graph-list-entry">
        <span className="flow-graph-list-feeds">{getIconCode("lucide:git-merge")}{feed.text}</span>
      </li>)}
    </ol>}
  </li>);
}

RunListItems.propTypes = {
  items: PropTypes.array.isRequired,
  selected: PropTypes.string,
  select: PropTypes.func.isRequired,
  keep: PropTypes.func.isRequired
};

/**
 * Run mode of the flow graph (the flow run page, #993): every task of one run with its state (icon and text; the line
 * of its box tells pending, skipped or deferred apart), its execution, duration and times, the due date of a deferred
 * one and the parent that closed a join with its wait; every edge with its condition and its state in the run (met,
 * not met, cut, pending; the edge that closed a join thicker). A node with an execution is a toggle button: clicking it
 * (or Enter, Space) selects it and launches the actions of the widget (its dependencies on its click event) with the
 * columns of its row sent as <column>.selected, like a grid row (IdeTsk.selected, ExeTsk.selected); a pending or
 * skipped task stays focusable but opens nothing. The selected node is kept in view and announced; Escape clears the
 * selection. A small toolbar zooms in, out and fits the graph to its width; the graph starts at the zoom that fits it
 * in its viewport (getFitZoom), measured the first time it is shown.
 * The view parameter draws the same run as a timeline (one row per task, its bars on a broken time axis, «Now») or as
 * a nested list (a join under the parent that closed it, «also feeds» under the others); their bars and items select
 * the task like its node.
 * A dependency of the screen hides it (visible false), so a page shows its views one at a time: it stays mounted with its
 * zoom and selection, out of the layout and of the accessibility tree (hidden).
 */
function FlowRunGraph({ id, style, model, address, target, view, hidden }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [zoom, setZoom] = useState(1);
  const viewport = useRef(null);
  const fitted = useRef(false);
  const nodeRefs = useRef(new Map());
  const values = model.values;
  const translate = (key, ...args) => translateLabel(key, t, ...args);

  const { graph, layout } = useMemo(() => {
    const built = buildFlowGraph(values);
    return { graph: built, layout: layoutFlowGraph(built.nodes, built.edges, RUN_GRAPH_SIZE) };
  }, [values]);
  // The selected node lives in selectedNode: a selection (model.selected) would replace the rows of the graph
  const selected = model.selectedNode ?? null;

  // The selected node stays in view (the side panel it opens covers part of the page)
  useEffect(() => {
    if (selected) nodeRefs.current.get(selected)?.scrollIntoView?.({ block: "nearest", inline: "nearest" });
  }, [selected, zoom]);

  // The graph starts at the zoom that fits it, once its viewport has a size (it is shown); the user zoom stays after
  useEffect(() => {
    const element = viewport.current;
    if (fitted.current || hidden || !element || !layout.nodes.length) return;
    const fit = getFitZoom(layout, { width: element.clientWidth, height: element.clientHeight });
    if (fit === null) return;
    fitted.current = true;
    setZoom(fit);
  }, [layout, hidden]);

  if (!layout.nodes.length) {
    return <div className={`flow-graph flow-graph-empty ${style}`} id={id} hidden={hidden}>
      {translate("SCREEN_TEXT_FLOW_GRAPH_EMPTY")}</div>;
  }
  const views = new Map(layout.nodes.map(node => [node.id, getRunNodeView(node, graph, translate)]));
  const select = (nodeId, selectable) => {
    if (!selectable) return;
    dispatch(updateModelWithDependencies(address, { event: "click", selectedNode: nodeId }));
    if (target) dispatch(addActionsTop([{ type: "screen", target, address }]));
  };
  // The first element of a task (its node, its first bar, its item) is the one kept in view
  const keep = nodeId => element => {
    if (element && !nodeRefs.current.has(nodeId)) nodeRefs.current.set(nodeId, element);
    if (!element) nodeRefs.current.delete(nodeId);
  };
  const onKeyDown = event => {
    if (event.key === "Escape" && selected) dispatch(updateModelWithDependencies(address, { selectedNode: null }));
  };
  const changeZoom = action => setZoom(current => getNextZoom(current, action,
    { available: viewport.current?.clientWidth ?? 0, width: layout.width }));
  const selectedNode = selected ? layout.nodes.find(node => node.id === selected) : null;
  const tool = (action, icon, label, disabled) => <button type="button" className="flow-graph-tool"
    aria-label={translate(label)} title={translate(label)} disabled={disabled} onClick={() => changeZoom(action)}>
    {getIconCode(icon)}</button>;

  const legend = <div className="flow-graph-legend flow-graph-run-legend">
    <span className="flow-graph-legend-title">{translate("SCREEN_TEXT_FLOW_RUN_LEGEND_STATES")}</span>
    {RUN_LEGEND.states.map(state => <span key={state} className={`flow-graph-legend-item flow-graph-legend-state is-${state}`}>
      <span aria-hidden="true">{getIconCode(RUN_STATES[state].icon)}</span>{translate(RUN_STATES[state].label)}
    </span>)}
    {view === "graph" && <>
      <span className="flow-graph-legend-title">{translate("SCREEN_TEXT_FLOW_RUN_LEGEND_EDGES")}</span>
      {RUN_EDGE_KINDS.map(kind => <span key={kind} className="flow-graph-legend-item">
        <span className={`flow-graph-legend-edge is-${kind}`} aria-hidden="true" />{translate(RUN_LEGEND.edges[kind])}
      </span>)}
    </>}
    {view === "timeline" && <span className="flow-graph-legend-item">
      <span className="flow-graph-legend-bar is-join-wait" aria-hidden="true" />
      {translate("SCREEN_TEXT_FLOW_RUN_LEGEND_JOIN_WAIT")}
    </span>}
  </div>;
  const live = <span className="flow-graph-live" aria-live="polite">
    {selectedNode ? translate("SCREEN_TEXT_FLOW_GRAPH_SELECTED", views.get(selectedNode.id).label) : ""}</span>;

  if (view !== "graph") {
    return <div className={`flow-graph flow-graph-run flow-graph-run-${view} ${style}`} id={id} hidden={hidden}
      onKeyDown={onKeyDown}>
      {view === "timeline"
        ? <RunTimeline timeline={getRunTimelineView(graph, translate)} selected={selected} select={select} keep={keep}
          translate={translate} />
        : <ol className="flow-graph-list" aria-label={translate("SCREEN_TEXT_FLOW_RUN_LIST")}>
          <RunListItems items={getRunListView(graph, translate)} selected={selected} select={select} keep={keep} />
        </ol>}
      {legend}
      {live}
    </div>;
  }

  return <div className={`flow-graph flow-graph-run ${style}`} id={id} hidden={hidden} onKeyDown={onKeyDown}>
    <div className="flow-graph-toolbar" role="group" aria-label={translate("SCREEN_TEXT_FLOW_GRAPH_ZOOM")}>
      {tool("out", "lucide:zoom-out", "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT", zoom <= RUN_ZOOM.min)}
      <span className="flow-graph-zoom">{`${Math.round(zoom * 100)} %`}</span>
      {tool("in", "lucide:zoom-in", "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN", zoom >= RUN_ZOOM.max)}
      {tool("fit", "lucide:scan", "SCREEN_TEXT_FLOW_GRAPH_FIT", false)}
    </div>
    <div className="flow-graph-viewport" ref={viewport}>
      <div className="flow-graph-zoomed" style={{ width: layout.width * zoom, height: layout.height * zoom }}>
        <div className="flow-graph-canvas" role="group" aria-label={translate("SCREEN_TEXT_FLOW_GRAPH")}
          style={{ width: layout.width, height: layout.height, transform: `scale(${zoom})` }}>
          <svg className="flow-graph-edges" width={layout.width} height={layout.height} aria-hidden="true">
            <defs>
              {RUN_EDGE_KINDS.map(kind =>
                <marker key={kind} id={`${id}-run-arrow-${kind}`} className={`flow-graph-arrow is-${kind}`}
                  viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                  <path d="M 0 0 L 10 5 L 0 10 z" />
                </marker>)}
            </defs>
            {layout.edges.map(edge => <path key={`${edge.from}-${edge.to}`}
              className={`flow-graph-edge is-${getRunEdgeKind(edge)}`} d={edgePath(edge)}
              markerEnd={`url(#${id}-run-arrow-${getRunEdgeKind(edge)})`} />)}
          </svg>
          {layout.nodes.map(node => {
            const view = views.get(node.id);
            const pressed = selected === node.id;
            // A run node shows what happened in that run: it is never dimmed because the task is inactive today
            return <button key={node.id} type="button" ref={keep(node.id)}
            className={`flow-graph-node flow-graph-run-node is-${node.state} is-line-${view.line}`}
            style={{ left: node.x, top: node.y, width: RUN_GRAPH_SIZE.nodeWidth, height: RUN_GRAPH_SIZE.nodeHeight }}
            aria-label={view.label} aria-pressed={pressed} aria-disabled={view.selectable ? undefined : true}
            onClick={() => select(node.id, view.selectable)} {...testHook(TestIds.flowGraphNode)}>
              <span className="flow-graph-node-state">
                {getIconCode(view.icon)}
                <span className="flow-graph-node-state-text">{view.stateText}</span>
                {view.executionId && <span className="flow-graph-node-execution">{view.executionId}</span>}
              </span>
              <span className="flow-graph-node-name" title={node.name}>{node.name}</span>
              {view.timing && <span className="flow-graph-node-meta">{view.timing}</span>}
              {view.due && <span className="flow-graph-node-meta">{getIconCode("lucide:hourglass")}{view.due}</span>}
              {view.join && <span className="flow-graph-node-meta">{getIconCode("lucide:git-merge")}{view.join}</span>}
              {view.skip && <span className="flow-graph-node-meta" title={view.skip}>{view.skip}</span>}
            </button>;
          })}
          {layout.edges.map(edge => <span key={`${edge.from}-${edge.to}`}
            className={`flow-graph-condition flow-graph-run-condition is-${getRunEdgeKind(edge)}`}
            style={{ left: (edge.start.x + edge.end.x) / 2, top: (edge.start.y + edge.end.y) / 2 }}>
            {getRunEdgeText(edge, translate)}</span>)}
        </div>
      </div>
    </div>
    {legend}
    {live}
  </div>;
}

FlowRunGraph.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string,
  model: PropTypes.object.isRequired,
  address: PropTypes.object,
  target: PropTypes.string,
  hidden: PropTypes.bool,
  view: PropTypes.string
};

/**
 * Classes of a node of the dependency graph
 * @param {object} node Node of the layout
 * @param {boolean} hasState Whether the node shows its state
 * @returns {string} Classes
 */
const getNodeClassName = (node, hasState) =>
  `flow-graph-node${hasState ? ` is-${node.state}` : ""}${node.active ? "" : " is-inactive"}${node.role ? ` is-${node.role}` : ""}`;

/**
 * Contents of a node of the dependency graph: its launch icon, its name, its state, its next run and the other tasks
 * it also waits for (the last two only in the task graph)
 * @param {object} node Node of the layout
 * @param {object} options Detail identifier, whether it shows its state, state and inactive texts, task graph and
 * translation function
 * @returns {JSX.Element} Contents
 */
const renderNodeContent = (node, { detailId, hasState, stateText, inactiveLabel, task, t }) => <>
  <span className="flow-graph-node-icon" title={translateLabel(node.launchLabel, t)}>
    {getIconCode(node.launchIcon)}
  </span>
  <span className="flow-graph-node-text" id={detailId}>
    <span className="flow-graph-node-name" title={node.name}>{node.name}</span>
    {(stateText || inactiveLabel) && <span className="flow-graph-node-state">
      {inactiveLabel && <span className="flow-graph-node-tag">{inactiveLabel}</span>}
      {hasState && getIconCode(STATE_ICONS[node.state])}
      {stateText && <span className="flow-graph-node-state-text">{stateText}</span>}
    </span>}
    {task && node.nextRun && <span className="flow-graph-node-next">
      {getIconCode("lucide:clock")}<span className="flow-graph-node-state-text">{node.nextRun}</span>
    </span>}
    {task && node.alsoWaitsFor?.length > 0 && <span className="flow-graph-node-also"
      title={`${translateLabel("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS", t)}: ${node.alsoWaitsFor.map(parent => parent.name).join(", ")}`}>
      <span className="flow-graph-node-also-title">{translateLabel("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS", t)}</span>
      {node.alsoWaitsFor.map(parent => <span key={parent.id} className="flow-graph-node-also-name">{parent.name}</span>)}
    </span>}
  </span>
</>;

/**
 * Flow graph of the scheduler: the dependencies of the tasks (getFlowGraph query rows) and, for a run, the state of
 * each task. Clicking a node sends it as the selected task and opens the `target` screen of the widget parameters;
 * without a target the nodes are not buttons. With the `mode` parameter `task` it draws the one-level graph of the
 * root task (the task screen): the tasks it runs after, the task and the tasks it launches, in three titled columns,
 * with the condition of every edge as icons (its text as tooltip, a legend under the graph), the state of the last
 * execution of each task with when it started, the next execution of a scheduled one and, in a child, the other tasks
 * it also waits for. With a `target` the tasks it runs after and the ones it launches open that screen
 * (the task itself is not a link): the task of the node goes as the `criterion` the screen reads (the task screen reads
 * IdeTsk.selected) and, when the target is the screen of the graph, the screen is loaded again with it. An inactive
 * task is muted, with an «Inactive» tag. With the `mode` parameter `run` it draws one flow run (see FlowRunGraph), as
 * a graph, a timeline or a list by its `view` parameter (graph, timeline or list; graph by default).
 * @category Widgets
 */
function AweFlowGraph({ id, parameters, style = "" }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { model = { values: [] }, address, attributes = {} } = useComponentState(id);
  const { target, mode, criterion, side, view } = parameters || attributes.parameters || {};
  const values = model.values;
  const task = mode === "task";
  const runMode = mode === "run";
  // A side of the task (the task editor) draws two columns instead of three
  const titles = getTaskGraphTitles(side);
  const taskSize = titles.length === 2 ? SIDE_SIZE : TASK_SIZE;

  const { layout, run } = useMemo(() => {
    if (runMode) return { layout: { nodes: [] }, run: true };
    const graph = task ? buildTaskGraph(values, side) : buildFlowGraph(values);
    return { layout: layoutFlowGraph(graph.nodes, graph.edges, task ? taskSize : SIZE), run: graph.run };
  }, [values, task, side, taskSize, runMode]);

  if (runMode) {
    return <FlowRunGraph id={id} style={style} model={model} address={address} target={target}
      view={getRunView(view)} hidden={attributes.visible === false} />;
  }
  if (!layout.nodes.length) {
    const empty = task ? "SCREEN_TEXT_FLOW_GRAPH_NO_DEPENDENCIES" : "SCREEN_TEXT_FLOW_GRAPH_EMPTY";
    return <div className={`flow-graph flow-graph-empty ${style}`} id={id}>{translateLabel(empty, t)}</div>;
  }
  const size = task ? taskSize : SIZE;
  const withState = run || task;
  const conditionLabel = edge => task ? getConditionLabel(edge) : CONDITION_LABELS[edge.condition];
  const width = layout.width;
  // The canvas is a list of tasks, unless its tasks are buttons
  const canvasRole = target ? (task ? "group" : undefined) : "list";

  const openTask = (taskId) => {
    if (task) {
      // The task screen reads its task from its criterion. On the screen itself it is loaded again in its view with the
      // task of the node as that criterion (a reload of the route would reload the parent view too, and the sub screen
      // again with the values it had); another screen is opened with the criterion set
      if (target === ViewRegistry.get(address.view)?.option) {
        dispatch(loadScreen(address.view, target, t, false, criterion ? { [criterion]: taskId } : {}));
        return;
      }
      if (criterion) dispatch(updateModel({ view: address.view, component: criterion }, { selected: [taskId] }));
      dispatch(addActionsTop([{ type: "screen", target, address }]));
      return;
    }
    dispatch(updateModel(address, { selected: [taskId] }));
    if (target) dispatch(addActionsTop([{ type: "screen", target, address }]));
  };

  return <div className={`flow-graph${task ? " flow-graph-task" : ""} ${style}`} id={id}>
    <div className="flow-graph-canvas" style={{ width, height: layout.height }} role={canvasRole}
      aria-label={canvasRole ? translateLabel(task ? "SCREEN_TEXT_FLOW_GRAPH_TASK" : "SCREEN_TEXT_FLOW_GRAPH", t) : undefined}>
      {task && getTaskGraphColumns(layout, size, titles).map(column => <React.Fragment key={column.title}>
        <span className="flow-graph-column" aria-hidden="true"
          style={{ left: column.left, width: column.width }}>{translateLabel(column.title, t)}</span>
        {column.none && <span className="flow-graph-none" aria-hidden="true"
          style={{ left: column.left, top: column.none.top, width: column.width, height: column.none.height }}>
          {translateLabel(TASK_GRAPH_NONE, t)}</span>}
      </React.Fragment>)}
      <svg className="flow-graph-edges" width={width} height={layout.height} aria-hidden="true">
        <defs>
          {["ok", "warning", "error"].map(condition =>
            <marker key={condition} id={`${id}-arrow-${condition}`} className={`flow-graph-arrow is-${condition}`}
              viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" />
            </marker>)}
        </defs>
        {layout.edges.map(edge =>
          <path key={`${edge.from}-${edge.to}`} className={`flow-graph-edge is-${edge.condition}`} d={edgePath(edge)}
            markerEnd={`url(#${id}-arrow-${edge.condition})`} />)}
      </svg>
      {layout.nodes.map(node => {
        const hasState = withState && node.state;
        const stateLabel = hasState ? translateLabel(STATE_LABELS[node.state], t) : null;
        const stateText = stateLabel && node.lastWhen ? `${stateLabel} · ${node.lastWhen}` : stateLabel;
        const inactiveLabel = node.active ? null : translateLabel("SCREEN_TEXT_FLOW_GRAPH_INACTIVE", t);
        const roleLabel = node.role ? translateLabel(titles[node.layer], t) : null;
        const className = getNodeClassName(node, hasState);
        const detailId = `${id}-node-${node.id}`;
        const content = renderNodeContent(node, { detailId, hasState, stateText, inactiveLabel, task, t });
        const nodeStyle = { left: node.x, top: node.y, width: size.nodeWidth, height: size.nodeHeight };
        const label = [roleLabel, node.name, stateText, inactiveLabel, task ? node.nextRun : null].filter(Boolean).join(" · ");
        if (target && task && node.role !== "self") {
          return <button key={node.id} type="button" className={className} style={nodeStyle}
            aria-label={translateLabel("SCREEN_TEXT_FLOW_GRAPH_OPEN_TASK", t, node.name)} aria-describedby={detailId}
            onClick={() => openTask(node.id)} {...testHook(TestIds.flowGraphNode)}>{content}</button>;
        }
        if (target && !task) {
          return <button key={node.id} type="button" className={className} style={nodeStyle} aria-label={label}
            aria-pressed={model.selected?.[0] === node.id}
            onClick={() => openTask(node.id)} {...testHook(TestIds.flowGraphNode)}>{content}</button>;
        }
        return <div key={node.id} role={target ? "group" : "listitem"} className={className} style={nodeStyle}
          aria-label={label} {...testHook(TestIds.flowGraphNode)}>{content}</div>;
      })}
      {layout.edges.filter(conditionLabel).map(edge => {
        const position = { left: (edge.start.x + edge.end.x) / 2, top: (edge.start.y + edge.end.y) / 2 };
        if (!task) {
          return <span key={`${edge.from}-${edge.to}`} className={`flow-graph-condition is-${edge.condition}`}
            style={position}>{translateLabel(conditionLabel(edge), t)}</span>;
        }
        const text = translateLabel(conditionLabel(edge), t);
        return <span key={`${edge.from}-${edge.to}`} className="flow-graph-condition flow-graph-condition-icons"
          role="img" aria-label={text} title={text} style={position}>
          {getConditionIcons(edge).map(icon =>
            <span key={icon} className={`flow-graph-condition-icon is-${icon}`}>{getIconCode(CONDITION_ICONS[icon])}</span>)}
        </span>;
      })}
    </div>
    {task && <div className="flow-graph-legend">
      <span>{translateLabel("SCREEN_TEXT_FLOW_GRAPH_LEGEND", t)}</span>
      {Object.keys(CONDITION_ICONS).map(icon => <span key={icon} className="flow-graph-legend-item">
        <span className={`flow-graph-condition-icon is-${icon}`} aria-hidden="true">{getIconCode(CONDITION_ICONS[icon])}</span>
        {translateLabel(CONDITION_LEGEND[icon], t)}
      </span>)}
    </div>}
  </div>;
}

AweFlowGraph.propTypes = {
  id: PropTypes.string.isRequired,
  parameters: PropTypes.object,
  style: PropTypes.string
};

export default AweFlowGraph;
