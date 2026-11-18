import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// Rows of the getFlowGraph query: one per task and parent
const row = (taskId, parentId, extra = {}) => ({id: `${taskId}-${parentId}`, taskId, parentId, name: `Task ${taskId}`, ...extra});

describe("awe-client-angular/src/test/jest/directives/flowGraph.test.js", () => {
  let $compile;
  let $rootScope;
  let model;
  let control;
  let actionController;
  let controller;

  beforeEach(() => {
    controller = {visible: true, parameters: {target: "update-scheduler-task"}};
    model = {values: [], selected: null};
    control = {
      changeModelAttribute: jest.fn((address, attributes) => Object.assign(model, attributes)),
      formatDataList: jest.fn(list => (list.length > 1 ? list : list[0] ?? null))
    };
    actionController = {defineActionListeners: jest.fn(), addActionList: jest.fn()};

    function ComponentMock(scope, id) {
      const component = {
        address: {view: "report", component: id}, api: {}, controller, id, listeners: {}, model, scope,
        asComponent: jest.fn(() => true)
      };
      scope.component = component;
      scope.controller = controller;
      return component;
    }

    angular.mock.module("aweApplication", {Component: ComponentMock, Control: control, ActionController: actionController});
    inject(["$compile", "$httpBackend", "$rootScope", (_$compile_, $httpBackend, _$rootScope_) => {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  /**
   * Compile the directive as a screen does (the server renders the flow-graph tag)
   * @returns {{root: Element, component: object}} Rendered graph
   */
  function compileGraph() {
    const element = $compile('<awe-flow-graph flow-graph-id="IdeTsk"></awe-flow-graph>')($rootScope.$new());
    $rootScope.$digest();
    return {root: element[0], component: element.isolateScope().component};
  }

  it("draws every task of the run with its state", () => {
    model.values = [
      row(1, null, {rootRun: "run", executionStatus: 0}),
      row(2, 1, {runStatus: "LAUNCHED", executionStatus: 0}),
      row(3, 1, {runStatus: "LAUNCHED", executionStatus: 1}),
      row(4, 2, {runStatus: "SKIPPED"}), row(4, 3, {runStatus: "SKIPPED"})
    ];
    const {root} = compileGraph();
    const labels = [...root.querySelectorAll("button")].map(button => button.getAttribute("aria-label"));

    expect(labels).toEqual(["Task 1 · ENUM_STATUS_OK", "Task 2 · ENUM_STATUS_OK", "Task 3 · ENUM_STATUS_ERROR",
      "Task 4 · SCREEN_TEXT_FLOW_GRAPH_SKIPPED"]);
    expect(root.querySelectorAll("path.flow-graph-edge")).toHaveLength(4);
  });

  it("opens the task of a node sending it as the selected task", () => {
    model.values = [row(1, null), row(2, 1)];
    const {root, component} = compileGraph();

    root.querySelectorAll("button")[1].click();

    expect(control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["2"]}, true);
    expect(component.api.getData()).toEqual({"IdeTsk.selected": "2"});
    expect(actionController.addActionList).toHaveBeenCalledWith(
      [{type: "screen", target: "update-scheduler-task", parameters: {}}], false,
      expect.objectContaining({address: component.address}));
    $rootScope.$digest();
    expect(root.querySelectorAll("button")[1].getAttribute("aria-pressed")).toBe("true");
  });

  it("tells when there is no flow to draw", () => {
    model.values = [row(1, null)];
    const {root} = compileGraph();

    expect(root.querySelectorAll("button")).toHaveLength(0);
    expect(root.textContent).toContain("SCREEN_TEXT_FLOW_GRAPH_EMPTY");
  });
});
