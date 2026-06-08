// Source-traceable Jest owner for the Karma dependency service spec.
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

require("../../js/services/dependency.js");

describe("Dependency Jest behavior coverage", () => {
  let ActionController;
  let Control;
  let Dependency;
  let ServerData;
  let utilities;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", $injector => {
      ActionController = $injector.get("ActionController");
      Control = $injector.get("Control");
      Dependency = $injector.get("Dependency");
      ServerData = $injector.get("ServerData");
      utilities = $injector.get("AweUtilities");
      $injector.get("Storage").putRoot("settings", {
        serverActionKey: "serverAction",
        targetActionKey: "targetAction"
      });
    }]);
  });

  function buildDependency(options = {}) {
    return new Dependency({
      actions: [],
      elements: [],
      index: "dep-1",
      type: "and",
      ...options
    }, {
      address: {component: "Field1", view: "base"},
      context: {screen: "home"},
      controller: {optionId: "option-1"},
      id: "Field1"
    });
  }

  it("evaluates AND/OR conditions, initial launch state, and inverted updates", () => {
    const andDependency = buildDependency({initial: true});
    const orDependency = buildDependency({invert: true, type: "or"});
    const baseCondition = [
      {abort: {value: false, string: "false"}, changed: {value: true, string: "first changed"}, condition: {value: true, string: "first"}, update: true},
      {abort: {value: false, string: "false"}, changed: {value: false, string: "second unchanged"}, condition: {value: false, string: "second"}, update: true}
    ];

    const andResult = andDependency.evaluate(baseCondition, "base.Field1");
    const orResult = orDependency.evaluate(baseCondition, "base.Field1");

    expect(andResult.valid).toBe(true);
    expect(andResult.condition.value).toBe(false);
    expect(andDependency.update).toBe(false);
    expect(andDependency.alreadyLaunched).toBe(true);
    expect(orResult.condition.value).toBe(true);
    expect(orDependency.update).toBe(false);
  });

  it("retrieves source values from literals, labels, formulas, resets, and query actions", () => {
    const dependency = buildDependency({
      formule: "[amount] * 2",
      label: "TITLE",
      query: "sourceField",
      serverAction: "data",
      targetAction: "LoadField",
      value: "literal value"
    });
    const values = {amount: 4, sourceField: "from criteria"};
    dependency.values = values;
    jest.spyOn(ActionController, "addActionList").mockImplementation(() => null);
    jest.spyOn(ServerData, "getServerAction").mockImplementation((address, custom) => ({address, ...custom}));

    dependency.source = "criteria-value";
    expect(dependency.retrieveSource(values, true, false)).toBe("from criteria");
    dependency.source = "value";
    expect(dependency.retrieveSource(values, true, false)).toBe("literal value");
    dependency.source = "label";
    expect(dependency.retrieveSource(values, true, false)).toBe("TITLE");
    dependency.source = "formule";
    expect(dependency.retrieveSource(values, true, false)).toBe(8);
    dependency.source = "reset";
    expect(dependency.retrieveSource(values, true, false)).toBeNull();
    dependency.source = "query";
    dependency.target = "label";

    expect(dependency.retrieveSource(values, true, false)).toBe("[[ DEFERRED ]]");
    expect(ActionController.addActionList).toHaveBeenCalledWith(expect.arrayContaining([
      expect.objectContaining({componentId: "Field1", controllerAttribute: "label", targetAction: "LoadField", type: "update-controller"}),
      {type: "end-dependency", parameters: {dependency}}
    ]), true, {address: dependency.component.address, context: dependency.component.context});
  });

  it("applies target updates to controller, model, api, and mapped follow-up actions", () => {
    const dependency = buildDependency({
      actions: [{parameters: {targetAction: "Reload-[sourceField]"}, targetAction: "Reload-[sourceField]", type: "server"}],
      query: "customAttribute"
    });
    const values = {sourceField: "Customer"};
    const onCheck = jest.fn();
    jest.spyOn(ActionController, "addActionList").mockImplementation(() => null);
    jest.spyOn(Control, "changeControllerAttribute").mockImplementation(() => null);
    jest.spyOn(Control, "changeModelAttribute").mockImplementation(() => null);
    jest.spyOn(Control, "launchApiMethod").mockImplementation(() => null);
    jest.spyOn(utilities, "replaceWildcard").mockImplementation((value, replacements) => value.replace("[sourceField]", replacements.sourceField));
    dependency.addCheck(onCheck);

    dependency.target = "label";
    dependency.applyTarget("Translated label", true, values);
    dependency.target = "set-required";
    dependency.applyTarget("required", true, values);
    dependency.target = "hide";
    dependency.applyTarget(null, false, values);
    dependency.target = "attribute";
    dependency.applyTarget("custom value", true, values);
    dependency.target = "input";
    dependency.applyTarget("selected value", true, values);
    dependency.target = "custom-target";
    dependency.applyTarget("api value", true, values);

    expect(Control.changeControllerAttribute).toHaveBeenCalledWith(dependency.component.address, {label: "Translated label"});
    expect(Control.changeControllerAttribute).toHaveBeenCalledWith(dependency.component.address, {required: true});
    expect(Control.changeControllerAttribute).toHaveBeenCalledWith(dependency.component.address, {visible: true});
    expect(Control.changeControllerAttribute).toHaveBeenCalledWith(dependency.component.address, {customAttribute: "custom value"});
    expect(Control.changeModelAttribute).toHaveBeenCalledWith(dependency.component.address, {selected: "selected value"}, true);
    expect(Control.launchApiMethod).toHaveBeenCalledWith(dependency.component.address, "changeValidation", ["required", true]);
    expect(Control.launchApiMethod).toHaveBeenCalledWith(dependency.component.address, "applyDependency", [dependency, "api value", true]);
    expect(ActionController.addActionList).toHaveBeenLastCalledWith([
      {parameters: {targetAction: "Reload-Customer"}, targetAction: "Reload-Customer", type: "server"}
    ], true, {address: dependency.component.address, context: dependency.component.context});
    expect(onCheck).toHaveBeenCalledTimes(1);
  });
});
