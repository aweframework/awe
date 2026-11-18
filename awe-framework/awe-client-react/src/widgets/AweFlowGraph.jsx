import React, { useMemo } from "react";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";
import { useComponentState } from "../hooks/useComponentState";
import { updateModel } from "../redux/actions/components";
import { addActionsTop } from "../redux/actions/actions";
import { getIconCode, translateLabel } from "../utilities";
import { buildFlowGraph, layoutFlowGraph } from "../utilities/flowGraph";
import { TestIds, testHook } from "../utilities/testIds";
import "./AweFlowGraph.less";

// Geometry of the graph, in SVG user units (the layout positions the nodes with them)
// The gap between levels leaves room for the condition label of an edge
const SIZE = { nodeWidth: 208, nodeHeight: 56, gapX: 104, gapY: 20 };

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

/**
 * Cubic curve from the right side of a parent to the left side of a child
 */
const edgePath = ({ start, end }) => {
  const bend = (end.x - start.x) / 2;
  return `M ${start.x} ${start.y} C ${start.x + bend} ${start.y}, ${end.x - bend} ${end.y}, ${end.x} ${end.y}`;
};

/**
 * Flow graph of the scheduler: the dependencies of the tasks (getFlowGraph query rows) and, for a run, the state of
 * each task. Clicking a node sends it as the selected task and opens the `target` screen of the widget parameters.
 * @category Widgets
 */
function AweFlowGraph({ id, parameters, style = "" }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { model = { values: [] }, address, attributes = {} } = useComponentState(id);
  const { target } = parameters || attributes.parameters || {};
  const values = model.values;

  const { layout, run } = useMemo(() => {
    const graph = buildFlowGraph(values);
    return { layout: layoutFlowGraph(graph.nodes, graph.edges, SIZE), run: graph.run };
  }, [values]);

  if (!layout.nodes.length) {
    return <div className={`flow-graph flow-graph-empty ${style}`} id={id}>{translateLabel("SCREEN_TEXT_FLOW_GRAPH_EMPTY", t)}</div>;
  }

  const openTask = (taskId) => {
    dispatch(updateModel(address, { selected: [taskId] }));
    if (target) dispatch(addActionsTop([{ type: "screen", target, address }]));
  };

  return <div className={`flow-graph ${style}`} id={id}>
    <div className="flow-graph-canvas" style={{ width: layout.width, height: layout.height }}>
      <svg className="flow-graph-edges" width={layout.width} height={layout.height} aria-hidden="true">
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
        const stateLabel = run ? translateLabel(STATE_LABELS[node.state], t) : null;
        const inactiveLabel = node.active ? null : translateLabel("SCREEN_TEXT_FLOW_GRAPH_INACTIVE", t);
        return <button key={node.id} type="button"
          className={`flow-graph-node${run ? ` is-${node.state}` : ""}${node.active ? "" : " is-inactive"}`}
          style={{ left: node.x, top: node.y, width: SIZE.nodeWidth, height: SIZE.nodeHeight }}
          aria-label={[node.name, stateLabel, inactiveLabel].filter(Boolean).join(" · ")}
          aria-pressed={model.selected?.[0] === node.id}
          onClick={() => openTask(node.id)} {...testHook(TestIds.flowGraphNode)}>
          <span className="flow-graph-node-icon" title={translateLabel(node.launchLabel, t)}>
            {getIconCode(node.launchIcon)}
          </span>
          <span className="flow-graph-node-text">
            <span className="flow-graph-node-name">{node.name}</span>
            {(stateLabel || inactiveLabel) && <span className="flow-graph-node-state">
              {run && getIconCode(STATE_ICONS[node.state])}{stateLabel || inactiveLabel}
            </span>}
          </span>
        </button>;
      })}
      {layout.edges.filter(edge => CONDITION_LABELS[edge.condition]).map(edge =>
        <span key={`${edge.from}-${edge.to}`} className={`flow-graph-condition is-${edge.condition}`}
          style={{ left: (edge.start.x + edge.end.x) / 2, top: (edge.start.y + edge.end.y) / 2 }}>
          {translateLabel(CONDITION_LABELS[edge.condition], t)}
        </span>)}
    </div>
  </div>;
}

AweFlowGraph.propTypes = {
  id: PropTypes.string.isRequired,
  parameters: PropTypes.object,
  style: PropTypes.string
};

export default AweFlowGraph;
