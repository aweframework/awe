import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {getIcon} from "../services/icon";
import {TestIds} from "../data/testIds";
import {buildFlowGraph, layoutFlowGraph} from "awe-client-react/utilities/flowGraph";

// Geometry of the graph, in px (the layout positions the nodes with it): the same as the React flow graph.
// The gap between levels leaves room for the condition label of an edge
const SIZE = {nodeWidth: 208, nodeHeight: 56, gapX: 104, gapY: 20};

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

/**
 * Cubic curve from the right side of a parent to the left side of a child
 * @param {{start: {x: number, y: number}, end: {x: number, y: number}}} edge Positioned edge
 * @returns {string} Path
 */
const edgePath = ({start, end}) => {
  const bend = (end.x - start.x) / 2;
  return `M ${start.x} ${start.y} C ${start.x + bend} ${start.y}, ${end.x - bend} ${end.y}, ${end.x} ${end.y}`;
};

const template = `<div class="flow-graph {{::controller.style}}" ng-attr-id="{{::flowGraphId}}" ng-show="controller.visible" ui-dependency="dependencies" ng-class="{'flow-graph-empty': !view.nodes.length}" ng-cloak>
  <span ng-if="!view.nodes.length" ng-bind="view.empty"></span>
  <div class="flow-graph-canvas" ng-if="view.nodes.length" ng-style="{width: view.width + 'px', height: view.height + 'px'}">
    <svg class="flow-graph-edges" ng-attr-width="{{view.width}}" ng-attr-height="{{view.height}}" aria-hidden="true" focusable="false">
      <defs>
        <marker ng-repeat="condition in view.conditions" ng-attr-id="{{::flowGraphId}}-arrow-{{condition}}" class="flow-graph-arrow is-{{condition}}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z"></path></marker>
      </defs>
      <path ng-repeat="edge in view.edges track by edge.key" class="flow-graph-edge is-{{edge.condition}}" ng-attr-d="{{edge.path}}" ng-attr-marker-end="url(#{{::flowGraphId}}-arrow-{{edge.condition}})"></path>
    </svg>
    <button ng-repeat="node in view.nodes track by node.id" type="button" data-testid="${TestIds.flowGraphNode}" class="flow-graph-node {{node.classes}}" ng-style="node.position" ng-attr-aria-label="{{node.label}}" ng-attr-aria-pressed="{{node.selected}}" ng-click="open(node.id)">
      ${getIconTemplate("flow-graph-node-icon", "span", "node.icon", 'ng-attr-title="{{node.launchLabel}}"')}
      <span class="flow-graph-node-text">
        <span class="flow-graph-node-name" ng-bind="node.name"></span>
        <span class="flow-graph-node-state" ng-if="node.stateText">${getIconTemplate("", "span", "node.stateIcon")}<span ng-bind="node.stateText"></span></span>
      </span>
    </button>
    <span ng-repeat="edge in view.conditionLabels track by edge.key" class="flow-graph-condition is-{{edge.condition}}" ng-style="edge.position" ng-bind="edge.text"></span>
  </div>
</div>`;

// Flow graph directive (#992): the dependencies of the scheduler tasks (rows of the getFlowGraph query) and, for a run,
// the state of each task. Clicking a node sends it as the selected task and opens the "target" screen of the widget
// parameters. The layout is shared with the React engine (awe-client-react/utilities/flowGraph)
aweApplication.directive('aweFlowGraph',
  ['Component', 'Control', 'AweUtilities', 'ActionController', '$filter',
    function (Component, Control, Utilities, $actionController, $filter) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          flowGraphId: '@'
        },
        link: function (scope) {
          const component = new Component(scope, scope.flowGraphId);
          if (!component.asComponent()) {
            return false;
          }

          const translate = $filter('translateMultiple');
          const getSelected = () => Utilities.asArray(component.model.selected).map(String)[0];

          /**
           * Build what the template draws from the rows of the model
           */
          const render = () => {
            const graph = buildFlowGraph(component.model.values || []);
            const layout = layoutFlowGraph(graph.nodes, graph.edges, SIZE);
            const selected = getSelected();
            const edgeKey = edge => `${edge.from}-${edge.to}`;
            scope.view = {
              empty: translate("SCREEN_TEXT_FLOW_GRAPH_EMPTY"),
              width: layout.width,
              height: layout.height,
              conditions: CONDITIONS,
              edges: layout.edges.map(edge => ({key: edgeKey(edge), condition: edge.condition, path: edgePath(edge)})),
              conditionLabels: layout.edges.filter(edge => CONDITION_LABELS[edge.condition]).map(edge => ({
                key: edgeKey(edge), condition: edge.condition, text: translate(CONDITION_LABELS[edge.condition]),
                position: {left: (edge.start.x + edge.end.x) / 2 + "px", top: (edge.start.y + edge.end.y) / 2 + "px"}
              })),
              nodes: layout.nodes.map(node => {
                const stateLabel = graph.run ? translate(STATE_LABELS[node.state]) : null;
                const inactiveLabel = node.active ? null : translate("SCREEN_TEXT_FLOW_GRAPH_INACTIVE");
                return {
                  id: node.id,
                  name: node.name,
                  icon: getIcon(node.launchIcon),
                  launchLabel: node.launchLabel ? translate(node.launchLabel) : null,
                  stateIcon: getIcon(graph.run ? STATE_ICONS[node.state] : null),
                  stateText: stateLabel || inactiveLabel,
                  label: [node.name, stateLabel, inactiveLabel].filter(Boolean).join(" · "),
                  classes: `${graph.run ? `is-${node.state}` : ""} ${node.active ? "" : "is-inactive"}`.trim(),
                  selected: selected === node.id,
                  position: {left: node.x + "px", top: node.y + "px", width: SIZE.nodeWidth + "px", height: SIZE.nodeHeight + "px"}
                };
              })
            };
          };

          /**
           * Select a task and open its screen
           * @param {string} taskId Task identifier
           */
          scope.open = taskId => {
            Control.changeModelAttribute(component.address, {selected: [taskId]}, true);
            const target = component.controller.parameters?.target;
            if (target) {
              // The screen action reads its parameters without checking them: an action without them never runs
              $actionController.addActionList([{type: "screen", target, parameters: {}}], false,
                {address: component.address, context: component.context});
            }
          };

          // The selected task goes to the server like the selected row of a grid (the task grid sends IdeTsk.selected)
          component.api.getData = () => ({[`${component.id}.selected`]: Control.formatDataList(Utilities.asArray(component.model.selected))});
          // The fill and reload actions replace the model values (the generic API keeps only the selected ones)
          component.api.updateModelValues = data => Object.assign(component.model, data);

          scope.$watchGroup([() => component.model.values, () => component.model.selected], render);
          component.listeners["languageChanged"] = scope.$on("languageChanged", render);
          render();
          return true;
        }
      };
    }
  ]);
