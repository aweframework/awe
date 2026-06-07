// Source-traceable focused Jest parity for controllers/form.js.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe("controllers/form.js", function() {
  let scope, compile, utilities, settings, actionController, control, serverData, validator, connection, httpBackend;

  function defineForm() {
    const element = compile("<form awe-form=''></form>")(scope);
    scope.$digest();
    return element;
  }

  function runFormAction(type, attributes = {}, options = {}) {
    actionController.closeAllActions();
    const action = actionController.generateAction({type, ...attributes}, options.context || {}, true, true);
    jest.spyOn(action, "launchAction");
    if (options.accept) {
      jest.spyOn(action, "accept").mockImplementation(options.accept);
    } else {
      jest.spyOn(action, "accept").mockImplementation(() => null);
    }

    action.run();
    utilities.timeout.flush();

    expect(action.launchAction).toHaveBeenCalled();
    return action;
  }

  beforeEach(function() {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "ServerData", "Validator", "AweUtilities", "AweSettings", "ActionController", "Control", "$httpBackend", "Connection",
      function($rootScope, $compile, ServerData, Validator, AweUtilities, AweSettings, ActionController, Control, $httpBackend, Connection) {
        scope = $rootScope.$new();
        compile = $compile;
        serverData = ServerData;
        validator = Validator;
        utilities = AweUtilities;
        settings = AweSettings;
        actionController = ActionController;
        control = Control;
        httpBackend = $httpBackend;
        connection = Connection;
      }]);
    httpBackend.when("POST", "settings").respond({});
    jest.spyOn(settings, "get").mockImplementation(key => ({targetActionKey: "targetAction"})[key]);
  });

  afterEach(function() {
    jest.restoreAllMocks();
  });

  it("links the awe-form directive onto a form element without layout shims", function() {
    const element = defineForm();

    expect(element[0].tagName).toBe("FORM");
    expect(scope.element[0]).toBe(element[0]);
  });

  it("downloads maintain files with form values and target-specific fields", function() {
    const callbackTarget = {view: "base", component: "report"};
    const formValues = {user: "alice"};
    const specificFields = {selectedRows: [1, 2]};
    const fileData = {url: "download/maintain/ExportUsers", params: {}};
    defineForm();
    jest.spyOn(serverData, "getFormValues").mockReturnValue(formValues);
    jest.spyOn(serverData, "getFileData").mockReturnValue(fileData);
    jest.spyOn(control, "getAddressApi").mockReturnValue({getSpecificFields: jest.fn().mockReturnValue(specificFields)});
    jest.spyOn(utilities, "downloadFile").mockImplementation(() => null);

    const action = runFormAction("server-download", {
      target: "report",
      address: callbackTarget,
      parameters: {targetAction: "ExportUsers", format: "xlsx"}
    });

    expect(control.getAddressApi).toHaveBeenCalledWith(callbackTarget);
    expect(serverData.getFileData).toHaveBeenCalledWith("download/maintain/ExportUsers", {
      format: "xlsx",
      targetAction: "ExportUsers",
      user: "alice",
      selectedRows: [1, 2]
    });
    expect(utilities.downloadFile).toHaveBeenCalledWith({...fileData, action});
  });

  it("downloads maintain files directly when no callback target is available", function() {
    const fileData = {url: "download/maintain/ExportAll", params: {}};
    defineForm();
    jest.spyOn(serverData, "getFormValues").mockReturnValue({filter: "active"});
    jest.spyOn(serverData, "getFileData").mockReturnValue(fileData);
    jest.spyOn(control, "getAddressApi");
    jest.spyOn(utilities, "downloadFile").mockImplementation(() => null);

    runFormAction("server-download", {parameters: {targetAction: "ExportAll"}});

    expect(control.getAddressApi).not.toHaveBeenCalled();
    expect(serverData.getFileData).toHaveBeenCalledWith("download/maintain/ExportAll", {
      filter: "active",
      targetAction: "ExportAll"
    });
    expect(utilities.downloadFile).toHaveBeenCalledWith({...fileData, action: expect.any(Object)});
  });

  it("copies the selected criterion value to the clipboard and accepts the action", function() {
    const clipboard = {writeText: jest.fn()};
    Object.defineProperty(navigator, "clipboard", {configurable: true, value: clipboard});
    defineForm();
    jest.spyOn(document.body, "focus").mockImplementation(() => null);
    jest.spyOn(control, "getAddressModel").mockReturnValue({selected: "copied-value"});
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    const action = runFormAction("copy-criterion-value-clipboard", {
      target: "criterion",
      address: {view: "base", component: "criterion"}
    });

    expect(control.getAddressModel).toHaveBeenCalledWith({view: "base", component: "criterion"});
    expect(document.body.focus).toHaveBeenCalled();
    expect(clipboard.writeText).toHaveBeenCalledWith("copied-value");
    expect(actionController.acceptAction).toHaveBeenCalledWith(action);
  });

  it("copies an empty string when the criterion selection is empty", function() {
    const clipboard = {writeText: jest.fn()};
    Object.defineProperty(navigator, "clipboard", {configurable: true, value: clipboard});
    defineForm();
    jest.spyOn(document.body, "focus").mockImplementation(() => null);
    jest.spyOn(control, "getAddressModel").mockReturnValue({selected: []});
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    runFormAction("copy-criterion-value-clipboard", {
      target: "criterion",
      address: {view: "base", component: "criterion"}
    });

    expect(clipboard.writeText).toHaveBeenCalledWith("");
  });

  it("submits logout through a local form fixture after disconnecting", function() {
    const fakeForm = {method: "", action: "", submit: jest.fn()};
    defineForm();
    jest.spyOn(actionController, "deleteStack").mockImplementation(() => null);
    jest.spyOn(connection, "disconnect").mockReturnValue({then: callback => callback()});
    jest.spyOn(connection, "getActionUrl").mockReturnValue("http://server/action/logout");
    jest.spyOn(document, "createElement").mockReturnValue(fakeForm);
    jest.spyOn(document.body, "appendChild").mockImplementation(() => fakeForm);
    jest.spyOn(control, "destroyAllViews").mockImplementation(() => null);

    runFormAction("logout", {address: {view: "base"}});

    expect(actionController.deleteStack).toHaveBeenCalled();
    expect(connection.disconnect).toHaveBeenCalled();
    expect(fakeForm.method).toBe("POST");
    expect(fakeForm.action).toBe("http://server/action/logout");
    expect(document.body.appendChild).toHaveBeenCalledWith(fakeForm);
    expect(fakeForm.submit).toHaveBeenCalled();
    expect(control.destroyAllViews).toHaveBeenCalled();
  });

  it("updates controller attributes from datalist rows and preserves explicit falsy values", function() {
    const address = {view: "base", component: "field"};
    defineForm();
    jest.spyOn(control, "changeControllerAttribute").mockImplementation(() => null);
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    runFormAction("update-controller", {
      target: "field",
      address,
      parameters: {attribute: "label", datalist: {rows: [{value: "Name"}]}}
    });
    runFormAction("update-controller", {target: "field", address, parameters: {attribute: "count", value: 0}});
    runFormAction("update-controller", {target: "field", address, parameters: {attribute: "visible", value: false}});
    runFormAction("update-controller", {target: "field", address, parameters: {attribute: "placeholder", value: ""}});

    expect(control.changeControllerAttribute.mock.calls).toEqual([
      [address, {label: "Name"}],
      [address, {count: 0}],
      [address, {visible: false}],
      [address, {placeholder: ""}]
    ]);
    expect(actionController.acceptAction).toHaveBeenCalledTimes(4);
  });

  it("normalizes select and suggest payloads through the utilities boundary", function() {
    const address = {view: "base", component: "selector"};
    const values = [{value: "A", label: "Alpha"}, {value: "B", label: "Beta"}];
    defineForm();
    jest.spyOn(utilities, "formatSelectedValues").mockReturnValue(["A", "B"]);
    jest.spyOn(control, "changeModelAttribute").mockImplementation(() => null);
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    runFormAction("select", {target: "selector", address, parameters: {values}});
    runFormAction("fill-suggest", {target: "selector", address, parameters: {values}});

    expect(utilities.formatSelectedValues).toHaveBeenCalledWith(values);
    expect(control.changeModelAttribute.mock.calls).toEqual([
      [address, {selected: ["A", "B"]}, true],
      [address, {selected: ["A", "B"], values}, true]
    ]);
    expect(actionController.acceptAction).toHaveBeenCalledTimes(2);
  });

  it("finishes validate actions based on validator errors", function() {
    defineForm();
    jest.spyOn(validator, "validateNode").mockReturnValueOnce([]).mockReturnValueOnce(["required"]);
    jest.spyOn(actionController, "finishAction").mockImplementation(() => null);

    const validAction = runFormAction("validate", {target: "missing-field", parameters: {}});
    const invalidAction = runFormAction("validate", {target: "missing-field", parameters: {}});

    expect(validator.validateNode).toHaveBeenCalledTimes(2);
    expect(actionController.finishAction).toHaveBeenNthCalledWith(1, validAction, true);
    expect(actionController.finishAction).toHaveBeenNthCalledWith(2, invalidAction, false);
  });

  it("adds confirm actions only when model-change predicates require user confirmation", function() {
    const target = {view: "base", component: "grid"};
    defineForm();
    jest.spyOn(control, "checkModelChanged").mockReturnValueOnce(true).mockReturnValueOnce(false);
    jest.spyOn(control, "checkModelEmpty").mockReturnValue(true);
    jest.spyOn(control, "addMessageToScope").mockImplementation(() => null);
    jest.spyOn(actionController, "addActionList").mockImplementation(() => null);
    jest.spyOn(actionController, "acceptAction").mockImplementation(() => null);

    runFormAction("confirm-updated-data", {target: "grid", address: target, context: "edition"});
    runFormAction("confirm-not-updated-data", {target: "grid", address: target, context: "edition"});
    runFormAction("confirm-empty-data", {target: "grid", address: target, context: "edition"});

    expect(control.addMessageToScope.mock.calls).toEqual([
      ["base", "CONFIRM_UPDATE_DATA", {title: "CONFIRM_TITLE_UPDATED_DATA", message: "CONFIRM_MESSAGE_UPDATED_DATA"}],
      ["base", "CONFIRM_NOT_UPDATE_DATA", {title: "CONFIRM_TITLE_NOT_UPDATED_DATA", message: "CONFIRM_MESSAGE_NOT_UPDATED_DATA"}],
      ["base", "CONFIRM_EMPTY_DATA", {title: "CONFIRM_TITLE_EMPTY_DATA", message: "CONFIRM_MESSAGE_EMPTY_DATA"}]
    ]);
    expect(actionController.addActionList).toHaveBeenCalledTimes(3);
    expect(actionController.addActionList).toHaveBeenNthCalledWith(1, [{type: "confirm", parameters: {target: "CONFIRM_UPDATE_DATA"}}], false, {address: target, context: "edition"});
    expect(actionController.addActionList).toHaveBeenNthCalledWith(2, [{type: "confirm", parameters: {target: "CONFIRM_NOT_UPDATE_DATA"}}], false, {address: target, context: "edition"});
    expect(actionController.addActionList).toHaveBeenNthCalledWith(3, [{type: "confirm", parameters: {target: "CONFIRM_EMPTY_DATA"}}], false, {address: target, context: "edition"});
  });

  it("sets update-controller attributes to undefined when no value or datalist rows exist", function() {
    const address = {view: "base", component: "field"};
    defineForm();
    jest.spyOn(control, "changeControllerAttribute").mockImplementation(() => null);

    runFormAction("update-controller", {target: "field", address, parameters: {attribute: "label", datalist: {rows: []}}});

    expect(control.changeControllerAttribute).toHaveBeenCalledWith(address, {label: undefined});
  });

  it("selects primitive values without replacing available option values", function() {
    const address = {view: "base", component: "selector"};
    defineForm();
    jest.spyOn(utilities, "formatSelectedValues").mockReturnValue("A");
    jest.spyOn(control, "changeModelAttribute").mockImplementation(() => null);

    runFormAction("select", {target: "selector", address, parameters: {value: "A"}});

    expect(control.changeModelAttribute).toHaveBeenCalledWith(address, {selected: "A"}, true);
  });

  it("fills suggest values while preserving the server-provided option list", function() {
    const address = {view: "base", component: "selector"};
    const values = [{value: "A", label: "Alpha"}];
    defineForm();
    jest.spyOn(utilities, "formatSelectedValues").mockReturnValue("A");
    jest.spyOn(control, "changeModelAttribute").mockImplementation(() => null);

    runFormAction("fill-suggest", {target: "selector", address, parameters: {values}});

    expect(control.changeModelAttribute).toHaveBeenCalledWith(address, {selected: "A", values}, true);
  });
});
