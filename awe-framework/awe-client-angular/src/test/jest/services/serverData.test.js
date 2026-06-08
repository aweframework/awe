// Focused source-traceable ServerData coverage. Karma original remains callable.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";

require("../../js/services/serverData.js");

describe("ServerData", function () {
  let $serverData;
  let $storage;
  let $connection;
  let $actionController;
  let $settings;
  let $utilities;
  let $control;
  let $log;
  let $templateCache;

  beforeEach(function () {
    angular.mock.module("aweApplication");

    inject(["$injector", function ($injector) {
      $serverData = $injector.get("ServerData");
      $storage = $injector.get("Storage");
      $connection = $injector.get("Connection");
      $actionController = $injector.get("ActionController");
      $settings = $injector.get("AweSettings");
      $utilities = $injector.get("AweUtilities");
      $control = $injector.get("Control");
      $log = $injector.get("$log");
      $templateCache = $injector.get("$templateCache");
    }]);
  });

  it("retrieves screen data with current form values, stores the view payload, and returns the template", async function () {
    const screenData = {data: {template: "<section>Loaded</section>", screen: {option: "customer-list"}, messages: {title: "Customers"}, components: []}};
    jest.spyOn($serverData, "getFormValues").mockReturnValue({CrtStatus: "ACTIVE"});
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($connection, "post").mockReturnValue(Promise.resolve(screenData));
    jest.spyOn($storage, "put");

    await expect($serverData.getScreenData("customer-list", "center")).resolves.toBe("<section>Loaded</section>");

    expect($connection.post).toHaveBeenCalledWith("/awe/screen-data/customer-list", {
      CrtStatus: "ACTIVE",
      view: "center",
      [$settings.get("optionKey")]: "customer-list",
      template: true
    }, "application/json");
    expect($storage.put).toHaveBeenCalledWith("screenData-center", screenData.data);
  });

  it("routes failed screen-data responses through the REST error action list for the base view", async function () {
    const errorActions = [{type: "message", parameters: {message: "Screen unavailable"}}];
    jest.spyOn($serverData, "getFormValues").mockReturnValue({CrtStatus: "ACTIVE"});
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($connection, "post").mockReturnValue(Promise.reject({status: 503, data: {title: "Service unavailable", message: "Screen unavailable"}}));
    jest.spyOn($utilities, "manageRestError").mockReturnValue(errorActions);
    jest.spyOn($actionController, "addActionList");

    await $serverData.getScreenData("customer-list", "center");

    expect($utilities.manageRestError).toHaveBeenCalledWith({status: 503, title: "Service unavailable", message: "Screen unavailable"});
    expect($actionController.addActionList).toHaveBeenCalledWith(errorActions, false, {address: {view: "base"}});
  });

  it("stores screen components as normalized models, controllers, APIs, current option, and launch actions", function () {
    const controller = {base: {currentOption: {id: "currentOption"}}};
    const storage = {controller, messages: {}, api: {base: {}}, screen: {}, model: {base: {currentOption: {selected: null, previous: null}}}, status: {base: "loaded"}};
    const data = {
      screen: {name: "report-screen", option: "RptOpt"},
      messages: {info: "ready"},
      components: [{id: "CrtStatus", controller: {id: "CrtStatus", label: "Status"}, model: {selected: ["ACTIVE"], defaultValues: ["ALL"], values: [{value: "ACTIVE", label: "Active"}]}}],
      actions: [{type: "server", parameters: {target: "afterLoad"}}]
    };

    jest.spyOn($storage, "get").mockImplementation(key => storage[key]);
    jest.spyOn($storage, "has").mockImplementation(key => key in storage);
    jest.spyOn($actionController, "addActionList").mockReturnValue("actions-launched");

    expect($serverData.storeScreenData(data, "center")).toBe("actions-launched");

    expect(storage.screen.center).toEqual(data.screen);
    expect(storage.messages.center).toEqual(data.messages);
    expect(controller.center.CrtStatus).toEqual({id: "CrtStatus", label: "Status", optionId: "RptOpt"});
    expect(storage.api.center.CrtStatus).toEqual({});
    expect(storage.model.center.CrtStatus).toEqual({values: [{value: "ACTIVE", label: "Active"}], selected: "ACTIVE", defaultValues: "ALL", previous: "ACTIVE"});
    expect(storage.model.base.currentOption.selected).toBe("RptOpt");
    expect(storage.model.base.currentOption.previous).toBe("RptOpt");
    expect($actionController.addActionList).toHaveBeenCalledWith(data.actions, true, {address: {}, context: ""});
  });

  it("ignores empty screen data without mutating stored view state or launching actions", function () {
    const storage = {controller: {base: {currentOption: {id: "currentOption"}}}, messages: {}, api: {base: {}}, screen: {}, model: {base: {currentOption: {selected: null, previous: null}}}};
    jest.spyOn($storage, "get").mockImplementation(key => storage[key]);
    jest.spyOn($actionController, "addActionList");

    expect($serverData.storeScreenData(null, "center")).toBeUndefined();

    expect(storage.screen.center).toBeUndefined();
    expect(storage.messages.center).toBeUndefined();
    expect(storage.api.center).toBeUndefined();
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it("builds endpoint URLs and file payloads from the raw application URL", function () {
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");

    expect($serverData.getScreenDataUrl(null)).toBe("/awe/screen-data");
    expect($serverData.getScreenDataUrl("home")).toBe("/awe/screen-data/home");
    expect($serverData.getTemplateUrl(null, "modal", "42")).toBe("/awe/template/screen?r=42");
    expect($serverData.getTemplateUrl("orders", "modal", "42")).toBe("/awe/template/screen/modal/orders?r=42");
    expect($serverData.getHelpUrl("orders")).toBe("/awe/template/help/orders");
    expect($serverData.getGenericFileUrl("download", "invoice")).toBe("/awe/download/invoice");
    expect($serverData.getFileData("export", {format: "csv"})).toEqual({url: "/awe/file/export", data: {format: "csv"}});
    expect($serverData.getAngularTemplateUrl("criteria/status.html")).toBe("/awe/template/angular/criteria/status.html");
  });

  it("preloads successful Angular templates into the template cache and invokes the load callback", function () {
    const success = jest.fn().mockName("success").mockImplementation(callback => callback("<div>Status</div>", 200));
    const onLoad = jest.fn().mockName("onLoad");
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($serverData, "get").mockReturnValue({success});
    jest.spyOn($templateCache, "put");

    $serverData.preloadAngularTemplate({path: "criteria/status.html", name: "statusTemplate"}, onLoad);

    expect($serverData.get).toHaveBeenCalledWith("/awe/template/angular/criteria/status.html");
    expect($templateCache.put).toHaveBeenCalledWith("statusTemplate", "<div>Status</div>");
    expect(onLoad).toHaveBeenCalledWith("<div>Status</div>");
  });

  it("does not cache or notify when an Angular template response is not successful", function () {
    const success = jest.fn().mockName("success").mockImplementation(callback => callback("", 204));
    const onLoad = jest.fn().mockName("onLoad");
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($serverData, "get").mockReturnValue({success});
    jest.spyOn($templateCache, "put");

    $serverData.preloadAngularTemplate({path: "criteria/status.html"}, onLoad);

    expect($serverData.get).toHaveBeenCalledWith("/awe/template/angular/criteria/status.html");
    expect($templateCache.put).not.toHaveBeenCalled();
    expect(onLoad).not.toHaveBeenCalled();
  });

  it("warns before component data overwrites existing model parameters and leaves missing methods untouched", function () {
    const model = {CrtStatus: "ACTIVE", keep: "yes"};
    const component = {getData: jest.fn().mockName("getData").mockReturnValue({CrtStatus: "INACTIVE", extra: "value"})};
    $log.warn = jest.fn();

    expect($serverData.getComponentData(component, model, "getData")).toEqual({CrtStatus: "INACTIVE", keep: "yes", extra: "value"});
    expect($log.warn).toHaveBeenCalledWith("[WARNING] Overwriting 'CrtStatus' duplicated parameter", {old: "ACTIVE", new: "INACTIVE"});
    expect($serverData.getComponentData({}, model, "getData")).toBe(model);
  });

  it("merges selector-specific print fields with report orientation", function () {
    const printComponent = {getPrintData: jest.fn().mockName("getPrintData").mockReturnValue({CrtStatus: "ACTIVE", orientation: "LANDSCAPE"})};
    jest.spyOn($storage, "get").mockImplementation(key => ({api: {center: {CrtStatus: printComponent}}, model: {report: {reportOrientation: {selected: "LANDSCAPE"}}}}[key] || {}));

    expect($serverData.getFormValuesForPrinting()).toEqual({CrtStatus: "ACTIVE", orientation: "LANDSCAPE"});
    expect(printComponent.getPrintData).toHaveBeenCalledWith("LANDSCAPE");
  });

  it("creates server actions and maintain actions with custom selector parameters", function () {
    jest.spyOn($storage, "get").mockReturnValue({center: {name: "report-screen"}});
    jest.spyOn($actionController, "addActionList").mockReturnValue("maintain-enqueued");

    expect($serverData.getServerAction({view: "center", component: "CrtStatus"}, {type: "data", targetAction: "loadStatus", CrtStatus: "ACTIVE"}, true, false)).toEqual({
      type: "server",
      async: true,
      silent: false,
      parameters: {screen: "report-screen", address: {view: "center", component: "CrtStatus"}, component: "CrtStatus", type: "data", targetAction: "loadStatus", CrtStatus: "ACTIVE", [$settings.get("serverActionKey")]: "data"}
    });

    expect($serverData.sendMaintain({type: "maintain", maintain: "SaveStatus"}, false, true)).toBe("maintain-enqueued");
    expect($actionController.addActionList).toHaveBeenCalledWith([{type: "server", async: false, silent: true, parameters: {[$settings.get("serverActionKey")]: "maintain", [$settings.get("targetActionKey")]: "SaveStatus"}}], true, {address: {}, context: ""});
  });

  it("cancels an in-flight server action by rejecting the current request", function () {
    const callbackTarget = {view: "center", component: "CrtStatus"};
    const request = {reject: jest.fn().mockName("reject")};
    const action = {attr: jest.fn().mockName("attr").mockImplementation(key => ({callbackTarget, parameters: {serverAction: "data", targetAction: "loadStatus"}}[key]))};
    jest.spyOn($control, "changeControllerAttribute");
    jest.spyOn($control, "getAddressApi").mockReturnValue({});
    jest.spyOn($connection, "sendMessage").mockReturnValue(request);

    $serverData.launchServerAction(action, {CrtStatus: "ACTIVE"});
    action.onCancel();

    expect($connection.sendMessage).toHaveBeenCalledWith({action, target: callbackTarget, values: {serverAction: "data", targetAction: "loadStatus", CrtStatus: "ACTIVE"}});
    expect(request.reject).toHaveBeenCalled();
  });

  it("delegates raw send and get calls to the connection boundary", function () {
    const message = {values: {serverAction: "data"}};
    const sendResponse = {data: {ok: true}};
    const getResponse = {data: "template"};
    jest.spyOn($connection, "send").mockReturnValue(sendResponse);
    jest.spyOn($connection, "get").mockReturnValue(getResponse);

    expect($serverData.send(message)).toBe(sendResponse);
    expect($connection.send).toHaveBeenCalledWith(message);
    expect($serverData.get("/awe/template/angular/criteria/status.html")).toBe(getResponse);
    expect($connection.get).toHaveBeenCalledWith("/awe/template/angular/criteria/status.html");
  });

  it("retrieves the initial screen without an option parameter and logs the initial-screen branch", async function () {
    const screenData = {data: {template: "<main>Initial</main>"}};
    jest.spyOn($serverData, "getFormValues").mockReturnValue({CrtStatus: "ACTIVE"});
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($connection, "post").mockReturnValue(Promise.resolve(screenData));
    jest.spyOn($storage, "put");
    $log.info = jest.fn();

    await expect($serverData.getScreenData(null, "base")).resolves.toBe("<main>Initial</main>");

    expect($connection.post).toHaveBeenCalledWith("/awe/screen-data", {CrtStatus: "ACTIVE", view: "base", template: true}, "application/json");
    expect($log.info).toHaveBeenCalledWith("Retrieving screen data for initial screen");
    expect($storage.put).toHaveBeenCalledWith("screenData-base", screenData.data);
  });

  it("stores components without optional model fields, current-option updates, or launch actions", function () {
    const storage = {controller: {base: {}}, messages: {}, api: {base: {}}, screen: {}, model: {base: {}}};
    const data = {screen: {name: "plain-screen", option: "PlainOpt"}, messages: {}, components: [{id: "CrtPlain", controller: {id: "CrtPlain"}, model: null}]};
    jest.spyOn($storage, "get").mockImplementation(key => storage[key]);
    jest.spyOn($actionController, "addActionList");

    expect($serverData.storeScreenData(data, "center")).toBeUndefined();

    expect(storage.controller.center.CrtPlain).toEqual({id: "CrtPlain", optionId: "PlainOpt"});
    expect(storage.model.center.CrtPlain).toEqual({values: [], selected: null, defaultValues: null});
    expect(storage.api.center.CrtPlain).toEqual({});
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it("uses fallback URL, template, printing, server-action, and cancellation branches when optional inputs are absent", function () {
    const success = jest.fn().mockName("success").mockImplementation(callback => callback("<div>Fallback</div>", 200));
    const action = {attr: jest.fn().mockName("attr").mockImplementation(key => ({callbackTarget: null, parameters: undefined}[key]))};
    jest.spyOn($connection, "getRawUrl").mockReturnValue("/awe");
    jest.spyOn($serverData, "get").mockReturnValue({success});
    jest.spyOn($templateCache, "put");
    jest.spyOn($storage, "get").mockImplementation(key => ({api: {center: {Printable: {getPrintData: jest.fn().mockName("getPrintData").mockReturnValue({orientation: "PORTRAIT"})}}}, model: {}, screen: {center: {name: "fallback-screen"}}}[key] || {}));
    jest.spyOn($control, "changeControllerAttribute");
    jest.spyOn($control, "getAddressApi");
    jest.spyOn($connection, "sendMessage").mockReturnValue({});

    expect($serverData.getTemplateUrl("orders", "modal")).toBe("/awe/template/screen/modal/orders");
    $serverData.preloadAngularTemplate({path: "criteria/fallback.html"});
    expect($templateCache.put).toHaveBeenCalledWith("criteria/fallback.html", "<div>Fallback</div>");
    expect($serverData.getFormValuesForPrinting()).toEqual({orientation: "PORTRAIT"});
    expect($serverData.getServerAction({view: "center", component: "CrtStatus"}, {targetAction: "loadStatus"}, false, true)).toEqual(expect.objectContaining({parameters: expect.objectContaining({serverAction: "data"})}));

    $serverData.launchServerAction(action, {CrtStatus: "ACTIVE"});
    action.onCancel();

    expect($connection.sendMessage).toHaveBeenCalledWith({action, target: null, values: {CrtStatus: "ACTIVE"}});
  });
});
