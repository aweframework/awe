const {
  buildComponentApiMap,
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
} = require("./contractSharedTestUtils.js");

describe("awe-framework/awe-client-angular/src/test/js/contracts/shared/contractSharedTestUtils.spec.js", function () {
  it("creates contract scopes with default report and contract context values", function () {
    const childScope = {};
    const $rootScope = {
      $new: jasmine.createSpy("$new").and.returnValue(childScope)
    };

    const scope = createContractScope($rootScope);

    expect(scope).toBe(childScope);
    expect(scope.view).toBe("report");
    expect(scope.context).toBe("contract");
  });

  it("builds api maps and storage trees from component entries", function () {
    const firstComponent = {getData: () => ({CrtOne: "A"})};
    const secondComponent = {getData: () => ({CrtTwo: "B"})};

    expect(buildComponentApiMap([
      {id: "CrtOne", component: firstComponent},
      {id: "CrtTwo", component: secondComponent}
    ])).toEqual({
      CrtOne: firstComponent,
      CrtTwo: secondComponent
    });
    expect(createApiStorage("report", [
      {id: "CrtOne", component: firstComponent},
      {id: "CrtTwo", component: secondComponent}
    ])).toEqual({
      report: {
        CrtOne: firstComponent,
        CrtTwo: secondComponent
      }
    });
  });

  it("merges component payloads without altering the individual component data", function () {
    const firstComponent = {getData: jasmine.createSpy("getDataOne").and.returnValue({CrtOne: "A"})};
    const secondComponent = {getData: jasmine.createSpy("getDataTwo").and.returnValue({CrtTwo: ["B"]})};

    expect(mergeComponentData([
      {id: "CrtOne", component: firstComponent},
      {id: "CrtTwo", component: secondComponent}
    ])).toEqual({
      CrtOne: "A",
      CrtTwo: ["B"]
    });
    expect(firstComponent.getData).toHaveBeenCalled();
    expect(secondComponent.getData).toHaveBeenCalled();
  });

  it("creates server actions with callback targets and parameters", function () {
    const attributes = {};

    function FakeAction() {
      this.attr = function (name, value) {
        if (arguments.length === 2) {
          attributes[name] = value;
          return this;
        }

        return attributes[name];
      };
    }

    const callbackTarget = {view: "report", component: "CrtServerAction"};
    const parameters = {serverAction: "data", targetAction: "loadContractHelper"};
    const action = createServerAction(FakeAction, callbackTarget, parameters);

    expect(action.attr("callbackTarget")).toEqual(callbackTarget);
    expect(action.attr("parameters")).toEqual(parameters);
  });

  it("builds launch request expectations that preserve action, target and merged values", function () {
    const action = {id: "action"};
    const target = {view: "report", component: "CrtTarget"};
    const request = {
      action,
      target,
      values: {
        serverAction: "data",
        targetAction: "loadContractHelper",
        CrtOne: "A",
        max: 10,
        ignored: true
      }
    };

    expect(request).toEqual(buildLaunchRequestExpectation({
      action,
      target,
      values: {
        serverAction: "data",
        targetAction: "loadContractHelper",
        CrtOne: "A",
        max: 10
      }
    }));
  });
});
