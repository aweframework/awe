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
        asComponent: jest.fn(() => true), storeEvent: jest.fn()
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
  function compileGraph(id = "IdeTsk") {
    const element = $compile(`<awe-flow-graph flow-graph-id="${id}"></awe-flow-graph>`)($rootScope.$new());
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

  it("draws the one-level graph of a task in three columns, without buttons and with the condition of every edge", () => {
    controller.parameters = {mode: "task"};
    model.values = [
      row(1, null, {lastStatus: 0, onWarning: 1}),
      row(2, 1, {rootTask: 2, lastStatus: 2}),
      row(3, 2), row(4, 3)
    ];
    const {root} = compileGraph();
    const labels = [...root.querySelectorAll("[role=listitem]")].map(node => node.getAttribute("aria-label"));

    expect(root.querySelectorAll("button")).toHaveLength(0);
    expect(labels).toEqual(["SCREEN_TEXT_FLOW_GRAPH_PARENTS · Task 1 · ENUM_STATUS_OK",
      "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2 · ENUM_STATUS_WARNING", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN · Task 3"]);
    expect([...root.querySelectorAll(".flow-graph-condition")].map(label => label.getAttribute("aria-label")))
      .toEqual(["SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING", "SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK"]);
    expect([...root.querySelectorAll(".flow-graph-condition")].map(label => label.querySelectorAll(".flow-graph-condition-icon").length))
      .toEqual([2, 1]);
    expect([...root.querySelectorAll(".flow-graph-column")].map(column => column.textContent)).toEqual([
      "SCREEN_TEXT_FLOW_GRAPH_PARENTS", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN"]);
    expect(root.querySelector(".is-self").getAttribute("aria-label")).toContain("Task 2");
  });

  it("draws only one side of the task in two columns when the side parameter asks for it", () => {
    const rows = [row(1, null, {onWarning: 1}), row(2, 1, {rootTask: 2}), row(3, 2)];
    const draw = side => {
      controller.parameters = {mode: "task", side};
      model.values = rows;
      const {root} = compileGraph();
      return {
        labels: [...root.querySelectorAll("[role=listitem]")].map(node => node.getAttribute("aria-label")),
        titles: [...root.querySelectorAll(".flow-graph-column")].map(column => column.textContent)
      };
    };

    expect(draw("parents")).toEqual({
      labels: ["SCREEN_TEXT_FLOW_GRAPH_PARENTS · Task 1", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2"],
      titles: ["SCREEN_TEXT_FLOW_GRAPH_PARENTS", "SCREEN_TEXT_FLOW_GRAPH_THIS_TASK"]
    });
    expect(draw("children")).toEqual({
      labels: ["SCREEN_TEXT_FLOW_GRAPH_THIS_TASK · Task 2", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN · Task 3"],
      titles: ["SCREEN_TEXT_FLOW_GRAPH_THIS_TASK", "SCREEN_TEXT_FLOW_GRAPH_CHILDREN"]
    });
  });

  // The one-level graph of the task 2 (the task screen): its parent 1 and its child 3, which also waits for the task 4
  const taskRows = (extra = {}) => [
    row(1, null, {lastStatus: 0, onWarning: 1, lastWhen: "hoy 08:00", nextRun: "Next: tomorrow 03:00", ...extra[1]}),
    row(2, 1, {rootTask: 2, lastStatus: 2}), row(3, 2), row(3, 4), row(4, null, {name: "Bank answer"})
  ];

  it("opens the task screen of a parent or a child of the task, and the task itself is not a link", () => {
    controller.parameters = {mode: "task", target: "scheduler-task", criterion: "IdeTsk.selected"};
    model.values = taskRows();
    const {root, component} = compileGraph();

    expect(root.querySelectorAll("button")).toHaveLength(2);
    expect([...root.querySelectorAll("button")].map(button => button.getAttribute("aria-label")))
      .toEqual(["SCREEN_TEXT_FLOW_GRAPH_OPEN_TASK", "SCREEN_TEXT_FLOW_GRAPH_OPEN_TASK"]);
    expect(root.querySelector("[role=group].is-self").getAttribute("aria-label")).toContain("Task 2");

    root.querySelectorAll("button")[1].click();

    expect(control.changeModelAttribute).toHaveBeenCalledWith({view: "report", component: "IdeTsk.selected"},
      {selected: ["3"]}, true);
    expect(actionController.addActionList).toHaveBeenCalledWith(
      [{type: "screen", target: "scheduler-task", parameters: {reload: true}}], false,
      expect.objectContaining({address: component.address}));
  });

  it("marks an inactive task with a tag, also when it has a state", () => {
    controller.parameters = {mode: "task"};
    model.values = taskRows({1: {active: 0}});
    const {root} = compileGraph();
    const parent = root.querySelector(".is-parent");

    expect(parent.querySelector(".flow-graph-node-tag").textContent).toBe("SCREEN_TEXT_FLOW_GRAPH_INACTIVE");
    expect(parent.textContent).toContain("ENUM_STATUS_OK");
  });

  it("tells when each task last ran, the next execution of a scheduled one and the other tasks a child waits for", () => {
    controller.parameters = {mode: "task"};
    model.values = taskRows();
    const {root} = compileGraph();
    const parent = root.querySelector(".is-parent");

    expect(parent.textContent).toContain("ENUM_STATUS_OK · hoy 08:00");
    expect(parent.textContent).toContain("Next: tomorrow 03:00");
    expect(root.querySelector(".flow-graph-node-also").textContent).toContain("SCREEN_TEXT_FLOW_GRAPH_ALSO_WAITS");
    expect(root.querySelector(".flow-graph-node-also").textContent).toContain("Bank answer");
    expect(root.querySelector(".flow-graph-legend").textContent).toContain("SCREEN_TEXT_FLOW_GRAPH_LEGEND");
  });

  it("tells when there is no flow to draw", () => {
    model.values = [row(1, null)];
    const {root} = compileGraph();

    expect(root.querySelectorAll("button")).toHaveLength(0);
    expect(root.textContent).toContain("SCREEN_TEXT_FLOW_GRAPH_EMPTY");
  });

  // The rows of getFlowRunGraph (the flow run page): 1 → 2, 1 → 3, 2 → 4 and 3 → 4 (a join), 4 → 5 not reached
  const at = time => `17/11/2026 ${time}`;
  const runRows = () => [
    row(1, null, {rootRun: "run", executionStatus: 0, executionId: 1092, IdeTsk: 1, ExeTsk: 1092, ExeTim: "1 min 12 s",
      startDate: at("08:05:00"), endDate: at("08:06:12"), onWarning: 1}),
    row(2, 1, {executionStatus: 2, executionId: 640, IdeTsk: 2, ExeTsk: 640, startDate: at("08:06:12"),
      endDate: at("08:09:52"), edgeState: "MET", conditionLabel: "OK o Aviso"}),
    row(3, 1, {executionStatus: 4, executionId: 875, IdeTsk: 3, ExeTsk: 875, deferred: 1, startDate: at("08:06:12"),
      ExeDueLbl: "Vence 12:15", edgeState: "MET", conditionLabel: "OK o Aviso"}),
    ...[2, 3].map(parent => row(4, parent, {runStatus: "WAITING", IdeTsk: 4, edgeState: parent === 2 ? "MET" : "PENDING",
      conditionLabel: "OK"})),
    row(5, 4, {IdeTsk: 5, edgeState: "PENDING"})
  ];
  const compileRun = () => {
    controller.parameters = {mode: "run"};
    model.values = runRows();
    return compileGraph("FlwRunGph");
  };
  const runNode = (root, name) => [...root.querySelectorAll(".flow-graph-run-node")]
    .find(node => node.getAttribute("aria-label").startsWith(`${name} ·`));
  const tool = (root, label) => root.querySelector(`.flow-graph-tool[aria-label="${label}"]`);

  describe("run mode", () => {
    beforeEach(() => {
      window.Element.prototype.scrollIntoView = jest.fn();
    });

    it("draws every task of the run with its state, execution, times and due date, and every edge with its condition", () => {
      const {root} = compileRun();

      expect(runNode(root, "Task 1").getAttribute("aria-label")).toBe(
        "Task 1 · ENUM_STATUS_OK · SCREEN_TEXT_EXECUTION 1092 · 1 min 12 s · 08:05 – 08:06");
      expect(runNode(root, "Task 3").textContent).toContain("Vence 12:15");
      expect(runNode(root, "Task 3").textContent).toContain("SCREEN_TEXT_ACTIVITY_WAITING");
      expect(runNode(root, "Task 4").textContent).toContain("SCREEN_TEXT_FLOW_RUN_PENDING");
      expect([...root.querySelectorAll(".flow-graph-run-condition")].map(label => label.textContent))
        .toEqual(["OK o Aviso", "OK o Aviso", "OK", "OK", "SCREEN_TEXT_FLOW_RUN_EDGE_OK"]);
    });

    it("selects the node of an execution, launches the actions of the widget and sends its row like a grid row", () => {
      const {root, component} = compileRun();

      runNode(root, "Task 2").click();
      $rootScope.$digest();

      expect(control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["2"]}, true);
      expect(component.storeEvent).toHaveBeenCalledWith("click");
      expect(component.api.getData()).toMatchObject({"FlwRunGph.selected": "2", "IdeTsk.selected": 2,
        "ExeTsk.selected": 640});
      expect(runNode(root, "Task 2").getAttribute("aria-pressed")).toBe("true");
      expect(runNode(root, "Task 1").getAttribute("aria-pressed")).toBe("false");
      expect(window.Element.prototype.scrollIntoView).toHaveBeenCalledWith({block: "nearest", inline: "nearest"});
      expect(root.querySelector("[aria-live=polite]").textContent).toMatch(/^SCREEN_TEXT_FLOW_GRAPH_SELECTED/);
    });

    it("keeps a task without execution focusable but it opens nothing", () => {
      const {root, component} = compileRun();
      const pending = runNode(root, "Task 4");

      pending.click();

      expect(pending.tagName).toBe("BUTTON");
      expect(pending.getAttribute("aria-disabled")).toBe("true");
      expect(control.changeModelAttribute).not.toHaveBeenCalled();
      expect(component.storeEvent).not.toHaveBeenCalled();
    });

    it("clears the selection with Escape", () => {
      const {root, component} = compileRun();
      runNode(root, "Task 2").click();
      $rootScope.$digest();

      runNode(root, "Task 2").dispatchEvent(new KeyboardEvent("keydown", {key: "Escape", bubbles: true}));
      $rootScope.$digest();

      expect(component.api.getData()).toMatchObject({"FlwRunGph.selected": null});
      expect(runNode(root, "Task 2").getAttribute("aria-pressed")).toBe("false");
    });

    it("zooms in and out a step and fits the graph to the width it has", () => {
      const {root} = compileRun();
      const zoom = () => root.querySelector(".flow-graph-zoom").textContent;

      tool(root, "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN").click();
      $rootScope.$digest();
      expect(zoom()).toBe("125 %");
      tool(root, "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT").click();
      tool(root, "SCREEN_TEXT_FLOW_GRAPH_ZOOM_OUT").click();
      $rootScope.$digest();
      expect(zoom()).toBe("75 %");

      tool(root, "SCREEN_TEXT_FLOW_GRAPH_FIT").click();
      $rootScope.$digest();
      expect(zoom()).toBe("50 %");
    });

    it("starts at the zoom that fits a graph wider than its viewport (50 % at least), once the viewport is measured", () => {
      // A viewport of 300 x 900 px while it is shown, nothing while it is hidden (as a browser measures it)
      const VIEWPORT = {clientWidth: 300, clientHeight: 900};
      const spies = Object.keys(VIEWPORT).map(name => jest.spyOn(window.HTMLElement.prototype, name, "get")
        .mockImplementation(function () {
          return this.classList.contains("flow-graph-viewport") && !this.closest(".ng-hide") ? VIEWPORT[name] : 0;
        }));
      try {
        // Hidden by its dependency it cannot be measured: it fits when it is shown
        controller.visible = false;
        const {root} = compileRun();
        const zoom = () => root.querySelector(".flow-graph-zoom").textContent;
        expect(zoom()).toBe("100 %");

        controller.visible = true;
        $rootScope.$digest();
        expect(zoom()).toBe("50 %");
        // The zoom buttons still change it
        tool(root, "SCREEN_TEXT_FLOW_GRAPH_ZOOM_IN").click();
        $rootScope.$digest();
        expect(zoom()).toBe("75 %");
      } finally {
        spies.forEach(spy => spy.mockRestore());
      }
    });
  });

  describe("run views", () => {
    let now;
    const compileView = (view, rows = runRows()) => {
      controller.parameters = {mode: "run", view};
      model.values = rows;
      return compileGraph("FlwRunGph");
    };
    const button = (root, name) => [...root.querySelectorAll("button")]
      .find(item => item.getAttribute("aria-label").startsWith(`${name} ·`));

    beforeEach(() => {
      window.Element.prototype.scrollIntoView = jest.fn();
      now = jest.spyOn(Date, "now").mockReturnValue(new Date(2026, 10, 17, 10, 42).getTime());
    });

    afterEach(() => {
      now.mockRestore();
    });

    it("draws the timeline: one row per task, its bars as toggle buttons that select the task, «Now» and the break", () => {
      const {root, component} = compileView("timeline");
      const timeline = root.querySelector("[role=table]");

      expect(timeline.getAttribute("aria-label")).toBe("SCREEN_TEXT_FLOW_RUN_TIMELINE");
      expect([...root.querySelectorAll("[role=rowheader]")].map(header => header.textContent.replace(/\s+/g, "")))
        .toEqual(["Task1ENUM_STATUS_OK1092", "Task2ENUM_STATUS_WARNING640", "Task4SCREEN_TEXT_FLOW_RUN_PENDING",
          "Task5SCREEN_TEXT_FLOW_RUN_PENDING", "Task3SCREEN_TEXT_ACTIVITY_WAITING875Vence12:15"]);
      expect(button(root, "Task 1").getAttribute("aria-label")).toBe(
        "Task 1 · ENUM_STATUS_OK · 08:05 – 08:06 · 1 min 12 s");
      expect(button(root, "Task 3").textContent.trim()).toBe("SCREEN_TEXT_ACTIVITY_WAITING · 2 h 35 min");
      expect(timeline.textContent).toContain("SCREEN_TEXT_FLOW_RUN_NOW 10:42");
      expect(timeline.textContent).toContain("≈");
      expect(button(root, "Task 4")).toBeUndefined();

      button(root, "Task 2").click();
      $rootScope.$digest();

      expect(control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["2"]}, true);
      expect(component.storeEvent).toHaveBeenCalledWith("click");
      expect(component.api.getData()).toMatchObject({"IdeTsk.selected": 2, "ExeTsk.selected": 640});
      expect(button(root, "Task 2").getAttribute("aria-pressed")).toBe("true");
      expect(button(root, "Task 1").getAttribute("aria-pressed")).toBe("false");
    });

    it("draws the list: a nested ordered list, a join under the parent that closed it and «also feeds» under the other", () => {
      const {root, component} = compileView("list", runRows().map(item => item.taskId === 4 ? {...item, joinedBy: 2} : item));
      $rootScope.$digest();
      const list = root.querySelector("ol");
      const nested = element => element.closest("li").parentElement.closest("li")?.querySelector("button");

      expect(list.getAttribute("aria-label")).toBe("SCREEN_TEXT_FLOW_RUN_LIST");
      expect(nested(button(root, "Task 4"))).toBe(button(root, "Task 2"));
      expect(nested(button(root, "Task 5"))).toBe(button(root, "Task 4"));
      expect(button(root, "Task 2").textContent).toContain("SCREEN_TEXT_TASK_WHEN_AFTER");
      expect(button(root, "Task 3").closest("li").textContent).toContain("SCREEN_TEXT_FLOW_RUN_ALSO_FEEDS");
      expect(button(root, "Task 4").getAttribute("aria-disabled")).toBe("true");

      button(root, "Task 4").click();
      expect(component.storeEvent).not.toHaveBeenCalled();
      button(root, "Task 3").click();
      $rootScope.$digest();

      expect(control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["3"]}, true);
      expect(component.storeEvent).toHaveBeenCalledWith("click");
      expect(button(root, "Task 3").getAttribute("aria-pressed")).toBe("true");
    });
  });
});
