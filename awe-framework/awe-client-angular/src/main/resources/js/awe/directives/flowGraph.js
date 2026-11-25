import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {getIcon} from "../services/icon";
import {TestIds} from "../data/testIds";
import {
  buildFlowGraph, buildTaskGraph, getConditionIcons, getConditionLabel, getFitZoom, getNextZoom, getRunEdgeKind, getRunEdgeText,
  getRunListView, getRunNodeView, getRunSelectedValues, getRunTimelineView, getRunView, getTaskGraphColumns,
  getTaskGraphTitles, layoutFlowGraph, RUN_EDGE_KINDS, RUN_GRAPH_SIZE, RUN_LEGEND, RUN_STATES, RUN_ZOOM, TASK_GRAPH_NONE
} from "awe-client-react/utilities/flowGraph";

// Geometry of the graph, in px (the layout positions the nodes with it): the same as the React flow graph.
// The gap between levels leaves room for the condition label of an edge
const SIZE = {nodeWidth: 208, nodeHeight: 56, gapX: 104, gapY: 20};
// The one-level graph of a task always has its three columns, leaves room above the nodes for their titles and between
// them for the condition of every edge; its nodes are taller, for a name in two lines, its state and its next execution
const TASK_SIZE = {nodeWidth: 240, nodeHeight: 92, gapX: 136, gapY: 20, top: 28, layers: 3};
// One side of a task (the task editor): two narrower columns, so the graph fits in a form column
const SIDE_SIZE = {nodeWidth: 220, nodeHeight: 92, gapX: 96, gapY: 20, top: 28, layers: 2};


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
const CONDITION_LABELS = {warning: "SCREEN_TEXT_FLOW_GRAPH_ON_WARNING", error: "SCREEN_TEXT_FLOW_GRAPH_ON_ERROR"};
const CONDITIONS = ["ok", "warning", "error"];
// Icons of the conditions of the one-level graph and their label in the legend
const CONDITION_ICONS = {ok: "fa-check", warning: "fa-exclamation-triangle", error: "fa-times"};
const CONDITION_LEGEND = {ok: "ENUM_STATUS_OK", warning: "ENUM_STATUS_WARNING", error: "ENUM_STATUS_ERROR"};

/**
 * Cubic curve from the right side of a parent to the left side of a child
 * @param {{start: {x: number, y: number}, end: {x: number, y: number}}} edge Positioned edge
 * @returns {string} Path
 */
const edgePath = ({start, end}) => {
  const bend = (end.x - start.x) / 2;
  return `M ${start.x} ${start.y} C ${start.x + bend} ${start.y}, ${end.x - bend} ${end.y}, ${end.x} ${end.y}`;
};

// What a node shows: the icon of its launch, its name, its inactive tag, its state (with when it started) and the next
// execution of a scheduled task
const nodeContent = `${getIconTemplate("flow-graph-node-icon", "span", "node.icon", 'ng-attr-title="{{node.launchLabel}}"')}
      <span class="flow-graph-node-text" ng-attr-id="{{node.detailId}}">
        <span class="flow-graph-node-name" ng-attr-title="{{node.name}}" ng-bind="node.name"></span>
        <span class="flow-graph-node-state" ng-if="node.stateText || node.inactiveText"><span class="flow-graph-node-tag" ng-if="node.inactiveText" ng-bind="node.inactiveText"></span>${getIconTemplate("", "span", "node.stateIcon")}<span class="flow-graph-node-state-text" ng-if="node.stateText" ng-bind="node.stateText"></span></span>
        <span class="flow-graph-node-next" ng-if="node.nextRun">${getIconTemplate("", "span", "view.nextIcon")}<span class="flow-graph-node-state-text" ng-bind="node.nextRun"></span></span>
        <span class="flow-graph-node-also" ng-if="node.alsoWaitsFor.length" ng-attr-title="{{node.alsoTitle}}"><span class="flow-graph-node-also-title" ng-bind="view.alsoTitle"></span><span ng-repeat="parent in node.alsoWaitsFor track by parent.id" class="flow-graph-node-also-name" ng-bind="parent.name"></span></span>
      </span>`;

// State of a task in the timeline and the list views of a run: its icon, its text and its execution
const runState = item => `<span class="flow-graph-node-state flow-graph-legend-state is-{{${item}.state}}">${getIconTemplate("", "span", `${item}.icon`)}<span class="flow-graph-node-state-text" ng-bind="${item}.stateText"></span><span class="flow-graph-node-execution" ng-if="${item}.executionId" ng-bind="${item}.executionId"></span></span>`;

// Grid lines, broken segments and «Now» of the timeline, under the bars of a row
const timelineMarks = `<span class="flow-graph-timeline-marks" aria-hidden="true"><span ng-repeat="tick in view.timeline.ticks track by $index" class="flow-graph-timeline-line" ng-style="{left: tick.x + '%'}"></span><span ng-repeat="item in view.timeline.breaks track by $index" class="flow-graph-timeline-break" ng-style="{left: item.x + '%', width: item.width + '%'}"></span><span ng-if="view.timeline.now" class="flow-graph-timeline-now" ng-style="{left: view.timeline.now.x + '%'}"></span></span>`;

// Timeline view of a run (Cronograma): one row per task and its bars on a broken time axis, «Now»; every bar of a task
// with an execution selects it like its node. Same markup as RunTimeline of the React AweFlowGraph
const runTimeline = `<div class="flow-graph-timeline" role="table" ng-attr-aria-label="{{view.timelineLabel}}">
    <div class="flow-graph-timeline-row flow-graph-timeline-head" role="row">
      <span class="flow-graph-timeline-task" role="columnheader" ng-bind="view.taskLabel"></span>
      <span class="flow-graph-timeline-axis" role="columnheader">${timelineMarks}<span ng-repeat="tick in view.timeline.ticks track by $index" class="flow-graph-timeline-tick" ng-style="{left: tick.x + '%'}" ng-bind="tick.text"></span><span ng-repeat="item in view.timeline.breaks track by $index" class="flow-graph-timeline-break-mark" aria-hidden="true" ng-style="{left: (item.x + item.width / 2) + '%'}">≈</span><span ng-if="view.timeline.now" class="flow-graph-timeline-now-label" ng-style="{left: view.timeline.now.x + '%'}" ng-bind="view.timeline.now.text"></span></span>
    </div>
    <div ng-repeat="row in view.timeline.rows track by row.id" class="flow-graph-timeline-row" role="row">
      <span class="flow-graph-timeline-task" role="rowheader"><span class="flow-graph-timeline-name" ng-attr-title="{{row.name}}" ng-bind="row.name"></span>${runState("row")}<span class="flow-graph-node-meta" ng-if="row.note" ng-attr-title="{{row.note}}" ng-bind="row.note"></span></span>
      <span class="flow-graph-timeline-track" role="cell">${timelineMarks}
        <span ng-if="row.wait" class="flow-graph-timeline-bar is-join-wait" role="img" ng-attr-aria-label="{{row.wait.label}}" ng-attr-title="{{row.wait.label}}" ng-style="{left: row.wait.x + '%', width: row.wait.width + '%'}" ng-bind="row.wait.text"></span>
        <button ng-repeat="bar in row.bars track by bar.key" type="button" data-testid="${TestIds.flowGraphNode}" data-node-id="{{row.id}}" class="flow-graph-timeline-bar is-{{bar.state}}" ng-class="{'is-open': bar.open}" ng-style="{left: bar.x + '%', width: bar.width + '%'}" ng-attr-aria-label="{{bar.label}}" ng-attr-title="{{bar.label}}" aria-pressed="{{row.selected}}" ng-attr-aria-disabled="{{row.selectable ? undefined : true}}" ng-click="select(row)" ng-bind="bar.text"></button>
        <span ng-if="row.due" class="flow-graph-timeline-due" aria-hidden="true" ng-style="{left: row.due.x + '%'}"><span ng-bind="row.due.text"></span></span>
      </span>
    </div>
  </div>`;

// One item of the list view of a run, with its children nested (the template includes itself) and its «also feeds»
// entries. Same markup as RunListItems of the React AweFlowGraph
const LIST_ITEM = "awe-flow-graph-run-list-item.html";
const listItem = `<button type="button" data-testid="${TestIds.flowGraphNode}" data-node-id="{{item.id}}" class="flow-graph-list-item is-{{item.state}} is-line-{{item.line}}" ng-attr-aria-label="{{item.label}}" aria-pressed="{{item.selected}}" ng-attr-aria-disabled="{{item.selectable ? undefined : true}}" ng-click="select(item)">
    ${runState("item")}
    <span class="flow-graph-list-name" ng-bind="item.name"></span>
    <span class="flow-graph-node-meta" ng-if="item.timing" ng-bind="item.timing"></span>
    <span class="flow-graph-node-meta" ng-if="item.due">${getIconTemplate("", "span", "view.dueIcon")}<span ng-bind="item.due"></span></span>
    <span class="flow-graph-node-meta" ng-if="item.join">${getIconTemplate("", "span", "view.joinIcon")}<span ng-bind="item.join"></span></span>
    <span class="flow-graph-node-meta" ng-if="item.skip" ng-bind="item.skip"></span>
    <span class="flow-graph-list-condition" ng-if="item.condition" ng-bind="item.condition"></span>
  </button>
  <ol class="flow-graph-list" ng-if="item.children.length || item.alsoFeeds.length">
    <li ng-repeat="item in item.children track by item.id" class="flow-graph-list-entry" ng-include="'${LIST_ITEM}'"></li>
    <li ng-repeat="feed in item.alsoFeeds track by feed.id" class="flow-graph-list-entry"><span class="flow-graph-list-feeds">${getIconTemplate("", "span", "view.joinIcon")}<span ng-bind="feed.text"></span></span></li>
  </ol>`;

// List view of a run (Lista): a nested ordered list from the root tasks
const runList = `<ol class="flow-graph-list" ng-attr-aria-label="{{view.listLabel}}"><li ng-repeat="item in view.list track by item.id" class="flow-graph-list-entry" ng-include="'${LIST_ITEM}'"></li></ol>`;

// Run mode (the flow run page): the zoom toolbar, the scaled graph with its nodes and edges by state (or the timeline,
// or the list, by the view parameter), the legend and the announcement of the selected node. Same markup as the React
// run graph (FlowRunGraph of AweFlowGraph)
const runContent = `<div class="flow-graph-run-content" ng-if="view.mode === 'timeline'">${runTimeline}</div>
  <div class="flow-graph-run-content" ng-if="view.mode === 'list'">${runList}</div>
  <div class="flow-graph-run-content" ng-if="view.mode === 'graph'"><div class="flow-graph-toolbar" role="group" ng-attr-aria-label="{{view.zoomLabel}}">
    <button ng-repeat="tool in view.tools track by tool.action" type="button" class="flow-graph-tool" ng-attr-aria-label="{{tool.label}}" ng-attr-title="{{tool.label}}" ng-disabled="tool.disabled" ng-click="changeZoom(tool.action)">${getIconTemplate("", "span", "tool.icon")}</button>
    <span class="flow-graph-zoom" ng-bind="view.zoomText"></span>
  </div>
  <div class="flow-graph-viewport">
    <div class="flow-graph-zoomed" ng-style="{width: view.width * view.zoom + 'px', height: view.height * view.zoom + 'px'}">
      <div class="flow-graph-canvas" role="group" ng-attr-aria-label="{{view.label}}" ng-style="{width: view.width + 'px', height: view.height + 'px', transform: 'scale(' + view.zoom + ')'}">
        <svg class="flow-graph-edges" ng-attr-width="{{view.width}}" ng-attr-height="{{view.height}}" aria-hidden="true" focusable="false">
          <defs>
            <marker ng-repeat="kind in view.edgeKinds" ng-attr-id="{{::flowGraphId}}-run-arrow-{{kind}}" class="flow-graph-arrow is-{{kind}}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z"></path></marker>
          </defs>
          <path ng-repeat="edge in view.edges track by edge.key" class="flow-graph-edge is-{{edge.kind}}" ng-attr-d="{{edge.path}}" ng-attr-marker-end="url(#{{::flowGraphId}}-run-arrow-{{edge.kind}})"></path>
        </svg>
        <button ng-repeat="node in view.nodes track by node.id" type="button" data-testid="${TestIds.flowGraphNode}" data-node-id="{{node.id}}" class="flow-graph-node flow-graph-run-node {{node.classes}}" ng-style="node.position" ng-attr-aria-label="{{node.label}}" aria-pressed="{{node.selected}}" ng-attr-aria-disabled="{{node.selectable ? undefined : true}}" ng-click="select(node)">
          <span class="flow-graph-node-state">${getIconTemplate("", "span", "node.icon")}<span class="flow-graph-node-state-text" ng-bind="node.stateText"></span><span class="flow-graph-node-execution" ng-if="node.executionId" ng-bind="node.executionId"></span></span>
          <span class="flow-graph-node-name" ng-attr-title="{{node.name}}" ng-bind="node.name"></span>
          <span class="flow-graph-node-meta" ng-if="node.timing" ng-bind="node.timing"></span>
          <span class="flow-graph-node-meta" ng-if="node.due">${getIconTemplate("", "span", "view.dueIcon")}<span ng-bind="node.due"></span></span>
          <span class="flow-graph-node-meta" ng-if="node.join">${getIconTemplate("", "span", "view.joinIcon")}<span ng-bind="node.join"></span></span>
          <span class="flow-graph-node-meta" ng-if="node.skip" ng-attr-title="{{node.skip}}" ng-bind="node.skip"></span>
        </button>
        <span ng-repeat="edge in view.edges track by edge.key" class="flow-graph-condition flow-graph-run-condition is-{{edge.kind}}" ng-style="edge.position" ng-bind="edge.text"></span>
      </div>
    </div>
  </div></div>
  <div class="flow-graph-legend flow-graph-run-legend">
    <span class="flow-graph-legend-title" ng-bind="view.legendStates"></span>
    <span ng-repeat="item in view.legendItems track by item.name" class="flow-graph-legend-item flow-graph-legend-state is-{{item.name}}"><span aria-hidden="true">${getIconTemplate("", "span", "item.icon")}</span><span ng-bind="item.text"></span></span>
    <span class="flow-graph-legend-title" ng-if="view.mode === 'graph'" ng-bind="view.legendEdges"></span>
    <span ng-repeat="item in view.legendEdgeItems track by item.name" ng-if="view.mode === 'graph'" class="flow-graph-legend-item"><span class="flow-graph-legend-edge is-{{item.name}}" aria-hidden="true"></span><span ng-bind="item.text"></span></span>
    <span class="flow-graph-legend-item" ng-if="view.mode === 'timeline'"><span class="flow-graph-legend-bar is-join-wait" aria-hidden="true"></span><span ng-bind="view.legendJoinWait"></span></span>
  </div>
  <span class="flow-graph-live" aria-live="polite" ng-bind="view.announcement"></span>`;

const template = `<div class="flow-graph {{::controller.style}}" ng-attr-id="{{::flowGraphId}}" ng-show="controller.visible" ui-dependency="dependencies" ng-class="{'flow-graph-empty': !view.nodes.length, 'flow-graph-task': view.task, 'flow-graph-run': view.run && view.nodes.length, 'flow-graph-run-timeline': view.run && view.mode === 'timeline', 'flow-graph-run-list': view.run && view.mode === 'list'}" ng-keydown="keydown($event)" ng-cloak>
  <span ng-if="!view.nodes.length" ng-bind="view.empty"></span>
  <div class="flow-graph-canvas" ng-if="view.nodes.length && !view.run" ng-style="{width: view.width + 'px', height: view.height + 'px'}" ng-attr-role="{{view.role}}" ng-attr-aria-label="{{view.role ? view.label : undefined}}">
    <span ng-repeat="column in view.columns track by $index" class="flow-graph-column" aria-hidden="true" ng-style="column.position" ng-bind="column.text"></span>
    <span ng-repeat="column in view.columns track by $index" ng-if="column.empty" class="flow-graph-none" aria-hidden="true" ng-style="column.nonePosition" ng-bind="view.none"></span>
    <svg class="flow-graph-edges" ng-attr-width="{{view.width}}" ng-attr-height="{{view.height}}" aria-hidden="true" focusable="false">
      <defs>
        <marker ng-repeat="condition in view.conditions" ng-attr-id="{{::flowGraphId}}-arrow-{{condition}}" class="flow-graph-arrow is-{{condition}}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z"></path></marker>
      </defs>
      <path ng-repeat="edge in view.edges track by edge.key" class="flow-graph-edge is-{{edge.condition}}" ng-attr-d="{{edge.path}}" ng-attr-marker-end="url(#{{::flowGraphId}}-arrow-{{edge.condition}})"></path>
    </svg>
    <button ng-repeat="node in view.links track by node.id" type="button" data-testid="${TestIds.flowGraphNode}" class="flow-graph-node {{node.classes}}" ng-style="node.position" ng-attr-aria-label="{{node.label}}" ng-attr-aria-pressed="{{node.selected}}" ng-attr-aria-describedby="{{node.describedBy}}" ng-click="open(node.id)">
      ${nodeContent}
    </button>
    <div ng-repeat="node in view.boxes track by node.id" ng-attr-role="{{node.role}}" data-testid="${TestIds.flowGraphNode}" class="flow-graph-node {{node.classes}}" ng-style="node.position" ng-attr-aria-label="{{node.label}}">
      ${nodeContent}
    </div>
    <span ng-repeat="edge in view.conditionLabels track by edge.key" ng-if="!edge.icons" class="flow-graph-condition is-{{edge.condition}}" ng-style="edge.position" ng-bind="edge.text"></span>
    <span ng-repeat="edge in view.conditionLabels track by edge.key" ng-if="edge.icons" class="flow-graph-condition flow-graph-condition-icons" role="img" ng-attr-aria-label="{{edge.text}}" ng-attr-title="{{edge.text}}" ng-style="edge.position"><span ng-repeat="icon in edge.icons track by icon.name" class="flow-graph-condition-icon is-{{icon.name}}">${getIconTemplate("", "span", "icon.icon")}</span></span>
  </div>
  <div class="flow-graph-legend" ng-if="view.task && view.nodes.length && !view.run"><span ng-bind="view.legend"></span><span ng-repeat="item in view.legendItems track by item.name" class="flow-graph-legend-item"><span class="flow-graph-condition-icon is-{{item.name}}" aria-hidden="true">${getIconTemplate("", "span", "item.icon")}</span><span ng-bind="item.text"></span></span></div>
  <div class="flow-graph-run-content" ng-if="view.run && view.nodes.length">${runContent}</div>
</div>`;

// Flow graph directive (#992): the dependencies of the scheduler tasks (rows of the getFlowGraph query) and, for a run,
// the state of each task. Clicking a node sends it as the selected task and opens the "target" screen of the widget
// parameters; without a target the nodes are not buttons. With the "mode" parameter "task" it draws the one-level
// graph of the root task (the task screen, #993): the tasks it runs after, the task and the tasks it launches, in three
// titled columns, with the condition of every edge as icons (its text as tooltip, a legend under the graph), the state
// of the last execution of each task with when it started, the next execution of a scheduled one and, in a child, the
// other tasks it also waits for. With a target the tasks it runs after and the ones it launches open that
// screen (the task itself is not a link): the task of the node goes to the "criterion" of the screen (the task screen
// reads it) and the screen is opened again. An inactive task is muted, with an «Inactive» tag. With the "mode" parameter
// "run" it draws one flow run (the flow run page, #993): every task with its state, execution, times, due date and the
// join it closed, every edge by its state in the run; a node with an execution selects itself (click, Enter, Space),
// raises the click event of the widget (its dependencies launch their actions) and sends the columns of its row as
// <column>.selected, like a grid row (IdeTsk.selected, ExeTsk.selected); it stays in view and is announced, Escape
// clears the selection and a toolbar zooms in, out and fits the graph to its width; the graph starts at the zoom that
// fits it in its viewport (getFitZoom), measured the first time it is shown. The "view" parameter draws the same
// run as a timeline ("timeline": one row per task, its bars on a broken time axis, «Now») or as a nested list ("list":
// a join under the parent that closed it, «also feeds» under the others); their bars and items select the task like its
// node. The layout and the texts are shared with the React engine (awe-client-react/utilities/flowGraph)
aweApplication.directive('aweFlowGraph',
  ['Component', 'Control', 'AweUtilities', 'ActionController', '$filter', '$templateCache',
    function (Component, Control, Utilities, $actionController, $filter, $templateCache) {
      // The list view includes this template in every item, for its children
      $templateCache.put(LIST_ITEM, listItem);
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          flowGraphId: '@'
        },
        link: function (scope, element) {
          const component = new Component(scope, scope.flowGraphId);
          if (!component.asComponent()) {
            return false;
          }

          const translate = $filter('translateMultiple');
          const getSelected = () => Utilities.asArray(component.model.selected).map(String)[0];
          const isRun = () => (component.controller.parameters || {}).mode === "run";
          // Translate a locale key and put its arguments in its {0}, {1}... placeholders
          const translateWith = (key, ...args) => args.reduce((text, arg, index) => text.replace(`{${index}}`, arg),
            translate(key));
          let zoom = 1;
          let scrolled = null;
          // The graph starts at the zoom that fits it once, when its viewport has a size (it is shown)
          let fitted = false;
          const fitZoom = () => {
            const viewport = element[0].querySelector(".flow-graph-viewport");
            const fit = fitted || !viewport ? null : getFitZoom(scope.view,
              {width: viewport.clientWidth, height: viewport.clientHeight});
            if (fit === null) {
              return;
            }
            fitted = true;
            zoom = fit;
            renderRun();
            scope.$digest();
          };

          /**
           * Build what the template draws for one flow run (run mode)
           */
          const renderRun = () => {
            const graph = buildFlowGraph(component.model.values || []);
            const layout = layoutFlowGraph(graph.nodes, graph.edges, RUN_GRAPH_SIZE);
            const selected = getSelected();
            const mode = getRunView((component.controller.parameters || {}).view);
            // A row of the timeline or an item of the list with its icon and whether it is the selected task
            const withSelection = item => ({...item, icon: getIcon(item.icon), selected: selected === item.id});
            const listItemView = item => ({...withSelection(item), children: item.children.map(listItemView)});
            const timeline = mode === "timeline" ? getRunTimelineView(graph, translateWith) : null;
            const nodes = layout.nodes.map(node => {
              const view = getRunNodeView(node, graph, translateWith);
              return {
                ...view, id: node.id, name: node.name, icon: getIcon(view.icon),
                selected: selected === node.id,
                // A run node shows what happened in that run: it is never dimmed because the task is inactive today
                classes: `is-${node.state} is-line-${view.line}`,
                position: {left: node.x + "px", top: node.y + "px", width: RUN_GRAPH_SIZE.nodeWidth + "px",
                  height: RUN_GRAPH_SIZE.nodeHeight + "px"}
              };
            });
            const selectedNode = nodes.find(node => node.selected);
            const tool = (action, icon, label, disabled) => ({action, icon: getIcon(icon), label: translate(label), disabled});
            scope.view = {
              run: true,
              mode,
              timeline: timeline ? {...timeline, rows: timeline.rows.map(withSelection)} : null,
              list: mode === "list" ? getRunListView(graph, translateWith).map(listItemView) : [],
              timelineLabel: translate("SCREEN_TEXT_FLOW_RUN_TIMELINE"),
              listLabel: translate("SCREEN_TEXT_FLOW_RUN_LIST"),
              taskLabel: translate("SCREEN_TEXT_FLOW_RUN_TASK"),
              legendJoinWait: translate("SCREEN_TEXT_FLOW_RUN_LEGEND_JOIN_WAIT"),
              empty: translate("SCREEN_TEXT_FLOW_GRAPH_EMPTY"),
              label: translate("SCREEN_TEXT_FLOW_GRAPH"),
              width: layout.width,
              height: layout.height,
              zoom,
              zoomText: `${Math.round(zoom * 100)} %`,
              zoomLabel: translate("SCREEN_TEXT_FLOW_GRAPH_ZOOM"),
              tools: [tool("out", "lucide:zoom-out", "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT", zoom <= RUN_ZOOM.min),
                tool("in", "lucide:zoom-in", "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN", zoom >= RUN_ZOOM.max),
                tool("fit", "lucide:scan", "SCREEN_TEXT_FLOW_GRAPH_FIT", false)],
              dueIcon: getIcon("lucide:hourglass"),
              joinIcon: getIcon("lucide:git-merge"),
              edgeKinds: RUN_EDGE_KINDS,
              edges: layout.edges.map(edge => ({
                key: `${edge.from}-${edge.to}`, kind: getRunEdgeKind(edge), path: edgePath(edge),
                text: getRunEdgeText(edge, translateWith),
                position: {left: (edge.start.x + edge.end.x) / 2 + "px", top: (edge.start.y + edge.end.y) / 2 + "px"}
              })),
              nodes,
              legendStates: translate("SCREEN_TEXT_FLOW_RUN_LEGEND_STATES"),
              legendItems: RUN_LEGEND.states.map(name => ({name, icon: getIcon(RUN_STATES[name].icon),
                text: translate(RUN_STATES[name].label)})),
              legendEdges: translate("SCREEN_TEXT_FLOW_RUN_LEGEND_EDGES"),
              legendEdgeItems: RUN_EDGE_KINDS.map(name => ({name, text: translate(RUN_LEGEND.edges[name])})),
              announcement: selectedNode ? translateWith("SCREEN_TEXT_FLOW_GRAPH_SELECTED", selectedNode.label) : ""
            };
            // The selected node stays in view (the side panel it opens covers part of the page)
            if (selected && selected !== scrolled) {
              scope.$$postDigest(() => {
                const node = element[0].querySelector(`[data-node-id="${selected}"]`);
                if (node && node.scrollIntoView) node.scrollIntoView({block: "nearest", inline: "nearest"});
              });
            }
            scrolled = selected || null;
            if (!fitted && mode === "graph" && layout.nodes.length > 0) {
              scope.$$postDigest(fitZoom);
            }
          };

          /**
           * State of a node of the graph: whether it shows one and its text (with its last run)
           * @param {object} node Node of the layout
           * @param {boolean} withState Whether the graph shows the states (a run or the task graph)
           * @returns {{hasState: boolean, text: string|null}} State of the node
           */
          const getNodeState = (node, withState) => {
            const hasState = withState && !!node.state;
            const stateLabel = hasState ? translate(STATE_LABELS[node.state]) : null;
            return {hasState, text: stateLabel && node.lastWhen ? `${stateLabel} · ${node.lastWhen}` : stateLabel};
          };

          /**
           * Parts of a node that only the task graph draws: its next run and the other parents it waits for
           * @param {object} node Node of the layout
           * @param {boolean} task Whether it is the task graph
           * @returns {{nextRun: string|null, alsoWaitsFor: object[], alsoTitle: string|null}} Task parts
           */
          const getNodeTaskParts = (node, task) => {
            if (!task) {
              return {nextRun: null, alsoWaitsFor: [], alsoTitle: null};
            }
            return {
              nextRun: node.nextRun ? node.nextRun : null,
              alsoWaitsFor: node.alsoWaitsFor || [],
              alsoTitle: node.alsoWaitsFor?.length
                ? `${translate("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS")}: ${node.alsoWaitsFor.map(parent => parent.name).join(", ")}` : null
            };
          };

          /**
           * Classes of a node of the graph
           * @param {object} node Node of the layout
           * @param {boolean} hasState Whether the node shows its state
           * @returns {string} Classes
           */
          const getNodeClasses = (node, hasState) =>
            `${hasState ? `is-${node.state}` : ""} ${node.active ? "" : "is-inactive"} ${node.role ? `is-${node.role}` : ""}`.replace(/\s+/g, " ").trim();

          /**
           * Build what the template draws from the rows of the model
           */
          const render = () => {
            if (isRun()) {
              renderRun();
              return;
            }
            const parameters = component.controller.parameters || {};
            const task = parameters.mode === "task";
            // A side of the task (the task editor) draws two columns instead of three
            const titles = getTaskGraphTitles(parameters.side);
            const size = task ? (titles.length === 2 ? SIDE_SIZE : TASK_SIZE) : SIZE;
            const graph = task ? buildTaskGraph(component.model.values || [], parameters.side)
              : buildFlowGraph(component.model.values || []);
            const layout = layoutFlowGraph(graph.nodes, graph.edges, size);
            const selected = getSelected();
            const edgeKey = edge => `${edge.from}-${edge.to}`;
            const conditionLabel = edge => task ? getConditionLabel(edge) : CONDITION_LABELS[edge.condition];
            const target = parameters.target;
            const nodes = layout.nodes.map(node => {
              const {hasState, text: stateText} = getNodeState(node, graph.run || task);
              const inactiveLabel = node.active ? null : translate("SCREEN_TEXT_FLOW_GRAPH_INACTIVE");
              const roleLabel = node.role ? translate(titles[node.layer]) : null;
              const {nextRun, alsoWaitsFor, alsoTitle} = getNodeTaskParts(node, task);
              // The parents and children of the task screen open their task; in the other modes every node with a target
              const link = !!target && (!task || node.role !== "self");
              const openTask = link && task;
              const detailId = `${scope.flowGraphId}-node-${node.id}`;
              return {
                id: node.id,
                name: node.name,
                link,
                role: target ? "group" : "listitem",
                icon: getIcon(node.launchIcon),
                launchLabel: node.launchLabel ? translate(node.launchLabel) : null,
                stateIcon: getIcon(hasState ? STATE_ICONS[node.state] : null),
                stateText,
                inactiveText: inactiveLabel,
                nextRun,
                alsoWaitsFor,
                alsoTitle,
                detailId,
                describedBy: openTask ? detailId : undefined,
                label: openTask ? translate("SCREEN_TEXT_FLOW_GRAPH_OPEN_TASK").replace("{0}", node.name)
                  : [roleLabel, node.name, stateText, inactiveLabel, nextRun].filter(Boolean).join(" · "),
                classes: getNodeClasses(node, hasState),
                selected: link && !task ? selected === node.id : undefined,
                position: {left: node.x + "px", top: node.y + "px", width: size.nodeWidth + "px", height: size.nodeHeight + "px"}
              };
            });
            scope.view = {
              task,
              role: target ? (task ? "group" : undefined) : "list",
              label: translate(task ? "SCREEN_TEXT_FLOW_GRAPH_TASK" : "SCREEN_TEXT_FLOW_GRAPH"),
              empty: translate(task ? "SCREEN_TEXT_FLOW_GRAPH_NO_DEPENDENCIES" : "SCREEN_TEXT_FLOW_GRAPH_EMPTY"),
              width: layout.width,
              height: layout.height,
              none: translate(TASK_GRAPH_NONE),
              nextIcon: getIcon("lucide:clock"),
              columns: task && layout.nodes.length ? getTaskGraphColumns(layout, size, titles).map(column => ({
                text: translate(column.title), empty: !!column.none,
                position: {left: column.left + "px", width: column.width + "px"},
                nonePosition: column.none ? {left: column.left + "px", top: column.none.top + "px", width: column.width + "px",
                  height: column.none.height + "px"} : null
              })) : [],
              conditions: CONDITIONS,
              edges: layout.edges.map(edge => ({key: edgeKey(edge), condition: edge.condition, path: edgePath(edge)})),
              conditionLabels: layout.edges.filter(conditionLabel).map(edge => ({
                key: edgeKey(edge), condition: edge.condition, text: translate(conditionLabel(edge)),
                icons: task ? getConditionIcons(edge).map(name => ({name, icon: getIcon(CONDITION_ICONS[name])})) : null,
                position: {left: (edge.start.x + edge.end.x) / 2 + "px", top: (edge.start.y + edge.end.y) / 2 + "px"}
              })),
              nodes,
              links: nodes.filter(node => node.link),
              boxes: nodes.filter(node => !node.link),
              alsoTitle: translate("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS"),
              legend: translate("SCREEN_TEXT_FLOW_GRAPH_LEGEND"),
              legendItems: CONDITIONS.map(name => ({name, icon: getIcon(CONDITION_ICONS[name]), text: translate(CONDITION_LEGEND[name])}))
            };
          };

          /**
           * Select a task and open its screen
           * @param {string} taskId Task identifier
           */
          scope.open = taskId => {
            const parameters = component.controller.parameters || {};
            if (parameters.mode === "task") {
              // The task screen reads its task from its criterion: the node gives it and opens the screen again
              if (parameters.criterion) {
                Control.changeModelAttribute({view: component.address.view, component: parameters.criterion},
                  {selected: [taskId]}, true);
              }
              $actionController.addActionList([{type: "screen", target: parameters.target, parameters: {reload: true}}],
                false, {address: component.address, context: component.context});
              return;
            }
            Control.changeModelAttribute(component.address, {selected: [taskId]}, true);
            const target = parameters.target;
            if (target) {
              // The screen action reads its parameters without checking them: an action without them never runs
              $actionController.addActionList([{type: "screen", target, parameters: {}}], false,
                {address: component.address, context: component.context});
            }
          };

          /**
           * Select a node of a run (run mode) and launch the actions of the widget: the dependencies on its click event.
           * A node without execution (pending, skipped) opens nothing
           * @param {object} node Node of the view
           */
          scope.select = node => {
            if (!node.selectable) {
              return;
            }
            Control.changeModelAttribute(component.address, {selected: [node.id]}, true);
            component.storeEvent("click");
            const target = (component.controller.parameters || {}).target;
            if (target) {
              $actionController.addActionList([{type: "screen", target, parameters: {}}], false,
                {address: component.address, context: component.context});
            }
          };

          /**
           * Escape clears the selected node of a run
           * @param {Event} event Key event
           */
          scope.keydown = event => {
            if (isRun() && event.key === "Escape" && getSelected() !== undefined) {
              Control.changeModelAttribute(component.address, {selected: []}, true);
            }
          };

          /**
           * Zoom the run graph a step in or out, or fit it to the width it has
           * @param {string} action in, out or fit
           */
          scope.changeZoom = action => {
            const viewport = element[0].querySelector(".flow-graph-viewport");
            zoom = getNextZoom(zoom, action, {available: viewport ? viewport.clientWidth : 0, width: scope.view.width});
            render();
          };

          // The selected task goes to the server like the selected row of a grid (the task grid sends IdeTsk.selected);
          // in run mode also every column of the row of the node as <column>.selected (IdeTsk.selected, ExeTsk.selected)
          component.api.getData = () => ({
            ...(isRun() ? getRunSelectedValues(component.model.values || [], getSelected()) : {}),
            [`${component.id}.selected`]: Control.formatDataList(Utilities.asArray(component.model.selected))
          });
          // The fill and reload actions replace the model values (the generic API keeps only the selected ones)
          component.api.updateModelValues = data => Object.assign(component.model, data);

          // The visibility too: a graph hidden by a dependency is fitted when it is shown
          scope.$watchGroup([() => component.model.values, () => component.model.selected,
            () => component.controller.visible], render);
          component.listeners["languageChanged"] = scope.$on("languageChanged", render);
          render();
          return true;
        }
      };
    }
  ]);
